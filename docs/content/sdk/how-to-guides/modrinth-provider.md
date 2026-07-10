# Using the Modrinth Provider

The `terracotta-provider-modrinth` module provides a complete implementation of the Terracotta provider interfaces for the Modrinth registry.

## Installation

Add the dependency to your project:

=== "Gradle (Kotlin)"

    ```kotlin
    implementation("io.github.beduality:terracotta-provider-modrinth:0.1.1")
    ```

=== "Maven"

    ```xml
    <dependency>
        <groupId>io.github.beduality</groupId>
        <artifactId>terracotta-provider-modrinth</artifactId>
        <version>0.1.1</version>
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

val client = ModrinthClient(token)
val registryProvider = ModrinthRegistryProvider(client)
registryProvider.apply(projectId = "my-plugin-id", operations = operations)
```

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
