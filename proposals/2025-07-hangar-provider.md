# Proposal: Hangar Provider Implementation

**Date**: 2025-07-10  
**Status**: Draft  
**Related**: External YAML Configuration

## Summary

Implement `TerracottaProvider` for Hangar (PaperMC plugin registry) with support for:
- Hangar REST API v1 authentication (JWT-based)
- Platform-grouped loader mapping (PAPER, VELOCITY, WATERFALL)
- Dynamic channel creation
- Project metadata management and file uploads

## Problem Statement

The `TerracottaProvider` interface exists but only Modrinth has a concrete implementation. Hangar requires distinct handling due to:

1. **JWT Authentication**: Time-bound JWT exchange (not static API key)
2. **Platform Grouping**: Multiple loaders map to single Hangar platform
3. **Channel Management**: Custom channels must be created if missing
4. **File-Only Uploads**: Append-only like CurseForge, but with some stateful features

## Hangar API Characteristics

### Authentication
- Uses JWT tokens obtained via API key
- Endpoint: `POST /api/v1/authenticate?apiKey=key`
- Token expires after time period (must refresh)

### Project Identification
- Uses slug (URL-friendly name) as primary identifier
- Example: `https://hangar.papermc.io/tr7zw/NBTAPI`

### Loader to Platform Mapping

| Canonical Loader | Hangar Platform | Notes |
|-----------------|-----------------|-------|
| BUKKIT | PAPER | Mapped to Paper platform |
| FOLIA | PAPER | Mapped to Paper platform |
| PAPER | PAPER | Direct match |
| PURPUR | PAPER | Mapped to Paper platform |
| SPIGOT | PAPER | Mapped to Paper platform |
| VELOCITY | VELOCITY | Direct match |
| BUNGEECORD | WATERFALL | Mapped to Waterfall |
| WATERFALL | WATERFALL | Direct match |
| FABRIC | (none) | Unsupported - skipped |
| FORGE | (none) | Unsupported - skipped |
| QUILT | (none) | Unsupported - skipped |
| NEOFORGE | (none) | Unsupported - skipped |
| SPONGE | (none) | Unsupported - skipped |

This requires a non-injective mapping function:

```
Λ_Hangar(loader) = 
    PAPER if loader in {BUKKIT, FOLIA, PAPER, PURPUR, SPIGOT}
    VELOCITY if loader == VELOCITY
    WATERFALL if loader in {BUNGEECORD, WATERFALL}
    null otherwise
```

### Upload Endpoint
```
POST /api/v1/projects/{slug}/upload

multipart/form-data:
  - versionUpload: JSON with version metadata
  - file: Binary JAR file
```

### Version Metadata Example
```json
{
  "version": "1.0.0",
  "channel": "Release",
  "description": "Release notes...",
  "platformDependencies": {
    "PAPER": ["1.20.1", "1.20.2"],
    "VELOCITY": ["1.1", "1.2"]
  },
  "dependencies": {
    "PAPER": [
      {
        "name": "LuckPerms",
        "required": true,
        "externalUrl": null
      }
    ]
  }
}
```

## Implementation Plan

### 1. Hangar Provider Factory

```kotlin
object HangarProviderFactory : ProviderFactory {
    override val id = "hangar"
    
    override fun createStateProvider(token: String?): StateProvider =
        HangarStateProvider(token)
    
    override fun createRegistryProvider(token: String?): RegistryProvider =
        HangarRegistryProvider(token)
}
```

### 2. JWT Token Manager

```kotlin
class HangarTokenManager(private val apiKey: String) {
    private var jwt: String? = null
    private var expiresAt: Long = 0
    
    suspend fun getToken(): String {
        if (jwt == null || System.currentTimeMillis() > expiresAt) {
            refresh()
        }
        return jwt!!
    }
    
    private suspend fun refresh() {
        val response = api.post("/api/v1/authenticate") {
            parameter("apiKey", apiKey)
        }
        jwt = response.jwt
        expiresAt = response.expiresAt - REFRESH_MARGIN
    }
}
```

### 3. Loader Platform Mapper

```kotlin
object HangarLoaderMapper {
    fun mapToPlatforms(loaders: List<TerracottaLoader>): Set<String> {
        return loaders.mapNotNull { loader ->
            when (loader) {
                TerracottaLoader.BUKKIT,
                TerracottaLoader.FOLIA,
                TerracottaLoader.PAPER,
                TerracottaLoader.PURPUR,
                TerracottaLoader.SPIGOT -> "PAPER"
                
                TerracottaLoader.VELOCITY -> "VELOCITY"
                
                TerracottaLoader.BUNGEECORD,
                TerracottaLoader.WATERFALL -> "WATERFALL"
                
                else -> null  // Unsupported loader
            }
        }.toSet()
    }
}
```

### 4. Hangar State Provider

