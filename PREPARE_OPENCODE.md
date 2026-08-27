# PREPARE_OPENCODE.md

How mansart-jakarta-persistence is set up to be implemented by a **local** model.

The engineering goal is Jakarta Persistence 3.2 with the official TCK green. The
second goal is a demonstration: **implementing a Jakarta specification with a
locally-hosted LLM is feasible**, provided the work is decomposed and the context
is calibrated rather than left to chance. This document records every setting, the
measurements that justify it, and how to run a session.

Everything here lives in the repository: `opencode.json`, `.opencode/agents/`,
`.opencode/commands/`, `.opencode/skills/`, `.opencode/OPERATING.md`, `AGENTS.md`,
the pilot files `mansart-jakarta-persistence/{PLAN,TASKS,STATUS}.md`, and the
measurements in `BENCH.md`.

**If you are here to use the setup rather than to understand it, go to §13** —
the operating manual. Sections 1 to 12 explain why each setting is what it is, and
§14 lists every trap that cost time.

---

## 1. The model

**`Qwen3.6-35B-A3B-MTPLX-Optimized-Speed`** (`Youssofal/…` on Hugging Face),
served by oMLX at `http://127.0.0.1:8000/v1`.

| property | value |
| --- | --- |
| architecture | `qwen3_5_moe_text`, 40 layers, 256 experts, 8 active |
| parameters | 35 B total, ~3 B active |
| quantisation | 4-bit affine, group size 64 (body); MTP sidecar 4-bit, group 32 |
| attention | hybrid — `full_attention_interval = 4`, so 10 full-attention layers + 30 gated-delta-net layers |
| `max_position_embeddings` | 262 144 |
| weights on disk | ~19.5 GB + 555 MB MTP sidecar |
| speculative decoding | native MTP sidecar, `mtp_depth_max = 3` |

**The MTP sidecar does not work here — leave `mtp_enabled` at `false`.** The build
ships one, and its manifest claims 0.886 acceptance at depth 1 for ×1.465, but oMLX
cannot load it: `mtp_enabled: true` routes to the VLM engine and the load fails with
`Received 2321 parameters not in model`. The reason is the next row of this table —
the model is multimodal on paper but oMLX serves it text-only, so the MTP path tries
to build a VLM from a text checkpoint. Measured with the sidecar off: **138.1 tok/s**,
which is exactly the figure the manifest attributes to MTP being *on*. Nothing is
being lost.

`Qwen3.6-35B-A3B` is a vision-language model — 333 `vision_tower` tensors, a full
`vision_config`, image and video preprocessors. **You cannot use any of it.** oMLX
classifies it correctly (`type: vlm, engine: vlm`), but the mlx-vlm loader rejects
the checkpoint, the VLM engine stops, and oMLX falls back to the text engine — which
is precisely why the model works at all. An `image_url` request comes back with
"I don't see any screenshot attached", verified in both content formats. Hence
`modalities: text-only` and `attachment: false` in `opencode.json`.

The MTPLX conversion is the single cause of both losses. If screenshot input ever
matters, the answer is a **separate** VLM rather than fixing this one: see
§5 on wiring a dedicated vision subagent.

Why this model rather than the 6-bit or 8-bit Qwen3.6 already installed: the
sidecar. A 4-bit body that decodes at 105 tok/s measured, on a specification task
where the bottleneck is turn latency across dozens of tool calls, beats a 6-bit
body at 65 tok/s. Quality is recovered structurally — by small task cards and a
mandatory validation gate — not by paying for more bits per weight.

### Sampling

`mtplx_runtime.json` recommends **temperature 0.6, top-p 0.95, top-k 20** for both
the target and the draft model. That is Qwen's thinking-mode profile and it is what
is configured. Never run this model near-greedy with thinking enabled: it collapses
into repetition loops.

---

## 2. oMLX configuration

Applied to `~/.omlx/model_settings.json` under the key
`Youssofal--Qwen3.6-35B-A3B-MTPLX-Optimized-Speed`:

| setting | value | reason |
| --- | --- | --- |
| `is_default` | `true` | the model OpenCode gets when nothing is specified |
| `is_pinned` | `true` | stays resident; avoids a ~3 s reload on every cold session |
| `max_context_window` | `131072` | server headroom — oMLX must never be the thing that truncates |
| `max_tokens` | `16384` | see the arithmetic in §3 |
| `thinking_budget_tokens` | `4096` (`thinking_budget_enabled: true`) | leaves ≥12 288 tokens for the tool call itself |
| `enable_thinking` | `true` | spec work needs reasoning; the budget keeps it from eating the turn |
| `force_sampling` | `true` | pins 0.6 / 0.95 / 20 regardless of what the client sends |
| `mtp_enabled` | **`false`** | oMLX cannot load this build's sidecar; `true` makes the model fail to load entirely (see §1) |
| `max_tool_result_tokens` | `8000` | server-side backstop against a Maven log flooding the window |
| `turboquant_kv_enabled` | `false` | KV at 128k is ~2.6 GB in fp16; long-context quality is the weak point, do not quantise it away |

Server-level settings that matter (`~/.omlx/settings.json`):
`sampling.max_context_window: 131072` — the server silently caps any model whose
own window is unset, so this must not be lower than the model's;
`preserve_mid_system_cache: true` — keeps the prompt prefix cached across turns,
which is what makes a 42k-token conversation affordable;
`integrations.opencode_model` set to this model.

> **Gotcha, learned the hard way.** A running oMLX rewrites
> `~/.omlx/model_settings.json` from its in-memory state. Edit the file with the
> app **stopped** (`kill <pid>` works; the AppleScript quit shows a dialog), then
> relaunch. A backup is written next to it before every edit made by this setup.

### KV-cache arithmetic

10 full-attention layers × 2 KV heads × 256 head-dim × 2 (K and V) × 2 bytes
≈ **20 KB per token**. The 30 gated-delta-net layers hold a constant-size
recurrent state. So 128k of context is ~2.6 GB of KV on top of 19.5 GB of weights.

**Memory is not the constraint.** That matters, because it means the reason to
keep the context small is quality and latency — not fitting in RAM.

---

## 3. Context calibration

This is the part that decides whether the experiment works.

