# Proposal: Add GitHub Release Provider

**Date**: 2025-07-12  
**Status**: Draft  
**Author**: Terracotta Team

## Summary

Add a `terracotta-provider-github-release` module that publishes Terracotta projects to **GitHub Releases**. It depends on the `terracotta-provider-git` module for Git-level operations (tags and file commits) and focuses exclusively on GitHub Releases: creating releases, composing release notes, and uploading assets. It is designed as a complementary distribution channel, not a Minecraft registry. Operations that do not map to GitHub Releases (gallery, repository lifecycle, repository topics, per-version loaders/game versions) are skipped with a warning rather than emulated poorly. All repository metadata and Git content writes are handled by the Git provider.

## Problem Statement

Many Minecraft projects distribute their JARs through GitHub Releases as a primary or fallback channel. Terracotta currently supports only Modrinth and Hangar, so users must maintain a separate GitHub Actions step or manual workflow to create releases and upload assets. A first-class provider would let teams publish to GitHub using the same `terracotta.yml` configuration and `terracottaApply` task they already use for registries.

## Goals

1. Allow users to publish JAR artifacts to GitHub Releases from a Terracotta run.
2. Compose release notes from `version.changelog` and project metadata in a configurable way.
3. Support additional release assets such as sources JARs, javadoc JARs, and checksums.
4. Refuse or warn for operations that do not fit the GitHub Releases model instead of surprising users.
5. Reuse the existing provider SPI (`ProviderFactory`, `RegistryProvider`, etc.).
6. Build on the `terracotta-provider-git` module for Git tags and file commits rather than duplicating that logic.

## Proposed Changes

### 1. Provider Identity

- **Provider ID**: `github-release`
- **Module**: `terracotta-provider-github-release`
- **Dependency**: `terracotta-provider-git` (for Git tags and file commits).
- **Token source**: `GITHUB_TOKEN` environment variable (or explicit `token` field).
- **Project ID**: GitHub repository in `owner/name` format.

### 2. Supported Operations

| Terracotta Operation | GitHub Mapping | Notes |
|---|---|---|
| `CreateProject` | Skip with warning | Repository lifecycle is handled by the `git` provider. |
| `UpdateMetadata` | Skip with warning | Repository description and `LICENSE` are handled by the `git` provider. |
| `UpdateTags` | Skip with warning | Repository topics are handled by the `git` provider. |
| `UpdateDescription` | Delegate to Git provider | `project.description` → `README.md` is handled by the `git` provider if configured. The GitHub Release provider skips if not configured. |
| `UploadVersion` | Create release + upload assets | Requires a Git tag. The Git provider creates the tag when `git.tag.actions` includes `create`. The GitHub Release provider creates the release and uploads the assets. |
| `UploadGalleryItem` | Skip with warning | No native gallery concept. |
| `UpdateGalleryItem` | Skip with warning | No native gallery concept. |
| `DeleteGalleryItem` | Skip with warning | No native gallery concept. |

### 3. Release Metadata

- **Release title**: `version.displayName`
- **Release body**: `version.changelog`
- **Release assets**: the primary JAR at `version.artifactPath` plus any additional assets configured via `assets`.
- **Pre-release flag**: derived from `version.releaseType` (`alpha`/`beta` → `true`, `release` → `false`).
- **Git tag**: required. The Git provider creates the tag if configured; otherwise the tag must already exist.

### 3.1 Minecraft-Specific Metadata in Release Body

GitHub releases have no native fields for Minecraft loaders, game versions, or environment. The provider inserts this metadata as a structured header at the top of the release body, followed by the changelog, so the compatibility information is immediately visible without interrupting the release notes.

**Default header format**:

```markdown
**Loaders:** fabric, paper
**Game versions:** 1.20, 1.21
**Environment:** server

---
```

The header is omitted entirely when the version has no loaders, game versions, or environment specified.

**Configuration**:

```yaml
providers:
  github-release:
    releaseBody:
      includeLoaders: true
      includeGameVersions: true
      includeEnvironment: true
      template: |
        **Loaders:** {{loaders}}
        **Game versions:** {{gameVersions}}
        **Environment:** {{environment}}

        ---
```

