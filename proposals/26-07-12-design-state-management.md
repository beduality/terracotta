---
description: Design proposal for a pluggable state-management layer in terracotta-core, starting with a file-backed implementation.
---

# State Management Design Proposal

## Problem

Terracotta computes a diff between the local `terracotta.yml` / Gradle DSL and the
remote registry state. Today that diff is purely declarative and transient: nothing
persisted between runs can reliably answer "what identity did this item have last time?".
This leads to:

- **Brittle identity matching**: gallery items, versions, and other entities are
  matched by unstable human-readable fields (title, version number, file name).
- **Lost cross-run context**: a CI runner that finishes `terracottaApply` leaves no
  trace of what it did; the next runner starts from scratch.
- **Unsafe retries**: a partially failed run cannot distinguish "already uploaded"
  from "not yet uploaded", risking duplicates.
- **Blocked CI features**: future cloud-backed locking, gateway state, and team-wide
  publish history all need a stable state abstraction.

## Scope

- Add a `state` subsystem inside `terracotta-core` (`io.github.beduality.terracotta.core.state`).
- Define a small, provider-agnostic `StateSource` interface.
- Provide a single concrete implementation: `FileSystemStateSource` that persists to
  `.terracotta-state.yml` next to the active config.
- Define the canonical persisted state schema.
- Wire the file source into the Gradle plugin as the default backend.
- Keep cloud / remote backends out of this change; the abstraction must only make
  them possible later.

Out of scope:

- Cloud or HTTP backends (follow-up work once the abstraction exists).
- Rewriting existing diff identity rules; this proposal supplies the tool, not the
  consumer.
- Concurrent access / distributed locking (separate proposal).
- State encryption or secrets storage.

## Public API contract

### `StateSource` interface

```kotlin
package io.github.beduality.terracotta.core.state

/**
 * Abstract backend for persisting and loading Terracotta run state.
 *
 * Implementations may read from and write to a local file, a CI-provided backend,
 * or a remote service. Callers receive an immutable [TerracottaState] snapshot.
 */
interface StateSource {
    /**
     * Loads the current persisted state.
     *
     * @return the loaded state, or an empty [TerracottaState] if none exists.
     */
    fun load(): TerracottaState

    /**
     * Persists the given state, replacing any previous state.
     *
     * @param state the state to persist.
     */
    fun save(state: TerracottaState)
}
```

### `TerracottaState` model

```kotlin
package io.github.beduality.terracotta.core.state

import java.time.Instant

/**
 * Root persisted state for a Terracotta project.
 *
 * @property version Schema version of the persisted file. Incremented when the
 *   on-disk format changes in a non-backwards-compatible way.
 * @property lastRun Summary of the most recent run that touched state.
 * @property projectId Stable project identifier used by Terracotta.
 * @property providers Per-provider state records.
 */
data class TerracottaState(
    val version: Int = 1,
    val lastRun: RunSummary? = null,
    val projectId: String? = null,
    val providers: Map<String, ProviderState> = emptyMap(),
)

/**
 * @property command The command/task that ran (e.g. `apply`, `plan`).
 * @property startedAt When the run began.
 * @property finishedAt When the run finished, if known.
 * @property commitSha Optional VCS commit SHA captured from the environment.
 */
data class RunSummary(
    val command: String,
    val startedAt: Instant,
    val finishedAt: Instant? = null,
    val commitSha: String? = null,
)

/**
 * @property versionIds IDs of versions known to have been published.
 * @property gallery Gallery item identities keyed by stable local identifier.
 * @property metadataHash Hash of the resolved metadata that produced this state.
 */
data class ProviderState(
    val versionIds: List<String> = emptyList(),
    val gallery: Map<String, GalleryItemIdentity> = emptyMap(),
    val metadataHash: String? = null,
)

/**
 * Stable identity for a single gallery item, used for cross-run matching.
 *
 * @property localKey Stable key from the local configuration.
 * @property remoteUrl Remote URL assigned by the provider after upload.
 * @property remoteId Optional provider-specific identifier.
 */
data class GalleryItemIdentity(
    val localKey: String,
    val remoteUrl: String? = null,
    val remoteId: String? = null,
)
```

### `FileSystemStateSource`

