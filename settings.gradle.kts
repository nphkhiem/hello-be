pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "english-for-your-children"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":feature:profiles")
include(":feature:learning")
include(":feature:caregiver")
include(":ui:tv")
include(":playback")
include(":domain")
include(":data")
include(":content:starter")
include(":test-support")
