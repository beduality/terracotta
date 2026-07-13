# Provider Logic

Terracotta keeps project data in a canonical model that is independent of any registry. Each registry still has its own rules for loaders, game versions, and supported operations. The provider logic layer captures those rules so they can be tested and reused without duplicating them inside HTTP clients or registry providers.

## What belongs in provider logic

Provider logic has two responsibilities:

- **Loader mapping**: translate Terracotta loader IDs into the names the registry understands. Modrinth uses the same slugs, while Hangar groups Paper-family loaders into `PAPER`.
- **Platform behavior**: describe how the registry behaves. A stateful registry can update project metadata; an append-only registry can only upload versions. The behavior also filters out operations the registry does not support.

## Why keep it separate

Without the layer, platform-specific transformations end up scattered across API clients and registry providers. That makes them harder to unit test and harder to keep consistent when both reading and writing state. By exposing `LoaderMapper` and `PlatformBehavior` through `ProviderLogic`, the same rules drive `StateProvider` mappings and `RegistryProvider` filtering.

Core also provides a `BaseRegistryProvider` that handles skipped-operation detection and logging automatically. Providers extend it, pass their `ProviderLogic` and platform id, and implement only `applySupported` — the base class owns the logger and emits generic warnings for any operations the platform behavior filters out.

## Example: Modrinth

Modrinth is stateful and uses loader slugs directly. Its logic layer is small:

```kotlin
object ModrinthProviderLogic : ProviderLogic {
    override val loaderMapper = ModrinthLoaderMapper   // identity
    override val platformBehavior = ModrinthPlatformBehavior // stateful
}
```

Because the platform behavior is stateful, `filterOperations` returns the input unchanged. The registry provider still applies each operation directly.

## Example: Hangar

Hangar is stateful but does not support project creation, gallery images, or icon uploads. The platform behavior filters those operations out before the registry provider sees them:

```kotlin
object HangarPlatformBehavior : PlatformBehavior {
    override val isStateful = true

    override fun filterOperations(operations: List<Operation>): List<Operation> =
        operations.filter {
            it is UpdateMetadata ||
                it is UpdateDescription ||
                it is UpdateCategories ||
                it is UploadVersion
        }
}
```

The same `HangarLoaderMapper` is also exposed as the `LoaderMapper` so that `HangarClient` uses the same mapping when uploading versions.

## Out of scope

Numeric ID resolution, such as CurseForge game version and dependency IDs, is intentionally deferred to the CurseForge provider plan. When that module is added, an `IdResolver` interface may be added to the logic layer without changing the existing Modrinth and Hangar implementations.

## See also

- [Provider Interfaces](../reference/provider-interfaces.md)
- [Implement a Custom Provider](../tutorials/implementing-a-custom-provider.md)
- [Hangar Loader Mapping](../../provider-hangar/explanation/loader-mapping.md)
