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

### 2026-08-31 — LM Studio (IQ4_NL) on BigMontreuil vs oMLX (MTPLX) on M5 Max

New setup: a second box, **BigMontreuil** (`192.168.2.2:1234`), running **LM Studio**
serving `unsloth/qwen3.6-35b-a3b`, quant **IQ4_NL**, `state: loaded`, `loaded_context:
131072`. Added to OpenCode as provider `lmstudio` (global config,
`@ai-sdk/openai-compatible`, baseURL above). Confirmed the exact model via LM Studio's
native `/api/v0/models` (the sibling `qwen/qwen3.6-35b-a3b` is Q4_K_M and not loaded).

**Protocol.** LM Studio's native `/api/v0/chat/completions` returns real
`stats.tokens_per_second` (pure decode) and `stats.time_to_first_token` (prefill). A
filler prompt padded to N tokens, 200 tokens generated, temperature 0.3. Each context
level uses a *distinct* prompt, so TTFT is a genuine cold prefill (no KV reuse).

LM Studio IQ4_NL @ BigMontreuil, full curve:

| context | decode tok/s | TTFT (cold) | prefill tok/s |
| --- | --- | --- | --- |
| 0.06k | 115.9 | 0.07 s | – |
| 4k | 109.9 | 1.46 s | ~2 770 |
| 16k | 103.7 | 4.54 s | ~3 530 |
| 32k | 95.1 | 6.63 s | ~4 830 |
| 64k | 79.2 | 15.14 s | ~4 230 |
| 100k | 66.8 | 20.22 s | ~4 950 |

Decode degrades gently (−42 % from empty to 100k); it stays usable at the top of the
window. Prefill holds around 4–5k tok/s.

**Pure-decode comparison, same day, same filler, decode isolated** (oMLX's `/v1` does
not expose per-phase stats, so oMLX decode is isolated with a two-request method:
`decode = 200 / (wall@208 − wall@8)`, which cancels prefill even when cached):

| context | oMLX MTPLX @ M5 Max | LM Studio IQ4_NL @ BigMontreuil |
| --- | --- | --- |
| 0 | 142.3 | 115.9 |
| 32k | 108.2 | 95.1 |
| 100k | not run | 66.8 |

At equal context the M5-Max/oMLX/MTPLX stack decodes ~15–20 % faster than the
BigMontreuil/LM-Studio/IQ4_NL stack. But this compares **two machines and two
quantizations at once** — it is a setup-vs-setup number, not a model or a quant verdict.

**Caveats — do not quote without them:**
- Two different boxes, two different quants (MTPLX vs IQ4_NL). Not apples-to-apples.
- A first oMLX **prefill** figure of ~58 000 tok/s was a **KV-cache artefact**: the
  two-request method replays the same 32k prompt, so the second call's prefill is
  cached and the delta is near-zero. Discarded — oMLX prefill was not cleanly measured
  here (a single cold 32k pass was ~3 350 tok/s wall-derived, one point only).
- An earlier wall-clock oMLX "17.5 tok/s @ 32k" mixed prefill+decode into one number
  and is **not** comparable to LM Studio's pure-decode 95; it was replaced by the
  two-request decode above. Same trap as the 2026-08-27 KV mea culpa: never compare a
  wall-clock number against a decode-only one.

**Takeaway.** LM Studio IQ4_NL on BigMontreuil is a viable second engine: slightly
slower decode than the Mac, but steady to 100k and easy to point OpenCode at. Whether
IQ4_NL trades quality against MTPLX is untested here — this entry is throughput only.

### 2026-08-31 — LM Studio IQ4_NL: quality battery (B/C, E, F), same probes as 08-26

Ran `.opencode/bench-lmstudio.py` — the 08-26 probes verbatim (8 tool-fidelity probes
at ctx~20k, 8 API questions, one multi-turn chain) against `unsloth/qwen3.6-35b-a3b`
IQ4_NL. Column added next to the 08-26 winner (4-bit MTPLX on the M5 Max):

