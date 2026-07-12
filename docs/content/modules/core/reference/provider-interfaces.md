# Provider Interfaces

The provider SPI decouples core from any specific registry implementation. It also defines a provider-specific logic layer that captures how a registry transforms Terracotta loaders and operations.

## Interfaces

### ProviderFactory

Creates state and registry providers for a registry, plus the provider-specific logic that drives them.

```kotlin
interface ProviderFactory {
    /** Unique identifier for this registry (e.g. "modrinth"). */
    val id: String

    /** Creates the provider-specific logic for this registry. */
    fun createProviderLogic(): ProviderLogic

    /** Creates a state provider for this registry. */
    fun createStateProvider(token: String?): StateProvider

    /** Creates a registry provider for this registry. */
    fun createRegistryProvider(token: String?): RegistryProvider
}
```

### ProviderLogic

Encapsulates loader mapping and platform behavior for a single registry. Implementations are provider-local and returned by `ProviderFactory.createProviderLogic`.

```kotlin
interface ProviderLogic {
    /** Loader mapping rules for this provider. */
    val loaderMapper: LoaderMapper

    /** Platform behavior rules for this provider. */
    val platformBehavior: PlatformBehavior
}
```

### LoaderMapper

Maps canonical Terracotta loader IDs to provider-specific platform names. Modrinth uses an identity mapping; Hangar groups loaders into `PAPER`, `VELOCITY`, and `WATERFALL`.

```kotlin
interface LoaderMapper {
    fun mapToPlatform(loaderId: String): String?
    fun mapToPlatforms(loaderIds: List<String>): Set<String>
}
```

### PlatformBehavior

Describes whether a registry is stateful and which operations it supports. Stateful registries can update project metadata; append-only registries only upload versions.

```kotlin
interface PlatformBehavior {
    val isStateful: Boolean
    fun filterOperations(operations: List<Operation>): List<Operation>
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

### BaseRegistryProvider

A base class for concrete registry providers. It owns the provider's logger, platform identifier, and skipped-operation logging. It filters incoming operations using the injected `ProviderLogic` and delegates the remaining operations to `applySupported`.

```kotlin
abstract class BaseRegistryProvider(
    providerLogic: ProviderLogic,
    platformId: String,
) : RegistryProvider {
    protected val logger: Logger

    override suspend fun apply(projectId: String, operations: List<Operation>)

    protected abstract suspend fun applySupported(
        projectId: String,
        operations: List<Operation>,
    )
}
```

Most providers should extend `BaseRegistryProvider` instead of implementing `RegistryProvider` directly.

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

- [Provider Logic](../explanation/provider-logic.md)
- [Implement a Custom Provider](../tutorials/implementing-a-custom-provider.md)
- [API Documentation](api.md)
