---
description: Close the session — hand the real numbers to the tracker, update STATUS.md and TASKS.md, and state whether the card is DONE.
agent: jpa-dev
---
Close this session.

1. State the card id, what actually changed, and the REAL numbers you measured
   this session (build result, unit `pass/total`, TCK `pass/total` if run). If you
   did not measure something, write `not measured` — do not carry forward the
   previous value.
2. Decide the card status: `DONE` only if `/gate` returned `GATE: PASS` in this
   session. Otherwise `WIP` with a one-line `blocked:` note naming the next
   concrete step.
3. Delegate to `@tracker` with exactly those facts, and let it write
   `STATUS.md` and `TASKS.md`. Do not write those files yourself.
4. If the session discovered work outside the card, tell `@tracker` to append new
   cards rather than growing the current one.
5. Finish with a commit: Conventional Commits, card id in the subject, signed
   off, `Co-Authored-By:` naming the local model. Do not push.

End with a 3-line handover: what the next session should open first, the one trap
it must know about, and the command that reproduces the current state.
