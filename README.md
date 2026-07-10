# Terracotta

[Docs](https://beduality.github.io/terracotta/) | [Contributing](./CONTRIBUTING.md) | [MIT License](./LICENSE)

Declarative Minecraft project registry management tool. Define your project metadata, description, tags, and version artifacts in your `build.gradle.kts` and sync them to registries like Modrinth.

## Installation

Add the Terracotta plugin and the provider you want to use (e.g., Modrinth) to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.beduality.terracotta") version "0.1.0"
}
```

See the [Gradle Plugin Documentation](https://beduality.github.io/terracotta/) for more details, or the [SDK Documentation](https://beduality.github.io/terracotta/tutorials/installation_sdk/) if you want to use Terracotta as a library!


## Usage

Configure Terracotta in your `build.gradle.kts`:

```kotlin
import io.github.beduality.terracotta.core.model.TerracottaEnvironment
import io.github.beduality.terracotta.core.model.TerracottaLoader

terracotta {
    providers {
        create("modrinth") {
            projectId.set("my-modrinth-project-id")
            token.set(System.getenv("MODRINTH_TOKEN")) // optional, defaults to MODRINTH_TOKEN env var
        }
        // Add other future providers here, e.g., create("curseforge"), etc.
    }
    name = "My Plugin"
    summary = "Lightweight Paper plugin"
    description = file("README.md").readText()
    license = "MIT"
    tags = listOf("utility", "paper")
    gameVersions = listOf("1.20.1", "1.20.2")
    loaders = listOf(TerracottaLoader.PAPER)
    environment = TerracottaEnvironment.SERVER_ONLY
}
```

Run tasks:

- Run a dry run on all providers:
  ```bash
  ./gradlew terracottaPlan
  ```

- Apply changes to all providers:
  ```bash
  ./gradlew terracottaApply
  ```
