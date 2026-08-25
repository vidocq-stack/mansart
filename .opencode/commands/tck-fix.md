---
description: Take ONE failing TCK client and make it pass for real — read the test source first, implement the spec behaviour, never special-case the test.
agent: jpa-dev
---
Make this TCK client pass: $ARGUMENTS

Order of work, no shortcuts:

1. Read the TCK test source itself, from the sources jar (see the
   `mansart-jpa-tck` skill for its path). Read the failing METHOD, not the whole
   class. Say in one line what the test actually asserts.
2. If the intended behaviour is not obvious from the test, ask `@spec-reader` ONE
   precise question and apply what it returns.
3. Run the client once via `@tck-runner` to capture the current failure and its
   real stack trace.
4. Reproduce the failure as a unit test in `mansart-persistence-tests` — a small
   one, using our own entities, not the TCK's. This is the test you make green.
5. Implement the spec behaviour. Not the test: if your fix mentions the TCK's
   entity names, table names or class names, it is wrong and `@auditor` will
   reject it.
6. Re-run the client, then `/gate` to check for regressions.

Report the before/after numbers for that client, and whether the full-suite total
moved. If the client still fails, say exactly which assertion still fails and
what you now believe the cause is — do not weaken the implementation to move the
number.
