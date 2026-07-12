# Proposal: Add GitHub Provider

**Date**: 2025-07-12  
**Status**: Draft  
**Author**: Terracotta Team

## Summary

Add a `terracotta-provider-github` module that manages **GitHub-specific repository control-plane metadata**: repository description, homepage, topics, visibility, license, and repository lifecycle. It uses the GitHub REST API. It is intentionally separate from the host-agnostic `terracotta-provider-git` module (which handles Git tags and file commits) and from `terracotta-provider-github-release` (which publishes GitHub Releases).

## Problem Statement

Teams that host projects on GitHub often want to keep repository metadata in sync with the Terracotta project model: the GitHub description should match `project.summary`, topics should match `project.tags`, and the repository homepage should point to the project URL. Today these updates require a separate GitHub Actions step or manual edits. A first-class GitHub control provider would let teams manage this metadata from the same `terracotta.yml` they use for Modrinth and Hangar.

## Goals

1. Allow users to manage GitHub repository metadata and lifecycle from a Terracotta run.
2. Keep GitHub repository settings in sync where it maps cleanly (`project.name` → repository name, `project.summary` → description, `project.tags` → topics, `project.url` → homepage, `project.license` → repository license). All writes require explicit opt-in.
3. Refuse or warn for operations that do not fit the GitHub model instead of surprising users.
4. Reuse the existing provider SPI (`ProviderFactory`, `RegistryProvider`, etc.).
5. Remain separate from Git-level operations and GitHub Releases distribution.

## Non-Goals

- Git-level operations such as committing `README.md`, `LICENSE`, `CHANGELOG.md`, or creating Git tags. Those belong to `terracotta-provider-git`.
- Publishing GitHub Releases or uploading release assets. Those belong to `terracotta-provider-github-release`.
- Support for GitLab, Bitbucket, Gitea, or other Git hosts. Those would be separate platform control providers.

## Proposed Changes

### 1. Provider Identity

- **Provider ID**: `github`
- **Module**: `terracotta-provider-github`
- **Target platform**: GitHub only.
- **Token source**: `GITHUB_TOKEN` environment variable (or explicit `token` field).
- **Project ID**: GitHub repository in `owner/name` format.

### 2. Supported Operations

| Terracotta Operation | GitHub Mapping | Notes |
|---|---|---|
| `CreateProject` | Create or update repository | `repo.actions: ["create"]` creates the repository using `project.name` as the repo name. `repo.actions: ["update"]` updates repository settings. `repo.actions: ["destroy"]` deletes the repository and requires `confirm: true` plus an optional environment variable safeguard. |
| `UpdateMetadata` | Update repository description, homepage, and license metadata | `project.summary` → description when `description` is enabled; `project.url` → homepage when `homepage` is enabled; `project.license` → repository license metadata when `license` is enabled. |
| `UpdateTags` | Update repository topics | `project.tags` → topics only when `topics` is enabled. |
| `UpdateDescription` | Skip with warning | `README.md` content is handled by the `git` provider. |
| `UploadVersion` | Skip with warning | Git tags and `CHANGELOG.md` are handled by the `git` provider. |
| `UploadGalleryItem` | Skip with warning | No native gallery concept on GitHub at the repository level. |
| `UpdateGalleryItem` | Skip with warning | No native gallery concept. |
| `DeleteGalleryItem` | Skip with warning | No native gallery concept. |

### 3. Repository Metadata

- `project.name` → repository name (only when `repo` is enabled with `create` or `update` action).
- `project.summary` → repository description (only when `description` is enabled with `update` action).
- `project.url` → repository homepage (only when `homepage` is enabled with `update` action).
- `project.license` → repository license metadata (only when `license` is enabled and the value is a known SPDX identifier). This does not write the `LICENSE` file; file writes are handled by the `git` provider.
- `project.tags` → repository topics (only when `topics` is enabled with `update` action).

### 4. Configuration

**YAML**:

```yaml
providers:
  github:
    projectId: "beduality/terracotta"
    repo: false
    description: false
    homepage: false
    license: false
    topics: false
```

**Gradle DSL**:

```kotlin
terracotta {
    providers {
        create("github") {
            projectId.set("beduality/terracotta")
            repo.set(false)
            description.set(false)
            homepage.set(false)
            license.set(false)
            topics.set(false)
        }
    }
}
```

All repository-level metadata flags default to `false`. The provider never creates the repository, updates the description, homepage, license metadata, or topics unless the corresponding flag is set to `true` or an object with the relevant `actions`.

### 4.1 Metadata Action Configuration

Repository-level metadata fields (`repo`, `description`, `homepage`, `license`, `topics`) can be set to either:

- `false` — disabled (default)
- `true` — enabled, allowing all supported actions with default settings
- an object with `actions` to limit which operations are allowed

Supported actions per field:

