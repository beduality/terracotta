# Release Report: Stabilize Gallery Item Identity via Persisted State

**Release version:** v0.8.0
**Release date:** 2026-07-13
**Plan:** `project/plans/archived/2026-07-stabilize-gallery-item-identity-plan.md`
**Design:** `project/designs/archived/26-07-12-gallery-item-identity.md`
**Pull request:** https://github.com/beduality/terracotta/pull/5
**Git tag:** https://github.com/beduality/terracotta/releases/tag/v0.8.0

## Summary

This release stabilizes gallery item identity using persisted state so images can be renamed or reordered without triggering a delete-and-reupload cycle.

## Changes

- Added optional `key` property to `TerracottaGalleryItem` and the Gradle DSL gallery extension.
- Added `GalleryIdentityReporter` provider capability and a `galleryLocalKey(item)` helper.
- Updated `DiffEngine.diff` with a persisted-gallery overload that matches local items to remote items by stable local key, falling back to title/ordering matching when no persisted identity exists.
- Updated `TerracottaPlanTask` and `TerracottaApplyTask` to load persisted gallery state, detect duplicate local keys, pass identities to `DiffEngine`, and save updated state after apply.
- Implemented `GalleryIdentityReporter` in `ModrinthRegistryProvider`; `ModrinthClient.uploadGalleryItem` now returns the created remote URL.
- Added unit and integration tests for config loading, diff engine gallery matching, Modrinth identity reporting, and Gradle task state-source wiring.
- Updated user-facing documentation and `CHANGELOG.md`.

## Verification

- `./gradlew check` passed.
- `./gradlew spotlessCheck` passed.
- `uv run mkdocs build --strict` passed.
- CI workflow `Build & Verify` passed on PR and `main`.
- Release workflow published `v0.8.0` to Maven Central and created a GitHub Release.
- Docs deployment workflow updated the live documentation site.

## Artifacts

- Source code: tag `v0.8.0`
- Maven Central artifacts for `terracotta-core`, `terracotta-gradle-plugin`, `terracotta-provider-modrinth`, `terracotta-provider-hangar`, and `terracotta-state-filesystem`.
- GitHub Release notes extracted from `CHANGELOG.md`.
