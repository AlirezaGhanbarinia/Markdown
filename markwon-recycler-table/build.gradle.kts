plugins {
    `maven-publish`
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {

    namespace = "io.noties.markwon.recycler.table"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.min.sdk.get().toInt()
        targetSdk = libs.versions.target.sdk.get().toInt()
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    api(projects.markwonCore)
    api(projects.markwonRecycler)
    api(projects.markwonExtTables)
    implementation(libs.androidx.core.ktx)
}