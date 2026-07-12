# State Management

Terracotta can persist small amounts of run state between executions so that future runs can answer questions such as "which remote identity did this gallery item have last time?".

## What is persisted

The state file stores a snapshot of the most recent `terracottaApply` run:

- **Schema version** of the persisted format.
- **Last run summary**: command name, start/finish timestamps, and an optional VCS commit SHA.
- **Project identifier** used by Terracotta.
- **Per-provider records**: published version IDs, gallery item identities, and a hash of the resolved metadata.

## The state file

By default the Gradle plugin writes state to `.terracotta-state.yml` in the project directory. You can override the location with the DSL:

```kotlin
terracotta {
    stateFile.set(file("custom-state.yml"))
}
```

## Do not commit the state file

`.terracotta-state.yml` is a build artifact. It describes the state of the world at the time `terracottaApply` last ran, and it will change after every publish. Committing it would create unnecessary merge conflicts and could leak provider-specific identifiers that are local to your workflow.

The Terracotta `.gitignore` already excludes the default filename. If you change `stateFile`, add your custom path to `.gitignore` as well.

## Pluggable backend

`terracotta-core` exposes a small `StateSource` interface. The Gradle plugin ships with a file-backed implementation today; cloud or remote backends can be added later without changing the public API.

See the [provider interfaces](../reference/provider-interfaces.md) and the [Dokka API Docs](../../../../apidocs/terracotta-core/index.html) for the full `io.github.beduality.terracotta.core.state` package.
