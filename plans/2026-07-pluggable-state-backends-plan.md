---
description: Make state persistence swappable via a StateSourceFactory SPI and extract the file-backed implementation into a dedicated terracotta-state-filesystem module.
---

# Pluggable State Backends

This plan follows `module-development-workflow.md` for a **new module / major feature / new public API** that touches `terracotta-core`, `terracotta-state-filesystem`, and `terracotta-gradle-plugin`.

## Source of truth

- Design proposal: `project/proposals/26-07-12-add-state-pluggable-backends.md`

## Progress summary

| Phase | Status | Notes |
|-------|--------|-------|
| System design | Done | Proposal approved; see Notes |
| Contract | Done | Core contract compiles |
| TDD | Done | Failing tests added and verified |
| Implementation | Done | Build and spotless pass |
| Review | Done | Auto-review passed, CHANGELOG updated |
| Documentation | Done | Docs build passes |
| Push to remote | Done | Branch pushed, CI green; merge to main deferred for human review |

## Phase 1: System design

- [x] Read `module-system-design-workflow.md`.
- [x] Review and approve `project/proposals/26-07-12-add-state-pluggable-backends.md`.
- [x] Resolve open questions in the proposal:
  - `stateSourceSettings` shape: `MapProperty<String, String>` (keeps Kotlin/Groovy DSL symmetric and simple).
  - Filesystem backend moves to a new `terracotta-state-filesystem` module.
  - Unknown backends fail during plugin application for fast feedback.
- [x] Finalize the public API contract: `StateSourceFactory`, `StateSourceConfig`, and `StateSource` semantics.
- [x] Define module boundaries and dependency rules:
  - `terracotta-core` owns the SPI and `TerracottaState` model.
  - `terracotta-state-filesystem` implements the file backend and registers via `ServiceLoader`.
  - `terracotta-gradle-plugin` resolves the backend through the DSL and depends on the filesystem module by default.
- [x] Define the `stateFile` deprecation and migration path.
- [x] Document design decisions that affect later phases in the notes below.

## Phase 2: Contract

- [x] Read `module-contract-workflow.md`.
- [x] Add `StateSourceFactory`, `StateSourceConfig`, and `StateSource` interfaces to `terracotta-core` with KDoc.
- [x] Define the public package and mark internal symbols clearly.
- [x] Run `:<module>:compileKotlin` for `terracotta-core` and `terracotta-gradle-plugin`.

## Phase 3: Test-driven development

- [x] Read `module-testing-workflow.md`.
- [x] Identify behavior and edge cases:
  - Unknown `stateSource` id produces a clear error.
  - Missing filesystem backend on the classpath fails gracefully.
  - Default path resolution when no `path` setting is provided.
  - Explicit `path` override via `stateSourceSettings`.
  - `stateFile` convenience sets the equivalent filesystem backend config.
- [x] Add failing unit tests for `StateSourceFactory` discovery and selection.
- [x] Add failing tests for `FileSystemStateSourceFactory` (default path, explicit path).
- [x] Move `FileSystemStateSourceTest` into `terracotta-state-filesystem` and ensure it still fails until the module exists.
- [x] Add Gradle plugin integration tests:
  - Default `stateSource` resolves to the filesystem backend.
  - Unknown `stateSource` fails with a descriptive message.
  - `stateFile` still works and behaves like `stateSource = "filesystem"`.
- [x] Run the new tests and confirm they fail for the expected reasons.

## Phase 4: Implementation

- [x] Read `module-implementation-workflow.md`.
- [x] Create the `terracotta-state-filesystem` module:
  - `modules/terracotta-state-filesystem/build.gradle.kts` depending on `terracotta-core`.
  - Move `FileSystemStateSource` and `YamlStateCodec` from `terracotta-core`.
  - Implement `FileSystemStateSourceFactory` with `id = "filesystem"`.
  - Register the factory via `META-INF/services/io.github.beduality.terracotta.core.state.StateSourceFactory`.
- [x] Update `terracotta-gradle-plugin`:
  - Add `implementation(project(":terracotta-state-filesystem"))`.
  - Replace `stateFile` with `stateSource` and `stateSourceSettings` in `TerracottaExtension`.
  - Keep `stateFile` as a deprecated convenience that maps to the filesystem backend.
  - Resolve the backend via `ServiceLoader` during plugin application or lazy task wiring.
- [x] Inject the resolved `StateSource` into tasks that need it (deferred; no task consumes state yet).
- [x] Refactor for composability and add KDoc to public APIs.
- [x] Run `./gradlew :build :spotlessCheck`.

## Phase 5: Review

- [x] Read `module-review-workflow.md`.
- [x] Run the design and code review checklists.
- [x] By default, confirm auto-review (tests, spotless, build, docs build) passes.
- [x] Escalate to human review for public API, build configuration, or multi-module changes if needed (multi-module/public API change; recommend human review before merge).
- [x] Verify public API packages preserve existing import paths or document migration.
- [x] Update `CHANGELOG.md` with the new module, SPI, and DSL changes.

## Phase 6: Documentation

- [x] Read `module-documentation-workflow.md`.
- [x] Update `docs/content/modules/core/explanation/state-management.md` to describe the factory SPI and backend selection.
- [x] Update `docs/content/modules/gradle-plugin/how-to-guides/kotlin-dsl-configuration.md` for `stateSource`/`stateSourceSettings` and `stateFile` deprecation.
- [x] Add a reference page for `terracotta-state-filesystem` under `docs/content/modules/terracotta-state-filesystem/` if the docs section exists.
- [x] Cross-link KDoc with `@see` tags where appropriate.
- [x] Final verification: `./gradlew build :spotlessCheck` and `mkdocs build --strict`.

## Phase 7: Push to remote

- [x] Push the branch or merge the pull request to `main` (branch `feat/pluggable-state-backends` pushed).
- [x] Verify remote CI passes after the push or merge (CI run 29208275385 green).

## Notes

- **2026-07-12**: Approved `project/proposals/26-07-12-add-state-pluggable-backends.md`.
- `stateSourceSettings` is a `MapProperty<String, String>` for simplicity and Groovy DSL parity.
- Filesystem backend moves to `terracotta-state-filesystem`; `terracotta-core` keeps only the SPI and model.
- Unknown `stateSource` fails during plugin application so users get immediate feedback.
- `stateFile` is deprecated with a warning and internally maps to `stateSource = "filesystem"` plus `stateSourceSettings["path"] = <file>`.
- `FileSystemStateSource` and `YamlStateCodec` moved as-is; the package stays `io.github.beduality.terracotta.core.state` to preserve imports.
- State backend resolution is validated in `project.afterEvaluate`; no task currently consumes the resolved `StateSource`, so task injection is deferred until a task needs it.
- Dokka generation is currently blocked on Java 26 (`IllegalArgumentException: 26.0.1` parsing `JavaVersion`), so the new module's docs link to the existing `terracotta-core` Dokka output and omit a separate Dokka nav entry.
