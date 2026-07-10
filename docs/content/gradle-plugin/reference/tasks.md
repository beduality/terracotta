# Gradle Tasks Reference

This reference describes the tasks provided by the Terracotta Gradle plugin.

---

## Tasks

### `terracottaPlan`

Generate a semantic diff comparing your local build configuration against the remote registry's project state. No changes are applied.

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

Applies the planned operations, updating registry metadata and uploading missing versions.

**Usage:**
```bash
./gradlew terracottaApply
```

**Requirements:**
- A valid `MODRINTH_TOKEN` environment variable or configured `modrinthToken` in the extension.
