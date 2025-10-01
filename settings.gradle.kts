pluginManagement {
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
