# Resolve Project Metadata

Resolve the effective metadata for a project directory by merging explicit configuration, detected values, and defaults.

## Preconditions

- A `TerracottaConfig` has been loaded.
- A `ProjectMetadataSource` supplies build-system values such as the project version.

## Steps

1. Create a `ProjectMetadataResolver` with the project directory, config, and source.
2. Call `resolve()`.

```kotlin
import io.github.beduality.terracotta.core.config.ProjectMetadataResolver
import io.github.beduality.terracotta.core.config.TerracottaConfigLoader
import io.github.beduality.terracotta.core.model.metadata.ProjectMetadataSource
import java.io.File

val config = TerracottaConfigLoader.load(File(projectDir, "terracotta.yml"))
val source = ProjectMetadataSource(
    name = "My Plugin",
    summary = "A useful plugin",
    version = "1.2.3",
)
val resolver = ProjectMetadataResolver(projectDir, config, source)
val metadata = resolver.resolve()
```

## Steps for changelog extraction

If the changelog should come from the changelog file for the current version:

```kotlin
val changelog = resolver.detectChangelog(metadata.changelog)
```

This returns the release notes for the resolved version, or `null` if no section is found.

## Outcome

You have a `ResolvedProjectMetadata` instance containing the merged values.

## Variants

- If you only need auto-detected metadata without explicit configuration, use `ProjectMetadataLoader.load(cache, source)` directly.

## See also

- [Metadata Resolution Precedence](../reference/metadata-resolution.md)
- [Metadata Resolution Explanation](../explanation/metadata-resolution.md)
