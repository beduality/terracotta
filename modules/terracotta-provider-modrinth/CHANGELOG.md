# Changelog — Terracotta Modrinth Provider

All notable changes to `terracotta-provider-modrinth` will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.8.0] - 2026-07-13

Stabilized gallery item identity using persisted state so images can be renamed or reordered without triggering a delete-and-reupload cycle.

### Added

- Added gallery identity reporting so image uploads return the new remote URL, updates keep the existing URL, and deletes omit the identity from persisted state.

## [0.7.0] - 2026-07-13

Narrowed Hangar license handling by mapping common SPDX identifiers to Hangar's license values and stopping `licenseUrl` from generating a recurring metadata diff on providers that cannot persist it.

### Added

- Added visibility mapping so Modrinth project status is read and written as `TerracottaVisibility`.

### Changed

- Changed category mapping so Modrinth featured and additional categories are read and written as `TerracottaProjectCategories`, including the featured/additional split when creating or patching projects.

## [0.6.0] - 2026-07-12

Introduced pluggable state management and canonical project links. State persistence is now backend-agnostic, with the file-backed implementation extracted into its own module, and project links now map consistently between Terracotta, Modrinth, and Hangar with full Gradle DSL support.

### Changed

- Changed project link mapping so remote Modrinth link fields (issues, source, wiki, discord, donations) are read into canonical links and written back during metadata updates and draft project creation.

## [0.5.0] - 2026-07-12

Added a provider-specific logic layer and project icon support so Modrinth integrations can filter unsupported operations and manage project icons.

### Added

- Added provider-specific operation filtering so unsupported Modrinth operations are skipped with a warning before reaching the registry.
- Added project icon support so icons can be uploaded to Modrinth (256 KiB limit, PNG/JPEG/WebP/GIF/BMP) and read back from the remote `icon_url` into the canonical `icon` field.

## [0.4.0] - 2026-07-12

Added license URL mapping and gallery image support so Modrinth projects can persist custom license URLs and upload gallery images.

### Added

- Added license URL mapping so Modrinth's `license.url` is read into the canonical `licenseUrl` field and written back when set.
- Added gallery image support so users can upload, update, and delete gallery images on Modrinth (5 MiB limit, PNG/JPEG/WebP/GIF/BMP).

## [0.3.0] - 2026-07-12

Added destructive registry support so projects and versions can be deleted from Modrinth.

### Added

- Added destructive registry support so projects and versions can be deleted from Modrinth.

## [0.1.1] - 2026-07-10

Bootstrapped the first concrete provider so Terracotta can sync project settings, metadata, and artifacts directly with Modrinth.

### Added

- Added Modrinth state and registry integration to sync project settings, metadata, and artifacts directly with Modrinth.
- Published the Modrinth provider to Maven Central so developers can use it as a library in their projects.
