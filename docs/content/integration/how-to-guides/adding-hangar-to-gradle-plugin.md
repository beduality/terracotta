# Adding Hangar

This guide explains how to add a [Hangar](https://hangar.papermc.io/) provider to an existing Terracotta setup. It assumes you have already installed the Terracotta Gradle plugin and configured your project metadata.

## Before you start

Hangar does not expose a project creation API. Log in to Hangar and create your project manually before running Terracotta. Make a note of the project slug — you will use it as the `projectId`.

## 1. Add the Hangar provider to `terracotta.yml`

Add a `hangar` entry under `providers:`

```yaml
providers:
  hangar:
    projectId: "my-plugin" # your Hangar project slug
```

If you already have other providers configured, add `hangar` alongside them:

```yaml
providers:
  modrinth:
    projectId: "my-plugin"
  hangar:
    projectId: "my-plugin"
```

## 2. Configure authentication

Create a Hangar API key from your Hangar account settings.

Set the key as an environment variable. Terracotta uses the `<PROVIDER>_TOKEN` convention, so the variable name for Hangar is `HANGAR_TOKEN`:

```=== "Linux / macOS"

    ```bash
    export HANGAR_TOKEN="your_hangar_token"
    ```

=== "Windows PowerShell"

    ```powershell
    $env:HANGAR_TOKEN="your_hangar_token"
    ```

Or set the token explicitly in your `build.gradle.kts`:

```kotlin
providers {
    create("hangar") {
        projectId.set("my-hangar-project-slug")
        token.set(System.getenv("HANGAR_TOKEN"))
    }
}
```

## 3. Verify supported loaders

Hangar uses a coarser platform model than individual loaders. Terracotta maps your configured loaders to Hangar platforms automatically:

| Terracotta loader | Hangar platform |
|-------------------|-----------------|
| `bukkit`          | `PAPER`         |
| `spigot`          | `PAPER`         |
| `paper`           | `PAPER`         |
| `purpur`          | `PAPER`         |
| `folia`           | `PAPER`         |
| `velocity`        | `VELOCITY`      |
| `bungeecord`      | `WATERFALL`     |
| `waterfall`       | `WATERFALL`     |

Unsupported loaders such as `fabric`, `forge`, `quilt`, `neoforge`, and `sponge` are skipped with a warning.

## 4. Plan and apply

Run a dry run to see what Terracotta will change on Hangar:

```bash
./gradlew terracottaPlanHangar
```

Apply the changes:

```bash
./gradlew terracottaApplyHangar
```

You can also run the aggregate tasks `terracottaPlan` and `terracottaApply` to plan or apply changes for all configured providers at once.

## Release channels

Hangar versions are uploaded to a channel. Terracotta maps your configured `releaseType` to Hangar channels:

| Terracotta `releaseType` | Hangar channel |
|--------------------------|------------------|
| `release`                | `Release`        |
| `beta`                   | `Snapshot`       |
| `alpha`                  | `Snapshot`       |

If the channel does not exist on the project yet, Terracotta creates it automatically before uploading.

## What's next?

- Learn about all available configuration options in the [Config Schema](../../config/schema.md).
- Use the [Kotlin DSL how-to guide](../../modules/gradle-plugin/how-to-guides/kotlin-dsl-configuration.md) to configure Hangar entirely in `build.gradle.kts`.
