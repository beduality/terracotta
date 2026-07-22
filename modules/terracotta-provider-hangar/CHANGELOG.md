# Changelog — Terracotta Hangar Provider

All notable changes to `terracotta-provider-hangar` will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.7.0] - 2026-07-13

Narrowed Hangar license handling by mapping common SPDX identifiers to Hangar's license values and stopping `licenseUrl` from generating a recurring metadata diff on providers that cannot persist it.

### Added

- Filtered out `UpdateVisibility` operations via `HangarPlatformBehavior` so Hangar applies continue without failing; a warning is logged for skipped operations.
- Added `HangarLicenseMapper` to map common SPDX license identifiers (e.g., `Apache-2.0`, `GPL-3.0-only`) to the license values accepted by Hangar's project settings API.
- Reported `supportsLicenseUrl = false` via `HangarProviderLogic` because Hangar's project API does not expose a license URL field.
- `HangarStateProvider` reverse-maps Hangar license values back to Terracotta license identifiers when reading project state.
- `HangarRegistryProvider` no longer warns about configured `licenseUrl`; the diff engine now ignores the field for Hangar.

### Changed

- `HangarStateProvider.fetchProject` maps Hangar `category` and `tags` to `TerracottaProjectCategories`.
- `HangarRegistryProvider` applies `UpdateCategories` by mapping the primary category and recognized tags back to Hangar's model.
- `HangarPlatformBehavior` filters on `UpdateCategories` instead of `UpdateTags`.

## [0.6.0] - 2026-07-12

Introduced pluggable state management and canonical project links. State persistence is now backend-agnostic, with the file-backed implementation extracted into its own module, and project links now map consistently between Terracotta, Modrinth, and Hangar with full Gradle DSL support.

### Changed

- `HangarStateProvider.fetchProject` now maps remote `homepage`, `source`, `issues`, `wiki`, `discord`, and `donations` to canonical links.
- `HangarRegistryProvider` updates Hangar project link fields during `UpdateMetadata`.

## [0.5.0] - 2026-07-12

Added a provider-specific logic layer so Hangar integrations can filter unsupported operations before they reach the registry provider.

### Added

- Added `HangarProviderLogic` and `HangarPlatformBehavior`, which filters out unsupported operations (`CreateProject`, gallery, and icon) before `HangarRegistryProvider` processes them. `HangarLoaderMapper` now implements the core `LoaderMapper` interface and is shared by the state and registry providers.
- Inherited warning behavior from `BaseRegistryProvider`, which logs warnings when unsupported operations are skipped, including `CreateProject`, gallery, and icon operations.

## [0.4.0] - 2026-07-12

Added warnings for unsupported `licenseUrl` and gallery operations so users are informed when Hangar cannot persist configured values.

### Added

- Hangar registry provider now warns when `licenseUrl` is configured, since Hangar does not support publishing a license URL.
- Hangar registry provider now skips gallery operations with a warning, since Hangar does not expose a gallery API.

## [0.3.0] - 2026-07-12

Added destructive registry support so projects and versions can be deleted from Hangar.

### Added

- Added `HangarDestructiveRegistryProvider` with DELETE endpoints for removing projects and versions from Hangar.

## [0.2.0] - 2026-07-11

Bootstrapped the Hangar provider so Terracotta can sync projects and versions with the PaperMC Hangar registry.

### Added

- Added Hangar provider module so Terracotta can sync projects and versions with the PaperMC Hangar registry.
  - Added JWT authentication using Hangar API keys.
  - Added loader-to-platform mapping so Bukkit/Spigot/Paper/Purpur/Folia projects are published as `PAPER`, Velocity as `VELOCITY`, and BungeeCord/Waterfall as `WATERFALL`.
  - Added automatic `Release`/`Snapshot` channel creation before uploading versions to Hangar.
  - Added multipart version uploads with per-platform game version dependencies.
