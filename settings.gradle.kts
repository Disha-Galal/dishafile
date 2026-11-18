pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "DishaFileKit"
include(":core")
include(":ai")
if (!providers.gradleProperty("coreOnly").isPresent) include(":app")
