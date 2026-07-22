# Smoke Testing a Release

Use this checklist after a release to confirm the artifacts, registry state, and public sites are all correct end-to-end.

You can run most of these checks automatically with pytest:

```bash
# Core checks (fast, no local Gradle build)
uv run pytest scripts/test_release_smoke.py --release-version 0.1.2 -v

# Full suite including build and E2E (requires a local Gradle installation and JDK 21)
uv run pytest scripts/test_release_smoke.py --release-version 0.1.2 \
    --build-from-tag --gradle-e2e --sdk-e2e -v
```

## What the smoke tests verify

The pytest suite in `scripts/test_release_smoke.py` checks the release end-to-end, without you having to read the test file:

- **GitHub release**: per-module releases exist with the right tags and titles, and include JAR assets (main, sources, and javadoc) for each released module.
- **Changelog sync**: each GitHub release body reflects the `## [VERSION]` section in the corresponding module's `CHANGELOG.md`.
- **Maven Central presence**: every expected POM, JAR, sources, javadoc, signature, and Gradle module file is live for each released module plus the Gradle plugin marker.
- **Artifact integrity**: downloaded JARs contain the expected classes and the plugin descriptor.
- **Javadoc quality**: the `-javadoc.jar` files contain real documentation, not just a manifest.
- **Version-string consistency**: each module's release tag has the correct version in its `gradle.properties` and `CHANGELOG.md`.
- **Build from tag**: the project builds cleanly with `./gradlew spotlessCheck build` at the release tag.
- **Gradle plugin E2E**: the plugin resolves from Maven Central and registers the expected tasks in a clean project.
- **SDK E2E**: the core and Modrinth provider artifacts resolve and compile in a clean Kotlin/JVM project.
- **Documentation**: the docs homepage and key sub-pages return 200 and mention the release version.

## Saving results as metrics

Instead of keeping a manual results table in this file, save the pytest output to a JSON report and archive it:

```bash
uv run pytest scripts/test_release_smoke.py --release-version 0.1.2 \
    --json-report --json-report-file reports/smoke-0.1.2.json -v
```

You can commit the JSON reports under `reports/`, attach them to release notes, or store them as CI artifacts so past results can be reviewed without rerunning the suite.

## Before you start

- You will need the release version, e.g. `0.1.2`.
- Have `gh` authenticated, `git`, JDK 21, and a local Gradle installation for the optional E2E tests.

## Running the automated smoke tests

The pytest suite in `scripts/test_release_smoke.py` verifies the release automatically.

```bash
# Core checks (fast, no local Gradle build)
uv run pytest scripts/test_release_smoke.py --release-version 0.1.2 -v

# Full suite including build and E2E
uv run pytest scripts/test_release_smoke.py --release-version 0.1.2 \
    --build-from-tag --gradle-e2e --sdk-e2e -v
```

## Still manual

The script does not (and arguably cannot) check everything:

- **Visual review of the GitHub release page** — screenshots, formatting, asset names.
- **GPG signature trust** — the script checks signature packets, not that the key is yours/trusted.
- **Docs content accuracy** — it checks URLs return 200, not that the installation instructions or API docs are correct.
- **Broken internal links** — use `mkdocs build --strict` or a link checker for a thorough pass.
- **Rollback readiness** — know whether Maven Central already published and how to recover if needed.

## If a check fails

- Maven Central publishes are irreversible. If the GitHub release is wrong but Maven Central is correct, edit the release or re-tag manually.
- If the tag points to the wrong commit, run `uv run scripts/release.py rollback` locally and re-trigger the workflow after fixing the issue.

If the pytest suite passes and the manual items above look good, the release is fully verified.
