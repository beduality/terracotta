# Changelog — Terracotta Core

All notable changes to `terracotta-core` will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.8.0] - 2026-07-13

Stabilizes gallery item identity using persisted state so images can be renamed or reordered without triggering a delete-and-reupload cycle.

### Added

- Added optional `key` property to `TerracottaGalleryItem` for stable local identity.
- Added `GalleryIdentityReporter` interface so providers can report stable gallery identities after applying operations.
- Updated `DiffEngine.diff` with a persisted-gallery overload that matches local items to remote items by stable `localKey` even when titles change; falls back to title/ordering matching when no persisted identity exists.
- Added top-level `galleryLocalKey(item)` helper returning `item.key ?: item.imagePath`.

## [0.7.0] - 2026-07-13

Narrows Hangar license handling by mapping common SPDX identifiers to Hangar's license values and stopping `licenseUrl` from generating a recurring metadata diff on providers that cannot persist it.

### Added

- Added `TerracottaCategory` and `TerracottaProjectCategories` models for structured project categories.
- Added `TerracottaVisibility` enum (`public`, `unlisted`, `archived`, `private`, `draft`) and a canonical `visibility` field on `TerracottaProject`, `TerracottaConfig`, and `ResolvedProjectMetadata`.
- Added `Operation.UpdateVisibility` and updated `DiffEngine` to emit it when the local and remote visibility differ.
- Added `ProviderLogic.supportsLicenseUrl` capability so providers can indicate whether they persist a custom license URL. `DiffEngine.diff` now accepts this flag and ignores `licenseUrl` differences when the provider reports it is unsupported.

### Changed

- Replaced free-form `tags: List<String>` on `TerracottaProject` with structured `categories: TerracottaProjectCategories`.
- Replaced `Operation.UpdateTags` with `Operation.UpdateCategories` carrying `TerracottaProjectCategories`.
- Updated `DiffEngine` to compare and emit category changes via `UpdateCategories`.

## [0.6.0] - 2026-07-12

Introduces pluggable state management and canonical project links. State persistence is now backend-agnostic, with the file-backed implementation extracted into its own module, and project links now map consistently between Terracotta, Modrinth, and Hangar with full Gradle DSL support.

### Added

- Added pluggable state-management SPI in `io.github.beduality.terracotta.core.state`: `StateSource`, `StateSourceFactory`, and `StateSourceConfig`. Backends are discovered at runtime via `ServiceLoader`.
- Added canonical project links (`homepage`, `source`, `issues`, `wiki`, `community`, `donations`, `other`) via `TerracottaProjectLinks` and `TerracottaDonationLink`. The `links` field is available on `TerracottaProject`, `TerracottaConfig`, `ProjectMetadata`, and `ResolvedProjectMetadata`. Link changes are detected by `DiffEngine` and emitted as `Operation.UpdateMetadata` with `linksChanged` and `newLinks`.

### Changed

- Moved `FileSystemStateSource` and `YamlStateCodec` from `terracotta-core` to the new `terracotta-state-filesystem` module. The package name `io.github.beduality.terracotta.core.state` is preserved, so existing imports continue to work when the new module is on the classpath.

## [0.5.0] - 2026-07-12

### Added

- Added provider-specific logic layer with `ProviderLogic`, `LoaderMapper`, and `PlatformBehavior` interfaces in `terracotta-core`. `ProviderFactory` now exposes `createProviderLogic()` so providers can share registry-specific loader mappings and operation filtering rules.
- Added `BaseRegistryProvider` in `terracotta-core`. It automatically filters operations using the injected `ProviderLogic`, logs warnings for skipped operations, and delegates the rest to provider-specific `applySupported` implementations.
- Added optional `icon` field to `TerracottaProject`, `TerracottaConfig`, `ResolvedProjectMetadata`, and project metadata resolution. The value is a local file path in configuration and a remote URL when read from provider state.
- Added `UploadIcon`, `UpdateIcon`, and `DeleteIcon` operations and diff logic that compares the local icon path against the remote icon URL.

### Changed

- Moved `ProjectMetadataDetector`, `ProjectMetadataLoader`, and built-in adapters into `io.github.beduality.terracotta.core.model.metadata.detector` (renamed from `io.github.beduality.terracotta.core.detect`). Service files and custom detector implementations must update package names and imports.

## [0.4.1] - 2026-07-12

### Fixed

