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

Each entry under `providers:` uses the provider ID (e.g. `modrinth`, `hangar`) as the key.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `projectId` | string | No | Project slug or ID on the provider. Required at runtime but can be supplied in the Kotlin DSL. |
| `token` | string | No | API token for the provider. Defaults to the `<PROVIDER>_TOKEN` environment variable. |

## Example

```yaml
name: "My Plugin"
summary: "Lightweight Paper plugin"
description: "A useful plugin."
tags:
  - paper
  - utility
license: "MIT"
gameVersions:
  - "1.21.8"
  - "1.21.7"
loaders:
  - paper
environment: server_only
releaseType: release
changelog: "Initial release"

convention:
  readme: terracotta
  changelog: keep-a-changelog

providers:
  modrinth:
    projectId: "my-plugin"
  hangar:
    projectId: "my-plugin"
```

## Partial files

All top-level fields are optional. Missing values are resolved from detected project files or caller-supplied defaults. Values set in the Kotlin DSL always override values from `terracotta.yml`.

## Auto-detection

Terracotta can infer several fields from standard project files and Gradle metadata when they are not configured explicitly. Detection runs after reading `terracotta.yml` but before applying Kotlin DSL overrides.

| Field | Source | Notes |
|-------|--------|-------|
| `name` | Gradle `project.name` | Used when no display name is configured. |
| `summary` | `README.md` | First non-heading paragraph. |
| `description` | `README.md` | Full file content. |
| `loaders` | `fabric.mod.json`, `META-INF/mods.toml`, `paper-plugin.yml`, `plugin.yml`, `bungee.yml`, `velocity-plugin.json`, `mods.toml` (Sponge), etc. | Loader forks are resolved automatically; Paper also implies Spigot and Bukkit. |
| `gameVersions` | Loader-specific descriptors | Extracted from platform metadata such as Fabric's `depends.minecraft`. |
| `environment` | Loader-specific descriptors | Defaults to `server_only` when not detected. |
| `license` | `LICENSE` or `LICENSE.txt` | Only common SPDX identifiers are recognized. |
| `releaseType` | Gradle project version | Detected from version strings containing `alpha`, `beta`, or `rc`. |
| `changelog` | `CHANGELOG.md` | Extracted using the configured changelog convention. |

## Loader IDs

Use the lowercase loader ID in `terracotta.yml` and in the Kotlin DSL:

- `bukkit`
- `bungeecord`
- `fabric`
- `folia`
- `forge`
- `neoforge`
- `paper`
- `purpur`
- `quilt`
- `spigot`
- `sponge`
- `velocity`
- `waterfall`

## Environment values

| Value | Meaning |
|-------|---------|
| `client_only` | Client-side only |
| `server_only` | Server-side only |
| `universal` | Works on both client and server |

## Release type values

| Value | Meaning |
|-------|---------|
| `release` | Stable release |
| `beta` | Beta release |
| `alpha` | Alpha release |

## See also

- [Load a terracotta.yml File](../how-to-guides/load-terracotta-config.md)
- [Resolve Project Metadata](../how-to-guides/resolve-project-metadata.md)
