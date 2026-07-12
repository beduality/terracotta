# Proposal: Add Git Provider

**Date**: 2025-07-12  
**Status**: Draft  
**Author**: Terracotta Team

## Summary

Add a `terracotta-provider-git` module that performs **host-agnostic Git operations**: creating and pushing Git tags, committing files (`README.md`, `LICENSE`, `CHANGELOG.md`), and keeping the local repository in sync with a remote. It uses the Git CLI (or an equivalent portable Git library) rather than platform-specific REST APIs, so it works with any Git host that speaks the Git protocol (GitHub, GitLab, Bitbucket, Gitea, etc.).

It does **not** manage platform-specific repository metadata such as description, topics, homepage, visibility, or repository lifecycle. Those concerns belong to platform control providers such as `terracotta-provider-github`. It also does not publish GitHub Releases; that belongs to `terracotta-provider-github-release`.

## Problem Statement

Many Terracotta projects already maintain their source code on GitHub, GitLab, Bitbucket, or Gitea. Keeping `README.md`, `LICENSE`, `CHANGELOG.md`, and Git tags in sync with the Terracotta project model currently requires manual steps or custom CI scripts per host. A first-class Git provider would let teams manage these Git-level files and tags from the same `terracotta.yml` configuration they use for Modrinth and Hangar, without tying them to one hosting platform's control API.

## Goals

1. Allow users to manage Git tags and repository file content from a Terracotta run.
2. Keep repository files in sync where it maps cleanly (`project.description` → `README.md`, `project.license` → `LICENSE`, `version.changelog` → `CHANGELOG.md`). All writes require explicit opt-in.
3. Work across Git hosts via the Git protocol rather than per-platform REST APIs.
4. Refuse or warn for operations that do not fit the Git model instead of surprising users.
5. Reuse the existing provider SPI (`ProviderFactory`, `RegistryProvider`, etc.).
6. Serve as a reusable dependency for platform-specific release providers such as `terracotta-provider-github-release`.

## Non-Goals

- Repository lifecycle (create, update, delete repositories).
- Repository description, topics, homepage, visibility, or other platform-specific metadata.
- Platform-specific release distribution (e.g., GitHub Releases).
- A generic Git CLI wrapper unrelated to the Terracotta project model.

## Proposed Changes

### 1. Provider Identity

- **Provider ID**: `git`
- **Module**: `terracotta-provider-git`
- **Target platform**: Any Git host. Uses the Git protocol, not a platform REST API.
- **Project ID**: repository in `owner/name` format. Used to build the default remote URL when `remote` is not explicitly configured.
- **Authentication**: HTTPS with token or SSH key. The provider does not store credentials; it reads them from environment variables or explicit configuration.

### 2. Supported Operations

| Terracotta Operation | Git Mapping | Notes |
|---|---|---|
| `CreateProject` | Skip with warning | Repository lifecycle belongs to the platform control provider. |
| `UpdateMetadata` | Skip with warning | Platform metadata such as repository description belongs to the platform control provider. `LICENSE` file writes are handled by `UpdateDescription` or `UploadVersion` depending on configuration. |
| `UpdateTags` | Skip with warning | Repository topics belong to the platform control provider. |
| `UpdateDescription` | Update `README.md` | Writes `project.description` to `README.md` (or `readme.filename`) only when `readme` is enabled with a supported `create` or `update` action. Requires `git.name` and `git.email`. |
| `UploadVersion` | Create tag + update `CHANGELOG.md` | Creates the Git tag only when `git.tag.actions` includes `create`. Prepends `version.changelog` to `CHANGELOG.md` (or `changelog.filename`) only when `changelog` is enabled with the `update` action. Requires `git.name` and `git.email` for file writes. |
| `UploadGalleryItem` | Skip with warning | No native gallery concept in Git. |
| `UpdateGalleryItem` | Skip with warning | No native gallery concept in Git. |
| `DeleteGalleryItem` | Skip with warning | No native gallery concept in Git. |

### 3. Local Repository

The provider needs a local Git clone to operate. Behavior is controlled by the `checkout` configuration:

- `checkout.strategy`: `auto` (default), `reuse`, or `clone`.
  - `auto`: reuse the current working directory if it is already a Git clone pointing at the configured remote; otherwise clone to a temporary directory.
  - `reuse`: always use the current working directory. Fails if it is not a Git clone for the configured remote.
  - `clone`: always clone to a fresh temporary directory.
- `checkout.branch`: the branch to checkout. Defaults to the remote's default branch (`main`, `master`, etc.).

