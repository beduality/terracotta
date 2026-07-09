# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **[Core]**: Canonical project and version domain models, provider abstractions (`StateProvider`, `RegistryProvider`), and `DiffEngine` calculating semantic operations. Includes support for full project creation when a project does not exist on the remote registry.
  - **Why**: Separates platform-agnostic business logic from registry integrations, facilitating robust diff calculations and enabling clean automation.
- **[CLI]**: Command-line frontend supporting `plan` and `apply` commands, complete with standard help options (`--help`) and colorized diff output.
  - **Why**: Provides developers with an ergonomic command-line interface to safely preview, get help on, and apply declarative updates.
- **[Modrinth]**: Modrinth state and registry integration using OkHttp and Jackson.
  - **Why**: Bootstraps the first concrete provider to sync project settings, metadata, and artifacts directly with Modrinth.
