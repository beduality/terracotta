# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Per-module changelogs live alongside each module under `modules/<module>/CHANGELOG.md`.
This root changelog tracks repository-wide changes (docs, CI/CD, tooling, conventions).

## [Unreleased]

### Changed

- Split the monolithic `CHANGELOG.md` into per-module changelogs under `modules/<module>/CHANGELOG.md` so each module's release history is self-contained. The root changelog now only tracks repo-wide changes (docs, CI/CD, tooling, conventions).
- Replaced custom regex-based semver parsing in `release.py` with the `semver` Python library for version bumping and validation.
- Reworked the docs homepage (`index.md`) to be more succinct: replaced the dense numbered workflow description with a Mermaid diagram and folded the gallery note and example output into accordions.

### Added

- Added `validate_next_version` to `release.py` so custom version bumps are restricted to the next valid patch, minor, or major. Rejects downgrades, same-version re-releases, and version jumps.
- Added 17 dry-run tests and 13 version validation unit tests for the release command.

### Fixed

- Fixed unknown module names raising `KeyError` instead of `ValueError` when passed via `--modules`.
- Fixed broken MkDocs Material content tab syntax in authentication sections of the Gradle plugin and integration how-to guides.
- Removed obsolete per-provider plan/apply task names from the docs homepage quick reference.
- Updated Gradle plugin and provider tutorials to explicitly install the `terracotta-state-filesystem` backend instead of assuming it is bundled.

## [0.8.0] - 2026-07-13

Stabilizes gallery item identity using persisted state so images can be renamed or reordered without triggering a delete-and-reupload cycle.

See per-module changelogs for module-specific changes in this release.

## [0.7.1] - 2026-07-13

Tightens the changelog standard so `[Unreleased]` summaries are written as direct summaries and cannot be promoted with stale "unreleased" wording.

### Fixed

- Corrected the 0.7.0 changelog summary to remove the incorrect "unreleased" wording.

### Changed

- Updated changelog guidelines to require direct summaries in `[Unreleased]`.
- Updated `scripts/release.py` to reject `[Unreleased]` summaries that start with "This release...", "This unreleased...", or "These unreleased...".
- Updated the plan generation workflow and template to remind users to write direct changelog summaries.

## [0.7.0] - 2026-07-13

See per-module changelogs for module-specific changes in this release.

### Docs

- Simplified homepage, integration tutorial, and getting-started docs to feature Modrinth as the default single provider, moving Hangar coverage to a follow-up how-to guide.

## [0.6.0] - 2026-07-12

Introduces pluggable state management and canonical project links. See per-module changelogs for module-specific changes.

### Repo

- Added a required release summary paragraph to the changelog standard. Every release section, including `[Unreleased]`, must now start with a summary before the first category heading. Updated the changelog guidelines, writing guide, design explanation, and `scripts/release.py` validation.

### Docs

- Decoupled Core documentation from Gradle plugin and state-management implementation details. Core docs now describe the generic state SPI and metadata resolution, and link to the Gradle plugin and `terracotta-state-filesystem` docs for frontend-specific examples.
- Removed Gradle DSL and build-tool assumptions from core KDoc (`StateSourceConfig`, `StateSourceFactory`, `TerracottaConfig`, `ProjectMetadataSource`).
- Tightened module focus in the `terracotta-state-filesystem` reference and the Gradle plugin Kotlin DSL guide so each page owns its own responsibilities and links across modules.
- Expanded the `terracotta-state-filesystem` documentation with a rewritten README, an expanded reference page, a how-to guide for replacing the filesystem backend, and an explanation of why YAML and file-backed persistence are the defaults.
- Generalized the Modrinth provider tutorial so registry-specific docs no longer depend on Gradle DSL syntax.

## [0.5.0] - 2026-07-12

See per-module changelogs for module-specific changes.

## [0.4.1] - 2026-07-12

### Fixed

- Republished as `0.4.1` to align Maven Central artifacts with the `v0.4.0` release tag. The initial `0.4.0` upload was published from a slightly earlier commit than the tagged release, so this patch ensures users receive the source reflected by the `v0.4.0` tag.

## [0.4.0] - 2026-07-12

See per-module changelogs for module-specific changes.

### Fixed

- Fixed the docs License page by copying the root `LICENSE` to `docs/LICENSE.md`, adding it to the Quick Start navigation, and updating the index link so the page is reachable and rendered correctly.

## [0.3.0] - 2026-07-12

### Docs

- Added "Navigating the Docs" to the Quick Start section so readers can understand the Diátaxis structure and find the right section.
- Added a complete set of repo documentation: reference pages for scripts, CI/CD, branches, project files, commit conventions, and documentation style; explanation pages for repo architecture, branch strategy, release design, and changelog design; and a first-contribution tutorial.
- Added integration documentation covering multi-provider publishing, provider configuration, integration design, and troubleshooting.
- Reorganized user documentation under `docs/content/modules/` and `docs/content/integration/` with Diátaxis sections for Core, Gradle Plugin, Modrinth Provider, and Hangar Provider; folded `docs/content/config/` into the Core reference.
- Added `@see` links and member-level KDoc to all public APIs in `terracotta-core`, `terracotta-gradle-plugin`, `terracotta-provider-modrinth`, and `terracotta-provider-hangar`, pointing to the new GitHub Pages docs.
- Removed the separate `docs/content/sdk/` section and folded its remaining pages into module docs: installation moved to `modules/core/tutorials/installation.md`, custom-provider content merged into `modules/core/tutorials/implementing-a-custom-provider.md`, provider API reference merged into `modules/core/reference/provider-interfaces.md`, architecture explanation merged into `modules/core/explanation/architecture.md`, and the Modrinth quick-start merged into `modules/provider-modrinth/tutorials/using-modrinth.md`.
- Added estimated reading time to every documentation page via `mkdocs-macros-plugin` and a custom Material content override.

