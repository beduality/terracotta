# Scripts Reference

Terracotta uses Python scripts for release automation, infrastructure management, and documentation tasks. All scripts are in `scripts/` and are run with `uv run`.

## Release automation

### `release.py`

End-to-end release orchestration. Used locally and by the [release.yml](ci-cd.md) GitHub Actions workflow.

| Command | Purpose |
|---|---|
| `uv run scripts/release.py` | Interactive wizard. Detects version bump, updates changelog and version files, builds, optionally publishes and pushes. |
| `uv run scripts/release.py trigger` | Triggers the `release.yml` workflow via `gh workflow run`. |
| `uv run scripts/release.py monitor [RUN_ID]` | Watches a release workflow run. |
| `uv run scripts/release.py rollback` | Reverts the last release commit and tag. |
| `uv run scripts/release.py extract-release-notes <module> <version>` | Extracts the `## [tag]` section from the module's `CHANGELOG.md` for the GitHub release body. |

Common flags:

| Flag | Effect |
|---|---|
| `--bump auto\|patch\|minor\|major\|X.Y.Z` | Version bump strategy. |
| `--modules module1,module2` | Comma-separated list of modules to release (skips change detection). |
| `--dry-run` | Compute versions and print planned changes without modifying files or building. |
| `--yes` | Skip confirmation prompts. |
| `--no-publish` | Do not publish to Maven Central. |
| `--no-push` | Do not push the tag or commit. |
| `--since <ref>` | Git ref to use as the change-detection baseline. |

## Infrastructure

### `load_pulumi_secrets.py`

Syncs secrets from a local `.env` file into Pulumi config for the `terracotta-github` module.

```bash
export PULUMI_CONFIG_PASSPHRASE="..."
uv run scripts/load_pulumi_secrets.py
```

The script parses `modules/terracotta-github/src/.../App.kt` to detect which secrets are needed, so the secret list stays in one place.

## Documentation

### `deployments.py`

Manages the `deployments.json` manifest that drives the docs [Last Changes](../../../last-changes.md) page. Parses module changelogs to extract structured metadata (title, summary, modules) for each version. Supports both versioned entries (module releases) and versionless entries (infrastructure applies, routine deployments).

Called automatically by `release.py` during each release to append deployment entries.

### `redeploy_all_docs.py`

Rebuilds and redeploys every versioned docs release using `mike`.

```bash
uv run python scripts/redeploy_all_docs.py
```

Use this when templates, hooks, or `mkdocs.yml` change and you want the new layout applied to historical versions.

### `add_kdoc_links.py`

Build hook that copies generated Dokka output into the docs site. Not usually invoked directly.

## Test helpers

### `test_release_smoke.py`

Pytest suite for smoke testing a release end-to-end. See [Smoke Testing a Release](../how-to-guides/smoke-testing-a-release.md).

### `test_load_pulumi_secrets.py`

Unit tests for `load_pulumi_secrets.py`.

### `test_deployments.py`

Unit tests for `deployments.py`.

### `redeploy_all_docs.test.py`

Unit tests for `redeploy_all_docs.py`.

### `conftest.py`

Shared pytest configuration and fixtures for the scripts test suite.
