# BENCH.md — mansart

Every performance figure quoted anywhere in this repository must have an entry
here: absolute date, hardware and runtime, the exact command, the raw results, and
the delta against the previous run of the same benchmark.

---

## 2026-08-26 — Local model selection for the OpenCode workflow

**Why**: `PREPARE_OPENCODE.md` pins `Qwen3.6-35B-A3B-MTPLX-Optimized-Speed` (4-bit)
as the model driving the Jakarta Persistence 3.2 work. Two candidates were proposed
as replacements — an 8-bit MTPLX build and a 6-bit MTPLX merge. This run decides.

**Hardware / runtime**: Apple M5 Max, 128 GB unified memory, macOS 26.5.2,
oMLX server on `127.0.0.1:8000`. Machine otherwise idle.

**Configuration**: all three models set identically in `~/.omlx/model_settings.json`
— `max_context_window` 131072, `max_tokens` 16384, `thinking_budget_tokens` 4096
(`enable_thinking` on), `force_sampling` at temperature 0.6 / top-p 0.95 / top-k 20,
`turboquant_kv_enabled` off. Sampling also forced per request.

**Command**: `python3 bench2.py` (script archived with this entry; batteries A/B/C/E/F
described below).

### Candidates

| tag | model | on disk |
| --- | --- | --- |
| 4bit | `Youssofal/Qwen3.6-35B-A3B-MTPLX-Optimized-Speed` | 20.0 GB |
| 6bit-SABER | `samuelfaj/Qwen3.6-35B-A3B-NSC-ACE-SABER-6bit-MTPLX-Optimized-Speed` | 28.5 GB |
| 8bit | `samuelfaj/Qwen3.6-35B-A3B-8bit-MTPLX-Optimized-Speed` | 37.6 GB |

**No MTP sidecar loads on this oMLX version — not on any of the three.** Setting
`mtp_enabled: true` makes the model fail to load outright:

```
6bit : ValueError: Lightning MTP is enabled for this model but the converted
       weights are missing the mtp.* tensors.
8bit : ValueError: Received 20 parameters not in model → VLM load failed
4bit : ValueError: Received 2321 parameters not in model → VLM load failed
```

The 4-bit was believed to be the exception for most of the day; it is not. It only
appeared to work because its `mtp_enabled: true` sat on the *other* id of the
duplicated install (see the follow-up section), so the copy actually serving
requests had MTP off. Verified afterwards: with `mtp_enabled: false` explicitly on
the served id, the same model decodes at **138.1 tok/s** — slightly *faster* than
the 129.4 tok/s recorded below.

**This does not weaken the comparison.** All three candidates ran autoregressive,
which makes the run apples-to-apples. It removes one *argument* for the 4-bit, not
the result. The "MTPLX" label buys nothing on any of these builds here.

### A. Decode throughput (prefill isolated with a `max_tokens=1` call)

| context | 4bit | 6bit-SABER | 8bit |
| --- | --- | --- | --- |
| 8 000 | **129.4 tok/s** | 197.7 (unreliable, see caveats) | 82.4 tok/s |
| 40 000 | **107.4 tok/s** | n/a | 74.5 tok/s |
| 80 000 | **89.6 tok/s** | n/a | 63.8 tok/s |

The 4-bit holds 89.6 tok/s at 80k — decode degrades gently with context, it does not
collapse.

### B/C. Tool-call fidelity, argument exactness, output economy (ctx ≈ 20k, 8 probes)

| | 4bit | 6bit-SABER | 8bit |
| --- | --- | --- | --- |
| well-formed calls | **8/8** | **8/8** | 7/8 |
| exact arguments | **8/8** | **8/8** | 7/8 |
| median output tokens | **367** | 666 | 932 |
| total output tokens | **3 423** | 5 042 | 5 619 |
| wall clock | **36.1 s** | 85.2 s | 90.5 s |

The 8-bit failure is the `terse` probe (`record_finding H.java 9 high 'leak'`):
`finish=length` at the 2 048-token probe cap with no tool call emitted — it spent
the entire budget reasoning about a one-line instruction. Reproduced in both runs
of the day.

### E. `jakarta.persistence` API correctness (8 checkable questions)

