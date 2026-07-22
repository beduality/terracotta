# Changelog

Repo-wide activity log. Module-specific changes live in
`modules/<module>/CHANGELOG.md`; documentation changes in
`docs/CHANGELOG.md`.

## 2026-07-22

Refactored the release process for per-module selective publishing, cleaned up the deployment manifest, and fixed historical changelog and deployment data issues.

### Added

- Added per-module selective release with independent versioning: each module has its own `gradle.properties`, `CHANGELOG.md`, and Maven Central publishing configuration.
- Added `abort`, `monitor`, and `trigger` subcommands to `release.py` for managing release workflow runs from the CLI.
- Added `--modules` flag to release specific modules, bypassing change detection.
- Added `--since` flag to filter change detection by a git ref.
- Added support for versionless deployment entries (e.g. infrastructure applies, documentation site deploys) in `deployments.json`.
- Added multiple module badge filter selection on the Last Changes page.

### Changed

- Replaced monolithic versioning and release workflow with per-module change detection, tagging, and publishing.
- Standard version bumps from a prerelease now produce a stable version (e.g. `0.8.0-beta.1` + patch → `0.8.1`).
- Refined `deployments.json` manifest: removed pseudo-modules (`docs`, `repo`, `release-pipeline`), added `github` module to historical entries, added 2 versionless pre-0.1.0 Pulumi deployments, and added 2 docs site deployment entries.
- Reordered Last Changes page header: title, version badge, module icons, release tag, date.

### Fixed

- Corrected historical changelog entries: method name `updateProject` → `patchProject` in Modrinth 0.7.0, warning attribution in Hangar 0.5.0, past-tense verbs in Hangar 0.7.0, inlined `**Why**:` lines across core 0.1.0, gradle-plugin 0.1.1, and modrinth 0.1.1.
- Backfilled missing release summaries for all pre-0.6.0 entries across core, gradle-plugin, hangar, and modrinth changelogs.
- Corrected `deployments.json` data: `0.4.1` missing `core` module, `0.4.0` title and summary referencing project icons (introduced in 0.5.0), sort order for versionless entries, and exact GitHub release `publishedAt` timestamps replacing rounded midnight values.

### Removed

- Removed dead code from `deployments.py`: `PSEUDO_MODULES`, `extract_modules`, `seed_from_changelog`, and `seed`/`generate` CLI commands.
- Removed `MODULE_ALIASES` and dead pseudo-module labels/icons from `macros.py`.

## 2026-07-21

Split the monolithic changelog into per-module files and hardened release version validation.

### Changed

- Split the monolithic `CHANGELOG.md` into per-module changelogs under `modules/<module>/CHANGELOG.md` so each module's release history is self-contained.
- Replaced custom regex-based semver parsing in `release.py` with the `semver` Python library for version bumping and validation.

### Added

- Added `validate_next_version` to `release.py` so custom version bumps are restricted to the next valid patch, minor, or major. Rejects downgrades, same-version re-releases, and version jumps.
- Added 17 dry-run tests and 13 version validation unit tests for the release command.

### Fixed

- Fixed unknown module names raising `KeyError` instead of `ValueError` when passed via `--modules`.

## 2026-07-13

Tightened the changelog standard to require release summaries and reject stale unreleased wording.

### Changed

- Tightened changelog standard: required release summaries, direct wording validation in `release.py`, corrected 0.7.0 changelog summary.

## 2026-07-12

Introduced required release summaries in the changelog standard and fixed docs deployment housekeeping.

### Added

- Added required release summary paragraph to the changelog standard.

### Fixed

- Updated `.gitignore` for generated `docs/LICENSE` file.

## 2026-07-11

Improved release automation with docs snippet validation, smoke-test archiving, and JAR artifact checks.

### Changed

- Updated release script for Gradle plugin version snippets in `docs/index.md`.
- Archived smoke-test results as structured JSON metrics.
- Added pre-publish checks for version sync and `-javadoc.jar` artifacts.

### Fixed

- Fixed GitHub release workflow to extract notes from `CHANGELOG.md` instead of external parser.
- Fixed `-javadoc.jar` publishing to include generated API docs.

## 2026-07-10

Shipped automated GitHub releases on tag push with JAR artifact uploads and Dokka API docs in CI.

### Added

- Implemented automated GitHub releases on tag push, JAR artifact uploads, Dokka generation in CI.

### Fixed

- Fixed release tooling to leave an empty "Unreleased" section after each release.
