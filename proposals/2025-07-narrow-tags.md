# Proposal: Narrow Canonical Tags/Categories Type

**Date**: 2025-07-10  
**Status**: Draft  
**Author**: Terracotta Team

## Summary

Replace the free-form `tags: List<String>` field in `TerracottaProject` with a structured category model that aligns with the actual capabilities of all supported registries (Modrinth, CurseForge, Hangar). The current string list doesn't reflect platform constraints and will cause mapping failures.

## Problem Statement

Currently, `TerracottaProject` has:

```kotlin
data class TerracottaProject(
    val tags: List<String>,
    // ...
)
```

This is **too permissive** and doesn't match what all platforms can represent:

| Platform | Tag/Category Support | Lowest Common Denominator |
|----------|---------------------|--------------------------|
| Modrinth | Free-form category strings | Category strings (structured) |
| CurseForge | Hierarchical categories (3 levels) | Categories (structured) |
| Hangar | No tag system - channels/categories only | Platform categories |

## Research Findings

### Modrinth (Labrinth API v2)
- **Tags**: Free-form category strings (no strict allowed list)
- Source: [Modrinth API Documentation](https://docs.modrinth.com/api)

### CurseForge (Upload API v1)
- **Tags**: Uses hierarchical **categories** system with 3 levels:
  - Class → Category → Subcategory
  - Example: `["mods", "mobs", "animals"]`
- Source: [CurseForge REST API](https://docs.curseforge.com/rest-api)

### Hangar (REST API v1)
- **Tags**: No tag system - uses channels/categories only
- Projects are organized by platform groups (PAPER, VELOCITY, WATERFALL)
- Source: [Hangar API](https://hangar.papermc.io)

## Proposed Changes

### Replace Free-Form Tags with Structured Categories

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
- Tags: Map `categories.primary.id` + `categories.additional.map { it.id }`

### CurseForge Provider
- Tags: Map to hierarchical category system (class → category → subcategory)

### Hangar Provider
- Tags: Map to platform categories (PAPER, VELOCITY, WATERFALL, etc.)

## Migration Path

1. Create `TerracottaCategory` and `TerracottaProjectCategories`
2. Change `tags: List<String>` → `categories: TerracottaProjectCategories`
3. Update Gradle plugin DSL to accept narrowed category types
4. Implement provider-specific category mappings

## Benefits

1. **Type Safety**: Catch invalid categories at compile time
2. **Cross-Platform Compatibility**: Canonical types match all platforms
3. **Clear Documentation**: Explicit category structure
4. **Easier Implementation**: Providers know exactly what to map

## Risks & Considerations

1. **Category Structure**: CurseForge's 3-level hierarchy may be overkill
   - **Mitigation**: Start simple (primary + additional), extend if needed

2. **Backward Compatibility**: Breaking change for existing configurations
   - **Mitigation**: Major version bump (v0.2.0), provide migration guide

## Next Steps

1. ✅ Complete research and documentation
2. 🔄 Create `TerracottaCategory` and `TerracottaProjectCategories`
3. 🔄 Update `TerracottaProject` tags field
4. 🔄 Update Gradle plugin DSL
5. 🔄 Implement provider category mappings
6. 🔄 Add comprehensive tests
7. 🔄 Update documentation

## References

- [Modrinth API Projects](https://docs.modrinth.com/api/projects)
- [CurseForge REST API](https://docs.curseforge.com/rest-api)
- [Hangar API](https://hangar.papermc.io)
