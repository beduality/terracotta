---
description: Introduce a terracotta-gha module that provides reusable GitHub Actions workflows and a composite action for running Terracotta in CI/CD pipelines.
---

# Terracotta GHA Module

## TL;DR

Add a `terracotta-gha` module that publishes reusable GitHub Actions assets so users
can run `terracottaPlan` and `terracottaApply` from CI without writing their own
workflow YAML. The module will live alongside the existing Gradle plugin and
providers, and its docs will clearly distinguish it from the internal
`terracotta-github` Pulumi infrastructure module.

## Problem Statement

Today, Terracotta users who want to automate publishing from GitHub Actions must
write their own workflows. They need to:

- Install the correct JDK and Gradle versions.
- Inject provider tokens as repository or environment secrets.
- Decide whether to run `terracottaPlan` on pull requests and `terracottaApply` on
  releases.
- Handle edge cases such as `--force` for `terracottaDestroy` or `--dry-run` for
  validation.

This raises the barrier to adoption and leads to inconsistent, error-prone setups.
A first-party GHA module would give users a maintained, opinionated starting point
that tracks Terracotta releases.

In addition, the existing `terracotta-github` module is internal infrastructure
that manages this repository via Pulumi. The naming is easily confused with a
user-facing GitHub Actions module, so documentation must clarify the distinction.

## Goals

- Add a `terracotta-gha` Gradle module under `modules/terracotta-gha/`.
- Provide a composite GitHub Action that installs the Terracotta CLI / invokes the
  Gradle plugin with a consistent environment.
- Provide reusable workflow templates for common CI flows:
  - `plan.yml` — run `terracottaPlan` on pull requests and pushes.
  - `apply.yml` — run `terracottaApply` after a GitHub Release is published.
- Support passing provider tokens via repository or environment secrets using the
  existing `MODRINTH_TOKEN`, `HANGAR_TOKEN`, etc. convention.
- Allow users to pin to a released version of the workflows/action via Terracotta
  version tags.
- Update `project/TODO.md` to link to this proposal once it is created.
- Add documentation that explains the difference between `terracotta-github`
  (internal Pulumi) and `terracotta-gha` (user-facing CI assets).

## Non-Goals

- Hosting the action in a separate repository is out of scope initially; the
  module will be part of the main Terracotta monorepo.
- Replacing the Gradle plugin with a standalone CLI entrypoint is out of scope.
- Supporting providers other than Modrinth and Hangar in the first iteration is
  out of scope; new providers should work automatically once their Gradle plugin
  integration exists.
- Implementing a full web dashboard or status reporting UI is out of scope.
- Automating creation of GitHub repository secrets is out of scope; that belongs
  to the existing `terracotta-github` Pulumi module.

## Proposed Design

### Module Layout

```
modules/terracotta-gha/
├── build.gradle.kts              # Packages action files into the distribution
├── action.yml                    # Composite action entrypoint
└── workflows/
    ├── plan.yml
    └── apply.yml
```

The module is included in `settings.gradle.kts` but does not publish a JVM
artifact to Maven Central. Instead, its build packages the action and workflow
files for release alongside Terracotta binaries.

### Composite Action

`action.yml` exposes a small, stable interface:

```yaml
name: 'Terracotta'
description: 'Run Terracotta plan or apply in a GitHub Actions workflow'
inputs:
  command:
    description: 'Command to run: plan or apply'
    required: true
    default: 'plan'
  working-directory:
    description: 'Directory containing terracotta.yml / build.gradle.kts'
    required: false
    default: '.'
runs:
  using: 'composite'
  steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v4
      with:
        distribution: 'temurin'
        java-version: '21'
    - run: ./gradlew terracotta${{ inputs.command }}
      working-directory: ${{ inputs.working-directory }}
      shell: bash
      env:
        MODRINTH_TOKEN: ${{ env.MODRINTH_TOKEN }}
        HANGAR_TOKEN: ${{ env.HANGAR_TOKEN }}
```

### Reusable Workflows

`workflows/plan.yml` runs on `pull_request` and `push` events. It uses the
composite action with `command: plan` so contributors can see the semantic diff
before merging.

`workflows/apply.yml` runs on `release: published`. It uses `command: apply` to
publish versions and metadata after a GitHub Release is created.

Users consume the workflows like any other reusable workflow:

```yaml
jobs:
  terracotta-plan:
    uses: beduality/terracotta/.github/workflows/plan.yml@v0.4.0
    secrets:
      MODRINTH_TOKEN: ${{ secrets.MODRINTH_TOKEN }}
      HANGAR_TOKEN: ${{ secrets.HANGAR_TOKEN }}
```

### Gradle Plugin Integration

The composite action invokes `./gradlew terracotta<Command>`. This keeps the CI
logic thin and ensures that CI behavior matches local behavior. No new task code
is required in `terracotta-gradle-plugin` for the first iteration.

If the Gradle plugin later gains a `--ci` or `--json` output mode, the composite
action can pass those options without changing the workflow contract.

## API Sketch

```yaml
# action.yml
inputs:
  command:
    required: true
  working-directory:
    required: false
    default: '.'
```

```yaml
# Example consumer workflow in a user's repository
name: Release
on:
  release:
    types: [published]
jobs:
  publish:
    uses: beduality/terracotta/.github/workflows/apply.yml@v0.4.0
    secrets:
      MODRINTH_TOKEN: ${{ secrets.MODRINTH_TOKEN }}
      HANGAR_TOKEN: ${{ secrets.HANGAR_TOKEN }}
```

## Testing Strategy

- **Unit tests**: Verify that the action/workflow YAML is syntactically valid and
  that required inputs are declared.
- **Integration tests**: Run the composite action in a throwaway repository that
  contains a minimal Gradle project and a mock provider. Assert that the correct
  Gradle task is invoked with the expected environment variables.
- **Manual verification**: Create a test release on a fork and confirm that
  `terracottaApply` publishes metadata as expected.

## Documentation Updates

- Add `docs/content/modules/terracotta-gha/README.md` explaining how to consume
  the composite action and reusable workflows.
- Add a reference page listing workflow inputs, supported events, and required
  secrets.
- Update `docs/content/modules/terracotta-github/README.md` (or create it) to
  clarify that `terracotta-github` is internal Pulumi infrastructure, not the
  user-facing GitHub Actions module.
- Add a short section to the integration tutorial showing a complete CI/CD setup
  from `terracottaPlan` on PR to `terracottaApply` on release.

## Open Questions

1. Should the action pin a specific Terracotta/Gradle plugin version, or always
   use the version declared in the consumer's `build.gradle.kts`?
2. Should `terracotta-gha` publish a Docker-based action instead of a composite
   action for environments without Gradle pre-installed?
3. How should workflow versioning align with Terracotta release versioning when
   the workflows are in the same repository?

## Risks

- **Risk**: GitHub Actions reusable workflows cannot easily be versioned
  independently from the repository. **Mitigation**: Tag releases and document
  that consumers should pin to a released tag, not `main`.
- **Risk**: The composite action requires Gradle to be available in the consumer
  repository. **Mitigation**: Document the requirement and consider a wrapper-based
  action in a future iteration.
- **Risk**: Secrets handling differs between repository-level and
  environment-level secrets. **Mitigation**: Provide examples for both and keep the
  action secret-agnostic (passing them through `env`).
