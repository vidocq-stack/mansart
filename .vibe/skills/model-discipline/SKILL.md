---
name: model-discipline
description: How to spend a Medium 3.5 / Small 4 cloud session well — cost per model, why compaction can't be switched off in Vibe (only its threshold moved), delegation as the real context-control mechanism, and the write-size rule. Load when a session starts feeling heavy, expensive, or when a tool call gets truncated.
user-invocable: false
---

# Spending a cloud session well

This is the cloud-API sibling of the local-model discipline documented for
the `ybl/jpa-opencode` attempt (`PREPARE_OPENCODE.md` on that branch). The
constraints are different — no context-truncation cliff, no thinking-budget
arithmetic — but the underlying discipline (small cards, delegate to burn
someone else's context, never let a build log into the window) still holds
and still decides whether a session finishes a card or wanders.

## What things cost

| model | alias | input $/M | output $/M | thinking |
| --- | --- | --- | --- | --- |
| `mistral-vibe-cli-latest` (Medium 3.5) | `mistral-medium-3.5` | 1.5 | **7.5** | max |
| `mistral-small-latest` (Small 4) | `mistral-small-4` | 0.15 | 0.6 | off |
| local `Devstral-Small-2-24B` (oMLX) | `devstral-local` | 0.0 | 0.0 | off |

Output tokens on Medium cost **12.5×** what they cost on Small 4 — the
economic half of the split, alongside the capability one: `dev`
(Medium) should be reading, deciding and writing *short* instructions;
`coder`/`tck-runner`/`tracker` (Small 4) write the bulk of the
Java/XML/status output. `devstral-local` is free but needs oMLX running —
switch to it per agent if cost matters more than staying cloud-hosted.

**Two model substitutions happened while designing this split, both worth
knowing about before assuming a model name means what its size implies**:

- **Devstral (the original "code" model) is deprecated and retired from
  La Plateforme** — every SKU past its retirement date on docs.mistral.ai,
  confirmed by two separate research passes. No agent references cloud
  Devstral any more; Small 4 absorbed its agentic-coding role and does the
  job instead.
- **Mistral Large 3 (considered for the "reasoning" role) was rejected in
  favour of keeping Medium 3.5** — Large 3 is *cheaper* than Medium, not
  more capable, shipped without a reasoning mode, and measurably loses to
  Medium 3.5 on the closest available agentic/reasoning benchmarks (vals.ai:
  +21 to +29 points on SWE-Bench-Verified/Terminal-Bench/Finance-Agent).
  `dev`/`thinker`/`spec-reader`/`auditor`/`guardian` all stay on Medium.

## Prompt caching (platform-level, not something to configure here)

La Plateforme discounts cached input tokens to **10% of the standard input
price** (`cached_input_price` in `.vibe/config.toml` reflects this: 0.15
vs 1.5 for Medium). Caching keys off a stable `prompt_cache_key` and hits
when a request shares a prefix with a recent one — Mistral's own docs
specifically list "agent completion requests that reuse the same context
across turns" as the headline use case, which describes every `vibe`
session. No CLI flag or config field for `prompt_cache_key` was found while
building this setup, so this is presumably handled internally by Vibe (one
key per session) rather than something to wire up — but it is not
confirmed. Cache granularity is 64 tokens; a session's early, stable
context (`AGENTS.md`, skills, the system prompt) is exactly the kind of
prefix that benefits, one more reason to keep the working set additions
(the ≤4 files a card touches) at the *end* of what an agent reads, not
interleaved with stable content.

## Compaction cannot be turned off — only its threshold moved

OpenCode's local setup ran with auto-compaction **off**, because a
compaction on a 3B-active local model measurably collapsed the session into
narrating tool calls instead of emitting them (0% tool-call rate after the
fourth compaction, see `PREPARE_OPENCODE.md` §"Why auto-compaction is OFF").
Vibe has no boolean equivalent: compaction always fires once
`auto_compact_threshold` is reached (`.vibe/config.toml` sets it per model,
150k here — see that file's comments; `compaction_model` is Small 4,
confirmed GA, so a fired compaction can't hit a retired endpoint). Whether
Medium 3.5 / Small 4 degrade the same way after a compaction is **not yet
measured on this setup** — do not assume either outcome. Treat the first
compaction of a real session as an experiment: watch the next few turns
for tool calls that turn into prose
narration ("Let me now...", "I'll load the skill and then:") and, if you see
it, stop and start a fresh session seeded from `STATUS.md` rather than
prodding the same one — a fresh session costs a few thousand tokens on
Medium and is strictly recoverable; a degraded one may not be.

For unattended/programmatic runs, `--max-tokens`, `--max-turns` and
`--max-price` (`vibe -p ... --max-tokens N`) are hard session-level
safety nets Vibe provides natively — OpenCode had no equivalent. Use them
for anything scripted (`/session-end`-driven automation, a CI-triggered
`/gate`).

## Delegation is the real context-control mechanism, same as before

`dev` delegates through the `task` tool to a **subagent** — a burnt
context is the subagent's, not `dev`'s. Depth is limited to 1 by Vibe
itself: a subagent cannot delegate further, so keep tasks handed to
`coder`/`spec-reader`/`tck-runner`/`auditor`/`guardian`/`tracker`/`thinker`
self-contained (see `.vibe/agents/*.toml`, and the root `AGENTS.md` for
when to reach for which one). Vibe's own `explore` builtin covers "where
is X, what already exists" without a custom agent file.

## No confirmed Java LSP in this setup

Unlike OpenCode (which shipped jdtls out of the box), this `vibe` install
has no verified Java language-server integration — a stale
`~/.vibe/mcp.json` entry for a `java-lsp` MCP server was found dead (Vibe
does not read `mcp.json` at all) and its package name
(`@modelcontextprotocol/server-lsp`) is unverified. Until a
real MCP LSP bridge is wired in via `[[mcp_servers]]` in `.vibe/config.toml`
and confirmed working, treat `grep`/`read_file` as the only navigation
tools — there is no `~100-token lsp call` shortcut to rely on the way the
OpenCode setup had. Budget for that: reading a Java file costs real tokens
here too, so keep the working set to the ≤4 files a card actually touches.

## The write-size rule (unchanged)

Keep a single `write_file` call under ~150 lines. A bigger class gets a
skeleton first (package, imports, type declaration, empty methods), then
several `edit` calls of one or two methods each — better for review, and it
avoids a truncated tool call eating a turn.

## When to stop

One card per session, same as before. When the card is done: `tck-runner`
for the measured number, `auditor` for drift, `tracker` to write
`STATUS.md`/`TASKS.md`, then a fresh session for the next card. A session
that reaches for a second card in the same window is not blocked by
anything technical — it is just paying Medium-3.5 prices for a decision that
a clean restart would make just as well for a fraction of the tokens.