### Measurement

A ladder run against the configured server: N tokens of real mansart Java sources,
then one instruction requiring a well-formed tool call with four required
arguments. Recorded 2026-08-25 on this machine.

| prompt tokens | wall | prefill | tool call valid | output tokens |
| --- | --- | --- | --- | --- |
| 7 336 | 5.0 s | 1 473 tok/s | yes | 394 |
| 21 188 | 7.0 s | 3 042 tok/s | yes | 291 |
| 41 894 | 11.7 s | 3 584 tok/s | yes | 289 |
| 62 256 | 23.8 s | 2 616 tok/s | yes | **1 093** |
| 83 750 | 21.2 s | 3 941 tok/s | yes | 304 |

Decode re-measured later at **138.1 tok/s** with prefill isolated; the earlier ~105 figure included prefill in the decode window. MTP was never active (§1).

Two conclusions, both of which changed the configuration:

1. **The old cliff is gone.** The previous 6-bit Qwen3.6 build truncated tool
   calls past roughly 60k tokens — EOS emitted in the middle of a `write`. This
   build stayed valid at 84k. The failure that shaped the earlier setup does not
   reproduce here, and the configuration should not be built around a ghost.
2. **Degradation appears as reasoning inflation, not as breakage.** At 62k the
   model spent 1 093 output tokens on a task that cost 289 tokens elsewhere. Long
   context does not make this model wrong; it makes it hesitant. Hesitant costs
   wall-clock and invites drift into speculation.

So the binding constraint is **latency and decision quality, not capacity**. A 42k
prompt costs ~12 s of prefill on a cache miss; an 84k prompt costs ~21 s. Across a
40-turn session that is eight minutes against fourteen — and the fourteen-minute
version makes worse decisions.

### The output budget

The old truncation bug is worth understanding, because the fix is the whole
arithmetic. Previously `max_tokens` was 12 288 with a thinking budget of 8 192,
leaving only 4 096 tokens for the actual tool call. A large `write` exceeded that,
the JSON was cut mid-argument, the call never closed, and the turn was lost. It
looked like a context-length problem. It was a budget-split problem.

The invariant:

```
generation cap  >  thinking_budget  +  largest_tool_call
```

**Which cap applies was verified by capturing the actual HTTP request** (§4.1).
OpenCode sends `max_tokens` equal to its own `limit.output`, and that overrides
the model's server-side `max_tokens`. So oMLX's 16 384 is only a ceiling; the
binding number is OpenCode's:

| knob | value | who wins |
| --- | --- | --- |
| oMLX `max_tokens` | 16 384 | ceiling only — never reached |
| OpenCode `limit.output` | 12 288 | **sent as `max_tokens` on every request** |
| oMLX `thinking_budget_tokens` | 4 096 | subtracted from the above |
| room left for the tool call | **8 192** | ≈ 600 lines of Java |
| OpenCode `limit.context` | 131 072 | display only — see below |

8 192 tokens for a tool call is twice what the broken configuration left, and far
above the ~150-line write ceiling the agents are told to respect. Raising
`limit.output` to 16 384 would buy more room per turn but would also raise the
reserve and shrink the window by the same amount; 12 288 is the better trade.

### The wall: where it is, and who puts it there

This section has been wrong twice, in opposite directions. What follows is what was
actually observed, in order.

**First claim — a hard wall at 61 440 enforced by `compaction.auto: false`.** Wrong.
A session was seen sitting at **144 %** and working fine: it peaked at 106 941 tokens
against a declared `limit.context` of 73 728. Two checks explained it:

- **OpenCode never computes overflow itself.** `isContextOverflowError()` requires an
  **HTTP 400** whose message contains "prompt is too long" or "tokens … maximum".
  `limit.context` drives the percentage display and — when auto is on — the
  compaction trigger. That is all it does.
- **oMLX served 139 417 and then 165 033 prompt tokens** against a model declaring
  `max_context_window: 131072`, `HTTP 200` both times, with a canary at position 0
  recalled correctly at 165k.

**Second claim — therefore no wall exists anywhere.** Also wrong, and the reason is
the trap from §3's model-assignment note: at the time of that test, the *served* id
had **no entry** in `model_settings.json`, so no window was enforced. Once
`max_context_window: 131072` was set explicitly on the served id, oMLX started
refusing:

```
POST /v1/chat/completions → 400:
  Prompt too long: 131344 tokens exceeds max context window of 131072 tokens
```

OpenCode classified it correctly and, with `auto: false`, stopped the session with
the error visible. **An explicitly configured window is enforced; the server default
was not.** The wall exists because it was configured, and it is where it was put.

### Why the wall is kept, and auto-compaction is not

Both failure modes have been measured, and they are not equivalent.

| | what it looks like | recoverable |
| --- | --- | --- |
| auto-compaction on | tool-call rate falls from 98 % to **0 %** over four turns; the model narrates calls instead of emitting them; the session looks merely idle | only by noticing, which takes a while |
| wall, auto off | an explicit `Prompt too long` error, session stops | immediately — state is on disk |

A loud failure beats a silent one. That is the whole argument, and the production
numbers add a second one: **long sessions are expensive regardless of whether
anything breaks.** Measured over 785 production requests in one day:

| prompt size | median decode | median turn |
| --- | --- | --- |
| 0–20k | **88.6 tok/s** | 3.6 s |
| 40–60k | 79.6 tok/s | 6.2 s |
| 80–100k | 68.4 tok/s | 7.1 s |
| 120–140k | **55.1 tok/s** | 19.1 s |

A session at 130k runs at 62 % of the speed of one at 20k. Letting the window grow
costs time even on the happy path.

**Reaching the wall is a decomposition failure, not a configuration one.** 131k on
one card means the card was too large, or the session took a second card, or context
leaked in — whole files instead of `lsp`, raw Maven output instead of `ctx`.

### What the evidence actually says about long context

The small-window doctrine was inherited from the previous 6-bit build, which
truncated tool calls past ~60k. On this build that cliff does not exist:

| measurement | result |
| --- | --- |
| single-shot ladder, 83 750 tokens | tool call structurally valid |
| canary at position 0, 165 033 tokens | recalled correctly |
| **real agentic session, peak 106 941 tokens, 0 compactions** | **104 / 107 assistant turns emitted a tool call — 97 %** |

