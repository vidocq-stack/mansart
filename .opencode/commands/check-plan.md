---
description: Review the most recently expanded milestone's cards in TASKS.md and FIX scope/size/co-location/ordering defects. Run in a FRESH session after /plan-next — a separate reviewer pass catches what the author missed.
agent: jpa-dev
---
Review the cards of the most recently expanded milestone in `TASKS.md`. Not to
implement anything — to find and fix planning defects. Run this in a FRESH session,
on purpose: you review far better a plan you did not just write.

Approach it as if someone else wrote this plan and your job is to tear it apart.

1. Identify the milestone under review: the last `## M<n> — …` section in `TASKS.md`
   (the one just expanded by `/plan-next`). Read its cards, and read the `PLAN.md`
   milestone table so you know which theme belongs to which milestone.

2. Check every card against four rules, and **fix what fails — do not merely report**:

   **Scope (be hardest here — it is where the local model drifts most).** Does the
   card belong to THIS milestone? Locking/versioning → M8, relationship cascade → M5,
   advanced queries → M4, stored procedures → M9, however a TCK client mixes them.
   An out-of-scope card: have `@tracker` mark it `BLOCKED` with "defer to M<x>", or
   remove it and note the behaviour under the target milestone in `PLAN.md`.

   **Size.** A card whose proof is a whole TCK file, or whose goal is "all remaining
   methods" / "the entire X interface", is not one card. Have it split into several,
   one per group of behaviours.

   **Co-location.** Each card's test path must be in the `src/test` of the MODULE it
   tests (core code → `mansart-persistence-core/src/test`), never in
   `mansart-persistence-tests`, never in `src/main`. Fix wrong paths.

   **Order & deps.** Foundational first; every card names correct `deps`; no cycle.

3. When a behaviour's milestone is genuinely ambiguous, ask `@spec-reader` ONE precise
   question — what does the TCK actually test, and does it require a later feature? —
   and decide from the answer, not from a guess.

4. Delegate every edit to `@tracker` via the `task` tool: it rewrites `TASKS.md` (and
   `PLAN.md` if you deferred a behaviour to a later milestone). Do not edit those
   files yourself, and do not touch production code.

5. Report, and nothing else:
   - cards deferred (id → target milestone),
   - cards split (id → into how many),
   - test paths corrected,
   - anything still genuinely uncertain that a human should decide.

You are the second pair of eyes the plan's author lacked. This pass exists precisely
because a model reviewing its own fresh work is a poor judge; a fresh session judging
someone else's is a better one.
