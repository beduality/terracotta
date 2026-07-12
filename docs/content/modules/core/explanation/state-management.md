# State Management

Terracotta can persist small amounts of run state between executions so that future runs can answer questions such as "which remote identity did this gallery item have last time?".

## What is persisted

The state file stores a snapshot of the most recent `terracottaApply` run:

- **Schema version** of the persisted format.
- **Last run summary**: command name, start/finish timestamps, and an optional VCS commit SHA.
- **Project identifier** used by Terracotta.
- **Per-provider records**: published version IDs, gallery item identities, and a hash of the resolved metadata.

## The state file

By default the Gradle plugin writes state to `.terracotta-state.yml` in the project directory using the `"filesystem"` backend. You can override the location with the DSL:

```kotlin
terracotta {
    stateSource.set("filesystem")
    stateSourceSettings.put("path", "custom-state.yml")
}
```

The older `stateFile` property is still supported but deprecated; it behaves like setting `stateSource = "filesystem"` with `stateSourceSettings["path"] = <file>`.

## Do not commit the state file

`.terracotta-state.yml` is a build artifact. It describes the state of the world at the time `terracottaApply` last ran, and it will change after every publish. Committing it would create unnecessary merge conflicts and could leak provider-specific identifiers that are local to your workflow.

The Terracotta `.gitignore` already excludes the default filename. If you change `stateFile`, add your custom path to `.gitignore` as well.

## Pluggable backends

`terracotta-core` exposes a small SPI for state backends:

- `StateSource` — loads and saves `TerracottaState`.
- `StateSourceFactory` — creates a `StateSource` from a `StateSourceConfig` and is identified by a stable `id`.
- `StateSourceConfig` — carries the project directory and backend-specific settings.

Backend implementations register a `StateSourceFactory` service in `META-INF/services/io.github.beduality.terracotta.core.state.StateSourceFactory`. The Gradle plugin discovers them with `java.util.ServiceLoader` and selects the factory whose `id` matches `terracotta.stateSource`.

### The filesystem backend

The Gradle plugin depends on `terracotta-state-filesystem` by default, so the `"filesystem"` backend is always available. It reads the `path` setting and falls back to `.terracotta-state.yml` in the project directory.

```kotlin
terracotta {
    stateSource.set("filesystem")
    stateSourceSettings.put("path", "custom-state.yml")
}
```

Future backends (for example, a cloud or CI-provided store) only need to implement the SPI, publish a module, and add it to the plugin classpath.

See the [provider interfaces](../reference/provider-interfaces.md), the [State Filesystem reference](../../terracotta-state-filesystem/reference/state-filesystem.md), and the [Dokka API Docs](../../../../apidocs/terracotta-core/index.html) for the full `io.github.beduality.terracotta.core.state` package.
