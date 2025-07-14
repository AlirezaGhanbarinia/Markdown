plugins {
    alias(libs.plugins.android.library)
}

android {

    namespace = "io.noties.markwon.image.glide"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.min.sdk.get().toInt()
        targetSdk = libs.versions.target.sdk.get().toInt()
    }
}

dependencies {
    api(projects.markwonCore)
    api(libs.glide)
    implementation(libs.androidx.core.ktx)
}