---
description: Add a first-class links field to TerracottaProject and TerracottaConfig so users declare project URLs once and each provider maps them to its native format.
---

# Plan: Canonical Project Links

This plan follows `module-development-workflow.md` for a **new public API** that
touches `terracotta-core`, `terracotta-gradle-plugin`, `terracotta-provider-modrinth`, and `terracotta-provider-hangar`.

## Source of truth

- TODO item: `Add Canonical Project Links` (`project/TODO.md:10`)
- Design proposal: `project/proposals/2026-07-canonical-project-links.md`

## Progress summary

| Phase | Status | Notes |
|-------|--------|-------|
| System design | Not started | |
| Test-driven development | Not started | |
| Implementation | Not started | |
| Review / release prep | Not started | |
| Documentation | Not started | |

## Phase 1: System design

- [ ] Read `module-system-design-workflow.md`.
- [ ] Read `project/proposals/2026-07-canonical-project-links.md`.
- [ ] Finalize the canonical model (`TerracottaProjectLinks`, `TerracottaDonationLink`) and its merge semantics.
- [ ] Confirm provider mapping rules for Modrinth, Hangar, and CurseForge (CurseForge mapping is design-only until its provider exists).
- [ ] Complete design review checklist from `module-review-workflow.md`.
- [ ] Update this plan with decisions that affect later phases (e.g., `other` type, donation validation strategy).

## Phase 2: Test-driven development

- [ ] Read `module-testing-workflow.md`.
- [ ] Identify behavior and edge cases: empty links, partial links, donation platform validation, `other` ordering, provider-specific dropping.
- [ ] Write failing tests for `TerracottaProjectLinks` serialization, merge semantics, and Gradle DSL generation.
- [ ] Write failing provider tests for Modrinth and Hangar mapping.
- [ ] Run tests and confirm they fail for the expected reason.

## Phase 3: Implementation

- [ ] Read `module-implementation-workflow.md`.
- [ ] Create `TerracottaProjectLinks` and `TerracottaDonationLink` in `terracotta-core`.
- [ ] Add `links` to `TerracottaProject`, `TerracottaConfig`, and `AbstractProjectMetadata`/`ProjectMetadata` with default values.
- [ ] Implement Gradle DSL `links { ... }` block in `TerracottaExtension` and `TerracottaProjectSpec`.
- [ ] Implement Modrinth read/write mapping.
- [ ] Implement Hangar write mapping.
- [ ] Implement CurseForge write mapping design (or stub until the provider is added).
- [ ] Make tests pass with the smallest change.
- [ ] Refactor and add KDoc.
- [ ] Run `:build :spotlessCheck`.

## Phase 4: Review / release prep

- [ ] Read `module-review-workflow.md`.
- [ ] Run code review checklist.
- [ ] Update `CHANGELOG.md` with the new links model and DSL.
- [ ] Final verification: `./gradlew build :spotlessCheck`.

## Phase 5: Documentation

- [ ] Read `module-documentation-workflow.md`.
- [ ] Update `docs/content/modules/core/reference/models.md` with `TerracottaProjectLinks`.
- [ ] Update `docs/content/modules/core/reference/config-schema.md` with the `links` YAML block.
- [ ] Update `docs/content/modules/provider-modrinth/reference/api.md` with mapping details.
- [ ] Update `docs/content/modules/provider-hangar/reference/api.md` with the default "Links" section mapping.
- [ ] Add a CurseForge mapping note when its provider docs are created.
- [ ] Cross-link KDoc with `@see`.
- [ ] Run `mkdocs build --strict`.

## Notes

<!-- Decisions, blockers, and discoveries go here as work progresses. -->
