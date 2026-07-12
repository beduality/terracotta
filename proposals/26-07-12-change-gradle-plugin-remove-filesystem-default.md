---
description: The Gradle plugin must fail fast with a clear error when the configured state backend is not on the classpath, while keeping terracotta-state-filesystem as a default dependency for now.
---

# Fail fast when the configured state backend is missing

## TL;DR

The Terracotta Gradle plugin ships with `terracotta-state-filesystem` on the classpath by default, but it currently imports `FileSystemStateSource` directly and only surfaces a missing backend through `ServiceLoader` resolution. This proposal keeps the default dependency for now, removes the direct coupling in the plugin source, and adds an explicit runtime check so the plugin fails early and clearly whenever the configured backend factory is unavailable.

## Problem Statement

`modules/terracotta-gradle-plugin/build.gradle.kts` declares:

```kotlin
dependencies {
    implementation(project(":terracotta-core"))
    implementation(project(":terracotta-state-filesystem"))
    // ...
}
```

The Gradle plugin therefore ships with the filesystem backend on the classpath by default. However, `TerracottaPlugin.kt` imports `FileSystemStateSource.DEFAULT_FILE_NAME` directly, which couples the plugin to a specific backend implementation class. If a user ever excludes `terracotta-state-filesystem` from the plugin classpath, the plugin can fail during application with a low-level `NoClassDefFoundError` rather than a meaningful Terracotta error.

Even when all classes are present, backend resolution happens lazily inside `StateSourceResolver` and the existing error message does not tell the user which backends *are* available or how to add the missing one.

Consequences:

- **Failures are obscure.** A missing or excluded backend produces class-loading errors instead of actionable diagnostics.
- **Module boundaries leak.** The frontend plugin knows implementation details of a specific backend.
- **Future opt-out is hard.** Before the filesystem backend can become an optional dependency, the plugin must be able to load and run without it.
- **Operator confusion.** The error "No state source factory found with id 'filesystem'" does not explain that a dependency may be missing.

## Goals

- Keep `implementation(project(":terracotta-state-filesystem"))` as a default dependency of the Gradle plugin for now.
- Remove direct references to `FileSystemStateSource` from the Gradle plugin source code so the plugin can load even when the backend is excluded.
- Ensure `StateSourceResolver` (or the code that calls it) fails fast with a clear error when the configured backend factory is missing.
- Improve the missing-backend error message to list the IDs of factories that *are* available and, when the filesystem backend is missing, point to the `terracotta-state-filesystem` dependency.
- Update documentation to clarify that the filesystem backend is bundled by default today but is still a pluggable dependency that can be replaced.

## Non-Goals

- Removing the default `terracotta-state-filesystem` dependency from the Gradle plugin at this stage.
- Removing or rewriting the `terracotta-state-filesystem` module.
- Changing the core state SPI or `StateSourceResolver` contract.
- Introducing a new default state backend.
- Making state management optional for tasks that require it (that decision is left to a later proposal).

## Proposed Design

### Gradle plugin dependencies

Keep `modules/terracotta-gradle-plugin/build.gradle.kts` unchanged for now:

```kotlin
dependencies {
    implementation(project(":terracotta-core"))
    implementation(project(":terracotta-state-filesystem"))
    // ...
}
```

### Remove direct coupling to the filesystem backend

In `TerracottaPlugin.kt`, remove the import and direct use of `FileSystemStateSource.DEFAULT_FILE_NAME`. Introduce a local constant for the default state filename so the plugin class loads without requiring the filesystem backend on the classpath.

```kotlin
internal const val DEFAULT_STATE_FILE_NAME = ".terracotta-state.yml"
```

`stateSource` keeps its `"filesystem"` convention, and `stateFile` keeps defaulting to `.terracotta-state.yml` through the local constant. The only change is that the plugin no longer references `FileSystemStateSource` directly.

### Explicit backend availability check

