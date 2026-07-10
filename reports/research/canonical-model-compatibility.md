# Canonical Model Compatibility: Modrinth, Hangar, CurseForge

> Research conducted July 2026 against current API documentation.

## Summary

The `terracotta-core` canonical model is intentionally narrow — it captures the common
denominator of project/version metadata across the three major Minecraft plugin registries.
After this research, two small additions were made to close compatibility gaps (SPONGE loader,
`releaseType`/`changelog` on versions). The model is now fully compatible with all three
platforms without requiring platform-specific escape hatches.

---

## Platform API Overview

| Platform   | API Version | Auth Mechanism            | Version Upload Format       |
|------------|-------------|---------------------------|-----------------------------|
| Modrinth   | v2          | Personal access token     | Multipart (JSON + file)     |
| Hangar     | v1          | JWT (from API key)        | Multipart (JSON + file)     |
| CurseForge | v1          | X-Api-Token header        | Multipart (metadata + file) |

---

## TerracottaLoader Mapping

The canonical `TerracottaLoader` enum uses lowercase string IDs that map directly to
Modrinth's loader strings and CurseForge's game dependency slugs. Hangar uses a coarser
"Platform" concept (PAPER, VELOCITY, WATERFALL) — multiple terracotta loaders collapse
into one Hangar platform.

| TerracottaLoader | Modrinth loader | Hangar Platform | CurseForge dependency |
|------------------|-----------------|-----------------|----------------------|
| BUKKIT           | `bukkit`        | PAPER           | Bukkit               |
| BUNGEECORD       | `bungeecord`    | WATERFALL       | BungeeCord           |
| FABRIC           | `fabric`        | —               | Fabric               |
| FOLIA            | `folia`         | PAPER           | Folia                |
| FORGE            | `forge`         | —               | Forge                |
| NEOFORGE         | `neoforge`      | —               | NeoForge             |
| PAPER            | `paper`         | PAPER           | Paper                |
| PURPUR           | `purpur`        | PAPER           | Purpur               |
| QUILT            | `quilt`         | —               | Quilt                |
| SPIGOT           | `spigot`        | PAPER           | Spigot               |
| SPONGE           | `sponge`        | —               | Sponge               |
| VELOCITY         | `velocity`      | VELOCITY        | Velocity             |
| WATERFALL        | `waterfall`     | WATERFALL       | Waterfall            |

**Notes:**
- Hangar only supports Paper-ecosystem and proxy plugins; mod loaders (Fabric, Forge, etc.)
  are not applicable there.
- A future Hangar provider should group loaders by platform when building the upload payload.
- CurseForge uses numeric IDs internally; the provider will need a lookup table or API call
  to `/api/game/dependencies` to resolve slugs to IDs.

---

## TerracottaEnvironment Mapping

| TerracottaEnvironment | Modrinth (project)                          | Modrinth (version)     | CurseForge gameVersionNames | Hangar       |
|-----------------------|---------------------------------------------|------------------------|-----------------------------|--------------|
| CLIENT_ONLY           | client_side=required, server_side=unsupported | `client_only`          | "Client"                    | N/A          |
| SERVER_ONLY           | client_side=optional, server_side=required    | `server_only`          | "Server"                    | Implied      |
| UNIVERSAL             | client_side=required, server_side=required    | `client_and_server`    | "Client", "Server"          | Implied      |

**Decision:** Keep the 3-value enum. Modrinth's expanded options (`server_only_client_optional`,
`singleplayer_only`, etc.) are too platform-specific to model canonically. Providers can
map UNIVERSAL to the closest appropriate value.

---

## TerracottaVersion Mapping

| TerracottaVersion field | Modrinth              | Hangar                | CurseForge           |
|-------------------------|-----------------------|-----------------------|----------------------|
| `version`               | `version_number`      | version string        | implicit (file name) |
| `artifactPath`          | file upload           | file upload           | file upload          |
| `gameVersions`          | `game_versions[]`     | `platformDependencies` (per-platform) | `gameVersions[]` (numeric IDs) |
| `loaders`               | `loaders[]`           | derived from platform | `gameDependencies`   |
| `environment`           | `environment`         | N/A                   | gameVersionNames     |
| `releaseType`           | `version_type`        | `channel`             | `releaseType`        |
| `changelog`             | `changelog`           | `description`         | `changelog`          |

