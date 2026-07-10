# Installing the Terracotta SDK

This page describes how to add the Terracotta SDK to your project as a dependency.

=== "Maven"

    Add the dependency to your `pom.xml`:

    ```xml
    <dependency>
        <groupId>io.github.beduality</groupId>
        <artifactId>terracotta-core</artifactId>
        <version>0.1.0</version>
    </dependency>
    ```

=== "Gradle (Kotlin)"

    Add the dependency to your `build.gradle.kts`:

    ```kotlin
    implementation("io.github.beduality:terracotta-core:0.1.0")
    ```

=== "Gradle (Groovy)"

    Add the dependency to your `build.gradle`:

    ```groovy
    implementation 'io.github.beduality:terracotta-core:0.1.0'
    ```

## Provider Modules

If you need specific provider implementations, you can also add them as dependencies:

### Modrinth Provider

=== "Gradle (Kotlin)"

    ```kotlin
    implementation("io.github.beduality:terracotta-provider-modrinth:0.1.0")
    ```

=== "Maven"

    ```xml
    <dependency>
        <groupId>io.github.beduality</groupId>
        <artifactId>terracotta-provider-modrinth</artifactId>
        <version>0.1.0</version>
    </dependency>
    ```

## Usage

After adding the dependencies, you can use the Terracotta SDK in your code:

```kotlin
import io.github.beduality.terracotta.core.diff.DiffEngine
import io.github.beduality.terracotta.provider.modrinth.ModrinthProviderFactory
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val token = System.getenv("MODRINTH_TOKEN")

    // Create providers via the factory
    val factory = ModrinthProviderFactory()
    val stateProvider = factory.createStateProvider(token)
    val registryProvider = factory.createRegistryProvider(token)

    // Fetch remote state and compute diff
    val remoteProject = stateProvider.fetchProject("my-project-id")
    val operations = DiffEngine.diff(localProject, remoteProject)

    // Apply changes
    if (operations.isNotEmpty()) {
        registryProvider.apply("my-project-id", operations)
    }
}
```

See the [reference documentation](../reference/) for detailed API documentation.
