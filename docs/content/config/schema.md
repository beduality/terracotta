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

providers:                        # Per-provider configuration
  modrinth:
    projectId: "my-plugin"        # Project slug or ID on the provider
    token: "optional-api-token"     # Optional; MODRINTH_TOKEN env var is used if omitted
```

## Top-level Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `name` | `string` | required | Project display name. |
| `summary` | `string` | required | Short project summary. |
| `description` | `string` | required | Full project description. |
| `tags` | `list<string>` | `[]` | Project tags or categories. |
| `license` | `string` | required | License name or SPDX identifier. |
| `gameVersions` | `list<string>` | `[]` | Supported Minecraft versions. |
| `loaders` | `list<string>` | `[]` | Supported loaders. See valid loader IDs below. |
| `environment` | `string` | `server_only` | Runtime environment. |
| `releaseType` | `string` | `release` | Stability channel for the uploaded version. |
| `changelog` | `string` | `""` | Changelog text for the version upload. |

## Provider Fields

Each entry under `providers:` uses the provider ID (e.g. `modrinth`, `hangar`) as the key.

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `projectId` | `string` | required | Project slug or ID on the provider. |
| `token` | `string` | `<PROVIDER>_TOKEN` env var | API token for the provider. |

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

In the Kotlin DSL you can use the typed enum, e.g. `TerracottaLoader.PAPER`.

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
