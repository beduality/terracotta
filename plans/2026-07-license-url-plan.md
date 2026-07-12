---
description: Implementation plan for adding licenseUrl support across core, Gradle DSL, Modrinth, and Hangar.
---

# License URL Support Plan

This plan follows `module-development-workflow.md` for a **new public API / feature** that
touches `terracotta-core`, `terracotta-gradle-plugin`, `terracotta-provider-modrinth`, and `terracotta-provider-hangar`.

## Source of truth

- TODO item: `Add support for License URL` (`project/TODO.md`)
- Design proposal: `project/proposals/2026-07-license-url.md`

## Progress summary

| Phase | Status | Notes |
|-------|--------|-------|
| System design | Completed | Proposal approved |
| TDD | Completed | Failing tests added before implementation; all passing now |
| Implementation | Completed | `licenseUrl` added to core, Gradle DSL, Modrinth, and Hangar |
| Build / quality checks | Completed | `./gradlew build :spotlessCheck` passed |
| Documentation | Completed | Config schema updated; `mkdocs build --strict` passed |
| Review / release prep | Completed | `CHANGELOG.md` updated |

## Phase 1: System design

- [x] Read design proposal.
- [x] Identify modules: `terracotta-core`, `terracotta-gradle-plugin`, `terracotta-provider-modrinth`, `terracotta-provider-hangar`.
- [x] Confirm `licenseUrl` is single canonical value (no per-provider override yet).

## Phase 2: Test-driven development

- [x] Add tests for YAML parsing of `licenseUrl`.
- [x] Add tests for `TerracottaConfig` / `ResolvedProjectMetadata` resolution.
- [x] Add tests for metadata merge and equality with `licenseUrl`.
- [x] Add tests for Gradle DSL `licenseUrl` property wiring.
- [x] Add tests for Modrinth create/patch payload including `license_url`.
- [x] Add tests for Modrinth fetch deserializing `license.url`.
- [x] Add tests for Hangar warning when `licenseUrl` is set.
- [x] Run new tests and confirm they fail for the expected reason.

## Phase 3: Implementation

- [x] Add `licenseUrl` to canonical model and metadata layers (`terracotta-core`).
- [x] Add `licenseUrl` to `TerracottaConfig` and `TerracottaConfigLoader`.
- [x] Add `licenseUrl` to `TerracottaExtension` and Gradle plugin wiring.
- [x] Update `ResolvedProjectMetadata` to include `licenseUrl`.
- [x] Update `ModrinthLicense`, `ModrinthProject`, `ModrinthStateProvider`, `ModrinthClient`, `DiffEngine`, `ModrinthRegistryProvider`.
- [x] Add Hangar warning for unsupported `licenseUrl`.
- [x] Run `:build :spotlessCheck`.

## Phase 4: Documentation

- [x] Update `docs/content/modules/core/reference/config-schema.md`.
- [x] Update generated KDoc references if needed.
- [x] Run `mkdocs build --strict`.

## Phase 5: Review / release prep

- [x] Run code review checklist.
- [x] Update `CHANGELOG.md`.
- [x] Final verification: `./gradlew build :spotlessCheck` and `mkdocs build --strict`.
- [x] Mark TODO item as done.

## Notes

- Kept `licenseUrl` as a single canonical value; per-provider overrides remain out of scope until the provider-specific logic layer is implemented.
- Modrinth `license_url` is only emitted when the local value is non-null, preserving existing remote URLs when not configured.
- URLs are compared exactly in the diff engine, matching Modrinth's case-sensitive path handling.
