plugins {
    alias(libs.plugins.android.library)
}

android {

    namespace = "io.noties.markwon"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.min.sdk.get().toInt()
    }
}

dependencies {

    api(libs.annotations)
    api(libs.commonmark)

    compileOnly(libs.core)
    compileOnly(libs.appcompat)

    implementation(libs.androidx.core.ktx)

}