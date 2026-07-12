# Using the Modrinth Provider

The `terracotta-provider-modrinth` module provides a complete implementation of the Terracotta provider interfaces for the Modrinth registry.

!!! tip "Quick start"

    If you are using the Terracotta SDK directly, the shortest path is:

    1. Add `terracotta-core` and `terracotta-provider-modrinth` to your project.
    2. Load `MODRINTH_TOKEN` from the environment.
    3. Create a `ModrinthProviderFactory` and build a local `TerracottaProject`.
    4. Run `DiffEngine.diff(local, remote)` and `registryProvider.apply(id, operations)`.

    See the sections below for the full walkthrough.

## Installation

Add the dependency to your project:

=== "Gradle (Kotlin)"

    ```kotlin
    implementation("io.github.beduality:terracotta-provider-modrinth:0.2.0")
    ```

=== "Maven"

    ```xml
    <dependency>
        <groupId>io.github.beduality</groupId>
        <artifactId>terracotta-provider-modrinth</artifactId>
        <version>0.2.0</version>
    </dependency>
    ```

## Authentication

The Modrinth provider requires an API token for authentication. You can obtain one from [Modrinth's settings](https://modrinth.com/settings/tokens).

**Important**: Never hardcode API tokens in your source code. Always load them from environment variables or configuration files.

### Loading from Environment Variables

```kotlin
val token = System.getenv("MODRINTH_TOKEN")
    ?: throw IllegalStateException("MODRINTH_TOKEN environment variable not set")
```

### Loading from .env File (Recommended for Development)

Add the [dotenv-kotlin](https://github.com/soywod/kotlin-dotenv) library to your project:

```kotlin
implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
```

Create a `.env` file in your project root (add it to `.gitignore`):

```env
MODRINTH_TOKEN=your-token-here
```

Load the token:

```kotlin
import io.github.cdimascio.dotenv.dotenv

val dotenv = dotenv()
val token = dotenv["MODRINTH_TOKEN"]
    ?: throw IllegalStateException("MODRINTH_TOKEN not found in .env file")
```

### Loading from Configuration Files

For production applications, use your application's configuration system:

```kotlin
// Using HOCON typesafe-config
val config = ConfigFactory.load()
val token = config.getString("modrinth.token")
```

## Using the Provider Factory (Recommended)

The simplest way to use the Modrinth provider is through `ModrinthProviderFactory`, which creates both providers for you:

```kotlin
import io.github.beduality.terracotta.provider.modrinth.ModrinthProviderFactory

val factory = ModrinthProviderFactory()
val stateProvider = factory.createStateProvider(token)
val registryProvider = factory.createRegistryProvider(token)
```

## State Provider

The `ModrinthStateProvider` fetches the current project state from Modrinth. It takes a `ModrinthClient` instance:

```kotlin
import io.github.beduality.terracotta.provider.modrinth.ModrinthStateProvider
import io.github.beduality.terracotta.provider.modrinth.client.ModrinthClient

val client = ModrinthClient(token)
val stateProvider = ModrinthStateProvider(client)
val remoteProject = stateProvider.fetchProject(projectId = "my-plugin-id")
```

## Registry Provider

The `ModrinthRegistryProvider` applies operations to Modrinth. It also takes a `ModrinthClient` instance:

```kotlin
import io.github.beduality.terracotta.provider.modrinth.ModrinthRegistryProvider
import io.github.beduality.terracotta.provider.modrinth.client.ModrinthClient
import io.github.beduality.terracotta.provider.modrinth.logic.ModrinthProviderLogic

val client = ModrinthClient(token)
val registryProvider = ModrinthRegistryProvider(client, ModrinthProviderLogic)
registryProvider.apply(projectId = "my-plugin-id", operations = operations)
```

## Gallery images

The Modrinth provider supports uploading, updating metadata, and deleting gallery images.
Images are declared in project configuration (for example, `terracotta.yml`) or through a build-tool DSL and matched by their title.

### Supported formats and limits

| Limit | Value |
|-------|-------|
| Maximum file size | 5 MiB |
| Supported extensions | `png`, `jpg`, `jpeg`, `webp`, `gif`, `bmp` |
| Identity key | Normalized title (trimmed and lowercased) |

### Configuration example

```yaml
name: "My Plugin"
# ... other fields ...

gallery:
  - path: "docs/assets/main.png"
    title: "Main inventory screen"
    description: "Shows the new GUI"
    featured: true
    ordering: 0
```

When `terracottaApply` runs, the provider uploads new images, updates metadata for
images whose title/description/featured/ordering changed, and deletes images whose
title no longer exists in the local configuration.

## Project icon

The Modrinth provider can upload and update the project icon. The icon is declared with the `icon` field in project configuration (for example, `terracotta.yml`) or through a build-tool DSL.

### Supported formats and limits

| Limit | Value |
|-------|-------|
| Maximum file size | 256 KiB |
| Supported extensions | `png`, `jpg`, `jpeg`, `webp`, `gif`, `bmp` |

### Configuration example

```yaml
name: "My Plugin"
icon: "docs/assets/icon.png"
# ... other fields ...
```

When the local icon is configured and the remote project has no icon, the provider
uploads it using `PATCH /project/{id}/icon`. When the remote project already has an
icon, the provider replaces it. Modrinth does not expose an API for deleting the
icon, so `DeleteIcon` operations are skipped with a warning.

## Complete Example

Here's a complete example showing how to use the Modrinth provider with the Terracotta core:

```kotlin
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.diff.DiffEngine
import io.github.beduality.terracotta.provider.modrinth.ModrinthProviderFactory
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // Load token from environment variable
    val token = System.getenv("MODRINTH_TOKEN")
        ?: throw IllegalStateException("MODRINTH_TOKEN environment variable not set")
    
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
    val factory = ModrinthProviderFactory()
    val stateProvider = factory.createStateProvider(token)
    val registryProvider = factory.createRegistryProvider(token)
    
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

## Error Handling

The providers may throw exceptions for various reasons:

- **Authentication errors**: Invalid or missing API token
- **Network errors**: Connection issues with Modrinth API
- **Not found errors**: Project doesn't exist on Modrinth
- **Validation errors**: Invalid project data

Always wrap provider calls in try-catch blocks:

```kotlin
try {
    val project = stateProvider.fetchProject(projectId)
} catch (e: Exception) {
    println("Failed to fetch project: ${e.message}")
}
```

## Rate Limiting

Modrinth has rate limits on their API. The provider handles retry logic automatically, but you should be mindful of making too many requests in a short period.
