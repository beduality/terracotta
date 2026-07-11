# Gradle Tasks Reference

This reference describes the tasks provided by the Terracotta Gradle plugin.

---

## Tasks

### `terracottaPlan`

Generate semantic diffs comparing your local build configuration against all configured remote registries' project states. No changes are applied.

**Usage:**
```bash
./gradlew terracottaPlan
```

**Output Indicators:**
- `+`: Indicates a resource (e.g. version) that will be uploaded or created.
- `-`: Indicates a resource or metadata element (e.g. tags) that will be deleted or removed.
- `~`: Indicates metadata fields, tags, or descriptions that will be updated on the remote registry.

---

### `terracottaApply`

Applies all planned operations, updating registry metadata and uploading missing versions for all configured providers.

**Usage:**
```bash
./gradlew terracottaApply
```

**Requirements:**
- Valid per-provider tokens configured (e.g., `MODRINTH_TOKEN` for Modrinth or `HANGAR_TOKEN` for Hangar).

---

### Per-Provider Tasks

For each provider you configure (e.g., "modrinth", "hangar"), the plugin will register two tasks:
- `terracottaPlan<ProviderName>` (e.g., `terracottaPlanModrinth`)
- `terracottaApply<ProviderName>` (e.g., `terracottaApplyHangar`)

These work just like the aggregate tasks, but only operate on a single provider.

**Usage:**
```bash
# Plan changes for Modrinth only
./gradlew terracottaPlanModrinth

# Apply changes for Hangar only
./gradlew terracottaApplyHangar
```
