plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "android.keychain.flutter"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }

    buildFeatures {
        buildConfig = true 
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.6.0")
    implementation("org.bouncycastle:bcpkix-jdk15to18:1.70")
}
