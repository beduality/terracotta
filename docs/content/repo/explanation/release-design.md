# Release Design

Terracotta releases are fully automated through GitHub Actions. The design prioritizes safety, repeatability, and easy rollback.

## Why CI-driven releases?

Releasing from a local machine is error-prone. A maintainer might use the wrong JDK, forget to run tests, or push a tag before artifacts are ready. Running the release in CI guarantees a clean, reproducible environment and a single source of truth for the release artifact.

## Release safety

The `release.yml` workflow performs every step in one run:

1. Detect changed modules by comparing each module's files against its last tag.
2. Bump version per module and update `modules/<module>/gradle.properties`, `modules/<module>/CHANGELOG.md`, and `deployments.json`.
3. Verify the build with `./gradlew spotlessCheck build` (includes downstream dependents).
4. Sign and publish artifacts to Maven Central per module.
5. Create per-module GitHub releases with JAR assets.
6. Push the release commit and per-module version tags.

If Maven Central publishing fails, `release.py` rolls back the local commit and tag so the repository is not left in a half-released state.

## Version detection

`release.py` inspects conventional commits since each module's last tag to suggest a semver bump per module:

- A breaking change marker (`!` or `BREAKING CHANGE` footer) triggers a major bump.
- A `feat` commit triggers a minor bump.
- A `fix` commit triggers a patch bump.
- Otherwise it defaults to patch.

You can override this with the workflow `bump` input.

## Documentation deployment

Docs are deployed by `deploy-docs.yml`, which triggers independently on pushes to `main` and version tags. This decouples documentation deployment from the release workflow — a release pushes tags and commits, and `deploy-docs.yml` picks up the tag push to deploy a versioned alias and update `latest`.

## Rollback

If a release goes wrong after it has been published, recovery depends on what failed:

- **Tag or GitHub release is wrong**: Delete the tag and release, fix the issue, and re-run the workflow.
- **Maven Central published bad artifacts**: Maven Central publishes are irreversible. In this case, release a follow-up version rather than trying to unpublish.
- **Local dry run failed**: Use `release.py rollback <module> <version>` to revert uncommitted version bumps and tag changes.
