---
description: Fix Hangar license mapping and stop licenseUrl from causing a recurring diff.
---

# Narrow Hangar License Integration

This plan follows `module-development-workflow.md` for a **bug fix** that
touches `terracotta-provider-hangar`, `terracotta-core`, and `docs`.

## Source of truth

- Design proposal: `project/proposals/26-07-12-narrow-license-hangar.md`

## Progress summary

| Phase | Status | Notes |
|-------|--------|-------|
| Phase 0: Investigation | Not started | |
| Phase 1: Contract | Not started | |
| Phase 2: Tests | Not started | |
| Phase 3: Implementation | Not started | |
| Phase 4: Review | Not started | |
| Phase 5: Documentation | Not started | |
| Phase 6: Push to remote | Not started | |

## Phase 0: Investigation

- [ ] Confirm Hangar API accepts the proposed license strings (`MIT`, `Apache 2.0`, `GPL`, `LGPL`, `AGPL`, `Other`).
- [ ] Confirm there is no `licenseUrl` field on the Hangar project API.
- [ ] Identify the smallest provider SPI addition needed to indicate `licenseUrl` support.
- [ ] Decide how unknown/custom identifiers map to Hangar's `Other` option.

## Phase 1: Contract

- [ ] Add a capability method (e.g. `supportsLicenseUrl`) to the provider SPI with a default implementation.
- [ ] Define the `HangarLicenseMapper` interface/object contract.
- [ ] Run `:<module>:compileKotlin` for affected modules.

## Phase 2: Tests

- [ ] Read `module-testing-workflow.md`.
- [ ] Add unit tests for `HangarLicenseMapper` covering MIT, Apache-2.0, GPL variants, LGPL variants, AGPL, CC0/Unlicense, and unknown identifiers.
- [ ] Add diff-engine tests asserting that `licenseUrl` is ignored when the provider reports it is unsupported.
- [ ] Run tests and confirm they fail for the expected reason.

## Phase 3: Implementation

- [ ] Read `module-implementation-workflow.md`.
- [ ] Implement `HangarLicenseMapper` and wire it into `HangarRegistryProvider` and `HangarStateProvider`.
- [ ] Update `DiffEngine` to consult the provider capability before comparing `licenseUrl`.
- [ ] Remove or silence the `licenseUrl` warning for unsupported providers.
- [ ] Run `:build :spotlessCheck`.

## Phase 4: Review

- [ ] Read `module-review-workflow.md`.
- [ ] Run code review checklist.
- [ ] Confirm auto-review (tests, spotless, build, docs build) passes.
- [ ] Update `CHANGELOG.md` with the user-visible fix.

## Phase 5: Documentation

- [ ] Read `module-documentation-workflow.md`.
- [ ] Update `docs/content/modules/provider-hangar/tutorials/using-hangar.md` with license mapping and `licenseUrl` behavior.
- [ ] Update `docs/content/modules/core/reference/config-schema.md` and `operations.md` as needed.
- [ ] Cross-link KDoc with `@see` where appropriate.
- [ ] Final verification: `./gradlew build :spotlessCheck` and `mkdocs build --strict`.

## Phase 6: Push to remote

- [ ] Push the branch or merge the pull request to `main`.
- [ ] Verify remote CI passes after the push or merge.

## Notes

<!-- Decisions, blockers, and discoveries go here as work progresses. -->
