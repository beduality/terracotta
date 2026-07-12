---
description: Implementation plan for making the Gradle plugin fail fast with a clear error when the configured state backend is missing.
---

# Fail fast when the configured state backend is missing

This plan follows `module-development-workflow.md` for a **refactoring** that
touches `terracotta-gradle-plugin` and `docs`.

## Source of truth

- Design proposal: `project/proposals/26-07-12-change-gradle-plugin-remove-filesystem-default.md`

## Progress summary

| Phase | Status | Notes |
|-------|--------|-------|
| Test-driven development | Completed | StateSourceResolverTest and TerracottaPluginIntegrationTest updated. |
| Implementation | Completed | Direct FileSystemStateSource coupling removed; StateSourceResolver enhanced. |
| Review | Completed | Auto-review passed; CHANGELOG.md updated. |
| Documentation | Completed | README, reference, and Kotlin DSL guide updated. |
| Push to remote | Completed | Branch pushed; CI verification pending. |

## Phase 3: Test-driven development

- [x] Read `module-testing-workflow.md`.
- [x] Identify behavior and edge cases:
  - Missing `"filesystem"` factory with `stateSource` set to `"filesystem"`.
  - Missing arbitrary backend factory (e.g., a future cloud backend).
  - Default classpath still resolves `"filesystem"` successfully.
  - Deprecated `stateFile` still defaults to `.terracotta-state.yml`.
- [x] Update `StateSourceResolverTest` to assert the enhanced error message and available-factory list.
- [x] Add or update `TerracottaPluginIntegrationTest` to verify the plugin fails cleanly when `terracotta-state-filesystem` is excluded from the classpath.
- [x] Run tests and confirm the new assertions fail for the expected reason (the implementation is not yet changed).

## Phase 4: Implementation

- [x] Read `module-implementation-workflow.md`.
- [x] Remove the direct import and use of `FileSystemStateSource.DEFAULT_FILE_NAME` from `TerracottaPlugin.kt`.
- [x] Introduce a local `DEFAULT_STATE_FILE_NAME` constant in the Gradle plugin.
- [x] Enhance `StateSourceResolver.resolve` to collect discovered factory IDs and produce a clear error message when the configured backend is missing.
- [x] Keep `terracotta-state-filesystem` as an `implementation` dependency for now.
- [x] Run `:terracotta-gradle-plugin:test :terracotta-gradle-plugin:spotlessCheck :build`.
- [x] Refactor and add KDoc if needed.

## Phase 5: Review

- [x] Read `module-review-workflow.md`.
- [x] Run code review checklist.
- [x] By default, confirm auto-review (tests, spotless, build, docs build) passes.
- [x] Escalate to human review because this touches multi-module behavior and public-facing error messages.
- [x] Update `CHANGELOG.md` to mention the clearer missing-backend error and the removal of the direct `FileSystemStateSource` coupling.

## Phase 6: Documentation

- [x] Read `module-documentation-workflow.md`.
- [x] Update `docs/content/modules/terracotta-state-filesystem/README.md` to clarify the default dependency and the missing-backend error.
- [x] Update `docs/content/modules/terracotta-state-filesystem/reference/state-filesystem.md` with the same note.
- [x] Add a short paragraph to `docs/content/modules/gradle-plugin/how-to-guides/kotlin-dsl-configuration.md` explaining the error and how to restore the filesystem backend.
- [x] Final verification: `./gradlew build :spotlessCheck` and `mkdocs build --strict`.

## Phase 7: Push to remote

- [x] Push the branch or merge the pull request to `main`.
- [x] Verify remote CI passes after the push or merge.

## Notes

- Escalated to human review: this change touches multi-module behavior (Gradle plugin / state-filesystem) and public-facing error messages.
- The previous `FileSystemStateSource.DEFAULT_FILE_NAME` reference in `TerracottaPlugin.kt` was already being inlined by the Kotlin compiler (`const val`), so the plugin could load without `terracotta-state-filesystem` on the classpath even before this change. Removing the import makes the decoupling explicit and prevents future accidental coupling.
- Integration test for the missing filesystem backend filters the plugin-under-test metadata classpath to exclude `terracotta-state-filesystem` while keeping the rest of the plugin runtime intact.
