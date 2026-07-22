# Changelog

Repo-wide activity log. Module-specific changes live in
`modules/<module>/CHANGELOG.md`; documentation changes in
`docs/CHANGELOG.md`. This file is ephemeral — old entries are
pruned periodically. See git history for the full record.

## [Unreleased]

### Changed

- Split the monolithic `CHANGELOG.md` into per-module changelogs under `modules/<module>/CHANGELOG.md` so each module's release history is self-contained.
- Replaced custom regex-based semver parsing in `release.py` with the `semver` Python library for version bumping and validation.

### Added

- Added `validate_next_version` to `release.py` so custom version bumps are restricted to the next valid patch, minor, or major. Rejects downgrades, same-version re-releases, and version jumps.
- Added 17 dry-run tests and 13 version validation unit tests for the release command.

### Fixed

- Fixed unknown module names raising `KeyError` instead of `ValueError` when passed via `--modules`.

## 2026-07-13

- Tightened changelog standard: required release summaries, direct wording validation in `release.py`, corrected 0.7.0 changelog summary.

## 2026-07-12

- Added required release summary paragraph to the changelog standard.
- Updated `.gitignore` for generated `docs/LICENSE` file.

## 2026-07-11

- Updated release script for Gradle plugin version snippets in `docs/index.md`.
- Fixed GitHub release workflow to extract notes from `CHANGELOG.md` instead of external parser.
- Archived smoke-test results as structured JSON metrics.
- Added pre-publish checks for version sync and `-javadoc.jar` artifacts.
- Fixed `-javadoc.jar` publishing to include generated API docs.

## 2026-07-10

- Implemented automated GitHub releases on tag push, JAR artifact uploads, Dokka generation in CI.
- Fixed release tooling to leave an empty "Unreleased" section after each release.
