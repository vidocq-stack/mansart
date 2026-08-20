---
description: Session start — read the tracker and debt backlog, pick ONE task for this session
agent: persistence-dev
---
Start a mansart-persistence session. Do exactly this, with short shell reads (never cat
whole files):

1. `sed -n '/## Current focus/,/## M7/p' PERSISTENCE-STATUS.md` and the first 6 lines
   after `## Session log`, plus the first 3 rows of `## TCK scoreboard`.
2. `grep -n '^- \[ \]' PERSISTENCE-DEBT.md | head -15` (open remediation items).
3. `git log --oneline -8` and `git status --short`.

Then propose ONE task for this session (either the tracker's current task or the
highest-leverage open debt item if it blocks TCK progress), state the acceptance
criterion (which test / TCK class must PASS), and wait for my go. Extra instructions:
$ARGUMENTS