That last row is the one that matters: it is the agentic-loop measurement the
single-shot ladder could not provide, and it independently confirms the diagnosis
below — the session that collapsed to 0 % tool calls was killed by *compaction*,
not by length.

`limit.context` is therefore set to **131 072**, matching what oMLX declares, so the
percentage means something again. The two-window trick that used to declare the same
weights twice under both of oMLX's ids was removed with it: redundant now.

The one negative signal that stands is the reasoning inflation measured at 62 256
tokens in the ladder (1 093 output tokens for a task costing 289 elsewhere). It did
**not** reproduce in the 107k session. Treat it as a reason to keep sessions short
by discipline, not as a number to encode in config.

The remaining real cost of a long window is prefill: 68 s at 165k from cold. Prefix
caching amortises successive turns, but any cache break is paid in full.

All agents share one model and one window; see the benchmark in `BENCH.md` for why
the higher-precision candidates were rejected.

### Why auto-compaction is OFF

`compaction.auto` is set to **`false`**, which is not the default. The reason is
measured, not theoretical.

The first real session ran cards JP-01 through JP-04 in one window: 236 messages,
four compactions. Its tool-call rate per segment:

| segment | assistant turns | with a real tool call |
| --- | --- | --- |
| before compaction #1 | 49 | 49 — 100 % |
| before #2 | 65 | 60 — 92 % |
| before #3 | 58 | 57 — 98 % |
| before #4 | 45 | 44 — 98 % |
| **after #4** | 4 | 0 — **0 %** |

Not a gradual decline — a cliff. After the fourth compaction the model stopped
emitting tool calls entirely and started *describing* them instead:

```
"**LOADING skill: mansart-jpa**  Let me load the architecture skill, then read
 TASKS.md JP-04 details and start:"
```

Then EOS. OpenCode sees no tool call, logs `exiting loop` at step 1, and the turn
ends. Pressing enter again appends another paragraph of prose, which reinforces the
pattern. The session is dead and looks merely idle.

The mechanism: the turn immediately following a compaction is, by construction, a
long text-only summary — 2 774 output tokens of prose here, not one tool call. If
the next few user messages are conversational rather than commands, the recent tail
is now entirely prose, and a 3B-active model imitates its recent tail harder than it
follows its system prompt.

Auto-compaction therefore let a session **degrade silently into chat mode** — the
worst possible failure, because it looks like the model merely being idle. With
`auto: false` the compaction never happens, and the measured 107k session shows what
that buys: 97 % tool calls, no collapse, right through to the commit.

It does **not** create a stopping point (see above — nothing does). Keeping sessions
short is a discipline the agent prompts enforce by asking, not a limit the harness
imposes. Work is never lost either way: files are written as the session goes, and
`STATUS.md` carries the handover.

The paired fix is in `.opencode/OPERATING.md`: *never announce a tool call in
prose — make the call*. That is the exact failure signature to watch for.

### The shape of a healthy session

Not a limit — a shape to aim for. The first line is measured, not estimated (§5.1):

```
system prompt (agent + AGENTS.md + OPERATING.md + skills index)  ~5.4 k
tool schemas + LSP diagnostics                                     ~3 k
the task card                                                      ~1 k
working set: at most 4 Java files                                 ~12 k
tool output (grepped, never raw)                                  ~10 k
headroom for reasoning and edits                                  ~20 k
```

A session that stays near that shape finishes its card in about 50k tokens and
never touches the interesting part of the window. A session that wanders — reading
whole files instead of using `lsp`, letting Maven logs in, taking a second card —
gets to 107k. Both work on this build. The first is three times faster.

### Model assignments

**One text model, every agent**: `Qwen3.6-35B-A3B-MTPLX-Optimized-Speed`, 131 072
window, for all twelve. Plus **one vision model**,
`Qwen3-VL-8B-Instruct-MLX-5bit` (6.3 GB, 5-bit affine g64, 36 text layers, 256k
positions), used by the `vision` agent alone — because the text model's own vision
tower is unreachable (§1). It is not pinned: it loads in ~2 s when needed and costs
nothing the rest of the time. Its sampling comes from its own
`generation_config.json` — temperature 0.7, top-p 0.8, top-k 20, no thinking.

Verified working on real screenshots: `3%` and `863.0 MB of 27.8 GB` read correctly
off a download panel, a shell command transcribed character-exact, 1–4 s per image,
~500–1 100 prompt tokens. One caveat measured at the same time: it misread
`samuelfaj` as `samuelselfaj`. **Verify identifiers it transcribes** — a one-character
slip in a package name or a class name is invisible and expensive.

Earlier revisions split the roster across an
`…-Optimized-Balance` build for the review agents and an 8-bit for `@thinker`; the
2026-08-26 benchmark (`BENCH.md`) found no quality difference and a 2.3× speed
penalty, so both were dropped and their weights deleted.

> **The trap that cost the most time here.** oMLX exposes a model under *two* ids
> when it exists in both stores — the repo-qualified `Youssofal--Qwen3.6-…` for the
> HuggingFace cache copy, and the bare `Qwen3.6-…` for the `~/omlx-models` copy.
> They are different physical files. Per-model settings in
> `~/.omlx/model_settings.json` are keyed by id, so tuning one id leaves the other
> on server defaults — temperature 1.0, `top_k` 0, no thinking budget, no MTP. That
> is exactly what happened here for several days: `opencode.json` used the bare id
> while every setting sat on the qualified one. Check which path the server actually
> loads before believing any tuning is live:
>
> ```bash
> grep -a 'BatchedEngine loaded' ~/.omlx/logs/server.log | tail -3
> ```

---

## 4. OpenCode configuration

`opencode.json` at the mansart root. Not the global `~/.config/opencode/opencode.json`
— **a running OpenCode rewrites the global file from its in-memory state**, so
durable configuration belongs to the project.

Beyond the model declarations above:

```jsonc
"compaction": { "auto": false, "reserved": 12288, "preserve_recent_tokens": 12000 },
"lsp": true,
"permission": { "external_directory": { "/tmp/**": "allow", "/private/tmp/**": "allow",
                                        "/Users/yblazart/.m2/**": "allow" },
                "doom_loop": "deny" },
"default_agent": "jpa-dev",
"instructions": ["AGENTS.md", ".opencode/OPERATING.md"]
```

