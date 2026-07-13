# Proposal: Override Pattern

**Date**: 2025-07-10  
**Status**: Draft  
**Related**: External YAML Configuration

## Summary

Add support for a two-tier configuration model: root-level fields act as shared defaults (canonical), and `providers.*.overrides` apply platform-specific customization. This allows DRY configuration while preserving access to platform-specific features.

## Problem Statement

Current `TerracottaProviderExtension` only has `projectId` and `token`:

```kotlin
abstract class TerracottaProviderExtension(private val name: String) : Named {
    abstract val projectId: Property<String>
    abstract val token: Property<String>
}
```

This forces duplication of common settings across provider blocks:

```kotlin
terracotta {
    providers {
        create("modrinth") {
            projectId.set("AABBCCDD")
            token.set(System.getenv("MODRINTH_TOKEN"))
            // Must duplicate these for each provider
            loaders.set(listOf(TerracottaLoader.FABRIC, TerracottaLoader.PAPER))
            gameVersions.set(listOf("1.20.1", "1.20.2"))
            environment.set(TerracottaEnvironment.UNIVERSAL)
        }
        create("curseforge") {
            projectId.set("123456")
            token.set(System.getenv("CURSEFORGE_TOKEN"))
            loaders.set(listOf(TerracottaLoader.FABRIC))  // Duplicated!
            gameVersions.set(listOf("1.20.1", "1.20.2"))  // Duplicated!
            environment.set(TerracottaEnvironment.UNIVERSAL)  // Duplicated!
        }
    }
}
```

## Solution: Root Defaults + Overrides Pattern

### YAML Configuration Structure

```yaml
# terracotta.yaml
# Root-level fields act as canonical defaults
loaders: ["fabric", "paper"]
game_versions: ["1.20.1", "1.20.2"]
environment: "universal"
release_type: "release"
changelog: "Standard release."

providers:
  modrinth:
    project_id: "AABBCCDD"
    token: ${MODRINTH_TOKEN}
    overrides:
      loaders: ["fabric"]  # Modrinth only supports fabric
      changelog: "Modrinth-specific notes"

  hangar:
    project_id: "example-mod-plugin"
    token: ${HANGAR_TOKEN}
    overrides:
      loaders: ["paper"]

  curseforge:
    project_id: 123456
    token: ${CURSEFORGE_TOKEN}
    overrides:
      relations:  # CurseForge-specific
        - slug: "fabric-api"
          type: "requiredDependency"
```

## Proposed Implementation

### 1. Updated TerracottaProviderExtension

```kotlin
abstract class TerracottaProviderExtension(private val name: String) : Named {
    abstract val projectId: Property<String>
    abstract val token: Property<String>
    abstract val overrides: Property<TerracottaProviderOverrides>
}

@Serializable
data class TerracottaProviderOverrides(
    val loaders: List<TerracottaLoader>? = null,
    val gameVersions: List<String>? = null,
    val environment: TerracottaEnvironment? = null,
    val releaseType: TerracottaReleaseType? = null,
    val changelog: String? = null,
    // Platform-specific settings (JSON object)
    val platformSpecific: Map<String, Any>? = null,
)
```

### 2. Resolution Logic

```kotlin
object TerracottaConfigResolver {
    fun resolveProviderSettings(
        providerName: String,
        root: TerracottaProviderSettings?,       // Root-level canonical defaults
        providerOverride: TerracottaProviderSettings?
    ): TerracottaProviderSettings {
        val rootOrEmpty = root ?: TerracottaProviderSettings()
        val overrideOrEmpty = providerOverride ?: TerracottaProviderSettings()

        return TerracottaProviderSettings(
            loaders = overrideOrEmpty.loaders ?: rootOrEmpty.loaders,
            gameVersions = overrideOrEmpty.gameVersions ?: rootOrEmpty.gameVersions,
            environment = overrideOrEmpty.environment ?: rootOrEmpty.environment,
            releaseType = overrideOrEmpty.releaseType ?: rootOrEmpty.releaseType,
            changelog = overrideOrEmpty.changelog ?: rootOrEmpty.changelog,
            platformSpecific = overrideOrEmpty.platformSpecific ?: rootOrEmpty.platformSpecific,
        )
    }
}
```

