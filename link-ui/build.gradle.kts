plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-android-extensions")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("org.jetbrains.dokka").version("0.10.0")
    id("com.jfrog.bintray")
}

android {
    compileSdkVersion(Versions.compileSdk)
    buildToolsVersion(Versions.buildTools)

    defaultConfig {
        minSdkVersion(Versions.minSdk)
        targetSdkVersion(Versions.targetSdk)
        versionCode = TinkLinkVersion.code
        versionName = TinkLinkVersion.name

        buildConfigField("String", "libraryVersion", "\"$versionName\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    resourcePrefix("tink_")
}

dependencies {
    api(project(":link"))

    implementation(Dependencies.kotlin_stdlib)
    implementation(Dependencies.Androidx.appcompat)
    implementation(Dependencies.Androidx.core_ktx)
    implementation(Dependencies.Androidx.constraint_layout)
    testImplementation(Dependencies.junit)
    testImplementation(Dependencies.Testing.test_assertj)
    androidTestImplementation(Dependencies.Testing.test_assertj)
    androidTestImplementation(Dependencies.Androidx.test_runner)
    androidTestImplementation(Dependencies.Androidx.test_espresso)

    implementation(Dependencies.Dagger.core)
    implementation(Dependencies.Dagger.dagger_android)
    implementation(Dependencies.Dagger.dagger_android_support)
    kapt(Dependencies.Dagger.dagger_android_processor)
    kapt(Dependencies.Dagger.dagger_android_compiler)

    kapt(Dependencies.Moshi.moshi_codegen)

    implementation(Dependencies.Androidx.navigation_fragment)
    implementation(Dependencies.Androidx.navigation_ui)

    implementation(Dependencies.timber)
    implementation(Dependencies.three_ten_abp)

    implementation(Dependencies.material_components)
    implementation(Dependencies.picasso)

    implementation(Dependencies.Androidx.browser)

    implementation(Dependencies.zxing)
}

apply(from = "../publishing.gradle")

tasks {
    dokka {
        doLast {
            println("Copying docs from /docs/link to /docs")
            copy {
                from("../docs/link")
                into("../docs")
            }
            println("Deleting /docs/link")
            delete("../docs/link")
        }
    }
}