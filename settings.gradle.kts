pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
// dependencyResolutionManagement {
//     repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // Changed from FAIL_ON_PROJECT_REPOS
//     repositories {
//         google()
//         mavenCentral()
//         // ivyLocal() removed as it was an incorrect attempt
//     }
// }
rootProject.name = "BillBuddy"
include(":androidApp")
include(":shared")
