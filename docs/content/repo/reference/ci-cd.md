# CI/CD Reference

Terracotta uses GitHub Actions for continuous integration, documentation deployment, and releases.

## Workflows

### `ci.yml`

Runs on every push and pull request.

| Job | What it does |
|---|---|
| `build` | Sets up JDK 21, runs `./gradlew spotlessCheck check build --no-daemon`, and uploads JAR artifacts. |

Artifacts are retained for 30 days.

### `deploy-docs.yml`

Builds and deploys the documentation site to GitHub Pages.

| Trigger | Behavior |
|---|---|
| Push to `main` | Deploys an `unreleased` version alias. |
| Version tag `v*` | Deploys a versioned release and updates the `latest` alias. |
| `workflow_call` | Called by `release.yml` after a release. |
| `workflow_dispatch` | Manual deployment with optional `ref` input. |

Steps:

1. Check out the target ref.
2. Configure GitHub Pages.
3. Set up JDK 21 and run Spotless check.
4. Generate Dokka multi-module documentation.
5. Set up `uv` and sync Python dependencies.
6. Deploy with `mike`:
   - Tagged release: `mike deploy <version> latest` + `mike set-default latest`
   - `main` push: `mike deploy -t "Unreleased" unreleased`
7. Upload the `gh-pages` branch artifact and deploy to Pages.

### `release.yml`

Performs a full release from a workflow dispatch.

| Input | Type | Default | Purpose |
|---|---|---|---|
| `bump` | choice | `auto` | Version bump strategy: `auto`, `patch`, `minor`, `major`, `custom`. |
| `version` | string | empty | Required only when `bump` is `custom`. |

Steps:

1. Check out `main` with full history.
2. Set up JDK 21 and `uv`.
3. Run `release.py --yes --publish` to bump, build, publish, and tag.
4. Read the released version from `gradle.properties`.
5. Extract release notes from the module's `CHANGELOG.md`.
6. Create a GitHub release with JAR artifacts.
7. Trigger `deploy-docs.yml` to publish versioned docs.

Required repository secrets:

| Secret | Purpose |
|---|---|
| `SONATYPE_USERNAME` | Sonatype Central Portal username |
| `SONATYPE_PASSWORD` | Sonatype Central Portal password or token |
| `SIGNING_KEY` | ASCII-armored GPG private key |
| `SIGNING_PASSWORD` | GPG key passphrase |

## Permissions

| Workflow | Required permissions |
|---|---|
| `ci.yml` | `contents: write` (for artifact upload) |
| `deploy-docs.yml` | `contents: write`, `pages: write`, `id-token: write` |
| `release.yml` | `contents: write`, `pages: write`, `id-token: write` |
