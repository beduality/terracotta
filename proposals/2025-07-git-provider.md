# Proposal: Add Git Provider

**Date**: 2025-07-12  
**Status**: Draft  
**Author**: Terracotta Team

## Summary

Add a `terracotta-provider-git` module that manages Git repository content, repository lifecycle, and Git metadata across Git hosting platforms. The provider is **not** a generic Git CLI wrapper: it uses platform REST APIs (starting with GitHub) to create and update repositories, commit files, manage tags, and sync repository metadata. It is designed to be used either as a standalone provider or as a reusable dependency for platform-specific release providers such as `terracotta-provider-github-release`. It does not handle platform-specific release concepts (e.g., GitHub Releases). All global repository metadata writes require explicit opt-in.

## Problem Statement

Many Terracotta projects already maintain their source code on GitHub, GitLab, or other Git hosts. Keeping repository metadata such as `README.md`, `LICENSE`, `CHANGELOG.md`, repository description, topics, and Git tags in sync with the Terracotta project model currently requires manual steps or custom CI scripts per platform. A first-class Git provider would let teams manage these files and tags from the same `terracotta.yml` configuration they use for Modrinth and Hangar, without requiring a platform-specific release.

## Goals

1. Allow users to manage Git repository metadata, content, and tags from a Terracotta run.
2. Keep repository files and metadata in sync where it maps cleanly (`project.name` → repository name, `project.summary` → repository description, `project.description` → `README.md`, `project.license` → `LICENSE`, `project.tags` → repository topics, `version.changelog` → `CHANGELOG.md`). All global repository metadata writes require explicit opt-in.
3. Refuse or warn for operations that do not fit the Git platform model instead of surprising users.
4. Reuse the existing provider SPI (`ProviderFactory`, `RegistryProvider`, etc.).
5. Be structured so that future backends (GitLab, Gitea) can be added without changing the user-facing configuration model.
6. Serve as a reusable dependency for platform-specific release providers such as `terracotta-provider-github-release`.

## Proposed Changes

### 1. Provider Identity

- **Provider ID**: `git`
- **Module**: `terracotta-provider-git`
- **Target platform**: Git hosting platforms. The initial implementation targets GitHub; the SPI is designed to support GitLab and Gitea in the future.
- **Platform config**: `platform` field (default `github`). The platform determines which REST API and authentication scheme are used.
- **Token source**: platform-specific environment variable (e.g., `GITHUB_TOKEN` for GitHub) or explicit `token` field.
- **Project ID**: repository in `owner/name` format.

### 2. Supported Operations

| Terracotta Operation | Git Mapping | Notes |
|---|---|---|
| `CreateProject` | Create or update repository | `repo.actions: ["create"]` creates the repository using `project.name` as the repo name. `repo.actions: ["update"]` updates repository settings. `repo.actions: ["destroy"]` deletes the repository and requires `confirm: true` plus an optional environment variable safeguard. |
| `UpdateMetadata` | Update repository description and `LICENSE` | `project.summary` maps to repository description only when `repoDescription` is enabled; `project.license` maps to the repo license and `LICENSE` file only when `license` is enabled. |
| `UpdateTags` | Update repository topics | `project.tags` map to repository topics only when `repoTopics` is enabled. |
| `UpdateDescription` | Update `README.md` | Writes `project.description` to `README.md` (or `readme.filename`) only when `readme` is enabled with a supported `create` or `update` action. Requires `git.name` and `git.email`. |
| `UploadVersion` | Create tag + update `CHANGELOG.md` | Creates the Git tag only when `git.tag.actions` includes `create`. Prepends `version.changelog` to `CHANGELOG.md` (or `changelog.filename`) only when `changelog` is enabled with the `update` action. Requires `git.name` and `git.email` for file writes. |
| `UploadGalleryItem` | Skip with warning | No native gallery concept in Git. |
| `UpdateGalleryItem` | Skip with warning | No native gallery concept in Git. |
| `DeleteGalleryItem` | Skip with warning | No native gallery concept in Git. |

### 3. Tag Strategy

