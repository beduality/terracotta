# Docs Changelog

Tracks changes to the documentation site (structure, pages, style, navigation).
Promoted manually per major version, matching the mike versioning scheme.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Changed

- Reworked the docs homepage (`index.md`) to be more succinct: replaced the dense numbered workflow description with a Mermaid diagram and folded the gallery note and example output into accordions.

### Fixed

- Fixed broken MkDocs Material content tab syntax in authentication sections of the Gradle plugin and integration how-to guides.
- Removed obsolete per-provider plan/apply task names from the docs homepage quick reference.
- Updated Gradle plugin and provider tutorials to explicitly install the `terracotta-state-filesystem` backend instead of assuming it is bundled.
