# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Per-module changelogs live alongside each module under `modules/<module>/CHANGELOG.md`.
Documentation changes are tracked in `docs/CHANGELOG.md`.
This root changelog tracks repository-wide changes (CI/CD, tooling, conventions).

## [Unreleased]

### Changed

- Split the monolithic `CHANGELOG.md` into per-module changelogs under `modules/<module>/CHANGELOG.md` so each module's release history is self-contained. The root changelog now only tracks repo-wide changes (CI/CD, tooling, conventions).
- Replaced custom regex-based semver parsing in `release.py` with the `semver` Python library for version bumping and validation.

### Added

- Added `validate_next_version` to `release.py` so custom version bumps are restricted to the next valid patch, minor, or major. Rejects downgrades, same-version re-releases, and version jumps.
- Added 17 dry-run tests and 13 version validation unit tests for the release command.

### Fixed

- Fixed unknown module names raising `KeyError` instead of `ValueError` when passed via `--modules`.

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

## [0.6.0] - 2026-07-12

Introduces pluggable state management and canonical project links. See per-module changelogs for module-specific changes.

### Added

- Added a required release summary paragraph to the changelog standard. Every release section, including `[Unreleased]`, must now start with a summary before the first category heading. Updated the changelog guidelines, writing guide, design explanation, and `scripts/release.py` validation.

## [0.5.0] - 2026-07-12

See per-module changelogs for module-specific changes.

## [0.4.1] - 2026-07-12

### Fixed

- Republished as `0.4.1` to align Maven Central artifacts with the `v0.4.0` release tag. The initial `0.4.0` upload was published from a slightly earlier commit than the tagged release, so this patch ensures users receive the source reflected by the `v0.4.0` tag.

## [0.4.0] - 2026-07-12

See per-module changelogs for module-specific changes.

## [0.3.0] - 2026-07-12

### Changed

- Updated `.gitignore` to ignore the generated `docs/LICENSE` file instead of `docs/LICENSE.md`.

## [0.2.0] - 2026-07-11

### Changed

- Updated the release script to keep Gradle plugin version snippets in `docs/index.md` in sync and removed `README.md` version-string handling.

### Fixed

- Fixed the GitHub release workflow so it extracts release notes directly from `CHANGELOG.md` instead of relying on an external parser that produced empty bodies for recent releases.

## [0.1.4] - 2026-07-11

No repo-wide changes in this release.

## [0.1.3] - 2026-07-11

### Added

- Archived smoke-test results as structured JSON metrics instead of manual tables.
- Added pre-publish checks that abort if version references are out of sync with the release version.
- Added pre-publish checks that abort if `-javadoc.jar` artifacts are missing or empty.

### Fixed

- Fixed publishing of `-javadoc.jar` artifacts so they include generated API documentation instead of being empty or missing, satisfying Maven Central requirements.
- Synchronized version references across `README.md`, `CHANGELOG.md`, generated release notes, and `docs/content/**/*.md` as part of the release process.

## [0.1.2] - 2026-07-10

### Fixed

- Fixed release tooling so each release leaves an empty "Unreleased" section in the changelog.
  - **Why**: Keeps the changelog ready for the next development cycle.

## [0.1.1] - 2026-07-10

### Added

- Implemented automated GitHub releases on tag push in `release.yml`.
- Added changelog parsing to the release workflow for release notes.
- Added JAR artifact uploads to the `ci.yml` workflow.
- Added Dokka documentation generation to the `deploy-docs.yml` workflow.

### Fixed

- Fixed `version` in `build.gradle.kts` being hardcoded to `0.1.0` instead of reading from `gradle.properties`.

## [0.1.0] - 2026-07-10

Initial release.
