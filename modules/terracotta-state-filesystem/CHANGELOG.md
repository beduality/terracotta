# Changelog — Terracotta State Filesystem

All notable changes to `terracotta-state-filesystem` will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.6.0] - 2026-07-12

Introduced pluggable state management and canonical project links. State persistence is now backend-agnostic, with the file-backed implementation extracted into its own module, and project links now map consistently between Terracotta, Modrinth, and Hangar with full Gradle DSL support.

### Added

- Added new `terracotta-state-filesystem` module with `FileSystemStateSource`, `YamlStateCodec`, and `FileSystemStateSourceFactory` (id `"filesystem"`). The factory is registered via `META-INF/services/io.github.beduality.terracotta.core.state.StateSourceFactory` and uses the `path` setting, defaulting to `.terracotta-state.yml` in the project directory.

### Changed

- Moved `FileSystemStateSource` and `YamlStateCodec` from `terracotta-core` to the new `terracotta-state-filesystem` module. The package name `io.github.beduality.terracotta.core.state` is preserved, so existing imports continue to work when the new module is on the classpath.
