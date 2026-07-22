# Changelog — Terracotta Hangar Provider

All notable changes to `terracotta-provider-hangar` will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.7.0] - 2026-07-13

Narrowed Hangar license handling by mapping common SPDX identifiers to Hangar's license values and stopping `licenseUrl` from generating a recurring metadata diff on providers that cannot persist it.

### Added

- Added SPDX license identifier mapping so common licenses (e.g., `Apache-2.0`, `GPL-3.0-only`) are mapped to Hangar's accepted license values on upload and reverse-mapped when reading project state.
- Added `supportsLicenseUrl = false` reporting so the diff engine ignores `licenseUrl` differences for Hangar, eliminating perpetual metadata updates and spurious warnings.
- Filtered out visibility update operations so Hangar applies continue without failing; a warning is logged for skipped operations.

### Changed

- Changed category mapping so Hangar's primary category and tags are read and written as `TerracottaProjectCategories`, with unsupported category operations filtered out.

## [0.6.0] - 2026-07-12

Introduced pluggable state management and canonical project links. State persistence is now backend-agnostic, with the file-backed implementation extracted into its own module, and project links now map consistently between Terracotta, Modrinth, and Hangar with full Gradle DSL support.

### Changed

- Changed project link mapping so remote Hangar link fields (homepage, source, issues, wiki, discord, donations) are read into canonical links and written back during metadata updates.

## [0.5.0] - 2026-07-12

Added a provider-specific logic layer so Hangar integrations can filter unsupported operations before they reach the registry provider.

### Added

- Added provider-specific operation filtering so unsupported Hangar operations (project creation, gallery, and icon) are skipped with a warning before reaching the registry.

## [0.4.0] - 2026-07-12

Added warnings for unsupported `licenseUrl` and gallery operations so users are informed when Hangar cannot persist configured values.

### Added

- Added warnings when `licenseUrl` is configured or gallery operations are attempted, since Hangar does not support publishing a license URL or exposing a gallery API.

## [0.3.0] - 2026-07-12

Added destructive registry support so projects and versions can be deleted from Hangar.

### Added

- Added destructive registry support so projects and versions can be deleted from Hangar.

## [0.2.0] - 2026-07-11

Bootstrapped the Hangar provider so Terracotta can sync projects and versions with the PaperMC Hangar registry.

### Added

- Added Hangar provider module so Terracotta can sync projects and versions with the PaperMC Hangar registry.
  - Added JWT authentication using Hangar API keys.
  - Added loader-to-platform mapping so Bukkit/Spigot/Paper/Purpur/Folia projects are published as `PAPER`, Velocity as `VELOCITY`, and BungeeCord/Waterfall as `WATERFALL`.
  - Added automatic `Release`/`Snapshot` channel creation before uploading versions to Hangar.
  - Added multipart version uploads with per-platform game version dependencies.
