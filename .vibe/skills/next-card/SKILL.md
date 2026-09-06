---
name: next-card
description: Run one full card end to end without further prompting — pick the next eligible card, settle the contract, delegate the implementation, build, measure with `verify`, commit if green, then stop. Use when the user types /next-card, or says "next card", "continue", "carry on", "commit it".
user-invocable: true
---

# next-card

One card, start to finish, no relaunch prompts.

Of 32 prompts typed over three days, **12 were mechanical relaunches**
("continue", "next card", "commit") that produced nothing but a turn of
context. This skill exists so those twelve are never typed again.

Run the steps below in order, without asking for confirmation between them.
Stop only where a STOP is written.

## 1. Pick the card

Read `mansart-jakarta-persistence/STATUS.md` (current focus, last log lines)
and `TASKS.md`. Take the **first** `TODO` card whose dependencies are all
`DONE`. Announce its id and one-line goal.

- If `PLAN.md` puts it in a later milestone, or a dependency does not exist
  yet: mark it `BLOCKED` with a one-line reason and take the next eligible
  card. Do not stub around it.
- If `TASKS.md` does not exist → **STOP** and run `/bootstrap-plan` instead.
- If there is no eligible card → **STOP** and say so.

**One card. Never two in a session.**

## 2. Settle the contract

From `docs/spec-notes/INDEX.md`, load **one** note. If it does not answer the
question, read the minimum span of the spec that does and write the note
(≤ 200 lines) before continuing. Never open the full spec text.

State: the behaviour, the signature, the failing test, and the **absolute path
of every file to touch**. If you cannot name the paths, find them once via
`recon` — not by searching yourself, and never twice.

## 3. Delegate the implementation

`task` → `impl`, with the contract from step 2 and the absolute paths. Never
write production code yourself.

If `impl` reports a missing path or an unclear contract, fix the contract and
re-delegate once. If it fails a second time on the same point → `task` →
`thinker`, then re-delegate. Never a third blind attempt.

## 4. Build

`./scripts/build.sh` (or via `impl`). Red → back to step 3 with the error,
at most twice, then STOP and report.

## 5. Measure

`task` → `verify`. It is the only source of numbers. Take its report verbatim.

- `BUILD: RED` → **STOP**. Report the figures. Do not commit, do not mark the
  card `DONE`, do not "explain" the failure away.
- `tests=0` after a green build → **STOP**. The suite did not run; that is a
  failure, not a pass.

## 6. Record and commit — only if green

1. Update `STATUS.md` and `TASKS.md` with `verify`'s figures **verbatim**.
   Never a number from memory, from the previous session, or rounded.
2. Commit: Conventional Commits, English, signed off (DCO), with a
   `Co-Authored-By:` trailer naming the model that wrote the change. The
   subject names the card id.
3. Do **not** push.

## 7. Stop

Print a three-line close: card id + status, `verify`'s numbers, and the id of
the next eligible card. Then **stop** — do not start it.

The next card gets a **fresh session**. That is not a formality: the most
expensive session measured ($19.55, 44% of a three-day bill) was one that kept
going, 757 steps, on a context that never reset.
