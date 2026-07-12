# Proposal: Narrow Canonical License Type

**Date**: 2025-07-10  
**Status**: Draft  
**Author**: Terracotta Team

## Summary

Narrow the canonical `TerracottaLicense` type in `TerracottaProject` to align with the actual capabilities of all supported registries (Modrinth, CurseForge, Hangar). The current free-form `String` type doesn't reflect platform constraints and will cause mapping failures.

## Problem Statement

Currently, `TerracottaProject` has:

```kotlin
data class TerracottaProject(
    val license: String,
    // ...
)
```

This is **too permissive** and doesn't match what all platforms can represent:

| Platform | License Support | Lowest Common Denominator |
|----------|----------------|--------------------------|
| Modrinth | SPDX ID + optional URL | SPDX ID only (no URL) |
| CurseForge | No project-level license field | Not supported |
| Hangar | Not explicitly documented | Not supported |

## Research Findings

### Modrinth (Labrinth API v2)
- **License**: Accepts `license_id` (SPDX string) and `license_url` (optional)
- Source: [Modrinth API Documentation](https://docs.modrinth.com/api)

### CurseForge (Upload API v1)
- **License**: No project-level license field (only file metadata)
- Source: [CurseForge REST API](https://docs.curseforge.com/rest-api)

### Hangar (REST API v1)
- **License**: Not explicitly documented
- Source: [Hangar API](https://hangar.papermc.io)

## Proposed Changes

### Create Narrowed TerracottaLicense

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

## Provider-Specific Mapping

### Modrinth Provider
- License: Direct mapping (`license.id`)
- License URL: Map `licenseUrl` when present

### CurseForge Provider
- License: No mapping (not supported at project level)

### Hangar Provider
- License: No mapping (not explicitly supported)

## Migration Path

1. Add new `TerracottaLicense` enum
2. Change `license: String` → `license: TerracottaLicense`
3. Add `licenseUrl: String?` to `TerracottaProject`
4. Update Gradle plugin DSL to accept narrowed license type
5. Implement provider-specific license mappings

## Benefits

1. **Type Safety**: Catch invalid licenses at compile time
2. **Cross-Platform Compatibility**: Canonical type matches all platforms
3. **Clear Documentation**: Explicit list of supported SPDX IDs
4. **Easier Implementation**: Providers know exactly what to map

## Risks & Considerations

1. **Loss of Flexibility**: Users can't specify custom license identifiers
   - **Mitigation**: Provide clear error messages with supported SPDX IDs

2. **Backward Compatibility**: Breaking change for existing configurations
   - **Mitigation**: Major version bump (v0.2.0), provide migration guide

## Next Steps

1. ✅ Complete research and documentation
2. 🔄 Implement `TerracottaLicense` enum
3. 🔄 Update `TerracottaProject` license field
4. 🔄 Update Gradle plugin DSL
5. 🔄 Implement provider license mappings
6. 🔄 Add comprehensive tests
7. 🔄 Update documentation

## References

- [SPDX License List](https://spdx.org/licenses/)
- [Modrinth API Projects](https://docs.modrinth.com/api/projects)
- [CurseForge REST API](https://docs.curseforge.com/rest-api)
- [Hangar API](https://hangar.papermc.io)
