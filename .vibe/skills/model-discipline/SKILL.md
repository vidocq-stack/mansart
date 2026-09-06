---
name: model-discipline
description: How to spend a GLM 5.2 / Small 4 session well — what each model costs, why reasoning is off by default and how to turn it on, why compaction can't be switched off in Vibe (only its threshold moved), and delegation as the real context-control mechanism. Load when a session starts feeling heavy or expensive, or when a tool call gets truncated.
user-invocable: false
---

# Model discipline

Cost here is not a background concern: the monthly budget is the binding
constraint on how much of this spec gets implemented.

## The priority order — measured, and counter-intuitive

10 sessions on `ybl/jpa-vibe`, 3–6 Sep 2026: **$43.98, 24 cards of 112,
1 701 steps, 138 M input tokens against 581 k output (238:1), 90.6% of input
served from cache, 81 000 tokens of context per step.**

**Input is 93% of the bill.** Therefore, in order:

1. **Volume of context re-sent** — −70% of context ≈ −65% of the bill. This is
   the only lever that matters at scale.
2. **Share of steps carried by Small 4** — 11× cheaper on *input*, which is
   the side that counts.
3. **Choice of reasoning model — about 1%.** GLM 5.2 and Medium 3.5 have
   near-identical *input* prices (1.54 vs 1.5), and input is where the money
   goes.

So: **never present the move to GLM 5.2 as a saving.** It was a quality and
context-window decision. The output-price table below is real but secondary —
581 k output tokens against 138 M input is a rounding error on the invoice.

Where the context actually came from, measured across 39 transcripts:

| Source, in the PRIMARY context | share |
| --- | --- |
| `read_file` | 42% |
| `bash` (build output, searches) | 28% |
| `write_file` + `edit` | 21% |
| `task` returns (i.e. actual delegation) | **4.6%** |

**72% of the primary context was `read_file` + `grep` + `bash` — all of it
delegable, none of it delegated.** That single number is the reconfiguration.

## What things cost

| model | alias | in $/M | cached in $/M | out $/M | context | reasoning |
| --- | --- | --- | --- | --- | --- | --- |
| GLM 5.2 (`zai-glm-5-2`) | `glm-5-2` | 1.54 | 0.154 | **4.84** | 1M | **off** |
| GLM 5.2, same model | `glm-5-2-think` | 1.54 | 0.154 | **4.84** | 1M | high |
| Mistral Small 4 | `mistral-small-4` | 0.15 | 0.015 | 0.60 | 128k | off |
| Mistral Medium 3.5 *(fallback, unassigned)* | `mistral-medium-3.5` | 1.5 | 0.15 | 7.5 | 256k | off |
| local Devstral-Small-2-24B (oMLX) | `devstral-local` | 0.0 | — | 0.0 | — | off |

Output on GLM 5.2 costs 8× Small 4, and input 10×. The input ratio is the one
that decides the bill. `spec` reads, decides and writes *short* instructions;
`impl`, `recon` and `verify` absorb the volume — both what they emit and,
more importantly, what they have to read in order to emit it.

## Reasoning is off by default — and that is the biggest lever

Thinking tokens bill as **output**, at 4.84 $/M. So the default alias
`glm-5-2` sets `thinking = "off"`, and only `thinker` runs on
`glm-5-2-think`.

What "off" does, precisely (`vibe/core/llm/backend/mistral.py`): the backend
looks `thinking` up in `_THINKING_TO_REASONING_EFFORT`, a table containing
only `low`/`medium`/`high`/`max`. `"off"` misses, so `reasoning_effort=None`
is sent and `include_reasoning_content` is `False`. No thinking tokens are
produced, none are billed.

To turn it on for a session: `/model glm-5-2-think`, or `/thinking` for the
level picker. Both are session-scoped and neither survives a restart — which
is the intent. **Turn it back off when the hard part is over.**

Two facts about the levels, so you don't pay for a placebo:

- `medium`, `high` and `max` **all map to `"high"`**. `max` buys nothing.
- `low` maps to `"none"` — i.e. `low` is *also* off, just spelled confusingly.

## Delegation is the context-control mechanism

A subagent's context is discarded when it returns. Everything bulky belongs
there:

| Work | Agent | Why |
| --- | --- | --- |
| Production code, javadoc, repetitive edits, status files | `impl` | 8x cheaper output, and the diff never enters the primary context |
| "Where is X", grep sweeps, build/test output, TCK runs | `recon` | Returns three lines instead of 400; Small 4 prices |
| One hard decision after two failed attempts | `thinker` | Fresh context beats a third attempt in a polluted one — and it is the only agent worth paying reasoning for |

