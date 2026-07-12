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
| TDD | Not started | |
| Implementation | Not started | |
| Build / quality checks | Not started | |
| Documentation | Not started | |
| Review / release prep | Not started | |

## Phase 1: System design

- [ ] Read or write the design proposal.
- [ ] Complete design review checklist from `module-review-workflow.md`.
- [ ] Update this plan with decisions that affect later phases.

## Phase 2: Test-driven development

- [ ] Identify behavior and edge cases.
- [ ] Write failing tests.
- [ ] Run tests and confirm they fail for the expected reason.

## Phase 3: Implementation

- [ ] Make tests pass with the smallest change.
- [ ] Refactor and add KDoc.
- [ ] Run `:build :spotlessCheck`.

## Phase 4: Documentation

- [ ] Add or update Diátaxis docs.
- [ ] Cross-link KDoc with `@see`.
- [ ] Run `mkdocs build --strict`.

## Phase 5: Review / release prep

- [ ] Run code review checklist.
- [ ] Update `CHANGELOG.md` if users need to know about the change.
- [ ] Final verification: `./gradlew build :spotlessCheck` and `mkdocs build --strict`.

## Notes

<!-- Decisions, blockers, and discoveries go here as work progresses. -->
