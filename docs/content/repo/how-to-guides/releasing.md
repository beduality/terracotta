# Releasing

This guide shows how to publish a new Terracotta release.

## Before you start

- Ensure the relevant `CHANGELOG.md` files have user-facing changes under `## [Unreleased]`. Module-specific changes go in `modules/<module>/CHANGELOG.md`; documentation changes go in `docs/CHANGELOG.md`; repo-wide changes go in the root `CHANGELOG.md`. See [Writing Changelog Entries](../how-to-guides/writing-changelog.md).
- Confirm the [CI workflow](../reference/ci-cd.md) is green on `main`.
- Verify release secrets are configured in the repository. See [CI/CD Reference](../reference/ci-cd.md) for the required secrets.

## 1. Choose a version bump

Releases follow [Semantic Versioning](https://semver.org/). The release script can detect the bump from conventional commits, or you can choose it explicitly.

| Trigger | When to use |
|---|---|
| `auto` | Let the script decide from commits since the last tag. |
| `patch` | Bug fixes only. |
| `minor` | New features, backward compatible. |
| `major` | Breaking changes. |
| `custom` | A specific version like `0.3.0`. |

## 2. Trigger the release workflow

Go to **Actions** → **Release** → **Run workflow**, then choose the bump strategy.

Or trigger from the CLI with the [GitHub CLI](https://cli.github.com/) authenticated:

```bash
uv run scripts/release.py trigger --bump auto --yes
```

To watch the run:

```bash
uv run scripts/release.py monitor
```

## 3. Verify the release

After the workflow succeeds:

1. Check that Maven Central has the new artifacts.
2. Review the GitHub release and JAR assets.
3. Confirm the versioned docs are live.

For a structured checklist, see [Smoke Testing a Release](../how-to-guides/smoke-testing-a-release.md).

## Dry run locally

To test the release logic without publishing or pushing:

```bash
uv run scripts/release.py release --dry-run --bump auto --yes
```

This computes version bumps and prints the planned changes without modifying files, building, or publishing.

## Roll back a failed release

If the workflow fails before publishing, `release.py` rolls back the version bump and tag automatically. For a manual rollback:

```bash
uv run scripts/release.py rollback
```

If Maven Central already published, you cannot unpublish. Release a follow-up version instead.

## Where to read more

- [Release Design](../explanation/release-design.md): Why releases are CI-driven and how rollback works.
- [Scripts Reference](../reference/scripts.md): All `release.py` commands and flags.
- [CI/CD Reference](../reference/ci-cd.md): Workflow inputs, secrets, and permissions.
