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
- `-`: Indicates a resource or metadata element (e.g. categories) that will be deleted or removed.
- `~`: Indicates metadata fields, categories, or descriptions that will be updated on the remote registry.

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

### `terracottaDestroy`

Deletes the remote project on all configured providers. This is a destructive operation that cannot be undone by Terracotta.

**Usage:**
```bash
# Preview what would be deleted without making remote calls
./gradlew terracottaDestroy --dry-run

# Delete the project on all providers after explicit confirmation
./gradlew terracottaDestroy --force
```

**Options:**

| Option | Meaning |
|--------|---------|
| `--force` / `-f` | Skip the confirmation prompt. Required in non-interactive environments such as CI. |
| `--versions-only` | Delete every published version while keeping the project page itself. |
| `--dry-run` | Print what would be destroyed and exit without making remote calls. |

!!! warning
    `terracottaDestroy` deletes remote data. Always run with `--dry-run` first, and never commit a CI step that runs `terracottaDestroy` without `--force` unless a human explicitly approves it.

---

### Per-Provider Tasks

For each provider you configure (e.g., "modrinth", "hangar"), the plugin will register three tasks:
- `terracottaPlan<ProviderName>` (e.g., `terracottaPlanModrinth`)
- `terracottaApply<ProviderName>` (e.g., `terracottaApplyHangar`)
- `terracottaDestroy<ProviderName>` (e.g., `terracottaDestroyModrinth`)

These work just like the aggregate tasks, but only operate on a single provider.

**Usage:**
```bash
# Plan changes for Modrinth only
./gradlew terracottaPlanModrinth

# Apply changes for Hangar only
./gradlew terracottaApplyHangar

# Destroy the project on Modrinth only
./gradlew terracottaDestroyModrinth --force

# Delete all versions on Hangar only, keeping the project page
./gradlew terracottaDestroyHangar --versions-only --force
```
