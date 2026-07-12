---
description: Introduce a non-published terracotta-integration module that centralizes integration tests currently scattered across other modules.
---

# Terracotta Integration Module

## TL;DR

Add a `modules/terracotta-integration` module that is included in the build but not
published, similar to `terracotta-github`. Move cross-module integration tests—starting
with the Gradle TestKit suites from `terracotta-gradle-plugin`—into this module while
keeping isolated, single-subsystem unit tests in their home modules.

## Problem Statement

Today, integration tests live inside published modules. The Gradle plugin module, for
example, contains `TerracottaPluginIntegrationTest` and
`TerracottaPluginTaskIntegrationTest`, both of which spin up real Gradle builds via
TestKit and exercise the plugin end-to-end. These tests are slower, depend on multiple
modules, and blur the line between the published artifact and its verification.

Keeping integration tests inside published modules has several drawbacks:

- Published modules ship test code and test-only dependencies in their build graphs even
  though consumers never need them.
- Integration tests that touch multiple modules (e.g., plugin + state filesystem +
  providers) must pick a "host" module arbitrarily, which creates confusing ownership.
- CI cannot easily run a separate "integration" layer because the tests are mixed with
  unit tests in each module.
- Adding future integration scenarios (real provider sandboxes, Docker fixtures, etc.)
  further bloats the modules they happen to test.

A dedicated, non-published integration module follows the same pattern already
established by `terracotta-github` (internal Pulumi infrastructure) and keeps the test
surface clean.

## Goals

- Add a `modules/terracotta-integration/` Gradle module that is built but not published.
- Move existing integration tests from other modules into `terracotta-integration`,
  starting with the Gradle TestKit suites from `terracotta-gradle-plugin`.
- Ensure `terracotta-integration` can depend on all other modules it needs to exercise,
  including the Gradle plugin, core, state filesystem, and providers.
- Keep unit tests in their original modules; only move tests that exercise multiple
  subsystems or require runtime fixtures such as Gradle TestKit.
- Update `settings.gradle.kts` to include the new module.
- Update CI so that integration tests run as part of the standard `check` lifecycle or as
  a separately invokable Gradle task.
- Update `project/TODO.md` to link to this proposal.
- Add or update documentation that explains the module's purpose and distinguishes it
  from published modules.

## Non-Goals

- Moving mocked, fast unit tests (e.g., provider tests that use Ktor `MockEngine`) into
  the integration module. Those should remain in their current modules.
- Adding new integration test scenarios beyond the existing Gradle TestKit suites in
  the first iteration; new scenarios will be added later as needed.
- Converting the integration module into a published artifact or a standalone CLI.
- Replacing or removing `terracotta-github`; it remains the model for non-published
  modules.

## Proposed Design

### Module Layout

```
modules/terracotta-integration/
├── build.gradle.kts              # Non-published test-only module
└── src/
    └── test/
        └── kotlin/
            └── io/github/beduality/terracotta/integration/
                └── gradle/
                    ├── TerracottaPluginIntegrationTest.kt
                    └── TerracottaPluginTaskIntegrationTest.kt
```

The module intentionally has no `src/main/kotlin` source set. It exists only to compile
and run integration tests.

### Build Configuration

`modules/terracotta-integration/build.gradle.kts` applies the same Kotlin, Spotless,
and toolchain conventions as other modules, but intentionally omits `maven-publish`,
`signing`, and the `java-gradle-plugin` plugin:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spotless)
}

dependencies {
    testImplementation(project(":terracotta-core"))
    testImplementation(project(":terracotta-state-filesystem"))
    testImplementation(project(":terracotta-provider-modrinth"))
    testImplementation(project(":terracotta-provider-hangar"))
    testImplementation(project(":terracotta-gradle-plugin"))

    testImplementation(gradleTestKit())
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint()
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

### Module Inclusion

`settings.gradle.kts` gains one line for the new module, following the existing
`terracotta-github` pattern:

```kotlin
include("modules:terracotta-integration")
project(":modules:terracotta-integration").projectDir = file("modules/terracotta-integration")
```

### Test Relocation

Move the following files from `modules/terracotta-gradle-plugin/src/test/kotlin/...` to
`modules/terracotta-integration/src/test/kotlin/io/github/beduality/terracotta/integration/gradle/`:

- `TerracottaPluginIntegrationTest.kt`
- `TerracottaPluginTaskIntegrationTest.kt`

Any shared helpers that are only used by these tests (for example, classpath helpers or
project-file writers) move with them. Helpers that remain useful for the Gradle plugin's
unit tests stay in `terracotta-gradle-plugin`.

After the move, `modules/terracotta-gradle-plugin/src/test/kotlin/` keeps the
remaining detector tests and any future fast unit tests.

### CI / Gradle Lifecycle

The standard `./gradlew check` already runs all `Test` tasks, so the relocated tests will
execute automatically once the module is included. No CI change is required for the basic
move, but we may optionally add a dedicated job step later if integration tests become
slow enough to warrant a separate invocation.

## API Sketch

No public API is introduced. The only new surface is the Gradle module itself.

## Testing Strategy

- **Unit tests**: No new unit tests are added by this proposal; existing unit tests stay
  in place.
- **Integration tests**: The relocated Gradle TestKit suites continue to validate that
  the plugin applies, resolves `terracotta.yml`, and runs tasks against a real Gradle
  runtime.
- **Manual verification**: After moving the tests, run `./gradlew
  :modules:terracotta-integration:test` and confirm that both suites pass. Then run
  `./gradlew check` to ensure the full build still succeeds.

## Documentation Updates

- Add `docs/content/modules/terracotta-integration/README.md` explaining that the module is
  non-published and hosts cross-module integration tests.
- Update the module overview at `docs/content/modules/overview.md` to list
  `terracotta-integration` alongside `terracotta-github` as internal/non-published
  modules.
- Update `project/TODO.md` to link to this proposal.

## Open Questions

1. Should integration tests be tagged with JUnit tags (e.g., `@Tag("integration")`) so
   CI can run them selectively, or is the module boundary sufficient?
2. Should the new module depend on all providers proactively, or only on the ones the
   current tests need, adding dependencies later as scenarios expand?
3. Should we introduce a separate `integrationTest` source set or Gradle task in the
   future, or is the default `src/test` layout adequate for now?

## Risks

- **Risk**: Moving the Gradle TestKit tests changes their classpath setup because they
  will now depend on the plugin module rather than being inside it. **Mitigation**:
  Verify `plugin-under-test-metadata.properties` and the test classpath helpers still
  resolve correctly; use `gradleTestKit()` and the project dependency on
  `terracotta-gradle-plugin` to wire the plugin under test.
- **Risk**: Other modules may accidentally depend back on the integration module.
  **Mitigation**: Keep `terracotta-integration` leaf-only in the project graph; no
  published module should declare a dependency on it.
- **Risk**: CI time may increase if integration tests are run on every PR.
  **Mitigation**: The current TestKit tests already run as part of `check`, so no
  immediate change is expected; revisit if the suite grows.
