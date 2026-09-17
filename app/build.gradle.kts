plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sandy.ravenscreenpet"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sandy.ravenscreenpet"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "5.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions { jvmTarget = "1.8" }
}
