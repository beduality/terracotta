# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

**Docs**

- Reorganized user documentation under `docs/content/modules/` and `docs/content/integration/` with Diátaxis sections for Core, Gradle Plugin, Modrinth Provider, and Hangar Provider; folded `docs/content/config/` into the Core reference.
- Added `@see` links and member-level KDoc to all public APIs in `terracotta-core`, `terracotta-gradle-plugin`, `terracotta-provider-modrinth`, and `terracotta-provider-hangar`, pointing to the new GitHub Pages docs.
- Removed the separate `docs/content/sdk/` section and folded its remaining pages into module docs: installation moved to `modules/core/tutorials/installation.md`, custom-provider content merged into `modules/core/tutorials/implementing-a-custom-provider.md`, provider API reference merged into `modules/core/reference/provider-interfaces.md`, architecture explanation merged into `modules/core/explanation/architecture.md`, and the Modrinth quick-start merged into `modules/provider-modrinth/tutorials/using-modrinth.md`.

### Fixed

**Docs**

- Fixed the pre-build hook destination so `LICENSE` is copied to `docs/LICENSE` instead of `docs/LICENSE.md`.
- Removed the CI/CD setup with GitHub Actions guide from `docs/content/integration/` because the workflow is not currently supported.
- Cleaned up `docs/content/repo/` by removing redundant pages, trimming the Diátaxis framework explanation, updating outdated navigation references, and removing remaining `SDK` terminology.
- Enforced strict Diátaxis discipline in `docs/content/repo/`: moved architecture explanation to `docs/content/modules/core/explanation/architecture.md`, rewrote `project-management.md` to remove procedural steps, moved `changelog-guidelines.md` to `reference/`, and added a clear target-audience statement to the repo README.

**Repo**

- Updated `.gitignore` to ignore the generated `docs/LICENSE` file instead of `docs/LICENSE.md`.

## [0.2.0] - 2026-07-11

### Added

**Hangar**

- Added Hangar provider module so Terracotta can sync projects and versions with the PaperMC Hangar registry.
  - Added JWT authentication using Hangar API keys.
  - Added loader-to-platform mapping so Bukkit/Spigot/Paper/Purpur/Folia projects are published as `PAPER`, Velocity as `VELOCITY`, and BungeeCord/Waterfall as `WATERFALL`.
  - Added automatic `Release`/`Snapshot` channel creation before uploading versions to Hangar.
  - Added multipart version uploads with per-platform game version dependencies.

**Core**

- Added automatic `releaseType` detection from the source version so callers can supply a version and have its release channel inferred (e.g. `1.0.0-beta.1` maps to `BETA`).
- Added automatic `gameVersion` detection from loader descriptors (`plugin.yml`, `paper-plugin.yml`, `fabric.mod.json`, `mods.toml`).
  - Detection is driven by a `GameVersionConvention` SPI with a `MinecraftGameVersionConvention` implementation, so it understands classic releases (`1.20.1`), snapshots (`25w14a`), pre-releases (`1.21.5-pre1`), release candidates (`1.21.5-rc1`), and version ranges such as `[1.20.1,1.20.2)`.
  - Detected versions are normalized and used as a low-priority default, so explicit `gameVersions` configuration still takes precedence.

**Gradle Plugin**

- Added `releaseType` and `gameVersion` detectors for Gradle-specific files (`gradle.properties`, `gradle/libs.versions.toml`, `build.gradle.kts` / `build.gradle`) via the `ProjectMetadataDetector` ServiceLoader SPI.
- Added fallback version resolution from `gradle.properties` when `project.version` is `unspecified`, so the resolved version is passed into core metadata resolution instead of core reading Gradle files directly.

### Removed

**Core**

- Removed Gradle business logic from `terracotta-core`: the `gradle.properties` version fallback now lives in `terracotta-gradle-plugin`.

### Changed

**Docs**

- Removed the `CI/CD Deployment` how-to guide from the docs navigation because the current stateless design does not support the documented workflow, preventing users from following an inaccurate guide.
- Moved the project introduction, installation, and usage content from `README.md` into `docs/index.md` so the docs site is the single source of truth for presentation content. `README.md` is now a lightweight index page that links to the documentation.
- Added a pre-build hook that copies `LICENSE` into `docs/LICENSE.md` so the docs site can link to the license without leaving the generated site.

**Repo**

- Updated the release script to keep Gradle plugin version snippets in `docs/index.md` in sync and removed `README.md` version-string handling.

### Fixed

**Repo**

- Fixed the GitHub release workflow so it extracts release notes directly from `CHANGELOG.md` instead of relying on an external parser that produced empty bodies for recent releases.
## [0.1.4] - 2026-07-11

### Added

**Docs**

- Added a new `Config` docs category with `Overview` and `Schema` pages so users can find the file-format reference independently of Gradle-plugin guides.

**Core**