- `includeLoaders` (boolean, default `true`) — include supported loaders in the header.
- `includeGameVersions` (boolean, default `true`) — include supported game versions in the header.
- `includeEnvironment` (boolean, default `true`) — include the environment (`client`, `server`, or `both`) in the header.
- `template` (string, optional) — custom template for the header. Available variables: `{{loaders}}`, `{{gameVersions}}`, `{{environment}}`, `{{changelog}}`, `{{displayName}}`, `{{version}}`.

If `template` is omitted, the provider uses the default header. If `template` is provided, it replaces the default header while the main changelog still comes from `version.changelog`. Setting all three `include*` flags to `false` disables the header.

### 3.2 Additional Release Assets

By default, the provider uploads only the primary JAR at `version.artifactPath`. Users can configure additional files to upload as release assets, such as sources JARs, javadoc JARs, signatures, or checksums.

```yaml
providers:
  github-release:
    assets:
      - path: "build/libs/{{project.name}}-{{version}}-sources.jar"
      - path: "build/libs/{{project.name}}-{{version}}-javadoc.jar"
      - path: "build/distributions/*.sha256"
```

**Gradle DSL**:

```kotlin
terracotta {
    providers {
        create("github-release") {
            assets.set(
                listOf(
                    "build/libs/{{project.name}}-{{version}}-sources.jar",
                    "build/libs/{{project.name}}-{{version}}-javadoc.jar",
                )
            )
        }
    }
}
```

Supported placeholders:

- `{{project.name}}` — project slug/name.
- `{{project.id}}` — project ID.
- `{{version}}` — raw version string.
- `{{version.displayName}}` — version display name.
- `{{releaseType}}` — release type (`alpha`, `beta`, `release`).

Glob patterns are supported. Each resolved path is uploaded as a release asset. If a configured path resolves to no files, the provider fails with a clear error so that a release is not published with missing expected assets. The primary JAR at `version.artifactPath` is always uploaded regardless of the `assets` configuration.

### 4. Configuration

**YAML**:

```yaml
providers:
  github-release:
    projectId: "beduality/terracotta"
    releaseBody:
      includeLoaders: true
      includeGameVersions: true
      includeEnvironment: true
    assets: []
```

**Gradle DSL**:

```kotlin
terracotta {
    providers {
        create("github-release") {
            projectId.set("beduality/terracotta")
            releaseBody {
                includeLoaders.set(true)
                includeGameVersions.set(true)
                includeEnvironment.set(true)
            }
            assets.set(emptyList())
        }
    }
}
```

All release-specific settings are optional and default to safe values. The provider does not modify repository metadata or Git content; those are handled by the `git` provider.

### 5. Client Design

Use the GitHub REST API via Ktor (consistent with Modrinth and Hangar clients):

- `GET /repos/{owner}/{repo}` to read repository metadata needed for the release.
- `POST /repos/{owner}/{repo}/releases` to create a release.
- `POST {upload_url}` to upload the release assets.

Git operations (tags, `README.md`, `LICENSE`, `CHANGELOG.md`) are delegated to the `terracotta-provider-git` module. The GitHub Release provider may call the Git provider's API directly or rely on the Git provider being configured in the same Terracotta run.

Authentication via `Authorization: Bearer {token}`.

### 6. Provider Ordering and Dependencies

The `github-release` provider declares a runtime dependency on the `git` provider. Terracotta's provider execution engine topologically sorts providers by their declared dependencies and runs them in order (see the [Provider Topological Sort Proposal](2025-07-provider-topological-sort.md)). This guarantees that when both providers are configured, the `git` provider runs first and creates tags, commits files, and updates repository metadata before the `github-release` provider attempts to create a release.

**Example combined configuration**:

```yaml
providers:
  git:
    projectId: "beduality/terracotta"
    platform: "github"
    git:
      tag:
        prefix: "v"
        actions: ["create"]
    repo: false
    repoDescription: false
    license: false
    repoTopics: false
    readme: true
    changelog: true
  github-release:
    projectId: "beduality/terracotta"
    releaseBody:
      includeLoaders: true
      includeGameVersions: true
      includeEnvironment: true
```

