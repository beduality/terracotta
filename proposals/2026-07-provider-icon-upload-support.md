---
description: Add a project icon field and provider API for uploading project icons alongside existing metadata and gallery support.
---

# Provider Icon Upload Support

## TL;DR

Terracotta projects currently ship with text metadata and gallery images, but have no first-class way to manage the project icon that appears on registry listings. This proposal adds an `icon` field to `TerracottaProject`, introduces icon upload/update operations, and implements them in the Modrinth and Hangar providers.

## Problem Statement

Project icons are one of the most visible pieces of a registry listing, yet Terracotta has no way to model or upload them:

- `TerracottaProject` has no `icon` field, so users cannot declare an icon in `terracotta.yml`.
- Modrinth supports a project icon via `icon_url`, but the Modrinth provider does not upload or update it.
- Hangar has a project avatar/resource path, but the Hangar provider ignores icons entirely.
- The GitHub provider (per its own proposal) intentionally skips concepts that do not map cleanly; repository avatars are out of scope.
- Users must manually upload icons after each publish run, which is error-prone and breaks the "configure once, publish repeatedly" workflow.

## Goals

- Add a stable, provider-agnostic `icon` field to `TerracottaProject` and `TerracottaConfig`.
- Introduce `UploadIcon` and `UpdateIcon` operations in `terracotta-core`.
- Implement icon upload for the Modrinth provider.
- Implement icon upload for the Hangar provider where its API supports it.
- Reuse the existing asset processing pipeline (`AssetProcessor`, `GalleryValidator`) so icons and gallery images share validation rules.
- Update the Gradle DSL and YAML config so users can declare an icon path.

## Non-Goals

- Adding icon support to the GitHub provider; GitHub repository avatars are not a Terracotta project icon concept.
- Automatic icon generation or image editing (resize, crop, format conversion) beyond the existing asset processor.
- Icon support for providers that do not yet exist (e.g., CurseForge).
- Changing the existing gallery image behavior or replacing gallery items with icons.

## Proposed Design

### Core Model and Config

Add an `icon` field to the project model and config. The value is a local file path in config, and may become a remote URL when fetched from a provider state provider.

**`TerracottaProject`**:
```kotlin
data class TerracottaProject(
    // ... existing fields ...
    /** Project icon: local path in config, remote URL when read from provider state. */
    val icon: String? = null,
    // ...
)
```

**`TerracottaConfig`**:
```kotlin
data class TerracottaConfig(
    // ... existing fields ...
    /** Path to the project icon file. */
    val icon: String? = null,
    // ...
)
```

### Operations

Add two operations to the `Operation` sealed interface in `terracotta-core`:

```kotlin
/** Uploads [iconPath] as the project icon. */
data class UploadIcon(val iconPath: String) : Operation {
    override val description: String = "+ Upload project icon"
}

/** Replaces the existing project icon with [iconPath]. */
data class UpdateIcon(
    val oldIconUrl: String?,
    val iconPath: String,
) : Operation {
    override val description: String = "~ Update project icon"
}
```

Providers that cannot distinguish "create" from "update" may treat both as an upsert.

### Diff Engine

Extend the diff engine in `terracotta-core` to compare the configured `icon` path with the remote icon URL:

- If the local config declares an icon and the remote project has no icon, produce `UploadIcon`.
- If the local config declares an icon and the remote icon differs, produce `UpdateIcon`.
- If the local config removes an icon and the remote has one, produce `DeleteIcon` (see API sketch).

Because the remote icon is a URL while the local icon is a file path, the diff compares "has any icon" rather than file contents. A checksum comparison can be added later if providers expose one.

### Asset Validation

Icons reuse the same validation used for gallery images:

| Rule | Value | Rationale |
|---|---|---|
| Supported extensions | `png`, `jpg`, `jpeg`, `webp`, `gif`, `bmp` | Matches Modrinth gallery limits |
| Maximum size | 5 MB | Avoids oversized uploads and matches gallery limit |

The `GalleryValidator` can be renamed or generalized to `ImageAssetValidator` if the team prefers a single shared component.

### Modrinth Provider

Modrinth exposes a project icon via `PATCH /project/{id}` with an `icon` multipart file field, or by setting `icon_url` to a remote URL. The provider will:

