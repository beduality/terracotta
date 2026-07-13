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
    id("io.github.beduality.terracotta") version "0.8.0"
}
```

## 2. Configure Terracotta

Create a `terracotta.yml` in your project root:

```yaml
providers:
  modrinth:
    projectId: "my-plugin" # your Modrinth project slug or ID
```

This is the smallest file that Terracotta needs. The plugin detects `name`, `summary`, `description`, `gameVersions`, `loaders`, `environment`, `license`, `releaseType`, and `changelog` from files such as `README.md`, `paper-plugin.yml`, `LICENSE`, `CHANGELOG.md`, and your Gradle project metadata.

The version number is discovered from the `jar` task output, and the artifact path is resolved from the main JAR produced by the build.

You can add explicit values to `terracotta.yml` to override detected values. Values set in the Kotlin DSL override both. See the [Kotlin DSL how-to guide](../how-to-guides/kotlin-dsl-configuration.md) for details.

## 3. Configure Authentication

Create a Modrinth API token from your Modrinth account settings.

Set the token as an environment variable:

=== "Linux / macOS"

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

* See the [Config Schema reference](../../core/reference/config-schema.md) for all available fields.
* Add a [Hangar provider](../../../integration/how-to-guides/adding-hangar-to-gradle-plugin.md) to publish to multiple registries.
* Read the [Kotlin DSL how-to guide](../how-to-guides/kotlin-dsl-configuration.md) to override values directly in `build.gradle.kts`.