```kotlin
class HangarStateProvider(private val token: String?) : StateProvider {
    private val api = HangarApiClient(token)
    
    override suspend fun fetchProject(projectId: String): TerracottaProject? {
        val slug = projectId  // Hangar uses slug as ID
        
        try {
            val project = api.get("/api/v1/projects/$slug")
            val versions = api.get("/api/v1/projects/$slug/versions")
            
            return TerracottaProject(
                id = slug,
                name = project.name,
                summary = project.summary,
                description = project.description,
                versions = versions.map { toTerracottaVersion(it) },
                tags = project.categories ?: emptyList(),
                license = project.license ?: "UNLICENSED",
            )
        } catch (e: ApiException) {
            if (e.statusCode == 404) return null
            throw e
        }
    }
    
    private fun toTerracottaVersion(version: HangarVersion): TerracottaVersion {
        // Map Hangar version to canonical TerracottaVersion
        return TerracottaVersion(
            version = version.version,
            artifactPath = version.fileName,
            gameVersions = extractGameVersions(version.platformDependencies),
            loaders = extractLoaders(version.platformDependencies),
            releaseType = mapReleaseType(version.channel),
            environment = TerracottaEnvironment.SERVER_ONLY,  // Hangar is server-only
        )
    }
    
    private fun mapReleaseType(channel: String): TerracottaReleaseType {
        return when (channel.lowercase()) {
            "release" -> TerracottaReleaseType.RELEASE
            "snapshot", "beta", "alpha" -> TerracottaReleaseType.BETA
            else -> TerracottaReleaseType.ALPHA
        }
    }
}
```

### 5. Hangar Registry Provider

```kotlin
class HangarRegistryProvider(private val token: String?) : RegistryProvider {
    private val api = HangarApiClient(token)
    private val tokenManager = HangarTokenManager(token!!)
    
    override suspend fun apply(projectId: String, operations: List<Operation>) {
        val slug = projectId
        
        for (operation in operations) {
            when (operation) {
                is Operation.CreateProject -> {
                    createProjectIfNotExists(slug, operation.project)
                }
                
                is Operation.UpdateMetadata -> {
                    updateProjectMetadata(slug, operation)
                }
                
                is Operation.UploadVersion -> {
                    uploadVersion(slug, operation.version)
                }
                
                else -> {
                    // No-op for Hangar (no UpdateTags, UpdateDescription support)
                    logger.warn("Operation not supported by Hangar: $operation")
                }
            }
        }
    }
    
    private suspend fun createProjectIfNotExists(slug: String, project: TerracottaProject) {
        // Check if project exists
        if (api.getOrNull("/api/v1/projects/$slug") != null) {
            logger.info("Project $slug already exists")
            return
        }
        
        // Create project
        api.post("/api/v1/projects") {
            json {
                "name" to project.name
                "slug" to slug
                "summary" to project.summary
                "description" to project.description
                "license" to project.license.id
                "categories" to project.tags.take(1)  // Primary category
                "additionalCategories" to project.tags.drop(1)  // Additional categories
            }
        }
    }
    
    private suspend fun uploadVersion(slug: String, version: TerracottaVersion) {
        // Map loaders to Hangar platforms
        val platforms = HangarLoaderMapper.mapToPlatforms(version.loaders)
        
        if (platforms.isEmpty()) {
            logger.warn("No supported platforms for version ${version.version}")
            return
        }
        
        // Build platform dependencies
        val platformDependencies = platforms.associateWith { platform ->
            version.gameVersions
        }
        
        // Upload
        api.post("/api/v1/projects/$slug/upload") {
            multipart {
                part("versionUpload", """
                    {
                        "version": "${version.version}",
                        "channel": "${version.releaseType.id}",
                        "description": "${version.changelog}",
                        "platformDependencies": ${platformDependencies.toJSONString()},
                        "dependencies": ${buildDependencies(version.dependencies).toJSONString()}
                    }
                """.trimIndent())
                part("file", version.artifactPath)
            }
        }
    }
    
    private suspend fun ensureChannelExists(slug: String, channel: String) {
        // Check if channel exists
        val channels = api.get("/api/v1/projects/$slug/channels")
        if (channel in channels.map { it.name }) return
        
        // Create channel
        api.post("/api/v1/projects/$slug/channels") {
            json {
                "name" to channel
                "color" to "green"
            }
        }
    }
}
```

## Platform-Specific Handling

### 1. Unsupported Loaders

Loaders not supported by Hangar (FABRIC, FORGE, QUILT, NEOFORGE, SPONGE) are silently skipped with a warning.

### 2. Environment Handling

Hangar is server-only. CLIENT_ONLY and UNIVERSAL are mapped to SERVER_ONLY with appropriate warnings.

### 3. Channel Creation

If a release type specifies a channel that doesn't exist, it's created automatically before upload.

## Testing Strategy

### Unit Tests
- Loader platform mapping (all loader combinations)
- Channel name resolution
- Platform dependency building

### Integration Tests
- Create test project on hangar.papermc.io
- Upload test version
- Verify project metadata
- Verify file uploaded

### Mock Tests
- Mock JWT token refresh
- Mock API responses for various scenarios
- Test error handling

## Dependencies

```kotlin
dependencies {
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
}
```

## Migration Path

1. Create `terracotta-provider-hangar` module
2. Implement Hangar API client
3. Implement StateProvider
4. Implement RegistryProvider
5. Add HangarProviderFactory to ServiceLoader
6. Test with real Hangar instance
7. Update documentation

## Next Steps

1. ✅ Create HangarProviderFactory
2. 🔄 Implement HangarApiClient
3. 🔄 Implement HangarStateProvider
4. 🔄 Implement HangarRegistryProvider
5. 🔄 Test with real Hangar instance
6. 🔄 Document provider usage

## References

- [Hangar API Documentation](https://hangar.papermc.io/api)
- [Hangar API GitHub](https://github.com/HangarMC/Hangar)
- [PaperMC Plugins](https://hangar.papermc.io)
