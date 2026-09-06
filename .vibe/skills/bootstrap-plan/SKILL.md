---
name: bootstrap-plan
description: Generate PLAN.md, TASKS.md and STATUS.md for a mansart sub-module from scratch. Use at the start of work on a sub-module that doesn't have these three files yet, or to regenerate them after a reset.
user-invocable: true
---

# Bootstrap a sub-module's planning documents

You are about to write three files for a target sub-module directory —
`PLAN.md` (milestone map), `TASKS.md` (the current milestone expanded into
work cards), `STATUS.md` (a fresh baseline) — that do not exist yet. This
is a **planning session, not an implementation session**: do not write any
Java, do not touch `pom.xml`/`module-info.java`, and stop once the three
files are written and reported. **Never read `mansart/CLAUDE.md`** —
everything you need is in this skill, the root `AGENTS.md`, and the
arguments this command was given; that file is for a different tool and is
not a source of truth for this session.

## 1. Determine the target directory and the spec

Takes up to two arguments: `/bootstrap-plan [<directory>] [<spec>]`.

- Both given → use them as-is, no inference needed. This is the path for a
  sub-module `AGENTS.md` doesn't describe yet (a brand-new one).
- Only `<directory>` given → the spec is whatever that directory's own
  architecture skill states (e.g. `mansart-jpa` names "Jakarta Persistence
  3.2"), or the root `AGENTS.md`'s "Current active work" section if it
  happens to name that same directory. If neither has it, ask the user
  for the spec — do not guess a version number.
- No arguments → target = the directory and spec named in root
  `AGENTS.md`'s "Current active work" section (self-contained — that
  section already states the spec and the locked decisions; you do not
  need to look anywhere else for them).

State the target directory and spec before doing anything else.

## 2. Load context — in this order

1. The root `AGENTS.md` (already loaded for this session) — the general
   engineering contract, and, if the target matches, the "Current active
   work" section's mission/locked-decisions summary. Do not re-litigate
   anything stated there as locked.
2. The target's own architecture skill if one exists (e.g. `mansart-jpa`
   for `mansart-jakarta-persistence`) — reactor layout, non-negotiables,
   build commands. Load `mansart-jpa-tck`/`vidocq-codegen` too if the
   target's skill points at them. If no such skill exists yet (a genuinely
   new sub-module), work from the spec argument and the sibling precedent
   in step 3 alone, and say so in the report at the end.
3. **A sibling, already-delivered mansart sub-module as structural
   precedent** — `mansart-jakarta-data` is the reference: read its
   `pom.xml` layout, module list, and (if still present in git history)
   the shape of its own milestone breakdown. Mirror module naming,
   reactor structure, and the "out-of-reactor TCK, standalone POM 4.0.0"
   pattern rather than inventing a new shape.
4. If the target is a spec implementation with an official TCK, note the
   TCK artifact coordinates and total test-method count via `recon`
   (one question: "for <spec>, what is the exact TCK artifact and how many
   client classes/test methods does it contain?") rather than guessing.

## 3. Write `PLAN.md`

Structure, mirroring the existing delivered modules' plans:

- One-paragraph mission statement naming the spec (with version) and the
  locked architecture decisions found in step 2 — from `AGENTS.md`'s
  "Current active work" section for the default target, from the target's
  own architecture skill if one exists, or from the `<spec>` argument plus
  whatever step 2/3 actually turned up for a brand-new sub-module.
- An architecture diagram (module → module data flow) and a reactor table
  (module name → role), built from what you actually found in step 2 —
  never invent a module that doesn't map to something in the sibling
  precedent or an explicit requirement.
- A milestone table. **Each milestone MUST end on a measured number**
  (TCK PASS/total for named client packages, or the equivalent
  spec-conformance signal for a non-TCK module) — never "the code
  compiles" and never a card count. **The very first milestone (M0) must
  wire the real, end-to-end harness before any feature work starts**: for
  a TCK-bearing module that means a real (even if `not implemented`-
  throwing) provider actually registered and reachable by the official
  TCK runner, proven by a baseline TCK run showing real `setup`-stage
  errors rather than a harness failure — not a stub the harness never
  actually exercises. This is not a style preference: a prior local-model
  attempt at `mansart-jakarta-persistence` (`ybl/jpa-opencode`) reached
  83% of its planned cards through several feature milestones while its
  TCK provider was never actually wired to the runner, leaving the *real*
  progress number unchanged from baseline the entire time. Do not repeat
  that mistake for this target or any other.
- A decomposition-rule paragraph: milestone = planning unit, card = work
  unit (≤4 files, one proving test, explicit deps), only the current
  milestone gets expanded into cards, `impl` expands the next one when
  the current one closes.
- An explicit non-goals list.

## 4. Draft the current milestone's cards

Expand **only** the first/current milestone (usually M0) into cards, in
the shape:

```
### <PREFIX>-xx — <goal, as a behaviour>            [TODO]
deps:   <PREFIX>-yy or —
files:  <max 4 paths>
proof:  <test path or TCK client>
notes:  <traps, one or two lines>
```

Pick a short id prefix from the target module name (e.g. `JP` for
`mansart-jakarta-persistence`). Do not expand any later milestone.

## 5. Delegate the actual write of `TASKS.md` and `STATUS.md`

`impl` is the only agent whose `write_file`/`edit` permissions cover
these two files (see `.vibe/agents/tracker.toml`) — this is enforced by
Vibe's own tool-permission system, not just a convention, so write `PLAN.md`
yourself (it isn't impl's file) but hand the drafted milestone table and
card list to `impl` via the `task` tool to actually write `TASKS.md`
(the milestone summary table + the expanded cards from step 4, later
milestones listed as `TO_DEFINE`) and `STATUS.md` (current focus = the
first card, all numbers `not measured`/`0`, empty session log, capped at 60
lines as always).

## 6. Report and stop

State the three file paths written, the milestone count, and the first
card's id. Do **not** start implementing that card in this same
session — bootstrapping the plan and working the first card are two
separate sessions, same discipline as every other card.
