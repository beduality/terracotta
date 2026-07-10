# Terracotta

[Docs](https://beduality.github.io/terracotta/) | [Contributing](./CONTRIBUTING.md) | [MIT License](./LICENSE)

Declarative Minecraft project registry management tool. Define your project metadata, description, tags, and version artifacts in your `build.gradle.kts` and sync them to registries like Modrinth.

## Installation

### Gradle Plugin Installation

Add the Terracotta plugin and the provider you want to use (e.g., Modrinth) to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.beduality.terracotta") version "0.1.0"
}

dependencies {
    terracotta("io.github.beduality:terracotta-provider-modrinth:0.1.0")
}
```

### SDK Installation

Add the Terracotta Core library to your project:

**Gradle (Kotlin):**
```kotlin
implementation("io.github.beduality:terracotta-core:0.1.0")
```

**Maven:**
```xml
<dependency>
    <groupId>io.github.beduality</groupId>
    <artifactId>terracotta-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

For the Modrinth provider:
```kotlin
implementation("io.github.beduality:terracotta-provider-modrinth:0.1.0")
```

See [SDK Installation Documentation](https://beduality.github.io/terracotta/tutorials/installation_sdk/) for more details.

## Usage

1. Configure Terracotta in your `build.gradle.kts`:
   ```kotlin
   terracotta {
       provider = "modrinth" // defaults to "modrinth"
       projectId = "my-plugin"
       name = "My Plugin"
       summary = "Lightweight Paper plugin"
       description = file("README.md").readText()
       license = "MIT"
       tags = listOf("utility", "paper")
       gameVersions = listOf("1.20.1", "1.20.2")
       loaders = listOf("paper")
       environment = "server_only"
       // token = System.getenv("TERRACOTTA_TOKEN") // optional, defaults to TERRACOTTA_TOKEN env var
   }
   ```
2. Run a dry run to generate a plan:
   ```bash
   ./gradlew terracottaPlan
   ```
3. Apply the changes:
   ```bash
   ./gradlew terracottaApply
   ```
