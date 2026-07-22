# Changelog — Terracotta Gradle Plugin

All notable changes to `terracotta-gradle-plugin` will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.8.0] - 2026-07-13

Stabilizes gallery item identity using persisted state so images can be renamed or reordered without triggering a delete-and-reupload cycle.

### Added

- Added `key` property to `TerracottaGalleryExtension` and wired it through `TerracottaExtensionConfigurer` and `TerracottaTaskRegistrar`.
- Updated `TerracottaPlanTask` and `TerracottaApplyTask` to load persisted gallery state from the configured `StateSource`, pass it to `DiffEngine.diff`, and detect duplicate local keys with a warning and fallback to title/ordering matching.
- Updated `TerracottaApplyTask` to merge reported gallery identities from `GalleryIdentityReporter` providers into provider state and save the updated `TerracottaState` after a successful apply.

## [0.7.0] - 2026-07-13

Narrows Hangar license handling by mapping common SPDX identifiers to Hangar's license values and stopping `licenseUrl` from generating a recurring metadata diff on providers that cannot persist it.

### Added

- Added `visibility` property to the `terracotta` DSL extension and wired it through `TerracottaExtensionConfigurer`, `TerracottaTaskRegistrar`, `TerracottaPlanTask`, and `TerracottaApplyTask`.

### Changed

- Replaced the `tags` DSL property with a nested `categories { ... }` block (`TerracottaCategoriesExtension` and `TerracottaCategoryExtension`).
- Updated `TerracottaPlanTask` and `TerracottaApplyTask` to wire categories into the resolved project metadata.
- Updated `TerracottaPlanTask` and `TerracottaApplyTask` to pass the provider's `supportsLicenseUrl` capability to `DiffEngine.diff`, so providers that cannot persist a license URL do not generate a perpetual metadata update.

## [0.6.0] - 2026-07-12

Introduces pluggable state management and canonical project links. State persistence is now backend-agnostic, with the file-backed implementation extracted into its own module, and project links now map consistently between Terracotta, Modrinth, and Hangar with full Gradle DSL support.

### Added

- Added `terracotta.stateSource` (`Property<String>`) and `terracotta.stateSourceSettings` (`MapProperty<String, String>`) DSL properties for selecting and configuring a state backend. `stateSource` defaults to `"filesystem"`; `stateSourceSettings` defaults to `mapOf("path" to <projectDir>/.terracotta-state.yml)`.
- Deprecated `terracotta.stateFile`; it still works and internally maps to `stateSource.set("filesystem")` with `stateSourceSettings.put("path", <file>)`.
- Added nested `terracotta.links { ... }` DSL extension (`TerracottaLinksExtension`) for configuring links in `build.gradle.kts`. DSL values override `terracotta.yml` values and are wired into `terracottaPlan` and `terracottaApply` tasks.

### Changed

- Improved the error when the configured state backend is missing so it lists available backends and, for the default `filesystem` backend, includes the dependency coordinates needed to restore it.
- The Gradle plugin no longer bundles `terracotta-state-filesystem`. Add it to the buildscript classpath to use the default filesystem backend, or use a custom `StateSourceFactory` implementation.

## [0.5.0] - 2026-07-12

### Added

- Added `terracotta.icon` DSL property as a `RegularFileProperty`, populated from `terracotta.yml` and wired into `terracottaPlan` and `terracottaApply` tasks.

### Changed

- Renamed Gradle metadata detector package from `io.github.beduality.terracotta.gradle.detect` to `io.github.beduality.terracotta.gradle.detector`.

## [0.4.0] - 2026-07-12

### Added

- Added `terracotta.licenseUrl` DSL property, populated from `terracotta.yml` and wired into `terracottaPlan` and `terracottaApply` tasks.
- Added `terracotta { gallery { ... } }` DSL with `TerracottaGalleryExtension`, populated from `terracotta.yml` and wired into `terracottaPlan` and `terracottaApply` tasks.

## [0.3.0] - 2026-07-12

### Added

- Added `terracottaDestroy` aggregate task and per-provider `terracottaDestroy<Provider>` tasks (e.g. `terracottaDestroyModrinth`).
- Added `--force`, `--dry-run`, and `--versions-only` options so destructive operations require explicit confirmation and can preview impact before running.

## [0.2.0] - 2026-07-11

### Added

- Added `releaseType` and `gameVersion` detectors for Gradle-specific files (`gradle.properties`, `gradle/libs.versions.toml`, `build.gradle.kts` / `build.gradle`) via the `ProjectMetadataDetector` ServiceLoader SPI.
- Added fallback version resolution from `gradle.properties` when `project.version` is `unspecified`, so the resolved version is passed into core metadata resolution instead of core reading Gradle files directly.

## [0.1.4] - 2026-07-11

### Added

- Added `terracotta.yml` support so users can define project metadata, tags, versions, and providers in a dedicated file instead of the Kotlin DSL.
- Added YAML-to-DSL precedence so values set in the Kotlin DSL override the same values from `terracotta.yml`, letting users keep shared metadata in YAML while still using Gradle for dynamic or secret values.
- Integrated project metadata auto-detection so the plugin infers loaders, environment, license, description, and summary from project files when they are not configured explicitly.
- Added a `conventions` nested DSL block for selecting README and changelog conventions.

## [0.1.1] - 2026-07-10

### Added

- Gradle plugin frontend supporting `terracottaPlan` and `terracottaApply` tasks, with configuration via the `terracotta` extension.
  - **Why**: Provides developers with an ergonomic build-tool integration that aligns with typical Minecraft mod/plugin workflows, with direct access to project artifacts and metadata.
- Supports multiple providers via `providers` container, with per-provider plan/apply tasks.
  - **Why**: Allows publishing to multiple registries at once.
- Uses provider-specific tokens (e.g., `MODRINTH_TOKEN`) environment variables.
  - **Why**: Improves security and flexibility by using separate tokens per provider.
- Added `maven-publish` and `signing` plugins.
  - **Why**: Enables the Gradle plugin to be published to Maven Central.
