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
| Push to `main` (filtered paths) | Deploys an `unreleased` version alias. If a per-module version tag points at the pushed commit, deploys a versioned release and updates the `latest` alias. |
| `workflow_dispatch` | Manual deployment. |

Steps:

1. Check out the target ref.
2. Configure GitHub Pages.
3. Set up JDK 21 and run Spotless check.
4. Generate Dokka multi-module documentation.
5. Set up `uv` and sync Python dependencies.
6. Deploy with `mike`:
   - Per-module version tag at HEAD: `mike deploy <version> latest` + `mike set-default latest`
   - No version tag: `mike deploy -t "Unreleased" unreleased`
7. Upload the `gh-pages` branch artifact and deploy to Pages.

### `release.yml`

Performs a per-module release from a workflow dispatch or automatically on push to `main` (filtered to `modules/**`, build files, and release scripts).

| Input | Type | Default | Purpose |
|---|---|---|---|
| `bump` | string | `auto` | Version bump strategy: `auto`, `patch`, `minor`, `major`, or a specific version like `0.9.0`. |
| `modules` | string | empty | Comma-separated modules to release (e.g. `terracotta-core,terracotta-provider-modrinth`). Skips change detection when specified. |

Steps:

1. Check out `main` with full history.
2. Set up JDK 21 and `uv`.
3. Run `release.py release --yes --publish --bump '<bump>' [--modules '<modules>']` to detect changed modules, bump versions, update changelogs and deployment manifest, build, publish to Maven Central, create GitHub releases, and push tags.
4. Push the release commit and tags.

The release script handles version extraction, changelog promotion, GitHub release creation, and JAR asset uploads internally. Docs are deployed separately by `deploy-docs.yml`, which triggers on pushes to `main` (filtered paths) and detects per-module version tags by checking `git tag --points-at HEAD`.

The release job is skipped when the head commit message starts with `chore: release` (release commits from the workflow itself) or contains `[skip release]` (manual opt-out for chore/fix commits that touch watched paths but don't warrant a release).

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
| `ci.yml` | `contents: read` (default; artifact upload needs no extra permissions) |
| `deploy-docs.yml` | `contents: write`, `pages: write`, `id-token: write` |
| `release.yml` | `contents: write` |
