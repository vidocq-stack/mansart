---
description: Run the anti-drift audit on the current branch (stubs, reflection, TCK leakage, disabled tests, module violations).
agent: auditor
subtask: true
---
Audit `mansart-jakarta-persistence/` for drift.

Scope: $ARGUMENTS if given, otherwise `git diff main...HEAD` restricted to
`mansart-jakarta-persistence/`.

Report in your standard `VERDICT:` format. List what you grepped for even when
you find nothing, so the caller can distinguish a clean branch from an audit that
did not actually look.
