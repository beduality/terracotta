---
description: Make state persistence swappable via a StateSourceFactory SPI and extract the file-backed implementation into a dedicated terracotta-state-filesystem module.
---

# Pluggable State Backends

This plan follows `module-development-workflow.md` for a **new module / major feature / new public API** that touches `terracotta-core`, `terracotta-state-filesystem`, and `terracotta-gradle-plugin`.

## Source of truth

- Design proposal: `project/proposals/2026-07-pluggable-state-backends.md`

## Progress summary

| Phase | Status | Notes |
|-------|--------|-------|
| System design | Not started | |
| TDD | Not started | |
| Implementation | Not started | |
| Review / release prep | Not started | |
| Documentation | Not started | |

## Phase 1: System design

- [ ] Read `module-system-design-workflow.md`.
- [ ] Review and approve `project/proposals/2026-07-pluggable-state-backends.md`.
- [ ] Resolve open questions in the proposal:
  - `stateSourceSettings` shape (`MapProperty<String, String>` vs nested DSL block).
  - Whether the filesystem backend stays in `terracotta-core` or moves to a new module.
  - When unknown backends fail (plugin application vs lazy task resolution).
- [ ] Finalize the public API contract: `StateSourceFactory`, `StateSourceConfig`, and `StateSource` semantics.
- [ ] Define module boundaries and dependency rules:
  - `terracotta-core` owns the SPI and `TerracottaState` model.
  - `terracotta-state-filesystem` implements the file backend and registers via `ServiceLoader`.
  - `terracotta-gradle-plugin` resolves the backend through the DSL and depends on the filesystem module by default.
- [ ] Define the `stateFile` deprecation and migration path.
- [ ] Document design decisions that affect later phases in the notes below.

## Phase 2: Test-driven development

- [ ] Read `module-testing-workflow.md`.
- [ ] Identify behavior and edge cases:
  - Unknown `stateSource` id produces a clear error.
  - Missing filesystem backend on the classpath fails gracefully.
  - Default path resolution when no `path` setting is provided.
  - Explicit `path` override via `stateSourceSettings`.
  - `stateFile` convenience sets the equivalent filesystem backend config.
- [ ] Add failing unit tests for `StateSourceFactory` discovery and selection.
- [ ] Add failing tests for `FileSystemStateSourceFactory` (default path, explicit path).
- [ ] Move `FileSystemStateSourceTest` into `terracotta-state-filesystem` and ensure it still fails until the module exists.
- [ ] Add Gradle plugin integration tests:
  - Default `stateSource` resolves to the filesystem backend.
  - Unknown `stateSource` fails with a descriptive message.
  - `stateFile` still works and behaves like `stateSource = "filesystem"`.
- [ ] Run the new tests and confirm they fail for the expected reasons.

## Phase 3: Implementation

- [ ] Read `module-implementation-workflow.md`.
- [ ] Add `StateSourceFactory` and `StateSourceConfig` to `terracotta-core` with KDoc.
- [ ] Create the `terracotta-state-filesystem` module:
  - `modules/terracotta-state-filesystem/build.gradle.kts` depending on `terracotta-core`.
  - Move `FileSystemStateSource` and `YamlStateCodec` from `terracotta-core`.
  - Implement `FileSystemStateSourceFactory` with `id = "filesystem"`.
  - Register the factory via `META-INF/services/io.github.beduality.terracotta.core.state.StateSourceFactory`.
- [ ] Update `terracotta-gradle-plugin`:
  - Add `implementation(project(":terracotta-state-filesystem"))`.
  - Replace `stateFile` with `stateSource` and `stateSourceSettings` in `TerracottaExtension`.
  - Keep `stateFile` as a deprecated convenience that maps to the filesystem backend.
  - Resolve the backend via `ServiceLoader` during plugin application or lazy task wiring.
- [ ] Inject the resolved `StateSource` into tasks that need it.
- [ ] Refactor for composability and add KDoc to public APIs.
- [ ] Run `./gradlew :build :spotlessCheck`.

## Phase 4: Review / release prep

- [ ] Read `module-review-workflow.md`.
- [ ] Run the design and code review checklists.
- [ ] Verify public API packages preserve existing import paths or document migration.
- [ ] Update `CHANGELOG.md` with the new module, SPI, and DSL changes.
- [ ] Final verification: `./gradlew build :spotlessCheck` and `mkdocs build --strict`.

## Phase 5: Documentation

- [ ] Read `module-documentation-workflow.md`.
- [ ] Update `docs/content/modules/core/explanation/state-management.md` to describe the factory SPI and backend selection.
- [ ] Update `docs/content/modules/gradle-plugin/how-to-guides/kotlin-dsl-configuration.md` for `stateSource`/`stateSourceSettings` and `stateFile` deprecation.
- [ ] Add a reference page for `terracotta-state-filesystem` under `docs/content/modules/terracotta-state-filesystem/` if the docs section exists.
- [ ] Cross-link KDoc with `@see` tags where appropriate.
- [ ] Run `mkdocs build --strict`.

## Notes

<!-- Decisions, blockers, and discoveries go here as work progresses. -->
