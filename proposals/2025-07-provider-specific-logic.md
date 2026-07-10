# Proposal: Provider-Specific Logic Layer

**Date**: 2025-07-10  
**Status**: Draft  
**Related**: Override Pattern, Narrow License and Tags

## Summary

Implement a provider-specific logic layer to handle platform-specific transformations:
- Loader mapping (`Λ_Hangar`, CurseForge loader → dependency)
- Numeric ID resolution (CurseForge game versions, dependencies)
- Platform distinction (stateful vs. append-only)

## Problem Statement

Each provider has unique requirements that can't be expressed in the canonical schema:

| Platform | Transformation Required | Example |
|----------|------------------------|---------|
| Modrinth | Direct mapping | Loader slug matches |
| CurseForge | Numeric ID resolution | "1.20.1" → 9843 |
| Hangar | Platform grouping | {FABRIC, FORGE} → (none) |

Currently, this logic is scattered or missing entirely.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Terracotta Core                         │
│                    (Canonical Schema)                       │
│          TerracottaProject, TerracottaVersion               │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              Provider-Specific Logic Layer                  │
│                    (Transformations)                        │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Modrinth    │  │  CurseForge  │  │   Hangar     │      │
│  │  Mappers     │  │  Resolvers   │  │  Mappers     │      │
│  │  Transform   │  │  ID Cache    │  │  Platform    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    API Clients                              │
│              (Platform-Specific HTTP)                       │
└─────────────────────────────────────────────────────────────┘
```

## Implementation Plan

### 1. Loader Mapping Interface

```kotlin
interface LoaderMapper {
    val canonicalLoader: TerracottaLoader
    fun mapToPlatform(): String?  // Returns platform name or null if unsupported
}

object ModrinthLoaderMapper : LoaderMapper {
    override val canonicalLoader: TerracottaLoader
        get() = TODO()  // Modrinth uses loaders directly
        
    override fun mapToPlatform(): String? = null  // No platform mapping
}

object CurseForgeLoaderMapper : LoaderMapper {
    override val canonicalLoader: TerracottaLoader
        get() = TODO()  // CurseForge uses loaders as dependencies
        
    override fun mapToPlatform(): String? = null  // No platform mapping
}

object HangarLoaderMapper : LoaderMapper {
    override val canonicalLoader: TerracottaLoader
        get() = TODO()  // Hangar groups loaders into platforms
        
    override fun mapToPlatform(): String? {
        // Maps to PAPER, VELOCITY, WATERFALL, or null
        return when (canonicalLoader) {
            TerracottaLoader.BUKKIT,
            TerracottaLoader.FOLIA,
            TerracottaLoader.PAPER,
            TerracottaLoader.PURPUR,
            TerracottaLoader.SPIGOT -> "PAPER"
            
            TerracottaLoader.VELOCITY -> "VELOCITY"
            
            TerracottaLoader.BUNGEECORD,
            TerracottaLoader.WATERFALL -> "WATERFALL"
            
            else -> null  // Unsupported by Hangar
        }
    }
}
```

### 2. Numeric ID Resolution Interface

```kotlin
interface IdResolver<T> {
    suspend fun resolve(slug: String): T
    suspend fun resolveFromCache(slug: String): T
    suspend fun cache(slug: String, id: T)
}

class CurseForgeGameVersionResolver(
    private val api: CurseForgeApiClient,
    private val cache: IdCache
) : IdResolver<Int> {
    override suspend fun resolve(slug: String): Int {
        // Query API if not cached
        return cache.getOrLoad("gameVersion:$slug") {
            val response = api.get("/api/game/versions") {
                parameter("filter-game-version", slug)
            }
            response.data.first().id
        }
    }
}

class CurseForgeLoaderResolver(
    private val api: CurseForgeApiClient,
    private val cache: IdCache
) : IdResolver<Int> {
    override suspend fun resolve(slug: String): Int {
        return cache.getOrLoad("loader:$slug") {
            val response = api.get("/api/game/dependencies") {
                parameter("filter-type", "modloader")
            }
            response.data.first { it.slug == slug }.id
        }
    }
}
```

### 3. Platform Distinction Interface

```kotlin
interface PlatformBehavior {
    val isStateful: Boolean  // Supports metadata updates
    val isAppendOnly: Boolean  // File-only uploads
    
    suspend fun applyProject(projectId: String, operations: List<Operation>)
    suspend fun applyFile(projectId: String, operations: List<Operation>)
}

class ModrinthPlatformBehavior : PlatformBehavior {
    override val isStateful = true
    override val isAppendOnly = false
    
    override suspend fun applyProject(projectId: String, operations: List<Operation>) {
        // Apply metadata updates
    }
    
    override suspend fun applyFile(projectId: String, operations: List<Operation>) {
        // Upload versions
    }
}

class CurseForgePlatformBehavior : PlatformBehavior {
    override val isStateful = false
    override val isAppendOnly = true
    
    override suspend fun applyProject(projectId: String, operations: List<Operation>) {
        // No-op (CurseForge doesn't support project metadata updates)
    }
    
    override suspend fun applyFile(projectId: String, operations: List<Operation>) {
        // Upload versions only
    }
}

class HangarPlatformBehavior : PlatformBehavior {
    override val isStateful = true
    override val isAppendOnly = false
    
    override suspend fun applyProject(projectId: String, operations: List<Operation>) {
        // Apply metadata updates
    }
    
