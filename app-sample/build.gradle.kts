plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.parcelize)
}

android {

    namespace = "io.noties.markwon.app"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    defaultConfig {
        applicationId = "io.noties.markwon.app"
        minSdk = 23
        targetSdk = libs.versions.target.sdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        setProperty("archivesBaseName", "markwon")

        buildConfigField("String", "GIT_REPOSITORY", "\"https://github.com/noties/Markwon\"")

        val scheme = "markwon"
        buildConfigField("String", "DEEPLINK_SCHEME", "\"$scheme\"")
        manifestPlaceholders["deeplink_scheme"] = scheme
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_1_8
        sourceCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    sourceSets {
        named("main") {
            java {
                srcDir("../sample-utils/annotations")
            }
        }
    }
}

kapt {
    arguments {
        arg("markwon.samples.file", "${projectDir}/samples.json".toString())
    }
}


configurations.all {
    exclude(group = "org.jetbrains", module= "annotations-java5")
}

dependencies {

    kapt(projects.sampleUtils.processor)
    kapt(libs.prism4j.bundler)

    implementation(projects.markwonCore)
    implementation(projects.markwonEditor)
    implementation(projects.markwonExtLatex)
    implementation(projects.markwonExtStrikethrough)
    implementation(projects.markwonExtTables)
    implementation(projects.markwonExtTasklist)
    implementation(projects.markwonHtml)
    implementation(projects.markwonImage)
    implementation(projects.markwonInlineParser)
    implementation(projects.markwonLinkify)
    implementation(projects.markwonRecycler)
    implementation(projects.markwonRecyclerTable)
    implementation(projects.markwonSimpleExt)
    implementation(projects.markwonSyntaxHighlight)

    implementation(projects.markwonImagePicasso)
    implementation(projects.markwonImageGlide)
    implementation(projects.markwonImageCoil)

    implementation(libs.appcompat)
    implementation(libs.kotlin.stdlib.jdk7)
    implementation(libs.recyclerView)
    implementation(libs.cardview)
    implementation(libs.fragment)
    implementation(libs.okhttp)
    implementation(libs.prism4j)
    implementation(libs.gson)
    implementation(libs.adapt)
    implementation(libs.debug)
    implementation(libs.android.svg)
    implementation(libs.android.gif)
    implementation(libs.coil)
}