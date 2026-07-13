# Terracotta

Declarative publishing for Minecraft projects. Define your metadata once in `terracotta.yml` or in your build configuration like [Gradle](https://gradle.org/), then sync with the [Modrinth](https://modrinth.com/) registry. Add [Hangar](https://hangar.papermc.io/) later using the same workflow.

---

<div class="grid cards" markdown>

-   :material-file-document-edit-outline:{ .lg .middle } __Define Once__

    ---

    Describe your project, versions, loaders, and supported game versions in one place. Terracotta auto-detects what it can from `README.md`, `LICENSE`, `CHANGELOG.md`, and platform-specific files.

-   :material-eye-outline:{ .lg .middle } __Plan Before You Publish__

    ---

    Run `./gradlew terracottaPlan` to see exactly what will change on Modrinth before any upload happens.

-   :material-publish:{ .lg .middle } __Publish to Modrinth__

    ---

    Run `./gradlew terracottaApply` to create or update your Modrinth project, upload versions, update metadata, and synchronize categories in one step. [Add Hangar](content/integration/how-to-guides/adding-hangar-to-gradle-plugin.md) when you are ready for a second registry.

-   :material-puzzle:{ .lg .middle } __Pluggable by Design__

    ---

    The core library, provider interfaces, and Gradle plugin are separate modules. Add new registries or build-tool integrations without changing the rest of the codebase.

</div>

## What Terracotta does

Terracotta compares the state you want with the state that exists on the configured registry, then computes and applies the smallest set of changes needed:

1. **Read local state** from `terracotta.yml`, build configuration files like `build.gradle.kts`, and detected project files.
2. **Fetch remote state** from the configured provider.
3. **Compute a diff** that produces semantic operations such as `CreateProject`, `UpdateMetadata`, `UpdateCategories`, `UploadVersion`, and gallery image changes.
4. **Apply the operations** or print a human-readable plan first.

Gallery images can optionally declare a stable `key` so they are matched across runs by identity rather than by title. This avoids accidental delete-and-reupload cycles when you rename screenshots or reorder images.

Example plan output:

```text
~ Update summary (from: "Old summary" to: "Lightweight Paper plugin")
~ Update categories (from: ["utility"] to: ["utility", "paper"])
+ Upload version 1.2.0 (file: build/libs/my-plugin-1.2.0.jar)
```

## Installation

=== "Gradle plugin"

    Add the plugin to your `build.gradle.kts`:

    ```kotlin
    plugins {
        id("io.github.beduality.terracotta") version "0.8.0"
    }
    ```

    Then add Modrinth:

    - [Adding Modrinth to the Gradle plugin](content/integration/how-to-guides/adding-modrinth-to-gradle-plugin.md)

    To publish to Hangar as well, see [Adding Hangar to the Gradle plugin](content/integration/how-to-guides/adding-hangar-to-gradle-plugin.md).

=== "Core library"

    Add the core and provider dependencies directly:

    ```kotlin
    dependencies {
        implementation("io.github.beduality:terracotta-core:0.2.0")
        implementation("io.github.beduality:terracotta-provider-modrinth:0.2.0")
    }
    ```

    To add Hangar, include `io.github.beduality:terracotta-provider-hangar:0.2.0` as well. See the [Core installation guide](content/modules/core/tutorials/installation.md) and the [Modrinth provider tutorial](content/modules/provider-modrinth/tutorials/using-modrinth.md) for a complete walkthrough.

## A minimal `terracotta.yml`

```yaml
providers:
  modrinth:
    projectId: "my-modrinth-project-id"
```

For most projects, this is enough. Terracotta detects `name`, `summary`, `description`, `gameVersions`, `loaders`, `environment`, `license`, `releaseType`, and `changelog` from files such as `README.md`, `fabric.mod.json`, `paper-plugin.yml`, `LICENSE`, `CHANGELOG.md`, and your Gradle project metadata. Only `providers` is required to tell Terracotta where to publish. See the [Config Schema reference](content/modules/core/reference/config-schema.md) for every available field.

## Common tasks

| Task | Command |
|---|---|
| Plan changes | `./gradlew terracottaPlan` |
| Apply changes | `./gradlew terracottaApply` |

See the [Gradle tasks reference](content/modules/gradle-plugin/reference/tasks.md) for the full list.

## Documentation

Terracotta docs are organized by what you are trying to do:

- **[Quick Start](content/modules/gradle-plugin/tutorials/getting-started.md)**: Publish your first release with the Gradle plugin.
- **[Navigating the Docs](content/navigating-docs.md)**: Learn how the docs are organized and where to look next.
- **[Integration](content/integration/README.md)**: Add a provider to the Gradle plugin, starting with Modrinth.
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
| Target registry | Modrinth (Hangar support available) |

## Links

- [:fontawesome-brands-github: GitHub](https://github.com/beduality/terracotta)
- [:fontawesome-brands-discord: Discord](https://discord.gg/D5meCv2Wnd)
- [License](LICENSE.md)
