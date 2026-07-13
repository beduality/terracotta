---
description: The Terracotta module workflow methodology and shared conventions.
---

# Terracotta Workflow Methodology

This directory defines how Terracotta modules are designed, built, tested,
documented, reviewed, and fixed. It is a phase-gated, test-first methodology
intended for both human and AI-assisted development.

## What it is built from

Each workflow packages an established practice:

| Workflow file | Established practice |
|---|---|
| `guides/development.md` | Phase-gated development, design-first / API-first planning |
| `guides/system-design.md` | Modular design, ports-and-adapters / clean architecture |
| `guides/contract.md` | Interface-first / API contract writing with KDoc |
| `guides/testing.md` | Test-driven development (TDD), behavior-driven testing |
| `guides/implementation.md` | Refactoring, composability, type safety |
| `guides/documentation.md` | Diátaxis documentation framework |
| `guides/bugfix.md` | Root-cause analysis, regression testing, minimal fixes |
| `guides/review.md` | Code review gates, merge / release review |
| `guides/plan.md` | On-demand plans from TODO/backlog items with checkbox tracking |

## What is distinctive here

- **Features and bug fixes have different sequences.** New work flows through
  optional brainstorm, design, contract, tests, implementation, review, docs, push and merge, and release report; bugs start with investigation.
- **Investigation is a first-class phase for bug fixes.** Reproduce, isolate the
  root cause, validate the hypothesis, and add a regression test *before* fixing.
- **Contract is explicit.** Public interfaces are written with KDoc before tests
  and implementation, making the API surface reviewable early.
- **Review is a mandatory gate**, not an optional checklist, but human review is optional and used only when blocked on taste, direction, or high-stakes decisions.
- **Brainstorm is optional** and happens before System design when the direction is unclear.
- **Push and merge plus a release report** close out every change that is released.
- **Shared conventions are centralized** here so child workflows stay focused.

## What is missing

Not covered: release/version bumps, hotfixes, incident response, dependency/security
updates, API deprecation, spikes/exploration, cross-module integration, performance
and observability, and contributor onboarding.

Highest-ROI extensions to add next:

- `guides/release.md` — version bump, changelog, publish.
- `guides/hotfix.md` — critical-patch path.
- `guides/spike.md` — exploratory work before design.

## How to use this directory

1. If starting from `project/TODO.md` or `project/BACKLOG.md`, run
   `guides/plan.md` to create a focused, checkable plan.
2. Read `guides/development.md` to choose the right sequence.
3. Follow the linked child workflow for your current phase.
4. Use this file for shared placeholders and commit/release conventions.

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
| Design proposal | `project/designs/<module>-design.md` |
| Plan file | `project/plans/YYYY-MM-<short-name>-plan.md` |
| Brainstorm note | `project/brainstorm/<datetime>-<title>.md` |
| Release report | `reports/<datetime>-<title>.md` |
| Release report template | `project/methodology/templates/report.md` |
| Archived plans | `project/plans/archived/<plan>.md` |
| Archived designs | `project/designs/archived/<design>.md` |
| Archived brainstorm notes | `project/brainstorm/archived/<note>.md` |
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
- Open a pull request, review it, and merge once CI is green for changes that affect public API, build configuration, or multiple modules.
- Observe CD after merge and confirm the deployment succeeds.
- Archive the plan, design, and brainstorm note used for the work by moving them into `project/plans/archived/`, `project/designs/archived/`, and `project/brainstorm/archived/` respectively.
- Copy `project/methodology/templates/report.md` to `reports/<datetime>-<title>.md` and fill it out.
