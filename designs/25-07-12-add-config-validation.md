# Proposal: Add Config Validation

**Date**: 2025-07-12  
**Status**: Draft  
**Author**: Terracotta Team

## Summary

Introduce a validation layer for Terracotta project configuration so that errors in `terracotta.yaml` / the Gradle plugin DSL are caught early, with clear, actionable messages. This reduces failed publishes, provider rejections, and silent misconfigurations.

## Problem Statement

Today Terracotta reads user configuration (YAML or Gradle DSL) and passes it more or less directly into provider mappings. This has several failure modes:

1. **Schema errors** go unnoticed until runtime — e.g., a missing required field, an unknown enum value, or a malformed URL.
2. **Provider-specific invariants** are only enforced by the remote API, leading to late, hard-to-read HTTP errors.
3. **Cross-field consistency** is not checked — e.g., a Modrinth-only field may be populated while no Modrinth provider is configured.
4. **Silent misconfiguration**: values that are technically valid but semantically wrong (e.g., `licenseUrl` without a license, or categories mapped to a disabled provider) are accepted.

This makes the tool harder to use and increases support burden.

## Goals

1. Fail fast at configuration-load time with clear, localized error messages.
2. Validate both the static schema and provider-specific semantic constraints.
3. Expose validation results through Gradle tasks and the CLI so CI can gate on them.
4. Avoid duplicating validation logic between YAML and DSL input paths.

## Proposed Changes

### 1. Define a Shared Validation Model

Create a provider-agnostic `TerracottaConfigValidator` in `terracotta-core` that operates on the canonical `TerracottaProject` model rather than raw YAML/DSL inputs.

```kotlin
interface ConfigValidator {
    fun validate(project: TerracottaProject): ValidationResult
}

data class ValidationResult(
    val errors: List<ValidationError>,
    val warnings: List<ValidationWarning>,
) {
    val isSuccess: Boolean get() = errors.isEmpty()
}

data class ValidationError(
    val path: String,          // e.g., "project.licenseUrl"
    val message: String,
    val suggestion: String? = null,
)
```

### 2. Validation Rules

#### Schema-level rules
- Required fields are present (`name`, `version`, `license`, `platforms`, etc.).
- Enum values are known (`TerracottaLicense`, provider IDs, platform types).
- URLs are well-formed (`homepage`, `source`, `licenseUrl`, issue tracker).
- Lists are non-empty where required (`platforms`, `categories`).

#### Provider-specific semantic rules
- If `licenseUrl` is set, `license` must be set.
- If a provider is enabled, all fields required by that provider are present.
- Disabled providers must not have exclusive fields populated (or warn if they are).
- `categories.primary.id` is in the provider-allowed set when a category mapping exists.
- `files` entries reference existing paths and use allowed upload modes.

#### Cross-field consistency rules
- `platforms` contains at least one entry.
- `version` matches a supported versioning scheme.
- `dependencies` do not reference the same project as both required and incompatible.
- Gallery and README assets exist on disk when referenced.

### 3. Integrate with YAML and DSL Loading

Both configuration entry points should normalize to `TerracottaProject` and then run the same validator:

```kotlin
// YAML path
val project = yamlLoader.load(file)
val result = ConfigValidator.validate(project)
if (!result.isSuccess) throw InvalidConfigurationException(result)

// Gradle DSL path
// The plugin's extension builder should produce a TerracottaProject
// and call the same validator before registering tasks.
```

### 4. Expose a `validate` Task

Add a Gradle task (and eventually CLI command) that runs validation without side effects:

```kotlin
abstract class ValidateTerracottaConfigTask : DefaultTask() {
    @TaskAction
    fun run() {
        val result = ConfigValidator.loadAndValidate(project.configFile)
        result.errors.forEach { logger.error("${it.path}: ${it.message}") }
        if (!result.isSuccess) throw GradleException("Terracotta configuration is invalid.")
        result.warnings.forEach { logger.warn("${it.path}: ${it.message}") }
    }
}
```

This task should run automatically as a dependency of publish/import tasks.

### 5. YAML Schema for IDE Support (Optional, Related)

Complementary work is proposed in [YAML Schema for IDEs](./2026-07-yaml-schema-for-ides.md). Validation logic and schema generation should share the same source of truth (the canonical model + rule definitions) so that IDE warnings and runtime errors stay in sync.

## Migration Path

1. Define `ValidationResult`, `ValidationError`, and the `ConfigValidator` interface in `terracotta-core`.
2. Implement schema-level validators against `TerracottaProject`.
3. Add provider-specific validators for Modrinth, CurseForge, and Hangar.
4. Wire `ConfigValidator` into the YAML loader and Gradle plugin extension.
5. Add the `validateTerracottaConfig` Gradle task and make publish tasks depend on it.
6. Add unit tests covering valid, invalid, and warning cases.
7. Document error messages and migration guide.

## Benefits

1. **Faster feedback loop**: Users catch mistakes before remote API calls fail.
2. **Clearer errors**: Terracotta can emit path-aware messages instead of provider error bodies.
3. **Consistency**: Same rules apply regardless of YAML or DSL input.
4. **Safer CI**: A dedicated validation task lets pipelines fail early and explicitly.
5. **Foundation for schema generation**: Shared rule definitions can drive IDE schemas.

## Risks & Considerations

1. **Overly strict validation**: New validators could break existing configurations that currently work.
   - **Mitigation**: Introduce warnings before errors; use a major version bump for breaking rules.

2. **Duplication with provider APIs**: Some rules may mirror provider-side validation.
   - **Mitigation**: Keep client-side checks focused on static/structural issues; defer provider-specific checks to providers where they are likely to change.

3. **Performance**: Large configurations or many files could slow validation.
   - **Mitigation**: Validation should be lazy and cacheable; asset existence checks can be skipped in dry-run mode.

## Next Steps

1. ✅ Define scope and draft proposal
2. 🔄 Design `ConfigValidator` API in `terracotta-core`
3. 🔄 Implement schema-level validation rules
4. 🔄 Implement provider-specific validation rules
5. 🔄 Wire validation into YAML loader and Gradle DSL
6. 🔄 Add `validateTerracottaConfig` Gradle task
7. 🔄 Add unit and integration tests
8. 🔄 Update documentation with error catalog

## References

- [YAML Schema for IDEs](./2026-07-yaml-schema-for-ides.md)
- [Narrow License](./2025-07-narrow-license.md)
- [Narrow Tags](./2025-07-narrow-tags.md)
- [Provider-Specific Logic Layer](./2025-07-provider-specific-logic.md)
