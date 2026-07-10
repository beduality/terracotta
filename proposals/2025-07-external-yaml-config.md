# Proposal: External YAML Configuration

**Date**: 2025-07-10  
**Status**: Draft  
**Related**: #1 Narrow License and Tags

## Summary

Add support for an external `terracotta.yaml` configuration file that provides a unified, platform-agnostic specification with canonical defaults and provider-specific overrides. This complements the existing Gradle DSL by offering a more portable, version-controlled configuration format.

## Problem Statement

Currently, Terracotta only supports configuration via Gradle DSL:

```kotlin
terracotta {
    providers {
        create("modrinth") { /* ... */ }
    }
    name = "My Mod"
    // ...
}
```

This has limitations:

1. **No external file support**: Configuration is tied to build scripts
2. **No canonical layer**: Can't define defaults that apply to all providers
3. **No provider overrides**: Each provider block duplicates common settings
4. **Build tool lock-in**: Harder to use Terracotta as a library or in non-Gradle workflows

## Use Cases

### Scenario 1: Multi-Provider with Shared Defaults

```yaml
# terracotta.yaml
# Canonical defaults applied to ALL providers
canonical:
  loaders: ["fabric", "paper"]
  game_versions: ["1.20.1", "1.20.2"]
  environment: "universal"
  release_type: "release"
  changelog: "Standard platform-agnostic release."

# Target platform identifiers & specific overrides
providers:
  modrinth:
    project_id: "AABBCCDD"
    featured: true
    overrides:
      changelog: "Modrinth-specific changelog notes."

  hangar:
    project_id: "example-mod-plugin"
    channel: "Snapshot"
    overrides:
      loaders: ["paper"]

  curseforge:
    project_id: 123456
    overrides:
      relations:
        - slug: "fabric-api"
          type: "required"
```

### Scenario 2: External Configuration for CI/CD

```yaml
# terracotta.yaml (read from repo root)
# Environment variables only for sensitive data
token: ${MODRINTH_TOKEN}  # resolved at runtime from env
```

```bash
# Can be run from any environment
terracotta plan --config terracotta.yaml
terracotta apply --config terracotta.yaml
```

### Scenario 3: Library Usage

```kotlin
val config = TerracottaConfig.load("terracotta.yaml")
val project = TerracottaProject.from(config)
val operations = DiffEngine.diff(project, remoteState)
TerracottaProvider.from(config).apply(operations)
```

## Proposed Schema

### YAML Configuration Structure

```yaml
schemaVersion: 1

# Canonical defaults (applied to all providers unless overridden)
canonical:
  id: "example-mod"
  name: "Example Mod"
  summary: "A unified platform-agnostic mod description."
  description: "file://README.md"  # Can reference external files
  tags: ["utility", "paper"]
  license: "MIT"
  loaders: ["fabric", "paper"]
  game_versions: ["1.20.1", "1.20.2"]
  environment: "universal"
  release_type: "release"
  changelog: "Standard release."

# Provider-specific targets
providers:
  modrinth:
    project_id: "AABBCCDD"
    token: ${MODRINTH_TOKEN}  # Environment variable reference
    featured: true
    overrides:
      loaders: ["fabric"]  # Override specific to Modrinth
      changelog: "Modrinth release notes"

  hangar:
    project_id: "example-mod-plugin"
    channel: "Snapshot"
    token: ${HANGAR_TOKEN}
    overrides:
      loaders: ["paper"]

  curseforge:
    project_id: 123456  # CurseForge uses numeric IDs
    token: ${CURSEFORGE_TOKEN}
    overrides:
      relations:  # CurseForge-specific dependency format
        - slug: "fabric-api"
          type: "requiredDependency"
```

### Configuration Resolution Hierarchy

```
┌─────────────────────────────────────────────────────────────┐
│                        Resolved Value                        │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              Provider Override (if defined)                 │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              Canonical Default (if defined)                 │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              Project-Level (global) settings                │
└─────────────────────────────────────────────────────────────┘
```

