# Proposal: Add GitHub Release Provider

**Date**: 2025-07-12  
**Status**: Draft  
**Author**: Terracotta Team

## Summary

Add a `terracotta-provider-github` module that publishes Terracotta projects to **GitHub Releases**. It is designed as a complementary distribution channel, not a Minecraft registry. Operations that do not map to GitHub concepts (gallery, per-version loaders/game versions) are skipped with a warning rather than emulated poorly. All global repository metadata writes require explicit opt-in.

## Problem Statement

Many Minecraft projects distribute their JARs through GitHub Releases as a primary or fallback channel. Terracotta currently supports only Modrinth and Hangar, so users must maintain a separate GitHub Actions step or manual workflow to create releases and upload assets. A first-class provider would let teams publish to GitHub using the same `terracotta.yml` configuration and `terracottaApply` task they already use for registries.

## Goals

1. Allow users to publish JAR artifacts to GitHub Releases from a Terracotta run.
2. Keep repository metadata in sync where it maps cleanly (summary → repository description, description → README, license, topics). All global repository metadata writes require explicit opt-in.
3. Refuse or warn for operations that do not fit GitHub's model instead of surprising users.
4. Reuse the existing provider SPI (`ProviderFactory`, `RegistryProvider`, etc.).

## Proposed Changes

### 1. Provider Identity

- **Provider ID**: `github`
- **Module**: `terracotta-provider-github`
- **Token source**: `GITHUB_TOKEN` environment variable (or explicit `token` field).
- **Project ID**: GitHub repository in `owner/name` format.

### 2. Supported Operations

| Terracotta Operation | GitHub Mapping | Notes |
|---|---|---|
| `CreateProject` | Create or update repository | `repo.actions: ["create"]` creates the repository using `project.name` as the repo name. `repo.actions: ["update"]` updates repository settings. `repo.actions: ["destroy"]` deletes the repository. |
| `UpdateMetadata` | Update repository description and license | `project.summary` maps to repository description only when `repoDescription` is enabled; `project.license` maps to the repo license and `LICENSE` file only when `license` is enabled. |
| `UpdateTags` | Update repository topics | `project.tags` map to repository topics only when `repoTopics` is enabled. |
| `UpdateDescription` | Update README | `project.description` maps to `README.md` only when `readme` is enabled. |
| `UploadVersion` | Create release + upload JAR | Requires a Git tag. Provider creates the tag if it does not exist. |
| `UploadGalleryItem` | Skip with warning | No native gallery concept. |
| `UpdateGalleryItem` | Skip with warning | No native gallery concept. |
| `DeleteGalleryItem` | Skip with warning | No native gallery concept. |

### 3. Tag Strategy

GitHub Releases require an underlying Git tag. The provider should create the tag automatically when it does not exist, using a configurable prefix.

**Default configuration**:

```yaml
providers:
  github:
    projectId: "beduality/terracotta"
    git:
      tagPrefix: "v"
```

A version `1.2.3` produces tag `v1.2.3`. Users can set `git.tagPrefix` to `""` to use the raw version.

### 3.1 Tag Target Commit

When the provider creates the tag, it must know which Git commit the tag should point to. By default, the provider uses the current commit available in the CI environment (e.g., `GITHUB_SHA` or the checked-out HEAD). Users can override the target commit per version via provider-specific version configuration (the general mechanism for provider-specific version configuration is not yet implemented, but the GitHub provider will consume it once available).

**Example provider-specific version configuration**:

```yaml
versions:
  - version: "1.2.3"
    providers:
      github:
        gitCommit: "abc123def456"
```

**Gradle DSL**:

```kotlin
version {
    version.set("1.2.3")
    providers {
        github {
            gitCommit.set("abc123def456")
        }
    }
}
```

If the tag already exists, the provider does not move or recreate it; the `gitCommit` value is only used when the provider creates the tag.

### 4. Release Metadata

- **Release title**: `version.displayName`
- **Release body**: `version.changelog`
- **Release assets**: the JAR at `version.artifactPath`
- **Pre-release flag**: derived from `version.releaseType` (`alpha`/`beta` → `true`, `release` → `false`).

### 4.1 Minecraft-Specific Metadata in Release Body

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
  github:
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

### 4.2 Additional Release Assets

By default, the provider uploads only the primary JAR at `version.artifactPath`. Users can configure additional files to upload as release assets, such as sources JARs, javadoc JARs, signatures, or checksums.

