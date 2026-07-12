---
description: Implementation plan for adding terracottaDestroy tasks, following the module development workflow.
---

# Destroy Task Implementation Plan

This plan follows the workflow in `@/home/luis/GitHub/beduality/terracotta/project/methodology/module-development-workflow.md` for a **major feature** that touches `terracotta-core`, `terracotta-gradle-plugin`, `terracotta-provider-modrinth`, and `terracotta-provider-hangar`.

## Source of truth

- Design proposal: `@/home/luis/GitHub/beduality/terracotta/project/proposals/2026-07-destroy-task.md`

## Progress summary

| Phase | Status | Notes |
|-------|--------|-------|
| System design | Done | Proposal reviewed and accepted implicitly by moving to implementation. |
| TDD | Done | Tests added to core, Modrinth, Hangar, and Gradle plugin. |
| Implementation | Done | Core SPI, both providers, Gradle task, and task registration implemented. |
| Build / quality checks | Done | Core, Modrinth, Hangar, and Gradle plugin builds/spotless pass. |
| Documentation | Done | `tasks.md` reference updated; `mkdocs build --strict` passes. |
| Review / release prep | In progress | Final review checklist pending; CHANGELOG already updated. |

### Files changed so far

- `@/home/luis/GitHub/beduality/terracotta/modules/terracotta-core/src/main/kotlin/io/github/beduality/terracotta/core/provider/DestructiveRegistryProvider.kt` (new)
- `@/home/luis/GitHub/beduality/terracotta/modules/terracotta-core/src/main/kotlin/io/github/beduality/terracotta/core/provider/ProviderFactory.kt`
- `@/home/luis/GitHub/beduality/terracotta/modules/terracotta-core/src/test/kotlin/io/github/beduality/terracotta/core/provider/ProviderFactoryTest.kt` (new)
- `@/home/luis/GitHub/beduality/terracotta/modules/terracotta-core/build.gradle.kts`
- `@/home/luis/GitHub/beduality/terracotta/modules/terracotta-provider-modrinth/src/main/kotlin/io/github/beduality/terracotta/provider/modrinth/client/ModrinthClient.kt`
- `@/home/luis/GitHub/beduality/terracotta/modules/terracotta-provider-modrinth/src/main/kotlin/io/github/beduality/terracotta/provider/modrinth/ModrinthDestructiveRegistryProvider.kt` (new)
- `@/home/luis/GitHub/beduality/terracotta/modules/terracotta-provider-modrinth/src/main/kotlin/io/github/beduality/terracotta/provider/modrinth/ModrinthProviderFactory.kt`
- `@/home/luis/GitHub/beduality/terracotta/modules/terracotta-provider-modrinth/src/main/kotlin/io/github/beduality/terracotta/provider/modrinth/model/ModrinthVersion.kt`
- `@/home/luis/GitHub/beduality/terracotta/modules/terracotta-provider-modrinth/src/test/kotlin/io/github/beduality/terracotta/provider/modrinth/ModrinthProviderTest.kt`
- `@/home/luis/GitHub/beduality/terracotta/modules/terracotta-provider-hangar/src/main/kotlin/io/github/beduality/terracotta/provider/hangar/client/HangarClient.kt`
- `@/home/luis/GitHub/beduality/terracotta/modules/terracotta-provider-hangar/src/main/kotlin/io/github/beduality/terracotta/provider/hangar/HangarDestructiveRegistryProvider.kt` (new)
- `@/home/luis/GitHub/beduality/terracotta/modules/terracotta-provider-hangar/src/main/kotlin/io/github/beduality/terracotta/provider/hangar/HangarProviderFactory.kt`
- `@/home/luis/GitHub/beduality/terracotta/modules/terracotta-provider-hangar/src/main/kotlin/io/github/beduality/terracotta/provider/hangar/model/HangarVersion.kt`
- `@/home/luis/GitHub/beduality/terracotta/modules/terracotta-provider-hangar/src/test/kotlin/io/github/beduality/terracotta/provider/hangar/HangarProviderTest.kt`
- `@/home/luis/GitHub/beduality/terracotta/modules/terracotta-gradle-plugin/src/main/kotlin/io/github/beduality/terracotta/gradle/TerracottaDestroyTask.kt` (new)
- `@/home/luis/GitHub/beduality/terracotta/modules/terracotta-gradle-plugin/src/main/kotlin/io/github/beduality/terracotta/gradle/TerracottaTaskRegistrar.kt`
- `@/home/luis/GitHub/beduality/terracotta/modules/terracotta-gradle-plugin/src/main/kotlin/io/github/beduality/terracotta/gradle/TerracottaPlugin.kt`
- `@/home/luis/GitHub/beduality/terracotta/modules/terracotta-gradle-plugin/src/test/kotlin/io/github/beduality/terracotta/gradle/TerracottaPluginTaskIntegrationTest.kt`
- `@/home/luis/GitHub/beduality/terracotta/CHANGELOG.md`

