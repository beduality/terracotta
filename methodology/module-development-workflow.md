---
description: Parent workflow that coordinates designing, contracting, testing, implementing, reviewing, documenting, and bug-fixing a Terracotta module.
---

# Module Development Workflow

A high-level workflow for building or significantly changing any `terracotta-*`
module. It coordinates focused child workflows and defines the order in which they
run.

For shared conventions, see `README.md` in this directory.

Main sequence: `System design → Contract → Tests → Implementation → Review → Documentation → Push to remote`

Bug-fix sequence: `Investigation → Tests → Implementation → Review → Documentation → Push`

Every change passes through **Review** and is pushed to **remote** before release.

---

## Phase 1: System design

**Goal**: define responsibilities, public API, and system fit.

**Read**: `project/methodology/module-system-design-workflow.md`

**Output before proceeding**:

- Design proposal in `project/proposals/<module>-design.md`.
- Scope: responsibilities, inputs, outputs, and side effects.
- Public API contract with Kotlin signatures or pseudo-code.
- Internal component boundaries and dependency rules.
- Error-handling and configuration strategy.

**Stop if**: the proposal has not been reviewed, or the module's responsibilities
overlap with an existing module.

---

## Phase 2: Contract

**Goal**: turn the approved design into a stable, KDoc-covered public interface before tests and implementation.

**Read**: `project/methodology/module-contract-workflow.md`

**Input from design phase**:

- Approved design proposal with public API sketch.
- Edge cases and error conditions.
- Module boundaries and dependency rules.

**Work**:

- Write or update public interfaces, abstract types, SPI entries, and data classes.
- Add KDoc for every public symbol intended for Dokka.
- Keep implementation details out of the contract.
- Run `:<module>:compileKotlin` to verify the contract compiles.

**Output before proceeding**:

- KDoc-covered contract source files.
- Passing `:<module>:compileKotlin`.

**Stop if**: the contract cannot be written without leaking implementation details, or the design needs revision. Return to Phase 1.

---

## Phase 3: Test-driven development

**Goal**: write executable specifications before production code exists.

**Read**: `project/methodology/module-testing-workflow.md`

**Input from contract phase**:

- KDoc-covered public API contract.
- Edge cases and error conditions.
- Module boundaries and seams for isolated testing.

**Output before proceeding**:

- Failing tests in `modules/<module>/src/test/kotlin/`.
- Confirmation that each test fails for the expected reason.

**Stop if**: tests are written against an unstable API, or if the tests reveal
that the design needs to change. In that case, return to Phase 1 or Phase 2.

---

## Phase 4: Implementation

**Goal**: make failing tests pass with a production-ready implementation.

**Read**: `project/methodology/module-implementation-workflow.md`

**Input from TDD phase**:

- Failing tests that describe behavior.
- A stable, KDoc-covered public API contract.

**Work**:

- Write the smallest implementation that turns tests green.
- Refactor for composability, modularity, extensibility, abstraction, and type safety.
- Document public API with KDoc for Dokka.
- Verify the full module build and quality checks.

**Output before proceeding**:

- Passing tests and a green `:build`.
- A clean public API with KDoc.
- Updated `build.gradle.kts` if new dependencies or tasks are needed.

**Stop if**: the implementation repeatedly forces API changes that invalidate
the design. Return to Phase 1 or Phase 2, then Phase 3.

---

## Phase 5: Review

**Goal**: validate the change against the review checklists.

**Read**: `project/methodology/module-review-workflow.md`

**Input from implementation phase**:

- Passing tests and a green `:build`.
- A clean public API with KDoc.

**Work**:

- Run the relevant automated review checks (tests, spotless, build, docs build).
- By default, **auto-review** is sufficient when all automated checks pass.
- Require human review when the change touches public API, build configuration, security-sensitive code, or spans multiple modules.
- Address feedback and re-run the relevant checks.

**Output before proceeding**:

- Approved change.
- Green CI.

**Stop if**: review reveals a design or implementation flaw. Return to the appropriate earlier phase.

---

## Phase 6: Documentation

**Goal**: publish user-facing guides and cross-linked API reference.

**Read**: `project/methodology/module-documentation-workflow.md`

**Input from review phase**:

- Approved change.
- Stable public API with KDoc.
- Behavior demonstrated by passing tests.

**Work**:

- Write Diátaxis docs under `docs/content/<module>/`.
- Wire docs into `mkdocs.yml`.
- Cross-link KDoc to user docs with `@see` tags.
- Deduplicate content from SDK or shared reference pages.
- Verify docs build and links work.

**Output before proceeding**:

- Merged user docs in `docs/content/<module>/`.
- Cross-linked KDoc.
- Passing `./gradlew :<module>:test :<module>:spotlessCheck` and `mkdocs build --strict`.

---

## Phase 7: Push to remote

**Goal**: share the approved change on the remote repository.

**Input from documentation phase**:

- Approved change.
- Built and verified docs.
- Green CI.

**Work**:

- Push the branch or merge the pull request to `main`.
- For small, low-risk, documentation-only changes, push directly to `main`.
- For changes that affect public API, build configuration, or multiple modules, open and merge a pull request.
- Verify the remote build passes after the push or merge.

**Output before considering the module complete**:

- Change available on remote `main`.
- Remote CI green.

**Stop if**: remote CI fails. Revert or fix-forward before continuing.

---

## Hand-off checklist

| From | To | Required artifact |
|------|----|-------------------|
| Design | Contract | Approved design proposal with public API sketch |
| Contract | Tests | KDoc-covered contract and passing `:<module>:compileKotlin` |
| Tests | Implementation | Failing tests exercising the public API contract |
| Implementation | Review | Passing tests and KDoc-covered public API |
| Investigation | Tests | Reproduced bug and regression test |
| Review | Docs | Approved change and green CI |
| Docs | Push | Built docs and green module checks |
| Push | Release | Change available on remote `main` and remote CI green |

---

## Scope guidance

Not every module change needs every phase.

| Change type | Phases needed |
|-------------|---------------|
| New module | Design → Contract → Tests → Implementation → Review → Docs → Push |
| Major feature | Design → Contract → Tests → Implementation → Review → Docs → Push |
| New public API or extension point | Design → Contract → Tests → Implementation → Review → Docs → Push |
| Bug fix | Investigation → Tests → Implementation → Review (→ Docs → Push if behavior changes) |
| Refactoring | Tests → Implementation → Review (→ Docs → Push if shared) |
| Documentation-only | Docs → Review → Push |

Insert **Contract** before Tests for bug fixes or refactorings that introduce a new public API surface or extension point.

---

## Review gates

Run `module-review-workflow.md` before moving forward. By default, **auto-review** is sufficient; escalate to human review for public API, build configuration, security-sensitive, or multi-module changes.

- **Design review** before contract.
- **Contract review** before tests.
- **Code review** before docs.
- **Documentation review** before push.
- **Merge / release review** before tagging or deploying.

Review feedback can loop a change back to an earlier phase.