| | 4bit MTPLX @ M5 Max (08-26) | IQ4_NL @ BigMontreuil |
| --- | --- | --- |
| well-formed calls | 8/8 | 6/8 → **8/8** (see note) |
| exact arguments | 8/8 | 6/8 → **8/8** (see note) |
| median out tok | 367 | 567 |
| total out tok | 3 423 | 7 010 |
| B/C wall | 36.1 s | 80.6 s |
| API correctness (E) | 8/8 | **7/8** (missed `cascade`) |
| chain (F) | correct, 3 turns | correct, 3 turns, 4.0 s |

**The two B/C "failures" are a config artefact, not a capability gap — verified.**
Both misses (`plain`, the *simplest* probe, and `terse`) were `finish=length`: the
model spent the whole 2 048-token probe budget *reasoning* about a one-line
instruction and never emitted the call. Re-ran those two probes two ways:

| probe | budget = 6 000 | `/no_think` |
| --- | --- | --- |
| plain | correct call | correct call |
| terse | correct call | correct call |

Both pass cleanly either way. So effective fidelity is **8/8 once the reasoning is
bounded** — IQ4_NL calls tools exactly right. The gap versus oMLX is that oMLX had a
`thinking_budget_tokens` cap; LM Studio here serves Qwen3.6 with reasoning on and
unbounded within the probe budget, so trivial instructions overflow.

**The real cost is output economy, and it is genuine.** IQ4_NL's reasoning roughly
**doubles** output (median 567 vs 367, total 7 010 vs 3 423) and wall (80.6 s vs
36.1 s) on the same probes. In an agent loop that means slower turns *and* a context
window that fills faster — the exact failure mode this whole workflow is built to
avoid. Not a correctness problem, an efficiency one.

`cascade` (E) is the one true knowledge miss: the model's `CascadeType` list dropped a
constant (the check requires all of PERSIST/MERGE/REMOVE/REFRESH/DETACH/ALL). Minor,
single-run.

**Caveats:** single run, no repetitions (per-probe medians are noisy; the pattern —
two length-overflows on trivial probes, doubled output — is the robust signal). Two
boxes, two quants: this is not an isolated IQ4_NL-vs-MTPLX verdict. Sampling was
matched (temp 0.6, top_p 0.95) but the reasoning-budget asymmetry above was not, and
it drove the headline B/C numbers.

**Verdict.** IQ4_NL on LM Studio is quality-competitive with the 4-bit MTPLX on
tool-call correctness (8/8 bounded) and near-parity on API knowledge (7/8), and the
chain works. **Before using it as an OpenCode engine, cap its reasoning** (a thinking
budget, or `/no_think` on mechanical steps) — otherwise trivial tool calls overflow
and every turn bleeds context. Untuned, it is the *worst* of the four for output
economy; tuned, it should match.

### 2026-08-31 — Dense Qwen3.8-27B on a single 3090 vs the M5-Max MoE

**Setup correction:** BigMontreuil is **1×3090 (24 GB)**, not two. An earlier "2×3090"
inference from LM Studio's "GPU 32 GB = Total 32 GB" was wrong — that estimate was at
the model's 262k max context; at 32768 the dense fits ~16–18 GB on a single card.
`qwen/qwen3.8-27b` is a **dense** 27B (arch qwen35, Q4_K_M GGUF with an MTP head),
loaded at 32768.

The question the user posed: does a dense 27B (27B active params/token) beat the
M5-Max MoE A3B (3B active) by enough to justify the box? Compared to the **M5 MoE**,
not to a M5 dense (which we do not have). Same throughput + quality protocol.

Throughput (LM Studio native `tokens_per_second` = pure decode):

| context | dense 27B @ 1×3090 | MoE A3B @ M5 Max |
| --- | --- | --- |
| 0 | 53 | 142 |
| ~28–32k | 51 | 108 |

Dense prefill ~580–1940 tok/s (heavy — 27B read per token). The 3090's ~50 tok/s is
near its bandwidth ceiling (936 GB/s ÷ ~16 GB ≈ 58): the card is well used, a dense 27B
is simply ~9× the MoE's per-token work. The MTP head gave no dramatic speculative boost.

Quality (same probes as 08-26):

