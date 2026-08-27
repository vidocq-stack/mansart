---
description: Expand the next milestone from PLAN.md into JP cards in TASKS.md. Planning, not coding — use it when the current milestone's cards are exhausted. Its output is human-reviewed.
agent: jpa-dev
---
The current milestone's cards are (nearly) exhausted. Expand the NEXT milestone from
`PLAN.md` into concrete `JP` cards in `TASKS.md`. This is planning, **not**
implementation — do not write a single line of production code.

1. Read the next undeveloped milestone in `PLAN.md` — the first `M<n>` whose cards
   are not yet in `TASKS.md`. State its id, its scope, and its target TCK packages.
   Also read the WHOLE milestone table: you need to know which theme belongs to
   which later milestone (see rule A below).

2. **Learn the real scope before inventing anything.** Delegate to `@spec-reader`
   via the `task` tool: from the TCK clients of the target packages (`ctx_search` on
   source `JPA32-TCK`), list the distinct behaviours they exercise, one line each,
   grouped by theme, with the client + test-method names.

3. Turn that into **atomic cards** using the template at the top of `TASKS.md`. Four
   rules, learned the hard way — a card that breaks one of them wastes every session
   that runs it:

   **A. Stay inside THIS milestone.** TCK clients mix themes: a `persist` client may
   also touch queries, locks, or relationships. Those belong to *later* milestones
   (queries → M4, relationships/cascade → M5, locking/versioning → M8 — check the
   PLAN table). If a behaviour belongs to a later milestone, **do not make a card for
   it here.** It will be covered there. Cover only this milestone's scope.

   **B. No monster cards.** A card whose proof is a whole TCK file, or whose goal is
   "implement the entire X interface", is not a card — it is an admission you did not
   split. If a client has more than ~8 test methods, break it into several cards, one
   per group of behaviours.

   **C. Tests co-located.** A card's test goes in the `src/test` of the MODULE it
   tests — a unit test for `core` code goes in `mansart-persistence-core/src/test`,
   never in `mansart-persistence-tests`, and never in `src/main`. Test-entity
   fixtures also live in `src/test`. (`mansart-persistence-tests` is only for
   cross-module Arquillian/integration.) This is what keeps Sonar coverage honest.

   **D. Order and deps.** Foundational first (identity map before persist, persist
   before merge, a type before what depends on it). Every card names its `deps`.

4. **Self-review before handing off.** Re-read your own list against A/B/C/D and fix
   what fails: any card outside this milestone's scope? any monster card? any test in
   the wrong module or in `src/main`? any dependency out of order? Correct them
   yourself now — do not hand a list you know is flawed.

5. Delegate the WRITE to `@tracker` via the `task` tool: it appends the cards under a
   new `## M<n> — <title>` section in `TASKS.md`, **replaces** the placeholder
   `## M<n> … M9 / Not expanded` line with `## M<n+1> … M9` (the milestone you just
   expanded must no longer read "not expanded"), and bumps the milestone line in
   `STATUS.md`. Do not edit those two files yourself.

6. Report, and nothing else: the milestone id, how many cards, and the first three
   (id + one-line goal). Then STOP — the next `/next` picks up card one.

You are weaker at planning than at execution; that is expected and it is why rules
A–D and the self-review exist. A human reviews what you produce before it is used.
