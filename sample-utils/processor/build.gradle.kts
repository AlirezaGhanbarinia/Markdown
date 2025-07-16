plugins {
    `maven-publish`
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

sourceSets {
    named("main") {
        java {
            srcDir("../annotations")
        }
    }
}

dependencies {
    implementation(libs.annotations)
    implementation(libs.gson)
    implementation(libs.commons.io)
}