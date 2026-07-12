---
description: Implementation plan for <short description>.
---

# <Title>

This plan follows `module-development-workflow.md` for a **<change type>** that
touches `<modules>`.

## Source of truth

- TODO item: `<text>` (`project/TODO.md`)
- Design proposal: `project/proposals/<proposal>.md` (if needed)

## Progress summary

| Phase | Status | Notes |
|-------|--------|-------|
| System design | Not started | |
| Contract | Not started | |
| TDD | Not started | |
| Implementation | Not started | |
| Review | Not started | |
| Documentation | Not started | |
| Push to remote | Not started | |

## Phase 1: System design

- [ ] Read `module-system-design-workflow.md`.
- [ ] Read or write the design proposal.
- [ ] Complete design review checklist from `module-review-workflow.md`.
- [ ] Update this plan with decisions that affect later phases.

## Phase 2: Contract

- [ ] Read `module-contract-workflow.md`.
- [ ] Write or update public interfaces, abstract types, SPI entries, and data classes.
- [ ] Add KDoc for every public symbol intended for Dokka.
- [ ] Run `:<module>:compileKotlin`.

## Phase 3: Test-driven development

- [ ] Read `module-testing-workflow.md`.
- [ ] Identify behavior and edge cases.
- [ ] Write failing tests against the contract.
- [ ] Run tests and confirm they fail for the expected reason.

## Phase 4: Implementation

- [ ] Read `module-implementation-workflow.md`.
- [ ] Make tests pass with the smallest change.
- [ ] Refactor and add KDoc.
- [ ] Run `:build :spotlessCheck`.

## Phase 5: Review

- [ ] Read `module-review-workflow.md`.
- [ ] Run code review checklist.
- [ ] By default, confirm auto-review (tests, spotless, build, docs build) passes.
- [ ] Escalate to human review for public API, build configuration, or multi-module changes if needed.
- [ ] Update `CHANGELOG.md` if users need to know about the change.

## Phase 6: Documentation

- [ ] Read `module-documentation-workflow.md`.
- [ ] Add or update Diátaxis docs.
- [ ] Cross-link KDoc with `@see`.
- [ ] Final verification: `./gradlew build :spotlessCheck` and `mkdocs build --strict`.

## Phase 7: Push to remote

- [ ] Push the branch or merge the pull request to `main`.
- [ ] Verify remote CI passes after the push or merge.

## Notes

<!-- Decisions, blockers, and discoveries go here as work progresses. -->
