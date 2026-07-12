# Releasing

This guide covers the release process for Terracotta.

## Overview

Releases are performed from the `Release` GitHub Actions workflow. Running the release in CI makes the process safer and easier to roll back:

- Bumping versions, changelog updates, build verification, Maven Central publishing, and git tagging all happen in one workflow.
- If publishing to Maven Central fails, `release.py` automatically rolls back the local tag and commit so no tag or changelog is left behind.
- The GitHub release and docs deployment only happen after a successful publish.
- The workflow can be re-run or the release can be re-triggered without force-pushing or manually cleaning up.

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

Follow the [Changelog Guidelines](../reference/changelog-guidelines.md):
- Focus on user/developer/operator impact
- Group by module (Docs, Repo, Core, Gradle Plugin, Modrinth, Hangar, etc.)

### 2. Trigger the Release Workflow

Go to **Actions** → **Release** → **Run workflow**. Choose:

- **bump**: `auto` (recommended), `patch`, `minor`, `major`, or `custom`
- **version**: required only when `bump` is `custom`

#### Trigger from the local CLI

You can also start the workflow from your machine without opening the GitHub web UI. Make sure the [GitHub CLI](https://cli.github.com/) is installed and authenticated (`gh auth login`):

```bash
# Wizard mode (prompts for bump type and confirmation)
uv run scripts/release.py trigger

# Non-interactive trigger
uv run scripts/release.py trigger --bump auto --yes

# Custom version
uv run scripts/release.py trigger --bump custom --version 1.2.3 --yes
```

The command runs `gh workflow run release.yml` against your current branch, passing the same `bump` and `version` inputs used by the web UI. After triggering, the script extracts the run ID and asks if you want to watch it live.

Watch the run in real time from the CLI:

```bash
# Monitor the latest release.yml run
uv run scripts/release.py monitor

# Monitor a specific run
uv run scripts/release.py monitor 1234567890
```

The workflow will:

1. Update `CHANGELOG.md`, `gradle.properties`, `pyproject.toml`, and `uv.lock` with the new version.
2. Run `./gradlew spotlessCheck build` to verify compilation and tests.
3. Publish to Maven Central using the central-portal-publisher plugin.
4. If publishing succeeds, commit the version changes and push the new `vX.Y.Z` tag.
5. Create a GitHub release with the changelog body and JAR artifacts.
6. Deploy documentation using `deploy-docs.yml`.

### 3. Distribution & Publishing

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

## Manual Override / Local

For local testing or dry runs, you can still run `release.py` locally:

```bash
uv run scripts/release.py --no-publish --no-push
```

This will run the version bump, changelog update, and build verification without actually publishing or pushing. To see the detected version without prompts, run:

```bash
uv run scripts/release.py --yes --no-publish --no-push
```

To run the full release from a local machine (not recommended):

```bash
uv run scripts/release.py --yes --publish --bump auto
```

This requires the same `SONATYPE_USERNAME`, `SONATYPE_PASSWORD`, `SIGNING_KEY`, and `SIGNING_PASSWORD` environment variables.

## Skip Dry-Run

To skip the build verification step:

```bash
uv run scripts/release.py --no-dry-run --no-publish --no-push
```

## Rollback

If the CI workflow fails, `release.py` will attempt to roll back the local tag and commit automatically. If a manual rollback is needed, run:

```bash
uv run scripts/release.py rollback
```

This reverts the version bump, changelog changes, and tag.

## What Happens on Release

1. **Changelog**: `## [Unreleased]` → `## [0.x.y] - YYYY-MM-DD`
2. **Version**: Updated in `gradle.properties`, `pyproject.toml`, and `uv.lock`
3. **Build**: `./gradlew spotlessCheck build` verifies the project
4. **Publish**: artifacts are signed and uploaded to Maven Central
5. **Tag**: the new `vX.Y.Z` tag is pushed to GitHub
6. **GitHub Release**: created with the changelog body and JARs
7. **Docs**: documentation is deployed with `mike`