- Republished as `0.4.1` to align Maven Central artifacts with the `v0.4.0` release tag. The initial `0.4.0` upload was published from a slightly earlier commit than the tagged release, so this patch ensures users receive the source reflected by the `v0.4.0` tag.

## [0.4.0] - 2026-07-12

### Added

- Added optional `licenseUrl` field to `TerracottaProject`, project metadata interfaces, `TerracottaConfig`, `ResolvedProjectMetadata`, and `Operation.UpdateMetadata`. URLs are compared exactly and trigger metadata updates when changed.
- Added `TerracottaGalleryItem` model and `gallery` field on `TerracottaProject`.
- Added `UploadGalleryItem`, `UpdateGalleryItem`, and `DeleteGalleryItem` operations and diff logic that matches images by normalized title or ordering.
- Added `AssetProcessor` SPI with `ProcessedAsset` and `IdentityAssetProcessor` default implementation, loaded via `AssetProcessorLoader`.
- Added `GalleryValidator` for checking file existence, supported extensions, and size limits.
- Added `gallery` section parsing to `TerracottaConfigLoader`.
- Added `ResolvedProjectMetadata.gallery` resolved from `terracotta.yml`.

### Changed

- Split `TerracottaConfig`, `TerracottaConventionConfig`, and `TerracottaProviderConfig` into separate source files under `io.github.beduality.terracotta.core.config` for better maintainability. No API or behavior changes.

## [0.3.0] - 2026-07-12

### Added

- Added `DestructiveRegistryProvider` interface and an optional `createDestructiveRegistryProvider` factory method on `ProviderFactory` so providers can opt into project and version deletion.

## [0.2.0] - 2026-07-11

### Added

- Added automatic `releaseType` detection from the source version so callers can supply a version and have its release channel inferred (e.g. `1.0.0-beta.1` maps to `BETA`).
- Added automatic `gameVersion` detection from loader descriptors (`plugin.yml`, `paper-plugin.yml`, `fabric.mod.json`, `mods.toml`).
  - Detection is driven by a `GameVersionConvention` SPI with a `MinecraftGameVersionConvention` implementation, so it understands classic releases (`1.20.1`), snapshots (`25w14a`), pre-releases (`1.21.5-pre1`), release candidates (`1.21.5-rc1`), and version ranges such as `[1.20.1,1.20.2)`.
  - Detected versions are normalized and used as a low-priority default, so explicit `gameVersions` configuration still takes precedence.

### Removed

- Removed Gradle business logic from `terracotta-core`: the `gradle.properties` version fallback now lives in `terracotta-gradle-plugin`.

## [0.1.4] - 2026-07-11

### Added

- Added `terracotta.yml` parsing so library consumers can read Terracotta configuration from a standard YAML file.
- Added an extensible loader system with a `TerracottaLoader` interface and a `TerracottaLoaderRegistry` that auto-detects supported platforms from project files.
- Added built-in loader adapters for Bukkit, BungeeCord, Fabric, Folia, Forge, NeoForge, Paper, Purpur, Quilt, Spigot, Sponge, Velocity, and Waterfall, with parent-child inheritance so detecting a fork also records its parent loaders (e.g. Paper implies Spigot and Bukkit).
- Added a `ProjectMetadataDetector` ServiceLoader SPI and a `ProjectMetadataLoader` that merges values detected from project files with explicit source metadata.
- Added built-in metadata detectors that read `README.md`, license files, and loader descriptors to infer description, summary, license, loaders, and environment.
- Added a `ProjectFileConvention` registry with `ReadmeConvention` and `ChangelogConvention` adapters, including `TerracottaReadmeConvention` and `KeepAChangelogConvention`.
- Added release-type detection from version strings so `1.0.0-beta.1` or `1.0.0-alpha.1` are classified automatically.
- Added a `VersionConventionResolver` with a semver convention for interpreting version strings.

## [0.1.0] - 2026-07-10

### Added

- Canonical project and version domain models.
  - **Why**: Provides structured data models for representing projects and versions.
- Provider abstractions (`StateProvider`, `RegistryProvider`, `ProviderFactory`).
  - **Why**: Separates platform-agnostic business logic from registry integrations.
- `DiffEngine` calculating semantic operations.
  - **Why**: Facilitates robust diff calculations.
- Support for full project creation when a project does not exist on the remote registry.
  - **Why**: Enables clean automation for new projects.
- Core SDK available via Maven Central.
  - **Why**: Enables developers to use Terracotta core logic as a library in their projects.