    override suspend fun applyFile(projectId: String, operations: List<Operation>) {
        // Upload versions
    }
}
```

### 4. Transformation Pipeline

```kotlin
class ProviderTransformationPipeline(
    private val loaderMapper: LoaderMapper,
    private val idResolvers: List<IdResolver<*>>,
    private val platformBehavior: PlatformBehavior
) {
    suspend fun transformProject(project: TerracottaProject): PlatformProject {
        val transformedLoaders = project.loaders.mapNotNull { loader ->
            loaderMapper.mapToPlatform(loader)
        }
        
        val transformedGameVersions = project.gameVersions.map { version ->
            val idResolver = idResolvers.firstIsInstance<IdResolver<Int>>()
            idResolver.resolve(version)
        }
        
        return PlatformProject(
            loaders = transformedLoaders,
            gameVersions = transformedGameVersions,
            // ... other platform-specific transformations
        )
    }
    
    suspend fun filterOperations(operations: List<Operation>): List<Operation> {
        if (platformBehavior.isStateful) {
            return operations
        }
        // Remove metadata operations for append-only platforms
        return operations.filterIsInstance<Operation.UploadVersion>()
    }
}
```

## Modrinth Provider

### Loader Mapping
Modrinth uses loaders directly, no transformation needed.

### ID Resolution
Modrinth uses human-readable slugs for game versions, no numeric IDs.

### Platform Behavior
Stateful: Supports metadata updates and file uploads.

```kotlin
object ModrinthProviderLogic : ProviderLogic {
    override val loaderMapper = ModrinthLoaderMapper
    override val idResolvers = emptyList<IdResolver<*>>()
    override val platformBehavior = ModrinthPlatformBehavior()
}
```

## CurseForge Provider

### Loader Mapping
CurseForge maps loaders to dependency slugs:
- FABRIC → "fabric"
- FORGE → "forge"
- etc.

### ID Resolution
CurseForge requires numeric IDs:
- Game versions: "1.20.1" → 9843
- Dependencies: resolved by slug
- Game dependencies: resolved by slug

### Platform Behavior
Append-only: Only file uploads supported.

```kotlin
class CurseForgeProviderLogic(
    private val idCache: IdCache
) : ProviderLogic {
    override val loaderMapper = CurseForgeLoaderMapper
    override val idResolvers = listOf(
        CurseForgeGameVersionResolver(api, idCache),
        CurseForgeLoaderResolver(api, idCache),
        CurseForgeDependencyResolver(api, idCache)
    )
    override val platformBehavior = CurseForgePlatformBehavior()
}
```

## Hangar Provider

### Loader Mapping
Hangar groups loaders into platforms:
- {BUKKIT, FOLIA, PAPER, PURPUR, SPIGOT} → "PAPER"
- VELOCITY → "VELOCITY"
- {BUNGEECORD, WATERFALL} → "WATERFALL"

### ID Resolution
Hangar uses slugs, no numeric IDs.

### Platform Behavior
Stateful: Supports project updates and file uploads.

```kotlin
object HangarProviderLogic : ProviderLogic {
    override val loaderMapper = HangarLoaderMapper
    override val idResolvers = emptyList<IdResolver<*>>()
    override val platformBehavior = HangarPlatformBehavior()
}
```

## ID Cache Implementation

```kotlin
class IdCache(private val cacheFile: Path) {
    private val cache: MutableMap<String, Int> = mutableMapOf()
    
    init {
        load()
    }
    
    suspend fun getOrLoad(key: String, fetch: suspend () -> Int): Int {
        return cache.getOrPut(key) {
            fetch()
            save()  // Persist after fetch
        }
    }
    
    private fun load() {
        if (Files.exists(cacheFile)) {
            cacheFile.useLines { lines ->
                lines.map { it.split(":") }
                    .filter { it.size == 2 }
                    .forEach { (key, value) ->
                        cache[key] = value.toInt()
                    }
            }
        }
    }
    
    private fun save() {
        cacheFile.parent.toFile().mkdirs()
        cacheFile.bufferedWriter().use { writer ->
            cache.forEach { (key, value) ->
                writer.lineTo("$key:$value")
            }
        }
    }
}
```

## Testing Strategy

### Unit Tests
- Loader mapping for all loaders
- ID resolution with cache
- Platform behavior (stateful vs append-only)

### Integration Tests
- Modrinth: Full metadata + upload
- CurseForge: Only file upload (metadata skipped)
- Hangar: Full metadata + upload

### Mock Tests
- Mock API responses for ID resolution
- Test cache persistence

## Benefits

1. **Separation of Concerns**: Logic separate from API clients
2. **Testability**: Easy to unit test transformations
3. **Extensibility**: New platforms can implement their own logic
4. **Maintainability**: Clear, documented transformation rules

## Migration Path

1. ✅ Define ProviderLogic interface
2. 🔄 Implement ModrinthProviderLogic
3. 🔄 Implement CurseForgeProviderLogic
4. 🔄 Implement HangarProviderLogic
5. 🔄 Update provider implementations to use logic layer
6. 🔄 Add comprehensive tests
7. 🔄 Document transformation rules

## Next Steps

1. Define ProviderLogic interface
2. Implement loader mappers
3. Implement ID resolvers
4. Implement platform behaviors
5. Wire logic into providers
6. Test with real platforms