In this configuration, Terracotta runs the `git` provider first, then the `github-release` provider. If `git` is not configured and `github-release` needs a tag that does not already exist, the `github-release` provider fails fast with a clear error telling the user to configure the `git` provider or create the tag manually.

## Migration Path

1. Create `terracotta-provider-github-release` module with build configuration.
2. Add `terracotta-provider-git` as a module dependency.
3. Implement `GitHubProviderFactory`, `GitHubStateProvider`, and `GitHubRegistryProvider`.
4. Implement `GitHubClient` with the endpoints above.
5. Declare the `github-release` provider's runtime dependency on the `git` provider so the execution engine orders them correctly.
6. Integrate with the Git provider for tag and file operations.
7. Map Terracotta operations as described, warning for unsupported ones.
8. Register the factory via `ServiceLoader` (`META-INF/services/io.github.beduality.terracotta.core.provider.ProviderFactory`).
9. Add unit tests with mocked GitHub API responses.
10. Update provider configuration reference and multi-provider tutorial.

## Benefits

1. **Single workflow**: Publish to Modrinth, Hangar, and GitHub Releases from one configuration.
2. **Less CI glue**: No need for a separate GitHub Actions release step.
3. **Clear separation**: Git operations and repository metadata are handled by the Git provider; GitHub Releases are handled by the GitHub Release provider.
4. **Consistent provider model**: GitHub fits the SPI even though the semantics differ from registries.

## Risks & Considerations

1. **Tag collisions**: If the tag already exists at a different commit, the release will attach to that commit.
   - **Mitigation**: The Git provider fetches the tag first and fails fast if it exists with a different SHA. The GitHub Release provider only proceeds when the expected tag is present.

2. **Minecraft-specific metadata is lost**: Loaders and game versions only appear in release notes.
   - **Mitigation**: Accept this as a GitHub limitation; it is still useful as an artifact host.

3. **Dependency on the Git provider**: The GitHub Release provider relies on `terracotta-provider-git` for tag creation and file commits. If the Git provider is not configured or not available, the GitHub Release provider cannot create tags or update files.
   - **Mitigation**: Document clearly that users must configure both providers when they want GitHub releases with tag creation or file commits. Ensure the Git provider SPI is stable.

4. **Provider is configurable but opinionated with safe defaults**: GitHub already has its own release workflow and idioms. The Terracotta GitHub Release provider is highly configurable, yet it still makes deliberate, opinionated choices (e.g., skipping the gallery) and ships with conservative defaults. It does not modify repository metadata or Git content. It should remain an optional distribution channel. The module documentation must prominently state that the provider is optional and defaults to safe, minimal behavior, so users understand that it is not a replacement for GitHub's native release tools or a full GitHub release management solution.
   - **Mitigation**: Add an explicit note in `terracotta-provider-github-release` documentation and tutorials describing which GitHub Release concepts are mapped, which are skipped, that users can opt in or ignore the provider entirely, and that Git operations require the `git` provider.

## Next Steps

1. ✅ Draft proposal
2. 🔄 Create `terracotta-provider-github-release` module skeleton and build configuration
3. 🔄 Add `terracotta-provider-git` dependency
4. 🔄 Implement `GitHubClient` and provider classes
5. 🔄 Integrate with the Git provider for tag and file operations
6. 🔄 Add operation mappings and warning logic
7. 🔄 Write unit and integration tests
8. 🔄 Update documentation and provider configuration reference
9. 🔄 Add GitHub provider to smoke test suite

## References

- [GitHub Releases API](https://docs.github.com/en/rest/releases/releases)
- [GitHub Repositories API](https://docs.github.com/en/rest/repos/repos)
- [Git Provider Proposal](2025-07-git-provider.md)
- [Provider Topological Sort Proposal](2025-07-provider-topological-sort.md)
- [Provider interfaces reference](https://beduality.github.io/terracotta/content/modules/core/reference/provider-interfaces.html)
- [Publishing to Multiple Providers](../docs/content/integration/tutorials/publishing-to-multiple-providers.md)
