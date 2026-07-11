---
description: The Terracotta module workflow methodology and shared conventions.
---

# Terracotta Workflow Methodology

This directory defines how Terracotta modules are designed, built, tested,
documented, reviewed, and fixed. It is a phase-gated, test-first methodology
intended for both human and AI-assisted development.

## What it is built from

This is not a brand-new methodology. It is a synthesis of established industry
practices:

| Workflow file | Established practice |
|---|---|
| `module-development-workflow.md` | Phase-gated development, design-first / API-first planning |
| `module-system-design-workflow.md` | Modular design, ports-and-adapters / clean architecture |
| `module-testing-workflow.md` | Test-driven development (TDD), behavior-driven testing |
| `module-implementation-workflow.md` | Refactoring, composability, type safety |
| `module-documentation-workflow.md` | Diátaxis documentation framework |
| `module-bugfix-workflow.md` | Root-cause analysis, regression testing, minimal fixes |
| `module-review-workflow.md` | Code review gates, merge / release review |

## What is distinctive here

The individual practices are well known; the packaging is project-specific:

- **Feature and bug-fix work follow different sequences.** New work flows through
  design, tests, implementation, review, and docs. Bugs start with investigation.
- **Investigation is a first-class phase for bug fixes.** Reproduction, root-cause
  isolation, hypothesis validation, and a regression test come before the fix.
- **Review is a mandatory gate**, not an optional checklist. Every change passes
  through design, code, documentation, and merge/release review before release.
- **Shared conventions are centralized** so each child workflow can stay focused
  on its phase.

## What is missing

If you want a full lifecycle methodology, these areas are not covered:

- **Release / version-bump workflow** — tagging, changelog finalization, artifact publishing.
- **Hotfix / critical-patch workflow** — when a bug must skip normal phases.
- **Incident response / post-mortem** — production failures and follow-up prevention.
- **Dependency update / security advisory workflow** — Renovate/Dependabot-style updates, CVE response.
- **API deprecation / migration workflow** — how to evolve or remove public APIs safely.
- **Spike / exploration workflow** — time-boxed research when the problem is not well understood.
- **Cross-module integration / release smoke testing** — end-to-end validation across modules.
- **Performance / observability workflow** — benchmarks, metrics, tracing.
- **Contributor onboarding / environment setup** — first-time setup for new developers.

## Highest-ROI extensions

If you want to extend this methodology, the most valuable additions would
probably be:

- `module-release-workflow.md` — version bump, changelog, publish.
- `module-hotfix-workflow.md` — critical-patch path.
- `module-spike-workflow.md` — exploratory work before design.

## How to use this directory

1. Read `module-development-workflow.md` to choose the right sequence for your
   change.
2. Follow the linked child workflow for the phase you are in.
3. Use this file for shared placeholders and commit / release conventions.

The child workflows are:

- `module-development-workflow.md` — parent workflow that coordinates the phases.
- `module-system-design-workflow.md` — design a module before writing tests or code.
- `module-testing-workflow.md` — write failing tests first.
- `module-implementation-workflow.md` — make tests pass with a production-ready implementation.
- `module-documentation-workflow.md` — write user docs and cross-link KDoc.
- `module-bugfix-workflow.md` — investigation and minimal-fix path for bugs.
- `module-review-workflow.md` — review checkpoints for design, code, docs, and merge/release.

## Shared placeholders

When applying any workflow, replace these placeholders consistently:

| Placeholder | Example value |
|---|---|
| `<module>` | `terracotta-core` |
| Module directory | `modules/<module>/` |
| Main source set | `modules/<module>/src/main/kotlin/` |
| Test source set | `modules/<module>/src/test/kotlin/` |
| Functional test source set | `modules/<module>/src/functionalTest/kotlin/` |
| Smoke test source set | `modules/<module>/src/smokeTest/kotlin/` |
| Build file | `modules/<module>/build.gradle.kts` |
| Docs directory | `docs/content/<module>/` |
| Design proposal | `project/proposals/<module>-design.md` |
| KDoc base URL | `https://beduality.github.io/terracotta/<module>/` |

Some workflows extend this list with phase-specific placeholders.

## Commit and release guidance

Stage changes for the files touched by the workflow, then commit with a clear summary:

```
feat(<module>): implement <behavior>
test(<module>): add tests for <behavior>
docs(<module>): add KDoc for public API
```

Choose the release path:

- Push directly to `main` for small, low-risk, documentation-only changes.
- Open a pull request for changes that affect public API, build configuration, or multiple modules.

## Related file patterns

- `modules/<module>/src/main/kotlin/` — production code.
- `modules/<module>/src/test/kotlin/` — unit and integration tests.
- `modules/<module>/src/functionalTest/kotlin/` — functional tests, when configured.
- `modules/<module>/src/smokeTest/kotlin/` — smoke tests, when configured.
- `modules/<module>/build.gradle.kts` — module build configuration.
- `gradle/libs.versions.toml` — shared dependency versions.
- `mkdocs.yml` — site navigation.
- `docs/content/<module>/` — user-facing documentation.
- `project/proposals/<module>-design.md` — design proposals.
