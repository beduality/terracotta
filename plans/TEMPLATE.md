---
description: Implementation plan for <short description>.
---

# <Title>

This plan follows `project/methodology/module-development-workflow.md` for a **<change type>** that
touches `<modules>`.

## Source of truth

- TODO item: `<text>` (`project/TODO.md`)
- Design proposal: `project/proposals/<proposal>.md` (if needed)

## Progress summary

| Phase | Status | Notes |
|-------|--------|-------|
| Brainstorm | Not started | Optional |
| System design | Not started | |
| Contract | Not started | |
| TDD | Not started | |
| Implementation | Not started | |
| Review | Not started | |
| Documentation | Not started | |
| Push and merge | Not started | |
| Release report | Not started | |

## Phase 1: Brainstorm

- [ ] Open a fresh `project/brainstorm/<datetime>-<title>.md` file.
- [ ] Spend a short, time-boxed session exploring alternatives, creative angles, and out-of-the-box ideas.
- [ ] Capture the best ideas, trade-offs, and open questions in the brainstorm file.
- [ ] If the brainstorm produces a better direction, update the design proposal and this plan before continuing.
- [ ] Keep the note succinct; the goal is sharper ideas, not a design document.

## Phase 2: System design

- [ ] Read `project/methodology/module-system-design-workflow.md`.
- [ ] Read or write the design proposal.
- [ ] Complete design review checklist from `module-review-workflow.md`.
- [ ] Update this plan with decisions that affect later phases.

## Phase 3: Contract

- [ ] Read `project/methodology/module-contract-workflow.md`.
- [ ] Write or update public interfaces, abstract types, SPI entries, and data classes.
- [ ] Add KDoc for every public symbol intended for Dokka.
- [ ] Run `:<module>:compileKotlin`.

## Phase 4: Test-driven development

- [ ] Read `project/methodology/module-testing-workflow.md`.
- [ ] Identify behavior and edge cases.
- [ ] Write failing tests against the contract.
- [ ] Run tests and confirm they fail for the expected reason.

## Phase 5: Implementation

- [ ] Read `project/methodology/module-implementation-workflow.md`.
- [ ] Make tests pass with the smallest change.
- [ ] Refactor and add KDoc.
- [ ] Run `:build :spotlessCheck`.

## Phase 6: Review

- [ ] Read `project/methodology/module-review-workflow.md`.
- [ ] Run code review checklist.
- [ ] By default, confirm auto-review (tests, spotless, build, docs build) passes.
- [ ] Update `CHANGELOG.md` if users need to know about the change.
- [ ] Optional: escalate to human review only when blocked on taste, direction, or high-stakes decisions.

## Phase 7: Documentation

- [ ] Read `project/methodology/module-documentation-workflow.md`.
- [ ] Add or update Diátaxis docs.
- [ ] Cross-link KDoc with `@see`.
- [ ] Final verification: `./gradlew build :spotlessCheck` and `mkdocs build --strict`.

## Phase 8: Push and merge

- [ ] Push the branch and open a pull request to `main`.
- [ ] Review the PR; address feedback and re-run checks.
- [ ] Once CI is green, merge the pull request.
- [ ] Observe CD and confirm the deployment succeeds.

## Phase 9: Release report

- [ ] Archive project artifacts used for this work:
  - Move this plan to `project/plans/archived/<plan>.md`.
  - Move the design proposal to `project/proposals/archived/<proposal>.md` if it is no longer needed.
  - Move the brainstorm note to `project/brainstorm/archived/<note>.md` if one was created.
- [ ] Copy `reports/release/TEMPLATE.md` to `project/reports/release/<datetime>-<title>.md` and fill it out.

## Notes

<!-- Decisions, blockers, and discoveries go here as work progresses. -->
