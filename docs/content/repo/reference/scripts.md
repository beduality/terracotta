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
| `uv run scripts/release.py extract-release-notes <version>` | Extracts the `## [version]` section from `CHANGELOG.md` for the GitHub release body. |

Common flags:

| Flag | Effect |
|---|---|
| `--bump auto\|patch\|minor\|major\|X.Y.Z` | Version bump strategy. |
| `--yes` | Skip confirmation prompts. |
| `--no-publish` | Do not publish to Maven Central. |
| `--no-push` | Do not push the tag or commit. |
| `--no-dry-run` | Skip the `./gradlew spotlessCheck build` verification. |

## Infrastructure

### `load-pulumi-secrets.py`

Syncs secrets from a local `.env` file into Pulumi config for the `terracotta-github` module.

```bash
export PULUMI_CONFIG_PASSPHRASE="..."
uv run scripts/load-pulumi-secrets.py
```

The script parses `modules/terracotta-github/src/.../App.kt` to detect which secrets are needed, so the secret list stays in one place.

## Documentation

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

Unit tests for `load-pulumi-secrets.py`.

### `redeploy_all_docs.test.py`

Unit tests for `redeploy_all_docs.py`.

### `conftest.py`

Shared pytest configuration and fixtures for the scripts test suite.
