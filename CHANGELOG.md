# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.2] - 2026-07-10

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
