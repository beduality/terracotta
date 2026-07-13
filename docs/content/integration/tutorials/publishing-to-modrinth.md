# Publishing to Modrinth

This tutorial walks you through publishing a Minecraft plugin to Modrinth with Terracotta.

## What you'll learn

- How to configure the Modrinth provider in `terracotta.yml`.
- How to authenticate with a Modrinth token.
- How to plan and apply changes to Modrinth.

## Prerequisites

- The Terracotta Gradle plugin installed. See [Installing the Gradle plugin](../../modules/gradle-plugin/tutorials/installation.md).
- The `terracotta-state-filesystem` module on your buildscript classpath (required for the default state backend).
- A Modrinth account and API token.
- A Gradle-based Minecraft plugin project with a JAR artifact.

## 1. Install the state backend

If you have not already, add the filesystem state backend to your `build.gradle.kts`:

```kotlin
buildscript {
    dependencies {
        classpath("io.github.beduality:terracotta-state-filesystem:0.8.0")
    }
}
```

## 2. Configure the Modrinth provider

Create or edit `terracotta.yml` in your project root:

```yaml
providers:
  modrinth:
    projectId: "my-plugin"
```

Use your Modrinth project slug or ID.

## 3. Set the token

Export the token in your shell:

```bash
export MODRINTH_TOKEN="your_modrinth_token"
```

On Windows PowerShell:

```powershell
$env:MODRINTH_TOKEN="your_modrinth_token"
```

Terracotta reads the token from the `MODRINTH_TOKEN` environment variable by default.

## 4. Plan the release

Run the plan task to see what Terracotta will do:

```bash
./gradlew terracottaPlan
```

Review the project metadata, version, supported loaders, and game versions.

## 5. Apply the release

When the plan looks correct, apply it:

```bash
./gradlew terracottaApply
```

Terracotta creates or updates the project on Modrinth and uploads the version.

## What's next?

- [Add Hangar to the Gradle plugin](../how-to-guides/adding-hangar-to-gradle-plugin.md) to publish to multiple registries.
- [Provider configuration](../reference/provider-configuration.md)
- [Troubleshoot provider integration](../how-to-guides/troubleshooting.md)
