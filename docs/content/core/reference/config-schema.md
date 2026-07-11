# Config Schema

This page describes the fields accepted by `terracotta.yml`. For type details, see the [generated KDoc](api.md).

## Top-level fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | No | Project display name. |
| `summary` | string | No | Short description. |
| `description` | string | No | Full description. |
| `tags` | list of strings | No | Search tags. |
| `license` | string | No | SPDX license identifier. |
| `gameVersions` | list of strings | No | Supported Minecraft versions. |
| `loaders` | list of strings | No | Loader identifiers. |
| `environment` | string | No | `client_only`, `server_only`, or `universal`. |
| `releaseType` | string | No | `release`, `beta`, or `alpha`. |
| `changelog` | string | No | Release notes for the current version. |
| `convention` | map | No | `readme` and `changelog` convention identifiers. |
| `providers` | map | No | Provider-specific configuration keyed by provider ID. |

## Convention block

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `readme` | string | `terracotta` | Convention for extracting summary/description from `README.md`. |
| `changelog` | string | `keep-a-changelog` | Convention for extracting version release notes from `CHANGELOG.md`. |

## Provider block

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `projectId` | string | No | Project ID on the provider. |
| `token` | string | No | Authentication token. |

## Example

```yaml
name: My Plugin
summary: A useful Paper plugin
tags:
  - paper
  - utility
license: MIT
gameVersions:
  - 1.21.1
loaders:
  - paper
environment: server_only
releaseType: release
convention:
  readme: terracotta
  changelog: keep-a-changelog
providers:
  modrinth:
    projectId: my-plugin
```

## Partial files

All top-level fields are optional. Missing values are resolved from detected project files or caller-supplied defaults.

## See also

- [Load a terracotta.yml File](../how-to-guides/load-terracotta-config.md)
- [Resolve Project Metadata](../how-to-guides/resolve-project-metadata.md)
