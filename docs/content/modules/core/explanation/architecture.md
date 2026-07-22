# Architecture

Terracotta is split into layers so that registry adapters and build-tool frontends can be added without changing the domain logic.

## Separation of concerns

The codebase is organized into four layers:

- **Core (`terracotta-core`)**: Platform-agnostic domain logic: canonical models, metadata resolution, the diff engine, and the provider SPI.
- **Providers (`terracotta-provider-*`)**: Registry-specific implementations that translate between core models and registry APIs.
- **Build-tool frontends (e.g. `terracotta-gradle-plugin`)**: User-facing integrations that discover providers, load configuration, and expose tasks or commands.
- **Infrastructure (`terracotta-github`)**: Pulumi program that manages repository settings and GitHub Actions secrets.

## Why core knows nothing about registries

`terracotta-core` defines only interfaces and abstractions such as `ProviderFactory`, `StateProvider`, `RegistryProvider`, `ProviderLogic`, and `BaseRegistryProvider`. It never imports a provider implementation. This means:

- A new registry can be supported by adding a module, not by editing core.
- Core logic is tested without network access or external credentials.
- The same core can be reused by a Maven plugin, a CLI, a CI action, or any other frontend later.

## Why state is compared instead of overwritten

Registries such as Modrinth and Hangar store metadata and versions that may have been edited by humans or other tools. Overwriting the whole project would destroy those changes. Terracotta fetches the current remote state, computes a semantic diff, and produces targeted operations:

- `UpdateMetadata` for project-level fields such as name, summary, and license.
- `UpdateCategories` for categories.
- `UpdateDescription` for the project body.
- `UploadVersion` for new versions.

This makes the tool safe to run repeatedly: only actual differences are applied.

## Why metadata is resolved from multiple sources

Project metadata can come from `terracotta.yml`, the Gradle DSL, detected files such as `README.md`, or sensible defaults. Terracotta resolves them in a fixed precedence so users can override detected values explicitly, while still benefiting from auto-detection when they omit optional fields.

See [Metadata Resolution](metadata-resolution.md) for the precedence rules and [Loader Hierarchy](loader-hierarchy.md) for how loader detection works.

## Provider discovery

Providers are discovered at runtime through Java's `ServiceLoader`. A provider JAR contains:

```
META-INF/services/io.github.beduality.terracotta.core.provider.ProviderFactory
```

A frontend such as the Terracotta Gradle plugin loads every available factory, then activates only the providers the user configured. Multiple providers can run in the same invocation. See the [Gradle plugin architecture](../../gradle-plugin/explanation/architecture.md) for how the plugin wires discovery into a build.

## Versioning and releases

All modules are versioned independently via per-module `gradle.properties` and `CHANGELOG.md` files. Core and provider artifacts are published to Maven Central; the Gradle plugin is published to the Gradle Plugin Portal. See [Releasing](../../../repo/how-to-guides/releasing.md) for the release process.
