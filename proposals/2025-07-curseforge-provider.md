# Proposal: CurseForge Provider Implementation

**Date**: 2025-07-10  
**Status**: Draft  
**Related**: External YAML Configuration, Override Pattern

## Summary

Implement `TerracottaProvider` for CurseForge with support for:
- Static API key authentication
- Numeric ID resolution (game versions, loaders, dependencies)
- Append-only file uploads (no project metadata updates)
- Flat form-data multipart uploads

## Problem Statement

CurseForge requires distinct handling due to:

1. **Numeric IDs**: Uses numeric IDs for game versions, loaders, and dependencies (not slugs)
2. **Append-Only**: Upload API is file-only; no project metadata update support
3. **Flat Multipart**: Single JSON metadata field (no nested structure)
4. **No Dependency Linking**: Dependencies must be resolved by slug and mapped to numeric IDs

## CurseForge API Characteristics

### Authentication
- Static API key in header: `X-Api-Token: <key>`
- No token refresh needed

### Project Identification
- Uses numeric project ID: `123456`
- Example: `https://www.curseforge.com/minecraft/mc-mods/fabric-api` → project ID `306612`

### Upload Endpoint
```
POST /api/projects/{projectId}/upload-file

multipart/form-data:
  - metadata: JSON with file metadata
  - file: Binary JAR file
```

### Metadata Structure
```json
{
  "changelog": "Release notes...",
  "changelogType": "markdown",
  "gameVersions": [9843, 9844],  // Numeric IDs
  "gameVersionNames": ["Client", "Server"],
  "releaseType": "release",
  "relations": {
    "projects": [
      {
        "slug": "fabric-api",
        "projectID": "306612",
        "type": "requiredDependency"
      }
    ]
  }
}
```

## Implementation Plan

### 1. CurseForge Provider Factory

```kotlin
object CurseForgeProviderFactory : ProviderFactory {
    override val id = "curseforge"
    
    override fun createStateProvider(token: String?): StateProvider =
        CurseForgeStateProvider(token)
    
    override fun createRegistryProvider(token: String?): RegistryProvider =
        CurseForgeRegistryProvider(token)
}
```

### 2. ID Resolution Cache

CurseForge requires numeric IDs for:
- Game versions (e.g., "1.20.1" → 9843)
- Game dependencies (e.g., "fabric" → 467)
- Mod dependencies (resolved by slug)

```kotlin
class CurseForgeIdCache(private val cacheFile: Path) {
    private val cache = mutableMapOf<String, Int>()
    
    init {
        load()
    }
    
    suspend fun getGameVersionId(version: String): Int {
        return cache.getOrPut("version:$version") {
            fetchGameVersionId(version)
        }
    }
    
    suspend fun getLoaderId(loaderSlug: String): Int {
        return cache.getOrPut("loader:$loaderSlug") {
            fetchLoaderId(loaderSlug)
        }
    }
    
    suspend fun getProjectIdBySlug(slug: String): Int {
        return cache.getOrPut("project:$slug") {
            fetchProjectIdBySlug(slug)
        }
    }
    
    private suspend fun fetchGameVersionId(version: String): Int {
        val response = api.get("/api/game/versions") {
            parameter("filter-game-version", version)
        }
        return response.data.first().id
    }
    
    private suspend fun fetchLoaderId(loaderSlug: String): Int {
        val response = api.get("/api/game/dependencies") {
            parameter("filter-type", "modloader")
        }
        return response.data.first { it.slug == loaderSlug }.id
    }
    
    private suspend fun fetchProjectIdBySlug(slug: String): Int {
        val response = api.get("/api/mods") {
            parameter("slug", slug)
        }
        return response.data.first().id
    }
}
```

### 3. Loader to CurseForge Dependency Mapping

| Canonical Loader | CurseForge Dependency Slug | Notes |
|-----------------|---------------------------|-------|
| FABRIC | fabric | Direct match |
| FORGE | forge | Direct match |
| QUILT | quilt | Direct match |
| NEOFORGE | neoforge | Direct match |
| PAPER, SPIGOT, etc. | bukkit | Mapped to bukkit |
| VELOCITY | velocity | Direct match |

```kotlin
object CurseForgeLoaderMapper {
    fun mapToDependencySlug(loader: TerracottaLoader): String {
        return when (loader) {
            TerracottaLoader.FABRIC -> "fabric"
            TerracottaLoader.FORGE -> "forge"
            TerracottaLoader.QUILT -> "quilt"
            TerracottaLoader.NEOFORGE -> "neoforge"
            TerracottaLoader.PAPER,
            TerracottaLoader.SPIGOT,
            TerracottaLoader.BUKKIT,
            TerracottaLoader.FOLIA,
            TerracottaLoader.PURPUR -> "bukkit"
            
            TerracottaLoader.VELOCITY -> "velocity"
            
            TerracottaLoader.BUNGEECORD,
            TerracottaLoader.WATERFALL -> "waterfall"
        }
    }
}
```

### 4. CurseForge State Provider