| | MoE A3B @ M5 Max | dense 27B @ 1×3090 |
| --- | --- | --- |
| tool fidelity | 8/8 | 7/8 (`terse`: finish=length again) |
| API correctness | 8/8 | **6/8** (missed `getReference` + `cascade`) |
| chain | correct | correct, 3 turns |

**The dense is not more accurate — it is less.** The intuition "dense, more active
params ⇒ smarter" does not hold for *this* model: qwen3.8-27b scores below the
qwen3.6-35b-a3b MoE on both fidelity and API knowledge, while decoding 2–3× slower and
drawing ~350 W against ~100 W. Different generations and architectures, so this is a
setup-vs-setup verdict, not "dense < MoE" in general — but for the choice in front of
us it is unambiguous.

**Verdict for this workflow: keep the M5 Max + MoE.** It wins on speed (142 vs 53),
quality (8/8 vs 6–7/8) and power (~100 W vs ~350 W) at the same time. A 3090 — one or
two — earns its place only for models that do not fit the Mac's effective bandwidth, or
for batched multi-user serving; neither is this project. The dense 27B on a single 3090
is strictly dominated here.

Caveats: single run; the `terse` length-overflow is the same unbounded-reasoning config
as the IQ4_NL entry; the `getReference` miss was a 1024-token over-reasoned wrong answer.

### 2026-09-02 — Three oMLX models head to head: A3B vs Qwen3-Coder-Next vs Mistral-119B

Compared `Qwen3.6-35B-A3B-MTPLX`, `Qwen3-Coder-Next-mxfp4-mlx` and
`Mistral-Small-4-119B-2603-4bit`, all served by oMLX on the M5 Max: prefill + decode
at 20k/50k/100k, plus the 08-26 quality batteries. Two protocol notes below matter.

**Throughput (prefill cold gen=1; decode isolated from two WARM calls, gen=8→208):**

| model | ctx | prefill tok/s | decode tok/s |
| --- | --- | --- | --- |
| Qwen3.6-A3B | 20k / 50k / 100k | 3033 / 2493 / 1268 | 119 / 94 / 69 |
| Qwen3-Coder-Next | 20k / 50k / 100k | 1226 / 1827 / 1442 | **256 / 210 / 175** |
| Mistral-119B | 4k | 1524 | 225 |
| Mistral-119B | ≥ 8k | — | HTTP 400 (context ceiling, no config) |

Two findings. **Qwen3-Coder-Next decodes ~2× faster than the A3B** (256 vs 119 at 20k)
and still holds 175 tok/s at 100k. And **Mistral-119B at 225 tok/s on 4k is not a dense
119B — it is a MoE** (only a fraction of params active per token); a true dense 119B
would crawl. But it 400s past ~4-8k because it has **no entry in
`~/.omlx/model_settings.json`** (only the A3B and the VLM are configured), so it runs on
a small default context window — unusable for this workflow's 20k+ contexts until given
a config with a raised `max_context_window`.

**Quality (same probes as 08-26; B/C needs ctx~20k, so the 119B could not run it):**

| | Qwen3.6-A3B | Qwen3-Coder-Next | Mistral-119B |
| --- | --- | --- | --- |
| tool calls well-formed | 7/8 | **8/8** | n/a (400 at 20k) |
| **arguments exact** | **7/8** | 4/8 | n/a |
| API correctness (E) | 7/8 | 7/8 | **8/8** |
| chain (F) | correct | correct | correct |
| median out tok | 327 | 99 | tiny (API 1-15 tok) |

**Verdict for this workflow: keep Qwen3.6-A3B.**
- **Qwen3-Coder-Next** is the speed king and wonderfully terse, but its failure is
  disqualifying for a code-editing agent: 8/8 well-formed calls yet only **4/8 exact
  arguments** — it emits the call but with the wrong file/line. Fast and wrong is worse
  than slow and right.
- **Mistral-119B** has the best API knowledge (8/8, the only one) and is a fast MoE, but
  is context-capped without a config, so it cannot serve the 20k+ contexts the workflow
  needs. Worth a re-test *if* debridled (add a `model_settings.json` entry with a large
  `max_context_window`) — the open question is whether its knowledge edge survives at
  100k and justifies its 26 s cold load.