```yaml
providers:
  github:
    assets:
      - path: "build/libs/{{project.name}}-{{version}}-sources.jar"
      - path: "build/libs/{{project.name}}-{{version}}-javadoc.jar"
      - path: "build/distributions/*.sha256"
```

**Gradle DSL**:

```kotlin
terracotta {
    providers {
        create("github") {
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

### 5. Repository Metadata

- `project.name` → GitHub repository name (only when `repo` is enabled with `create` or `update` action).
- `project.summary` → repository description (only when `repoDescription` is enabled with `update` action).
- `project.description` → `README.md` contents (only when `readme` is enabled with `update` action).
- `project.license` → repository license and `LICENSE` file (only when `license` is enabled and the value is a known SPDX identifier). The default file name is `LICENSE`; use `license.filename` to override it. The `license.actions` field controls whether to `create`, `update`, or `destroy` the file.
- `project.tags` → repository topics (only when `repoTopics` is enabled with `update` action).

### 6. Configuration

**YAML**:

```yaml
providers:
  github:
    projectId: "beduality/terracotta"
    git:
      tagPrefix: "v"
      name: "Terracotta"
      email: "terracotta@example.com"
    repo: false
    repoDescription: false
    license: false
    repoTopics: false
    readme: false
```

**Gradle DSL**:

```kotlin
terracotta {
    providers {
        create("github") {
            projectId.set("beduality/terracotta")
            git {
                tagPrefix.set("v")
                name.set("Terracotta")
                email.set("terracotta@example.com")
            }
            repo.set(false)
            repoDescription.set(false)
            license.set(false)
            repoTopics.set(false)
            readme.set(false)
        }
    }
}
```

All repository-level metadata flags default to `false`. The provider never creates the repository, updates the description, license, topics, or overwrites the README unless the corresponding flag is set to `true` or an object with the relevant `actions`. This keeps Terracotta runs safe for repositories that are already managed by hand or by other tooling.

### 6.1 Metadata Action Configuration

All repository-level metadata fields (`repo`, `repoDescription`, `license`, `repoTopics`, `readme`) can be set to either:

- `false` — disabled (default)
- `true` — enabled, allowing all supported actions with default settings
- an object with `actions` to limit which operations are allowed

For file-based metadata (`license`, `readme`), the object also accepts `filename`.

Supported actions per field:

- `repo`: `create`, `update`, `destroy`
- `repoDescription`: `update`, `destroy`
- `license`: `create`, `update`, `destroy`
- `repoTopics`: `update`, `destroy`
- `readme`: `create`, `update`, `destroy`

**Object form examples**:

```yaml
providers:
  github:
    repo:
      actions: ["create"]
    repoDescription:
      actions: ["update"]
    license:
      filename: "LICENSE"
      actions: ["create", "update"]
    repoTopics:
      actions: ["update"]
    readme:
      filename: "README.md"
      actions: ["create", "update"]
```

`actions: true` is shorthand for all actions supported by the field.

### 6.2 Git Committer Configuration

File operations performed via the GitHub Contents API (for example, creating or updating `LICENSE` and `README.md`) require a committer name and email. The provider exposes this through a `git` block.

```yaml
providers:
  github:
    git:
      name: "Terracotta"
      email: "terracotta@example.com"
