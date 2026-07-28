pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "Awan"

includeBuild("build-logic")

// App module
include(":app")
include(":core:design-system")
include(":core:navigation")

// Core modules
include(":core:model")
include(":core:domain")
include(":core:data")
include(":core:common")
include(":core:datastore-proto")
include(":core:datastore")
include(":core:network")
include(":core:database")

// Feature modules
include(":feature:splash:api")
include(":feature:splash:impl")
include(":feature:onboarding:api")
include(":feature:onboarding:impl")
include(":feature:auth:api")
include(":feature:auth:impl")
include(":feature:home:api")
include(":feature:home:impl")
include(":feature:calendar:api")
include(":feature:calendar:impl")
include(":feature:chat:api")
include(":feature:chat:impl")
include(":feature:goals:api")
include(":feature:goals:impl")
include(":feature:profile:api")
include(":feature:profile:impl")
include(":feature:profile-setup:api")
include(":feature:profile-setup:impl")
// No api/impl split: the add-task sheet is state-driven, not a navigation destination, so it has
// no Route to export and only :app consumes it.
include(":feature:add-task")
include(":core:domain")
include(":core:data")
