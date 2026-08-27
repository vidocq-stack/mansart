---
description: Expand the next milestone from PLAN.md into JP cards in TASKS.md. This is planning, not coding — use it when the current milestone's cards are exhausted.
agent: jpa-dev
---
The current milestone's cards are (nearly) exhausted. Your job is to expand the
NEXT milestone from `PLAN.md` into concrete `JP` cards in `TASKS.md`. This is
planning, **not** implementation — do not write a single line of production code.

1. Read the next undeveloped milestone in `PLAN.md` — the first `M<n>` whose cards
   are not yet in `TASKS.md`. State its id, its scope, and its target TCK packages.

2. **Learn the real scope before inventing anything.** Delegate to `@spec-reader`
   via the `task` tool: ask it to list, from the TCK clients of those target
   packages (`ctx_search` on source `JPA32-TCK`, or `unzip -l | grep` as fallback),
   the distinct behaviours those clients actually exercise — one line each, grouped
   by theme. You want the executable truth of what the milestone must satisfy, not a
   guess from the one-line scope in PLAN.

3. Turn that into **atomic cards**, each following the template at the top of
   `TASKS.md`:
   - one behaviour, stated as a behaviour (not "write class X");
   - at most **4 files**;
   - exactly **one** proving test — a named TCK client, or a unit-test path;
   - explicit `deps` on earlier cards;
   - known traps in `notes` (schema, cross-module coverage, no-stub rule…).
   Order them so the most foundational come first (persist before merge, identity
   map before flush, a type before what depends on it). Size each for one focused
   session (~50k tokens). When in doubt, split — a card that is too big is the
   expensive mistake, because it costs every session that runs it.

4. Delegate the WRITE to `@tracker` via the `task` tool: it appends the cards under
   a new `## M<n> — <title>` section in `TASKS.md`, and bumps the milestone line in
   `STATUS.md`. Do not edit those two files yourself.

5. Report, and nothing else: the milestone id, how many cards you created, and the
   first three (id + one-line goal). Then STOP — the next `/next` picks up card one.

You are weaker at planning than at execution; that is expected. Compensate by
reading the tests before describing the cards, by keeping cards small, and by never
inventing a behaviour the TCK does not actually check. A human will review what you
produce before it is used.
