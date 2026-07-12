# Terracotta State Filesystem

The file-backed state backend for Terracotta.

This module implements the `terracotta-core` state SPI and persists run state to a YAML file on the local filesystem. It is the default backend used by the Terracotta Gradle plugin, so the `"filesystem"` backend is available out of the box. Because the SPI is pluggable, you can replace this backend with a custom implementation when a local file no longer fits your workflow.

## What it does

- Stores `TerracottaState` as a single `.terracotta-state.yml` file in the project directory.
- Writes the file atomically so a failed run cannot corrupt persisted state.
- Is discovered at runtime through Java's `ServiceLoader` under the factory id `"filesystem"`.

## When to use it

Use the filesystem backend when:

- You run Terracotta from a single machine or CI workspace.
- You want state to survive across Gradle daemon restarts and task reruns.
- You prefer a human-readable, diff-friendly state file for troubleshooting.

## When to replace it

Consider replacing the filesystem backend when:

- Multiple CI runners or team members need a shared view of state.
- State must live outside the build workspace for compliance or backup reasons.
- You want to store state in a database or object store instead of a file.

If the backend is excluded from the plugin classpath and no replacement is configured, the build fails fast with a clear error that lists available factories and points back to this module.

## Quick start

The Gradle plugin already includes this module, so no extra configuration is required. To customize the state file path:

```kotlin
terracotta {
    stateSource = "filesystem"
    stateSourceSettings["path"] = "state/terracotta.yml"
}
```

To remove or replace the backend, see the [Replace the Filesystem Backend how-to guide](how-to-guides/replace-filesystem-backend.md).

## Learn more

- [State Filesystem Reference](reference/state-filesystem.md) — factory id, settings, and implementation notes.
- [State Filesystem Design](explanation/state-filesystem.md) — why YAML and why file-backed by default.
- [Replace the Filesystem Backend](how-to-guides/replace-filesystem-backend.md) — how to exclude or replace the backend.
- [State Management explanation](../core/explanation/state-management.md) — the core state SPI and how to implement a custom backend.
- [Kotlin DSL configuration guide](../gradle-plugin/how-to-guides/kotlin-dsl-configuration.md) — Gradle-specific state backend configuration.
