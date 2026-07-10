# Proposal: Narrow Canonical License and Tags Types

**Date**: 2025-07-10  
**Status**: Draft  
**Author**: Terracotta Team

## Summary

Narrow the canonical `TerracottaLicense` and `tags` types in `TerracottaProject` to align with the actual capabilities of all supported registries (Modrinth, CurseForge, Hangar). The current free-form string types don't reflect platform constraints and will cause mapping failures.

## Problem Statement

Currently, `TerracottaProject` has:

```kotlin
data class TerracottaProject(
    val tags: List<String>,
    val license: String,
    // ...
)
```

This is **too permissive** and doesn't match what all platforms can represent:

| Aspect | Modrinth | CurseForge | Hangar | Lowest Common Denominator |
|--------|----------|------------|--------|--------------------------|
| License | SPDX ID + optional URL | No project license | Not documented | SPDX ID only (no URL) |
| Tags | Free-form categories | Hierarchical categories | No tags system | Categories (structured) |

## Research Findings

### Modrinth (Labrinth API v2)
- **License**: Accepts `license_id` (SPDX string) and `license_url` (optional)
- **Tags**: Free-form category strings (no strict allowed list)
- Source: [Modrinth API Documentation](https://docs.modrinth.com/api)

### CurseForge (Upload API v1)
- **License**: No project-level license field (only file metadata)
- **Tags**: Uses hierarchical **categories** system with 3 levels:
  - Class → Category → Subcategory
  - Example: `["mods", "mobs", "animals"]`
- Source: [CurseForge REST API](https://docs.curseforge.com/rest-api)

### Hangar (REST API v1)
- **License**: Not explicitly documented
- **Tags**: No tag system - uses channels/categories only
- Projects are organized by platform groups (PAPER, VELOCITY, WATERFALL)

## Proposed Changes

### 1. Create Narrowed TerracottaLicense

```kotlin
@Serializable
enum class TerracottaLicense(val id: String) {
    @SerialName("MIT")
    MIT("MIT"),
    
    @SerialName("Apache-2.0")
    APACHE_2_0("Apache-2.0"),
    
    @SerialName("GPL-3.0-only")
    GPL_3_0_ONLY("GPL-3.0-only"),
    
    @SerialName("LGPL-3.0-only")
    LGPL_3_0_ONLY("LGPL-3.0-only"),
    
    @SerialName("BSD-3-Clause")
    BSD_3_CLAUSE("BSD-3-Clause"),
    
    @SerialName("BSD-2-Clause")
    BSD_2_CLAUSE("BSD-2-Clause"),
    
    @SerialName("Unlicense")
    UNLICENSE("Unlicense"),
    
    @SerialName("CC0-1.0")
    CC0_1_0("CC0-1.0"),
    
    @SerialName("MPL-2.0")
    MPL_2_0("MPL-2.0"),
    
    @SerialName("EPL-2.0")
    EPL_2_0("EPL-2.0"),
}
```

**Add to `TerracottaProject`**:
```kotlin
val license: TerracottaLicense,
val licenseUrl: String? = null,  // Optional for Modrinth
```

### 2. Replace Free-Form Tags with Structured Categories

**Current**:
```kotlin
val tags: List<String>,
```

**Proposed**:
```kotlin
@Serializable
data class TerracottaCategory(
    val id: String,
    val displayName: String,
)

@Serializable
data class TerracottaProjectCategories(
    val primary: TerracottaCategory,
    val additional: List<TerracottaCategory> = emptyList(),
)
```

**Add to `TerracottaProject`**:
```kotlin
val categories: TerracottaProjectCategories,
```

## Provider-Specific Mapping

### Modrinth Provider
- License: Direct mapping (`license.id`)
- Tags: Map `categories.primary.id` + `categories.additional.map { it.id }`

### CurseForge Provider
- License: No mapping (not supported at project level)
- Tags: Map to hierarchical category system (class → category → subcategory)

### Hangar Provider
- License: No mapping (not explicitly supported)
- Tags: Map to platform categories (PAPER, VELOCITY, WATERFALL, etc.)

## Migration Path

1. Add new `TerracottaLicense` enum
2. Change `license: String` → `license: TerracottaLicense`
3. Add `licenseUrl: String?` to `TerracottaProject`
4. Create `TerracottaCategory` and `TerracottaProjectCategories`
5. Change `tags: List<String>` → `categories: TerracottaProjectCategories`
6. Update Gradle plugin DSL to accept narrowed types
7. Implement provider-specific mappings

## Benefits

1. **Type Safety**: Catch invalid licenses/tags at compile time
2. **Cross-Platform Compatibility**: Canonical types match all platforms
3. **Clear Documentation**: Explicit list of supported licenses/categories
4. **Easier Implementation**: Providers know exactly what to map

## Risks & Considerations

1. **Loss of Flexibility**: Users can't specify custom license identifiers
   - **Mitigation**: Provide clear error messages with supported SPDX IDs
   
2. **Category Structure**: CurseForge's 3-level hierarchy may be overkill
   - **Mitigation**: Start simple (primary + additional), extend if needed

3. **Backward Compatibility**: Breaking change for existing configurations
   - **Mitigation**: Major version bump (v0.2.0), provide migration guide

## Next Steps

1. ✅ Complete research and documentation
2. 🔄 Implement narrowed types
3. 🔄 Update Gradle plugin DSL
4. 🔄 Implement provider mappings
5. 🔄 Add comprehensive tests
6. 🔄 Update documentation

## References

- [SPDX License List](https://spdx.org/licenses/)
- [Modrinth API Projects](https://docs.modrinth.com/api/projects)
- [CurseForge REST API](https://docs.curseforge.com/rest-api)
- [Hangar API](https://hangar.papermc.io)
