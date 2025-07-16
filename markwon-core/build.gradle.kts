plugins {
    `maven-publish`
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {

    namespace = "io.noties.markwon"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.min.sdk.get().toInt()
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    api(libs.annotations)
    api(libs.commonmark)

    compileOnly(libs.core)
    compileOnly(libs.appcompat)

    implementation(libs.androidx.core.ktx)

}