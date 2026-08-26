---
name: context-discipline
description: How to spend a 128k window well on a local model — the measured long-context behaviour of Qwen3.6-35B-A3B-MTPLX, the output budget, LSP-first navigation, and the write-size rule. Load when a session starts feeling heavy or when a tool call gets truncated.
---

# Spending a 128k window well

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

Two further probes, and one real session:

- a canary word placed at **position 0** of a **165 033**-token prompt was recalled
  correctly — no head truncation, no loss of the beginning;
- a real agentic session peaked at **106 941 tokens with zero compactions** and
  still emitted a tool call in **104 of 107** assistant turns (97 %), all the way
  through to the commit.

What to read out of all that:

1. **Capacity is not your problem.** The ~60k cliff belonged to the earlier 6-bit
   build. This one holds structurally past 100k in a real loop.
2. **The one negative signal is hesitation, not breakage.** At 62k in the ladder
   the model burned 1 093 output tokens on a task costing 289 elsewhere. It did not
   reproduce in the 107k session — treat it as a reason to stay tidy, not a wall.
3. **Nothing stops you.** `limit.context` is a display denominator; neither
   OpenCode nor the server enforces it. The indicator can read over 100 %. Judge a
   session by whether it is still calling tools, never by the percentage.

So the binding constraint is **latency and decisiveness, not capacity**. A 42k
prompt costs ~12 s of prefill on a cache miss, 165k costs ~68 s. Prefix caching
amortises successive turns, but any cache break is paid in full. A disciplined card
finishes around 50k; a wandering one reaches 107k and takes three times longer for
the same result.

## The budget

Configured, and why:

| knob | value | reason |
| --- | --- | --- |
| oMLX `max_context_window` | 131 072 | server headroom; never the thing that truncates |
| oMLX `max_tokens` | 16 384 | must exceed thinking budget + largest tool call |
| oMLX `thinking_budget_tokens` | 4 096 | leaves ≥12 288 for the tool call itself |
| oMLX sampling | 0.6 / 0.95 / top-k 20 | exactly what `mtplx_runtime.json` recommends; never near-greedy with thinking on |
| OpenCode `limit.context` | 131 072 | matches oMLX; drives the % indicator only — **enforces nothing** |
| OpenCode `limit.output` | 12 288 | sent as `max_tokens` on every request; overrides oMLX's 16 384 |
| auto-compaction | **off** | compaction is what kills sessions here — see the third rule below |

The output line is the fix for the old "EOS in the middle of a tool call" bug:
`max_tokens` was 12 288 while the thinking budget was 8 192, leaving only 4 096
for a large `write`. The write was cut mid-JSON, the call never closed, and the
turn was lost. Keep the generation cap above `thinking_budget + biggest_write`;
today that leaves **8 192 tokens** for a tool call, roughly 600 lines of Java.

The shape of a healthy session — a target, not a limit:

```
system prompt (agent + AGENTS.md + OPERATING.md + skills index)  ~5.4 k
tool schemas + LSP diagnostics                                     ~3 k
the task card                                                      ~1 k
working set: at most 4 Java files                                 ~12 k
tool output (grepped, never raw)                                  ~12 k
headroom for reasoning and edits                                  ~17 k
```

That lands a finished card around **50k**. The window holds far more, and the
measurements say it still works up there — but every token you did not need is
prefill you pay for on every subsequent cache break.

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
   hits the wall — and the first real session proved what that costs: four cards
   in one window, four compactions, and after the fourth the model stopped
   emitting tool calls entirely and started describing them in prose instead
   (0 % tool calls over the last four turns, against 97 % before). That is why
   auto-compaction is now off: the session errors instead of degrading quietly.
4. **150 lines per `write`, hard.** Bigger class? Write the skeleton — package,
   imports, type declaration, empty methods — then fill it with several `edit`
   calls of one or two methods each. This is also better for review.
5. **Delegate to burn someone else's context.** `@explore` for "where is X",
   `@spec-reader` for "what does the spec say" (it runs in a 128k window),
   `@tck-runner` for suites, `@auditor` for review, `@tracker` for state files,
   `@thinker` for one hard decision. Their contexts die; yours does not.

## When to stop

When the card is done, not when the window fills. Run `/gate`, then `/session-end`,
then tell the user to start a fresh session for the next card. A fresh session
seeded from `STATUS.md` costs ~5.4k tokens and behaves like turn one.

There is no wall to hit — nothing will stop you, and the indicator can go past
100 %. That is exactly why the stopping decision is yours: a second card in the
same window is not blocked, it is just slower and worse than a restart.

**A symptom to recognise in yourself**: if you catch yourself writing "Let me load
the skill and then…:" or "**LOADING skill: x**" instead of calling the tool, you
have drifted into narrating. Emit the call. That drift is what a dead session looks
like from the inside.

If a `write` or `edit` fails twice with a schema error, write the file with a
bash heredoc and move on. Do not spend three turns fighting a tool.
