# Using the Hangar Provider

The `terracotta-provider-hangar` module provides a complete implementation of the Terracotta provider interfaces for the [Hangar](https://hangar.papermc.io/) registry (PaperMC's plugin distribution platform).

## Installation

Add the dependency to your project:

=== "Gradle (Kotlin)"

    ```kotlin
    implementation("io.github.beduality:terracotta-provider-hangar:0.2.0")
    ```

=== "Maven"

    ```xml
    <dependency>
        <groupId>io.github.beduality</groupId>
        <artifactId>terracotta-provider-hangar</artifactId>
        <version>0.2.0</version>
    </dependency>
    ```

## Authentication

The Hangar provider requires an API key. You can create one from your Hangar account settings.

**Important**: Never hardcode API keys in your source code. Always load them from environment variables or configuration files.

### Loading from Environment Variables

```kotlin
val apiKey = System.getenv("HANGAR_TOKEN")
    ?: throw IllegalStateException("HANGAR_TOKEN environment variable not set")
```

### Loading from .env File (Recommended for Development)

Add the [dotenv-kotlin](https://github.com/soywod/kotlin-dotenv) library to your project:

```kotlin
implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
```

Create a `.env` file in your project root (add it to `.gitignore`):

```env
HANGAR_TOKEN=your-api-key-here
```

Load the key:

```kotlin
import io.github.cdimascio.dotenv.dotenv

val dotenv = dotenv()
val apiKey = dotenv["HANGAR_TOKEN"]
    ?: throw IllegalStateException("HANGAR_TOKEN not found in .env file")
```

## Using the Provider Factory (Recommended)

The simplest way to use the Hangar provider is through `HangarProviderFactory`, which creates both providers for you:

```kotlin
import io.github.beduality.terracotta.provider.hangar.HangarProviderFactory

val factory = HangarProviderFactory()
val stateProvider = factory.createStateProvider(apiKey)
val registryProvider = factory.createRegistryProvider(apiKey)
```

## State Provider

The `HangarStateProvider` fetches the current project state from Hangar. It takes a `HangarClient` instance:

```kotlin
import io.github.beduality.terracotta.provider.hangar.HangarStateProvider
import io.github.beduality.terracotta.provider.hangar.client.HangarClient

val client = HangarClient(apiKey)
val stateProvider = HangarStateProvider(client)
val remoteProject = stateProvider.fetchProject(projectId = "my-plugin-slug")
```

## Registry Provider

The `HangarRegistryProvider` applies operations to Hangar. It also takes a `HangarClient` instance:

```kotlin
import io.github.beduality.terracotta.provider.hangar.HangarRegistryProvider
import io.github.beduality.terracotta.provider.hangar.client.HangarClient
import io.github.beduality.terracotta.provider.hangar.logic.HangarProviderLogic

val client = HangarClient(apiKey)
val registryProvider = HangarRegistryProvider(client, HangarProviderLogic)
registryProvider.apply(projectId = "my-plugin-slug", operations = operations)
```

## Complete Example

Here's a complete example showing how to use the Hangar provider with the Terracotta core:

```kotlin
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.diff.DiffEngine
import io.github.beduality.terracotta.provider.hangar.HangarProviderFactory
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // Load API key from environment variable
    val apiKey = System.getenv("HANGAR_TOKEN")
        ?: throw IllegalStateException("HANGAR_TOKEN environment variable not set")

    // Define your local project state
    val localProject = TerracottaProject(
        id = "my-plugin",
        name = "My Plugin",
        summary = "A great plugin",
        description = "Full description here",
        versions = emptyList(),
        tags = listOf("plugin", "utility"),
        license = "MIT"
    )

    // Create providers via the factory
    val factory = HangarProviderFactory()
    val stateProvider = factory.createStateProvider(apiKey)
    val registryProvider = factory.createRegistryProvider(apiKey)

    // Fetch remote state
    val remoteProject = stateProvider.fetchProject(localProject.id)

    // Calculate differences
    val operations = DiffEngine.diff(localProject, remoteProject)

    // Apply changes
    if (operations.isNotEmpty()) {
        registryProvider.apply(localProject.id, operations)
        println("Applied ${operations.size} changes")
    } else {
        println("No changes needed")
    }
}
```

## Project Creation

Hangar does not expose a project creation API. Projects must be created manually on Hangar first. When Terracotta receives a `CreateProject` operation, it logs a warning and skips the operation. Once the project exists on Hangar, Terracotta can sync metadata and upload versions.

## Loader-to-Platform Mapping

Hangar uses a coarser "platform" concept than individual loaders. Terracotta maps multiple Paper-ecosystem loaders to the same Hangar platform:

| Terracotta Loader | Hangar Platform |
|-------------------|-----------------|
| `bukkit`          | `PAPER`         |
| `spigot`          | `PAPER`         |
| `paper`           | `PAPER`         |
| `purpur`          | `PAPER`         |
| `folia`           | `PAPER`         |
| `velocity`        | `VELOCITY`      |
| `bungeecord`      | `WATERFALL`     |
| `waterfall`       | `WATERFALL`     |

Loaders such as `fabric`, `forge`, `quilt`, `neoforge`, and `sponge` are not supported by Hangar and are skipped with a warning.

## Release Channels

Hangar versions are uploaded to a channel. Terracotta maps release types to Hangar channels:

| Terracotta Release Type | Hangar Channel |
|-------------------------|----------------|
| `release`               | `Release`      |
| `beta`                  | `Snapshot`     |
| `alpha`                 | `Snapshot`     |

If a channel does not exist on the project yet, Terracotta creates it automatically before uploading.

## Error Handling

The providers may throw exceptions for various reasons:

- **Authentication errors**: Invalid or missing API key
- **Network errors**: Connection issues with Hangar API
- **Not found errors**: Project doesn't exist on Hangar (create it manually first)
- **Validation errors**: Invalid project data or unsupported loader/platform combination

Always wrap provider calls in try-catch blocks:

```kotlin
try {
    val project = stateProvider.fetchProject(projectId)
} catch (e: Exception) {
    println("Failed to fetch project: ${e.message}")
}
```

## Rate Limiting

Hangar has rate limits on their API. The provider does not implement automatic retries, so you should be mindful of making too many requests in a short period.
