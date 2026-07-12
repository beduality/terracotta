---
description: Implementation plan for a provider-specific logic layer in terracotta-core, with concrete implementations for Modrinth and Hangar.
---

# Provider-Specific Logic Layer Implementation Plan

This plan follows `module-development-workflow.md` for a **new public API / extension point** that touches `terracotta-core`, `terracotta-provider-modrinth`, and `terracotta-provider-hangar`. The CurseForge provider is out of scope until its module is added.

## Source of truth

- TODO item: `Add [Provider-Specific Logic Layer](./proposals/2025-07-provider-specific-logic.md)` (`project/TODO.md`)
- Design proposal: `project/proposals/2025-07-provider-specific-logic.md`

## Progress summary

| Phase | Status | Notes |
|-------|--------|-------|
| System design | Completed | Core: `ProviderLogic`, `LoaderMapper`, `PlatformBehavior`. Provider-local: mapper/behavior implementations. CurseForge logic deferred. |
| TDD | Completed | Tests added for core interfaces, provider logic, and factory `createProviderLogic()`. |
| Implementation | Completed | `ProviderLogic` wired into Modrinth and Hangar registry providers and factories. `HangarLoaderMapper` implements `LoaderMapper`. |
| Build / quality checks | Completed | `:build` and `:spotlessCheck` pass for `terracotta-core`, `terracotta-provider-modrinth`, `terracotta-provider-hangar`. |
| Documentation | In progress | Need to add Diátaxis docs and run `mkdocs build --strict`. |
| Review / release prep | Not started | |

Update this during your session.

## Phase 1: System design

- [x] Read `module-system-design-workflow.md`.
- [x] Read `project/proposals/2025-07-provider-specific-logic.md`.
- [x] Decide which interfaces/objects live in `terracotta-core` and which are provider-local.
- [x] Finalize the public API surface for in-scope providers (Modrinth + Hangar). `IdResolver`, `IdCache`, and `ProviderTransformationPipeline` are deferred to the CurseForge provider plan.
- [x] Investigate whether the existing `HangarLoaderMapper` can be reused as-is or needs an interface wrapper. Decision: make it implement `LoaderMapper`; retain `mapPlatformToLoader` as a Hangar-specific helper.
- [x] Determine how `ProviderLogic` is discovered and consumed by `RegistryProvider` implementations. Decision: add `ProviderFactory.createProviderLogic()` and inject the resulting `ProviderLogic` into registry providers.
- [x] Complete design review checklist from `module-review-workflow.md`.
- [x] Update this plan with decisions that affect later phases.

### Design decisions

- **Core API** (`io.github.beduality.terracotta.core.provider.logic`):
  - `ProviderLogic`: composition of `loaderMapper` + `platformBehavior`.
  - `LoaderMapper`: maps a canonical loader ID to a provider-specific platform name; default `mapToPlatforms(List)` helper.
  - `PlatformBehavior`: `isStateful` + `filterOperations(List<Operation>)`. Both Modrinth and Hangar are stateful, but Hangar filters unsupported operations (CreateProject, gallery, icon).
- **Provider-local**:
  - `ModrinthProviderLogic`: identity loader mapper, stateful behavior that accepts all operations.
  - `HangarProviderLogic`: reuses `HangarLoaderMapper`, stateful behavior that filters unsupported operations.
- **Out of scope**: CurseForge numeric ID resolution (`IdResolver`, `IdCache`) and project-level transformation pipeline.
- **Wiring**: `ProviderFactory` gains a required `createProviderLogic()` method. `RegistryProvider` implementations receive `ProviderLogic` via constructor and use `platformBehavior.filterOperations()` before applying. `HangarClient` receives the loader mapper via constructor so the same instance is used by state and registry providers.

## Phase 2: Test-driven development

- [x] Read `module-testing-workflow.md`.
- [x] Identify behavior and edge cases for loader mapping: identity (Modrinth) and platform grouping (Hangar); unsupported loaders return `null` and are skipped.
- [x] Identify behavior and edge cases for platform behavior: stateful platforms keep all operations; Hangar filters `CreateProject`, gallery, and icon operations.
- [x] `IdResolver`/`IdCache` intentionally out of scope for Modrinth/Hangar.
- [ ] Write failing tests in `terracotta-core` and affected provider modules.
- [ ] Run tests and confirm they fail for the expected reason.

## Phase 3: Implementation

- [x] Read `module-implementation-workflow.md`.
- [ ] Add `ProviderLogic` and related interfaces to `terracotta-core`.
- [ ] Implement `ModrinthProviderLogic` in `terracotta-provider-modrinth`.
- [ ] Implement `HangarProviderLogic` in `terracotta-provider-hangar`, reusing `HangarLoaderMapper`.
- [ ] Update `ModrinthRegistryProvider` and `HangarRegistryProvider` to consume the logic layer.
- [ ] Make tests pass with the smallest change.
- [ ] Refactor and add KDoc.
- [ ] Run `:build :spotlessCheck`.

## Phase 4: Documentation

- [ ] Read `module-documentation-workflow.md`.
- [ ] Add or update Diátaxis docs under `docs/content/modules/core/` and provider-specific sections.
- [ ] Cross-link KDoc with `@see`.
- [ ] Run `mkdocs build --strict`.

## Phase 5: Review / release prep

- [ ] Read `module-review-workflow.md`.
- [ ] Run code review checklist.
- [ ] Update `CHANGELOG.md` if users need to know about the change.
- [ ] Final verification: `./gradlew build :spotlessCheck` and `mkdocs build --strict`.

## Notes

- Hangar loader mapping already exists in `modules/terracotta-provider-hangar/src/main/kotlin/io/github/beduality/terracotta/provider/hangar/mapper/HangarLoaderMapper.kt` and probably needs refactoring.
- CurseForge logic is intentionally deferred to the CurseForge provider plan.
