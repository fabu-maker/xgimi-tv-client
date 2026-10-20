plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Stable release signing is injected only by CI after decrypting the keystore.
val signingPassword = providers.environmentVariable("XGIMI_SIGNING_PASSPHRASE").orNull
val signingKeystore = rootProject.file("build/signing/release.p12")

android {
    signingConfigs {
        if (!signingPassword.isNullOrBlank() && signingKeystore.isFile) {
            create("secureRelease") {
                storeFile = signingKeystore
                storePassword = signingPassword
                keyAlias = "projector-release"
                keyPassword = signingPassword
                storeType = "PKCS12"
            }
        }
    }
    namespace = "dev.projectortv.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.projectortv.client"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable = false
            if (!signingPassword.isNullOrBlank() && signingKeystore.isFile) {
                signingConfig = signingConfigs.getByName("secureRelease")
            }
        }
        debug { isDebuggable = true }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isReturnDefaultValues = true }
    lint { abortOnError = true; checkReleaseBuilds = true }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
