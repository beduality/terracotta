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
| TDD | Not started | |
| Implementation | Not started | |
| Build / quality checks | Not started | |
| Documentation | Not started | |
| Review / release prep | Not started | |

## Phase 1: System design

- [x] Read design proposal.
- [x] Identify modules: `terracotta-core`, `terracotta-gradle-plugin`, `terracotta-provider-modrinth`, `terracotta-provider-hangar`.
- [ ] Confirm `licenseUrl` is single canonical value (no per-provider override yet).

## Phase 2: Test-driven development

- [ ] Add tests for YAML parsing of `licenseUrl`.
- [ ] Add tests for `TerracottaConfig` / `ResolvedProjectMetadata` resolution.
- [ ] Add tests for metadata merge and equality with `licenseUrl`.
- [ ] Add tests for Gradle DSL `licenseUrl` property wiring.
- [ ] Add tests for Modrinth create/patch payload including `license_url`.
- [ ] Add tests for Modrinth fetch deserializing `license.url`.
- [ ] Add tests for Hangar warning when `licenseUrl` is set.
- [ ] Run new tests and confirm they fail for the expected reason.

## Phase 3: Implementation

- [ ] Add `licenseUrl` to canonical model and metadata layers (`terracotta-core`).
- [ ] Add `licenseUrl` to `TerracottaConfig` and `TerracottaConfigLoader`.
- [ ] Add `licenseUrl` to `TerracottaExtension` and Gradle plugin wiring.
- [ ] Update `ResolvedProjectMetadata` to include `licenseUrl`.
- [ ] Update `ModrinthLicense`, `ModrinthProject`, `ModrinthStateProvider`, `ModrinthClient`, `DiffEngine`, `ModrinthRegistryProvider`.
- [ ] Add Hangar warning for unsupported `licenseUrl`.
- [ ] Run `:build :spotlessCheck`.

## Phase 4: Documentation

- [ ] Update `docs/content/modules/core/reference/config-schema.md`.
- [ ] Update generated KDoc references if needed.
- [ ] Run `mkdocs build --strict`.

## Phase 5: Review / release prep

- [ ] Run code review checklist.
- [ ] Update `CHANGELOG.md`.
- [ ] Final verification: `./gradlew build :spotlessCheck` and `mkdocs build --strict`.
- [ ] Mark TODO item as done.

## Notes

<!-- Decisions, blockers, and discoveries go here as work progresses. -->
