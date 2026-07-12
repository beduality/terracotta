# Proposal: Narrow Hangar License Integration

**Date**: 2026-07-12  
**Status**: Draft  
**Author**: Terracotta Team

## Summary

Fix Hangar license handling so that the canonical `license` field maps to Hangar's constrained license dropdown and `licenseUrl` does not cause a recurring diff. The current implementation forwards raw SPDX strings to Hangar and warns on every run when a URL is configured, which is both noisy and incorrect.

## Problem Statement

`TerracottaProject` stores a free-form `license` string plus an optional `licenseUrl`. The current Hangar integration has two concrete bugs:

1. **SPDX identifiers are not mapped to Hangar's license values.**
   - Hangar's project settings UI offers: `Unspecified`, `MIT`, `Apache 2.0`, `GPL`, `LGPL`, `AGPL`, `Other` (with a custom name).
   - The Hangar API accepts a single `license` string in `PATCH /projects/{slug}`.
   - Terracotta sends the canonical value verbatim, e.g. `Apache-2.0` instead of `Apache 2.0`. Values that do not match the UI will likely be treated as `Other` or rejected by validation.

2. **`licenseUrl` creates a perpetual diff loop.**
   - `DiffEngine` compares `local.licenseUrl != remote.licenseUrl`.
   - `HangarStateProvider` cannot read a URL from Hangar because the API does not expose one, so `remote.licenseUrl` is always `null`.
   - Every run with a configured `licenseUrl` therefore produces an `UpdateMetadata` operation, and `HangarRegistryProvider` logs a warning and discards it.

This results in redundant warnings and unnecessary metadata updates.

## Research Findings

### Hangar (REST API v1)
- **License endpoint**: `PATCH /projects/{slug}` accepts a JSON body with a `license` field.
- **Known UI values**: `Unspecified`, `MIT`, `Apache 2.0`, `GPL`, `LGPL`, `AGPL`, `Other`.
- **Custom licenses**: When `Other` is selected, the UI exposes a custom name field and a URL field. The exact API shape for the custom name is not yet documented; the safest assumption is that the `license` string itself is the displayed identifier.
- **License URL**: No dedicated `licenseUrl` field is exposed on the project API, so Terracotta cannot persist it.

### Modrinth (Labrinth API v2)
- `license_id` accepts an SPDX string and `license_url` is optional.
- This already works correctly in the Modrinth provider.

## Proposed Changes

### 1. Add SPDX-to-Hangar license mapping

Introduce a small mapper in the Hangar provider that normalizes common canonical values to the strings Hangar recognizes:

```kotlin
internal object HangarLicenseMapper {
    fun toHangarLicense(spdxId: String): String =
        when (spdxId.uppercase()) {
            "MIT" -> "MIT"
            "APACHE-2.0" -> "Apache 2.0"
            "GPL-3.0", "GPL-3.0-ONLY", "GPL-2.0", "GPL-2.0-ONLY" -> "GPL"
            "LGPL-3.0", "LGPL-3.0-ONLY", "LGPL-2.0", "LGPL-2.0-ONLY" -> "LGPL"
            "AGPL-3.0", "AGPL-3.0-ONLY" -> "AGPL"
            "UNLICENSE", "CC0-1.0" -> "Unspecified" // Hangar has no public-domain option
            else -> "Other" // Preserve custom identifiers as "Other"
        }
}
```

`HangarRegistryProvider` should call this mapper before sending `license` to `HangarClient.updateProject`. `HangarStateProvider` should reverse the mapping when reading state back into a `TerracottaProject`, falling back to the raw Hangar value if it is not recognized.

### 2. Stop `licenseUrl` from triggering a diff on Hangar

Two options are acceptable; the first is preferred because it is provider-agnostic:

- **Option A (preferred)**: Make the diff engine ignore `licenseUrl` when the remote provider does not support it. Add a `supportsLicenseUrl` capability to `StateProvider`/`RegistryProvider` or to provider metadata, and skip the comparison when the capability is false.
- **Option B**: Make `HangarStateProvider` populate `licenseUrl` with the same value as the local config (e.g. by reading it from the local project and including it in the returned state). This is fragile because the remote state is not actually authoritative.

If Option A is too invasive for a bugfix, Option C is acceptable as a short-term fix:
- **Option C**: In `HangarRegistryProvider`, silently drop `licenseUrl` changes without producing a warning, and in `HangarStateProvider` expose a `licenseUrl` equal to the local project's value if known. This removes the warning spam but still causes a diff every run.

The recommended approach is Option A, possibly implemented via a provider capability flag so that the core diff engine remains provider-agnostic.

### 3. Update `HangarProject` model if needed

If the Hangar API exposes a separate `licenseUrl` or custom-name field for `Other` licenses, add it to `HangarProject` and propagate it. Until confirmed, map unknown identifiers to `Other` and preserve the custom name in the `license` string.

