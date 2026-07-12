# Terracotta State Filesystem

The file-backed state backend for Terracotta.

This module implements the `terracotta-core` state SPI and persists run state to a YAML file on the local filesystem. The Gradle plugin depends on it by default today, so the `"filesystem"` backend is available out of the box. It remains a pluggable dependency, however, and can be replaced by another backend implementation if needed.

If the `filesystem` backend is not on the plugin classpath, the plugin fails fast with a clear error that lists the available factories and points back to this module.

See the [State Filesystem Reference](reference/state-filesystem.md) and the [State Management explanation](../core/explanation/state-management.md) for details.
