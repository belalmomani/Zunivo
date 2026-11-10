plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Signing comes ONLY from environment variables (never commit keys or passwords).
// Without them, release builds are simply unsigned.
val keystorePath: String? = System.getenv("ZUNIVO_KEYSTORE_PATH")

// Start.io production App ID: pass -PSTARTIO_APP_ID=... or the STARTIO_APP_ID environment variable.
// Without it, release builds contain NO active ads. Debug builds use Start.io's public demo ID with test ads.
val startioAppId: String? =
    (providers.gradleProperty("STARTIO_APP_ID").orNull ?: System.getenv("STARTIO_APP_ID"))?.trim()?.takeIf { it.isNotEmpty() }
// Supabase (designer pages): only the project URL and the PUBLIC anon key. Never put service_role anywhere in the app.
fun publicConfig(name: String, allowed: String): String =
    (providers.gradleProperty(name).orNull ?: System.getenv(name))?.trim().orEmpty().filter { it.isLetterOrDigit() || it in allowed }
val supabaseUrl: String = publicConfig("SUPABASE_URL", ":/._-")
val supabaseAnonKey: String = publicConfig("SUPABASE_ANON_KEY", "._-")
// Builds for Google Play must not self-update from APKs: pass -PPLAY_BUILD=true.
val playBuild: Boolean = providers.gradleProperty("PLAY_BUILD").orNull == "true"

android {
    namespace = "com.zunivo.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.zunivo.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1          // +1 for every production release
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "SELF_UPDATE_ENABLED", (!playBuild).toString())
        buildConfigField("String", "SUPABASE_URL", "\"" + supabaseUrl + "\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"" + supabaseAnonKey + "\"")
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
        debug {
            buildConfigField("String", "STARTIO_APP_ID", "\"205489527\"")   // public Start.io demo ID, test ads only
            buildConfigField("boolean", "ADS_ENABLED", "true")
            buildConfigField("boolean", "ADS_TEST", "true")
        }
        release {
            if (startioAppId != null) {
                buildConfigField("String", "STARTIO_APP_ID", "\"" + startioAppId + "\"")
                buildConfigField("boolean", "ADS_ENABLED", "true")
            } else {
                // No App ID: the SDK is never initialised and no ad code runs. The app works normally, without ads.
                buildConfigField("String", "STARTIO_APP_ID", "\"\"")
                buildConfigField("boolean", "ADS_ENABLED", "false")
            }
            buildConfigField("boolean", "ADS_TEST", "false")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

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
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidsvg)
    implementation(libs.startapp.inapp)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
}