1. Read the existing icon from `ModrinthProject.icon_url` in the state provider.
2. On `UploadIcon` or `UpdateIcon`, call `PATCH /project/{id}` with the processed icon file as multipart data.
3. Leave `icon_url` untouched if no icon is configured.

### Hangar Provider

Hangar supports project resource/avatar uploads. The Hangar provider will:

1. Read the current avatar URL from the Hangar project response when available.
2. On `UploadIcon` or `UpdateIcon`, upload the file to the project avatar endpoint if the API supports it.
3. If the Hangar API does not expose a programmatic avatar upload, warn once and skip the operation.

### GitHub Provider

The GitHub provider intentionally skips icon operations. Repository avatars are a GitHub-native concept that does not map to a Terracotta project icon. The provider will log a single warning if an icon is configured.

## API Sketch

```kotlin
// New operations in terracotta-core
data class UploadIcon(val iconPath: String) : Operation

data class UpdateIcon(
    val oldIconUrl: String?,
    val iconPath: String,
) : Operation

data class DeleteIcon(val oldIconUrl: String) : Operation

// Extended model in terracotta-core
data class TerracottaProject(
    // ... existing fields ...
    val icon: String? = null,
)

// Modrinth client extension
class ModrinthClient {
    suspend fun uploadIcon(
        projectId: String,
        iconPath: String,
    )
}
```

**YAML configuration**:
```yaml
project:
  name: "Terracotta"
  summary: "A Minecraft mod publisher."
  icon: "docs/assets/icon.png"
  gallery:
    - imagePath: "docs/assets/screenshot.png"
      title: "Main screen"
```

**Gradle DSL**:
```kotlin
terracotta {
    project {
        icon.set("docs/assets/icon.png")
    }
}
```

## Testing Strategy

- **Unit tests** in `terracotta-core`: verify the diff engine produces the correct icon operation when the configured icon changes, appears, or disappears.
- **Unit tests** in `terracotta-provider-modrinth`: verify the client calls `PATCH /project/{id}` with the icon multipart body and the correct content type.
- **Unit tests** in `terracotta-provider-hangar`: verify the provider warns and skips when the avatar API is unavailable, or uploads when it is available.
- **Integration tests**: run a `terracottaApply` plan against a test Modrinth project and confirm the icon operation is included in the plan output.
- **Manual verification**: configure a local `terracotta.yml` with an icon, run `terracottaApply`, and verify the icon appears on the Modrinth project page.

## Documentation Updates

- Update the config schema reference (`docs/content/modules/core/reference/config-schema.md`) to document the `icon` field.
- Update the models reference (`docs/content/modules/core/reference/models.md`) to describe `TerracottaProject.icon`.
- Update the Modrinth provider tutorial to mention icon upload support.
- Update the Gradle plugin how-to guide (`docs/content/modules/gradle-plugin/how-to-guides/kotlin-dsl-configuration.md`) with the `icon.set(...)` DSL example.
- Add a note to the GitHub provider documentation explaining that project icons are not synced to GitHub.

## Open Questions

1. Should the diff engine compare icon file contents (e.g., SHA-256) to avoid re-uploading identical icons, or is "local icon configured vs remote icon absent/different" sufficient?
2. Should icons support remote URLs in config (e.g., `icon: "https://example.com/icon.png"`) so users can reference an existing CDN image instead of a local file?
3. Should the validator allow SVG files, or restrict to raster formats to match provider limitations?

## Risks

- **Provider API differences**: Modrinth and Hangar handle icons differently. A generic `icon` field may not map cleanly to every future provider.
  - **Mitigation**: Keep the model simple (a single optional path/URL) and let each provider decide how to translate it. Document unsupported providers clearly.

- **Remote icon URLs are not stable**: The diff engine compares local paths against remote URLs, so re-running the same config may produce spurious `UpdateIcon` operations if the provider regenerates URLs.
  - **Mitigation**: Treat the icon diff as presence-based by default, and add optional checksum comparison later behind a flag.

- **Image validation drift**: If one provider supports SVG and another does not, the shared validator may reject valid files for one provider.
  - **Mitigation**: Use the most restrictive common denominator (raster formats) as the default, and allow provider-specific validators to override for their own formats.

- **Accidental icon overwrites**: Uploading a new icon replaces the previous one immediately on the registry.
  - **Mitigation**: Icon operations appear in the plan output, so users can review them before `terracottaApply` runs. Keep the operation explicit and opt-in via configuration.
