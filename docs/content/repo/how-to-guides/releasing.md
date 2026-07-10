# Releasing

This guide covers the automated release process for Terracotta.

## Overview

Releases are handled by an automated release script that:

- Bumps versions based on conventional commits
- Updates the changelog
- Creates and pushes a git tag
- Triggers the release workflow for distribution

## Versioning

Releases follow [Semantic Versioning](https://semver.org/). The release script automatically determines the version bump:

| Commit Type | Version Bump |
|-------------|--------------|
| `fix` | patch (0.1.x) |
| `feat` | minor (0.x.0) |
| `BREAKING CHANGE` or `!` suffix | major (x.0.0) |

In wizard mode, the script displays the detected bump and suggested version as the default.

## Release Process

### 1. Update the Changelog

Before releasing, add all user-facing changes under `## [Unreleased]` in `CHANGELOG.md`:

```md
## [Unreleased]

### Added
#### Docs
- Added new documentation page for X

### Changed
#### Core
- Updated Y to improve Z

### Fixed
#### CLI
- Fixed issue with command ABC
```

Follow the guidelines in `guidelines/changelog.md`:
- Focus on user/developer/operator impact
- Group by module (Docs, Core, Gradle Plugin, Modrinth, SDK, etc.)

### 2. Run the Release Script

```bash
uv run scripts/release.py
```

The script will:

1. Show the detected version bump and suggested version
2. Let you accept or override the version
3. Update `CHANGELOG.md` with the new version
4. Create a git commit and tag
5. Push changes to the remote repository

### 3. Distribution & Publishing

Once the version tag is pushed, `.github/workflows/release.yml` automatically triggers:

#### Gradle Plugin Publishing

The Gradle plugin is automatically published to the [Gradle Plugin Portal](https://plugins.gradle.org/).

#### Maven Central Publishing

The following modules are published automatically:

| Module | Maven Coordinates |
|--------|-------------------|
| Core | `io.github.beduality:terracotta-core` |
| Modrinth Provider | `io.github.beduality:terracotta-provider-modrinth` |

The workflow uses the [Nexus Publish Plugin](https://github.com/gradle-nexus/publish-plugin) to:

- Build and sign artifacts with GPG
- Upload to Sonatype OSSRH staging
- Automatically close and release the staging repository

### Release Secrets

The release workflow uses these GitHub repository secrets:

| Secret | Purpose |
|--------|---------|
| `OSSRH_USERNAME` | Sonatype OSSRH username |
| `OSSRH_PASSWORD` | Sonatype OSSRH password/token |
| `SIGNING_KEY` | GPG private key (ASCII-armored) |
| `SIGNING_PASSWORD` | GPG key passphrase |

Configure these in your repository settings under **Secrets and variables** → **Actions**.

## Manual Override

To manually specify the version:

```bash
uv run scripts/release.py patch     # 0.1.x
uv run scripts/release.py minor     # 0.x.0
uv run scripts/release.py major     # x.0.0
uv run scripts/release.py 0.2.0     # custom version
```

## Rollback

If a release needs to be rolled back:

```bash
uv run scripts/release.py rollback
```

This reverts the version bump, changelog changes, and tag.

## What Happens on Release

1. **Changelog**: `## [Unreleased]` → `## [0.x.y] - YYYY-MM-DD`
2. **Version**: Updated in `build.gradle.kts`
3. **Tag**: Created and pushed to GitHub
4. **CI/CD**: Release workflow triggers
5. **Gradle Plugin**: Published to Gradle Plugin Portal
6. **Maven Central**: Artifacts published and released
