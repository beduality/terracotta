---
description: Implementation plan for replacing free-form tags with structured TerracottaCategory / TerracottaProjectCategories types.
---

# Narrow Canonical Tags/Categories Type

This plan follows `module-development-workflow.md` for a **major feature / new public API** that
touches `terracotta-core`, `terracotta-gradle-plugin`, `terracotta-provider-modrinth`, `terracotta-provider-hangar`, and `docs`.

## Source of truth

- TODO item: `Add [Narrow Tags](./proposals/25-07-10-narrow-tags-canonical.md)` (`project/TODO.md`)
- Design proposal: `project/proposals/25-07-10-narrow-tags-canonical.md`

## Progress summary

| Phase | Status | Notes |
|-------|--------|-------|
| System design | Not started | |
| Contract | Not started | |
| Test-driven development | Not started | |
| Implementation | Not started | |
| Review | Not started | |
| Documentation | Not started | |
| Push to remote | Not started | |

## Phase 1: System design

- [ ] Read `module-system-design-workflow.md`.
- [ ] Read `project/proposals/25-07-10-narrow-tags-canonical.md`.
- [ ] Decide whether `TerracottaCategory` carries provider-agnostic IDs only or also platform-specific aliases.
- [ ] Decide how the Gradle DSL exposes `categories` (nested block vs. direct assignment).
- [ ] Decide how unknown / unsupported category IDs are handled (fail fast vs. warn and drop).
- [ ] Confirm CurseForge mapping remains out of scope for this phase.
- [ ] Complete design review checklist from `module-review-workflow.md`.
- [ ] Update this plan with decisions that affect later phases.

## Phase 2: Contract

- [ ] Read `module-contract-workflow.md`.
- [ ] Add `TerracottaCategory` and `TerracottaProjectCategories` to `terracotta-core` with `@Serializable` and KDoc.
- [ ] Replace `TerracottaProject.tags: List<String>` with `TerracottaProject.categories: TerracottaProjectCategories`.
- [ ] Update `TerracottaConfig.tags` to `TerracottaConfig.categories`.
- [ ] Update `ResolvedProjectMetadata.tags` to `ResolvedProjectMetadata.categories`.
- [ ] Update the Gradle plugin `TerracottaExtension.tags` to a structured categories DSL.
- [ ] Add KDoc for every new public symbol intended for Dokka.
- [ ] Run `:<module>:compileKotlin` for affected modules.

## Phase 3: Test-driven development

- [ ] Read `module-testing-workflow.md`.
- [ ] Identify behavior and edge cases:
  - Primary category required; additional categories optional.
  - Empty additional categories.
  - Modrinth featured-tag limit (max 3: primary + 2 additional).
  - Hangar single category plus recognized tags (`addon`, `library`, `folia`).
  - Unknown category IDs fail fast or warn.
  - YAML/DSL round-trip serialization.
- [ ] Add failing tests for `TerracottaCategory`, `TerracottaProjectCategories`, and config/project serialization.
- [ ] Add failing tests for Modrinth category mapping (featured vs. additional).
- [ ] Add failing tests for Hangar category/tag mapping.
- [ ] Update existing tests that construct `TerracottaProject` / `ResolvedProjectMetadata` with the new field.
- [ ] Run tests and confirm they fail for the expected reason.

## Phase 4: Implementation

- [ ] Read `module-implementation-workflow.md`.
- [ ] Implement `TerracottaCategory` and `TerracottaProjectCategories` in `terracotta-core`.
- [ ] Migrate `TerracottaProject`, `TerracottaConfig`, and `ResolvedProjectMetadata` from `tags` to `categories`.
- [ ] Update `ProjectMetadataResolver` to map config categories into resolved metadata.
- [ ] Update the Gradle plugin DSL to accept structured categories.
- [ ] Implement Modrinth provider mapping (featured + additional categories).
- [ ] Implement Hangar provider mapping (single category + optional tags).
- [ ] Keep CurseForge mapping in design only; defer implementation to a later phase.
- [ ] Run `:build :spotlessCheck`.
- [ ] Refactor and add KDoc.

## Phase 5: Review

- [ ] Read `module-review-workflow.md`.
- [ ] Run code review checklist.
- [ ] By default, confirm auto-review (tests, spotless, build, docs build) passes.
- [ ] Escalate to human review because this changes public API and the Gradle DSL.
- [ ] Update `CHANGELOG.md` to document the breaking change and migration path.

## Phase 6: Documentation

- [ ] Read `module-documentation-workflow.md`.
- [ ] Update `docs/content/modules/core/reference/config-schema.md` with the new `categories` field.
- [ ] Update `docs/content/modules/core/reference/models.md` with `TerracottaCategory` and `TerracottaProjectCategories`.
- [ ] Update `docs/content/modules/gradle-plugin/how-to-guides/kotlin-dsl-configuration.md` with the new categories DSL.
- [ ] Update provider-specific docs (Modrinth, Hangar) with category mapping behavior.
- [ ] Cross-link KDoc with `@see` where appropriate.
- [ ] Final verification: `./gradlew build :spotlessCheck` and `mkdocs build --strict`.

## Phase 7: Push to remote

- [ ] Push the branch or merge the pull request to `main`.
- [ ] Verify remote CI passes after the push or merge.

## Notes

<!-- Decisions, blockers, and discoveries go here as work progresses. -->
