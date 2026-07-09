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

After adding the dependency, you can use the Terracotta SDK in your code:

```kotlin
import io.github.beduality.terracotta.core.*
import io.github.beduality.terracotta.provider.modrinth.*

// Create a state provider
val stateProvider = ModrinthStateProvider(token = "your-token")

// Create a registry provider
val registryProvider = ModrinthRegistryProvider(token = "your-token")

// Use the diff engine
val diffEngine = DiffEngine()
val operations = diffEngine.calculate(localState, remoteState)
```

See the [reference documentation](../reference/) for detailed API documentation.