In CI environments the repository is usually already cloned, so `auto` or `reuse` avoids redundant work. In local runs or when isolation is required, `clone` keeps changes out of the user's working directory.

### 4. Tag Strategy

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

#### 4.1 Tag Target Commit

When the provider creates the tag, it must know which Git commit the tag should point to. By default, the provider uses `HEAD` of the checked-out branch. Users can override the target commit per version via provider-specific version configuration.

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

### 5. File Commit Behavior

File commits are performed on the configured branch using the configured committer identity.

#### 5.1 `README.md`

By default, the provider does not modify `README.md`. When `readme` is enabled with the `update` action, the provider writes `project.description` to the configured file and commits it. If the file does not exist, it is created.

#### 5.2 `LICENSE`

By default, the provider does not modify the license file. When `license` is enabled and the value is a known SPDX identifier, the provider writes the license text to the configured file and commits it. The `license.actions` field controls whether to `create`, `update`, or `destroy` the file.

#### 5.3 `CHANGELOG.md`

By default, the provider does not modify `CHANGELOG.md`. When `changelog` is enabled with the `update` action, the provider prepends the release's `version.changelog` to the configured file and commits it. If the file does not exist, it is created.

### 6. Configuration

**YAML**:

```yaml
providers:
  git:
    projectId: "beduality/terracotta"
    remote: "https://github.com/beduality/terracotta.git"
    checkout:
      strategy: "auto"
      branch: "main"
    auth:
      type: "token"
      token: "${GITHUB_TOKEN}"
    git:
      tag:
        prefix: "v"
        actions: []
      name: "Terracotta"
      email: "terracotta@example.com"
    license: false
    readme: false
    changelog: false
```

**Gradle DSL**:

```kotlin
terracotta {
    providers {
        create("git") {
            projectId.set("beduality/terracotta")
            remote.set("https://github.com/beduality/terracotta.git")
            checkout {
                strategy.set("auto")
                branch.set("main")
            }
            auth {
                type.set("token")
                token.set("\${GITHUB_TOKEN}")
            }
            git {
                tag {
                    prefix.set("v")
                    actions.set(emptyList())
                }
                name.set("Terracotta")
                email.set("terracotta@example.com")
            }
            license.set(false)
            readme.set(false)
            changelog.set(false)
        }
    }
}
```

All file and tag write flags default to `false`. The provider never overwrites the README, updates the changelog, creates the license file, or creates tags unless the corresponding flag or action is enabled.

### 6.1 Metadata Action Configuration

File-based metadata fields (`license`, `readme`, `changelog`) can be set to either:

- `false` — disabled (default)
- `true` — enabled, allowing all supported actions with default settings
- an object with `actions` to limit which operations are allowed

The object also accepts `filename`.

Supported actions per field:

- `license`: `create`, `update`, `destroy`
- `readme`: `create`, `update`, `destroy`
- `changelog`: `create`, `update`, `destroy`

**Object form examples**:

```yaml
providers:
  git:
    license:
      filename: "LICENSE"
      actions: ["create", "update"]
    readme:
      filename: "README.md"
      actions: ["create", "update"]
    changelog:
      filename: "CHANGELOG.md"
      actions: ["update"]
```

### 6.2 Authentication Configuration

The provider needs push access to the remote. Supported authentication schemes:

- `token`: HTTPS with a personal access token. The token is interpolated from `token` or from the environment variable referenced by `tokenEnv` (default `GIT_TOKEN`).
- `ssh`: SSH key authentication. The `privateKeyPath` points to an SSH private key; `passphrase` is optional.

For HTTPS with a token, the provider constructs the remote URL as `https://{token}@{host}/{owner}/{repo}.git` if `remote` is not explicitly configured.

### 6.3 Git Committer Configuration

File operations require a committer name and email. The provider exposes this through a `git` block.

- `git.name` (string, required for file commits) — committer name.
- `git.email` (string, required for file commits) — committer email.

If a file commit is attempted and `git.name` or `git.email` is missing, the provider fails with a clear error message.

### 7. Client Design

The provider executes Git commands through the Git CLI. The internal interface abstracts the following operations:

- `clone(remote, branch, directory)` — clone the remote repository.
- `checkout(branch)` — switch to the target branch.
- `pull()` — synchronize with the remote.
- `commit(files, message, committer)` — stage and commit the given files.
- `push()` — push commits and tags to the remote.
- `createTag(name, targetCommit)` — create an annotated or lightweight tag.
- `tagExists(name)` — check whether a tag already exists.

All commands log their arguments (with credentials redacted) and surface failures clearly. The provider runs commands idempotently where possible: if a file already has the desired content, no commit is created.

