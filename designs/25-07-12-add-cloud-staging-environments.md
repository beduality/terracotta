# Proposal: Add Support for Staging Environments

**Date**: 2025-07-12  
**Status**: Draft  
**Author**: Terracotta Team

## Summary

Introduce first-class support for staging environments so users can publish, validate, and test against non-production provider instances — starting with Modrinth Staging. The abstraction should live in `terracotta-core` so each provider can expose its staging counterpart consistently without leaking environment-specific details into task logic.

## Problem Statement

Currently Terracotta targets only production provider APIs. This makes it risky to test publishes or validate configurations:

- A misconfigured project can create a real, public release on Modrinth/CurseForge/Hangar.
- CI pipelines that exercise the full publish flow must either skip it or accept real side effects.
- There is no way to point a provider at a staging URL without ad-hoc overrides.

Modrinth already provides a public staging API (`https://staging-api.modrinth.com`). Other providers may offer sandboxes or staging endpoints in the future. Terracotta should support these cleanly.

## Goals

1. Allow users to mark a provider as targeting a staging environment.
2. Keep staging logic provider-agnostic in `terracotta-core` while letting each provider declare its staging endpoint and constraints.
3. Make staging opt-in and explicit so it cannot accidentally replace a production publish.
4. Support staging-aware validation, dry-run, and publish tasks.

## Proposed Changes

### 1. Core Abstraction: `TerracottaEnvironment`

Add an environment concept to `terracotta-core` that providers can implement.

```kotlin
interface TerracottaEnvironment {
    val name: String
    val isProduction: Boolean
}

enum class BuiltInEnvironment(override val isProduction: Boolean) : TerracottaEnvironment {
    PRODUCTION(isProduction = true),
    STAGING(isProduction = false),
}
```

Each provider declares the environments it supports and the endpoint/behavior for each.

```kotlin
interface ProviderEnvironmentSupport {
    fun endpoint(environment: TerracottaEnvironment): String
    fun isSupported(environment: TerracottaEnvironment): Boolean
}
```

### 2. Provider Declarations

Each provider module declares its environments:

```kotlin
// terracotta-provider-modrinth
object ModrinthEnvironments : ProviderEnvironmentSupport {
    override fun endpoint(environment: TerracottaEnvironment): String = when (environment) {
        BuiltInEnvironment.PRODUCTION -> "https://api.modrinth.com/v2"
        BuiltInEnvironment.STAGING -> "https://staging-api.modrinth.com/v2"
        else -> throw UnsupportedEnvironmentException("Modrinth", environment)
    }

    override fun isSupported(environment: TerracottaEnvironment): Boolean =
        environment in setOf(BuiltInEnvironment.PRODUCTION, BuiltInEnvironment.STAGING)
}
```

Providers that do not support staging (yet) simply reject non-production environments.

### 3. Rename the Existing `project.environment` Field

Terracotta's project model already exposes an `environment` field that describes the game-side runtime environment (e.g., `client`, `server`, or `both`). This is ambiguous with the new provider environment concept (production/staging). Before introducing the provider `environment` setting, we must rename the existing field to `gameEnvironment` (or equivalent) and update the YAML/DSL models, parsers, and documentation so the two concepts are clearly distinct.

**Old YAML**:
```yaml
project:
  environment: "client"
```

**New YAML**:
```yaml
project:
  gameEnvironment: "client"
```

**Gradle DSL**:
```kotlin
terracotta {
    project {
        gameEnvironment.set("client")
    }
}
```

### 4. Configuration

Add an optional `environment` field to provider configuration. Default is `production`.

**YAML**:
```yaml
platforms:
  modrinth:
    environment: staging
    projectId: "abc123"
```

**Gradle DSL**:
```kotlin
terracotta {
    modrinth {
        environment.set(Environment.STAGING)
        projectId.set("abc123")
    }
}
```

Top-level default environment could also be supported:

```yaml
environment: staging
platforms:
  modrinth:
    projectId: "abc123"
```

### 5. Task and Validation Behavior

- A provider configured for staging uses its staging endpoint for all API calls.
- The `validateTerracottaConfig` task should warn when staging is enabled so CI logs are explicit.
- Publish/import tasks targeting staging should include the environment name in their output.
- Cross-provider checks should account for environment differences where relevant (e.g., staging may not support all category IDs).

### 6. CLI / Gradle Task Flags

Optionally allow overriding the environment from the command line for one-off tests:

```bash
./gradlew publishTerracotta --environment=staging
```

This overrides the configured value for the run but does not mutate configuration files.

## Migration Path

1. Add `TerracottaEnvironment` and `ProviderEnvironmentSupport` to `terracotta-core`.
2. Rename the existing `project.environment` field to `gameEnvironment` (or equivalent) and update all models, parsers, and documentation.
3. Implement `ModrinthEnvironments` in `terracotta-provider-modrinth`.
4. Add `environment` to provider configuration models and DSL extensions.
5. Update provider clients to select endpoints based on the configured environment.
6. Update validation rules to accept/validate environment values per provider.
7. Add integration tests that hit Modrinth Staging for real publish/validate flows behind a flag.
8. Document staging usage, limitations, and how to obtain staging project IDs.

## Benefits

1. **Safer testing**: Users can exercise the full publish flow without real releases.
2. **Better CI**: Pipelines can run staging publishes as part of regression testing.
3. **Consistent abstraction**: Future providers gain staging support by implementing the same interface.
4. **Explicit and safe**: Staging must be configured deliberately; production remains the default.

## Risks & Considerations

1. **Provider parity**: Staging APIs may differ subtly from production (rate limits, allowed values, auth).
   - **Mitigation**: Document known differences per provider; keep provider-specific environment support isolated.

2. **State leakage**: Staging projects may share IDs or slugs with production, causing confusion.
   - **Mitigation**: Clear task output and warnings; consider requiring explicit staging project IDs.

3. **Security**: Staging credentials might be reused from production.
   - **Mitigation**: Document best practices; do not store credentials in repository files.

4. **Breaking change**: Adding an `environment` field changes the configuration schema.
   - **Mitigation**: Default to `production`; existing configurations remain valid.

## Next Steps

1. ✅ Define scope and draft proposal
2. 🔄 Rename existing `project.environment` to `gameEnvironment` (or equivalent) to avoid ambiguity
3. 🔄 Add `TerracottaEnvironment` abstraction to `terracotta-core`
4. 🔄 Implement Modrinth staging endpoint support
5. 🔄 Add `environment` to provider configuration and DSL
6. 🔄 Update provider clients to use environment-aware endpoints
7. 🔄 Update validation and task output
8. 🔄 Add tests and documentation

## References

- [Modrinth Staging API Documentation](https://docs.modrinth.com)
- [Authentication Workflows](./2025-07-authentication-workflows.md)
- [Config Validation](./2025-07-config-validation.md)