**Never grep a wide sweep from `spec`, and never read a file to find out
whether it is the right file.** That output lands in the primary context and
is re-sent, at GLM input prices, on every subsequent turn of the session.

A subagent's reading is free the moment it returns. The primary's reading is
paid for again on every step until the session ends. In the measurement, the
primary did 1 717 tool calls against 750 across 29 subagent runs — the
expensive context was carried by the expensive model.

### How far the mechanisms actually get you

Replaying the new rules against the recorded transcripts:

| Measure | Projected primary context / step |
| --- | --- |
| measured, before | 89 500 |
| + guard hook (blocks re-reads, repeat searches, archive dumps, build spew) | 80 200 |
| + delegating 75% of the delegable volume | **43 300** |
| + `auto_compact_threshold = 60000` | hard ceiling at 60 000 |

Read that honestly: **the guards buy 12%; delegation buys the rest, and
neither reaches the 30 000 target on its own.** The last stretch comes from
ending the session when the card is done. Configuration cannot do that part.

Vibe's builtin `explore` subagent declares no model of its own, so it inherits
the session's active model — i.e. GLM. Use `recon` instead; that is what it
exists for.

## Prompt caching — not a setting, a discipline

La Plateforme discounts cached input to 10% of standard price
(`cached_input_price` in `.vibe/config.toml` records this; it configures
nothing — there is no cache key in the Vibe schema). Caching keys off a stable
request **prefix**.

What that means in practice:

- `AGENTS.md` sits near the front of every request. Keep it short, dense and
  **stable** — every edit invalidates the cached prefix for every later turn.
- Don't override the primary agent's `system_prompt_id`; the builtin `cli`
  prompt is identical across sessions and caches perfectly.
- Volatile material (file dumps, logs, grep output) must go to a subagent, or
  it becomes part of the prefix and is re-sent forever.

Delegation *is* the caching strategy.

## Compaction cannot be switched off

Vibe has no boolean for it. Compaction fires when a model's
`auto_compact_threshold` is reached — 800k for GLM 5.2, 120k for Small 4.
The levers are:

- **the threshold** — 800k is the value Mistral pushes for a 1M-context model,
  and it is the expensive end: a turn near 800k of uncached context costs
  roughly $1.2 of input alone. If a session is being *paid for* rather than
  *finished*, lower this (400k is a reasonable middle). Not the thinking
  level — that is already off.
- **`compaction_model`** — Small 4, so a fired compaction is cheap and cannot
  hit a retired endpoint.
- **`compaction_prompt_id`** — `mansart-compact`, in `.vibe/prompts/`. It
  produces a fixed-section handoff (card, contract, measured TCK baseline,
  files, tests, next step) and explicitly excludes spec reasoning, because
  that already lives on disk in `docs/spec-notes/`.
- **`raise_on_compaction_failure = true`** — a failed compaction stops the
  session loudly instead of continuing on a truncated history.

Whether GLM 5.2 degrades after a compaction is **not measured on this setup**
— do not assume either outcome. A 3B-active local model measurably collapsed
into narrating tool calls instead of emitting them after its fourth
compaction. Treat the first compaction of a real session as an experiment:
watch the next few turns for tool calls turning into prose ("Let me now…",
"I'll load the skill and then:"). If you see it, stop and start a fresh
session seeded from `STATUS.md` — a fresh session costs a few thousand tokens
and is strictly recoverable; a degraded one may not be.

`context_warnings = true` fires before the threshold, so a card can be
finished and the session restarted cleanly instead of compacted mid-card.

## Hard limits for unattended runs

`vibe -p "…" --max-tokens N --max-turns N --max-price D`. Session-level, and
the only mechanism here that *cannot* be talked past by a model that decides
it needs one more look at the file.

## Write size

A single `write_file` of a large generated class is billed as output at the
writing agent's rate. That is a reason to delegate it to `impl`, not a reason
to split it — a truncated file costs a retry at the same price.

## One card per session

When the card is done: `recon` for the measured TCK number, `recon` again for
a drift review of the diff, `impl` to update `STATUS.md`/`TASKS.md`, then a
**fresh session** for the next card. A session that reaches for a second card
is not blocked by anything technical — it is just paying GLM prices for a
decision a clean restart would make just as well, on a context a fraction of
the size.