- Added `terracotta.yml` parsing so library consumers can read Terracotta configuration from a standard YAML file.
- Added an extensible loader system with a `TerracottaLoader` interface and a `TerracottaLoaderRegistry` that auto-detects supported platforms from project files.
- Added built-in loader adapters for Bukkit, BungeeCord, Fabric, Folia, Forge, NeoForge, Paper, Purpur, Quilt, Spigot, Sponge, Velocity, and Waterfall, with parent-child inheritance so detecting a fork also records its parent loaders (e.g. Paper implies Spigot and Bukkit).
- Added a `ProjectMetadataDetector` ServiceLoader SPI and a `ProjectMetadataLoader` that merges values detected from project files with explicit source metadata.
- Added built-in metadata detectors that read `README.md`, license files, and loader descriptors to infer description, summary, license, loaders, and environment.
- Added a `ProjectFileConvention` registry with `ReadmeConvention` and `ChangelogConvention` adapters, including `TerracottaReadmeConvention` and `KeepAChangelogConvention`.
- Added release-type detection from version strings so `1.0.0-beta.1` or `1.0.0-alpha.1` are classified automatically.
- Added a `VersionConventionResolver` with a semver convention for interpreting version strings.

**Gradle Plugin**

- Added `terracotta.yml` support so users can define project metadata, tags, versions, and providers in a dedicated file instead of the Kotlin DSL.
- Added YAML-to-DSL precedence so values set in the Kotlin DSL override the same values from `terracotta.yml`, letting users keep shared metadata in YAML while still using Gradle for dynamic or secret values.
- Integrated project metadata auto-detection so the plugin infers loaders, environment, license, description, and summary from project files when they are not configured explicitly.
- Added a `conventions` nested DSL block for selecting README and changelog conventions.

### Fixed

**Docs**

- Removed the stale `docs/overrides` reference from `mkdocs.yml` so docs deployments no longer fail with a missing custom_dir path.
## [0.1.3] - 2026-07-11

### Added

**Docs**

- Added the smoke-testing release guide in plain language and added guidance on saving pytest JSON reports as metrics.

**Repo**

- Archived smoke-test results as structured JSON metrics instead of manual tables.

### Changed

**Docs**

- Moved the Changes link from the custom docs header into the main navigation so it works easily on mobile.
- Reorganized changelog docs into a succinct explanation and a practical how-to guide, and updated `CHANGELOG.md` to use bold module headings.

### Fixed

**Docs**

- Fixed docs deployments so the live site reliably matches the latest generated release, preventing stale or mismatched content.
- Configured versioned docs to build from the release tag, so the docs site updates as soon as a release goes out.
- Synchronized version references across `README.md`, `CHANGELOG.md`, generated release notes, and `docs/content/**/*.md` as part of the release process.

**Repo**

- Added pre-publish checks that abort if version references are out of sync with the release version.

**SDK**

- Fixed publishing of `-javadoc.jar` artifacts so they include generated API documentation instead of being empty or missing, satisfying Maven Central requirements.
- Added pre-publish checks that abort if `-javadoc.jar` artifacts are missing or empty.
## [0.1.2] - 2026-07-10

### Fixed

#### Repo

- Fixed release tooling so each release leaves an empty "Unreleased" section in the changelog.
  - **Why**: Keeps the changelog ready for the next development cycle.
- Fixed docs deployment so routine pushes no longer reset the default site version from `latest` to `unreleased`.
  - **Why**: Prevents the root site from drifting away from the latest release.

## [0.1.1] - 2026-07-10

### Added

#### Docs

- Added multi-module API reference links to the docs navigation.
  - **Why**: Provides API documentation for each module (core, gradle-plugin, provider-modrinth).
- Fixed broken internal links and replaced full URLs with relative links where appropriate.
  - **Why**: Ensures documentation is accurate and up-to-date.

#### Gradle Plugin

- Gradle plugin frontend supporting `terracottaPlan` and `terracottaApply` tasks, with configuration via the `terracotta` extension.
  - **Why**: Provides developers with an ergonomic build-tool integration that aligns with typical Minecraft mod/plugin workflows, with direct access to project artifacts and metadata.
- Supports multiple providers via `providers` container, with per-provider plan/apply tasks.
  - **Why**: Allows publishing to multiple registries at once.
- Uses provider-specific tokens (e.g., `MODRINTH_TOKEN`) environment variables.
  - **Why**: Improves security and flexibility by using separate tokens per provider.
- Added `maven-publish` and `signing` plugins.
  - **Why**: Enables the Gradle plugin to be published to Maven Central.

#### Modrinth

- Modrinth state and registry integration using Ktor Client and Kotlinx Serialization.
  - **Why**: Bootstraps the first concrete provider to sync project settings, metadata, and artifacts directly with Modrinth.
- Modrinth provider available via Maven Central.
  - **Why**: Enables developers to use Modrinth integration as a library in their projects.
- Added `maven-publish` plugin.
  - **Why**: Enables the Modrinth provider to be published to Maven Central.

#### Repo

- Implemented automated GitHub releases on tag push in `release.yml`.
  - **Why**: Automates the release process and makes artifacts available to users.
- Added changelog parsing to the release workflow for release notes.
  - **Why**: Generates release notes automatically from `CHANGELOG.md`.
- Added JAR artifact uploads to the `ci.yml` workflow.
  - **Why**: Makes build artifacts available for verification and release.
- Added Dokka documentation generation to the `deploy-docs.yml` workflow.
  - **Why**: Generates API reference documentation automatically on every docs deploy.

### Fixed

#### Repo

- Fixed `version` in `build.gradle.kts` being hardcoded to `0.1.0` instead of reading from `gradle.properties`.
  - **Why**: Ensures the published version matches the intended release version.

## [0.1.0] - 2026-07-10

### Added

#### Docs

- Added documentation website at `https://beduality.github.io/terracotta/`.
  - **Why**: Makes comprehensive, convenient, accessible documentation publicly available.

#### Core

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
