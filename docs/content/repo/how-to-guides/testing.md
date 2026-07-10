# Testing

This guide covers running and verifying tests in Terracotta.

## Test Types

Terracotta has two tiers of tests.

### Unit & Integration Tests

Run as part of the standard build, covering:

- Core business logic in `terracotta-core`
- Provider implementations
- Gradle plugin tasks

Do **not** require network access or external credentials.

#### Running

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew test
```

#### Test Organization

| Location | Tests for |
|----------|-----------|
| `modules/terracotta-core/src/test/` | Core modules |
| Provider modules | Provider-specific tests |
| `modules/terracotta-gradle-plugin/src/test/` | Gradle plugin |

#### Running Specific Tests

```bash
# Run tests for a specific module
./gradlew :terracotta-core:test

# Run tests with a specific tag
./gradlew test --tests "*Integration*"
```

### Smoke Tests

Verify Terracotta Gradle plugin integration against the live Modrinth API:

- End-to-end behavior
- Real network responses
- Authentication flows
- Actual command output

**Excluded from default test run** due to external API dependency.

#### Prerequisites

- Set `MODRINTH_TOKEN` environment variable to a valid Modrinth API token

#### Running

```bash
MODRINTH_TOKEN=<your-token> JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew terracottaSmokeTest
```

> Run smoke tests before submitting a PR that touches provider logic or anything affecting Modrinth API integration.

## Test Coverage

Terracotta uses JaCoCo for test coverage reporting.

### Generate Coverage Report

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew jacocoTestReport
```

### View HTML Reports

Reports are generated at:

- `modules/terracotta-core/build/reports/jacoco/test/html/index.html`
- `modules/terracotta-provider-modrinth/build/reports/jacoco/test/html/index.html`
- `modules/terracotta-gradle-plugin/build/reports/jacoco/test/html/index.html`

### Coverage Expectations

| Type | Expectation |
|------|-------------|
| Modified code | Maintain or improve coverage |
| Important logic paths | Covered by tests |
| Integration paths | Verified by smoke tests |

## Troubleshooting

### Tests Fail to Run

1. Ensure `JAVA_HOME` is set to JDK 21
2. Run `./gradlew clean test` to clear cached builds
3. Check network access if using external dependencies

### Low Coverage

- Run `jacocoTestReport` to view coverage details
- Focus on uncovered branches and edge cases
- Add tests for error paths

### Smoke Tests Failing

1. Verify `MODRINTH_TOKEN` is valid
2. Check Modrinth API status
3. Ensure project exists on Modrinth

## Best Practices

1. **Write tests first** for new behavior (TDD when appropriate)
2. **Run tests after changes** to catch regressions
3. **Add tests for edge cases** before implementing features
