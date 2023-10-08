pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
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
                includeGroup("com.github.invissvenska")
            }
        }
    }
}
rootProject.name = "Catima"
include(":app")
