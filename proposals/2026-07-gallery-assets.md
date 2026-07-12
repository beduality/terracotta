---
description: Design proposal for adding gallery image support to Terracotta, with core abstractions and a Modrinth provider implementation.
---

# Gallery Assets Design Proposal

## Problem

Terracotta currently manages project metadata, tags, and version artifacts. It does
not manage visual assets such as project gallery images. Modrinth supports a gallery
of up to 64 images per project, but there is no way to declare, diff, or upload those
images from Terracotta.

## Scope

- Add a provider-agnostic `TerracottaGalleryItem` model in `terracotta-core`.
- Allow users to declare gallery images in `terracotta.yml` and in the Gradle DSL.
- Extend the diff engine so it can plan gallery additions, updates, and deletions.
- Add gallery operations to the `Operation` sealed interface.
- Implement gallery upload/update/delete in the Modrinth provider.
- Make the Hangar provider ignore gallery operations with a warning (Hangar does not
  expose a gallery API).
- Add a core `AssetProcessor` SPI so providers can compress or convert assets before
  upload, with a no-op default.
- Update user documentation and tests.

Out of scope:
- Project icons (separate feature, same API family but different endpoint).
- Provider-specific asset types beyond gallery images.
- Persistent state for gallery identities; identity is derived from declared metadata
  for this version and will be revisited once the state-management system exists.
- Concrete compression/conversion implementations beyond the SPI and no-op default.

## Public API contract

### Core model

```kotlin
package io.github.beduality.terracotta.core.model

/**
 * A single gallery image for a project.
 *
 * @property imagePath Local file path when declared in config, or remote URL when
 *   fetched from a provider.
 * @property title Human-readable title used as the stable identity key.
 * @property description Optional longer description.
 * @property featured Whether the image should be highlighted by the provider.
 * @property ordering Display order; lower values come first.
 */
data class TerracottaGalleryItem(
    val imagePath: String,
    val title: String = "",
    val description: String = "",
    val featured: Boolean = false,
    val ordering: Int = 0,
)
```

`TerracottaProject` gains a new optional field:

```kotlin
val gallery: List<TerracottaGalleryItem> = emptyList(),
```

### Operations

```kotlin
sealed interface Operation {
    // ... existing operations ...

    /** Uploads [item] as a new gallery image. */
    data class UploadGalleryItem(val item: TerracottaGalleryItem) : Operation {
        override val description: String = "+ Upload gallery image '${item.title}'"
    }

    /**
     * Updates an existing gallery image from [oldItem] to [newItem].
     *
     * Providers that cannot update in place may implement this as a delete followed
     * by an upload.
     */
    data class UpdateGalleryItem(
        val oldItem: TerracottaGalleryItem,
        val newItem: TerracottaGalleryItem,
    ) : Operation {
        override val description: String = "~ Update gallery image '${newItem.title}'"
    }

    /** Deletes the gallery image described by [item] from the remote project. */
    data class DeleteGalleryItem(val item: TerracottaGalleryItem) : Operation {
        override val description: String = "- Delete gallery image '${item.title}'"
    }
}
```

### Asset processing SPI

```kotlin
package io.github.beduality.terracotta.core.asset

import java.io.File

/**
 * Transforms a local asset file before it is uploaded to a provider.
 *
 * Implementations may compress, convert, or otherwise prepare the file. The default
 * implementation returns the original file unchanged.
 */
interface AssetProcessor {
    /**
     * Processes [inputFile] and returns a file ready for upload.
     *
     * @return a [ProcessedAsset] describing the file to upload.
     */
    fun process(inputFile: File): ProcessedAsset
}

/**
 * Describes an asset file after processing.
 *
 * @property path Path to the processed file.
 * @property contentType MIME type of the processed file.
 * @property extension File extension to use when uploading.
 */
data class ProcessedAsset(
    val path: String,
    val contentType: String,
    val extension: String,
)

/** No-op [AssetProcessor] that returns the original file. */
object IdentityAssetProcessor : AssetProcessor {
    override fun process(inputFile: File): ProcessedAsset {
        return ProcessedAsset(
            path = inputFile.absolutePath,
            contentType = "application/octet-stream",
            extension = inputFile.extension,
        )
    }
}
```

Providers receive an `AssetProcessor` through their factory or client configuration. The
Modrinth client will call `process(...)` on the local image before building the multipart
body.

### Config schema

New top-level `gallery` key in `terracotta.yml`:

```yaml
gallery:
  - path: "docs/assets/screenshot-main.png"
    title: "Main inventory screen"
    description: "Shows the new GUI"
    featured: true
    ordering: 0
  - path: "docs/assets/screenshot-config.png"
    title: "Configuration UI"
    ordering: 1
```