### 3. Usage Example

```kotlin
val rootDefaults = TerracottaProviderSettings(
    loaders = listOf(TerracottaLoader.FABRIC, TerracottaLoader.PAPER),
    gameVersions = listOf("1.20.1", "1.20.2"),
    environment = TerracottaEnvironment.UNIVERSAL,
    releaseType = TerracottaReleaseType.RELEASE,
    changelog = "Standard release."
)

val modrinthOverrides = TerracottaProviderOverrides(
    loaders = listOf(TerracottaLoader.FABRIC),  // Override root loaders
    changelog = "Modrinth-specific notes"  // Override root changelog
)

val resolved = TerracottaConfigResolver.resolveProviderSettings(
    providerName = "modrinth",
    root = rootDefaults,
    providerOverride = modrinthOverrides
)

// resolved.loaders == [FABRIC]
// resolved.changelog == "Modrinth-specific notes"
```

### 4. Gradle DSL Integration

```kotlin
terracotta {
    // Root-level canonical defaults (optional)
    loaders.set(listOf(TerracottaLoader.FABRIC, TerracottaLoader.PAPER))
    gameVersions.set(listOf("1.20.1", "1.20.2"))
    environment.set(TerracottaEnvironment.UNIVERSAL)

    providers {
        create("modrinth") {
            projectId.set("AABBCCDD")
            token.set(System.getenv("MODRINTH_TOKEN"))

            // Provider-specific overrides
            overrides.set(
                TerracottaProviderOverrides(
                    loaders = listOf(TerracottaLoader.FABRIC),
                    changelog = "Modrinth-specific notes"
                )
            )
        }

        create("curseforge") {
            projectId.set("123456")
            token.set(System.getenv("CURSEFORGE_TOKEN"))

            overrides.set(
                TerracottaProviderOverrides(
                    platformSpecific = mapOf(
                        "relations" to listOf(
                            mapOf("slug" to "fabric-api", "type" to "requiredDependency")
                        )
                    )
                )
            )
        }
    }
}
```

## Platform-Specific Overrides

Some platforms need custom settings not in canonical schema:

### CurseForge Relations

```yaml
providers:
  curseforge:
    overrides:
      relations:
        - slug: "fabric-api"
          type: "requiredDependency"
```

### Modrinth Featured

```yaml
providers:
  modrinth:
    overrides:
      featured: true
```

### Hangar Channel

```yaml
providers:
  hangar:
    overrides:
      channel: "Snapshot"
```

## Benefits

1. **DRY**: Common settings defined once at the root level
2. **Flexibility**: Platform-specific overrides when needed
3. **Clarity**: Explicit separation of common vs. platform-specific
4. **Backward Compatible**: Optional root defaults and `overrides`

## Migration Path

1. ✅ Update `TerracottaProviderExtension` with `overrides` property
2. 🔄 Update Gradle DSL so root-level fields act as canonical defaults
3. 🔄 Implement resolution logic
4. 🔄 Update YAML parser to use root-level defaults and provider overrides
5. 🔄 Add tests for resolution precedence
6. 🔄 Document usage

## Alternatives Considered

### Alternative 1: Only Provider Overrides (No Root Defaults)
**Pros**: Simpler, no overlap  
**Cons**: Requires full duplication for each provider

### Alternative 2: Deep Merge (Recursively merge all fields)
**Pros**: Maximum flexibility  
**Cons**: Complex, hard to reason about, potential for conflicts

### Alternative 3: Single Configuration Per Provider
**Pros**: Simple, no merge complexity  
**Cons**: High duplication, error-prone

**Chosen**: Root-level defaults + provider overrides because it balances DRY principles with platform flexibility while remaining easy to understand.
