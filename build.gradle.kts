plugins {
    `maven-publish`
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}

buildscript {

    dependencies {

    }
}