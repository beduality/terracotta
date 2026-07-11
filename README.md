# Terracotta

[Docs](https://beduality.github.io/terracotta/) | [Contributing](./CONTRIBUTING.md) | [MIT License](./LICENSE)

Declarative Minecraft project registry management tool. Define your project metadata, description, tags, and version artifacts in your `build.gradle.kts` and sync them to registries like Modrinth.

## Installation

Add the Terracotta plugin and the provider you want to use (e.g., Modrinth) to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.beduality.terracotta") version "0.1.3"
}
```

See the [Gradle Plugin Documentation](https://beduality.github.io/terracotta/) for more details, or the [SDK Documentation](https://beduality.github.io/terracotta/sdk/reference/installation/) if you want to use Terracotta as a library!


## Usage

Create a `terracotta.yml` in your project root:

```yaml
name: "My Plugin"
summary: "Lightweight Paper plugin"
description: "A useful Paper plugin."
license: "MIT"
tags:
  - paper
  - utility
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
    projectId: "my-modrinth-project-id"
```

Versions are automatically discovered from your build.

Run tasks:

- Run a dry run on all providers:
  ```bash
  ./gradlew terracottaPlan
  ```

- Apply changes to all providers:
  ```bash
  ./gradlew terracottaApply
  ```
