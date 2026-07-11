# Terracotta

[Docs](https://beduality.github.io/terracotta/) | [Contributing](./CONTRIBUTING.md) | [MIT License](./LICENSE)

Declarative Minecraft project registry management tool. Define your project metadata in `terracotta.yml` (or in your `build.gradle.kts`) and sync it to registries like Modrinth and Hangar. Terracotta auto-detects loaders, environment, license, and description from standard project files when you leave them unset.

## Installation

Add the Terracotta plugin and the provider you want to use (e.g., Modrinth) to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.beduality.terracotta") version "0.1.4"
}
```

See the [Gradle Plugin Documentation](https://beduality.github.io/terracotta/) for more details, or the [SDK Documentation](https://beduality.github.io/terracotta/sdk/reference/installation/) if you want to use Terracotta as a library!


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

`loaders`, `environment`, `license`, `description`, `summary`, and `changelog` are automatically detected from files such as `fabric.mod.json`, `README.md`, `LICENSE`, and `CHANGELOG.md`. You can override any detected value by adding it to `terracotta.yml`, and you can change how files are interpreted with the `convention:` block. See the [Config documentation](https://beduality.github.io/terracotta/config/) for the full schema and convention options.

Provider-specific setup guides: [Hangar](https://beduality.github.io/terracotta/gradle-plugin/how-to-guides/adding-hangar-provider/) and [Modrinth](https://beduality.github.io/terracotta/sdk/how-to-guides/modrinth-provider/).

Run tasks:

- Run a dry run on all providers:
  ```bash
  ./gradlew terracottaPlan
  ```

- Apply changes to all providers:
  ```bash
  ./gradlew terracottaApply
  ```
