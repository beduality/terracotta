# Terracotta

Declarative publishing for Minecraft projects. Define your metadata once in `terracotta.yml` or in your build configuration like [Gradle](https://gradle.org/), then automatically sync to providers like [Modrinth](https://modrinth.com/) and [Hangar](https://hangar.papermc.io/).

---

<div class="grid cards" markdown>

-   :material-file-document-edit-outline:{ .lg .middle } __Define Once__

    ---

    Describe your project, versions, loaders, and supported game versions in one place. Terracotta auto-detects what it can from `README.md`, `LICENSE`, `CHANGELOG.md`, and platform-specific files.

-   :material-eye-outline:{ .lg .middle } __Plan Before You Publish__

    ---

    Run `./gradlew terracottaPlan` to see exactly what will change on every registry before any upload happens.

-   :material-publish:{ .lg .middle } __Publish Everywhere__

    ---

    Run `./gradlew terracottaApply` to create projects, upload versions, update metadata, and synchronize tags across Modrinth and Hangar in one step.

-   :material-puzzle:{ .lg .middle } __Pluggable by Design__

    ---

    The core library, provider interfaces, and Gradle plugin are separate modules. Add new registries or build-tool integrations without changing the rest of the codebase.

</div>

## What Terracotta does

Terracotta compares the state you want with the state that exists on each registry, then computes and applies the smallest set of changes needed:

1. **Read local state** from `terracotta.yml`, build configuration files like `build.gradle.kts`, and detected project files.
2. **Fetch remote state** from each configured provider.
3. **Compute a diff** that produces semantic operations such as `CreateProject`, `UpdateMetadata`, `UpdateTags`, and `UploadVersion`.
4. **Apply the operations** or print a human-readable plan first.

Example plan output:

```text
~ Update summary (from: "Old summary" to: "Lightweight Paper plugin")
~ Update tags (from: ["utility"] to: ["utility", "paper"])
+ Upload version 1.2.0 (file: build/libs/my-plugin-1.2.0.jar)
```

## Installation

=== "Gradle plugin"

    Add the plugin to your `build.gradle.kts`:

    ```kotlin
    plugins {
        id("io.github.beduality.terracotta") version "0.3.0"
    }
    ```

    Then add a provider:

    - [Adding Modrinth to the Gradle plugin](content/integration/how-to-guides/adding-modrinth-to-gradle-plugin.md)
    - [Adding Hangar to the Gradle plugin](content/integration/how-to-guides/adding-hangar-to-gradle-plugin.md)

=== "Core library"

    Add the core and provider dependencies directly:

    ```kotlin
    dependencies {
        implementation("io.github.beduality:terracotta-core:0.2.0")
        implementation("io.github.beduality:terracotta-provider-modrinth:0.2.0")
    }
    ```

    See the [Core installation guide](content/modules/core/tutorials/installation.md) and the [Modrinth provider tutorial](content/modules/provider-modrinth/tutorials/using-modrinth.md) for a complete walkthrough.

## A minimal `terracotta.yml`

```yaml
providers:
  modrinth:
    projectId: "my-modrinth-project-id"
  hangar:
    projectId: "my-hangar-project-slug"
```

For most projects, this is enough. Terracotta detects `name`, `summary`, `description`, `gameVersions`, `loaders`, `environment`, `license`, `releaseType`, and `changelog` from files such as `README.md`, `fabric.mod.json`, `paper-plugin.yml`, `LICENSE`, `CHANGELOG.md`, and your Gradle project metadata. Only `providers` is required to tell Terracotta where to publish. See the [Config Schema reference](content/modules/core/reference/config-schema.md) for every available field.

## Common tasks

| Task | Command |
|---|---|
| Plan changes across all providers | `./gradlew terracottaPlan` |
| Apply changes across all providers | `./gradlew terracottaApply` |
| Plan only Modrinth | `./gradlew terracottaPlanModrinth` |
| Apply only Modrinth | `./gradlew terracottaApplyModrinth` |

See the [Gradle tasks reference](content/modules/gradle-plugin/reference/tasks.md) for the full list.

## Documentation

Terracotta docs are organized by what you are trying to do:

- **[Quick Start](content/modules/gradle-plugin/tutorials/getting-started.md)**: Publish your first release with the Gradle plugin.
- **[Navigating the Docs](content/navigating-docs.md)**: Learn how the docs are organized and where to look next.
- **[Integration](content/integration/README.md)**: Add Modrinth or Hangar to the Gradle plugin.
- **[Modules](content/modules/overview.md)**
  - [Core](content/modules/core/README.md): Domain models, diff engine, and provider SPI.
  - [Gradle Plugin](content/modules/gradle-plugin/README.md): DSL, tasks, and build integration.
  - [Modrinth Provider](content/modules/provider-modrinth/README.md): Modrinth registry integration.
  - [Hangar Provider](content/modules/provider-hangar/README.md): Hangar registry integration.
- **[Repo](content/repo/README.md)**: Build, test, contribute, and release Terracotta itself.

## Setup requirements

| Component | Version |
|---|---|
| JVM / JDK | 17+ (builds use JDK 21) |
| Gradle | 8.0+ |
| Target registries | Modrinth, Hangar |

## Links

- [:fontawesome-brands-github: GitHub](https://github.com/beduality/terracotta)
- [:fontawesome-brands-discord: Discord](https://discord.gg/D5meCv2Wnd)
- [License](LICENSE)
