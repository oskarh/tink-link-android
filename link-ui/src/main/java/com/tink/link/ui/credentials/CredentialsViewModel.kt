package com.tink.link.ui.credentials

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tink.core.Tink
import com.tink.link.authentication.AuthenticationTask
import com.tink.link.core.credentials.CredentialsRepository
import com.tink.link.core.credentials.CredentialsStatus
import com.tink.link.core.user.UserContext
import com.tink.link.getUserContext
import com.tink.link.ui.Event
import com.tink.link.ui.extensions.toFieldMap
import com.tink.model.credentials.Credentials
import com.tink.model.credentials.RefreshableItem
import com.tink.model.credentials.plus
import com.tink.model.misc.Field
import com.tink.model.provider.Provider
import com.tink.model.user.Scope
import com.tink.service.handler.ResultHandler
import com.tink.service.streaming.publisher.StreamObserver
import com.tink.service.streaming.publisher.StreamSubscription
import org.threeten.bp.Instant
import retrofit2.HttpException
import java.util.concurrent.atomic.AtomicBoolean

internal class CredentialsViewModel : ViewModel() {
    internal var scopes: List<Scope> = emptyList()
    internal var authorizeUser: Boolean = false

    private val userContext: UserContext = requireNotNull(Tink.getUserContext())
    private val credentialsRepository: CredentialsRepository = userContext.credentialsRepository

    private val _credentials = MutableLiveData<Credentials>()
    val credentials: LiveData<Credentials> = _credentials

    private val _authorizationCode = MutableLiveData<String>()
    val authorizationCode: LiveData<String> = _authorizationCode

    private val _viewState = MutableLiveData<ViewState>().also { it.value = ViewState.NOT_LOADING }
    val viewState: LiveData<ViewState> = MediatorLiveData<ViewState>().apply {
        fun update() {
            val viewState = _viewState.value ?: return
            value =
                if (authorizeUser &&
                    _authorizationCode.value.isNullOrEmpty() &&
                    viewState == ViewState.UPDATED
                ) {
                    if (authorizeUser && !authorizationDone.get()) {
                        // RC Fallback: We should authorize but didn't start it yet.
                        // This is due to never entering "loading" state (polling timer).
                        authorizeUser(scopes)
                    }
                    ViewState.UPDATING
                } else {
                    viewState
                }
        }
        addSource(_viewState) { update() }
        addSource(_authorizationCode) { if (authorizeUser) update() }
    }

    private val _errorEvent = MutableLiveData<Event<String>>()
    val errorEvent: LiveData<Event<String>> = _errorEvent

    private val _authenticationSuccessfulEvent = MutableLiveData<Event<Unit>>()
    val authenticationSuccessfulEvent: LiveData<Event<Unit>> = _authenticationSuccessfulEvent

    private val _fields = MutableLiveData<List<Field>>()
    val fields: LiveData<List<Field>> = _fields

    private var streamSubscription: StreamSubscription? = null
        set(value) {
            field?.unsubscribe()
            field = value
        }

    fun setFields(fields: List<Field>) = _fields.postValue(fields)
    fun updateViewState(viewState: ViewState) = _viewState.postValue(viewState)

    private fun getCredentialsStreamObserver(
        onAwaitingAuthentication: (AuthenticationTask) -> Unit,
        onError: (Throwable) -> Unit
    ): StreamObserver<CredentialsStatus> {
        return object : StreamObserver<CredentialsStatus> {
            override fun onNext(value: CredentialsStatus) {
                when (value) {
                    is CredentialsStatus.Success -> {
                        _credentials.postValue(value.credentials)
                        _viewState.postValue(ViewState.UPDATED)
                    }

                    is CredentialsStatus.Loading -> {
                        _viewState.postValue(ViewState.UPDATING)
                        _authenticationSuccessfulEvent.postValue(Event(Unit))
                        if (authorizeUser && !authorizationDone.get()) {
                            authorizeUser(scopes)
                        }
                    }

                    is CredentialsStatus.AwaitingAuthentication -> {
                        _viewState.postValue(ViewState.WAITING_FOR_AUTHENTICATION)
                        onAwaitingAuthentication(value.authenticationTask)
                    }
                }
            }

            override fun onError(error: Throwable) {
                _viewState.postValue(ViewState.NOT_LOADING)
                onError(error)
            }
        }
    }

    /**
     * Pass the filled [fields] to the [credentialsRepository] to authorize the user.
     */
    fun createCredentials(
        provider: Provider,
        fields: List<Field>,
        onAwaitingAuthentication: (AuthenticationTask) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        streamSubscription = credentialsRepository.create(
            provider.name,
            provider.credentialsType,
            fields.toFieldMap(),
            getCredentialsStreamObserver(onAwaitingAuthentication, onError),
            createRefreshableItems(scopes, provider.capabilities)
        )
    }

