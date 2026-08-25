# PREPARE_OPENCODE.md

How mansart-jakarta-persistence is set up to be implemented by a **local** model.

The engineering goal is Jakarta Persistence 3.2 with the official TCK green. The
second goal is a demonstration: **implementing a Jakarta specification with a
locally-hosted LLM is feasible**, provided the work is decomposed and the context
is calibrated rather than left to chance. This document records every setting, the
measurements that justify it, and how to run a session.

Everything here lives in the repository: `opencode.json`, `.opencode/agents/`,
`.opencode/commands/`, `.opencode/skills/`, `AGENTS.md`, and the pilot files
`mansart-jakarta-persistence/{PLAN,TASKS,STATUS}.md`.

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

The MTP sidecar is the point of this build. Its own manifest
(`mtplx_runtime.json`) reports acceptance 0.886 at depth 1, giving **1.465×** over
plain autoregressive decoding — 138 tok/s versus 94 — and diminishing returns at
depth 2 and 3. Depth 1 is what the runtime picks; leave it alone.

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
| `mtp_enabled` | `true` | activates the sidecar — without it the build is just a 4-bit quant |
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

Decode measured at ~105 tok/s with MTP enabled.

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

### The budget, and why 53k

The old truncation bug is worth understanding, because the fix is the whole
arithmetic. Previously `max_tokens` was 12 288 with a thinking budget of 8 192,
leaving only 4 096 tokens for the actual tool call. A large `write` exceeded that,
the JSON was cut mid-argument, the call never closed, and the turn was lost. It
looked like a context-length problem. It was a budget-split problem.

The invariant:

```
max_tokens  >  thinking_budget  +  largest_tool_call
16384       >  4096             +  12288
```

Which then fixes everything downstream:

| knob | value |
| --- | --- |
| oMLX `max_tokens` | 16 384 |
| oMLX `thinking_budget_tokens` | 4 096 |
| OpenCode `limit.output` | 12 288 — exactly what oMLX guarantees after thinking |
| OpenCode `limit.context` | 65 536 |

OpenCode reserves `min(20000, output)` for compaction, so auto-compaction fires at
`65 536 − 12 288 ≈ **53 000 tokens**`. That sits inside the fully-clean measured
zone, with per-turn cache-miss prefill capped around 15 s.

Target split of the 53k working window:

```
system prompt + AGENTS.md + one skill      ~6 k
tool schemas + LSP diagnostics             ~3 k
the task card                              ~1 k
working set: at most 4 Java files         ~12 k
tool output (grepped, never raw)          ~10 k
headroom for reasoning and edits          ~20 k
```

### Two windows, one model

The same weights are declared twice in `opencode.json`, using the two ids oMLX
exposes for them:

- `Qwen3.6-35B-A3B-MTPLX-Optimized-Speed` → 65 536, for everything that edits code;
- `Youssofal--Qwen3.6-35B-A3B-MTPLX-Optimized-Speed` → 131 072, for the read-only
  `spec-reader`, whose job is one long single-shot read producing one small answer.
  That is precisely the workload the 84k probe validated.

`Youssofal--…-Balance` (65 536) drives the review agents, and `Qwen3.6-35B-A3B-8bit`
(65 536) is reserved for `@thinker`, called at most once or twice per session.

---

## 4. OpenCode configuration

`opencode.json` at the mansart root. Not the global `~/.config/opencode/opencode.json`
— **a running OpenCode rewrites the global file from its in-memory state**, so
durable configuration belongs to the project.

Beyond the model declarations above:

```jsonc
"compaction": { "auto": true, "reserved": 12288, "preserve_recent_tokens": 12000 },
"lsp": true,
"permission": { "external_directory": { "/tmp/**": "allow", "/private/tmp/**": "allow",
                                        "/Users/yblazart/.m2/**": "allow" },
                "doom_loop": "deny" },
"instructions": ["AGENTS.md"]
```

`doom_loop: deny` matters with a local model: it stops a repetition loop from
burning an hour of GPU.

