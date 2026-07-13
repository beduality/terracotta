---
description: Stabilize gallery item identity via persisted state.
---

# Stabilize Gallery Item Identity via Persisted State

This plan follows `project/methodology/module-development-workflow.md` for a **major feature** that
touches `terracotta-core`, `terracotta-gradle-plugin`, `terracotta-state-filesystem`, and provider modules (`terracotta-provider-modrinth`, `terracotta-provider-hangar`).

## Source of truth

- TODO item: `Stabilize gallery item identity via persisted state` (`project/TODO.md`)
- Design proposal: `project/designs/26-07-12-gallery-item-identity.md`

## Progress summary

| Phase | Status | Notes |
|-------|--------|-------|
| Brainstorm | Skipped | Well-understood problem; no note needed |
| System design | Complete | Open questions resolved; design proposal updated |
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

- [x] Read `project/methodology/module-system-design-workflow.md`.
- [x] Read or write the design proposal.
- [x] Decide how to generate and store stable `localKey` values for gallery items from the local configuration: explicit `key` property, fallback to absolute `imagePath`.
- [x] Decide how `DiffEngine` receives persisted `ProviderState`: new overload accepting `Map<String, GalleryItemIdentity>`, with title/ordering fallback.
- [x] Decide how providers update persisted gallery identities: optional `GalleryIdentityReporter` interface, `ModrinthRegistryProvider` captures upload response URLs.
- [x] Decide how to handle title changes: update in place when identity matches; delete+upload only when identity is absent or key conflicts force fallback.
- [x] Complete design review checklist from `module-review-workflow.md`.
- [x] Update this plan with decisions that affect later phases.

## Phase 3: Contract

- [ ] Create a feature branch from `main` after the design proposal is approved.
- [ ] Read `project/methodology/module-contract-workflow.md`.
- [ ] Write or update public interfaces, abstract types, SPI entries, and data classes.
- [ ] Add KDoc for every public symbol intended for Dokka.
- [ ] Run `:terracotta-core:compileKotlin` and `:terracotta-gradle-plugin:compileKotlin`.

## Phase 4: Test-driven development

- [ ] Read `project/methodology/module-testing-workflow.md`.
- [ ] Identify behavior and edge cases:
  - Gallery item matched by stable local key even when title changes.
  - New item without persisted state matched by title or ordering fallback.
  - Deleted item removes its persisted identity.
  - Updated item persists new remote URL/ID.
  - State round-trips through `YamlStateCodec` correctly.
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
- [ ] Add or update Diátaxis docs covering persisted state and gallery identity.
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
  - Move the design proposal to `project/designs/archived/<design>.md` if it is no longer needed.
  - Move the brainstorm note to `project/brainstorm/archived/<note>.md` if one was created.
- [ ] Copy `project/reports/TEMPLATE.md` to `project/reports/release/<datetime>-<title>.md` and fill it out.

## Notes

<!-- Decisions, blockers, and discoveries go here as work progresses. -->
