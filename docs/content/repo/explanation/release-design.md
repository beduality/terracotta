# Release Design

Terracotta releases are fully automated through GitHub Actions. The design prioritizes safety, repeatability, and easy rollback.

## Why CI-driven releases?

Releasing from a local machine is error-prone. A maintainer might use the wrong JDK, forget to run tests, or push a tag before artifacts are ready. Running the release in CI guarantees a clean, reproducible environment and a single source of truth for the release artifact.

## Release safety

The `release.yml` workflow performs every step in one run:

1. Bump version and update `CHANGELOG.md`, `gradle.properties`, `pyproject.toml`, `deployments.json`, and `uv.lock`.
2. Verify the build with `./gradlew spotlessCheck build`.
3. Sign and publish artifacts to Maven Central.
4. Push the version tag only if publishing succeeds.
5. Create the GitHub release.
6. Deploy versioned documentation.

If Maven Central publishing fails, `release.py` rolls back the local commit and tag so the repository is not left in a half-released state.

## Version detection

`release.py` inspects conventional commits since the last tag to suggest a semver bump:

- A breaking change marker (`!` or `BREAKING CHANGE` footer) triggers a major bump.
- A `feat` commit triggers a minor bump.
- A `fix` commit triggers a patch bump.
- Otherwise it defaults to patch.

You can override this with the workflow `bump` input.

## Documentation coupling

Docs are deployed from the exact release tag, not from `main`. This ensures that the published API reference and guides match the released code. After a release, `deploy-docs.yml` deploys a versioned alias and updates `latest`.

## Rollback

If a release goes wrong after it has been published, recovery depends on what failed:

- **Tag or GitHub release is wrong**: Delete the tag and release, fix the issue, and re-run the workflow.
- **Maven Central published bad artifacts**: Maven Central publishes are irreversible. In this case, release a follow-up version rather than trying to unpublish.
- **Local dry run failed**: Use `release.py rollback` to revert uncommitted version bumps and tag changes.
