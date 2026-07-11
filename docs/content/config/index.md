# Config

Terracotta is configured through a `terracotta.yml` file in your project root. The Gradle plugin reads this file and applies its values as defaults, which the Kotlin DSL in `build.gradle.kts` can override.

When a field is not set in either `terracotta.yml` or the Kotlin DSL, Terracotta attempts to auto-detect it from standard project files such as `README.md`, `LICENSE`, `fabric.mod.json`, and `paper-plugin.yml`.

- [Schema](schema.md) — complete reference for `terracotta.yml`