`doom_loop: deny` matters with a local model: it stops a repetition loop from
burning an hour of GPU.

The `ctx` MCP server (context-mode) is declared globally and is used by every
agent to route build and test output. It is the difference between a Maven log
costing 4 000 tokens and costing 40.

### 4.1 Verifying what is actually sent

Configuration that is never inspected is configuration that is assumed. A small
logging proxy in front of oMLX — OpenCode's `provider.omlx.options.baseURL`
pointed at it for one run — captures the exact request body:

```bash
# proxy on 127.0.0.1:8099 forwarding to 127.0.0.1:8000, dumping the first
# /chat/completions body to disk; then:
opencode run --agent jpa-dev "Say OK."
```

That is how the two corrections in this document were found:

- **`max_tokens` is OpenCode's, not oMLX's** — see §3. The arithmetic was wrong
  until the request was read.
- **`topP` is not a valid frontmatter field; the key is `top_p`.** OpenCode
  silently routes unknown frontmatter fields into `options`, so `topP: 0.95` was
  being forwarded to the server as a meaningless `topP` parameter while the real
  `top_p` stayed at its default of 1. Fixed in `jpa-dev`, `spec-reader` and
  `thinker`; the capture now shows `top_p: 0.95`.

Also worth knowing: **`temperature` is not sent at all** by this provider path.
That is harmless here only because oMLX runs with `force_sampling: true`, which
pins 0.6 / 0.95 / 20 server-side regardless of what the client asks for. On a
server without that switch, the agents' `temperature:` would be silently ignored.

Re-run the capture after any change to sampling, model limits or agent
frontmatter. It costs one trivial turn.

---

## 5. Agents

`.opencode/agents/`. One primary, eleven subagents, one dual-mode (`vision`). The design principle: **a subagent
burns its own context, not the primary's.** Delegation is the main context-control
mechanism, ahead of compaction.

| agent | mode | model window | role |
| --- | --- | --- | --- |
| `jpa-dev` | primary | 128k | the developer. Full-auto edits and shell. One card per session. |
| `spec-reader` | subagent | **128k** | read-only spec and TCK-source oracle. One question, one cited answer, ≤40 lines. |
| `tck-runner` | subagent | 128k | runs the TCK, reports real integers, never edits main sources. |
| `sonar-runner` | subagent | 128k | starts the Sonar container, scans, reads the gate from the **API** not the log. Reports issues on new code only. |
| `codegen` | subagent | 128k | owns APT / Maven-plugin / runtime-fallback generation. |
| `auditor` | subagent | 128k | anti-drift: stubs, reflection, TCK leakage, disabled tests, module violations. |
| `explore` | subagent | 128k | scout. Returns `file:line` pointers, never file contents. |
| `tracker` | subagent | 128k | the only writer of `STATUS.md` and `TASKS.md`. |
| `module-guardian` | subagent | 128k | `module-info.java` review. |
| `dependency-gatekeeper` | subagent | 128k | enforces the zero-dependency rule on every POM change. |
| `virtual-threads-reviewer` | subagent | 128k | pinning, `ThreadLocal`, platform pools, connection lifetime. |
| `thinker` | subagent | 128k | one hard decision, after two failed attempts. Returns a decision, not an essay. |
| `vision` | **all** | 128k (**Qwen3-VL 8B**) | the only agent that can see. Turns a screenshot into exact text. Select it to paste an image, or delegate so the image never enters the coding window. |

### Delegation is what makes the restrictions real — and `@name` does not delegate

Verified by probe, twice:

- a subagent delegated through the `task` tool genuinely loses what its frontmatter
  removes. `@auditor` asked to write `/tmp/auditor-probe.txt` produced no file (the
  `write` tool is simply absent from its set), and asked to run a `bash` command
  outside its allowlist it answered "refused — the system injected a permission
  rule denying all bash calls". Both `tools:` and `permission.bash:` are enforced.
- **but `@name` in a prompt delegates nothing.** Typing `@auditor écris "test" dans
  /tmp/x.txt` in the TUI was answered *inline by the primary agent*, which ran the
  `echo` itself and then explained that the auditor is "a quality-review agent, not
  a general-purpose worker". `@` is a file-reference sigil in OpenCode; an agent
  name after it is just text.

The consequence matters more than it looks: work that should have run in a
restricted subagent instead runs in the primary agent's context, with the primary
agent's permissions, spending the primary agent's window. The `@x` notation used
throughout these prompts is a *convention meaning "emit a `task` call"*, and
`.opencode/OPERATING.md` now says so explicitly. To force a subagent reliably from
the outside, use a command with `agent:` + `subtask: true` — which is what `/audit`,
`/spec`, `/tck`, `/sonar` and `/status` already do.

An earlier revision of this document claimed the opposite — that a global
`*: allow` rule neutralised per-agent restrictions. That was wrong, and it came
from a bad test: forcing a `mode: subagent` agent to run as the top-level agent via
`opencode run --agent` falls back to the default agent's tool set.

`jpa-dev` runs **full-auto** — the equivalent of Claude Code's auto-accept mode:
`edit: allow`, `bash: "*": allow`. Destructive git operations (`reset`, `clean`,
`restore`, `checkout --`, `push`) and `rm -rf` stay on `ask`. Being allowed to run
everything is paired with an explicit obligation to verify everything: the agent
prompt forbids reporting a result that was not observed in tool output.

Review agents have `write`, `edit` and `patch` disabled and a bash allowlist
restricted to `grep`, `sed -n`, `ls`, `find` and read-only `git`. They cannot
"helpfully" fix what they are auditing.

`module-guardian` is the workspace's `jpms-guardian` under a name that respects the
terminology rule — the abbreviation is banned in prose and identifiers.

### 5.1 What the system prompt actually contains

An agent's markdown body becomes its `prompt`, and — verified by capture (§4.1) —
**it replaces OpenCode's built-in system prompt entirely.** It does not append to
it. The built-in `build` agent ships 96 lines covering tone and conciseness,
proactiveness, following existing conventions, code style, task procedure, tool
usage policy and the `file:line` reference convention. A custom agent gets none of
that.

