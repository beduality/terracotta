# Provider Interfaces

The provider SPI decouples core from any specific registry implementation.

## Interfaces

| Interface | Responsibility |
|-----------|----------------|
| `ProviderFactory` | Creates state and registry providers for a registry. |
| `StateProvider` | Fetches the remote project state. |
| `RegistryProvider` | Applies a list of operations to the remote registry. |

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
- [SDK Provider API](../../../sdk/reference/provider-api.md)
- [API Documentation](api.md)