`path` is required. All other fields are optional and default to the values shown for
`TerracottaGalleryItem`.

### Gradle DSL

```kotlin
terracotta {
    gallery {
        register("mainScreenshot") {
            imageFile.set(file("docs/assets/screenshot-main.png"))
            title.set("Main inventory screen")
            description.set("Shows the new GUI")
            featured.set(true)
            ordering.set(0)
        }
        register("configScreenshot") {
            imageFile.set(file("docs/assets/screenshot-config.png"))
            title.set("Configuration UI")
            ordering.set(1)
        }
    }
}
```

## Diff behavior

The diff engine treats the gallery list as a declarative set. Identity is derived from
the normalized `title` (trimmed, case-insensitive). If a local item and a remote item
share the same title, they are considered the same gallery image.

- If a title is empty, `ordering` is used as the identity key. This is a fallback and
  should be documented as brittle.
- Items present locally but missing remotely produce `UploadGalleryItem`.
- Items present remotely but missing locally produce `DeleteGalleryItem`.
  Deletion is enabled by default because the declarative config is versioned in Git;
  users restore a removed item by restoring the config entry. No extra `--force` flag is
  required.
- Matched items with different metadata produce `UpdateGalleryItem`.

The remote `imagePath` is the provider URL; the local `imagePath` is the file path.
Only metadata is compared during diff; file contents are compared only by provider-side
behavior during upload.

## Provider-specific behavior

### Modrinth

Modrinth exposes:

- `POST /project/{id|slug}/gallery` with query params `ext`, `featured`, `title`,
  `description`, `ordering` and the image bytes as `multipart/form-data`.
- `PATCH /project/{id|slug}/gallery?url=...` with query params for changed fields.
- `DELETE /project/{id|slug}/gallery?url=...`.

Remote gallery items are read from the `gallery` array returned by `GET /project/{id|slug}`.
Each item contains `url`, `title`, `description`, `featured`, `ordering`, and `created`.

Validation lives in `terracotta-core` so all providers share the same file-existence
and format checks. A core `GalleryValidator` enforces:

- File must exist.
- Extension must be one of the supported image formats.
- File size must not exceed a provider-specific limit passed by the caller.

The Modrinth client passes a 5 MiB limit to the validator and accepts `png`, `jpg`,
`jpeg`, `webp`, `gif`, and `bmp`. Before upload it runs the image through the configured
`AssetProcessor`.

The Modrinth provider logs a warning and skips the `featured` flag if more than one
local item is marked featured, because Modrinth only allows one featured image.

### Hangar

Hangar does not expose a gallery API. `HangarRegistryProvider` will catch gallery
operations and emit a single warning:

```
Hangar does not support gallery images; skipping N gallery operation(s).
```

## Internal component boundaries

- `terracotta-core` owns the `TerracottaGalleryItem` model, the new `Operation` types,
  the diff logic, and the `AssetProcessor` SPI.
- `terracotta-core` owns config parsing for the `gallery` section and shared
  `GalleryValidator`.
- `terracotta-provider-modrinth` owns the `ModrinthGalleryItem` DTO, the client methods,
  the registry/state provider translations, and the default `AssetProcessor` wiring.
- `terracotta-gradle-plugin` owns the `gallery` DSL extension and wiring into tasks.
- Docs live under `docs/content/modules/core/` and `docs/content/modules/provider-modrinth/`.

## Error-handling strategy

- Missing file, unsupported format, or oversized file: fail fast with a clear
  `IOException` before any network call.
- Diff engine never throws; it only emits operations.
- Providers translate HTTP errors into `IOException` with the response body.
- Hangar ignores gallery operations rather than failing, so users can keep a shared
  gallery config even when publishing to both providers.

## Open questions and risks

1. Title-based identity is accepted for the first version. It will be revisited once
   the state-management system is designed.
2. Validation lives in core with provider-specific limits passed to the validator.
3. Remote gallery deletion is enabled by default. Restoring a deleted image requires
   restoring the config entry and re-running the apply task.
4. Modrinth URLs for gallery items are content-addressed and cannot be predicted before
   upload. Title-based matching should work for stable titles but will create duplicate
   images if a title changes.
5. The `AssetProcessor` SPI is intentionally minimal. Concrete implementations such as
   WebP conversion or lossy compression are follow-up work.

## Success criteria

- `terracotta.yml` can declare gallery images and they are uploaded on `terracottaApply`.
- `terracottaPlan` shows gallery operations when local and remote states differ.
- Modrinth provider uploads, updates metadata, and deletes gallery images correctly.
- Hangar provider skips gallery operations without failing the build.
- Core and provider tests cover diff, config parsing, and client behavior.
- Documentation is updated and `mkdocs build --strict` passes.
