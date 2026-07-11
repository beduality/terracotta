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
    id("io.github.beduality.terracotta") version "0.1.4"
}
```

## 2. Configure Terracotta

Create a `terracotta.yml` in your project root:

```yaml
name: "My Plugin"
summary: "Lightweight Paper plugin"
description: "A useful Paper plugin."
tags:
  - paper
  - utility
license: "MIT"
gameVersions:
  - "1.21.8"
  - "1.21.7"
loaders:
  - paper
environment: server_only
releaseType: release
changelog: "Initial release"

providers:
  modrinth:
    projectId: "my-plugin" # your Modrinth project slug or ID
```

The version is automatically discovered from your Gradle build (via the `jar` task output).

You can omit fields such as `loaders`, `environment`, `license`, `description`, `summary`, and `changelog` when the corresponding project files (e.g. `fabric.mod.json`, `README.md`, `LICENSE`, `CHANGELOG.md`) are present. Terracotta detects them automatically. Values in `terracotta.yml` override detected values, and values in the Kotlin DSL override both.

If you prefer to configure Terracotta in `build.gradle.kts`, see the [Kotlin DSL how-to guide](../how-to-guides/kotlin-dsl-configuration.md). You can also mix both: put shared metadata in `terracotta.yml` and override specific values in the Kotlin DSL.

## 3. Configure Authentication

Create a Modrinth API token from your Modrinth account settings.

Set the token as an environment variable for Modrinth:

```=== "Linux / macOS"

    ```bash
    export MODRINTH_TOKEN="your_modrinth_token"
    ```

=== "Windows PowerShell"

    ```powershell
    $env:MODRINTH_TOKEN="your_modrinth_token"
    ```

Or set the token explicitly in your build.gradle.kts:

```kotlin
providers {
    create("modrinth") {
        projectId.set("my-modrinth-project-id")
        token.set(System.getenv("MODRINTH_TOKEN"))
    }
}
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
