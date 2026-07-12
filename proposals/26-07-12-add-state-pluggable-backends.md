---
description: Make state persistence swappable via a StateSourceFactory SPI and extract the file-backed implementation into a dedicated terracotta-state-filesystem module.
---

# Pluggable State Backends

## TL;DR

The initial state-management implementation hard-codes the file-backed backend inside the Gradle plugin. This proposal introduces a `StateSourceFactory` SPI so developers can choose how state is persisted, moves the file-backed backend into a dedicated `terracotta-state-filesystem` module, and lets the Gradle plugin select a backend by name through the DSL.

## Problem Statement

Right now `TerracottaPlugin` creates the default state file path and implicitly assumes a filesystem backend:

```kotlin
extension.stateFile.convention(
    project.layout.projectDirectory.file(FileSystemStateSource.DEFAULT_FILE_NAME),
)
```

This couples the plugin to the filesystem implementation. Teams running Terracotta in CI or from a cloud dashboard may want a shared, durable backend without managing `.terracotta-state.yml` across runners. Keeping the backend selection inside the plugin makes that impossible without editing the plugin source or forking it.

## Goals

1. Make the state backend selectable through the Gradle DSL (e.g. `stateSource.set("filesystem")`).
2. Move the filesystem implementation into its own module (`terracotta-state-filesystem`) so it is optional and composable.
3. Keep the SPI (`StateSource`, `TerracottaState`) in `terracotta-core` as the stable contract.
4. Provide a registration mechanism so new backends can be discovered at runtime, similar to provider factories.
5. Preserve the current `.terracotta-state.yml` default for users who do not opt into a different backend.

## Non-Goals

- Implement a cloud/CI/proprietary backend in this proposal.
- Change the `TerracottaState` model or the semantics of `StateSource.load/save`.
- Add task-level state loading/saving logic (that remains out of scope until diff consumers exist).
- Define authentication, endpoints, or pricing for non-file backends.

## Proposed Design

### StateSourceFactory SPI

Add a small factory interface in `terracotta-core`:

```kotlin
package io.github.beduality.terracotta.core.state

interface StateSourceFactory {
    val id: String
    fun create(config: StateSourceConfig): StateSource
}
```

`StateSourceConfig` carries backend-specific settings as a `Map<String, String>` plus the Gradle project directory for backends that need a default file path:

```kotlin
data class StateSourceConfig(
    val projectDir: File,
    val settings: Map<String, String>,
)
```

Factories are discovered through `ServiceLoader` using the service file:

```
META-INF/services/io.github.beduality.terracotta.core.state.StateSourceFactory
```

### terracotta-state-filesystem module

Create a new module `modules/terracotta-state-filesystem/`:

- Depends on `terracotta-core`.
- Contains `FileSystemStateSource` and `YamlStateCodec` (moved from `terracotta-core`).
- Provides `FileSystemStateSourceFactory` with `id = "filesystem"`.
- The factory reads `path` from `StateSourceConfig.settings` and falls back to `.terracotta-state.yml` in `projectDir`.

The Gradle plugin adds `implementation(project(":terracotta-state-filesystem"))` so the default backend is always available.

### Gradle plugin changes

Replace the file-specific `stateFile` property with a backend-neutral `stateSource` property:

```kotlin
abstract class TerracottaExtension {
    /**
     * State backend identifier. Defaults to "filesystem".
     */
    abstract val stateSource: Property<String>

    /**
     * Backend-specific settings. The filesystem backend uses "path".
     */
    abstract val stateSourceSettings: MapProperty<String, String>
}
```

For backward compatibility, keep `stateFile` as a convenience that sets `stateSource = "filesystem"` and `stateSourceSettings["path"]`. Deprecate it with a warning pointing users to the new DSL.

`TerracottaPlugin` resolves the backend like this:

