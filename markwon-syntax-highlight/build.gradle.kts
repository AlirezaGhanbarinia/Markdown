plugins {
    alias(libs.plugins.android.library)
}

android {

    namespace = "io.noties.markwon.syntax"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.min.sdk.get().toInt()
        targetSdk = libs.versions.target.sdk.get().toInt()
    }
}

dependencies {
    api(projects.markwonCore)
    api(libs.prism4j)
    implementation(libs.androidx.core.ktx)
}