**Gap closed:** `releaseType` and `changelog` were added in this iteration. Both fields
have sensible defaults (`RELEASE` and empty string) so existing code is unaffected.

---

## TerracottaReleaseType Mapping

| TerracottaReleaseType | Modrinth version_type | Hangar channel | CurseForge releaseType |
|-----------------------|-----------------------|----------------|------------------------|
| RELEASE               | `release`             | `Release`      | `release`              |
| BETA                  | `beta`                | `Snapshot`     | `beta`                 |
| ALPHA                 | `alpha`               | `Snapshot`     | `alpha`                |

**Note:** Hangar uses named channels that projects create. By convention, non-release builds
go to "Snapshot". A future Hangar provider could make the channel name configurable.

---

## TerracottaProject Mapping

| TerracottaProject field | Modrinth         | Hangar           | CurseForge            |
|-------------------------|------------------|------------------|-----------------------|
| `id`                    | slug / base62 ID | project slug     | numeric project ID    |
| `name`                  | `title`          | `name`           | project name          |
| `summary`               | `description`    | `description`    | summary               |
| `description`           | `body`           | page content     | — (not via upload API)|
| `tags`                  | `categories[]`   | `tags[]`         | categories            |
| `license`               | `license_id`     | license type     | license               |

**Verdict:** The project model is sufficient for all three platforms. CurseForge's upload API
is more limited (it doesn't support creating/updating project metadata — only file uploads),
so the `CreateProject` and `UpdateMetadata` operations may not apply there.

---

## Provider Implementation Notes

### Modrinth (implemented)
- Fully functional state + registry provider.
- Now passes `releaseType` and `changelog` on version upload.
- Maps `TerracottaEnvironment` to `client_side`/`server_side` for project creation.

### Hangar (not yet implemented)
- Auth: POST `/api/v1/authenticate?apiKey=<key>` → JWT token.
- Version upload: POST `/api/v1/projects/{slug}/upload` (multipart: `versionUpload` JSON + files).
- Key differences from Modrinth:
  - Game versions are per-platform (e.g., Paper supports 1.19-1.20.2, Velocity supports 3.3).
  - Files can be per-platform or shared across platforms.
  - No project creation API — projects must exist on Hangar first.
- Loader-to-platform mapping needed (collapse BUKKIT/SPIGOT/PAPER/FOLIA/PURPUR → PAPER).

### CurseForge (not yet implemented)
- Auth: `X-Api-Token` header.
- Version upload: POST `/api/projects/{projectId}/upload-file` (multipart: metadata JSON + file).
- Key differences:
  - Game versions use numeric IDs — requires a lookup via `/api/game/versions`.
  - Loaders are expressed as game dependencies (also numeric IDs via `/api/game/dependencies`).
  - No project creation/update API for metadata — only file management.
  - Supports `relations` for dependencies (required, optional, incompatible, embedded).

---

## Changes Made

1. **TerracottaLoader**: Added `SPONGE` (supported by Modrinth and CurseForge).
2. **TerracottaReleaseType**: New enum (RELEASE, BETA, ALPHA).
3. **TerracottaVersion**: Added `releaseType` and `changelog` fields with defaults.
4. **ModrinthClient**: Now uses version's `releaseType` and `changelog` instead of hardcoded values.
5. **ModrinthStateProvider**: Populates `releaseType` and `changelog` from API response.
6. **Gradle plugin**: Exposes `releaseType` and `changelog` in the DSL with conventions.

---

## Sources

- [Modrinth API — Create Version](https://docs.modrinth.com/api/operations/createversion/)
- [Modrinth API — Loader List](https://docs.modrinth.com/api/operations/loaderlist)
- [Hangar Publishing Docs](https://docs.papermc.io/misc/hangar-publishing/)
- [Hangar Version Uploader Example (kennytv)](https://gist.github.com/kennytv/a227d82249f54e0ad35005330256fee2)
- [CurseForge Upload API](https://support.curseforge.com/en/support/solutions/articles/9000197321-curseforge-upload-api)
- [HangarMC/Hangar Repository](https://github.com/HangarMC/Hangar)

Content was rephrased for compliance with licensing restrictions.