- `repo`: `create`, `update`, `destroy`
- `description`: `update`, `destroy`
- `homepage`: `update`, `destroy`
- `license`: `update`, `destroy`
- `topics`: `update`, `destroy`

**Object form examples**:

```yaml
providers:
  github:
    repo:
      actions: ["create"]
    description:
      actions: ["update"]
    homepage:
      actions: ["update"]
    license:
      actions: ["update"]
    topics:
      actions: ["update"]
```

### 5. Client Design

Use the GitHub REST API via Ktor (consistent with Modrinth and Hangar clients):

- `GET /repos/{owner}/{repo}` to read repository metadata.
- `POST /orgs/{owner}/repos` or `POST /user/repos` to create the repository when `repo.actions` includes `create`.
- `PATCH /repos/{owner}/{repo}` to update repository settings (description, homepage, visibility, license) when the corresponding actions are enabled.
- `DELETE /repos/{owner}/{repo}` to delete the repository when `repo.actions` includes `destroy`.
- `PUT /repos/{owner}/{repo}/topics` to replace repository topics when `topics.actions` includes `update`.

Authentication via `Authorization: Bearer {token}`.

## Migration Path

1. Create `terracotta-provider-github` module with build configuration.
2. Implement `GitHubProviderFactory`, `GitHubStateProvider`, `GitHubRegistryProvider`, and optional `GitHubDestructiveRegistryProvider`.
3. Implement `GitHubClient` with the endpoints above.
4. Map Terracotta operations as described, warning for unsupported ones.
5. Register the factory via `ServiceLoader` (`META-INF/services/io.github.beduality.terracotta.core.provider.ProviderFactory`).
6. Add unit tests with mocked GitHub API responses.
7. Update provider configuration reference and multi-provider tutorial.

## Benefits

1. **Single workflow**: Manage GitHub repository metadata from the same `terracotta.yml` used for Modrinth and Hangar.
2. **Clear separation**: Git-level operations are handled by `terracotta-provider-git`; GitHub repository control is handled by `terracotta-provider-github`; GitHub Releases are handled by `terracotta-provider-github-release`.
3. **Consistent provider model**: GitHub fits the SPI even though the semantics differ from registries.
4. **Safe defaults**: All metadata writes and repository lifecycle operations are opt-in.

## Risks & Considerations

1. **Repository deletion is irreversible**: `repo.actions: ["destroy"]` deletes the entire repository, including issues, pull requests, releases, and wiki history. This is the most destructive action the provider supports and must be difficult to trigger accidentally.
   - **Mitigation**: Require `confirm: true` inside the `repo` object, log a prominent warning before execution, support dry-run mode in `terracottaPlan`, and optionally require an environment variable such as `TERRACOTTA_GITHUB_ALLOW_REPO_DELETE=true`. Refuse to run if confirmation is missing.

2. **Repository topics are global**: `UpdateTags` applies to the whole repo, not the version.
   - **Mitigation**: Require explicit opt-in via `topics: true` or a `topics` object with `update` action; default to `false` and skip with a warning.

3. **License metadata is distinct from the LICENSE file**: Updating the repository license via the GitHub API does not create or update the `LICENSE` file in the repository. Users who want both must enable the `git` provider's `license` setting as well.
   - **Mitigation**: Document the distinction clearly. Provide a combined example that uses both providers.

4. **Description and homepage overwrites are dangerous**: Overwriting a hand-written description or homepage URL can break project discovery.
   - **Mitigation**: Require explicit opt-in via `description` / `homepage`; default to `false` and skip with a warning.

5. **Provider is optional and conservative**: The GitHub control provider is highly configurable but ships with safe defaults. All metadata writes and repository lifecycle operations are disabled by default. Module documentation must state that the provider is optional and defaults to minimal behavior.
   - **Mitigation**: Add an explicit note in `terracotta-provider-github` documentation describing which GitHub concepts are mapped, which are skipped, and that users can opt in or ignore the provider entirely.

## Next Steps

1. ✅ Draft proposal
2. 🔄 Create module skeleton and build configuration
3. 🔄 Implement `GitHubClient` and provider classes
4. 🔄 Add operation mappings and warning logic
5. 🔄 Write unit and integration tests
6. 🔄 Update documentation and provider configuration reference
7. 🔄 Add GitHub provider to smoke test suite

## References

- [GitHub Repositories API](https://docs.github.com/en/rest/repos/repos)
- [Git Provider Proposal](2025-07-git-provider.md)
- [GitHub Release Provider Proposal](2025-07-github-release-provider.md)
- [Provider Topological Sort Proposal](2025-07-provider-toposort.md)
- [Provider interfaces reference](https://beduality.github.io/terracotta/content/modules/core/reference/provider-interfaces.html)
- [Publishing to Multiple Providers](../docs/content/integration/tutorials/publishing-to-multiple-providers.md)