```

**Gradle DSL**:

```kotlin
terracotta {
    providers {
        create("github") {
            git {
                name.set("Terracotta")
                email.set("terracotta@example.com")
            }
        }
    }
}
```

- `git.name` (string, required for file commits) — committer name used for API-created commits.
- `git.email` (string, required for file commits) — committer email used for API-created commits.

If a file commit is attempted and `git.name` or `git.email` is missing, the provider fails with a clear error message. No default is provided because the committer identity is repository-specific.

### 7. Client Design

Use the GitHub REST API via Ktor (consistent with Modrinth and Hangar clients):

- `GET /repos/{owner}/{repo}` to read repository metadata.
- `POST /orgs/{owner}/repos` or `POST /user/repos` to create the repository when `repo.actions` includes `create`.
- `PATCH /repos/{owner}/{repo}` to update repository settings when `repo.actions` includes `update`.
- `DELETE /repos/{owner}/{repo}` to delete the repository when `repo.actions` includes `destroy`.
- `PATCH /repos/{owner}/{repo}` to update the repository description when `repoDescription.actions` includes `update`.
- `PUT /repos/{owner}/{repo}/topics` to replace repository topics when `repoTopics.actions` includes `update`.
- `PUT /repos/{owner}/{repo}/contents/{filename}` to create or replace the license file when `license` is enabled, using the configured `git` committer.
- `DELETE /repos/{owner}/{repo}/contents/{filename}` to remove the license file when `license.actions` includes `destroy`, using the configured `git` committer.
- `PUT /repos/{owner}/{repo}/contents/{filename}` to create or replace the README when `readme` is enabled, using the configured `git` committer.
- `DELETE /repos/{owner}/{repo}/contents/{filename}` to remove the README when `readme.actions` includes `destroy`, using the configured `git` committer.
- `POST /repos/{owner}/{repo}/releases` to create a release.
- `POST {upload_url}` to upload the release asset.
- `POST /repos/{owner}/{repo}/git/refs` to create the tag reference.

Authentication via `Authorization: Bearer {token}`.

## Migration Path

1. Create `terracotta-provider-github` module with build configuration.
2. Implement `GitHubProviderFactory`, `GitHubStateProvider`, `GitHubRegistryProvider`, and optional `GitHubDestructiveRegistryProvider`.
3. Implement `GitHubClient` with endpoints above.
4. Map Terracotta operations as described, warning for unsupported ones.
5. Register the factory via `ServiceLoader` (`META-INF/services/io.github.beduality.terracotta.core.provider.ProviderFactory`).
6. Add unit tests with mocked GitHub API responses.
7. Update provider configuration reference and multi-provider tutorial.

## Benefits

1. **Single workflow**: Publish to Modrinth, Hangar, and GitHub Releases from one configuration.
2. **Less CI glue**: No need for a separate GitHub Actions release step.
3. **Consistent provider model**: GitHub fits the SPI even though the semantics differ from registries.

## Risks & Considerations

1. **Git tags are global**: Creating a tag on `UploadVersion` mutates the repository. Users may already have their own tagging workflow.
   - **Mitigation**: Make tag creation explicit via `createTag: true` and default to `false` if preferred. At minimum document the behavior.

2. **Tag collisions**: If the tag already exists at a different commit, the release will attach to that commit.
   - **Mitigation**: Fetch the tag first and fail fast if it exists with a different SHA.

3. **README writes are tempting but dangerous**: Updating `README.md` from `project.description` overwrites hand-written documentation.
   - **Mitigation**: Require explicit opt-in via `readme: true` or a `readme` object with `update` action; default to `false` and skip with a warning.

4. **Repository topics are global**: `UpdateTags` applies to the whole repo, not the version.
   - **Mitigation**: Require explicit opt-in via `repoTopics: true` or a `repoTopics` object with `update` action; default to `false` and skip with a warning.

5. **Minecraft-specific metadata is lost**: Loaders and game versions only appear in release notes.
   - **Mitigation**: Accept this as a GitHub limitation; it is still useful as an artifact host.

6. **Provider is configurable but opinionated with safe defaults**: GitHub already has its own release workflow and idioms. The Terracotta GitHub provider is highly configurable, yet it still makes deliberate, opinionated choices (e.g., mapping repo topics, skipping the gallery) and ships with conservative defaults. All global metadata writes are disabled by default and tag creation is opt-in. It should remain an optional distribution channel. The module documentation must prominently state that the provider is optional and defaults to safe, minimal behavior, so users understand that it is not a replacement for GitHub's native release tools or a full GitHub release management solution.
   - **Mitigation**: Add an explicit note in `terracotta-provider-github` documentation and tutorials describing which GitHub concepts are mapped, which are skipped, and that users can opt in or ignore the provider entirely.

## Next Steps

1. ✅ Draft proposal
2. 🔄 Create module skeleton and build configuration
3. 🔄 Implement `GitHubClient` and provider classes
4. 🔄 Add operation mappings and warning logic
5. 🔄 Write unit and integration tests
6. 🔄 Update documentation and provider configuration reference
7. 🔄 Add GitHub provider to smoke test suite

## References

- [GitHub Releases API](https://docs.github.com/en/rest/releases/releases)
- [GitHub Repositories API](https://docs.github.com/en/rest/repos/repos)
- [Provider interfaces reference](https://beduality.github.io/terracotta/content/modules/core/reference/provider-interfaces.html)
- [Publishing to Multiple Providers](../docs/content/integration/tutorials/publishing-to-multiple-providers.md)
