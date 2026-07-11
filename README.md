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
    name.set("My Plugin")
    summary.set("Lightweight Paper plugin")
    description.set(file("README.md").readText())
    license.set("MIT")
    tags.set(listOf("utility", "paper"))
    gameVersions.set(listOf("1.20.1", "1.20.2"))
    loaders.set(listOf(TerracottaLoader.PAPER))
    environment.set(TerracottaEnvironment.SERVER_ONLY)
    // Versions are automatically discovered from your build
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