Git tags are global repository objects. The provider can create a tag automatically when it does not exist, but only when `git.tag.actions` includes `create`. By default, `git.tag.actions` is empty and the provider expects the tag to already exist, failing fast if it is missing.

**Default configuration**:

```yaml
providers:
  git:
    projectId: "beduality/terracotta"
    git:
      tag:
        prefix: "v"
        actions: []
```

A version `1.2.3` produces tag `v1.2.3`. Users can set `git.tag.prefix` to `""` to use the raw version. Adding `actions: ["create"]` allows the provider to create the tag when it does not exist. Tag deletion is not triggered by any standard Terracotta operation and is left out of scope for now.

### 3.1 Tag Target Commit

When the provider creates the tag, it must know which Git commit the tag should point to. By default, the provider uses the current commit available in the CI environment (e.g., `GITHUB_SHA` or the checked-out HEAD). Users can override the target commit per version via provider-specific version configuration (the general mechanism for provider-specific version configuration is not yet implemented, but the Git provider will consume it once available).

**Example provider-specific version configuration**:

```yaml
versions:
  - version: "1.2.3"
    providers:
      git:
        gitCommit: "abc123def456"
```

**Gradle DSL**:

```kotlin
version {
    version.set("1.2.3")
    providers {
        git {
            gitCommit.set("abc123def456")
        }
    }
}
```

If the tag already exists, the provider does not move or recreate it; the `gitCommit` value is only used when the provider creates the tag.

### 4. Changelog File Updates

By default, the provider does not modify `CHANGELOG.md`. When `changelog` is enabled with the `update` action, the provider prepends the release's `version.changelog` to the configured changelog file. If the file does not exist, it is created.

### 5. Repository Metadata

- `project.name` → repository name (only when `repo` is enabled with `create` or `update` action).
- `project.summary` → repository description (only when `repoDescription` is enabled with `update` action).
- `project.description` → `README.md` contents (only when `readme` is enabled with `update` action).
- `project.license` → repository license and `LICENSE` file (only when `license` is enabled and the value is a known SPDX identifier). The default file name is `LICENSE`; use `license.filename` to override it. The `license.actions` field controls whether to `create`, `update`, or `destroy` the file.
- `project.tags` → repository topics (only when `repoTopics` is enabled with `update` action).
- `version.changelog` → `CHANGELOG.md` contents (only when `changelog` is enabled with `update` action). The provider prepends the release's changelog to the configured file; if the file does not exist, it is created.

### 6. Configuration

**YAML**:

```yaml
providers:
  git:
    projectId: "beduality/terracotta"
    platform: "github"
    git:
      tag:
        prefix: "v"
        actions: []
      name: "Terracotta"
      email: "terracotta@example.com"
    repo: false
    repoDescription: false
    license: false
    repoTopics: false
    readme: false
    changelog: false
```

**Gradle DSL**:

```kotlin
terracotta {
    providers {
        create("git") {
            projectId.set("beduality/terracotta")
            platform.set("github")
            git {
                tag {
                    prefix.set("v")
                    actions.set(emptyList())
                }
                name.set("Terracotta")
                email.set("terracotta@example.com")
            }
            repo.set(false)
            repoDescription.set(false)
            license.set(false)
            repoTopics.set(false)
            readme.set(false)
            changelog.set(false)
        }
    }
}
```

All repository-level metadata flags default to `false`. The provider never creates the repository, updates the description, license, topics, overwrites the README, or updates the changelog unless the corresponding flag is set to `true` or an object with the relevant `actions`. This keeps Terracotta runs safe for repositories that are already managed by hand or by other tooling.

### 6.1 Metadata Action Configuration

All repository-level metadata fields (`repo`, `repoDescription`, `license`, `repoTopics`, `readme`, `changelog`) can be set to either:

- `false` — disabled (default)
- `true` — enabled, allowing all supported actions with default settings
- an object with `actions` to limit which operations are allowed

For file-based metadata (`license`, `readme`, `changelog`), the object also accepts `filename`.

Supported actions per field:

