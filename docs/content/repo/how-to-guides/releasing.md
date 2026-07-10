# Releasing

This guide covers the automated release process for Terracotta.

## Overview

Releases are handled by an automated release script that:

- Bumps versions based on conventional commits
- Updates the changelog
- Validates the publishing configuration
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
4. Run `./gradlew build` to verify compilation and tests pass
5. Create a git commit and tag
6. Push changes to the remote repository

### 3. Distribution & Publishing

Once the version tag is pushed, `.github/workflows/release.yml` automatically triggers:

#### Maven Central Publishing

The following modules are published automatically via the [Sonatype Central Portal](https://central.sonatype.com/):

| Module | Maven Coordinates |
|--------|-------------------|
| Core | `io.github.beduality:terracotta-core` |
| Gradle Plugin | `io.github.beduality:terracotta-gradle-plugin` |
| Modrinth Provider | `io.github.beduality:terracotta-provider-modrinth` |

The workflow uses the [central-portal-publisher](https://github.com/tddworks/central-portal-publisher) Gradle plugin to:

- Bundle all module artifacts with proper Maven repository layout
- Sign artifacts with GPG
- Upload the bundle to the Sonatype Central Portal
- Automatically publish to Maven Central

### Release Secrets

The release workflow uses these GitHub repository secrets:

| Secret | Purpose |
|--------|---------|
| `SONATYPE_USERNAME` | Sonatype Central Portal username |
| `SONATYPE_PASSWORD` | Sonatype Central Portal password/token |
| `SIGNING_KEY` | GPG private key (ASCII-armored) |
| `SIGNING_PASSWORD` | GPG key passphrase |

Configure these in your repository settings under **Secrets and variables** → **Actions**.

#### Loading Secrets via Pulumi

The `terracotta-github` module manages GitHub Actions secrets as infrastructure. To sync secrets from your `.env` file into Pulumi config (which then provisions them as GitHub Actions secrets):

```bash
export PULUMI_CONFIG_PASSPHRASE="your-passphrase"
uv run scripts/load-pulumi-secrets.py
cd modules/terracotta-github && pulumi up
```

The `PULUMI_CONFIG_PASSPHRASE` env var is required for non-interactive secret encryption. Alternatively, use `PULUMI_CONFIG_PASSPHRASE_FILE` to point to a file containing the passphrase.

The script auto-detects which secrets are needed by parsing `App.kt` — no manual list to maintain.

## Manual Override

To manually specify the version:

```bash
uv run scripts/release.py patch     # 0.1.x
uv run scripts/release.py minor     # 0.x.0
uv run scripts/release.py major     # x.0.0
uv run scripts/release.py 0.2.0     # custom version
```

## Skip Dry-Run

To skip the build verification dry-run step:

```bash
uv run scripts/release.py 0.2.0 --no-dry-run
```

## Rollback

If a release needs to be rolled back:

```bash
uv run scripts/release.py rollback
```

This reverts the version bump, changelog changes, and tag.

## What Happens on Release

1. **Changelog**: `## [Unreleased]` → `## [0.x.y] - YYYY-MM-DD`
2. **Version**: Updated in `gradle.properties`
3. **Tag**: Created and pushed to GitHub
4. **CI/CD**: Release workflow triggers
5. **Maven Central**: Artifacts bundled, signed, and published via Central Portal
