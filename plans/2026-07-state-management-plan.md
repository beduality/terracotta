---
description: Implementation plan for a pluggable state-management layer in terracotta-core, starting with a file-backed implementation.
---

# State Management Implementation Plan

This plan follows `module-development-workflow.md` for a **feature** that
touches `terracotta-core`, `terracotta-gradle-plugin`.

## Source of truth

- TODO item: Add [State Management](./proposals/2026-07-state-management.md) (`project/TODO.md`)
- Design proposal: `project/proposals/2026-07-state-management.md`

## Progress summary

| Phase | Status | Notes |
|-------|--------|-------|
| System design | Completed | Design proposal reviewed against checklist |
| TDD | Completed | 5 tests in FileSystemStateSourceTest |
| Implementation | Completed | Core + Gradle DSL wiring |
| Review / release prep | Completed | CHANGELOG updated, green build |
| Documentation | Completed | Explanation + DSL guide + cross-links |

## Phase 1: System design

- [x] Read `module-system-design-workflow.md`.
- [x] Read or write the design proposal.
- [x] Complete design review checklist from `module-review-workflow.md`.
- [x] Update this plan with decisions that affect later phases.

## Phase 2: Test-driven development

- [x] Read `module-testing-workflow.md`.
- [x] Identify behavior and edge cases, including `java.time.Instant` serialization.
- [x] Write failing tests.
- [x] Run tests and confirm they fail for the expected reason.

## Phase 3: Implementation

- [x] Read `module-implementation-workflow.md`.
- [x] Make tests pass with the smallest change.
- [x] Refactor and add KDoc.
- [x] Add `.terracotta-state.yml` to `.gitignore`.
- [x] Verify `java.time.Instant` round-trips through the YAML codec.
- [x] Run `./gradlew :terracotta-core:build :terracotta-gradle-plugin:build :spotlessCheck`.

## Phase 4: Review / release prep

- [x] Read `module-review-workflow.md`.
- [x] Run code review checklist.
- [x] Update `CHANGELOG.md`.
- [x] Final verification: `./gradlew build :spotlessCheck` and `mkdocs build --strict`.

## Phase 5: Documentation

- [x] Read `module-documentation-workflow.md`.
- [x] Add or update Diátaxis docs, including that `.terracotta-state.yml` should not be committed.
- [x] Cross-link KDoc with `@see`.
- [x] Run `mkdocs build --strict`.

## Notes

- Design proposal `project/proposals/2026-07-state-management.md` is already written; Phase 1 is mainly review and checklist completion.
- Design review fixed an overload conflict in `FileSystemStateSource`: the original secondary constructor `constructor(directory: Path)` conflicted with the primary `constructor(file: Path)` on the JVM. Replaced with companion factory methods `forFile` and `forDirectory`.
- SpotlessApply cleaned up pre-existing unused imports in `TerracottaProject.kt` and `TerracottaConfigLoaderLinksTest.kt` so the full `:spotlessCheck` passes.
- The state file is not yet wired into `terracottaPlan`/`terracottaApply` task actions because diff identity consumers are out of scope for this change. The Gradle plugin exposes the `stateFile` DSL property and convention; task-level load/save will follow once state consumers exist.

<!-- Decisions, blockers, and discoveries go here as work progresses. -->