```kotlin
class CurseForgeStateProvider(private val token: String?) : StateProvider {
    private val api = CurseForgeApiClient(token)
    private val cache = CurseForgeIdCache(Path.of(".terracotta", "curseforge-cache.json"))
    
    override suspend fun fetchProject(projectId: String): TerracottaProject? {
        val numericId = projectId.toIntOrNull()
            ?: throw IllegalArgumentException("CurseForge requires numeric project ID")
        
        try {
            val project = api.get("/api/mods/$numericId")
            val files = api.get("/api/mods/$numericId/files") {
                parameter("pageSize", "100")
            }
            
            return TerracottaProject(
                id = project.id.toString(),
                name = project.name,
                summary = project.summary,
                description = project.description,
                versions = files.map { toTerracottaVersion(it) },
                tags = project.categories?.map { it.name } ?: emptyList(),
                license = project.license?.id ?: "UNLICENSED",
            )
        } catch (e: ApiException) {
            if (e.statusCode == 404) return null
            throw e
        }
    }
    
    private fun toTerracottaVersion(file: CurseForgeFile): TerracottaVersion {
        return TerracottaVersion(
            version = file.fileDate.toString(),
            artifactPath = file.fileName,
            gameVersions = file.gameVersions,
            loaders = file.modLoaders.map { TerracottaLoader.fromId(it) },
            releaseType = mapReleaseType(file.releaseType),
            environment = TerracottaEnvironment.SERVER_ONLY,  // CurseForge supports client/server
        )
    }
    
    private fun mapReleaseType(releaseType: Int): TerracottaReleaseType {
        return when (releaseType) {
            1 -> TerracottaReleaseType.RELEASE
            2 -> TerracottaReleaseType.BETA
            3 -> TerracottaReleaseType.ALPHA
            else -> TerracottaReleaseType.RELEASE
        }
    }
}
```

### 5. CurseForge Registry Provider

```kotlin
class CurseForgeRegistryProvider(private val token: String?) : RegistryProvider {
    private val api = CurseForgeApiClient(token)
    private val cache = CurseForgeIdCache(Path.of(".terracotta", "curseforge-cache.json"))
    
    override suspend fun apply(projectId: String, operations: List<Operation>) {
        val numericId = projectId.toIntOrNull()
            ?: throw IllegalArgumentException("CurseForge requires numeric project ID")
        
        for (operation in operations) {
            when (operation) {
                is Operation.UploadVersion -> {
                    uploadVersion(numericId, operation.version)
                }
                
                else -> {
                    logger.warn("Operation not supported by CurseForge: $operation")
                }
            }
        }
    }
    
    private suspend fun uploadVersion(projectId: Int, version: TerracottaVersion) {
        // Resolve game version IDs
        val gameVersionIds = version.gameVersions.map { 
            cache.getGameVersionId(it) 
        }
        
        // Resolve dependency IDs
        val dependencyProjects = version.dependencies.map { dep ->
            val depProjectId = cache.getProjectIdBySlug(dep.projectId)
            
            CurseForgeDependency(
                slug = dep.projectId,
                projectID = depProjectId.toString(),
                type = mapDependencyType(dep.dependencyType)
            )
        }
        
        // Build metadata
        val metadata = CurseForgeUploadMetadata(
            changelog = version.changelog,
            changelogType = "markdown",
            gameVersions = gameVersionIds,
            gameVersionNames = listOf("Client", "Server"),  // CurseForge specific
            releaseType = version.releaseType.id,
            relations = CurseForgeRelations(projects = dependencyProjects)
        )
        
        // Upload
        api.post("/api/projects/$projectId/upload-file") {
            multipart {
                part("metadata", metadata.toJSONString())
                part("file", version.artifactPath)
            }
        }
    }
    
    private fun mapDependencyType(type: String): String {
        return when (type) {
            "required" -> "requiredDependency"
            "optional" -> "optionalDependency"
            "embedded" -> "embeddedLibrary"
            "incompatible" -> "incompatible"
            else -> "requiredDependency"
        }
    }
}
```

## Platform-Specific Handling

### 1. Append-Only Limitation

CurseForge does NOT support:
- Project metadata updates
- Description changes
- Tag updates

**Solution**: Skip metadata operations with warning, only upload files

### 2. Client/Server Environment

CurseForge uses `gameVersionNames` for client/server filtering:
- `["Client"]` → client_only
- `["Server"]` → server_only
- `["Client", "Server"]` → universal

### 3. Dependency Resolution

Dependencies are resolved from slug to numeric ID:
1. Look up slug in cache
2. If not found, query API and cache result
3. Use numeric ID in upload

## Testing Strategy

### Unit Tests
- ID cache loading/saving
- Loader to dependency mapping
- Game version ID resolution

### Integration Tests
- Create test project on CurseForge
- Upload test version
- Verify file appears on site

### Mock Tests
- Mock API responses for ID resolution
- Test error handling for missing dependencies

## Dependencies

```kotlin
dependencies {
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
}
```

## Migration Path

1. Create `terracotta-provider-curseforge` module
2. Implement CurseForge API client
3. Implement ID cache with filesystem persistence
4. Implement StateProvider
5. Implement RegistryProvider
6. Add CurseForgeProviderFactory to ServiceLoader
7. Test with real CurseForge project
8. Update documentation

## Next Steps

1. ✅ Create CurseForgeProviderFactory
2. 🔄 Implement CurseForgeApiClient
3. 🔄 Implement CurseForgeIdCache
4. 🔄 Implement CurseForgeStateProvider
5. 🔄 Implement CurseForgeRegistryProvider
6. 🔄 Test with real CurseForge project
7. 🔄 Document provider usage

## References

- [CurseForge REST API](https://docs.curseforge.com/rest-api)
- [CurseForge Upload API](https://www.curseforge.com/docs/api)
