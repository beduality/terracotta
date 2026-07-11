# Implement a Custom Provider

In this tutorial you will implement a minimal `ProviderFactory`, `StateProvider`, and `RegistryProvider` for a fake registry called `example`. The provider will not make network calls; it will keep state in memory so you can verify the integration locally.

By the end you will be able to discover the provider through Java's `ServiceLoader` and apply a diff to it.

## Prerequisites

- A local copy of the Terracotta repository.
- JDK 17 or later.
- You have read [Compute a Diff](../how-to-guides/compute-a-diff.md) or understand how `DiffEngine` works.

## Step 1: Create the state provider

Create `modules/terracotta-core/src/test/kotlin/io/github/beduality/terracotta/core/provider/example/ExampleStateProvider.kt`:

```kotlin
package io.github.beduality.terracotta.core.provider.example

import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.provider.StateProvider

class ExampleStateProvider : StateProvider {
    private val projects = mutableMapOf<String, TerracottaProject>()

    fun seed(projectId: String, project: TerracottaProject) {
        projects[projectId] = project
    }

    override suspend fun fetchProject(projectId: String): TerracottaProject? = projects[projectId]
}
```

## Step 2: Create the registry provider

Create `ExampleRegistryProvider.kt` in the same package:

```kotlin
package io.github.beduality.terracotta.core.provider.example

import io.github.beduality.terracotta.core.diff.Operation
import io.github.beduality.terracotta.core.provider.RegistryProvider

class ExampleRegistryProvider(private val state: ExampleStateProvider) : RegistryProvider {
    private val log = mutableListOf<String>()

    override suspend fun apply(projectId: String, operations: List<Operation>) {
        operations.forEach { log.add(it.description) }
    }

    fun applied(): List<String> = log
}
```

## Step 3: Create the factory

Create `ExampleProviderFactory.kt`:

```kotlin
package io.github.beduality.terracotta.core.provider.example

import io.github.beduality.terracotta.core.provider.ProviderFactory
import io.github.beduality.terracotta.core.provider.RegistryProvider
import io.github.beduality.terracotta.core.provider.StateProvider

class ExampleProviderFactory : ProviderFactory {
    override val id: String = "example"

    override fun createStateProvider(token: String?): StateProvider = ExampleStateProvider()

    override fun createRegistryProvider(token: String?): RegistryProvider {
        throw IllegalStateException("Use createStateProvider and ExampleRegistryProvider(state) directly in tests")
    }
}
```

## Step 4: Write a test

Create `ExampleProviderIntegrationTest.kt`:

```kotlin
package io.github.beduality.terracotta.core.provider.example

import io.github.beduality.terracotta.core.diff.DiffEngine
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.TerracottaVersion
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExampleProviderIntegrationTest {
    @Test
    fun `creates project and uploads version`() = runBlocking {
        val local = TerracottaProject(
            id = "example-project",
            name = "Example Project",
            summary = "A test project",
            description = "A longer description",
            versions = listOf(
                TerracottaVersion(
                    version = "1.0.0",
                    artifactPath = "/tmp/example.jar",
                    gameVersions = listOf("1.21.1"),
                ),
            ),
            tags = listOf("test"),
            license = "MIT",
        )

        val state = ExampleStateProvider()
        val registry = ExampleRegistryProvider(state)
        val operations = DiffEngine.diff(local, null)

        registry.apply("example-project", operations)

        assertEquals(2, registry.applied().size)
        assertEquals("+ Create project Example Project", registry.applied()[0])
        assertEquals("+ Upload version 1.0.0", registry.applied()[1])
    }
}
```

## Step 5: Run the test

```bash
./gradlew :terracotta-core:test --tests "io.github.beduality.terracotta.core.provider.example.ExampleProviderIntegrationTest"
```

The test should pass.

## Final result

You now have a minimal provider that:

- Implements the three core provider interfaces.
- Receives operations computed by `DiffEngine`.
- Can be discovered and wired into the Gradle plugin via `ServiceLoader`.

## Production checklist

When you move from the in-memory example to a real registry, keep these points in mind:

- **Authentication**: accept the token in `ProviderFactory.createStateProvider` and `createRegistryProvider` and pass it to your HTTP client.
- **Error handling**: throw descriptive exceptions for network, auth, and validation failures. Return `null` from `StateProvider.fetchProject` only when the project does not exist.
- **Rate limiting**: add retries or back-off for registry rate limits.
- **Model mapping**: translate registry-specific responses into `TerracottaProject` and `TerracottaVersion`, and handle each `Operation` subtype in `RegistryProvider.apply`.
- **ServiceLoader registration**: create `META-INF/services/io.github.beduality.terracotta.core.provider.ProviderFactory` containing the fully qualified name of your factory.

## Next steps

- Read the [Provider Interfaces](../reference/provider-interfaces.md) reference for the interface signatures and discovery rules.
- Review the [Architecture](../explanation/architecture.md) explanation to understand why providers are decoupled from the Gradle plugin.
