# Docs Changelog

Tracks changes to the documentation site (structure, pages, style, navigation).
Promoted manually per major version, matching the mike versioning scheme.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

Built the initial documentation site with MkDocs Material and deployed at `https://beduality.github.io/terracotta/`, with versioned docs via mike.

### Changed

- Made the entire Last Changes card clickable (stretched-link overlay); module icon links remain separate.

### Added

- Added documentation site built with MkDocs Material, with Diátaxis-structured content, estimated reading times on every page, a homepage with feature cards and a plan/apply flowchart, and versioned docs via mike.
- Added Quick Start section: introduction, Getting Started tutorial, Navigating the Docs guide, Last Changes page (deployment manifest-driven, with module badge filters, versionless entry support, and responsive layout for mobile/tablet/laptop), docs changelog, and License page.
- Added Integration section: Modrinth publishing tutorial, how-to guides for adding Modrinth and Hangar to the Gradle plugin, troubleshooting, provider configuration reference, and integration design explanation.
- Added Modules section with Diátaxis-structured docs (tutorials, how-to guides, reference, explanation) for Core, Gradle Plugin, State Filesystem, Modrinth Provider, and Hangar Provider, plus Dokka API docs for each.
- Added Repo contributor documentation: building, testing, contributing, releasing, smoke testing, changelog writing (three-tier system and activity log format), code style, commit conventions, CI/CD, branch strategy, file structure, and tech stack.
