# API Reference

The Terracotta SDK provides a modular, platform-agnostic API for Minecraft project registry management.

## Package Structure

| Package | Purpose |
|---------|---------|
| `io.github.beduality.terracotta.core.model` | Canonical data models |
| `io.github.beduality.terracotta.core.model.loader` | Loader interface and registry |
| `io.github.beduality.terracotta.core.model.metadata` | Project metadata models and source |
| `io.github.beduality.terracotta.core.model.projectfile` | Project file cache and conventions |
| `io.github.beduality.terracotta.core.config` | `terracotta.yml` parsing |
| `io.github.beduality.terracotta.core.provider` | Provider interfaces |
| `io.github.beduality.terracotta.core.diff` | Diff engine and operations |
| `io.github.beduality.terracotta.provider.modrinth` | Modrinth provider implementation |

## Core Models

### TerracottaProject

The canonical representation of a Minecraft project.

```kotlin
data class TerracottaProject(
    val schemaVersion: Int = 1,
    val id: String,
    val name: String,
    val summary: String,
    val description: String,
    val versions: List<TerracottaVersion>,
    val tags: List<String>,
    val license: String,
)
```

**Fields**:

| Field | Type | Required | Default |
|-------|------|----------|---------|
| `schemaVersion` | `Int` | No | `1` |
| `id` | `String` | Yes | - |
| `name` | `String` | Yes | - |
| `summary` | `String` | Yes | - |
| `description` | `String` | Yes | - |
| `versions` | `List<TerracottaVersion>` | Yes | - |
| `tags` | `List<String>` | Yes | - |
| `license` | `String` | Yes | - |

### TerracottaVersion

Represents a single version of a project.

```kotlin
data class TerracottaVersion(
    val version: String,
    val artifactPath: String,
    val gameVersions: List<String>,
    val loaders: List<TerracottaLoader> = emptyList(),
    val environment: TerracottaEnvironment = TerracottaEnvironment.SERVER_ONLY,
    val releaseType: TerracottaReleaseType = TerracottaReleaseType.RELEASE,
    val changelog: String = "",
    val displayName: String = "",
)
```

### TerracottaEnvironment

| Value | ID | Description |
|-------|-----|-------------|
| `CLIENT_ONLY` | `"client_only"` | Client-side only content |
| `SERVER_ONLY` | `"server_only"` | Server-side only content |
| `UNIVERSAL` | `"universal"` | Works on both client and server |

### TerracottaLoader

`TerracottaLoader` is an interface representing a mod/plugin platform. Implementations are registered in `TerracottaLoaderRegistry` and detected from project files.

Built-in loader IDs:

| ID | Description |
|-----|-------------|
| `"bukkit"` | Bukkit API |
| `"bungeecord"` | BungeeCord proxy |
| `"fabric"` | Fabric loader |
| `"folia"` | Folia server |
| `"forge"` | Forge loader |
| `"neoforge"` | NeoForge loader |
| `"paper"` | Paper server (implies `spigot` and `bukkit`) |
| `"purpur"` | Purpur server |
| `"quilt"` | Quilt loader |
| `"spigot"` | Spigot server (implies `bukkit`) |
| `"sponge"` | Sponge API |
| `"velocity"` | Velocity proxy |
| `"waterfall"` | Waterfall proxy |

Loaders can declare a parent loader so that detecting a fork also records its parent chain (e.g. Paper implies Spigot and Bukkit).

### TerracottaReleaseType

| Value | ID | Description |
|-------|-----|-------------|
| `RELEASE` | `"release"` | Stable release |
| `BETA` | `"beta"` | Beta/testing release |
| `ALPHA` | `"alpha"` | Alpha/experimental release |

## Provider Interfaces

### ProviderFactory

Factory interface for creating registry-specific providers.

```kotlin
interface ProviderFactory {
    val id: String
    fun createStateProvider(token: String?): StateProvider
    fun createRegistryProvider(token: String?): RegistryProvider
}
```

**Methods**:

| Method | Description |
|--------|-------------|
| `id` | Unique registry identifier (e.g., "modrinth") |
| `createStateProvider(token)` | Create a state provider with optional auth token |
| `createRegistryProvider(token)` | Create a registry provider with optional auth token |

**Provider Discovery**: Providers are discovered via Java's `ServiceLoader` mechanism.

### StateProvider

Fetches remote project state from a registry.

```kotlin
interface StateProvider {
    suspend fun fetchProject(projectId: String): TerracottaProject?
}
```

**Methods**:

| Method | Description |
|--------|-------------|
| `fetchProject(projectId)` | Fetch the remote project, or `null` if not found |

### RegistryProvider

Applies changes to a registry.

```kotlin
interface RegistryProvider {
    suspend fun apply(projectId: String, operations: List<Operation>)
}
```

**Methods**:

| Method | Description |
|--------|-------------|
| `apply(projectId, operations)` | Apply the given operations to the remote project |

## Diff Engine

### DiffEngine

Computes the semantic diff between local and remote project states.

```kotlin
object DiffEngine {
    fun diff(
        local: TerracottaProject,
        remote: TerracottaProject?,
    ): List<Operation>
}
```

**Behavior**:

| Scenario | Operations |
|----------|------------|
| `remote == null` | `CreateProject` + `UploadVersion` for each version |
| Metadata changed | `UpdateMetadata` |
| Description changed | `UpdateDescription` |
| Tags changed | `UpdateTags` |
| New version exists | `UploadVersion` for that version |

### Operation

A diff operation describing a change to apply.

```kotlin
sealed interface Operation {
    val description: String
}
```

**Subtypes**:

| Operation | Description |
|-----------|-------------|
| `Operation.UpdateDescription(old, new)` | Update project description |
| `Operation.UpdateTags(oldTags, newTags)` | Update project tags |
| `Operation.UpdateMetadata(...)` | Update project metadata |
| `Operation.UploadVersion(version)` | Upload a new version |
| `Operation.CreateProject(project)` | Create a new project |

## Modrinth Provider

### ModrinthProviderFactory

The factory for creating Modrinth providers.

```kotlin
class ModrinthProviderFactory : ProviderFactory {
    override val id: String
    override fun createStateProvider(token: String?): StateProvider
    override fun createRegistryProvider(token: String?): RegistryProvider
}
```

### ModrinthStateProvider

Fetches project state from Modrinth.

```kotlin
class ModrinthStateProvider(
    private val client: ModrinthClient
) : StateProvider {
    suspend fun fetchProject(projectId: String): TerracottaProject?
}
```

### ModrinthRegistryProvider

Applies changes to Modrinth.

```kotlin
class ModrinthRegistryProvider(
    private val client: ModrinthClient
) : RegistryProvider {
    suspend fun apply(projectId: String, operations: List<Operation>)
}
```

### ModrinthClient

Low-level HTTP client for Modrinth API.

```kotlin
class ModrinthClient(token: String) {
    suspend fun request(path: String, method: HttpMethod, body: Any?): HttpResponse
}
```

## Dependencies

### Core SDK

```kotlin
implementation("io.github.beduality:terracotta-core:0.1.3")
```

| Dependency | Version | Purpose |
|------------|---------|---------|
| Kotlin | 2.3.21 | Language |
| Kotlinx Serialization | 1.7.1 | JSON serialization |
| Kotlinx Coroutines | 1.8.1 | Async operations |

### Modrinth Provider

```kotlin
implementation("io.github.beduality:terracotta-provider-modrinth:0.1.3")
```

| Dependency | Version | Purpose |
|------------|---------|---------|
| Ktor Client | 2.3.12 | HTTP client |
| Kotlinx Serialization | 1.7.1 | JSON serialization |