For a frontier model that matters little. For a 3B-active local model it matters a
great deal: those are exactly the rules that keep it terse, stop it re-reading
files it already has, and make it batch independent tool calls.

Two of the built-in rules are also actively wrong here — "DO NOT ADD ***ANY***
COMMENTS unless asked" contradicts a project whose Javadoc is part of the
deliverable, and its npm-flavoured lint/typecheck advice does not apply to a Maven
reactor. So inheriting it wholesale would not have been right either.

The resolution: `.opencode/OPERATING.md`, injected into **every** agent through
`instructions`. It carries the operational scaffolding — conciseness, `file:line`,
"never assume a library is available" (which reinforces the zero-dependency rule),
Javadoc policy, `todowrite` as the in-session decomposition tool, batching
independent calls, `lsp` before `grep`, routing logs through `ctx`, and the
obligation never to claim an unobserved result — written for this repository
rather than for a generic assistant.

Measured composition of `jpa-dev`'s system prompt, in order:

| part | lines | source |
| --- | --- | --- |
| agent prompt | 0–86 | `.opencode/agents/jpa-dev.md` body |
| environment block | 87–95 | OpenCode (model id, cwd, git status) |
| global instructions | 96–98 | `~/.claude/CLAUDE.md` |
| engineering contract | 99–219 | `AGENTS.md` |
| operating rules | 220–303 | `.opencode/OPERATING.md` |
| skills index | 304–356 | discovered `SKILL.md` frontmatter |

**356 lines, 19 429 characters, ≈5 400 tokens** — 4 % of the 128k window,
which is the budget line in §3. Skill *bodies* are not included; they are pulled
in on demand by the `skill` tool, which is the point of putting them in skills
rather than in the prompt.

---

## 6. Commands

`.opencode/commands/`, invoked as `/name`.

| command | what it does |
| --- | --- |
| `/next [JP-xx]` | opens a session: **warms jdtls and indexes the TCK**, reads `STATUS.md`, picks one `TODO` card, states the failing test and the ≤4 files before writing anything |
| `/gate` | the validation gate: build, unit tests, `@auditor`, the card's TCK client, and a Sonar scan of the touched module. Returns `GATE: PASS` or `FAIL`. Required before any card becomes `DONE` |
| `/tck [Client\|all]` | delegates a TCK run to `@tck-runner` |
| `/sonar [module\|all]` | delegates a SonarQube scan to `@sonar-runner` (§12) |
| `/tck-fix <Client>` | the fix loop: read the test source → ask `@spec-reader` if needed → reproduce as a local unit test → implement the *spec* → re-run → `/gate` |
| `/audit [scope]` | anti-drift audit of the branch |
| `/spec <question>` | one question to the spec oracle |
| `/status` | where the project stands, from `STATUS.md` and `TASKS.md` only |
| `/session-end` | hands real numbers to `@tracker`, updates state, commits |
| `/see <question>` | reads an attached image with the vision model and returns its exact text (§5) |
| `/log-bug`, `/log-bench` | append to `BUG.md` / `BENCH.md` in the workspace format |

---

## 7. Skills

`.opencode/skills/`, loaded on demand rather than living in the system prompt.

- **`mansart-jpa`** — reactor layout, what is shared with `mansart-jakarta-data`
  and `mansart-transactions`, build commands, definition of done.
- **`mansart-jpa-tck`** — TCK scale and package distribution, the out-of-reactor
  runner, how to read surefire instead of the console, how to extract one test
  method from the sources jar, and the official-DDL schema trap.
- **`vidocq-codegen`** — the three-tier doctrine, in full.
- **`context-discipline`** — the measurements above plus the five working rules.
  Load it when a session starts feeling heavy.

Only the frontmatter (name + description) of each skill sits in the system prompt;
the body is fetched by the `skill` tool when an agent decides it is relevant. That
is why long reference material belongs in a skill and short always-true rules
belong in `.opencode/OPERATING.md`.

---

## 8. Code generation: the three tiers

The rule the whole architecture rests on: **everything a framework would normally
discover by reflection is produced as code before it runs.**

1. **APT** — `mansart-persistence-processor`, `SourceVersion.RELEASE_25`. Sees the
   entities in the sources being compiled, emits Java sources: `_Entity` static
   metamodel, `EntityDescriptor` (mapping metadata as constants), and accessors
   doing direct `getfield`/`putfield`. Covers the overwhelming majority of cases.
2. **Maven plugin** — `mansart-persistence-maven-plugin`, bound to
   `process-classes`, using the **Class-File API** (`java.lang.classfile`, JEP 484).
   An `@Entity` inside a dependency jar was compiled elsewhere and APT never sees
   it; the plugin scans the dependency classpath and emits the same shapes as
   `.class` files. Lazy-association proxies for external entities are generated
   here too — real subclasses, never `java.lang.reflect.Proxy`.
3. **Runtime Class-File API** — `ClassFile.of().build(...)` plus
   `MethodHandles.Lookup.defineHiddenClass` at `EntityManagerFactory` bootstrap,
   for an entity neither tier reached. An escape hatch, not a design: it logs a
   warning naming the class and pointing at the plugin.

Tiers 1 and 2 must be behaviourally identical, enforced by
`mansart-persistence-external-it` running the same assertions against a tier-1
entity and a tier-2 entity loaded from `mansart-persistence-external-lib`.

**This is not speculative.** All three shapes already ship in this repository for
Jakarta Data: `mansart-data-processor`, `mansart-data-maven-plugin`
(`GenerateExternalRepositoriesMojo`), `mansart-data-external-lib`,
`mansart-data-external-it`. The persistence work mirrors a proven pattern rather
than inventing one.

Banned: `java.lang.reflect.Proxy`, ASM, Byte Buddy, cglib, `Field.setAccessible`,
`Class.forName` on a user entity, `MethodHandles` resolution against a user class
on a hot path.

---

## 9. Decomposition: the answer to "the code is enormous"

A larger context does not help a local model; past ~50k it costs latency and
decisiveness without buying accuracy (§3). The strategy is therefore to make each
unit of work small enough that a large context is never needed.