- **Qwen3.6-A3B** is slower in raw decode but the **most accurate** (7/8 exact args, 7/8
  API), runs cleanly to 100k, and has the best prefill. Correctness on arguments is what
  an agent that edits real files lives or dies by.

**Caveats:** single run, noisy per-probe medians. First throughput pass reported `nan`
decode — the method compared a cold call against a warm one (oMLX's SSD prefix cache
made the warm gen=408 finish *before* the cold gen=1); fixed by warming first, then
timing two warm calls. Prefill figures are cold (first, cache-sensitive) and indicative,
not steady-state. The 119B's `A3B`-less name hides that it is a MoE — confirmed only by
its decode speed, not by its card.

### 2026-09-02 (follow-up) — 119B debridled, and the "exact args" doubt settled

Gave the 119B a `model_settings.json` entry with `max_context_window: 131072` (backup
kept, `enable_thinking: false`). **Alone** it answers at 20k, but **in a real pool it is
unusable**: oMLX keeps 2 models resident (health `loaded=2`, 93 GB), so the 119B (71 GB)
plus any second model exceeds RAM and it 400s / no-usage-OOMs at 20k. Its API knowledge
is 8/8 on short prompts, but throughput and tool-fidelity at 20k are impossible while
another model co-resides. And oMLX did **not** drop it on idle — no HTTP unload endpoint
exists (all 404), 123/128 GB stayed pinned through a 2-min idle. The app's **Restart
button** is the only clean release (server is a child of `oMLX.app`).

**The exact-args doubt, settled by logging got-vs-want:**
- **Qwen3-Coder-Next hallucinates references.** On `nested` and `terse` it replaced the
  requested file with a REAL file from the 20k context (`DataStoreResolver.java`,
  `JtaTransactionBridge.java`) instead of the literal instruction. 6/8 exact — a real
  defect, not a scoring bug, and disqualifying for a code agent: it would file a finding
  against the wrong file.
- **Qwen3.6-A3B: got == want on all eight** (8/8 valid, 8/8 exact, 8/8 API). Not
  distracted by the context.

Throughput caveat: this second pass's prefill/decode were cache artefacts (it read
decode 1929 tok/s — impossible; oMLX's prefix cache made the "warm" calls near-instant).
The trustworthy figures stay the first fresh pass: A3B decode 110/94/78 and Coder
~256/210/175 at 20k/50k/100k.

**Verdict, reinforced: keep Qwen3.6-A3B.** The Coder is faster but hallucinates file/line
refs under context; the 119B is a memory trap that cannot co-reside with the working
model. Argument accuracy is what an editing agent lives on, and only the A3B holds 8/8.

### 2026-09-02 (solo) — Mistral-119B alone: capped by RAM, not by quality

User restarted oMLX with only the 119B loaded (71 GB, system at 127/128 GB). Even alone:
- **20k works; 50k and 100k both 400.** With 71 GB of weights resident, only ~22 GB is
  left for KV, and a 50k+ context's KV for a 119B does not fit. The model tops out around
  20-40k of context on this 128 GB box — a hard **RAM wall, not a config one**. It cannot
  serve the workflow's 100k contexts. (decode read `nan` again — the unique-prefix trick
  still hit oMLX's KV cache; moot, the RAM wall settles it.)

Quality with args logged: **7/8 exact** — the one miss is `terse`, where it too swapped
the asked `H.java` for a real context file (`JtaTransactionBridge.java`), keeping line 9.
Better than the Coder (6/8, hallucinates on `nested` AND `terse`), below the A3B (8/8).
API 7/8 this pass (missed `flush-no-tx`; run-to-run variance).

Across all three, only **Qwen3.6-A3B is fully robust to the context** (8/8 exact args);
the Coder and the 119B both get pulled toward real files under a 20k codebase on the most
elliptical probes. Final answer on the "strange args": the effect is real, model-side,
and the A3B is the only one immune.

**Definitive verdict: Qwen3.6-A3B.** The 119B is disqualified not on knowledge (7-8/8)
but on physics — 71 GB of weights leave no room for large-context KV on a 128 GB machine.
