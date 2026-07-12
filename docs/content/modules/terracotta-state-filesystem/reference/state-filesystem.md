# State Filesystem Reference

The `terracotta-state-filesystem` module provides the default `"filesystem"` backend for Terracotta's pluggable state management SPI.

The Terracotta Gradle plugin depends on this module by default today, so the `"filesystem"` backend is available out of the box. It is still a pluggable dependency, and future versions may make it optional. If it is excluded from the plugin classpath, the plugin fails fast with a clear error that lists available factories and includes the dependency coordinates needed to restore the file backend.

## Factory id

```
filesystem
```

## Settings

| Key | Required | Default | Description |
|-----|----------|---------|-------------|
| `path` | No | `.terracotta-state.yml` in the project directory | Absolute or relative path to the YAML state file. |

## Front-end configuration

Frontends pass the `path` setting through `StateSourceConfig.settings`. The Terracotta Gradle plugin exposes this as `stateSourceSettings`; see the [Kotlin DSL configuration guide](../../gradle-plugin/how-to-guides/kotlin-dsl-configuration.md).

## Implementation notes

- `FileSystemStateSourceFactory` implements `StateSourceFactory` and is registered as a Java service.
- `FileSystemStateSource` writes the file atomically by creating a temporary sibling file and moving it into place.
- State is encoded and decoded by `YamlStateCodec` using the `TerracottaState` model.

See the [Dokka API Docs](../../../../apidocs/terracotta-core/index.html) and the [State Management explanation](../../core/explanation/state-management.md) for the full SPI and usage details.
