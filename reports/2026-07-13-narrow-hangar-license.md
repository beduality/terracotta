# Release Report: Narrow Hangar License Integration

**Release version:** v0.7.0
**Release date:** 2026-07-13
**Plan:** `project/plans/archived/2026-07-narrow-hangar-license-plan.md`
**Design:** `project/designs/archived/26-07-12-narrow-license-hangar.md`
**Pull request:** https://github.com/beduality/terracotta/pull/4
**Git tag:** https://github.com/beduality/terracotta/releases/tag/v0.7.0

## Summary

This release fixes Hangar license handling by mapping common SPDX identifiers to Hangar's license dropdown values and preventing `licenseUrl` from causing a recurring metadata diff on providers that cannot persist it.

## Changes

- Added `ProviderLogic.supportsLicenseUrl` capability with a default implementation of `true`.
- Updated `DiffEngine.diff` to ignore `licenseUrl` differences when the provider reports it is unsupported.
- Added `HangarLicenseMapper` to translate SPDX identifiers such as `Apache-2.0` and `GPL-3.0-only` to Hangar license values.
- Wired `HangarLicenseMapper` into `HangarStateProvider` and `HangarRegistryProvider`.
- Set `HangarProviderLogic.supportsLicenseUrl = false` and removed the recurring `licenseUrl` warning.
- Updated `TerracottaPlanTask` and `TerracottaApplyTask` to pass the provider capability to `DiffEngine`.
- Added unit tests for `HangarLicenseMapper` and `DiffEngine` licenseUrl behavior.
- Updated user-facing documentation and `CHANGELOG.md`.

## Verification

- `./gradlew build :spotlessCheck` passed.
- `uv run mkdocs build --strict` passed.
- `TerracottaPluginIntegrationTest` passed.
- CI workflow `Build & Verify` passed on `main`.
- Release workflow published `v0.7.0` to Maven Central and created a GitHub Release.
- Docs deployment workflow updated the live documentation site.

## Artifacts

- Source code: tag `v0.7.0`
- Maven Central artifacts for `terracotta-core`, `terracotta-gradle-plugin`, `terracotta-provider-hangar`, `terracotta-provider-modrinth`, and `terracotta-state-filesystem`.
- GitHub Release notes extracted from `CHANGELOG.md`.
