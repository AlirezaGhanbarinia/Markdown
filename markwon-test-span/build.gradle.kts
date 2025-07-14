plugins {
    alias(libs.plugins.android.library)
}

android {

    namespace = "io.noties.markwon.test"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.min.sdk.get().toInt()
        targetSdk = libs.versions.target.sdk.get().toInt()
    }
}

dependencies {
    api(libs.annotations)
    api(libs.ixJava)
    implementation(libs.androidx.core.ktx)
}
