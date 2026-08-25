---
description: Start a work session — read STATUS.md, pick the next TODO card from TASKS.md, and announce the plan before touching anything.
agent: jpa-dev
---
Start a new work session on mansart-jakarta-persistence.

1. Read `mansart-jakarta-persistence/STATUS.md` — the `## Current focus` block and
   the last 3 lines of `## Session log` only. Do not read the whole file if it has
   grown.
2. Read `mansart-jakarta-persistence/TASKS.md` and pick the first card whose
   status is `TODO` and whose dependencies are all `DONE`. If $ARGUMENTS names a
   card id, use that one instead and say why it is safe to take out of order.
3. Load the `mansart-jpa` skill, and `mansart-jpa-tck` if the card names a TCK
   client.
4. Before writing any code, state in at most 12 lines:
   - the card id and its goal;
   - the failing test you will write FIRST, and its exact path;
   - the files you will touch (max 4);
   - the command that will prove the card is done.

Then stop and wait for nothing — proceed with step 4's plan, starting with the
failing test. Do not open a second card in this session.
