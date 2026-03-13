pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        exclusiveContent {
            forRepositories(maven("https://jitpack.io"))
            filter {
                // limit jitpack repository to these groups
                includeGroup("com.github.yalantis")
            }
        }
    }
}
rootProject.name = "Catima"
include(":app")
