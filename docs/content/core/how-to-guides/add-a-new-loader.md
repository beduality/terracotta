# Add a New Loader

Register a custom `TerracottaLoader` at runtime without modifying `terracotta-core`.

## Preconditions

- `terracotta-core` is on the classpath.
- The loader implementation class is available.

## Steps

1. Implement `TerracottaLoader`.
2. Call `TerracottaLoaderRegistry.register(loader)` before detection runs.

```kotlin
import io.github.beduality.terracotta.core.model.AbstractTerracottaLoader
import io.github.beduality.terracotta.core.model.TerracottaLoaderRegistry
import io.github.beduality.terracotta.core.model.projectfile.ProjectFileCache

class CustomLoader : AbstractTerracottaLoader("custom", "Custom") {
    override fun detect(cache: ProjectFileCache): Boolean {
        return cache.read("custom.mod.json") != null
    }
}

TerracottaLoaderRegistry.register(CustomLoader())
```

## Outcome

The loader is now considered during `TerracottaLoaderRegistry.detectAll(cache)`.

## Variants

- For loaders that extend an existing platform, set the `parent` loader in the constructor.

```kotlin
class CustomPaperFork : AbstractTerracottaLoader("custom-paper", "Custom Paper", PaperLoader())
```

## See also

- [Implement a Custom Loader](../tutorials/implementing-a-custom-loader.md)
- [Loader Hierarchy](../explanation/loader-hierarchy.md)
