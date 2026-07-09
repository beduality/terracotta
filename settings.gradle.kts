pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "terracotta"

include("terracotta-core")
include("terracotta-provider-modrinth")
include("modules:terracotta-github")

project(":terracotta-core").projectDir = file("modules/terracotta-core")
project(":terracotta-provider-modrinth").projectDir = file("modules/terracotta-provider-modrinth")
project(":modules:terracotta-github").projectDir = file("modules/terracotta-github")