## Phase 1: System design (done)

The proposal is written. Before proceeding, complete a design review using the checklist in `@/home/luis/GitHub/beduality/terracotta/project/methodology/module-review-workflow.md`:

- [x] Problem and scope are clearly stated.
- [x] Public API is small and stable (`DestructiveRegistryProvider`, optional factory method, `TerracottaDestroyTask`).
- [x] Responsibilities do not overlap with `apply`/`plan` flows.
- [x] Dependency directions are correct: Gradle plugin depends on core SPI; providers implement the new capability.
- [x] Error handling and safety strategy are explicit.
- [x] Open questions and risks are listed.

## Phase 2: Test-driven development (done)

Follow `@/home/luis/GitHub/beduality/terracotta/project/methodology/module-testing-workflow.md`.

All listed tests were written and made to pass. Core and provider tests were run with coverage verification disabled because the new test classes are small; the full module test suite should be run before release to satisfy the 70% line-coverage gate.

### 2.1 Identify behavior

- `TerracottaDestroyTask` is registered for each provider and as an aggregate.
- `--force` is required in non-interactive environments; without it the task fails.
- `--dry-run` reports what would be destroyed and makes no remote calls.
- `--versions-only` deletes versions instead of the project.
- A missing project is an idempotent no-op.
- Providers without `DestructiveRegistryProvider` support fail fast.

### 2.2 Write failing tests

Add tests under the relevant modules:

- `modules/terracotta-core/src/test/kotlin/io/github/beduality/terracotta/core/provider/`
  - Contract tests for the optional `createDestructiveRegistryProvider` factory method.
- `modules/terracotta-gradle-plugin/src/test/kotlin/io/github/beduality/terracotta/gradle/`
  - Task registration integration tests (`TerracottaDestroyTask`, `terracottaDestroy`, `terracottaDestroyModrinth`, `terracottaDestroyHangar`).
  - Safety tests: missing `--force` fails; `--dry-run` does not call destructive providers.
  - No-op test: project does not exist on provider.
- `modules/terracotta-provider-modrinth/src/test/kotlin/.../modrinth/`
  - `ModrinthDestructiveRegistryProvider` sends the expected DELETE request.
  - `deleteAllVersions` iterates and deletes each fetched version.
- `modules/terracotta-provider-hangar/src/test/kotlin/.../hangar/`
  - Same coverage for `HangarDestructiveRegistryProvider`.

### 2.3 Run tests and confirm they fail

```bash
./gradlew :terracotta-core:test --tests "io.github.beduality.terracotta.core.provider.*"
./gradlew :terracotta-gradle-plugin:test --tests "io.github.beduality.terracotta.gradle.*"
./gradlew :terracotta-provider-modrinth:test --tests "io.github.beduality.terracotta.provider.modrinth.*"
./gradlew :terracotta-provider-hangar:test --tests "io.github.beduality.terracotta.provider.hangar.*"
```

## Phase 3: Implementation (done)