- `repo`: `create`, `update`, `destroy`
- `repoDescription`: `update`, `destroy`
- `license`: `create`, `update`, `destroy`
- `repoTopics`: `update`, `destroy`
- `readme`: `create`, `update`, `destroy`
- `changelog`: `create`, `update`, `destroy`

**Object form examples**:

```yaml
providers:
  git:
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
    changelog:
      filename: "CHANGELOG.md"
      actions: ["update"]
```

`actions: true` is shorthand for all actions supported by the field.

### 6.2 Git Committer Configuration

File operations performed via the GitHub Contents API (for example, creating or updating `LICENSE`, `README.md`, and `CHANGELOG.md`) require a committer name and email. The provider exposes this through a `git` block.

```yaml
providers:
  git:
    git:
      name: "Terracotta"
      email: "terracotta@example.com"
```

**Gradle DSL**:

```kotlin
terracotta {
    providers {
        create("git") {
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

The provider uses a platform-specific backend. The initial backend targets the GitHub REST API via Ktor (consistent with Modrinth and Hangar clients); future backends will use GitLab and Gitea equivalents.

**Repository lifecycle** (GitHub backend):

- `GET /repos/{owner}/{repo}` to read repository metadata.
- `POST /orgs/{owner}/repos` or `POST /user/repos` to create the repository when `repo.actions` includes `create`.
- `PATCH /repos/{owner}/{repo}` to update repository settings when `repo.actions` includes `update`.
- `DELETE /repos/{owner}/{repo}` to delete the repository when `repo.actions` includes `destroy`.
- `PATCH /repos/{owner}/{repo}` to update the repository description when `repoDescription.actions` includes `update`.
- `PUT /repos/{owner}/{repo}/topics` to replace repository topics when `repoTopics.actions` includes `update`.

**File commits** (GitHub backend):

- `PUT /repos/{owner}/{repo}/contents/{filename}` to create or replace the license file when `license` is enabled, using the configured `git` committer.
- `DELETE /repos/{owner}/{repo}/contents/{filename}` to remove the license file when `license.actions` includes `destroy`, using the configured `git` committer.
- `PUT /repos/{owner}/{repo}/contents/{filename}` to create or replace the README when `readme` is enabled, using the configured `git` committer.
- `DELETE /repos/{owner}/{repo}/contents/{filename}` to remove the README when `readme.actions` includes `destroy`, using the configured `git` committer.
- `PUT /repos/{owner}/{repo}/contents/{filename}` to create or update the changelog file when `changelog` is enabled, prepending the release's changelog; uses the configured `git` committer.
- `DELETE /repos/{owner}/{repo}/contents/{filename}` to remove the changelog file when `changelog.actions` includes `destroy`, using the configured `git` committer.

**Tags** (GitHub backend):

- `POST /repos/{owner}/{repo}/git/refs` to create the tag reference when `git.tag.actions` includes `create`.

Authentication is platform-specific (e.g., `Authorization: Bearer {token}` for GitHub).

## Migration Path

1. Create `terracotta-provider-git` module with build configuration.
2. Implement `GitProviderFactory`, `GitStateProvider`, `GitRegistryProvider`, and optional `GitDestructiveRegistryProvider`.
3. Implement `GitClient` with endpoints above.
4. Map Terracotta operations as described, warning for unsupported ones.
5. Register the factory via `ServiceLoader` (`META-INF/services/io.github.beduality.terracotta.core.provider.ProviderFactory`).
6. Add unit tests with mocked GitHub API responses.
7. Update provider configuration reference and multi-provider tutorial.

## Benefits

1. **Single workflow**: Manage Git repository files and tags from the same `terracotta.yml` used for Modrinth and Hangar.
2. **Less CI glue**: No need for a separate GitHub Actions step to commit `README`, `LICENSE`, or `CHANGELOG` updates.
3. **Reusable dependency**: The Git provider can be reused by platform-specific release providers (such as `github-release`) for tags, file commits, and repository metadata. Declared provider dependencies ensure it runs before the dependent release provider.
4. **Consistent provider model**: Git fits the SPI even though the semantics differ from registries.

## Risks & Considerations

1. **Repository deletion is irreversible**: `repo.actions: ["destroy"]` deletes the entire repository, including issues, pull requests, releases, and wiki history. This is the most destructive action the provider supports and must be difficult to trigger accidentally.
   - **Mitigation**: Require `confirm: true` inside the `repo` object, log a prominent warning before execution, support dry-run mode in `terracottaPlan`, and optionally require an environment variable such as `TERRACOTTA_GIT_ALLOW_REPO_DELETE=true`. Refuse to run if confirmation is missing.

2. **Git tags are global**: Creating a tag on `UploadVersion` mutates the repository. Users may already have their own tagging workflow.
   - **Mitigation**: Require explicit opt-in via `git.tag.actions: ["create"]`. When `create` is not included, the provider fails fast if the required tag does not already exist, instead of creating it silently.

3. **Tag collisions**: If the tag already exists at a different commit, the provider cannot move it without potentially breaking other workflows.
   - **Mitigation**: Fetch the tag first and fail fast if it exists with a different SHA. Never move or recreate an existing tag.

4. **README writes are tempting but dangerous**: Updating `README.md` from `project.description` overwrites hand-written documentation.
   - **Mitigation**: Require explicit opt-in via `readme: true` or a `readme` object with `update` action; default to `false` and skip with a warning.

5. **Repository topics are global**: `UpdateTags` applies to the whole repo, not the version.
   - **Mitigation**: Require explicit opt-in via `repoTopics: true` or a `repoTopics` object with `update` action; default to `false` and skip with a warning.

6. **Changelog writes are global**: Updating `CHANGELOG.md` affects the whole repository history and may conflict with a project's own changelog workflow.
   - **Mitigation**: Require explicit opt-in via `changelog: true` or a `changelog` object with `update` action; default to `false` and skip with a warning. Clearly document that the provider prepends the release's changelog to the configured file.

7. **File commits are permanent**: Each `PUT` or `DELETE` via the Contents API creates a new commit on the default branch.
   - **Mitigation**: Log every file commit clearly, support dry-run mode in `terracottaPlan`, and require `git.name` / `git.email` so the committer is explicit.

8. **Platform APIs differ**: Repository description, topics, and repository creation APIs differ across GitHub, GitLab, and Gitea. A backend abstraction is required to keep the user-facing config portable.
   - **Mitigation**: Start with a GitHub backend and design the internal client interface so that GitLab/Gitea backends can be added without changing the YAML/DSL model. Document which features are supported per platform.

9. **Provider is configurable but opinionated with safe defaults**: Git repositories are typically managed by hand or by other tooling. The Terracotta Git provider is highly configurable, yet it ships with conservative defaults. All file writes, tag creation, and repository metadata writes are disabled by default. It should remain an optional provider. The module documentation must prominently state that the provider is optional and defaults to safe, minimal behavior.
   - **Mitigation**: Add an explicit note in `terracotta-provider-git` documentation and tutorials describing which Git concepts are mapped, which are skipped, and that users can opt in or ignore the provider entirely.

## Next Steps

1. ✅ Draft proposal
2. 🔄 Create module skeleton and build configuration
3. 🔄 Implement `GitClient` and provider classes
4. 🔄 Add operation mappings and warning logic
5. 🔄 Write unit and integration tests
6. 🔄 Update documentation and provider configuration reference
7. 🔄 Add Git provider to smoke test suite

## References

- [GitHub Git Database API](https://docs.github.com/en/rest/git)
- [GitHub Contents API](https://docs.github.com/en/rest/repos/contents)
- [GitHub Release Provider Proposal](2025-07-github-provider.md)
- [Provider Topological Sort Proposal](2025-07-provider-topological-sort.md)
- [Provider interfaces reference](https://beduality.github.io/terracotta/content/modules/core/reference/provider-interfaces.html)
- [Publishing to Multiple Providers](../docs/content/integration/tutorials/publishing-to-multiple-providers.md)