`StateSourceResolver.resolve` already throws a `GradleException` when `ServiceLoader` cannot find a matching factory. Enhance it to:

1. Collect all discovered factory IDs.
2. Include the missing ID, the available IDs, and a dependency hint in the exception message.

Example message when `terracotta-state-filesystem` is missing and `stateSource` is `"filesystem"`:

```
No state source factory found with id 'filesystem'. Available factories: [].
Make sure the backend module is on the classpath. For the default filesystem backend, add:
  implementation("io.github.beduality:terracotta-state-filesystem:<version>")
```

For other backends, the hint can be generic:

```
No state source factory found with id '<id>'. Available factories: [...].
Make sure the backend module is on the classpath.
```

### Deprecated `stateFile` property

`stateFile` remains deprecated. When set, it should still imply `"filesystem"` and map to `stateSourceSettings["path"]`, using the local default filename constant.

## API Sketch

No public API changes are required. The DSL remains the same.

When the filesystem backend is eventually excluded, the user restores it by adding it to the buildscript classpath:

```kotlin
buildscript {
    dependencies {
        classpath("io.github.beduality:terracotta-state-filesystem:...")
    }
}
```

Until then, users who do not customize the classpath continue to receive the filesystem backend by default.

## Testing Strategy

- **Unit tests:** Update `StateSourceResolverTest` to assert the enhanced error message when no factory is available and to verify available factory IDs are listed.
- **Integration tests:** Add or update `TerracottaPluginIntegrationTest` to verify that:
  - Applying the plugin with the default classpath resolves `"filesystem"` successfully.
  - Excluding `terracotta-state-filesystem` from the plugin classpath fails during task execution with the new error message, not a `NoClassDefFoundError`.
  - The deprecated `stateFile` property still works and defaults to `.terracotta-state.yml`.
- **Manual verification:** Create a minimal project, apply the plugin, and confirm that `terracottaPlan` succeeds out of the box but fails with a clear message when the filesystem backend is excluded.

## Documentation Updates

- `docs/content/modules/terracotta-state-filesystem/README.md`: Keep the note that the Gradle plugin currently depends on it by default, but add that it can be replaced and that missing backends produce a clear error.
- `docs/content/modules/terracotta-state-filesystem/reference/state-filesystem.md`: Keep the description of the default backend; add a note about the missing-backend error and how to add the dependency if it is excluded.
- `docs/content/modules/gradle-plugin/how-to-guides/kotlin-dsl-configuration.md`: Keep "The default backend is 'filesystem'"; add a short paragraph explaining what error users see if the backend is missing and how to add it back.
- `docs/content/modules/gradle-plugin/tutorials/installation.md`: No change required, since the default setup still includes the filesystem backend.
- `CHANGELOG.md`: Add an entry for the improved error message and the removal of the direct `FileSystemStateSource` coupling.

## Open Questions

1. Should the missing-backend error include a per-backend dependency hint, or is a generic message sufficient?
2. Should the plugin expose a helper configuration (e.g., `dependencies { terracotta(...) }`) so users can add or replace backends without `buildscript` classpath manipulation?
3. When should we actually remove the default `terracotta-state-filesystem` dependency from the Gradle plugin (a follow-up proposal)?

## Risks

- **Risk:** Changing the error message or the timing of the failure could break tests that assert on the old exception text.  
  **Mitigation:** Update `StateSourceResolverTest` and integration tests as part of this change.
- **Risk:** Removing the `FileSystemStateSource` import without updating the `stateFile` convention could accidentally change the default filename.  
  **Mitigation:** Introduce the local `DEFAULT_STATE_FILE_NAME` constant and assert its value matches the previous default in tests.
- **Risk:** Users who exclude `terracotta-state-filesystem` today will still get an error; this proposal only makes the error clearer.  
  **Mitigation:** Document the clearer error in the changelog so users understand the immediate behavior and the future direction.