**Three levels:**

- **Milestone** (`PLAN.md`) — the planning unit. M0 to M9, each ending on a
  *measured* TCK number for named client packages. A milestone is never "done
  because it compiles".
- **Card** (`TASKS.md`, `JP-xx`) — the work unit, sized for one focused session: one
  behaviour, **at most 4 files**, exactly one proving test, explicit dependencies.
- **Session** — one card. Opened with `/next`, closed with `/session-end`.

**Only the next milestone is expanded into cards.** `@tracker` expands the
following one when the current closes. Planning ten milestones of cards up front
would be fiction, and re-reading them would cost context every single session.

**State lives in files, not in the window.** `STATUS.md` is capped at 60 lines and
is the entire handover between sessions: next card, what is in flight, the one trap
to know, real numbers, and the last 20 log lines. A new session seeded from
`STATUS.md` costs ~1k tokens and is strictly better than a compacted one —
compaction discards exactly the tool output that is about to be needed and keeps
the prose that is not.

**Discovered work becomes a new card**, appended by `@tracker`, never merged into
the card in flight. Scope creep inside a session is how a healthy window becomes a
compacted one.

The TCK gives the decomposition a natural grain: 269 client classes, ~1 745 test
methods. `/tck-fix <Client>` is one card's worth of work almost by construction.

---

## 10. Using the LSP properly

OpenCode ships **jdtls** (already in `~/.cache/opencode/bin/jdtls`) and resolves the
project root by walking up to the aggregator `pom.xml`, so the multi-module reactor
is handled natively. `"lsp": true`.

**Why it was useless until now — and the fix.** A dead session's export showed
*one* `lsp` call (`documentSymbol`), returning `status: error`, followed by 147
`read`/`unzip`/`grep` calls. The model tried the LSP once, it failed, and it gave up
for the rest of the session — rational, and fatal. Two causes:

- **No `.sdkmanrc` in mansart** (the `CLAUDE.md` said one was "coming"). OpenCode
  inherited whatever Java was in the shell — Java 26 — for a project that builds at
  `release 25`. Fixed: `mansart/.sdkmanrc` now pins `java=25-tem` / `maven=3.9.16`,
  the same as vauban — but pinned to the **patch** release `25.0.3-tem`, not
  `25-tem`, for the reason below. Launch with `sdk env` before `opencode`.

  > **The non-obvious trap.** OpenCode detects the JDK by parsing `java -version`
  > with the regex `/"(\d+)\.\d+\.\d+"/` — it requires an `"X.Y.Z"` string. Modern
  > "major" JDKs print the version *without* dots: `25-tem` → `"25"`, `26` → `"26"`,
  > `21` → `"21"` — none match, so OpenCode concludes there is no Java and disables
  > jdtls silently ("no Java server configured"). Only a patch release prints the
  > matchable form: `25.0.3-tem` → `"25.0.3"`. This is an OpenCode bug; the
  > workaround is to pin a patch version. Verify after launch:
  > `pgrep -f 'org.eclipse.jdt.ls.core'` should show a process parented by opencode.
- **jdtls starts lazily and indexes 30 modules** (1–2 min), and — the part that
  cost an extra round — it spawns **only when a Java file is read**, not when the
  `lsp` tool is called ("LSPs will activate as files are read"). A bare
  `workspaceSymbol` hits a server that does not exist yet and returns empty. So
  `/next` warms it in the right order: **read one small `.java`** (a `module-info`)
  to trigger the spawn, index the TCK (a natural ~1 s pause), then call `lsp` and
  retry once if still mid-index. Every agent prompt also says a failed `lsp` means
  *not ready, retry* — never fall back to `read` on a single error.

Trust but verify: `pgrep -f 'org.eclipse.jdt.ls.core'` during a session tells you
whether OpenCode's jdtls is actually up (distinct from Claude Code's own).

The rule in every agent prompt: **`lsp` before `grep`, always.**

| question | operation | wrong answer |
| --- | --- | --- |
| where is this defined | `goToDefinition` | `find -name '*.java'` |
| who calls this | `findReferences` | `grep -r ClassName` |
| what is the signature | `hover` | reading 300 lines |
| what is in this file | `documentSymbol` | reading the file |
| find a type anywhere | `workspaceSymbol` | `grep -r` |

An `lsp` call costs ~100 tokens. Reading a Java file costs ~3 000. Over a session
that is the difference between finishing a card and compacting halfway through.
`grep` remains correct for XML, Markdown, properties and string literals.

**The ECJ caveat.** jdtls compiles with ECJ, not javac, and it does not run our
annotation processors the way the Maven build does. Its diagnostics are advisory:
fix them before building, but only `./mvnw` decides. A red squiggle on a generated
`_Entity` before the first Maven build is expected and must not be "fixed".

---

## 11. The TCK

`jakarta.tck:persistence-tck-spec-tests:3.2.1` is already installed in the local
M2, alongside `persistence-tck-dist`, `persistence-tck-common`, `dbprocedures` and
`sigtest-maven-plugin`. **269 client classes, ~1 745 test methods.** Pin 3.2.1; the
3.2.2 and 4.0.0 SNAPSHOTs move.

`mansart-persistence-tck` is **out of the reactor** — standalone POM,
`modelVersion 4.0.0`, no `<parent>` — like `mansart-data-tck` and
`mansart-transactions-tck`. Runner script `run-official-tck-persistence-3.2.sh`,
profiles `tck-run`, `tck-pg`, `tck-sig`, mirroring the Data runner.

Two rules that decide whether the number means anything:

- **The schema comes from the TCK's own DDL** (`persistence-tck/sql/<db>/…`), not
  from anything mansart generates. `setup*Data failed` is the single largest error
  source and it belongs to the harness, not to the provider. Card JP-06.
- **Only PASS counts.** ERROR → FAIL is not progress. Stubs that quieten a test are
  forbidden and `@auditor` rejects them. An honest 0/1745 baseline is worth more
  than an invented 40.

---

## 12. Quality: SonarQube on local Docker

The TCK says whether the implementation is *correct*. It says nothing about whether
it is maintainable. SonarQube covers the second half, and it has to run **often** —
a quality gate wired in after twenty thousand lines is a project; wired in on an
empty reactor it is five minutes. Hence card **JP-01b**, scheduled before JP-02.

