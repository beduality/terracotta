---
description: Allow any Terracotta config value to be loaded from an external file using a `<field>Path` key convention.
---

# File-Backed Config Fields via `<field>Path`

## TL;DR

Introduce a convention where any scalar or list field in `terracotta.yml` can be supplied from an external file by appending `Path` to the field name (e.g. `descriptionPath: docs/description.md`).
This keeps long or generated values out of the main config file while preserving a single source of truth.

## Problem Statement

`terracotta.yml` is designed to be compact, but several real-world values do not fit comfortably in a single YAML document:

- **Long descriptions** are hard to review and edit when embedded as a folded YAML block.
- **Changelogs** are often maintained in a separate file (e.g. `CHANGELOG.md`) and copied into `terracotta.yml` by hand.
- **Generated lists** such as supported game versions or tags are produced by other tools and must be manually merged into YAML.
- **Shared metadata** (e.g. a project summary reused across multiple projects) cannot be referenced from a common file.

Today users must either inline these values or rely on build-tool logic (Gradle DSL) to override them, which pushes project metadata out of `terracotta.yml` and into `build.gradle.kts`.

## Goals

- Support `<field>Path` keys for every scalar and list field in `TerracottaConfig` and its nested models.
- Resolve relative paths against the directory containing `terracotta.yml`.
- Produce clear, actionable errors when a referenced file is missing or unreadable.
- Keep the change backward-compatible: existing `terracotta.yml` files continue to work unchanged.
- Make the resolution rules testable and reusable outside the Gradle plugin.

## Non-Goals

- Loading non-textual assets (e.g. icon images) via `*Path` keys; `icon` remains a direct file reference.
- Recursive `*Path` files (a file loaded via `*Path` cannot itself contain another `*Path` reference).
- Inline environment variable interpolation; that is covered by [External YAML Configuration](./2025-07-external-yaml-config.md).
- Changing the Gradle DSL; this proposal focuses on YAML loading only.

## Proposed Design

### Core resolver in `terracotta-core`

Add a `PathBackedValueResolver` that post-processes the raw YAML map before it is mapped to `TerracottaConfig`.
The resolver is base-directory aware so that relative paths behave predictably regardless of where the build is invoked.

```kotlin
internal class PathBackedValueResolver(
    private val baseDir: File,
) {
    fun resolveString(map: MutableMap<String, Any?>, field: String): String? {
        val direct = map.readString(field)
        val path = map.readString("${field}Path") ?: return direct
        val content = resolveFile(path) ?: return direct
        return content
    }

    fun resolveStringList(map: MutableMap<String, Any?>, field: ListField): List<String>? {
        val direct = map.readStringList(field.name)
        val path = map.readString("${field.name}Path") ?: return direct
        val content = resolveFile(path) ?: return direct
        return content.lines().filter { it.isNotBlank() }
    }

    private fun resolveFile(path: String): String? {
        val file = File(path).let { if (it.isAbsolute) it else File(baseDir, path) }
        if (!file.exists()) {
            throw ConfigLoadException("Referenced file does not exist: ${file.absolutePath}")
        }
        return file.readText(Charsets.UTF_8).trimEnd()
    }
}
```

### Field coverage

The first implementation covers all string and list-of-string fields currently defined in the schema:

| Field | Type | Example `*Path` key |
|-------|------|-------------------|
| `name` | `String?` | `namePath` |
| `summary` | `String?` | `summaryPath` |
| `description` | `String?` | `descriptionPath` |
| `license` | `String?` | `licensePath` |
| `licenseUrl` | `String?` | `licenseUrlPath` |
| `icon` | `String?` | `iconPath` (loads the path string, not the image bytes) |
| `gameVersions` | `List<String>?` | `gameVersionsPath` |
| `loaders` | `List<String>?` | `loadersPath` |
| `environment` | `String?` | `environmentPath` |
| `releaseType` | `String?` | `releaseTypePath` |
| `changelog` | `String?` | `changelogPath` |

Nested structures receive the same treatment:

