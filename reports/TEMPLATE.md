---
description: Release report template for Terracotta.
---

# Release Report — <version> — <title>

## Summary

Brief description of what this release changes and why it matters.

## Verification

- [ ] Local build passes: `./gradlew spotlessCheck build`
- [ ] Docs build passes: `mkdocs build --strict`
- [ ] CI passes on the release PR or tag push
- [ ] CD completes and artifacts are reachable

## Released version links

- Documentation: https://beduality.github.io/terracotta/<version>/
- GitHub release: https://github.com/beduality/terracotta/releases/tag/v<version>
- Maven Central artifacts: https://central.sonatype.com/artifact/io.github.beduality/<module>/<version>
- Gradle Plugin Portal: https://plugins.gradle.org/plugin/io.github.beduality.terracotta/<version>
- CI/CD run: <link to the release workflow run>

## Follow-up notes

Anything to watch, revisit, or improve in the next release.
