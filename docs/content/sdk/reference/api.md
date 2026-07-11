# API Reference

The Terracotta SDK provides a modular, platform-agnostic API for Minecraft project registry management.

For detailed documentation on `terracotta-core` data models, loader registry, diff engine, metadata resolution, and provider interfaces, see the dedicated [Core documentation](../../core/README.md).

## Core API entry points

| Concern | Core docs | KDoc |
|---------|-----------|------|
| Data models | [Models](../../core/reference/models.md) | [Core API Docs](../../../apidocs/terracotta-core/index.html) |
| Loader detection | [Loaders](../../core/reference/loaders.md) | [Core API Docs](../../../apidocs/terracotta-core/index.html) |
| Metadata resolution | [Metadata Resolution](../../core/reference/metadata-resolution.md) | [Core API Docs](../../../apidocs/terracotta-core/index.html) |
| Diff engine | [Operations](../../core/reference/operations.md) | [Core API Docs](../../../apidocs/terracotta-core/index.html) |
| Provider SPI | [Provider Interfaces](../../core/reference/provider-interfaces.md) | [Core API Docs](../../../apidocs/terracotta-core/index.html) |

## Package overview

| Package | Purpose |
|---------|---------|
| `io.github.beduality.terracotta.core.*` | Domain models, resolution, diff engine, and provider interfaces. See [Core docs](../../core/README.md). |
| `io.github.beduality.terracotta.provider.modrinth` | Modrinth provider implementation. |
| `io.github.beduality.terracotta.provider.hangar` | Hangar provider implementation. |

## Provider implementations

### Modrinth Provider

| Class | Responsibility |
|-------|----------------|
| `ModrinthProviderFactory` | ServiceLoader entry point for Modrinth. |
| `ModrinthStateProvider` | Fetches project state from Modrinth. |
| `ModrinthRegistryProvider` | Applies operations to Modrinth. |
| `ModrinthClient` | Low-level HTTP client. |

For implementation guidance, see the [Implement a Custom Provider](../../core/tutorials/implementing-a-custom-provider.md) tutorial and the [Provider API](provider-api.md) reference.

## Dependencies

### Core SDK

```kotlin
implementation("io.github.beduality:terracotta-core:0.2.0")
```

| Dependency | Purpose |
|------------|---------|
| Kotlin | Language runtime |
| Kotlinx Serialization | JSON serialization |
| Kotlinx Coroutines | Async operations |

### Modrinth Provider

```kotlin
implementation("io.github.beduality:terracotta-provider-modrinth:0.2.0")
```

| Dependency | Purpose |
|------------|---------|
| Ktor Client | HTTP client |
| Kotlinx Serialization | JSON serialization |

### Hangar Provider

```kotlin
implementation("io.github.beduality:terracotta-provider-hangar:0.2.0")
```

## See also

- [Installing the Terracotta SDK](installation.md)
- [Provider API](provider-api.md)
- [Core documentation](../../core/README.md)