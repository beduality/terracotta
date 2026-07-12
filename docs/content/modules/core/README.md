# Core Module Documentation

`terracotta-core` is the platform-agnostic Kotlin library that powers Terracotta. It contains the canonical domain models, metadata resolution, diff engine, and provider interfaces used by the Gradle plugin and SDK consumers.

This documentation follows the [Documentation Framework](../../navigating-docs.md), which organizes content by user intent.

## Tutorials

Learning-oriented guides that walk you through implementing a small, working extension.

- **[Implement a Custom Loader](tutorials/implementing-a-custom-loader.md)**: Teach `terracotta-core` to detect a new Minecraft platform.
- **[Implement a Custom Metadata Detector](tutorials/implementing-a-custom-metadata-detector.md)**: Extract metadata from a project file Terracotta does not yet understand.
- **[Implement a Custom Provider](tutorials/implementing-a-custom-provider.md)**: Add support for a new project registry.

## How-To Guides

Task-oriented guides that solve specific problems.

- **[Load a `terracotta.yml` File](how-to-guides/load-terracotta-config.md)**: Parse configuration from disk.
- **[Resolve Project Metadata](how-to-guides/resolve-project-metadata.md)**: Combine explicit, detected, and default metadata.
- **[Compute a Diff](how-to-guides/compute-a-diff.md)**: Generate operations from local and remote project states.
- **[Add a New Loader](how-to-guides/add-a-new-loader.md)**: Register a loader at runtime.
- **[Add a Project-File Convention](how-to-guides/add-a-new-project-file-convention.md)**: Teach Terracotta to read a new README or changelog format.
- **[Normalize Game Versions](how-to-guides/normalize-game-versions.md)**: Convert raw version strings to canonical Minecraft identifiers.

## Reference

Lookup information for core concepts, schemas, and identifiers.

- **[Models](reference/models.md)**: Canonical project and version models.
- **[Loaders](reference/loaders.md)**: Built-in loader identifiers and detection files.
- **[Operations](reference/operations.md)**: Diff-engine operations and their behavior.
- **[Config Schema](reference/config-schema.md)**: `terracotta.yml` field reference.
- **[Metadata Resolution](reference/metadata-resolution.md)**: Precedence rules for explicit, detected, and default values.
- **[Conventions](reference/conventions.md)**: README and changelog conventions.
- **[Version Conventions](reference/version-conventions.md)**: Game-version and project-version parsing rules.
- **[Provider Interfaces](reference/provider-interfaces.md)**: SPI for registry integrations.
- **[API Documentation](reference/api.md)**: Link to the generated Dokka API reference.

## Explanation

Understanding-oriented content that explains why core is designed the way it is.

- **[Architecture](explanation/architecture.md)**: Why `terracotta-core` is build-tool and registry agnostic.
- **[Metadata Resolution](explanation/metadata-resolution.md)**: Why explicit values override detected values, and detected values override defaults.
- **[Diff Engine](explanation/diff-engine.md)**: Why Terracotta computes semantic operations instead of overwriting remote state.
- **[Loader Hierarchy](explanation/loader-hierarchy.md)**: Why loaders declare parent platforms.
- **[Conventions](explanation/conventions.md)**: Why project-file interpretation is pluggable.
