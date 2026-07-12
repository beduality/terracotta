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
| Test-driven development | Not started | |
| Implementation | Not started | |
| Review | Not started | |
| Documentation | Not started | |
| Push to remote | Not started | |

## Phase 3: Test-driven development

- [ ] Read `module-testing-workflow.md`.
- [ ] Identify behavior and edge cases:
  - Missing `"filesystem"` factory with `stateSource` set to `"filesystem"`.
  - Missing arbitrary backend factory (e.g., a future cloud backend).
  - Default classpath still resolves `"filesystem"` successfully.
  - Deprecated `stateFile` still defaults to `.terracotta-state.yml`.
- [ ] Update `StateSourceResolverTest` to assert the enhanced error message and available-factory list.
- [ ] Add or update `TerracottaPluginIntegrationTest` to verify the plugin fails cleanly when `terracotta-state-filesystem` is excluded from the classpath.
- [ ] Run tests and confirm the new assertions fail for the expected reason (the implementation is not yet changed).

## Phase 4: Implementation

- [ ] Read `module-implementation-workflow.md`.
- [ ] Remove the direct import and use of `FileSystemStateSource.DEFAULT_FILE_NAME` from `TerracottaPlugin.kt`.
- [ ] Introduce a local `DEFAULT_STATE_FILE_NAME` constant in the Gradle plugin.
- [ ] Enhance `StateSourceResolver.resolve` to collect discovered factory IDs and produce a clear error message when the configured backend is missing.
- [ ] Keep `terracotta-state-filesystem` as an `implementation` dependency for now.
- [ ] Run `:terracotta-gradle-plugin:test :terracotta-gradle-plugin:spotlessCheck :build`.
- [ ] Refactor and add KDoc if needed.

## Phase 5: Review

- [ ] Read `module-review-workflow.md`.
- [ ] Run code review checklist.
- [ ] By default, confirm auto-review (tests, spotless, build, docs build) passes.
- [ ] Escalate to human review because this touches multi-module behavior and public-facing error messages.
- [ ] Update `CHANGELOG.md` to mention the clearer missing-backend error and the removal of the direct `FileSystemStateSource` coupling.

## Phase 6: Documentation

- [ ] Read `module-documentation-workflow.md`.
- [ ] Update `docs/content/modules/terracotta-state-filesystem/README.md` to clarify the default dependency and the missing-backend error.
- [ ] Update `docs/content/modules/terracotta-state-filesystem/reference/state-filesystem.md` with the same note.
- [ ] Add a short paragraph to `docs/content/modules/gradle-plugin/how-to-guides/kotlin-dsl-configuration.md` explaining the error and how to restore the filesystem backend.
- [ ] Final verification: `./gradlew build :spotlessCheck` and `mkdocs build --strict`.

## Phase 7: Push to remote

- [ ] Push the branch or merge the pull request to `main`.
- [ ] Verify remote CI passes after the push or merge.

## Notes

<!-- Decisions, blockers, and discoveries go here as work progresses. -->