```kotlin
val factories = ServiceLoader.load(StateSourceFactory::class.java).associateBy { it.id }
val factory = factories[extension.stateSource.get()]
    ?: throw IllegalArgumentException("Unknown state source: ${extension.stateSource.get()}")
val source = factory.create(
    StateSourceConfig(
        projectDir = project.projectDir,
        settings = extension.stateSourceSettings.get(),
    )
)
```

The resolved `StateSource` can then be injected into tasks once they need it.

### Cloud / CI backend hook

Future backends (e.g. `terracotta-state-cloud`) only need to:

1. Implement `StateSource` and `StateSourceFactory`.
2. Register the factory via `ServiceLoader`.
3. Accept their own settings through `stateSourceSettings`.

Example future DSL:

```kotlin
terracotta {
    stateSource.set("cloud")
    stateSourceSettings.put("projectUuid", "...")
    stateSourceSettings.put("endpoint", "https://state.terracotta.cloud")
}
```

## API Sketch

```kotlin
// terracotta-core
interface StateSourceFactory {
    val id: String
    fun create(config: StateSourceConfig): StateSource
}

data class StateSourceConfig(
    val projectDir: File,
    val settings: Map<String, String>,
)

// terracotta-state-filesystem
class FileSystemStateSourceFactory : StateSourceFactory {
    override val id = "filesystem"
    override fun create(config: StateSourceConfig): StateSource {
        val path = config.settings["path"]
            ?: File(config.projectDir, FileSystemStateSource.DEFAULT_FILE_NAME).toPath()
        return FileSystemStateSource.forFile(path)
    }
}

// Gradle DSL
terracotta {
    stateSource.set("filesystem")
    stateSourceSettings.put("path", ".terracotta-state.yml")
}
```

## Testing Strategy

- Unit tests for `StateSourceFactory` discovery: verify that an unknown `stateSource` throws a clear error.
- Unit tests for `FileSystemStateSourceFactory`: default path resolution and explicit path override.
- Gradle plugin integration tests:
  - Default `stateSource` resolves to the filesystem backend.
  - Unknown `stateSource` fails during plugin application or task execution with a descriptive message.
  - `stateFile` still works and is equivalent to `stateSource = "filesystem"`.
- Move the existing `FileSystemStateSourceTest` to the new module and ensure it still passes.

## Documentation Updates

- Update `docs/content/modules/core/explanation/state-management.md` to describe the factory SPI and backend selection.
- Update `docs/content/modules/gradle-plugin/how-to-guides/kotlin-dsl-configuration.md` to document `stateSource` and `stateSourceSettings`, and mark `stateFile` as deprecated.
- Add a reference page for `terracotta-state-filesystem` under `docs/content/modules/terracotta-state-filesystem/` if the module gets its own docs section.
- Update `CHANGELOG.md` with the new module and DSL changes.

## Open Questions

1. Should `stateSourceSettings` be a nested DSL block (`stateSourceSettings { put("path", ...) }`) instead of a `MapProperty<String, String>` for better Groovy DSL support?
2. Should the filesystem backend remain in `terracotta-core` as a built-in default, or is moving it to a separate module worth the extra module?
3. Should unknown backends fail at plugin application time or lazily when a task actually needs state?

## Risks

- **Risk**: Moving `FileSystemStateSource` out of `terracotta-core` breaks external callers that imported it directly.  
  **Mitigation**: Keep the package name the same (`io.github.beduality.terracotta.core.state`) by making `terracotta-state-filesystem` contribute classes to that package, or deprecate the old import path and provide a migration note in `CHANGELOG.md`.

- **Risk**: Adding a factory SPI increases complexity for a feature that currently has only one backend.  
  **Mitigation**: Keep the SPI minimal and keep the filesystem module as a default dependency of the Gradle plugin. Users who do not configure a backend see no behavior change.

- **Risk**: `stateFile` deprecation noise for existing users.  
  **Mitigation**: Deprecate softly; the property continues to work and the warning explains the new DSL.
