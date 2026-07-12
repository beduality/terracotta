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
include("terracotta-state-filesystem")
include("terracotta-provider-modrinth")
include("terracotta-provider-hangar")
include("terracotta-gradle-plugin")
include("modules:terracotta-github")

project(":terracotta-core").projectDir = file("modules/terracotta-core")
project(":terracotta-state-filesystem").projectDir = file("modules/terracotta-state-filesystem")
project(":terracotta-provider-modrinth").projectDir = file("modules/terracotta-provider-modrinth")
project(":terracotta-provider-hangar").projectDir = file("modules/terracotta-provider-hangar")
project(":terracotta-gradle-plugin").projectDir = file("modules/terracotta-gradle-plugin")
project(":modules:terracotta-github").projectDir = file("modules/terracotta-github")