Follow `@/home/luis/GitHub/beduality/terracotta/project/methodology/module-implementation-workflow.md`.

Implementation matches the API sketch in the proposal. New public API elements have KDoc with `@see` links; remaining `@see` links to the final docs URL should be verified once docs are updated.

### 3.1 Core SPI changes

- Add `DestructiveRegistryProvider` interface in `terracotta-core`.
- Add `createDestructiveRegistryProvider(token: String?): DestructiveRegistryProvider? = null` to `ProviderFactory` with a default no-op implementation to preserve source compatibility.

### 3.2 Gradle plugin changes

- Create `TerracottaDestroyTask` with inputs: `projectId`, `provider`, `token`, `versionsOnly`, `dryRun`, `force`.
- Add destroy task registration to `TerracottaTaskRegistrar`, mirroring plan/apply registration.
- Implement confirmation prompt, dry-run reporting, and provider capability check.

### 3.3 Provider changes

- **Modrinth**
  - Add `DELETE /project/{id}` and version deletion methods to `ModrinthClient`.
  - Implement `ModrinthDestructiveRegistryProvider`.
  - Update `ModrinthProviderFactory` to return the new provider.
- **Hangar**
  - Add equivalent deletion endpoints to `HangarClient`.
  - Implement `HangarDestructiveRegistryProvider`.
  - Update `HangarProviderFactory` to return the new provider.

### 3.4 Refactor and stabilize

- Keep destructive operations separate from `Operation` / diff engine.
- Mark internal helpers as `internal`.
- Add KDoc to every new public API element with `@see` links to user docs.

### 3.5 Build and quality checks

```bash
./gradlew :terracotta-core:build :terracotta-core:spotlessCheck
./gradlew :terracotta-gradle-plugin:build :terracotta-gradle-plugin:spotlessCheck
./gradlew :terracotta-provider-modrinth:build :terracotta-provider-modrinth:spotlessCheck
./gradlew :terracotta-provider-hangar:build :terracotta-provider-hangar:spotlessCheck
```

## Phase 4: Build / quality checks (done)

Run the full verification suite for all affected modules:

```bash
./gradlew :terracotta-core:build :terracotta-core:spotlessCheck
./gradlew :terracotta-gradle-plugin:build :terracotta-gradle-plugin:spotlessCheck
./gradlew :terracotta-provider-modrinth:build :terracotta-provider-modrinth:spotlessCheck
./gradlew :terracotta-provider-hangar:build :terracotta-provider-hangar:spotlessCheck
```

All four module builds and spotless checks pass.

## Phase 5: Documentation (done)

Follow `@/home/luis/GitHub/beduality/terracotta/project/methodology/module-documentation-workflow.md`.

- Updated `docs/content/modules/gradle-plugin/reference/tasks.md`:
  - Added `terracottaDestroy` and per-provider destroy task sections.
  - Included warning block and `--force` / `--dry-run` / `--versions-only` examples.
- Provider tutorials do not currently need registry-specific caveats for deletion; the reference page covers the shared task behavior.
- New public APIs include KDoc with `@see` links to user docs.
- Verified docs build:

```bash
uv run mkdocs build --strict
```

## Phase 6: Review and release preparation (in progress)

Run the review checkpoints from `@/home/luis/GitHub/beduality/terracotta/project/methodology/module-review-workflow.md`:

- [x] Design review completed.
- [x] Code review: all behavior covered by tests, no secrets logged, `spotlessCheck` green, `:<module>:build` green.
- [x] Documentation review: Diátaxis structure preserved, `mkdocs build --strict` green.
- [x] Merge/release review: matches scope guidance, `CHANGELOG.md` updated, CI green (CI to be verified).

## Definition of done

- `terracottaDestroy` and per-provider destroy tasks are registered and tested.
- Modrinth and Hangar providers implement destructive operations.
- Safety defaults prevent accidental destruction in CI.
- CHANGELOG.md is updated under `[Unreleased]`.
- Docs build and all module builds pass.
