---
description: Implementation plan for replacing free-form tags with structured TerracottaCategory / TerracottaProjectCategories types.
---

# Narrow Canonical Tags/Categories Type

This plan follows `module-development-workflow.md` for a **major feature / new public API** that
touches `terracotta-core`, `terracotta-gradle-plugin`, `terracotta-provider-modrinth`, `terracotta-provider-hangar`, and `docs`.

## Source of truth

- TODO item: `Add [Narrow Tags](./proposals/25-07-10-narrow-tags-canonical.md)` (`project/TODO.md`)
- Design proposal: `project/designs/25-07-10-narrow-tags-canonical.md`

## Progress summary

| Phase | Status | Notes |
|-------|--------|-------|
| System design | Completed | See Notes section for decisions |
| Contract | Completed | Core models, config, resolver, Gradle DSL, providers |
| Test-driven development | Completed | Existing tests updated to categories; new mapping tests can be added in follow-up |
| Implementation | Completed | Category mapping implemented alongside contract |
| Review | Completed | Auto-review and human review passed for public API change |
| Documentation | Completed | Config schema, models, DSL, provider tutorials, explanations updated; build and mkdocs verified |
| Push to remote | Completed | Merged via PR #2; CI passed |

## Phase 1: System design

- [x] Read `module-system-design-workflow.md`.
- [x] Read `project/designs/25-07-10-narrow-tags-canonical.md`.
- [x] Decide whether `TerracottaCategory` carries provider-agnostic IDs only or also platform-specific aliases.
- [x] Decide how the Gradle DSL exposes `categories` (nested block vs. direct assignment).
- [x] Decide how unknown / unsupported category IDs are handled (fail fast vs. warn and drop).
- [x] Confirm CurseForge mapping remains out of scope for this phase.
- [x] Complete design review checklist from `module-review-workflow.md`.
- [x] Update this plan with decisions that affect later phases.

## Phase 2: Contract

- [x] Read `module-contract-workflow.md`.
- [x] Add `TerracottaCategory` and `TerracottaProjectCategories` to `terracotta-core` with `@Serializable` and KDoc.
- [x] Replace `TerracottaProject.tags: List<String>` with `TerracottaProject.categories: TerracottaProjectCategories`.
- [x] Update `TerracottaConfig.tags` to `TerracottaConfig.categories`.
- [x] Update `ResolvedProjectMetadata.tags` to `ResolvedProjectMetadata.categories`.
- [x] Update the Gradle plugin `TerracottaExtension.tags` to a structured categories DSL.
- [x] Add KDoc for every new public symbol intended for Dokka.
- [x] Run `:<module>:compileKotlin` for affected modules.

## Phase 3: Test-driven development

- [x] Read `module-testing-workflow.md`.
- [x] Identify behavior and edge cases:
  - Primary category required; additional categories optional.
  - Empty additional categories.
  - Modrinth featured-tag limit (max 3: primary + 2 additional).
  - Hangar single category plus recognized tags (`addon`, `library`, `folia`).
  - Unknown category IDs fail fast or warn.
  - YAML/DSL round-trip serialization.
- [x] Update existing tests that construct `TerracottaProject` / `ResolvedProjectMetadata` with the new field.
- [x] Run tests and confirm they pass.

## Phase 4: Implementation

- [x] Read `module-implementation-workflow.md`.
- [x] Implement `TerracottaCategory` and `TerracottaProjectCategories` in `terracotta-core`.
- [x] Migrate `TerracottaProject`, `TerracottaConfig`, and `ResolvedProjectMetadata` from `tags` to `categories`.
- [x] Update `ProjectMetadataResolver` to map config categories into resolved metadata.
- [x] Update the Gradle plugin DSL to accept structured categories.
- [x] Implement Modrinth provider mapping (featured + additional categories).
- [x] Implement Hangar provider mapping (single category + optional tags).
- [x] Keep CurseForge mapping in design only; defer implementation to a later phase.
- [x] Run `:build :spotlessCheck`.
- [x] Refactor and add KDoc.

## Phase 5: Review

- [x] Read `module-review-workflow.md`.
- [x] Run code review checklist.
- [x] By default, confirm auto-review (tests, spotless, build) passes.
- [x] Escalate to human review because this changes public API and the Gradle DSL.
- [x] Update `CHANGELOG.md` to document the breaking change and migration path.

## Phase 6: Documentation

- [x] Read `module-documentation-workflow.md`.
- [x] Update `docs/content/modules/core/reference/config-schema.md` with the new `categories` field.
- [x] Update `docs/content/modules/core/reference/models.md` with `TerracottaCategory` and `TerracottaProjectCategories`.
- [x] Update `docs/content/modules/gradle-plugin/how-to-guides/kotlin-dsl-configuration.md` with the new categories DSL.
- [x] Update provider-specific docs (Modrinth, Hangar) with category mapping behavior.
- [x] Cross-link KDoc with `@see` where appropriate.
- [x] Final verification: `./gradlew build :spotlessCheck`.
- [x] Final verification: `mkdocs build --strict`.

## Phase 7: Push to remote

- [x] Push the branch or merge the pull request to `main`.
- [x] Verify remote CI passes after the push or merge.

## Notes

### Phase 1 decisions

- `TerracottaCategory` uses provider-agnostic IDs only; platform-specific aliases live in provider mapping code.
- Gradle DSL exposes `categories` as a nested block (patterned after `links`) with `primary { ... }` and `additional { ... }` containers.
- Unknown / unsupported category IDs fail fast with a clear error message at resolution or provider-mapping time.
- CurseForge mapping stays out of scope for this phase; the canonical model is designed so it can be added later.
- `TerracottaProject` will use `categories: TerracottaProjectCategories`; free-form `tags` is removed.
