# Changelog

Repo-wide activity log. Module-specific changes live in
`modules/<module>/CHANGELOG.md`; documentation changes in
`docs/CHANGELOG.md`.

## 2026-07-22

Refactors the release process for per-module selective publishing, cleans up the deployment manifest, and fixes historical changelog and deployment data issues.

### Fixed

- Fixed incorrect method name `updateProject` → `patchProject` in Modrinth 0.7.0 changelog.
- Corrected warning attribution in Hangar 0.5.0 changelog from `HangarRegistryProvider` to `BaseRegistryProvider`.
- Fixed Hangar 0.7.0 changelog entries to start with past-tense verbs per changelog guidelines.
- Removed `**Why**:` lines from core 0.1.0, gradle-plugin 0.1.1, and modrinth 0.1.1 changelogs; inlined reasons into each entry.
- Backfilled missing release summaries for all pre-0.6.0 entries across core, gradle-plugin, hangar, and modrinth changelogs.
- Fixed `0.4.1` deployment entry missing `core` in its modules list.
- Corrected `0.4.0` deployment title and summary that incorrectly referenced project icons (introduced in 0.5.0).
- Fixed `deployments.json` sort order: versioned entries now precede versionless entries, and versionless entries sort descending by `createdAt`.
- Replaced rounded midnight timestamps in `deployments.json` with exact GitHub release `publishedAt` times for all versioned entries.

### Changed

- Refined `deployments.json` manifest: removed pseudo-modules (`docs`, `repo`, `release-pipeline`), added `github` module to historical entries, added 2 versionless pre-0.1.0 Pulumi deployments, and added 2 docs site deployment entries.
- Made `version` optional in deployment entries to support non-versioned deployments (e.g. infrastructure applies).
- Reordered Last Changes page header: title, version badge, module icons, release tag, date.
- Enabled multiple module badge filters simultaneously on the Last Changes page.

### Removed

- Removed dead code from `deployments.py`: `PSEUDO_MODULES`, `extract_modules`, `seed_from_changelog`, and `seed`/`generate` CLI commands.
- Removed `MODULE_ALIASES` and dead pseudo-module labels/icons from `macros.py`.

## 2026-07-21

Splits the monolithic changelog into per-module files and hardens release version validation.

### Changed

- Split the monolithic `CHANGELOG.md` into per-module changelogs under `modules/<module>/CHANGELOG.md` so each module's release history is self-contained.
- Replaced custom regex-based semver parsing in `release.py` with the `semver` Python library for version bumping and validation.

### Added

- Added `validate_next_version` to `release.py` so custom version bumps are restricted to the next valid patch, minor, or major. Rejects downgrades, same-version re-releases, and version jumps.
- Added 17 dry-run tests and 13 version validation unit tests for the release command.

### Fixed

- Fixed unknown module names raising `KeyError` instead of `ValueError` when passed via `--modules`.

## 2026-07-13

Tightens the changelog standard to require release summaries and reject stale unreleased wording.

- Tightened changelog standard: required release summaries, direct wording validation in `release.py`, corrected 0.7.0 changelog summary.

## 2026-07-12

Introduces required release summaries in the changelog standard and fixes docs deployment housekeeping.

- Added required release summary paragraph to the changelog standard.
- Updated `.gitignore` for generated `docs/LICENSE` file.

## 2026-07-11

Improves release automation with docs snippet validation, smoke-test archiving, and JAR artifact checks.

- Updated release script for Gradle plugin version snippets in `docs/index.md`.
- Fixed GitHub release workflow to extract notes from `CHANGELOG.md` instead of external parser.
- Archived smoke-test results as structured JSON metrics.
- Added pre-publish checks for version sync and `-javadoc.jar` artifacts.
- Fixed `-javadoc.jar` publishing to include generated API docs.

## 2026-07-10

Ships automated GitHub releases on tag push with JAR artifact uploads and Dokka API docs in CI.

- Implemented automated GitHub releases on tag push, JAR artifact uploads, Dokka generation in CI.
- Fixed release tooling to leave an empty "Unreleased" section after each release.
