# State Filesystem Design

The `terracotta-state-filesystem` module is the default backend for Terracotta's pluggable state management SPI. It stores run state in a single YAML file on the local filesystem.

## Why YAML

YAML was chosen for the default backend because the Minecraft community already uses it extensively:

- Server administrators configure Paper, Spigot, Bukkit, and plugin configs in YAML every day.
- Mod and plugin developers are familiar with YAML from resource packs, data packs, and project metadata.
- YAML is human-readable and diff-friendly, which makes state files easy to inspect, version-control, and debug.

For a tool that runs inside Gradle builds for Minecraft projects, using YAML lowers the cognitive overhead for the people most likely to read or troubleshoot the state file.

## Why file-backed by default

A local file is the smallest possible runtime dependency:

- No external service to configure, authenticate, or maintain.
- Works offline and in CI without extra credentials.
- Survives across Gradle daemon restarts and task reruns.
- Easy to delete or reset when reproducing an issue.

The trade-off is that the file lives on the machine running the build. Teams that share state across machines, or that want high availability, can replace the filesystem backend with a custom implementation without changing core logic.

## When to replace it

Consider a custom backend when:

- Multiple CI runners or team members need a shared view of state.
- State must survive outside the build workspace.
- Compliance or backup policies require storage in a database or object store.

The SPI is intentionally small: implement `StateSourceFactory` and `StateSource`, register the factory with `ServiceLoader`, and set `terracotta.stateSource` to the new factory id.

For the SPI details, see the [State Management explanation](../../core/explanation/state-management.md). For configuring or replacing the backend in a Gradle build, see the [Configure the Filesystem Backend how-to guide](../how-to-guides/configure-filesystem-backend.md).
