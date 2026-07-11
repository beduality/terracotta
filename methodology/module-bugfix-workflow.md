---
description: Reusable workflow for diagnosing and fixing bugs in a Terracotta module.
---

# Module Bug-fix Workflow

A repeatable workflow for fixing bugs in any `terracotta-*` module. It focuses on
reproduction, root-cause analysis, and minimal fixes that do not change unrelated
behavior.

This workflow runs inside the module development workflow. For bug fixes, the
typical path is: Investigation → Tests → Implementation → Review.

## 1. Reproduce the bug

Before changing code, create a reliable reproduction.

- Convert the bug report into a failing test or a minimal script.
- Confirm the failure happens on the current `main` / target branch.
- Record the exact inputs, environment, and observed output.

If you cannot reproduce it, stop. A bug that cannot be reproduced cannot be
verified as fixed.

## 2. Isolate the root cause

Narrow down where the bug originates.

- Use the failing test to trace the code path.
- Add logging or temporary assertions to inspect state.
- Use version control (`git bisect`, `git log`, `git blame`) to find when the bug
  was introduced.

Form a clear hypothesis: "The bug happens because X under condition Y."

## 3. Write a regression test

Add a test that fails because of the bug.

- Place it next to existing tests for the affected behavior.
- Make the test as small as possible while still reproducing the bug.
- Name the test so it describes the bug, not the fix.

Run the test and confirm it fails for the expected reason.

## 4. Validate the hypothesis

Test your root-cause hypothesis with the smallest possible change.

- Change one thing at a time.
- Re-run the regression test after each change.
- If the test still fails, refine the hypothesis and try again.

Avoid fixing symptoms. Address the root cause.

## 5. Apply the minimal fix

Write the smallest change that makes the regression test pass and does not
break existing tests.

- Do not refactor unrelated code in the same commit.
- Do not change public API unless the bug requires it; if it does, return to the
  system-design workflow first.

## 6. Verify the fix

Run the full module test suite and quality checks:

```bash
./gradlew :<module>:test :<module>:spotlessCheck
```

If `spotlessCheck` fails, run `:spotlessApply` to auto-fix formatting, then
review the diff.

Also run tests that exercise nearby behavior to catch regressions.

## 7. Document the fix

Update docs if the bug changed observable behavior or if users need to know
about the fix.

- Add a `CHANGELOG.md` entry under the unreleased version.
- Update relevant user docs if the fix affects a documented contract.

---

## Module placeholders

For shared placeholders, see `README.md` in this directory.

Phase-specific placeholders:

| Placeholder | Example value |
|-------------|---------------|
| Regression test | `modules/<module>/src/test/kotlin/<package>/<Bug>Test.kt` |

---

## Related files

- `README.md` — shared placeholders and commit/release guidance.
- `module-development-workflow.md` — parent workflow and scope guidance.
- `module-review-workflow.md` — review checkpoints before merging.
- `modules/<module>/src/test/kotlin/` — regression tests.
- `modules/<module>/src/main/kotlin/` — fixed implementation.
- `CHANGELOG.md` — release notes.
