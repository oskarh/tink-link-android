object Versions {
    const val compileSdk = 30
    const val buildTools = "29.0.3"
    const val minSdk = 21
    const val targetSdk = 30

    const val kotlin = "1.4.21"
    const val dagger = "2.16"
    const val androidx_appcompat = "1.2.0"
    const val androidx_core_ktx = "1.3.2"
    const val coroutines = "1.3.9"
    const val retrofit = "2.6.2"
    const val rxjava = "2.1.9"
    const val navigation = "2.2.2"
    const val dokka = "0.10.1"
    const val moshi = "1.9.2"
}

object Dependencies {
    object Androidx {
        const val appcompat = "androidx.appcompat:appcompat:${Versions.androidx_appcompat}"
        const val core_ktx = "androidx.core:core-ktx:${Versions.androidx_core_ktx}"
        const val constraint_layout = "androidx.constraintlayout:constraintlayout:1.1.3"
        const val test_runner = "androidx.test:runner:1.2.0"
        const val test_espresso = "androidx.test.espresso:espresso-core:3.2.0"
        const val navigation_fragment = "androidx.navigation:navigation-fragment-ktx:${Versions.navigation}"
        const val navigation_ui = "androidx.navigation:navigation-ui-ktx:${Versions.navigation}"
        const val browser = "androidx.browser:browser:1.2.0"
    }

    object Coroutines {
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
        const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
    }

    object Dagger {
        private const val base_url = "com.google.dagger"
        const val core = "$base_url:dagger-android-support:${Versions.dagger}"
        const val dagger_android = "$base_url:dagger-android:${Versions.dagger}"
        const val dagger_android_support = "$base_url:dagger-android-support:${Versions.dagger}"
        const val dagger_android_processor = "$base_url:dagger-android-processor:${Versions.dagger}"
        const val dagger_android_compiler = "$base_url:dagger-compiler:${Versions.dagger}"
    }

    object Retrofit {
        const val retrofit = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
        const val retrofit_adapter_rxjava = "com.squareup.retrofit2:adapter-rxjava2:${Versions.retrofit}"
        const val retrofit_converter_gson =  "com.squareup.retrofit2:converter-gson:${Versions.retrofit}"
    }

    object Testing {
        const val test_junit_api = "org.junit.jupiter:junit-jupiter-api:5.2.0"
        const val test_junit_engine = "org.junit.jupiter:junit-jupiter-engine:5.2.0"
        const val test_junit_params = "org.junit.jupiter:junit-jupiter-params:5.2.0"
        const val test_mockk = "io.mockk:mockk:1.8.7"
        const val test_assertj = "org.assertj:assertj-core:3.11.1"
    }

    object Moshi {
        const val moshi_codegen = "com.squareup.moshi:moshi-kotlin-codegen:${Versions.moshi}"
    }

    const val material_components = "com.google.android.material:material:1.2.1"

    const val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"
    const val junit = "junit:junit:4.12"
    const val rxjava = "io.reactivex.rxjava2:rxjava:${Versions.rxjava}"
    const val three_ten_abp = "com.jakewharton.threetenabp:threetenabp:1.2.1"
    const val timber = "com.jakewharton.timber:timber:4.7.1"
    const val picasso = "com.squareup.picasso:picasso:2.71828"
    const val ktlint = "com.pinterest:ktlint:0.37.1"
    const val zxing = "com.google.zxing:core:3.4.0"

    object Tink {
        private const val coreVersion = "0.6.1"
        const val core = "com.tink:core:$coreVersion"
    }
}