## Migration Path

1. Create `terracotta-provider-git` module with build configuration.
2. Implement `GitProviderFactory`, `GitStateProvider`, `GitRegistryProvider`, and optional `GitDestructiveRegistryProvider`.
3. Implement `GitClient` as a thin wrapper over the Git CLI.
4. Map Terracotta operations as described, warning for unsupported ones.
5. Register the factory via `ServiceLoader` (`META-INF/services/io.github.beduality.terracotta.core.provider.ProviderFactory`).
6. Add unit tests with mocked Git CLI responses and integration tests against a temporary local repository.
7. Update provider configuration reference and multi-provider tutorial.

## Benefits

1. **Single workflow**: Manage Git tags and repository files from the same `terracotta.yml` used for Modrinth and Hangar.
2. **Host portability**: Works with any Git host because it uses the Git protocol, not a platform-specific API.
3. **Less CI glue**: No need for a separate CI step to commit `README`, `LICENSE`, or `CHANGELOG` updates.
4. **Reusable dependency**: The Git provider can be reused by platform-specific release providers (such as `github-release`) for tag creation.
5. **Clear separation**: Repository control-plane concerns live in platform providers; pure Git operations live here.

## Risks & Considerations

1. **Git tags are global**: Creating a tag on `UploadVersion` mutates the repository. Users may already have their own tagging workflow.
   - **Mitigation**: Require explicit opt-in via `git.tag.actions: ["create"]`. When `create` is not included, the provider fails fast if the required tag does not already exist, instead of creating it silently.

2. **Tag collisions**: If the tag already exists at a different commit, the provider cannot move it without potentially breaking other workflows.
   - **Mitigation**: Fetch tags and fail fast if the tag exists with a different target. Never move or recreate an existing tag.

3. **README writes are tempting but dangerous**: Updating `README.md` from `project.description` overwrites hand-written documentation.
   - **Mitigation**: Require explicit opt-in via `readme: true` or a `readme` object with `update` action; default to `false` and skip with a warning.

4. **Changelog writes are global**: Updating `CHANGELOG.md` affects the whole repository history and may conflict with a project's own changelog workflow.
   - **Mitigation**: Require explicit opt-in via `changelog: true` or a `changelog` object with `update` action; default to `false` and skip with a warning. Clearly document that the provider prepends the release's changelog to the configured file.

5. **File commits are permanent**: Each commit becomes part of the repository history.
   - **Mitigation**: Log every commit clearly, support dry-run mode in `terracottaPlan`, and require `git.name` / `git.email` so the committer is explicit.

6. **Credential exposure**: Token-based HTTPS URLs and SSH keys must not be logged.
   - **Mitigation**: Redact tokens from all logs and error messages. Prefer reading tokens from environment variables or a credential helper rather than committed configuration files.

7. **Working directory conflicts**: Reusing the user's working directory may conflict with uncommitted changes.
   - **Mitigation**: Default to a clean state check. If uncommitted changes exist, fail fast unless `checkout.strategy` is `clone`. In `clone` mode, operate on a temporary directory so the user's workspace is untouched.

8. **SSH key availability in CI**: SSH keys are not always available in CI environments.
   - **Mitigation**: Default to token-based HTTPS. Document how to configure SSH for local development if desired.

9. **Provider is optional and conservative**: The Git provider is highly configurable but ships with safe defaults. All file writes and tag creation are disabled by default. Module documentation must state that the provider is optional and defaults to minimal behavior.
   - **Mitigation**: Add an explicit note in `terracotta-provider-git` documentation describing which Git concepts are mapped, which are skipped, and that users can opt in or ignore the provider entirely.

## Next Steps

1. ✅ Draft proposal
2. 🔄 Create module skeleton and build configuration
3. 🔄 Implement `GitClient` and provider classes
4. 🔄 Add operation mappings and warning logic
5. 🔄 Write unit and integration tests
6. 🔄 Update documentation and provider configuration reference
7. 🔄 Add Git provider to smoke test suite

## References

- [Git Provider Proposal](2025-07-git-provider.md)
- [GitHub Provider Proposal](2025-07-github-provider.md)
- [GitHub Release Provider Proposal](2025-07-github-release-provider.md)
- [Provider Topological Sort Proposal](2025-07-provider-topological-sort.md)
- [Provider interfaces reference](https://beduality.github.io/terracotta/content/modules/core/reference/provider-interfaces.html)
- [Publishing to Multiple Providers](../docs/content/integration/tutorials/publishing-to-multiple-providers.md)
