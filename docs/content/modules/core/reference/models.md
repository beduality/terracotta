# Core Models

This page describes the canonical domain models in `terracotta-core`. For field-level API details, see the [generated KDoc](../reference/api.md).

## TerracottaProject

Represents a project as it should appear on a registry.

| Field | Purpose |
|-------|---------|
| `schemaVersion` | Internal schema version. Always `1`. |
| `id` | Registry project identifier. |
| `name` | Display name. |
| `summary` | Short description or tagline. |
| `description` | Full project description. |
| `versions` | List of `TerracottaVersion` objects. |
| `categories` | `TerracottaProjectCategories` object. |
| `license` | SPDX license identifier. |
| `licenseUrl` | Optional URL to the full license text. |
| `icon` | Path to the project icon file, or the remote icon URL when read from provider state. |
| `gallery` | List of `TerracottaGalleryItem` objects. |
| `links` | `TerracottaProjectLinks` object. |
| `visibility` | `TerracottaVisibility` value. Defaults to `PUBLIC`. |

## TerracottaGalleryItem

Represents a single gallery image for a project. When the item is declared in
`terracotta.yml`, `imagePath` is a local file path. When the item is fetched from a
provider, `imagePath` holds the remote URL.

| Field | Purpose | Default |
|-------|---------|---------|
| `imagePath` | Local file path or remote URL. | — |
| `key` | Optional stable local identity key. | `null` |
| `title` | Human-readable title used as the fallback identity key. | `""` |
| `description` | Optional longer description. | `""` |
| `featured` | Whether the image is highlighted. | `false` |
| `ordering` | Display order; lower values come first. | `0` |

## TerracottaVersion

Represents a single release of a project.

| Field | Purpose | Default |
|-------|---------|---------|
| `version` | Version string. | — |
| `artifactPath` | Path to the compiled artifact. | — |
| `gameVersions` | Supported Minecraft versions. | — |
| `loaders` | Loader identifiers. | `emptyList()` |
| `environment` | `CLIENT_ONLY`, `SERVER_ONLY`, or `UNIVERSAL`. | `SERVER_ONLY` |
| `releaseType` | `RELEASE`, `BETA`, or `ALPHA`. | `RELEASE` |
| `changelog` | Release notes. | `""` |
| `displayName` | Human-readable version title. | `""` |

## TerracottaEnvironment

| Value | Serialized ID |
|-------|---------------|
| `CLIENT_ONLY` | `client_only` |
| `SERVER_ONLY` | `server_only` |
| `UNIVERSAL` | `universal` |

Use `TerracottaEnvironment.fromId(id)` to parse a string value.

## TerracottaReleaseType

| Value | Serialized ID |
|-------|---------------|
| `RELEASE` | `release` |
| `BETA` | `beta` |
| `ALPHA` | `alpha` |

Use `TerracottaReleaseType.fromId(id)` to parse a string value.

## TerracottaVisibility

| Value | Serialized ID |
|-------|---------------|
| `PUBLIC` | `public` |
| `UNLISTED` | `unlisted` |
| `ARCHIVED` | `archived` |
| `PRIVATE` | `private` |
| `DRAFT` | `draft` |

Use `TerracottaVisibility.fromId(id)` to parse a string value.

## See also

- [API Documentation](api.md)