- `links.homepage`, `links.source`, `links.issues`, `links.wiki`, `links.community`
- `links.donations[].platform` and `links.donations[].url`
- `gallery[].title` and `gallery[].description` (note: `gallery[].path` remains the image file reference)

### Precedence rules

When both a direct value and a `*Path` value are present, the `*Path` value takes precedence.
This is consistent with the idea that the explicit `Path` key is the user's deliberate choice to externalize the value.

```yaml
# direct value wins against missing Path
description: Inline description

# Path wins against direct value
description: This is ignored
descriptionPath: docs/description.md
```

### List file format

Files referenced by `*Path` keys for list fields contain one entry per line.
Blank lines are ignored, which makes it natural to keep a trailing newline.

```text
1.21
1.20.6
1.20.4
```

```yaml
gameVersionsPath: docs/game-versions.txt
```

## API Sketch

No public API changes are required for end users; the feature is expressed entirely through YAML keys.
Internally, `TerracottaConfigLoader.load(file: File)` continues to be the entry point.
The loader will instantiate `PathBackedValueResolver(baseDir = file.parentFile)` and use it during parsing.

```kotlin
object TerracottaConfigLoader {
    fun load(file: File): TerracottaConfig {
        if (!file.exists()) return TerracottaConfig()
        val map = Yaml().load(file.inputStream()) as? Map<String, Any?> ?: emptyMap()
        val resolver = PathBackedValueResolver(file.parentFile)
        return parse(map, resolver)
    }
}
```

A new public exception type may be introduced for load-time failures:

```kotlin
class ConfigLoadException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
```

## Testing Strategy

- **Unit tests in `TerracottaConfigLoaderTest`**:
  - `descriptionPath` loads the file contents and overrides an inline `description`.
  - Missing `descriptionPath` file throws `ConfigLoadException` with the absolute file path in the message.
  - `gameVersionsPath` parses a newline-delimited file into a list.
  - Relative `*Path` values resolve against the `terracotta.yml` parent directory.
  - Absolute `*Path` values resolve directly.
  - Blank lines in list files are ignored.
- **Nested model tests**:
  - `links.homepagePath` resolves inside the `links` block.
  - `gallery[].descriptionPath` resolves for each gallery item.
- **Regression tests**:
  - Existing `terracotta.yml` files without any `*Path` keys produce identical `TerracottaConfig` objects.
  - Empty list files yield empty lists, not `null`.

## Documentation Updates

- Update `docs/content/modules/core/reference/config-schema.md` to document the `<field>Path` convention and the list file format.
- Add a how-to guide at `docs/content/modules/core/how-to-guides/load-values-from-files.md` with examples for `descriptionPath`, `changelogPath`, and `gameVersionsPath`.
- Mention the feature in `docs/index.md` under the core capabilities section.
- Update `CHANGELOG.md` under `[Unreleased]` when implemented.

## Open Questions

1. Should files referenced by `*Path` keys be trimmed of surrounding whitespace, or should leading/trailing whitespace be preserved exactly?
2. Should list files support comma-separated values or comments (e.g. lines starting with `#`)?
3. Should the `*Path` mechanism also cover provider-specific configuration (`providers.<id>.projectIdPath`, `providers.<id>.tokenPath`) to support secrets in separate files?
4. Is it acceptable for `*Path` keys to take precedence over direct values, or should the two forms be mutually exclusive?

## Risks

- **Risk**: A typo in a `*Path` key silently does nothing because the unknown key is ignored.
  **Mitigation**: Add validation (see [Config Validation](./2025-07-config-validation.md)) that warns when a `*Path` key has no corresponding base field.

- **Risk**: Reading arbitrary files during config loading could be a security concern in shared CI environments.
  **Mitigation**: Restrict resolution to files inside the project directory; reject absolute paths that escape the project root, or emit a warning and require an explicit allowlist.

- **Risk**: Large files could cause memory pressure if loaded eagerly.
  **Mitigation**: Apply a reasonable size limit (e.g. 1 MiB) and fail fast with a clear message; this is appropriate for metadata values.

