# Changelog — Terracotta Modrinth Provider

All notable changes to `terracotta-provider-modrinth` will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.8.0] - 2026-07-13

Stabilizes gallery item identity using persisted state so images can be renamed or reordered without triggering a delete-and-reupload cycle.

### Added

- `ModrinthRegistryProvider` implements `GalleryIdentityReporter`: uploads return the new remote URL, updates keep the existing URL, and deletes omit the identity.
- `ModrinthClient.uploadGalleryItem` returns the created gallery image URL.

## [0.7.0] - 2026-07-13

Narrows Hangar license handling by mapping common SPDX identifiers to Hangar's license values and stopping `licenseUrl` from generating a recurring metadata diff on providers that cannot persist it.

### Added

- `ModrinthStateProvider.fetchProject` maps Modrinth project `status` to `TerracottaVisibility`.
- `ModrinthRegistryProvider` applies `UpdateVisibility` by patching the project `status`.

### Changed

- `ModrinthStateProvider.fetchProject` maps Modrinth `categories` and `additional_categories` to `TerracottaProjectCategories`.
- `ModrinthRegistryProvider` applies `UpdateCategories` to Modrinth featured and additional categories.
- `ModrinthClient.createProject` and `updateProject` serialize Terracotta categories using the Modrinth featured / additional split.

## [0.6.0] - 2026-07-12

Introduces pluggable state management and canonical project links. State persistence is now backend-agnostic, with the file-backed implementation extracted into its own module, and project links now map consistently between Terracotta, Modrinth, and Hangar with full Gradle DSL support.

### Changed

- `ModrinthStateProvider.fetchProject` now maps remote `issues_url`, `source_url`, `wiki_url`, `discord_url`, and `donation_urls` to canonical links.
- `ModrinthRegistryProvider` patches Modrinth project link fields during `UpdateMetadata` and includes link fields when creating draft projects.

## [0.5.0] - 2026-07-12

### Added

- Added `ModrinthProviderLogic` exposing identity loader mapping and stateful platform behavior. `ModrinthRegistryProvider` now consumes the logic layer to filter operations.
- Added project icon support via `ModrinthClient.uploadIcon`, which calls `PATCH /project/{id}/icon` with a multipart file (256 KiB limit, PNG/JPEG/WebP/GIF/BMP). `ModrinthStateProvider.fetchProject` now reads `icon_url` into the canonical `icon` field.

## [0.4.0] - 2026-07-12

### Added

- Added `license_url` mapping: `ModrinthStateProvider.fetchProject` reads `license.url` into the canonical `licenseUrl`, and `ModrinthClient.createProject` / `ModrinthRegistryProvider` emit `license_url` when it is set.
- Added gallery image support via `ModrinthClient.uploadGalleryItem`, `updateGalleryItem`, and `deleteGalleryItem`, using the core `GalleryValidator` (5 MiB limit, PNG/JPEG/WebP/GIF/BMP) and the configured `AssetProcessor`.

## [0.3.0] - 2026-07-12

### Added

- Added `ModrinthDestructiveRegistryProvider` with DELETE endpoints for removing projects and versions from Modrinth.

## [0.1.1] - 2026-07-10

### Added

- Modrinth state and registry integration using Ktor Client and Kotlinx Serialization.
  - **Why**: Bootstraps the first concrete provider to sync project settings, metadata, and artifacts directly with Modrinth.
- Modrinth provider available via Maven Central.
  - **Why**: Enables developers to use Modrinth integration as a library in their projects.
- Added `maven-publish` plugin.
  - **Why**: Enables the Modrinth provider to be published to Maven Central.