### The instance

Container **`mansart-sonar`**, `sonarqube:community` 26.5.0, published on host port
**9001** (container 9000). Project key `vidocq-mansart-persistence`.

> **It has no volume mounted.** Its data lives in the container's writable layer.
> `docker stop` and `docker start` are safe. `docker rm mansart-sonar` destroys the
> project history and any token that was minted for it. The `sonar-runner` agent has
> `docker rm` and `docker volume` on `ask` for exactly this reason.

### Maven wiring

In `mansart-jakarta-persistence/pom.xml`, mirroring `vauban/pom.xml`:
`sonar.projectKey`, `sonar.java.source=25`, `sonar.host.url`,
`sonar.coverage.jacoco.xmlReportPaths`, and `sonar-maven-plugin 5.1.0.4751` in
`pluginManagement`. Scoped to the persistence sub-reactor — the delivered modules
are untouched.

JaCoCo needs nothing new: `vidocq-parent` already manages it behind a **`quality`**
profile, with `prepare-agent` and a `report` execution bound to `verify`. Two
consequences that cost an afternoon if you miss them:

```bash
./mvnw -ntp -Pquality -pl mansart-jakarta-persistence/<module> -am verify \
  org.sonarsource.scanner.maven:sonar-maven-plugin:sonar
```

- **`-Pquality`** or JaCoCo never runs at all;
- **`verify`**, not `test` — the XML report is produced at `verify`. Without both,
  Sonar reports 0 % coverage and you go hunting for a bug that is not there.

The exclusions matter more here than anywhere else in the workspace:
`**/generated/**,**/generated-sources/**,**/target/**`. APT emits the `_Entity`
metamodel and the descriptors, and the Maven plugin emits `.class` files for
external entities. Analysing generated code would drown the report in issues nobody
wrote by hand — and, worse, would make the model try to "fix" its own generator
output.

### Authentication

**SonarQube 26.5 does not allow anonymous analysis.** `sonar.forceAuthentication`
was removed in the 10.x line; a token is required. The runner tries without one and,
on 401/403, stops immediately with the exact instruction rather than retrying:

```bash
# http://localhost:9001 → My Account → Security → generate a token
export SONAR_TOKEN=<token>          # before launching opencode
```

### Where it hooks into the loop

| when | what | scope |
| --- | --- | --- |
| every card, inside `/gate` | `@sonar-runner` on the **module the card touched** | one module |
| on demand | `/sonar [module\|all]` | your choice |
| at milestone close | `/sonar all`, gate status recorded in `STATUS.md` beside the TCK number | whole sub-reactor |

A `BLOCKER` or `CRITICAL` **on new code** fails the gate. Pre-existing debt does
not: a card is accountable for the code it wrote, not for the backlog. That
distinction is what keeps the gate from becoming noise the model learns to ignore.

### The context rule, again

A scanner log is thousands of lines. `@sonar-runner` never reads it. It runs the
scan through the `ctx` tools and then queries the API, which answers in a few
hundred bytes:

```
/api/qualitygates/project_status?projectKey=vidocq-mansart-persistence
/api/issues/search?componentKeys=…&inNewCodePeriod=true&statuses=OPEN
/api/measures/component?component=…&metricKeys=new_coverage,new_violations
```

Same discipline as reading surefire XML instead of the Maven console. It is the
single reason a quality gate can run on every card without eating the window.

---

## 13. Operating manual

### 13.1 First time on this machine

Nothing here is optional; each line is something whose absence produced a bug
documented in §14.

```bash
sdk env                                    # in mansart/ — Java 25 + Maven 3.9.16
./mvnw -ntp install -DskipTests            # the reactor must be in the local M2
open -a oMLX                               # the model server; auto-starts on launch
export SONAR_TOKEN=<token>                 # http://localhost:9001 → My Account → Security
```

Check the server is serving what you think it is — this is the single most useful
diagnostic in the whole setup:

```bash
grep -a 'Discovered model' ~/.omlx/logs/server.log | tail -2
```

You want two lines: `Qwen3.6-35B-A3B-MTPLX-Optimized-Speed` (pinned, the text
model) and `Qwen3-VL-8B-Instruct-MLX-5bit` (on demand, vision).

### 13.2 A session

```bash
cd ~/projects/perso/vidocq/mansart && sdk env
opencode --agent jpa-dev
```

`default_agent` is set, but the flag is the form verified to work. Then, inside:

```
/next                # opens the next card: reads STATUS.md, picks one TODO,
                     # states the failing test and the ≤4 files BEFORE writing
                     # ... TDD loop: red, green, refactor ...
/gate                # build + unit + @auditor + TCK client + Sonar on the module
/session-end         # @tracker writes STATUS.md and TASKS.md, then commit
```

**One card per session.** Quit and relaunch for the next one. Nothing enforces
this — the harness has no wall (§3) — which is exactly why it is a discipline you
apply rather than a limit you hit.

### 13.3 Mid-session

| you need | do this | not this |
| --- | --- | --- |
| what the spec says | `/spec <question>` | reading the spec yourself |
| to read a screenshot | switch to the `vision` agent, or `/see <question>` | describing it in prose |
| to make one TCK client pass | `/tck-fix <Client>` | editing until it goes green |
| to know if a card is really done | `/audit` then `/gate` | believing the build output |
| the quality gate alone | `/sonar [module]` | reading the scanner log |
| where something is in Java | the `lsp` tool | `grep -r` |

`/gate` starts the `mansart-sonar` container itself if it is stopped. It takes
40–90 s to boot on the first gate of the day and competes for RAM with the pinned
model; `docker stop mansart-sonar` between sessions rather than `docker rm` (§12).

### 13.4 Adding or closing work

`TASKS.md` and `STATUS.md` belong to `@tracker` — do not hand-edit them mid-session,
`/session-end` will overwrite. To add work discovered along the way, tell
`/session-end` about it: it appends a **new** card rather than growing the one in
flight. A card names one behaviour, at most 4 files, exactly one proving test, and
its dependencies (§9).

### 13.5 When something changes

