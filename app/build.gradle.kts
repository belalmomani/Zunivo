plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Signing comes ONLY from environment variables (never commit keys or passwords).
// Without them, release builds are simply unsigned.
val keystorePath: String? = System.getenv("ZUNIVO_KEYSTORE_PATH")

android {
    namespace = "com.zunivo.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zunivo.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1          // +1 for every production release
        versionName = "0.1.0"
    }

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ZUNIVO_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ZUNIVO_KEY_ALIAS")
                keyPassword = System.getenv("ZUNIVO_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