| | 4bit | 6bit-SABER | 8bit |
| --- | --- | --- | --- |
| correct | **8/8** | **8/8** | **8/8** |
| wall clock | **30.6 s** | 37.4 s | 62.6 s |

Exception types for `find`/`getReference`/`flush`/`getSingleResult`, full
`CascadeType` and `FetchType` constant lists, `LockModeType.OPTIMISTIC_FORCE_INCREMENT`.
No quantisation level separates them on knowledge — only on speed.

### F. Multi-turn tool chain (`list_files` → `read_file` → `record_finding`)

| | 4bit | 6bit-SABER | 8bit |
| --- | --- | --- | --- |
| chain | correct | correct | correct |
| turns | 3 | 3 | 3 |
| wall clock | **5.4 s** | 6.3 s | 6.5 s |

### Total wall clock, batteries B/C + E

| 4bit | 6bit-SABER | 8bit |
| --- | --- | --- |
| **66.7 s** | 122.6 s | 153.1 s |

### Verdict

**Keep the 4-bit.** It matches both candidates on every quality metric that
discriminates — 8/8 fidelity, 8/8 exact arguments, 8/8 API correctness, chain
correct — while being **2.3× faster end to end** than the 8-bit and using **half the
memory** (20.0 GB vs 37.6 GB).

The 8-bit is also the only model that failed anything, and it failed the same probe
twice. The hypothesis that a higher-precision quant would be more decisive is not
supported: with thinking enabled and budgeted, the 8-bit produced the *worst* output
economy of the three (median 932 tokens against 367).

The 6-bit SABER merge is quality-equivalent and roughly half the speed. Nothing
justifies the switch.

### Caveats — do not quote these numbers without them

- **Single run, no repetitions.** Medians over 8 probes are noisy. The wall-clock
  totals (66.7 / 122.6 / 153.1 s) are the robust signal; the per-probe medians are
  not. An earlier run the same day, with the 8-bit misconfigured (no thinking
  budget, temperature 1.0), produced the *opposite* economy ranking — which is
  precisely why configuration symmetry matters.
- **The prefill column is not comparable across models** and was omitted: cache
  state differed (the 4-bit and 8-bit had warm prefixes from earlier runs, the
  SABER model was cold). Reported prefill times of 0.7–2.0 s are cache hits, 2.8–38.8 s
  are cold.
- **6-bit SABER decode is unreliable**: measured as `wall(400 tokens) − wall(1 token)`,
  which goes negative when the two calls hit different cache states — hence the
  197.7 tok/s outlier at 8k and the `n/a` at 40k and 80k. Its wall-clock totals are
  trustworthy; its tok/s figures are not.
- The 8-bit `terse` failure hit the *probe's* 2 048-token cap, below the 4 096-token
  thinking budget and far below the 12 288-token production output limit. In a real
  session it would likely have emitted the call eventually — but only after burning
  several thousand tokens on a trivial instruction.
- `NSC-ACE-SABER` is a merge/finetune lineage, not a plain quantisation of Qwen3.6.
  Its results say nothing about 6-bit quantisation as such.
- **Every figure here is autoregressive decode.** `mtp_enabled` must stay `false`
  for this model on this oMLX version, or it will not load. If a future oMLX build
  accepts these sidecars, the whole campaign should be re-run — the published
  acceptance for the 4-bit (0.886 at depth 1, ×1.465) would change the ranking.
- **The model is multimodal but is served text-only, and the same fault kills MTP.**
  The checkpoint carries 333 `vision_tower` tensors and a full `vision_config`
  (27 layers, patch 16), architecture `Qwen3_5MoeForConditionalGeneration`. oMLX
  *does* classify it correctly — `Discovered model: … (type: vlm, engine: vlm,
  size: 20.52GB, text-only: 19.65GB)` — but the mlx-vlm loader then rejects the
  checkpoint (`Received 2321 parameters not in model`), `VLMBatchedEngine` stops,
  and oMLX falls back to the text `BatchedEngine`. That fallback is why everything
  else works. Verified twice, with both the `image_url` and `image` content forms:
  the model answers "I don't see any screenshot attached". So `opencode.json` is
  right to declare text-only modalities and `attachment: false`. The MTPLX
  conversion is the common cause: it produces a checkpoint mlx-vlm cannot consume,
  which costs both the vision tower and the MTP sidecar. A plain (non-MTPLX)
  conversion of the same weights would likely restore both — untested.

