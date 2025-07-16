pluginManagement {
    repositories {
        maven { setUrl("https://maven.myket.ir") }
        gradlePluginPortal()
        google()
        mavenCentral()

        maven { setUrl("https://jitpack.io") }
    }
    dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
        repositories {
            maven { setUrl("https://maven.myket.ir") }
            gradlePluginPortal()
            google()
            mavenCentral()
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app-sample")
include(":sample-utils:processor")


include(":markwon-core")
include(":markwon-editor")
include(":markwon-ext-latex")
include(":markwon-ext-strikethrough")
include(":markwon-ext-tables")
include(":markwon-ext-tasklist")
include(":markwon-html")
include(":markwon-image")
include(":markwon-image-coil")
include(":markwon-image-glide")
include(":markwon-image-picasso")
include(":markwon-inline-parser")
include(":markwon-linkify")
include(":markwon-recycler")
include(":markwon-recycler-table")
include(":markwon-simple-ext")
include(":markwon-syntax-highlight")
include(":markwon-test-span")

