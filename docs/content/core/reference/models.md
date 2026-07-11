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
| `tags` | Search tags. |
| `license` | SPDX license identifier. |

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

## See also

- [API Documentation](api.md)
