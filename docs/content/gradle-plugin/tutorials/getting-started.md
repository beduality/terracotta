# Getting Started

This tutorial walks you through publishing your first plugin to Modrinth using Terracotta Gradle plugin.

By the end of this tutorial, you will have:

- Installed the Terracotta Gradle plugin
- Configured Terracotta in your build file
- Connected Terracotta to your Modrinth account
- Uploaded your first release

## Prerequisites

Before starting, you need:

1. A [Modrinth](https://modrinth.com/) account.
2. A Modrinth API token.
3. A Gradle-based Minecraft plugin project.

## 1. Install Terracotta Plugin

Add the Terracotta plugin to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.beduality.terracotta") version "0.1.0"
}
```

## 2. Configure Terracotta

In your `build.gradle.kts`, configure Terracotta with your project information:

```kotlin
terracotta {
    projectId = "my-plugin" // your Modrinth project slug or ID
    name = "My Plugin"
    summary = "Lightweight Paper plugin"
    description = file("README.md").readText()
    tags = listOf("paper", "utility")
    license = "MIT"
    gameVersions = listOf("1.21.8", "1.21.7")
    loaders = listOf("paper")
    environment = "server_only"
}
```

Make sure your project contains a `README.md` file with your plugin description.

## 3. Configure Modrinth authentication

Create a Modrinth API token from your Modrinth account settings.

Set the token as an environment variable:

=== "Linux / macOS"

    ```bash
    export MODRINTH_TOKEN="your_modrinth_api_token"
    ```

=== "Windows PowerShell"

    ```powershell
    $env:MODRINTH_TOKEN="your_modrinth_api_token"
    ```

## 4. Preview the changes

Before uploading anything, use `terracottaPlan` to see what Terracotta will change:

```bash
./gradlew terracottaPlan
```

Terracotta will compare your local configuration with the Modrinth project and show the operations it would perform.

## 5. Publish your plugin

Apply the changes:

```bash
./gradlew terracottaApply
```

Terracotta will update your Modrinth project metadata, upload your release, and publish your compiled artifact.

## Finished

Your plugin is now published on Modrinth through Terracotta.

Next steps:

* Learn about all available configuration options
* Automate releases in CI/CD
* Manage multiple versions and platforms