### Fixed

- Fixed the pre-build hook destination so `LICENSE` is copied to `docs/LICENSE` instead of `docs/LICENSE.md`.
- Removed the CI/CD setup with GitHub Actions guide from `docs/content/integration/` because the workflow is not currently supported.
- Cleaned up `docs/content/repo/` by removing redundant pages, trimming the Diátaxis framework explanation, updating outdated navigation references, and removing remaining `SDK` terminology.
- Simplified module READMEs by removing redundant inline Diátaxis explanations and linking readers to the shared "Navigating the Docs" page.
- Enforced strict Diátaxis discipline in `docs/content/repo/`: moved architecture explanation to `docs/content/modules/core/explanation/architecture.md`, rewrote `project-management.md` to remove procedural steps, moved `changelog-guidelines.md` to `reference/`, and added a clear target-audience statement to the repo README.

### Repo

- Updated `.gitignore` to ignore the generated `docs/LICENSE` file instead of `docs/LICENSE.md`.

## [0.2.0] - 2026-07-11

### Docs

- Removed the `CI/CD Deployment` how-to guide from the docs navigation because the current stateless design does not support the documented workflow, preventing users from following an inaccurate guide.
- Moved the project introduction, installation, and usage content from `README.md` into `docs/index.md` so the docs site is the single source of truth for presentation content. `README.md` is now a lightweight index page that links to the documentation.
- Added a pre-build hook that copies `LICENSE` into `docs/LICENSE.md` so the docs site can link to the license without leaving the generated site.

### Repo

- Updated the release script to keep Gradle plugin version snippets in `docs/index.md` in sync and removed `README.md` version-string handling.

### Fixed

- Fixed the GitHub release workflow so it extracts release notes directly from `CHANGELOG.md` instead of relying on an external parser that produced empty bodies for recent releases.

## [0.1.4] - 2026-07-11

### Docs

- Added a new `Config` docs category with `Overview` and `Schema` pages so users can find the file-format reference independently of Gradle-plugin guides.

### Fixed

- Removed the stale `docs/overrides` reference from `mkdocs.yml` so docs deployments no longer fail with a missing custom_dir path.

## [0.1.3] - 2026-07-11

### Docs

- Added the smoke-testing release guide in plain language and added guidance on saving pytest JSON reports as metrics.

### Repo

- Archived smoke-test results as structured JSON metrics instead of manual tables.

### Changed

- Moved the Changes link from the custom docs header into the main navigation so it works easily on mobile.
- Reorganized changelog docs into a succinct explanation and a practical how-to guide, and updated `CHANGELOG.md` to use bold module headings.

### Fixed

- Fixed docs deployments so the live site reliably matches the latest generated release, preventing stale or mismatched content.
- Configured versioned docs to build from the release tag, so the docs site updates as soon as a release goes out.
- Synchronized version references across `README.md`, `CHANGELOG.md`, generated release notes, and `docs/content/**/*.md` as part of the release process.

### Repo

- Added pre-publish checks that abort if version references are out of sync with the release version.

### SDK

- Fixed publishing of `-javadoc.jar` artifacts so they include generated API documentation instead of being empty or missing, satisfying Maven Central requirements.
- Added pre-publish checks that abort if `-javadoc.jar` artifacts are missing or empty.

## [0.1.2] - 2026-07-10

### Fixed

- Fixed release tooling so each release leaves an empty "Unreleased" section in the changelog.
  - **Why**: Keeps the changelog ready for the next development cycle.
- Fixed docs deployment so routine pushes no longer reset the default site version from `latest` to `unreleased`.
  - **Why**: Prevents the root site from drifting away from the latest release.

## [0.1.1] - 2026-07-10

### Docs

- Added multi-module API reference links to the docs navigation.
  - **Why**: Provides API documentation for each module (core, gradle-plugin, provider-modrinth).
- Fixed broken internal links and replaced full URLs with relative links where appropriate.
  - **Why**: Ensures documentation is accurate and up-to-date.

### Repo

- Implemented automated GitHub releases on tag push in `release.yml`.
  - **Why**: Automates the release process and makes artifacts available to users.
- Added changelog parsing to the release workflow for release notes.
  - **Why**: Generates release notes automatically from `CHANGELOG.md`.
- Added JAR artifact uploads to the `ci.yml` workflow.
  - **Why**: Makes build artifacts available for verification and release.
- Added Dokka documentation generation to the `deploy-docs.yml` workflow.
  - **Why**: Generates API reference documentation automatically on every docs deploy.

### Fixed

- Fixed `version` in `build.gradle.kts` being hardcoded to `0.1.0` instead of reading from `gradle.properties`.
  - **Why**: Ensures the published version matches the intended release version.

## [0.1.0] - 2026-07-10

### Docs

- Added documentation website at `https://beduality.github.io/terracotta/`.
  - **Why**: Makes comprehensive, convenient, accessible documentation publicly available.
