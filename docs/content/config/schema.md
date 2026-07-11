# Schema

This page describes the `terracotta.yml` schema. For details on how the Gradle plugin applies this file, see the [Gradle Plugin documentation](../gradle-plugin/reference/tasks.md).

---

## Example

```yaml
name: "My Plugin"                 # Project display name
summary: "Lightweight Paper plugin" # Short summary
description: "A useful plugin."   # Full description
tags:                             # List of tags / categories
  - paper
  - utility
license: "MIT"                    # License name or SPDX identifier
gameVersions:                     # Supported Minecraft versions
  - "1.21.8"
  - "1.21.7"
loaders:                          # Supported mod/plugin loaders
  - paper
environment: server_only          # client_only | server_only | universal
releaseType: release              # release | beta | alpha
changelog: "Initial release"      # Version changelog

convention:                       # README and changelog conventions
  readme: terracotta              # terracotta
  changelog: keep-a-changelog     # keep-a-changelog

providers:                        # Per-provider configuration
  modrinth:
    projectId: "my-plugin"        # Project slug or ID on the provider
    token: "optional-api-token"     # Optional; MODRINTH_TOKEN env var is used if omitted
  hangar:
    projectId: "my-plugin"        # Project slug on Hangar
    token: "optional-api-key"       # Optional; HANGAR_TOKEN env var is used if omitted
```

## Top-level Fields

All top-level fields are optional. When a value is omitted, the Gradle plugin falls back to auto-detected values from project files, then to Gradle defaults (e.g. `project.name` or `project.description`). Values set in the Kotlin DSL always override values from `terracotta.yml`.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `name` | `string` | auto-detected / `project.name` | Project display name. |
| `summary` | `string` | auto-detected / `project.description` | Short project summary. |
| `description` | `string` | auto-detected / `README.md` | Full project description. |
| `tags` | `list<string>` | `[]` | Project tags or categories. |
| `license` | `string` | auto-detected / `LICENSE` | License name or SPDX identifier. |
| `gameVersions` | `list<string>` | `[]` | Supported Minecraft versions. |
| `loaders` | `list<string>` | auto-detected | Supported loaders. See valid loader IDs below. |
| `environment` | `string` | auto-detected / `server_only` | Runtime environment. |
| `releaseType` | `string` | auto-detected / `release` | Stability channel for the uploaded version. |
| `changelog` | `string` | auto-detected / `""` | Changelog text for the version upload. |

## Convention Fields

Each entry under `convention:` selects the convention used to interpret project files.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `readme` | `string` | `terracotta` | README convention. `terracotta` uses the full file as the description and the first non-heading paragraph as the summary. |
| `changelog` | `string` | `keep-a-changelog` | Changelog convention. `keep-a-changelog` extracts the section under `## [version]` from `CHANGELOG.md`. |

## Provider Fields

Each entry under `providers:` uses the provider ID (e.g. `modrinth`, `hangar`) as the key.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `projectId` | `string` | none | Project slug or ID on the provider. Required at runtime but can be supplied in the Kotlin DSL. |
| `token` | `string` | `<PROVIDER>_TOKEN` env var | API token for the provider. |

---

## Auto-Detection

Terracotta can infer several fields from standard project files when they are not configured explicitly. Detection runs after reading `terracotta.yml` but before applying Kotlin DSL overrides.

| Field | Source | Notes |
|-------|--------|-------|
| `loaders` | `fabric.mod.json`, `META-INF/mods.toml`, `paper-plugin.yml`, `plugin.yml`, `bungee.yml`, `velocity-plugin.json`, `mods.toml` (Sponge), etc. | Loader forks are resolved automatically; Paper also implies Spigot and Bukkit. |
| `environment` | Loader-specific descriptors (e.g. `fabric.mod.json` `environment` key) | Defaults to `server_only` when not detected. |
| `license` | `LICENSE` or `LICENSE.txt` | Only common SPDX identifiers (MIT, Apache-2.0, etc.) are recognized. |
| `description` | `README.md` | Full file content. |
| `summary` | `README.md` | First non-heading paragraph. |
| `releaseType` | Gradle project version | Detected from version strings containing `alpha`, `beta`, or `rc`. |
| `changelog` | `CHANGELOG.md` | Extracted using the configured changelog convention. |

---

## Loader IDs

Use the lowercase loader ID in `terracotta.yml`:

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

In the Kotlin DSL you can pass the same lowercase string IDs to `loaders.set(listOf(...))`.

---

## Environment Values

| Value | Meaning |
|-------|---------|
| `client_only` | Client-side only |
| `server_only` | Server-side only |
| `universal` | Works on both client and server |

---

## Release Type Values

| Value | Meaning |
|-------|---------|
| `release` | Stable release |
| `beta` | Beta release |
| `alpha` | Alpha release |
