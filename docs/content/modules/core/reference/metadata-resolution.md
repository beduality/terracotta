# Metadata Resolution

This page describes how `ProjectMetadataResolver` merges values from multiple sources into `ResolvedProjectMetadata`.

## Resolution precedence

From highest to lowest priority:

1. **Explicit configuration** from `terracotta.yml`.
2. **Detected values** from `ProjectMetadataDetector` implementations.
3. **Build-system source** passed as `ProjectMetadataSource`.
4. **Hard defaults** defined by the resolver.

## Field-specific rules

| Field | Explicit | Detected | Source | Default |
|-------|----------|----------|--------|---------|
| `name` | `config.name` | `detected.name` | `source.name` | `""` |
| `summary` | `config.summary` | `detected.summary` | `source.summary` | `""` |
| `description` | `config.description` | `detected.description` | — | `""` |
| `categories` | `config.categories` | — | — | `TerracottaProjectCategories(...)` |
| `license` | `config.license` | `detected.license` | — | `""` |
| `gameVersions` | `config.gameVersions` | `detected.gameVersions` | — | `emptyList()` |
| `loaders` | `config.loaders` | `detected.loaders` | — | `emptyList()` |
| `environment` | `config.environment` | `detected.environment` | — | `SERVER_ONLY` |
| `releaseType` | `config.releaseType` | `detected.releaseType` | `detectReleaseType(source.version)` | `RELEASE` |
| `visibility` | `config.visibility` | — | — | `PUBLIC` |
| `changelog` | `config.changelog` | Extracted from `CHANGELOG.md` | — | `""` |

## List merging

When two sources provide lists for `gameVersions` or `loaders`, the result is a distinct union.

## Release type detection

If `releaseType` is not configured or detected, the version string from `ProjectMetadataSource` is parsed:

- Pre-release containing `alpha` → `ALPHA`
- Pre-release containing `beta`, `snapshot`, or `rc` → `BETA`
- Otherwise → `RELEASE`

## Version filtering

The build-system version `"unspecified"` is treated as absent.

## See also

- [Resolve Project Metadata](../how-to-guides/resolve-project-metadata.md)
- [Metadata Resolution Explanation](../explanation/metadata-resolution.md)
- [API Documentation](api.md)