### Follow-up, same day — disk cleanup and an id/settings mismatch

Auditing the model library after the benchmark turned up two things worth
recording:

- **The winning model was installed twice**, 20 GB in `~/omlx-models/Youssofal/`
  and 20 GB again in the HuggingFace cache. oMLX exposes each copy under a
  different id (bare vs repo-qualified) and lists them as two library entries.
- **Every tuned setting was on the wrong id.** `opencode.json` used the bare id
  (the `~/omlx-models` copy, confirmed by `BatchedEngine loaded:` in the server
  log) while `model_settings.json` carried `mtp_enabled`, `thinking_budget_tokens`,
  `force_sampling` and the pin on the repo-qualified id. The copy actually serving
  requests was running on server defaults. Settings have been moved onto the served
  id and the duplicate copy deleted.

The benchmark numbers above are unaffected: they were produced by explicit model
ids, and the winning run used the bare id whose configuration is now the live one.
They should nevertheless be re-taken if the MTP state of that id is ever in doubt.

Disk reclaimed: 61 GB total — 14 GB of provably broken downloads (partial shards,
empty snapshot directories), 28 GB for the rejected `…-Optimized-Balance`, 20 GB
for the duplicate copy, minus overlap, plus ~47 empty metadata stubs left in the
HuggingFace cache by deletions made through the oMLX UI, which only manages
`~/omlx-models`.

### 2026-08-27 — Server ageing costs ~25% decode, a restart recovers it

Session decode averages had drifted down and it looked like a regression. It was
not the model or the sampling config: it was oMLX uptime.

Decode rate at fixed context, oMLX's own logged tok/s:

| context | 25/08 (server fresh) | 26/08 (aged) | 27/08 after restart |
| --- | --- | --- | --- |
| 20k | 113 | 86 | ~89 |
| 40k | 102 | 71 | **92** |

The 40k row is the clean signal: 71 tok/s after ~19 h of uptime (SSD cache grown to
90 GB, memory fragmentation), 92 tok/s immediately after a restart, without touching
any setting. The sampling changes of 26/08 (force_sampling, top_k 20, thinking
budget) are therefore not the cause — a restart alone recovered it.

First-pass measurements at a cold context (11, 28 tok/s) are artefacts of the SSD
cache being rebuilt; only the second pass (warm cache) is trustworthy.

**Operational takeaway: restart oMLX periodically** (each morning, or when decode
feels slow). The Restart button is enough.

### 2026-08-27 — KV cache quantization (int8): no speed gain, rejected

A suggestion (via Gemini) to enable KV-cache quantization for speed. Tested, rejected.

Two premises were wrong before testing:
- **oMLX has no `--kv-bits` flag** — KV quantization is `turboquant_kv_enabled` /
  `turboquant_kv_bits` in `~/.omlx/model_settings.json`.
- **The KV is not 12–16 GB at 100k, it is ~1.9 GB.** This is a hybrid model: only
  10 of 40 layers are full-attention (KV grows), the other 30 are gated-delta-net
  (constant recurrent state, no growing KV). Estimating it as a dense transformer
  overstates the KV ~6×.

Measured, at equal server state (fresh, warm cache, 2nd pass), 40k context:

| | decode |
| --- | --- |
| fp16 KV (default) | 61.8 tok/s |
| int8 KV | 61.8 tok/s |

Identical. No gain — expected, since the KV was never the bottleneck at 2 GB.

**Methodology note / mea culpa**: a first, hasty reading showed int8 "18–28% slower",
but that compared a *rested* server (76 tok/s) against a *fresh* one (62). Server
freshness, not KV bits, drove the difference (see the 2026-08-27 uptime entry). Never
compare decode across two server states. The clean same-state comparison is the 40k
row above: a draw.

**Verdict: keep `turboquant_kv_enabled: false` (fp16 KV).** No speed to gain, and KV
quantization is exactly what would erode long-context quality — the model's weak
spot. Reverted after the test.
