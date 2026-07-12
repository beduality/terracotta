# Terracotta State Filesystem

The file-backed state backend for Terracotta.

This module implements the `terracotta-core` state SPI and persists run state to a YAML file on the local filesystem. It provides the default `"filesystem"` backend, which you can install on the Gradle plugin classpath. Because the SPI is pluggable, you can replace this backend with a custom implementation when a local file no longer fits your workflow.

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

If the `"filesystem"` backend is requested but not on the classpath, the frontend fails fast with a clear error that lists available factories and points back to this module.

## Quick start

Add the module to your classpath and select the `"filesystem"` backend through your frontend. For example:

=== "Gradle Plugin"

    ```kotlin
    buildscript {
        dependencies {
            classpath("io.github.beduality:terracotta-state-filesystem:<version>")
        }
    }

    terracotta {
        stateSource = "filesystem"
        stateSourceSettings["path"] = "state/terracotta.yml"
    }
    ```

=== "Library"

    ```kotlin
    dependencies {
        implementation("io.github.beduality:terracotta-state-filesystem:<version>")
    }
    ```

    ```kotlin
    import io.github.beduality.terracotta.core.state.StateSourceConfig

    val config = StateSourceConfig(
        projectDir = projectDir,
        settings = mapOf("path" to "state/terracotta.yml"),
    )
    val source = factory.create(config)
    ```

To configure or replace the backend, see the [Configure the Filesystem Backend how-to guide](how-to-guides/configure-filesystem-backend.md).

## Learn more

- [State Filesystem Reference](reference/state-filesystem.md) — factory id, settings, and implementation notes.
- [State Filesystem Design](explanation/state-filesystem.md) — why YAML and why file-backed by default.
- [Configure the Filesystem Backend](how-to-guides/configure-filesystem-backend.md) — how to install, configure, or replace the backend.
- [State Management explanation](../core/explanation/state-management.md) — the core state SPI and how to implement a custom backend.
- [Kotlin DSL configuration guide](../gradle-plugin/how-to-guides/kotlin-dsl-configuration.md) — Gradle-specific state backend configuration.
