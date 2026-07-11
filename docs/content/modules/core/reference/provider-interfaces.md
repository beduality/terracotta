# Provider Interfaces

The provider SPI decouples core from any specific registry implementation.

## Interfaces

### ProviderFactory

Creates state and registry providers for a registry.

```kotlin
interface ProviderFactory {
    /** Unique identifier for this registry (e.g. "modrinth"). */
    val id: String

    /** Creates a state provider for this registry. */
    fun createStateProvider(token: String?): StateProvider

    /** Creates a registry provider for this registry. */
    fun createRegistryProvider(token: String?): RegistryProvider
}
```

### StateProvider

Fetches the current remote project state from a registry.

```kotlin
interface StateProvider {
    /**
     * Fetches the remote state of a project.
     * Returns null if the project does not exist yet.
     */
    suspend fun fetchProject(projectId: String): TerracottaProject?
}
```

### RegistryProvider

Applies a list of operations to the remote registry.

```kotlin
interface RegistryProvider {
    /** Applies the given operations to the remote registry. */
    suspend fun apply(
        projectId: String,
        operations: List<Operation>,
    )
}
```

## Discovery

Provider factories are discovered at runtime through Java's `ServiceLoader`. A provider JAR must contain:

```
META-INF/services/io.github.beduality.terracotta.core.provider.ProviderFactory
```

with the fully qualified name of the factory implementation.

## Implementation requirements

- `ProviderFactory.id` must be unique across all providers.
- `StateProvider.fetchProject` returns `null` when the project does not exist.
- `RegistryProvider.apply` translates generic `Operation` objects into registry-specific API calls.

## See also

- [Implement a Custom Provider](../tutorials/implementing-a-custom-provider.md)
- [API Documentation](api.md)