```kotlin
package io.github.beduality.terracotta.core.state

import java.nio.file.Path

/**
 * [StateSource] backed by a YAML file on the local filesystem.
 *
 * The default filename is `.terracotta-state.yml`. It is written atomically: a
 * temporary file is created next to the target, then moved into place.
 */
class FileSystemStateSource private constructor(
    private val file: Path,
) : StateSource {

    override fun load(): TerracottaState {
        if (!file.exists()) return TerracottaState()
        val yaml = file.readText()
        return YamlStateCodec.decode(yaml)
    }

    override fun save(state: TerracottaState) {
        val yaml = YamlStateCodec.encode(state)
        val temp = file.resolveSibling(".${file.fileName}.tmp")
        temp.writeText(yaml)
        temp.moveTo(file, overwrite = true)
    }

    companion object {
        const val DEFAULT_FILE_NAME = ".terracotta-state.yml"

        fun forFile(file: Path): FileSystemStateSource = FileSystemStateSource(file)

        fun forDirectory(directory: Path): FileSystemStateSource =
            FileSystemStateSource(directory.resolve(DEFAULT_FILE_NAME))
    }
}
```

### Default file name

The default persisted file name is:

```
.terracotta-state.yml
```

It lives in the same directory as the active `terracotta.yml`. The Gradle plugin can
optionally expose `stateFile` to override the location.

### YAML schema example

```yaml
version: 1
lastRun:
  command: apply
  startedAt: "2026-07-12T03:00:00Z"
  finishedAt: "2026-07-12T03:00:42Z"
  commitSha: a1b2c3d
projectId: my-awesome-mod
providers:
  modrinth:
    versionIds:
      - "1.0.0"
      - "1.0.1"
    gallery:
      mainScreenshot:
        localKey: mainScreenshot
        remoteUrl: https://cdn.modrinth.com/...
        remoteId: abc123
    metadataHash: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
```

## Internal component boundaries

- `terracotta-core` owns the `StateSource` interface, `TerracottaState`, and the
  `FileSystemStateSource` implementation.
- `terracotta-core` owns YAML encoding/decoding for the state file (a small internal
  codec reused by the file source).
- `terracotta-gradle-plugin` owns default file path resolution and exposes an optional
  `stateFile` DSL property.
- Providers and the diff engine remain consumers of state; they do not own persistence.

## Error-handling strategy

- `StateSource.load()` returns an empty state when the file is missing; missing state
  is normal, not an error.
- Corrupt state files throw a descriptive `IOException` with the path and the parse
  error.
- `StateSource.save()` writes atomically and throws `IOException` on disk failures.
- The apply task must not fail silently if state cannot be saved after a successful
  remote mutation; it should fail fast to avoid drift between reality and the persisted
  record.

## Integration sketch

1. Gradle plugin resolves the active config directory and creates a
   `FileSystemStateSource` pointing at `.terracotta-state.yml`.
2. Before planning, the plugin loads state and passes it into the operation context.
3. After apply succeeds, the plugin updates `lastRun`, provider records, and gallery
   identities, then calls `save()`.
4. `plan` loads state for read-only identity hints but does not write it.

## Open questions and risks

1. Should the state file be committed to Git? Recommendation: no, it is a build
   artifact and should be added to `.gitignore` by default.
2. How do we handle provider renames or slug changes? Store the Terracotta project
   identifier separately from provider slugs.
3. Schema versioning: `version` starts at `1`. Future incompatible changes bump the
   number and require a migration helper.
4. Should multiple concurrent local processes lock the state file? Out of scope for
   the first iteration; document that callers should avoid parallel apply tasks.
5. Cloud backend: when it arrives, it implements `StateSource` and the plugin selects
   it via configuration, leaving the core API unchanged.

## Success criteria

- `TerracottaState` and `StateSource` exist in `terracotta-core`.
- `FileSystemStateSource` can load and save `.terracotta-state.yml` round-trip.
- The Gradle plugin wires the file source as the default state backend.
- Missing state files are handled gracefully.
- Corrupt state files produce a clear `IOException`.
- Tests cover load/save round-trips, missing files, and parse errors.
- Docs describe the state file purpose and that it should not be committed.
