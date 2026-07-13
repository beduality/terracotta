---
description: Parent workflow that coordinates designing, contracting, testing, implementing, reviewing, documenting, and bug-fixing a Terracotta module.
---

# Module Development Workflow

A high-level workflow for building or significantly changing any `terracotta-*`
module. It coordinates focused child workflows and defines the order in which they
run.

For shared conventions, see `README.md` in this directory.

Main sequence: `Brainstorm (optional) → System design → Contract → Tests → Implementation → Review → Documentation → Push and merge → Release report`

Bug-fix sequence: `Investigation → Tests → Implementation → Review (→ Documentation → Push and merge → Release report if behavior changes)`

Every change passes through **Review** and is pushed to **remote** before release.

---

## Phase 1: Brainstorm (optional)

**Goal**: explore alternatives, creative angles, and out-of-the-box ideas before committing to a design direction.

**When to skip**: the change is small, well-understood, or has a clear precedent.

**Read**: `project/methodology/guides/brainstorm.md`

**Work**:

- Open a fresh `project/brainstorm/<datetime>-<title>.md` from `project/methodology/templates/brainstorm.md`.
- Spend a short, time-boxed session generating ideas and trade-offs.
- Capture the best ideas, alternatives, and open questions succinctly.
- If the brainstorm produces a better direction, update the design proposal and plan before continuing.

**Output before proceeding**:

- A succinct brainstorm note, or a conscious decision to skip this phase.

**Stop if**: the brainstorm reveals the change is larger than expected. Return to planning or split the work.

---

## Phase 2: System design

**Goal**: define responsibilities, public API, and system fit.

**Read**: `system-design.md`

**Output before proceeding**:

- Design proposal in `project/designs/<module>-design.md`.
- Scope: responsibilities, inputs, outputs, and side effects.
- Public API contract with Kotlin signatures or pseudo-code.
- Internal component boundaries and dependency rules.
- Error-handling and configuration strategy.

**Stop if**: the proposal has not been reviewed, or the module's responsibilities
overlap with an existing module.

---

## Phase 3: Contract

**Goal**: turn the approved design into a stable, KDoc-covered public interface before tests and implementation.

**Read**: `contract.md`

**Input from design phase**:

- Approved design proposal with public API sketch.
- Edge cases and error conditions.
- Module boundaries and dependency rules.

**Work**:

- Create a feature branch from `main` after the design proposal is approved.
- Write or update public interfaces, abstract types, SPI entries, and data classes.
- Add KDoc for every public symbol intended for Dokka.
- Keep implementation details out of the contract.
- Run `:<module>:compileKotlin` to verify the contract compiles.

**Output before proceeding**:

- KDoc-covered contract source files.
- Passing `:<module>:compileKotlin`.

**Stop if**: the contract cannot be written without leaking implementation details, or the design needs revision. Return to Phase 2 (System design).

---

## Phase 4: Test-driven development

**Goal**: write executable specifications before production code exists.

**Read**: `testing.md`

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

## Phase 5: Implementation

**Goal**: make failing tests pass with a production-ready implementation.

**Read**: `implementation.md`

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

## Phase 6: Review

**Goal**: validate the change against the review checklists.

**Read**: `review.md`

**Input from implementation phase**:

- Passing tests and a green `:build`.
- A clean public API with KDoc.

**Work**:

- Run the relevant automated review checks (tests, spotless, build, docs build).
- By default, **auto-review** is sufficient when all automated checks pass.
- Optionally escalate to human review only when blocked on taste, direction, or high-stakes decisions (e.g., public API, build configuration, security-sensitive code, or multi-module changes).
- Address feedback and re-run the relevant checks.

**Output before proceeding**:

- Approved change.
- Green CI.

**Stop if**: review reveals a design or implementation flaw. Return to the appropriate earlier phase.

---

## Phase 7: Documentation

**Goal**: publish user-facing guides and cross-linked API reference.

**Read**: `documentation.md`

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

## Phase 8: Push and merge

**Goal**: share the approved change on the remote repository.

**Input from documentation phase**:

- Approved change.
- Built and verified docs.
- Green CI.

**Work**:

- Push the branch and open a pull request to `main`.
- Review the PR; address feedback and re-run checks.
- For small, low-risk, documentation-only changes, push directly to `main`.
- Once CI is green, merge the pull request.
- Observe CD and confirm the deployment succeeds.

**Output before proceeding**:

- Change available on remote `main`.
- Remote CI/CD green.

**Stop if**: remote CI or CD fails. Revert or fix-forward before continuing.

---

## Phase 9: Release report

**Goal**: clean up project artifacts and record the release outcome.

**Input from push and merge phase**:

- Change available on remote `main`.
- Green CI/CD.

**Work**:

- Archive the plan, design proposal, and brainstorm note used for this work by moving them into `project/plans/archived/`, `project/designs/archived/`, and `project/brainstorm/archived/` respectively.
- Copy `project/methodology/templates/report.md` to `reports/<datetime>-<title>.md` and fill it out with the released version links and verification results.

**Output before considering the module complete**:

- Project artifacts archived.
- Release report written.

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
| Docs | Push and merge | Built docs and green module checks |
| Push and merge | Release report | Change available on remote `main` and green CI/CD |
| Release report | Archive | Project artifacts archived and release report written |

---

## Scope guidance

Not every module change needs every phase.

| Change type | Phases needed |
|-------------|---------------|
| New module | Brainstorm? → Design → Contract → Tests → Implementation → Review → Docs → Push and merge → Release report |
| Major feature | Brainstorm? → Design → Contract → Tests → Implementation → Review → Docs → Push and merge → Release report |
| New public API or extension point | Brainstorm? → Design → Contract → Tests → Implementation → Review → Docs → Push and merge → Release report |
| Bug fix | Investigation → Tests → Implementation → Review (→ Docs → Push and merge → Release report if behavior changes) |
| Refactoring | Tests → Implementation → Review (→ Docs → Push and merge → Release report if shared) |
| Documentation-only | Docs → Review → Push and merge → Release report |

Insert **Contract** before Tests for bug fixes or refactorings that introduce a new public API surface or extension point.

---

## Review gates

Run `review.md` before moving forward. By default, **auto-review** is sufficient; escalate to human review only when blocked on taste, direction, or high-stakes decisions (e.g., public API, build configuration, security-sensitive, or multi-module changes).

- **Design review** before contract.
- **Contract review** before tests.
- **Code review** before docs.
- **Documentation review** before push.
- **Merge / release review** before tagging or deploying.

Review feedback can loop a change back to an earlier phase.
