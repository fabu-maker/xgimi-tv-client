plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val signingPassword = providers.environmentVariable("XGIMI_SIGNING_PASSPHRASE").orNull
val signingKeystore = rootProject.file("build/signing/release.p12")

android {
    namespace = "dev.projectortv.app"
    compileSdk = 35

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

    defaultConfig {
        applicationId = "dev.projectortv.client"
        minSdk = 28
        targetSdk = 35
        versionCode = 5
        versionName = "0.5.0-tv-facade"
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
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
