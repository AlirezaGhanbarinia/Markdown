plugins {
    alias(libs.plugins.android.library)
}

android {

    namespace = "io.noties.markwon.ext.latex"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.min.sdk.get().toInt()
        targetSdk = libs.versions.target.sdk.get().toInt()
    }
}

dependencies {

    api(projects.markwonCore)
    api(projects.markwonInlineParser)

    api(libs.jlatexmath.android)

    implementation(libs.androidx.core.ktx)
}