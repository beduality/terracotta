# Core API Reference

The `terracotta-core` module is published to Maven Central, allowing developers to build IDE plugins, build-tool plugins (Gradle/Maven), or custom integration workflows.

## Maven Dependency

```kotlin
implementation("io.github.beduality:terracotta-core:0.1.0")
```

---

## Canonical Models

### `TerracottaProject`

Holds project details and its versions:

```kotlin
data class TerracottaProject(
    val id: String,
    val name: String,
    val summary: String,
    val description: String,
    val versions: List<TerracottaVersion>,
    val tags: List<String>,
    val license: String
)
```

### `TerracottaVersion`

Holds version details:

```kotlin
data class TerracottaVersion(
    val version: String,
    val artifactPath: String,
    val gameVersions: List<String>
)
```

---

## Operations & Diff Engine

### `Operation`

A sealed interface representing mutations:

- `UpdateMetadata`: Represents updates to name, summary, and license.
- `UpdateDescription`: Represents updates to body/description.
- `UpdateTags`: Represents category/tag updates.
- `UploadVersion`: Represents uploading new version JARs.

### `DiffEngine`

Calculates changes required to transition from remote to local state:

```kotlin
object DiffEngine {
    fun diff(local: TerracottaProject, remote: TerracottaProject?): List<Operation>
}
```
