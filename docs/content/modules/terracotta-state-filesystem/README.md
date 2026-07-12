# Terracotta State Filesystem

The file-backed state backend for Terracotta.

This module implements the `terracotta-core` state SPI and persists run state to a YAML file on the local filesystem. The Gradle plugin depends on it by default, so the `"filesystem"` backend is available out of the box.

See the [State Filesystem Reference](reference/state-filesystem.md) and the [State Management explanation](../core/explanation/state-management.md) for details.
