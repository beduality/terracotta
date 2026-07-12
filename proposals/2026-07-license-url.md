# Proposal: License URL Support

**Date**: 2026-07-12  
**Status**: Draft  
**Related**: [Narrow License and Tags](./2025-07-narrow-license-tags.md), [Provider-Specific Logic Layer](./2025-07-provider-specific-logic.md)

## Summary

Add an optional `licenseUrl` field to the canonical project metadata, `terracotta.yml`, and the Gradle DSL. The URL will be forwarded to platforms that accept it (currently Modrinth) and ignored by platforms that do not (currently Hangar). This lets users publish custom licenses, additional terms, or a direct link to the full license text without changing the existing SPDX-based `license` field.

## Problem Statement

Right now Terracotta only supports an SPDX-style `license` identifier, for example:

```yaml
license: "MIT"
```

Modrinth's API supports both a license identifier **and** an optional URL:

- `license_id` — the SPDX identifier.
- `license_url` — an optional URL to the license text (max 2048 characters).

This is useful for:

- Custom or non-SPDX licenses.
- All-rights-reserved projects that still want to link to terms.
- Projects that want to point to a specific rendering of the license text.

Without `licenseUrl`, Terracotta cannot represent this metadata and users must set it manually in the Modrinth web UI after each metadata sync.

## Research Findings

### Modrinth

- Create/patch project request accepts `license_id` (string) and `license_url` (nullable string).
- Project response includes `license.id` and `license.url`.
- Source: [Modrinth API docs](https://docs.modrinth.com/api/)

### Hangar

- Project creation accepts only a `license` string; no URL field is documented or used in the current provider implementation.
- The URL can be stored in the canonical model but will not be sent to Hangar.

### CurseForge

- No project-level license field exists in the CurseForge project API, so this change has no effect there.

## Proposed Changes

### 1. Canonical model

Add `licenseUrl: String? = null` to the canonical project model and all metadata layers:

- `io.github.beduality.terracotta.core.model.TerracottaProject`
- `io.github.beduality.terracotta.core.model.metadata.ProjectMetadata`
- `io.github.beduality.terracotta.core.model.metadata.AbstractProjectMetadata`
- `io.github.beduality.terracotta.core.model.metadata.TerracottaProjectMetadata`
- `io.github.beduality.terracotta.core.config.ResolvedProjectMetadata`

Update `merge()` and `equals()/hashCode()` in `AbstractProjectMetadata` so the URL participates in metadata resolution and diffing.

### 2. `terracotta.yml` schema

Add a new top-level optional string:

```yaml
license: "MIT"
licenseUrl: "https://github.com/example/my-plugin/blob/main/LICENSE"
```

Update `TerracottaConfig` and `TerracottaConfigLoader` to parse the field. Because the `LICENSE` file detector only yields an SPDX identifier, `licenseUrl` is configuration-only and does not have auto-detection support.

### 3. Gradle DSL

Add to `TerracottaExtension`:

```kotlin
abstract class TerracottaExtension {
    /** Optional URL to the full license text. */
    abstract val licenseUrl: Property<String>
}
```

Values set via the DSL override `terracotta.yml`, consistent with other extension properties.

### 4. Modrinth provider

- Add `url: String? = null` to `ModrinthLicense`.
- Update `ModrinthStateProvider.fetchProject()` to map `project.license.url` into the canonical `licenseUrl`.
- Update `ModrinthClient.createProject()` to emit `license_url` when `licenseUrl` is non-null.
- Update `Operation.UpdateMetadata` and `DiffEngine` to track `licenseUrlChanged` and a `newLicenseUrl` value.
- Update `ModrinthRegistryProvider` so a metadata patch includes `license_url` when the URL changed.

Example create-project payload change:

```json
{
  "slug": "my-plugin",
  "title": "My Plugin",
  "license_id": "MIT",
  "license_url": "https://example.com/LICENSE",
  ...
}
```

When `licenseUrl` is null the field is omitted entirely, preserving existing behavior.

### 5. Hangar provider

Hangar's project API only accepts a `license` string, so `licenseUrl` cannot be forwarded. To avoid silent data loss, the Hangar registry provider should warn when `licenseUrl` is non-null:

```kotlin
if (!project.licenseUrl.isNullOrBlank()) {
    logger.warn("Hangar does not support licenseUrl; the configured URL will not be published.")
}
```

No other API mapping change is required.

## Migration Path

1. Add `licenseUrl` to the canonical model and metadata interfaces.
2. Add `licenseUrl` to `TerracottaConfig` and the YAML loader.
3. Add `licenseUrl` to the Gradle extension and plugin wiring.
4. Update the Modrinth model, state provider, diff engine, and registry provider.
5. Update `docs/content/modules/core/reference/config-schema.md` and the generated KDoc references.
6. Add unit and integration tests covering:
   - YAML parsing and DSL wiring.
   - Metadata resolution (configuration overrides detection).
   - Diff engine emits an update when only the URL changes.
   - Modrinth create/patch payload includes `license_url` when set.
   - Modrinth fetch deserializes `license.url`.

## Backward Compatibility

- `licenseUrl` is optional and defaults to `null`. Existing `terracotta.yml` files and Gradle builds continue to work unchanged.
- Providers that do not support a license URL simply ignore the field.
- Modrinth projects created before this change keep their existing URL because a missing local `licenseUrl` does not patch the remote value.

## Risks & Considerations

1. **URL validation** — Modrinth enforces a 2048-character limit. Consider lightweight validation (non-empty, valid URL syntax, length) at config load time or before sending.
2. **Case-insensitive license diff** — The current diff compares `license.uppercase()`. `licenseUrl` should be compared exactly (URLs are case-sensitive in the path).
3. **Provider override** — The [Provider-Specific Logic Layer](./2025-07-provider-specific-logic.md) may eventually allow per-provider overrides. Until then, `licenseUrl` is a single canonical value.

## Next Steps

1. Implement canonical model changes.
2. Implement YAML and Gradle DSL support.
3. Implement Modrinth mapping and diff engine changes.
4. Add tests.
5. Update documentation.

## References

- [Modrinth API Documentation](https://docs.modrinth.com/api/)
- [Modrinth OpenAPI `license_url` field](https://raw.githubusercontent.com/modrinth/code/refs/heads/main/apps/docs/public/openapi.yaml)
- [Hangar API](https://hangar.papermc.io/api-docs)
