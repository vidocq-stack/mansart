---
description: Session end — hand the real results to the tracker agent and stop
agent: persistence-dev
---
Close this session. Collect from THIS conversation's tool outputs only: task worked on,
files changed (`git status --short`), unit test results, TCK PASS/Total if run, full
build verdict if run, debt items touched. Then delegate to `@tracker`:
- update "Current focus" (current/next task);
- tick the checkbox ONLY if the validation gate was green (and, for TCK tasks, PASS
  increased) — otherwise write "IN PROGRESS" with the facts;
- add one session-log line (newest first) and, if the TCK ran, one scoreboard row;
- close/open PERSISTENCE-DEBT.md items as appropriate.
Report the tracker's `git diff --stat`. Do not commit unless I ask. Notes: $ARGUMENTS
