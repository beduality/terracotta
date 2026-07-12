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
| Modrinth | Controlled category list + up to 3 featured tags | Category strings (structured) |
| CurseForge | Category graph with numeric IDs | Categories (structured) |
| Hangar | Single category + optional tags | Single category + tags |

## Research Findings

### Modrinth (Labrinth API v3/v2)
- **Categories**: Controlled vocabulary chosen from checkboxes in the project settings UI
  - Users can select any number of applicable categories from the allowed list
  - Up to 3 selected categories can be marked as **featured tags**
  - Remaining selected categories are still stored as additional tags
- Source: [Modrinth API - Get a list of categories](https://docs.modrinth.com/api/operations/categorylist/)

### CurseForge (Core API v1)
- **Categories**: Category graph with numeric IDs
  - Each category has `id`, `classId` (top-level class), `parentCategoryId`, and `isClass`
  - Projects expose `classId`, `primaryCategoryId`, and a `categories` array
  - The "Class → Category → Subcategory" string-path model is inaccurate; mappings must use numeric IDs
- Source: [CurseForge REST API](https://docs.curseforge.com/rest-api)

### Hangar (REST API v1/v2)
- **Platform**: The runtime platform is derived from loaders/artifacts (`PAPER`, `VELOCITY`, `WATERFALL`, `FOLIA`). It is separate from categorization.
- **Category**: A single project category chosen from a controlled list (e.g., `gameplay`). The UI describes this as "one of 10 categories".
- **Tags**: Optional flags including `addon`, `library`, and `folia` compatible.
- Source: [Hangar API Docs](https://hangar.papermc.io/api-docs)

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
- Map `categories.primary.id` plus up to 2 of the first `additional` IDs to the featured `categories` array (max 3)
- Map the remaining selected categories to `additional_categories`
- Validate all IDs against the platform category list

### CurseForge Provider (design only for this phase)
- Map canonical category IDs to CurseForge numeric category IDs via provider-specific mapping
- Set `primaryCategoryId` from `categories.primary`
- Populate `classId` and the `categories` array
- **Note**: CurseForge mapping is out of scope for the initial execution of this proposal; it is included in the design so the canonical model remains extensible.

### Hangar Provider
- Map `categories.primary.id` to Hangar's single project category (e.g., `gameplay`)
- Map recognized `additional` category IDs to Hangar tags (`addon`, `library`, `folia`)
- Derive the runtime platform (`PAPER`, `VELOCITY`, `WATERFALL`, `FOLIA`) from `loaders`, not from categories

## Migration Path

1. Create `TerracottaCategory` and `TerracottaProjectCategories`
2. Change `tags: List<String>` → `categories: TerracottaProjectCategories`
3. Update Gradle plugin DSL to accept narrowed category types
4. Implement provider-specific category mappings for Modrinth and Hangar
5. Keep CurseForge mapping in the canonical design but defer implementation to a later phase

## Benefits

1. **Type Safety**: Catch invalid categories at compile time
2. **Cross-Platform Compatibility**: Canonical types match all platforms
3. **Clear Documentation**: Explicit category structure
4. **Easier Implementation**: Providers know exactly what to map

## Risks & Considerations

1. **Category Structure**: CurseForge's numeric category graph, Modrinth's controlled vocabulary, and Hangar's single category + tags require provider-specific mappings
   - **Mitigation**: Keep the canonical model simple (primary + additional); let each provider resolve IDs to its native representation. Hangar's runtime platform stays derived from `loaders`.

2. **Backward Compatibility**: Breaking change for existing configurations
   - **Mitigation**: Major version bump (v0.2.0), provide migration guide

## Next Steps

1. ✅ Complete research and documentation
2. 🔄 Create `TerracottaCategory` and `TerracottaProjectCategories`
3. 🔄 Update `TerracottaProject` tags field
4. 🔄 Update Gradle plugin DSL
5. 🔄 Implement provider category mappings for Modrinth and Hangar
6. 🔄 Defer CurseForge mapping implementation to a later phase
7. 🔄 Add comprehensive tests
8. 🔄 Update documentation

## References

- [Modrinth API - Categories](https://docs.modrinth.com/api/operations/categorylist/)
- [Modrinth API - Projects](https://docs.modrinth.com/api/projects)
- [CurseForge REST API](https://docs.curseforge.com/rest-api)
- [Hangar API Docs](https://hangar.papermc.io/api-docs)
