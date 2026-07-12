# State Filesystem Reference

The `terracotta-state-filesystem` module provides the default `"filesystem"` backend for Terracotta's pluggable state management SPI. It persists run state to a single YAML file on the local filesystem.

Add this module to your classpath to make the `"filesystem"` backend available. If it is missing and a frontend selects `"filesystem"`, the frontend fails fast with a clear error that lists available factories and includes the dependency coordinates needed to restore the backend.

## Factory id

```
filesystem
```

## Settings

| Key | Required | Default | Description |
|-----|----------|---------|-------------|
| `path` | No | `.terracotta-state.yml` in the project directory | Absolute or relative path to the YAML state file. |

## Default state file

By default, the backend reads and writes `.terracotta-state.yml` in the project directory. The file name starts with a dot so it is hidden on Unix-like systems, keeping project roots tidy. You can override the path through the `path` setting.

## Usage examples

=== "Gradle Plugin"

    ```kotlin
    terracotta {
        stateSource.set("filesystem")
        stateSourceSettings.put("path", "state/terracotta.yml")
    }
    ```

=== "Core SPI"

    ```kotlin
    import io.github.beduality.terracotta.core.state.StateSourceConfig

    val config = StateSourceConfig(
        projectDir = projectDir,
        settings = mapOf("path" to "custom-state.yml"),
    )
    val source = factory.create(config)
    ```

## Behavior

- Reads the existing state file on load; if the file does not exist, the backend returns an empty state.
- Writes the state file atomically by creating a temporary sibling file and moving it into place, so a failed write cannot corrupt the previous state.
- Stores state as YAML using the `TerracottaState` model.

## Implementation notes

- `FileSystemStateSourceFactory` implements `StateSourceFactory` and is registered as a Java service under `META-INF/services/io.github.beduality.terracotta.core.state.StateSourceFactory`.
- `FileSystemStateSource` performs the atomic file operations.
- `YamlStateCodec` handles YAML encoding and decoding.

## See also

- [State Filesystem Design](../explanation/state-filesystem.md) — why YAML and why file-backed by default.
- [Configure the Filesystem Backend](../how-to-guides/configure-filesystem-backend.md) — how to install, configure, or replace the backend.
- [State Management explanation](../../core/explanation/state-management.md) — the core state SPI.
- [Kotlin DSL configuration guide](../../gradle-plugin/how-to-guides/kotlin-dsl-configuration.md) — Gradle-specific state backend configuration.
- [Dokka API Docs](../../../../apidocs/terracotta-core/index.html) for the full `io.github.beduality.terracotta.core.state` package.
