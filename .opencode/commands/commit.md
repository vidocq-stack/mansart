---
description: Update STATUS.md and TASKS.md with the real numbers, then commit — in that order, in one commit. The geste that replaces "git commit" so the tracker is never out of sync.
agent: jpa-dev
---
Commit the current work with the tracker guaranteed up to date. Order matters and
is the whole point: the state files are refreshed FIRST, then everything is
committed together.

Optional scope/subject hint from the user: $ARGUMENTS

1. **State the facts, measured — not guessed.** In three lines: the card id, what
   actually changed since the last commit, and the REAL numbers you observed this
   session (build result, unit `pass/total`, TCK `pass/total` if a suite ran). If
   you did not measure something this session, write `not measured` — never carry a
   previous value forward as if it were fresh.

2. **Delegate the state update to the tracker — emit a real `task` call.** Writing
   `@tracker` in prose does nothing (it is a naming convention, not a dispatch).
   Call the `task` tool with the `tracker` subagent, handing it exactly the facts
   from step 1, so it rewrites `STATUS.md`'s `## Current focus` block and the card
   line in `TASKS.md`. Do not edit those two files yourself.

3. **Verify the tracker actually wrote.** `git status --short` must now show
   `STATUS.md` and/or `TASKS.md` modified (unless nothing about the card's state
   changed — a pure refactor commit may legitimately leave them alone; say so if
   that is the case).

4. **Stage and commit everything together** — the code AND the refreshed state
   files, in one commit, so they can never drift apart:
   - Conventional Commits, with the card id in the subject, e.g.
     `feat(persistence): JP-11 EntityType concrete impl`.
   - Signed off (`-s`), and a `Co-Authored-By:` trailer naming the local model.
   - **Do not push.**

5. Report the commit hash and the one-line subject. Nothing else.

This is not `/session-end`: it does not close the session or hand off. Use it for
any clean commit mid-card. When the card is actually done, use `/gate` then
`/session-end`.
