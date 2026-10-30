// AGP 9 ships built-in Kotlin (KGP 2.2.10 at runtime). We pin a newer KGP so the
// Compose compiler plugin version (same as Kotlin) matches. Keep this in sync with
// `kotlin` in gradle/libs.versions.toml.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
