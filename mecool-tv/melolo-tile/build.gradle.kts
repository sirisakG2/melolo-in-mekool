plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mecool.melolotile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mecool.melolotile"
        minSdk = 26
        targetSdk = 34
        versionCode = 5
        versionName = "2.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources.excludes += "META-INF/*"
    }
}

dependencies {
    // Talks to the box's own adbd on localhost:5555 to open Melolo in a freeform window.
    implementation("dev.mobile:dadb:1.2.7")
}
