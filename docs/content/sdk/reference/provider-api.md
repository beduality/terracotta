# Provider API Reference

This reference covers the provider interfaces that enable Terracotta's modular architecture.

## ProviderFactory

The factory interface for creating state and registry providers for a specific registry.

```kotlin
interface ProviderFactory {
    /** The unique identifier for this registry (e.g. "modrinth") */
    val id: String

    /** Creates a state provider for this registry using the given auth token (may be null) */
    fun createStateProvider(token: String?): StateProvider

    /** Creates a registry provider for this registry using the given auth token (may be null) */
    fun createRegistryProvider(token: String?): RegistryProvider
}
```

### Implementation Requirements

1. Implement all three properties/methods
2. Register via Java ServiceLoader at `META-INF/services/io.github.beduality.terracotta.core.provider.ProviderFactory`
3. Use unique registry ID (e.g., "modrinth", "hangar", "curseforge")

### Example

```kotlin
class MyRegistryProviderFactory : ProviderFactory {
    override val id = "my-registry"
    
    override fun createStateProvider(token: String?): StateProvider {
        return MyRegistryStateProvider(token)
    }
    
    override fun createRegistryProvider(token: String?): RegistryProvider {
        return MyRegistryRegistryProvider(token)
    }
}
```

## StateProvider

Fetches the current remote project state from a registry.

```kotlin
interface StateProvider {
    /**
     * Fetches the current remote state of a project.
     * Returns null if the project does not exist yet.
     */
    suspend fun fetchProject(projectId: String): TerracottaProject?
}
```

### Implementation Requirements

1. Fetch project data from the registry API
2. Translate registry-specific data into `TerracottaProject`
3. Return `null` if project doesn't exist
4. Handle authentication tokens

## RegistryProvider

Applies operations to a registry.

```kotlin
interface RegistryProvider {
    /**
     * Applies the given operations to the remote registry.
     */
    suspend fun apply(
        projectId: String,
        operations: List<Operation>,
    )
}
```

### Implementation Requirements

1. Implement all operation types from `Operation` sealed interface
2. Handle rate limiting and retries
3. Map generic operations to registry-specific API calls
4. Handle authentication tokens

## Operation Types

| Operation | Description |
|-----------|-------------|
| `Operation.UpdateDescription(old, new)` | Update project description |
| `Operation.UpdateTags(oldTags, newTags)` | Update project tags |
| `Operation.UpdateMetadata(...)` | Update project metadata |
| `Operation.UploadVersion(version)` | Upload a new version |
| `Operation.CreateProject(project)` | Create a new project |

## ServiceLoader Registration

To make your provider discoverable, create `META-INF/services/io.github.beduality.terracotta.core.provider.ProviderFactory` containing the fully qualified name of your implementation class:

```
com.example.MyRegistryProviderFactory
```

## Error Handling

- `fetchProject()`: Return `null` for non-existent projects, throw for network/auth errors
- `apply()`: Throw exceptions for failed operations with descriptive messages