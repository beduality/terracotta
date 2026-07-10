# Tech Stack

## Kotlin / JVM

The main codebase is written in **Kotlin 2.0.0** targeting JVM 17, compiled under **JDK 21**.

| Concern | Tool | How to run |
|---------|------|------------|
| Dependency management | [Gradle](https://gradle.org/) with version catalog (`gradle/libs.versions.toml`) | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew build` |
| Formatting & linting | [ktlint](https://pinterest.github.io/ktlint/) via [Spotless](https://github.com/diffplug/spotless) 6.25.0 | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew spotlessCheck` |
| Auto-fix formatting | ktlint via Spotless | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew spotlessApply` |
| API docs | [Dokka](https://github.com/Kotlin/dokka) 1.9.20 | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew dokkaHtml` |
| Testing | JUnit 5.10.2 | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew test` |

Kotlin does not have a separate type-checker step — type errors surface during compilation (`./gradlew build`).

Spotless must pass before a PR can be merged. Run `spotlessApply` to auto-fix formatting rather than fixing it by hand.

### Python

Python is used for the release process automation and documentation tooling. The minimum required version is **Python 3.13**. Dependencies are managed with [uv](https://docs.astral.sh/uv/).

| Concern | Tool |
|---------|------|
| Dependency management | [uv](https://docs.astral.sh/uv/) |
| Testing | [pytest](https://pytest.org/) |

There is no enforced formatter or linter for Python scripts at this time. Keep scripts readable and consistent with the existing style.
