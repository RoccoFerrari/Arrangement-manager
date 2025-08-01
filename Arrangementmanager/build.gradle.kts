// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

buildscript {
    dependencies {
        // ... altre dipendenze
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.7.7") // o una versione più recente
    }
}