## Implementation Plan

### 1. YAML Configuration Model

```kotlin
@Serializable
data class TerracottaConfig(
    val schemaVersion: Int = 1,
    val project: TerracottaProject,
    val canonical: TerracottaProviderSettings? = null,
    val providers: Map<String, TerracottaProviderConfig> = emptyMap(),
)

@Serializable
data class TerracottaProviderConfig(
    val projectId: String,
    val token: String? = null,  // Can reference env vars
    val overrides: TerracottaProviderSettings? = null,
)

@Serializable
data class TerracottaProviderSettings(
    val loaders: List<TerracottaLoader>? = null,
    val gameVersions: List<String>? = null,
    val environment: TerracottaEnvironment? = null,
    val releaseType: TerracottaReleaseType? = null,
    val changelog: String? = null,
    // Platform-specific settings...
)
```

### 2. Configuration Loading

```kotlin
object TerracottaConfigLoader {
    fun load(path: String): TerracottaConfig
    fun load(yaml: String): TerracottaConfig
    fun loadFromGradle(): TerracottaConfig  // From build.gradle.kts
}
```

### 3. Resolution Logic

```kotlin
object TerracottaConfigResolver {
    fun resolveProviderSettings(
        providerName: String,
        config: TerracottaConfig
    ): TerracottaProviderSettings {
        val projectSettings = config.project.settings
        val canonical = config.canonical ?: TerracottaProviderSettings()
        
        val providerConfig = config.providers[providerName]
        val providerOverride = providerConfig?.overrides ?: TerracottaProviderSettings()
        
        return TerracottaProviderSettings(
            loaders = providerOverride.loaders
                ?: canonical.loaders
                ?: projectSettings.loaders,
            // ... resolve other fields similarly
        )
    }
}
```

### 4. Gradle Plugin Integration

```kotlin
abstract class TerracottaExtension {
    // Keep Gradle DSL for flexibility
    abstract val configPath: Property<String>  // Optional
    abstract val project: ObjectContainer
    abstract val canonical: ObjectContainer
    abstract val providers: NamedDomainObjectContainer<TerracottaProviderExtension>
}
```

## Benefits

1. **Portability**: Same config works across Gradle, library usage, CLI tools
2. **DRY**: Canonical defaults eliminate duplication
3. **Version Control**: External YAML is easier to diff and review
4. **Environment Separation**: Token resolution from environment variables
5. **Tool Agnostic**: CLI, library, or Gradle can all use the same config

## Risks & Considerations

1. **Duplication**: Two configuration entry points (Gradle + YAML)
   - **Mitigation**: Gradle extension can reference YAML file; YAML can reference Gradle

2. **Complexity**: Nested configuration hierarchy may be confusing
   - **Mitigation**: Clear documentation, sensible defaults, validator errors

3. **Loading Priority**: Need clear resolution order
   - **Mitigation**: Document precedence: provider overrides > canonical > project

## Next Steps

1. ✅ Define YAML schema
2. 🔄 Implement configuration model
3. 🔄 Add configuration loader
4. 🔄 Implement resolution logic
5. 🔄 Integrate with Gradle plugin
6. 🔄 Add comprehensive tests

## Alternatives Considered

### Alternative 1: Only Gradle DSL
**Pros**: Simpler, Gradle users are familiar with this approach  
**Cons**: Locks users into Gradle, no external configuration, harder to test

### Alternative 2: JSON instead of YAML
**Pros**: Easier to parse, no YAML parser dependency  
**Cons**: Less readable, harder to write by hand

### Alternative 3: Multiple config files per provider
**Pros**: Maximum separation  
**Cons**: More files to manage, harder to find canonical defaults

**Chosen**: Single YAML file with canonical + provider overrides because it strikes the best balance between clarity and flexibility.
