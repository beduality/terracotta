# Terracotta

Terracotta is a declarative Minecraft project registry management tool. Define your project metadata in `terracotta.yml` (or in your `build.gradle.kts`) and sync it to registries like Modrinth and Hangar. Terracotta auto-detects loaders, environment, license, and description from standard project files when you leave them unset.

---

<div class="grid cards" markdown>

-   :material-file-document-edit-outline:{ .lg .middle } __Declarative Configuration__

    ---

    Define your project info, description, tags, license, and version artifacts in `terracotta.yml` or your `build.gradle.kts`.

    [:octicons-arrow-right-24: Getting started](content/gradle-plugin/tutorials/getting-started.md)

-   :material-eye-outline:{ .lg .middle } __Dry Runs (Plan)__

    ---

    Preview all updates, tag changes, and version uploads before executing them, preventing incorrect uploads or metadata state drift.

    [:octicons-arrow-right-24: Gradle tasks](content/gradle-plugin/reference/tasks.md)

-   :material-publish:{ .lg .middle } __Maven SDK__

    ---

    Integrate Terracotta's core directly into custom automation pipelines.

    [:octicons-arrow-right-24: Core API](content/sdk/reference/api.md)

-   :material-sync:{ .lg .middle } __CI/CD Integration__

    ---

    Easily integrate with GitHub Actions to apply registry changes automatically when you push tags or release artifacts.

    [:octicons-arrow-right-24: CI/CD Guide](content/gradle-plugin/how-to-guides/ci-cd-setup.md)

</div>

## How It Works

1. You configure Terracotta in `terracotta.yml` or your `build.gradle.kts`.
2. Run `./gradlew terracottaPlan` to view a diff of local configuration vs remote registry state.
3. Run `./gradlew terracottaApply` to push changes (metadata, upload versions, synchronize tags) to the remote registry.

Example output from `terracottaPlan`:

```text
~ Update summary (from: "Old summary" to: "Lightweight Paper plugin")
~ Update tags (from: ["utility"] to: ["utility", "paper"])
+ Upload version 1.2.0 (file: build/libs/my-plugin-1.2.0.jar)
```

## Installation

Add the Terracotta plugin and the provider you want to use (for example, Modrinth) to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.beduality.terracotta") version "0.4.0"
}
```

See the [Gradle Plugin installation guide](content/gradle-plugin/tutorials/installation.md) for more details, or the [SDK installation guide](content/sdk/reference/installation.md) if you want to use Terracotta as a library.

## Usage

Create a `terracotta.yml` in your project root. Only the values that cannot be inferred from your project files need to be declared explicitly:

```yaml
name: "My Plugin"
tags:
  - paper
  - utility
gameVersions:
  - "1.21.8"
  - "1.21.7"
releaseType: release

providers:
  modrinth:
    projectId: "my-modrinth-project-id"
  hangar:
    projectId: "my-hangar-project-slug"
```

`loaders`, `environment`, `license`, `description`, `summary`, and `changelog` are automatically detected from files such as `fabric.mod.json`, `README.md`, `LICENSE`, and `CHANGELOG.md`. You can override any detected value by adding it to `terracotta.yml`, and you can change how files are interpreted with the `convention:` block. See the [Config documentation](content/config/index.md) for the full schema and convention options.

Provider-specific setup guides: [Hangar](content/gradle-plugin/how-to-guides/adding-hangar-provider.md) and [Modrinth](content/sdk/how-to-guides/modrinth-provider.md).

Run tasks:

- Run a dry run on all providers:
  ```bash
  ./gradlew terracottaPlan
  ```

- Apply changes to all providers:
  ```bash
  ./gradlew terracottaApply
  ```

## Setup Requirements

| Component | Version |
|---|---|
| JVM / JDK | 17+ |
| Gradle | 8.0+ |
| Target Registries | Modrinth, Hangar |

## Links

- [:fontawesome-brands-github: GitHub](https://github.com/beduality/terracotta)
- [:fontawesome-brands-discord: Discord](https://discord.gg/D5meCv2Wnd)
- [License](LICENSE.md)
