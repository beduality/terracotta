---
description: Fix Hangar license mapping and stop licenseUrl from causing a recurring diff.
---

# Narrow Hangar License Integration

This plan follows `project/methodology/module-development-workflow.md` for a **bug fix** that
touches `terracotta-provider-hangar`, `terracotta-core`, and `docs`.

## Source of truth

- TODO item: "Add [Narrow License](./designs/26-07-12-narrow-license-hangar.md)" (`project/TODO.md`)
- Design proposal: `project/designs/26-07-12-narrow-license-hangar.md`

## Progress summary

| Phase | Status | Notes |
|-------|--------|-------|
| Phase 0: Investigation | Completed | Confirmed Hangar has no `licenseUrl` field; smallest SPI addition is a `ProviderLogic` capability. |
| Phase 1: Contract | Completed | Added `supportsLicenseUrl` to `ProviderLogic` with a default implementation. |
| Phase 2: Tests | Completed | Added `HangarLicenseMapperTest` and `DiffEngine` licenseUrl tests. |
| Phase 3: Implementation | Completed | Implemented mapper, wired provider/state, updated `DiffEngine` and Gradle tasks. |
| Phase 4: Review | Completed | Auto-review passed (`./gradlew build :spotlessCheck`, `mkdocs build --strict`). |
| Phase 5: Documentation | Completed | Updated Hangar tutorial, config schema, operations reference, and `CHANGELOG.md`. |
| Phase 6: Push and merge | Not started | Branch `feature/narrow-hangar-license` is ready to push. |
| Phase 7: Release report | Not started | Pending merge to `main`. |

## Phase 0: Investigation

- [x] Confirm Hangar API accepts the proposed license strings (`MIT`, `Apache 2.0`, `GPL`, `LGPL`, `AGPL`, `Other`).
- [x] Confirm there is no `licenseUrl` field on the Hangar project API.
- [x] Identify the smallest provider SPI addition needed to indicate `licenseUrl` support.
- [x] Decide how unknown/custom identifiers map to Hangar's `Other` option.

## Phase 1: Contract

- [x] Create a feature branch from `main` after the design proposal is approved.
- [x] Add a capability method (e.g. `supportsLicenseUrl`) to the provider SPI with a default implementation.
- [x] Define the `HangarLicenseMapper` interface/object contract.
- [x] Run `:<module>:compileKotlin` for affected modules.

## Phase 2: Tests

- [x] Read `module-testing-workflow.md`.
- [x] Add unit tests for `HangarLicenseMapper` covering MIT, Apache-2.0, GPL variants, LGPL variants, AGPL, CC0/Unlicense, and unknown identifiers.
- [x] Add diff-engine tests asserting that `licenseUrl` is ignored when the provider reports it is unsupported.
- [x] Run tests and confirm they fail for the expected reason.

## Phase 3: Implementation

- [x] Read `module-implementation-workflow.md`.
- [x] Implement `HangarLicenseMapper` and wire it into `HangarRegistryProvider` and `HangarStateProvider`.
- [x] Update `DiffEngine` to consult the provider capability before comparing `licenseUrl`.
- [x] Remove or silence the `licenseUrl` warning for unsupported providers.
- [x] Run `:build :spotlessCheck`.

## Phase 4: Review

- [x] Read `module-review-workflow.md`.
- [x] Run code review checklist.
- [x] Confirm auto-review (tests, spotless, build, docs build) passes.
- [x] Update `CHANGELOG.md` with the user-visible fix.

## Phase 5: Documentation

- [x] Read `module-documentation-workflow.md`.
- [x] Update `docs/content/modules/provider-hangar/tutorials/using-hangar.md` with license mapping and `licenseUrl` behavior.
- [x] Update `docs/content/modules/core/reference/config-schema.md` and `operations.md` as needed.
- [x] Cross-link KDoc with `@see` where appropriate.
- [x] Final verification: `./gradlew build :spotlessCheck` and `mkdocs build --strict`.

## Phase 6: Push and merge

- [ ] Push the branch and open a pull request to `main`.
- [ ] Review the PR; address feedback and re-run checks.
- [ ] Once CI is green, merge the pull request.
- [ ] Observe CD and confirm the deployment succeeds.

## Phase 7: Release report

- [ ] Archive project artifacts used for this work:
  - Move this plan to `project/plans/archived/<plan>.md`.
  - Move the design proposal to `project/designs/archived/<design>.md` if it is no longer needed.
  - Move the brainstorm note to `project/brainstorm/archived/<note>.md` if one was created.
- [ ] Copy `project/reports/TEMPLATE.md` to `project/reports/release/<datetime>-<title>.md` and fill it out.

## Notes

<!-- Decisions, blockers, and discoveries go here as work progresses. -->
