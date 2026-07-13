# Config Schema

This page describes the fields accepted by `terracotta.yml`. For type details, see the [generated KDoc](api.md).

## Top-level fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | No | Project display name. |
| `summary` | string | No | Short description. |
| `description` | string | No | Full description. |
| `categories` | map | No | Project categories with `primary` and `additional`. |
| `license` | string | No | SPDX license identifier. |
| `licenseUrl` | string | No | Optional URL to the full license text. Forwarded to Modrinth; ignored by Hangar, which does not expose a license URL field. When using Hangar, a configured `licenseUrl` does not generate a metadata update. |
| `icon` | string | No | Path to the project icon file. Uploaded to Modrinth; skipped by Hangar. |
| `gameVersions` | list of strings | No | Supported Minecraft versions. |
| `loaders` | list of strings | No | Loader identifiers. |
| `environment` | string | No | `client_only`, `server_only`, or `universal`. |
| `releaseType` | string | No | `release`, `beta`, or `alpha`. |
| `visibility` | string | No | `public`, `unlisted`, `archived`, `private`, or `draft`. Defaults to `public`. |
| `changelog` | string | No | Release notes for the current version. |
| `gallery` | list | No | Gallery images for the project. See [Gallery block](#gallery-block). |
| `links` | map | No | Canonical project links. See [Links block](#links-block). |
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
| `projectId` | string | No | Project slug or ID on the provider. Required at runtime; can also be supplied through caller-provided defaults (for example, a build-tool DSL). |
| `token` | string | No | API token for the provider. Defaults to the `<PROVIDER>_TOKEN` environment variable. |

## Gallery block

Each entry under `gallery:` describes an image to associate with the project.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `path` | string | Yes | Local file path to the image. |
| `title` | string | No | Human-readable title used as the stable identity key. |
| `description` | string | No | Optional longer description. |
| `featured` | boolean | No | Whether the image should be highlighted by the provider. |
| `ordering` | integer | No | Display order; lower values come first. |

Identity matching uses the normalized title (trimmed and lowercased). When a title
is not provided, items are matched by `ordering`.

## Links block

Each entry under `links:` is optional. Unknown keys are preserved in the `other` map.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `homepage` | string | No | Project homepage URL. |
| `source` | string | No | Source code repository URL. |
| `issues` | string | No | Issue tracker URL. |
| `wiki` | string | No | Wiki or documentation URL. |
| `community` | string | No | Community invite URL (e.g. Discord). |
| `donations` | list | No | Donation links. See [Donation link](#donation-link). |
| `other` | map | No | Arbitrary label-to-URL entries. |

### Donation link

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `platform` | string | Yes | Donation platform name or identifier. |
| `url` | string | Yes | URL to the donation page. |

## Example

```yaml
name: "My Plugin"
summary: "Lightweight Paper plugin"
description: "A useful plugin."
categories:
  primary:
    id: paper
    displayName: Paper
  additional:
    - id: utility
      displayName: Utility
license: "MIT"
licenseUrl: "https://github.com/example/my-plugin/blob/main/LICENSE"
icon: "docs/assets/icon.png"
gameVersions:
  - "1.21.8"
  - "1.21.7"
loaders:
  - paper
environment: server_only
releaseType: release
visibility: unlisted
changelog: "Initial release"

gallery:
  - path: "docs/assets/screenshot.png"
    title: "Main inventory screen"
    description: "Shows the new GUI"
    featured: true
    ordering: 0

links:
  homepage: "https://example.com/my-plugin"
  source: "https://github.com/example/my-plugin"
  issues: "https://github.com/example/my-plugin/issues"
  wiki: "https://github.com/example/my-plugin/wiki"
  community: "https://discord.gg/example"
  donations:
    - platform: "ko-fi"
      url: "https://ko-fi.com/example"
  other:
    twitter: "https://twitter.com/example"

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

All top-level fields are optional. Missing values are resolved from detected project files or caller-supplied defaults. Values supplied by the caller (for example, a build-tool DSL) always override values from `terracotta.yml`.

## Auto-detection

Terracotta can infer several fields from standard project files and caller-supplied build-system values when they are not configured explicitly. Detection runs after reading `terracotta.yml` but before applying caller-supplied overrides.

| Field | Source | Notes |
|-------|--------|-------|
| `name` | Build-system project name | Used when no display name is configured. |
| `summary` | `README.md` | First non-heading paragraph. |
| `description` | `README.md` | Full file content. |
| `loaders` | `fabric.mod.json`, `META-INF/mods.toml`, `paper-plugin.yml`, `plugin.yml`, `bungee.yml`, `velocity-plugin.json`, `mods.toml` (Sponge), etc. | Loader forks are resolved automatically; Paper also implies Spigot and Bukkit. |
| `gameVersions` | Loader-specific descriptors | Extracted from platform metadata such as Fabric's `depends.minecraft`. |
| `environment` | Loader-specific descriptors | Defaults to `server_only` when not detected. |
| `license` | `LICENSE` or `LICENSE.txt` | Only common SPDX identifiers are recognized. |
| `licenseUrl` | None | Must be configured explicitly; not auto-detected. |
| `releaseType` | Build-system project version | Detected from version strings containing `alpha`, `beta`, or `rc`. |
| `visibility` | None | Defaults to `public` when not configured. |
| `changelog` | `CHANGELOG.md` | Extracted using the configured changelog convention. |

A build-tool frontend such as the Terracotta Gradle plugin supplies build-system values through a `ProjectMetadataSource`. See [Resolve Project Metadata](../how-to-guides/resolve-project-metadata.md) for the full resolution workflow.

## Loader IDs

Use the lowercase loader ID in `terracotta.yml` and in caller-supplied configuration:

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

## Visibility values

| Value | Meaning |
|-------|---------|
| `public` | Publicly listed and discoverable. |
| `unlisted` | Accessible by direct URL but hidden from listings and search. |
| `archived` | Read-only and marked as no longer maintained. |
| `private` | Visible only to project members or the owner. |
| `draft` | Not yet published; visible only to the owner. |

## See also

- [Load a terracotta.yml File](../how-to-guides/load-terracotta-config.md)
- [Resolve Project Metadata](../how-to-guides/resolve-project-metadata.md)