## Provider-Specific Mapping

### Hangar Provider
- Map canonical SPDX/short values to Hangar's dropdown strings.
- Do not send `licenseUrl` to the Hangar API.
- Do not emit a warning when `licenseUrl` changes but the provider cannot persist it.
- Optionally warn once per project if `licenseUrl` is set, so users understand it is not stored on Hangar.

### Modrinth Provider
- No changes required; SPDX + `license_url` already work.

### CurseForge Provider (future)
- Out of scope for this bugfix; CurseForge has no project-level license field.

## Migration Path

1. Add `HangarLicenseMapper` and wire it into `HangarRegistryProvider` and `HangarStateProvider`.
2. Add a provider capability or metadata field indicating whether `licenseUrl` is supported.
3. Update `DiffEngine` to consult the capability before comparing `licenseUrl`.
4. Add unit tests for the mapping and for the diff behavior.
5. Update the Hangar provider tutorial and reference docs.

## Benefits

1. **Fewer warnings**: `licenseUrl` no longer produces a recurring warning on every run.
2. **Correct Hangar values**: Common SPDX identifiers map to the expected dropdown options.
3. **No unnecessary API calls**: Eliminates redundant `UpdateMetadata` operations caused by `licenseUrl`.
4. **Preserved flexibility**: Unknown identifiers fall back to `Other` instead of failing validation.

## Risks & Considerations

1. **Unknown Hangar API behavior for custom licenses**
   - **Risk**: Sending `Other` or a custom string may not match the API's expected shape.
   - **Mitigation**: Test against the live Hangar API with a test project, or read the Paper/Hangar source to confirm the accepted `license` values. If custom names require a separate field, update `HangarProject` and `HangarClient` accordingly.

2. **Provider capability changes core API surface**
   - **Risk**: Adding `supportsLicenseUrl` to providers is a small breaking change for the provider SPI.
   - **Mitigation**: Provide a default implementation that returns `true` to keep existing providers unchanged, or add the capability as a method on `RegistryProvider` with a default body.

3. **Case-insensitive matching hides typos**
   - **Risk**: `toHangarLicense` is case-insensitive, which could mask invalid identifiers.
   - **Mitigation**: Only perform the mapping at the provider boundary; keep the canonical model strict and validate SPDX identifiers at configuration time (future work, out of scope here).

## Testing Strategy

- **Unit tests in `HangarProviderTest`**:
  - `license: "MIT"` is sent as `"MIT"` and read back as `"MIT"`.
  - `license: "Apache-2.0"` is sent as `"Apache 2.0"` and read back as `"Apache-2.0"`.
  - `license: "GPL-3.0-only"` is sent as `"GPL"` and read back as `"GPL-3.0-only"`.
  - `license: "BSD-3-Clause"` is sent as `"Other"` and read back as `"BSD-3-Clause"`.
- **Diff engine tests in `DiffEngineTest`**:
  - With a provider that does not support `licenseUrl`, a local `licenseUrl` should not produce an `UpdateMetadata` operation.
- **Integration tests in `TerracottaPluginIntegrationTest`**:
  - A project with `license: MIT` and `licenseUrl` configured should produce no metadata diff for Hangar when name/summary/license are unchanged.

## Documentation Updates

- Update `docs/content/modules/provider-hangar/tutorials/using-hangar.md` to explain:
  - Supported license values and how they map to Hangar's dropdown.
  - That `licenseUrl` is not stored on Hangar.
- Update `docs/content/modules/core/reference/config-schema.md` to clarify that `licenseUrl` is ignored by Hangar (already partially noted, but can be strengthened).
- Update `docs/content/modules/core/reference/operations.md` to note that `licenseUrl` changes are provider-dependent.

## Next Steps

1. 🔄 Confirm the exact Hangar API values for custom `Other` licenses.
2. 🔄 Implement `HangarLicenseMapper` and wire it into the provider.
3. 🔄 Add provider capability for `licenseUrl` and update `DiffEngine`.
4. 🔄 Add unit and integration tests.
5. 🔄 Update provider documentation.

## References

- [SPDX License List](https://spdx.org/licenses/)
- [Hangar API](https://hangar.papermc.io/api-docs)
- [Modrinth API Projects](https://docs.modrinth.com/api/projects)
- `modules/terracotta-provider-hangar/src/main/kotlin/io/github/beduality/terracotta/provider/hangar/HangarRegistryProvider.kt`
- `modules/terracotta-provider-hangar/src/main/kotlin/io/github/beduality/terracotta/provider/hangar/HangarStateProvider.kt`
- `modules/terracotta-core/src/main/kotlin/io/github/beduality/terracotta/core/diff/DiffEngine.kt`
