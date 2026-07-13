# Implement a Custom Provider

In this tutorial you will implement a minimal `ProviderFactory`, `StateProvider`, and `RegistryProvider` for a fake registry called `example`. The provider will not make network calls; it will keep state in memory so you can verify the integration locally.

By the end you will be able to discover the provider through Java's `ServiceLoader` and apply a diff to it.

## Prerequisites

- A local copy of the Terracotta repository.
- JDK 17 or later.
- You have read [Compute a Diff](../how-to-guides/compute-a-diff.md) or understand how `DiffEngine` works.
- You have read the [Provider Logic](../explanation/provider-logic.md) explanation.

## Step 1: Create the state provider

Create `modules/terracotta-core/src/test/kotlin/io/github/beduality/terracotta/core/provider/example/ExampleStateProvider.kt`:

```kotlin
package io.github.beduality.terracotta.core.provider.example

import io.github.beduality.terracotta.core.model.TerracottaCategory
import io.github.beduality.terracotta.core.model.TerracottaProject
import io.github.beduality.terracotta.core.model.TerracottaProjectCategories
import io.github.beduality.terracotta.core.provider.StateProvider

class ExampleStateProvider : StateProvider {
    private val projects = mutableMapOf<String, TerracottaProject>()

    fun seed(projectId: String, project: TerracottaProject) {
        projects[projectId] = project
    }

    override suspend fun fetchProject(projectId: String): TerracottaProject? = projects[projectId]
}
```

## Step 2: Create the provider logic

Create `ExampleProviderLogic.kt` in the same package. The provider logic tells Terracotta how to map loaders and which operations the registry supports. For this in-memory example we use an identity loader mapper and a stateful platform behavior that accepts every operation.

```kotlin
package io.github.beduality.terracotta.core.provider.example

import io.github.beduality.terracotta.core.provider.logic.LoaderMapper
import io.github.beduality.terracotta.core.provider.logic.PlatformBehavior
import io.github.beduality.terracotta.core.provider.logic.ProviderLogic

object ExampleLoaderMapper : LoaderMapper {
    override fun mapToPlatform(loaderId: String): String? = loaderId
}

object ExamplePlatformBehavior : PlatformBehavior {
    override val isStateful: Boolean = true
}

object ExampleProviderLogic : ProviderLogic {
    override val loaderMapper: LoaderMapper = ExampleLoaderMapper
    override val platformBehavior: PlatformBehavior = ExamplePlatformBehavior
}
```

## Step 3: Create the registry provider

Create `ExampleRegistryProvider.kt` in the same package. Extend `BaseRegistryProvider` so core handles filtering, logging, and skipped-operation warnings for you.

```kotlin
package io.github.beduality.terracotta.core.provider.example

import io.github.beduality.terracotta.core.diff.Operation
import io.github.beduality.terracotta.core.provider.BaseRegistryProvider
import io.github.beduality.terracotta.core.provider.logic.ProviderLogic

class ExampleRegistryProvider(
    private val state: ExampleStateProvider,
    providerLogic: ProviderLogic,
) : BaseRegistryProvider(providerLogic, "example") {
    private val log = mutableListOf<String>()

    override suspend fun applySupported(
        projectId: String,
        operations: List<Operation>,
    ) {
        operations.forEach { log.add(it.description) }
    }

    fun applied(): List<String> = log
}
```

## Step 4: Create the factory

Create `ExampleProviderFactory.kt`:

```kotlin
package io.github.beduality.terracotta.core.provider.example

import io.github.beduality.terracotta.core.provider.ProviderFactory
import io.github.beduality.terracotta.core.provider.RegistryProvider
import io.github.beduality.terracotta.core.provider.StateProvider
import io.github.beduality.terracotta.core.provider.logic.ProviderLogic

class ExampleProviderFactory : ProviderFactory {
    override val id: String = "example"

    override fun createProviderLogic(): ProviderLogic = ExampleProviderLogic

    override fun createStateProvider(token: String?): StateProvider = ExampleStateProvider()

    override fun createRegistryProvider(token: String?): RegistryProvider {
        throw IllegalStateException("Use createStateProvider and ExampleRegistryProvider(state, logic) directly in tests")
    }
}
```

## Step 5: Write a test

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
            categories = TerracottaProjectCategories(
                primary = TerracottaCategory("test", "Test"),
            ),
            license = "MIT",
        )

        val state = ExampleStateProvider()
        val registry = ExampleRegistryProvider(state, ExampleProviderLogic)
        val operations = DiffEngine.diff(local, null)

        registry.apply("example-project", operations)

        assertEquals(2, registry.applied().size)
        assertEquals("+ Create project Example Project", registry.applied()[0])
        assertEquals("+ Upload version 1.0.0", registry.applied()[1])
    }
}
```

## Step 6: Run the test

```bash
./gradlew :terracotta-core:test --tests "io.github.beduality.terracotta.core.provider.example.ExampleProviderIntegrationTest"
```

The test should pass.

## Final result

You now have a minimal provider that:

- Implements the four core provider interfaces (`ProviderFactory`, `ProviderLogic`, `StateProvider`, `RegistryProvider` via `BaseRegistryProvider`).
- Uses `ProviderLogic` to map loaders and filter unsupported operations.
- Extends `BaseRegistryProvider` so core handles logging and filtering automatically.
- Receives operations computed by `DiffEngine`.
- Can be discovered by any frontend that loads `ProviderFactory` services, such as the Terracotta Gradle plugin.

## Production checklist

When you move from the in-memory example to a real registry, keep these points in mind:

- **Authentication**: accept the token in `ProviderFactory.createStateProvider` and `createRegistryProvider` and pass it to your HTTP client.
- **Error handling**: throw descriptive exceptions for network, auth, and validation failures. Return `null` from `StateProvider.fetchProject` only when the project does not exist.
- **Rate limiting**: add retries or back-off for registry rate limits.
- **Model mapping**: translate registry-specific responses into `TerracottaProject` and `TerracottaVersion`, and handle each `Operation` subtype in `applySupported`.
- **Provider logic**: decide how your registry maps loaders and which operations it supports. Inject the same `ProviderLogic` instance into both the state provider's loader mapping and the registry provider.
- **BaseRegistryProvider**: extend `BaseRegistryProvider` so core handles filtering, logging, and skipped-operation warnings for you.
- **ServiceLoader registration**: create `META-INF/services/io.github.beduality.terracotta.core.provider.ProviderFactory` containing the fully qualified name of your factory.

## Next steps

- Read the [Provider Interfaces](../reference/provider-interfaces.md) reference for the interface signatures and discovery rules.
- Read the [Provider Logic](../explanation/provider-logic.md) explanation for design rationale and examples.
- Review the [Architecture](../explanation/architecture.md) explanation to understand why providers are decoupled from the Gradle plugin.
