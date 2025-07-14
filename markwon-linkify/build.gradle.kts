plugins {
    alias(libs.plugins.android.library)
}

android {

    namespace = "io.noties.markwon.linkify"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.min.sdk.get().toInt()
        targetSdk = libs.versions.target.sdk.get().toInt()
    }
}

dependencies {
    compileOnly(libs.core)
    api(projects.markwonCore)
    implementation(libs.androidx.core.ktx)
}
