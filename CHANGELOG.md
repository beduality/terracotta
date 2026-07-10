# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **[Docs]**: Added documentation website at `https://beduality.github.io/terracotta/`.
  - **Why**: Provides documentation for the project.
- **[Core]**: Canonical project and version domain models, provider abstractions (`StateProvider`, `RegistryProvider`), and `DiffEngine` calculating semantic operations. Includes support for full project creation when a project does not exist on the remote registry.
  - **Why**: Separates platform-agnostic business logic from registry integrations, facilitating robust diff calculations and enabling clean automation.
- **[Gradle Plugin]**: Gradle plugin frontend supporting `terracottaPlan` and `terracottaApply` tasks, with configuration via the `terracotta` extension.
  - **Why**: Provides developers with an ergonomic build-tool integration that aligns with typical Minecraft mod/plugin workflows, with direct access to project artifacts and metadata.
- **[Modrinth]**: Modrinth state and registry integration using Ktor Client and Kotlinx Serialization.
  - **Why**: Bootstraps the first concrete provider to sync project settings, metadata, and artifacts directly with Modrinth.
- **[SDK]**: Core SDK and Modrinth provider available via Maven Central.
  - **Why**: Enables developers to use Terracotta as a library in their projects.
