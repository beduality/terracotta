# State Management

`terracotta-core` defines a small SPI for persisting run state between executions. This lets callers (and the providers they drive) answer questions such as "which remote identity did this gallery item have last time?" without coupling core to any particular storage backend or build tool.

## What is persisted

`TerracottaState` stores a snapshot of the most recent run that touched state:

- **Schema version** of the persisted format.
- **Last run summary**: command name, start/finish timestamps, and an optional VCS commit SHA.
- **Project identifier** used by Terracotta.
- **Per-provider records**: published version IDs, gallery item identities, and a hash of the resolved metadata.

## Core state SPI

The SPI is intentionally minimal and lives in `io.github.beduality.terracotta.core.state`:

- `StateSource` — loads and saves `TerracottaState`.
- `StateSourceFactory` — creates a `StateSource` from a `StateSourceConfig` and is identified by a stable `id`.
- `StateSourceConfig` — carries the project directory and backend-specific settings.

`StateSourceFactory` implementations are discovered at runtime through Java's `ServiceLoader`. A backend JAR must contain:

```
META-INF/services/io.github.beduality.terracotta.core.state.StateSourceFactory
```

with the fully qualified name of the factory implementation.

## Using a backend

Core itself does not select a backend. A frontend constructs a `StateSourceConfig` with the project directory and the settings map for the chosen backend, then asks the matching factory for a `StateSource`:

```kotlin
import io.github.beduality.terracotta.core.state.StateSourceConfig

val config = StateSourceConfig(
    projectDir = projectDir,
    settings = mapOf("path" to "custom-state.yml"),
)
val source = factory.create(config)
source.save(state)
```

The backend `id`, the contents of `settings`, and the lifecycle of the saved state are all frontend concerns.

## Module-specific guidance

- For the file-backed `"filesystem"` backend, see the [State Filesystem reference](../../terracotta-state-filesystem/reference/state-filesystem.md).
- For configuring a state backend through the Terracotta Gradle plugin, see the [Kotlin DSL configuration guide](../../gradle-plugin/how-to-guides/kotlin-dsl-configuration.md).

See the [Dokka API Docs](../../../../apidocs/terracotta-core/index.html) for the full `io.github.beduality.terracotta.core.state` package.