    fun updateCredentials(
        id: String,
        provider: Provider,
        fields: List<Field>,
        onAwaitingAuthentication: (AuthenticationTask) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        streamSubscription = credentialsRepository.update(
            id,
            provider.name,
            fields.toFieldMap(),
            getCredentialsStreamObserver(onAwaitingAuthentication, onError)
        )
    }

    fun authenticateCredentials(
        id: String,
        onAwaitingAuthentication: (AuthenticationTask) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        streamSubscription = credentialsRepository.authenticate(
            id,
            getCredentialsStreamObserver(onAwaitingAuthentication, onError)
        )
    }

    fun refreshCredentials(
        credentials: Credentials,
        forceAuthenticate: Boolean,
        onAwaitingAuthentication: (AuthenticationTask) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        streamSubscription = credentialsRepository.refresh(
            credentialsId = credentials.id,
            authenticate = credentials
                .sessionExpiryDate
                ?.let { it <= Instant.now() } // Set authenticate to TRUE if session has expired
                ?: forceAuthenticate,
            statusChangeObserver = getCredentialsStreamObserver(onAwaitingAuthentication, onError)
        )
    }

    private var currentlyAuthorizing = AtomicBoolean(false)
    private var authorizationDone = AtomicBoolean(false)

    private fun authorizeUser(scopes: List<Scope>) {
        if (currentlyAuthorizing.compareAndSet(false, true)) {
            userContext.authorize(
                scopes.toSet(),
                ResultHandler(
                    { authorizationCode ->
                        authorizationDone.set(true)
                        currentlyAuthorizing.set(false)
                        _authorizationCode.postValue(authorizationCode)
                    },
                    {
                        _errorEvent.postValue(Event(it.localizedMessage ?: ""))
                        currentlyAuthorizing.set(false)
                    }
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamSubscription?.unsubscribe()
    }

    enum class ViewState {
        NOT_LOADING,
        WAITING_FOR_AUTHENTICATION,
        UPDATING,
        UPDATED,
    }
}

internal fun createRefreshableItems(
    scopes: List<Scope>,
    providerCapabilities: List<Provider.Capability>
) =
    mutableSetOf<RefreshableItem>().apply {
        // Add default refreshable items - accounts, e-invoices and transfer destinations
        addAll(RefreshableItem.accounts() + RefreshableItem.EINVOICES + RefreshableItem.TRANSFER_DESTINATIONS)
        if (scopes.containsTransactions()) addAll(RefreshableItem.transactions())
        if (scopes.containsIdentityData()) add(RefreshableItem.IDENTITY_DATA)
    }
        .intersect(providerCapabilities.toRefreshableItems())

private fun List<Scope>.containsTransactions() =
    contains(Scope.TransactionsRead) ||
        contains(Scope.CustomScope("transactions:write")) || // TODO: Add to scope sealed class
        contains(Scope.CustomScope("transactions:categorize")) // TODO: Add to scope sealed class

private fun List<Scope>.containsIdentityData() =
    contains(Scope.IdentityRead) || contains(Scope.CustomScope("identity:write")) // TODO: Add to scope sealed class

private fun List<Provider.Capability>.toRefreshableItems(): Set<RefreshableItem> {
    val refreshableItems = mutableSetOf<RefreshableItem>()
    if (contains(Provider.Capability.CHECKING_ACCOUNTS)) {
        refreshableItems.addAll(RefreshableItem.CHECKING_ACCOUNTS + RefreshableItem.CHECKING_TRANSACTIONS)
    }
    if (contains(Provider.Capability.SAVINGS_ACCOUNTS)) {
        refreshableItems.addAll(RefreshableItem.SAVING_ACCOUNTS + RefreshableItem.SAVING_TRANSACTIONS)
    }
    if (contains(Provider.Capability.CREDIT_CARDS)) {
        refreshableItems.addAll(RefreshableItem.CREDITCARD_ACCOUNTS + RefreshableItem.CREDITCARD_TRANSACTIONS)
    }
    if (contains(Provider.Capability.LOANS)) {
        refreshableItems.addAll(RefreshableItem.LOAN_ACCOUNTS + RefreshableItem.LOAN_TRANSACTIONS)
    }
    if (contains(Provider.Capability.INVESTMENTS)) {
        refreshableItems.addAll(RefreshableItem.INVESTMENT_ACCOUNTS + RefreshableItem.INVESTMENT_TRANSACTIONS)
    }
    if (contains(Provider.Capability.EINVOICES)) {
        refreshableItems.add(RefreshableItem.EINVOICES)
    }
    if (contains(Provider.Capability.TRANSFERS)) {
        refreshableItems.add(RefreshableItem.TRANSFER_DESTINATIONS)
    }
    if (contains(Provider.Capability.IDENTITY_DATA)) {
        refreshableItems.add(RefreshableItem.IDENTITY_DATA)
    }
    return refreshableItems
}

fun Throwable.isExistingCredentialsError() = this is HttpException && this.code() == 409
