---
name: context-discipline
description: How to work inside a 53k-token window on a local model — the measured limits of Qwen3.6-35B-A3B-MTPLX, the token budget, LSP-first navigation, and the write-size rule. Load when a session starts feeling heavy or when a tool call gets truncated.
---

# Working inside 53k tokens

The model is **Qwen3.6-35B-A3B-MTPLX-Optimized-Speed**: 35B mixture-of-experts,
3B active, 4-bit affine body (group 64), with a native MTP sidecar. 40 layers,
`full_attention_interval = 4` — so 10 full-attention layers and 30
gated-delta-net layers. `max_position_embeddings` is 262 144.

## Measured behaviour (2026-08-25, this machine)

Single-shot probe: N tokens of real mansart Java sources, then one instruction
requiring a well-formed tool call.

| prompt tokens | wall | prefill | tool call valid | output tokens |
| --- | --- | --- | --- | --- |
| 7 336 | 5.0 s | 1 473 tok/s | yes | 394 |
| 21 188 | 7.0 s | 3 042 tok/s | yes | 291 |
| 41 894 | 11.7 s | 3 584 tok/s | yes | 289 |
| 62 256 | 23.8 s | 2 616 tok/s | yes | **1 093** |
| 83 750 | 21.2 s | 3 941 tok/s | yes | 304 |

Decode is ~105 tok/s with MTP on (measured; the model card claims 138 at depth 1
with 0.886 acceptance, against 94 for plain autoregressive).

Two things to read out of that table:

1. **Tool calls do not break at 84k.** The earlier 6-bit Qwen3.6 build truncated
   past ~60k; this one does not. The cliff is not where it used to be.
2. **Degradation shows up as reasoning inflation before it shows up as
   breakage.** At 62k the model burned 1 093 output tokens on a task that cost
   289 elsewhere. That is the real signal: long context does not make the model
   wrong, it makes it hesitant, and hesitant costs time and invites drift.

The binding constraint is therefore **latency and quality, not capacity**. A 42k
prompt costs ~12 s of prefill on a cache miss; an 84k prompt costs ~21 s. Over a
40-turn session that is the difference between eight minutes and fourteen.

## The budget

Configured, and why:

| knob | value | reason |
| --- | --- | --- |
| oMLX `max_context_window` | 131 072 | server headroom; never the thing that truncates |
| oMLX `max_tokens` | 16 384 | must exceed thinking budget + largest tool call |
| oMLX `thinking_budget_tokens` | 4 096 | leaves ≥12 288 for the tool call itself |
| oMLX sampling | 0.6 / 0.95 / top-k 20 | exactly what `mtplx_runtime.json` recommends; never near-greedy with thinking on |
| OpenCode `limit.context` | 65 536 | compaction fires at 65 536 − 12 288 ≈ **53 k** |
| OpenCode `limit.output` | 12 288 | matches what oMLX guarantees after thinking |

That last line is the fix for the old "EOS in the middle of a tool call" bug:
`max_tokens` was 12 288 while the thinking budget was 8 192, leaving only 4 096
for a large `write`. The write was cut mid-JSON, the call never closed, and the
turn was lost. Keep `max_tokens > thinking_budget + biggest_write`.

Target split of the 53k working window:

```
system prompt + AGENTS.md + one skill      ~6 k
tool schemas + LSP diagnostics             ~3 k
the task card                              ~1 k
working set: at most 4 Java files         ~12 k
tool output (grepped, never raw)          ~10 k
headroom for reasoning and edits          ~20 k
```

## The five rules

1. **LSP before grep, always.** `goToDefinition`, `findReferences`, `hover`,
   `documentSymbol`, `workspaceSymbol`. An `lsp` call is ~100 tokens; reading a
   Java file is ~3 000. Use `grep` only for non-Java files, XML, Markdown and
   string literals. jdtls compiles with ECJ and does not run our annotation
   processors the way javac does — its diagnostics are advisory, `./mvnw`
   decides.
2. **Never let a build log into the window.** Route every Maven, test and TCK
   invocation through the `ctx` tools and read surefire XML, not console output.
3. **One task card per session.** Cards live in `TASKS.md`, name at most 4 files,
   and carry the test that proves them. Two cards in one window is how a session
   ends in compaction.
4. **150 lines per `write`, hard.** Bigger class? Write the skeleton — package,
   imports, type declaration, empty methods — then fill it with several `edit`
   calls of one or two methods each. This is also better for review.
5. **Delegate to burn someone else's context.** `@explore` for "where is X",
   `@spec-reader` for "what does the spec say" (it runs in a 128k window),
   `@tck-runner` for suites, `@auditor` for review, `@tracker` for state files,
   `@thinker` for one hard decision. Their contexts die; yours does not.

## When to stop

Past ~70 % of the window: finish the current step, run `/session-end`, start a
fresh session. A compacted session is strictly worse than a new one seeded from
`STATUS.md` — compaction throws away exactly the tool output you are about to
need and keeps the prose you do not.

If a `write` or `edit` fails twice with a schema error, write the file with a
bash heredoc and move on. Do not spend three turns fighting a tool.
