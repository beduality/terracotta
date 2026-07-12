---
description: Reusable workflow for diagnosing and fixing bugs in a Terracotta module.
---

# Module Bug-fix Workflow

A repeatable workflow for fixing bugs in any `terracotta-*` module. Focuses on
reproduction, root-cause analysis, and minimal fixes that do not change unrelated
behavior.

Typical path: Investigation → Tests → Implementation → Review.

## 1. Reproduce the bug

Before changing code, create a reliable reproduction:

- Convert the bug report into a failing test or minimal script.
- Confirm the failure on the current `main` / target branch.
- Record inputs, environment, and observed output.

If you cannot reproduce it, stop. A bug that cannot be reproduced cannot be
verified as fixed.

## 2. Isolate the root cause

Narrow down where the bug originates:

- Trace the code path with the failing test.
- Add logging or temporary assertions to inspect state.
- Use `git bisect`, `git log`, or `git blame` to find when it was introduced.

Form a clear hypothesis: "The bug happens because X under condition Y."

## 3. Write a regression test

Add a regression test that fails because of the bug:

- Place it next to existing tests for the affected behavior.
- Make it as small as possible while still reproducing the bug.
- Name it after the bug, not the fix.

Run it and confirm it fails for the expected reason.

## 4. Validate the hypothesis

Validate the hypothesis with the smallest possible change:

- Change one thing at a time.
- Re-run the regression test after each change.
- Refine the hypothesis if the test still fails.

Address the root cause, not symptoms.

## 5. Apply the minimal fix

Write the smallest change that makes the regression test pass without breaking
existing tests. Do not refactor unrelated code in the same commit. If the bug
requires a public API change, return to the system-design workflow first.

## 6. Verify the fix

Run the full module test suite and quality checks:

```bash
./gradlew :<module>:test :<module>:spotlessCheck
```

If `spotlessCheck` fails, run `:spotlessApply`, then review the diff. Also run
tests that exercise nearby behavior to catch regressions.

## 7. Document the fix

Update docs if the bug changed observable behavior or if users need to know
about the fix:

- Add a `CHANGELOG.md` entry under the unreleased version.
- Update relevant user docs if the fix affects a documented contract.