The `ctx` MCP server (context-mode) is declared globally and is used by every
agent to route build and test output. It is the difference between a Maven log
costing 4 000 tokens and costing 40.

---

## 5. Agents

`.opencode/agents/`. One primary, ten subagents. The design principle: **a subagent
burns its own context, not the primary's.** Delegation is the main context-control
mechanism, ahead of compaction.

| agent | mode | model window | role |
| --- | --- | --- | --- |
| `jpa-dev` | primary | 64k | the developer. Full-auto edits and shell. One card per session. |
| `spec-reader` | subagent | **128k** | read-only spec and TCK-source oracle. One question, one cited answer, ≤40 lines. |
| `tck-runner` | subagent | 64k | runs the TCK, reports real integers, never edits main sources. |
| `codegen` | subagent | 64k (Balance) | owns APT / Maven-plugin / runtime-fallback generation. |
| `auditor` | subagent | 64k (Balance) | anti-drift: stubs, reflection, TCK leakage, disabled tests, module violations. |
| `explore` | subagent | 64k | scout. Returns `file:line` pointers, never file contents. |
| `tracker` | subagent | 64k | the only writer of `STATUS.md` and `TASKS.md`. |
| `module-guardian` | subagent | 64k (Balance) | `module-info.java` review. |
| `dependency-gatekeeper` | subagent | 64k | enforces the zero-dependency rule on every POM change. |
| `virtual-threads-reviewer` | subagent | 64k (Balance) | pinning, `ThreadLocal`, platform pools, connection lifetime. |
| `thinker` | subagent | 64k (**8-bit**) | one hard decision, after two failed attempts. Returns a decision, not an essay. |

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

---

## 6. Commands

`.opencode/commands/`, invoked as `/name`.

| command | what it does |
| --- | --- |
| `/next [JP-xx]` | opens a session: reads `STATUS.md`, picks one `TODO` card, states the failing test and the ≤4 files before writing anything |
| `/gate` | the validation gate: build, unit tests, `@auditor`, and the card's TCK client. Returns `GATE: PASS` or `FAIL`. Required before any card becomes `DONE` |
| `/tck [Client\|all]` | delegates a TCK run to `@tck-runner` |
| `/tck-fix <Client>` | the fix loop: read the test source → ask `@spec-reader` if needed → reproduce as a local unit test → implement the *spec* → re-run → `/gate` |
| `/audit [scope]` | anti-drift audit of the branch |
| `/spec <question>` | one question to the spec oracle |
| `/status` | where the project stands, from `STATUS.md` and `TASKS.md` only |
| `/session-end` | hands real numbers to `@tracker`, updates state, commits |
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
- **Card** (`TASKS.md`, `JP-xx`) — the work unit, sized for one 53k session: one
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
the card in flight. Scope creep inside a session is how a 53k window becomes a
compacted one.

The TCK gives the decomposition a natural grain: 269 client classes, ~1 745 test
methods. `/tck-fix <Client>` is one card's worth of work almost by construction.

---

## 10. Using the LSP properly

OpenCode ships **jdtls** (already in `~/.cache/opencode/bin/jdtls`) and resolves the
project root by walking up to the aggregator `pom.xml`, so the multi-module reactor
is handled natively. `"lsp": true`.

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

## 12. Running a session

```bash
cd ~/projects/perso/vidocq/mansart
sdk env                    # Java 25 + Maven 3.9.16
opencode                   # picks up ./opencode.json and AGENTS.md
```

Then, inside OpenCode:

```
/next                      # opens the next card, states the plan
                           # ... TDD loop ...
/gate                      # build + unit + audit + TCK
/session-end               # tracker writes STATUS.md and TASKS.md, then commit
```

**One card per session. Always a fresh session for a new card.** When the context
indicator passes ~70 %, stop, `/session-end`, restart.

Useful mid-session: `/spec <question>` rather than reading the spec yourself;
`/tck-fix <Client>` for the TCK loop; `/audit` before you believe a card is done.

---

## 13. Known traps

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

---

## 14. What would make the demonstration succeed

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