| you changed | you must |
| --- | --- |
| `opencode.json`, an agent, a command, a skill | quit and relaunch OpenCode — config is read once at startup |
| `~/.omlx/model_settings.json` | **stop oMLX first** (`kill $(pgrep -f 'oMLX.app/Contents/MacOS/oMLX')`), edit, relaunch — a running app rewrites the file from memory |
| sampling, model limits, agent frontmatter | re-run the request capture (§4.1) — silent no-ops are the norm here, not the exception |

### 13.6 Signs to stop and restart

- the agent answers, announces what it will do, and stops — it is narrating tool
  calls instead of emitting them (§3). Do not prod it; quit, restart, `/next`.
- the same edit fails twice with a schema error — write it with a bash heredoc.
- you are on your second card in one window — you are already paying for it.

State is always on disk. A session that dies loses a turn, never work.

## 14. Known traps

| symptom | cause | fix |
| --- | --- | --- |
| oMLX settings revert after an edit | the running app rewrites `model_settings.json` from memory | stop the app, edit, relaunch |
| OpenCode's global config reverts | a running OpenCode rewrites `~/.config/opencode/opencode.json` | keep durable config in the project `opencode.json`; restart OpenCode after a global edit |
| tool call truncated mid-JSON | `max_tokens` ≤ thinking budget + write size | keep the §3 invariant; keep single `write` calls under ~150 lines |
| repetition loop | near-greedy sampling with thinking enabled | `force_sampling: true` at 0.6 / 0.95 / 20 |
| model silently capped at 32k | `sampling.max_context_window` in `settings.json` applies to models with no window of their own | set the model's `max_context_window` explicitly |
| a red squiggle on a generated class | jdtls uses ECJ and does not run our APT | run `./mvnw install` once; trust Maven, not jdtls |
| Maven log floods the window | not routed through `ctx` | `ctx_execute` / `ctx_batch_execute`, and read surefire XML |
| a `write` fails twice with a schema error | tool-call formatting | write the file with a bash heredoc and move on |
| the agent answers, announces what it will do, and stops — every time | it is describing tool calls instead of emitting them, usually after a compaction (§3) | do not prod it; quit, restart, `/next`. State is in `STATUS.md` |
| decode speed drifts down over a day | oMLX uptime — the SSD cache grows to ~90 GB and memory fragments; costs ~25% decode after ~19 h (measured, BENCH.md) | restart oMLX (the Restart button); a fresh server recovers the speed |
| the context indicator reads over 100 % | expected: `limit.context` is a display denominator, nothing enforces it (§3) | not a fault. Judge the session by whether it still calls tools, not by the percentage |
| an agent ignores a frontmatter setting | unknown fields are silently routed into `options` instead of erroring — `topP` vs `top_p` | check the allowed field list, then confirm with a request capture (§4.1) |
| a custom agent behaves less carefully than `build` | its prompt replaced the built-in one (§5.1) | the shared scaffolding is in `.opencode/OPERATING.md`; check it is still listed in `instructions` |
| a review agent did the work instead of reviewing it | `@name` was written in prose instead of a `task` call — nothing was delegated (§5) | it ran in the primary agent's context with its permissions; re-run through a command with `agent:` + `subtask: true` |
| config edits appear to do nothing | OpenCode loads config once at startup and does not hot-reload | quit and restart OpenCode |
| Sonar reports 0 % coverage | JaCoCo never ran — missing `-Pquality`, or the build stopped at `test` instead of `verify` | `./mvnw -Pquality … verify sonar:sonar` |
| Sonar analysis refused with 401 | SonarQube 26.5 dropped anonymous analysis | mint a token in the UI, `export SONAR_TOKEN=…` before launching opencode |
| Sonar full of issues on `_Entity` classes | generated code is being analysed | check `sonar.exclusions` still covers `**/generated-sources/**` |
| the Sonar project history vanished | `docker rm mansart-sonar` — the container has no volume | it is not recoverable; `stop`/`start` only, never `rm` |
| the model answers "I don't see any screenshot attached" | the text model is blind; its vision tower is unreachable (§1) | use the `vision` agent or `/see` — a different model entirely |
| "no Java server configured" / jdtls never starts | OpenCode's version regex needs `"X.Y.Z"`; a major-only JDK prints `"25"` and is rejected (§10) | pin a patch release in `.sdkmanrc` (`java=25.0.3-tem`), `sdk env`, relaunch |
| an identifier read off a screenshot does not exist | the vision model made an OCR slip (`samuelfaj` → `samuelselfaj`, measured) | never paste a transcribed package or class name without checking it |
| a model loads but ignores its tuning | its settings are keyed to the *other* id of a duplicated install (§3) | `grep -a 'BatchedEngine loaded' ~/.omlx/logs/server.log \| tail -3` to see which path is live |
| ECJ rejects `Map<String, ?>` overriding `Map<?, ?>` | ECJ strictness on generic erasure — `String` vs `?` is not erasure-compatible | use `Map<?, ?>` in every `PersistenceProvider` method signature |
| JPMS `provides` invisible on the classpath | ServiceLoader on the classpath ignores JPMS service declarations | ship a `META-INF/services/jakarta.persistence.spi.PersistenceProvider` file for classpath discoverability |

---

## 15. What would make the demonstration succeed

The claim under test is not "a local model can write Java". It is that a
**35B-active-3B model running on one machine** can implement a real Jakarta
specification when the harness supplies what the model lacks: memory (`STATUS.md`),
decomposition (`TASKS.md`), navigation (LSP), verification (`/gate`), and an
adversarial reviewer (`@auditor`).

Concretely, the demonstration succeeds if:

1. the M0 baseline is measured and recorded honestly, whatever it is;
2. each milestone closes on a **measured** TCK number for its named packages, and
   the number moves in the right direction;
3. no card is closed by a stub, a disabled test, or a TCK special-case — verified
   by `@auditor` on every gate;
4. the architecture constraints hold throughout: no runtime reflection on user
   classes, strict module declarations, zero external runtime dependencies,
   virtual threads;
5. sessions stay inside their window — a compaction is a decomposition bug, and it
   is worth recording when it happens.

It fails, honestly and usefully, if the TCK number plateaus while the card count
keeps rising. That result is worth publishing too.
