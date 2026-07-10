# Architecture Reference

This reference documents the module structure and responsibilities in the Terracotta repository.

## Module Architecture

Terracotta uses a **multi-module Gradle project** to separate platform-agnostic business logic from specific registry providers.

```
modules/
├── terracotta-core/              # Pure domain library
├── terracotta-provider-modrinth/ # Modrinth provider implementation
├── terracotta-gradle-plugin/     # Gradle plugin frontend
└── terracotta-github/            # Pulumi infrastructure
```

## Module Details

### terracotta-core

**Package**: `io.github.beduality.terracotta.core.*`

The core SDK with canonical models, provider interfaces, and the semantic diff engine.

**Characteristics**:
- Pure Kotlin/JVM - no external dependencies
- Published to Maven Central as `io.github.beduality:terracotta-core`
- No network, framework, or build-tool dependencies

**Responsibilities**:
- Canonical data models (`TerracottaProject`, `TerracottaVersion`)
- Provider interfaces (`ProviderFactory`, `StateProvider`, `RegistryProvider`)
- Diff engine (`DiffEngine`, `Operation` sealed interface)

**Key Components**:

| Component | Purpose |
|-----------|---------|
| `core.model.*` | Domain models (Project, Version, Environment, Loader, ReleaseType) |
| `core.provider.*` | Provider interfaces for registry integration |
| `core.diff.*` | Semantic diff engine and operations |

### terracotta-provider-modrinth

**Package**: `io.github.beduality.terracotta.provider.modrinth.*`

Modrinth registry provider implementation.

**Characteristics**:
- Depends on Ktor Client and Kotlinx Serialization
- Published to Maven Central as `io.github.beduality:terracotta-provider-modrinth`

**Responsibilities**:
- Implement `ProviderFactory` for Modrinth
- Implement `StateProvider` to fetch project state from Modrinth API
- Implement `RegistryProvider` to apply changes to Modrinth API

**Key Components**:

| Component | Purpose |
|-----------|---------|
| `ModrinthProviderFactory` | Factory creating Modrinth providers |
| `ModrinthClient` | Low-level HTTP client for Modrinth API |
| `ModrinthStateProvider` | Fetches project state from Modrinth |
| `ModrinthRegistryProvider` | Applies operations to Modrinth |

### terracotta-gradle-plugin

**Package**: `io.github.beduality.terracotta.gradle.*`

Build-tool integration providing tasks with Gradle DSL.

**Characteristics**:
- Uses ServiceLoader for provider discovery
- Depends on Gradle API and core modules
- Published to Gradle Plugin Portal

**Responsibilities**:
- Register Gradle plugin and extension
- Create `terracottaPlan` and `terracottaApply` tasks
- Discover and configure providers dynamically

**Key Components**:

| Component | Purpose |
|-----------|---------|
| `TerracottaPlugin` | Plugin entry point |
| `TerracottaExtension` | DSL configuration entry point |
| `TerracottaPlanTask` | Computes and displays diff |
| `TerracottaApplyTask` | Computes and applies diff |

### terracotta-github

**Package**: `com.pulumi.terracotta.*`

Infrastructure management using Pulumi.

**Characteristics**:
- Uses Pulumi Java/Kotlin SDK
- Manages GitHub repository configuration
- Not published to Maven Central

**Responsibilities**:
- Manage repository settings and permissions
- Configure GitHub Actions secrets
- Set up repository rules and webhooks

## Provider Discovery

Providers are discovered at runtime via Java's `ServiceLoader`:

1. Provider modules register implementations in `META-INF/services/`
2. The Gradle plugin loads all available `ProviderFactory` instances
3. Users configure which providers to use in their Gradle DSL

## Package Dependencies

| Module | Depends On |
|--------|-----------|
| `terracotta-core` | Standard Kotlin/Java APIs only |
| `terracotta-provider-modrinth` | `terracotta-core`, Ktor Client, Kotlinx Serialization |
| `terracotta-gradle-plugin` | `terracotta-core`, Gradle API |
| `terracotta-github` | Pulumi SDK |

## Versioning

- Core modules published to Maven Central
- Gradle plugin published to Gradle Plugin Portal
- All versions tracked in `gradle/libs.versions.toml`