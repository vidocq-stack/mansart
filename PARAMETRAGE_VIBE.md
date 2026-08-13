# Mistral Vibe setup for mansart — reproducible guide

This document describes the complete Mistral Vibe configuration put in place for
mansart (August 2026), and how to reproduce it on another project of the Vidocq
workspace. Everything was verified live with `vibe -p` probes (commands at the end).

## What was set up

| Piece | Location | Scope |
| --- | --- | --- |
| `java-lsp` MCP server (jdtls via LSP→MCP bridge) | `.vibe/config.toml` | project |
| `ctx` MCP server (context-mode sandbox + knowledge base) | `.vibe/config.toml` | project |
| 5 specialist subagents | `.vibe/agents/*.toml` | project |
| 4 skills (2 guidance, 2 slash commands) | `.vibe/skills/*/SKILL.md` | project |
| RTK command-rewrite hook | `~/.vibe/hooks.toml` + `~/.vibe/hooks/rtk-rewrite-vibe.sh` | user (all projects) |
| Model guidance (subagent/skill/ctx routing) | `AGENTS.md` | project |

## Prerequisites

- Vibe CLI installed (`vibe --version`).
- The project directory must be **trusted** by Vibe (`~/.vibe/trusted_folders.toml`,
  or accept the trust prompt on first launch). Project-level `.vibe/` config, agents,
  skills, and hooks are only loaded from trusted folders.
- For java-lsp: `jdtls` (`brew install jdtls`), Go toolchain, a JDK.
- For RTK: `rtk` >= 0.23.0 and `jq` in `PATH`.
- For context-mode: Node.js and the context-mode distribution (here: the Claude Code
  plugin cache; an npm install works too).

## 1. Java language server over MCP (the `java-lsp` fix)

### The problem

The original config launched jdtls directly as an MCP server:

```toml
# ~/.vibe/config.toml — BROKEN, do not do this
mcp_servers = [
    { name = "java-lsp", transport = "stdio", command = "/opt/homebrew/bin/jdtls" },
]
```

Result: `MCP server 'java-lsp' failed to connect: Timed out while waiting for
response to ClientRequest. Waited 10.0 seconds.`

**Root cause: jdtls speaks LSP, not MCP.** Both are JSON-RPC over stdio, but they are
different protocols: Vibe sends an MCP `initialize` request that jdtls never answers,
so the connection times out. An LSP server must be wrapped by an LSP→MCP bridge.
(Side note: a stray `~/.vibe/mcp.json` referencing `@modelcontextprotocol/server-lsp`
was a dead end — Vibe does not read `mcp.json`, and that npm package does not exist.)

### The fix

Install [mcp-language-server](https://github.com/isaacphi/mcp-language-server), a Go
bridge that starts the LSP server and exposes its features as MCP tools:

```bash
go install github.com/isaacphi/mcp-language-server@latest   # → ~/go/bin/
mkdir -p ~/.cache/jdtls/<project>-workspace                  # jdtls data dir
```

Then declare it in the **project-level** `.vibe/config.toml` (one bridge instance per
project, because `--workspace` is project-specific):

```toml
# <project>/.vibe/config.toml
[[mcp_servers]]
name = "java-lsp"
transport = "stdio"
command = "/Users/yblazart/go/bin/mcp-language-server"
args = [
    "--workspace", "/absolute/path/to/<project>",
    "--lsp", "/opt/homebrew/bin/jdtls",
    "--", "-data", "/Users/yblazart/.cache/jdtls/<project>-workspace",
]
env = { JAVA_HOME = "/Users/yblazart/.sdkman/candidates/java/25-tem" }
startup_timeout_sec = 120   # jdtls imports the Maven reactor on first start
tool_timeout_sec = 60
```

The broken entry in `~/.vibe/config.toml` was replaced by `mcp_servers = []`.
Note: project `config.toml` takes precedence per key over the user-level one, and is
only loaded in trusted folders.

Exposed tools (verified): `java-lsp_definition`, `java-lsp_diagnostics`,
`java-lsp_edit_file`, `java-lsp_hover`, `java-lsp_references`, `java-lsp_rename_symbol`.

## 2. context-mode as a Vibe MCP server (token savings on large outputs)

The context-mode Claude Code plugin is, at its core, a standalone MCP server
(`node start.mjs`). Vibe can run it directly:

```toml
# <project>/.vibe/config.toml
[[mcp_servers]]
name = "ctx"
transport = "stdio"
command = "/opt/homebrew/bin/node"
args = ["/Users/yblazart/.claude/plugins/cache/context-mode/context-mode/1.0.65/start.mjs"]
startup_timeout_sec = 60
tool_timeout_sec = 120
```

Exposed tools (verified): `ctx_ctx_batch_execute`, `ctx_ctx_execute`,
`ctx_ctx_execute_file`, `ctx_ctx_index`, `ctx_ctx_search`, `ctx_ctx_fetch_and_index`,
`ctx_ctx_stats`, `ctx_ctx_doctor`, `ctx_ctx_upgrade`.

Two caveats:

1. **The path is pinned to the installed plugin version** (`1.0.65`). After a plugin
   update, bump the path (or install context-mode via npm and point at a stable path).
2. **Vibe has no session-start hook injection**, which is how the Claude plugin teaches
   the model to route large outputs through `ctx_*`. The equivalent on Vibe is a
   section in `AGENTS.md` (loaded into context every session) — see §5.

## 3. Subagents (`.vibe/agents/*.toml`)

Vibe custom agents are TOML files in `./.vibe/agents/` (project) or `~/.vibe/agents/`
(user). Two kinds, declared via `agent_type`:

- `"agent"` — user-facing, selectable with `vibe --agent <filename>` or `Shift+Tab`.
- `"subagent"` — delegation-only; the model spawns it through the `task` tool. It runs
  independently, returns text only, and **cannot ask the user questions** — so every
  tool it needs must have `permission = "always"` in its own TOML.

Key fields: `display_name`, `description` (what the parent model reads to decide when
to delegate — put the full mandate here), `safety` (border color hint: `safe` /
`neutral` / `destructive` / `yolo`), `enabled_tools` (allowlist of tool names, globs
supported), and per-tool `[tools.<name>] permission = "always" | "ask" | "never"`.

Five subagents were created for mansart, ported from the workspace Claude agents
(`.claude/agents/*.md`) and adapted to the Jakarta Persistence 3.2 work:

| Subagent | Tools | Mandate |
| --- | --- | --- |
| `jpms-guardian` | `grep`, `read_file` (read-only) | Audits `module-info.java`: minimal `exports`, justified `opens`, no automatic modules, `requires` ↔ `pom.xml` cross-check. Punch list output, never edits. |
| `classfile-codegen` | `grep`, `read_file` (read-only) | Reviews static codegen. **Order of preference: APT-generated sources first; Class-File API (JEP 484) as a last resort only** (bytecode-level needs, written justification). Never ASM/Byte Buddy. AOT-compatible output, ServiceLoader discovery. |
| `virtual-threads-reviewer` | `grep`, `read_file` (read-only) | Hunts pinning risks (`synchronized` around blocking I/O — JDBC!), `ThreadLocal` abuse (→ `ScopedValue`), wrong executors (→ virtual-thread-per-task), missing `StructuredTaskScope`. |
| `dependency-gatekeeper` | `grep`, `read_file`, `bash` | Reviews every `pom.xml` dependency change against the zero-deps charter: Jakarta specs only in production scope, drivers `provided`, forbidden list (Jackson, Guava, ASM, Lombok, Spring…). Verdict per change. |
| `tck-runner` | `bash`, `grep`, `read_file` | Runs/triages Jakarta TCKs. Enforces the out-of-reactor rule (standalone Model 4.0.0 POMs, never `mvn -pl` from a parent reactor). Smoke first, then full; classifies failures (impl bug / harness / missing artifacts). |

Reproduction on another project: copy the five `.toml` files into
`<project>/.vibe/agents/` and adapt the project-specific parts of each `description`
(module names, TCK runner names, allowed dependencies).

## 4. Skills (`.vibe/skills/<name>/SKILL.md`)

A Vibe skill is a directory containing a `SKILL.md` with YAML frontmatter:

```markdown
---
name: my-skill            # lowercase, digits, hyphens; becomes /my-skill
description: When to load this skill (shown in menus, read by the model).
user_invocable: true      # appears in the slash-command menu
---
# Instructions (markdown body, injected into the prompt when invoked)
```

Search paths: `./.vibe/skills/` (project), `~/.vibe/skills/` (user), `.agents/skills/`.
Project wins over user on name conflicts. Skills are invoked by the user as `/name`
(with optional trailing instructions) or loaded by the model when relevant.

Four skills in `mansart/.vibe/skills/`:

| Skill | Type | Content |
| --- | --- | --- |
| `mansart-persistence` | guidance (pre-existing, amended) | Architecture and workflow for the Jakarta Persistence 3.2 implementation: module layout, JPQL pipeline, static metamodel, dev/debug flags. Amended so codegen is **APT-first, Class-File API last resort** (see §6). |
| `mansart-persistence-tck` | guidance (pre-existing) | Persistence 3.2 TCK setup (`setup-tck.sh`), execution profiles (`-Ptck`, `-Ppgsql`), failure-analysis playbook, progress tracking. |
| `log-bug` | slash command (new) | Appends a structured entry (`BUG-YYYYMMDD-NN`) to the sub-project `BUG.md`, per the workspace convention. Ported from `.claude/skills/log-bug`. |
| `log-bench` | slash command (new) | Appends a benchmark entry (`BENCH-YYYYMMDD-NN`) with environment capture and delta vs previous run to `BENCH.md`. Ported from `.claude/skills/log-bench`. |

## 5. `AGENTS.md` as the glue

Vibe loads up to two `AGENTS.md` files into every session: `~/.vibe/AGENTS.md`
(user-level) and the first one found walking up from the working directory (trusted
folders only). Subagents inherit it too. mansart's `AGENTS.md` gained two sections:

- **"Engineering directives (mansart-persistence 3.2)"** — JDK 25, Virtual Threads +
  `ScopedValue` (never `ThreadLocal`), APT-first codegen (Class-File API last resort),
  CDI via Vauban BCE (mirror mansart-data-cdi), English, TDD, official TCK green.
- **"Working protocol (context-window efficiency)"** — one task per session; read
  `PERSISTENCE-STATUS.md` at start, update it at end; **validation gate: a task may
  only be marked done after `./mvnw -ntp clean install` at the mansart root is green
  on every module** (full non-regression, also purges stale jdtls/ECJ classes).
- **"Known pitfall: stale ECJ classes in `target/` (jdtls)"** — see §10.
- **"Vibe subagents and skills (`.vibe/`)"** — tells the model which subagents exist
  and to delegate proactively, and lists the slash commands. Without this, discovery
  relies only on the `task` tool's agent list.
- **"Context-window discipline (context-mode)"** — the Vibe replacement for the
  plugin's hook injection: any command expected to produce more than ~20 lines
  (Maven, TCK, dependency trees, log analysis, doc fetches) must go through
  `ctx_ctx_batch_execute` / `ctx_ctx_execute` / `ctx_ctx_fetch_and_index` +
  `ctx_ctx_search` instead of plain `bash`; plain `bash` stays for short commands.

## 6. APT-first codegen policy

At the maintainer's request, the code-generation preference is:

1. **APT** (`javax.annotation.processing`) — the default: generate Java **sources**
   (static metamodel `_Entity`, entity support classes for dirty tracking / lazy
   loading, proxies as generated subclasses, repository impls). Emitted via `Filer`,
   incremental-friendly, `RELEASE_25`.
2. **Class-File API** (JEP 484) — **last resort**, only when source generation cannot
   express the need (e.g. modifying already-compiled bytecode), each use documented.
   Never ASM / Byte Buddy / Javassist; never runtime bytecode generation.

Applied in: `.vibe/agents/classfile-codegen.toml`, `.vibe/skills/mansart-persistence/SKILL.md`
(Core Principles §1, Bytecode Enhancement warning, "Adding a New Entity Feature"),
and the `AGENTS.md` subagent listing.

## 7. RTK hook (user-level, all projects)

Vibe hooks live in `hooks.toml` (`./.vibe/hooks.toml` project-level, trusted folders
only; `~/.vibe/hooks.toml` user-level; same-name project entry wins). A `pre_tool`
hook fires before the permission prompt, receives a JSON payload on stdin
(`tool_name`, `tool_call_id`, `tool_input`, session context) and may answer on stdout:

- `{"decision": "deny", "reason": "..."}` — blocks the call, `reason` goes to the LLM;
- `{"hook_specific_output": {"tool_input": {...}}}` — **full replacement** of the tool
  arguments (re-validated against the tool schema; what the permission prompt shows,
  what runs, and what later LLM turns see);
- empty stdout + exit 0 — passthrough.

`~/.vibe/hooks.toml`:

```toml
[[hooks]]
name = "rtk-rewrite"
type = "pre_tool"
match = "bash"
command = "/Users/yblazart/.vibe/hooks/rtk-rewrite-vibe.sh"
timeout = 15.0
strict = false
description = "Rewrite shell commands through rtk (Rust Token Killer) for token savings."
```

`~/.vibe/hooks/rtk-rewrite-vibe.sh` (executable) is a thin port of the Claude Code
hook: it extracts `.tool_input.command`, delegates to `rtk rewrite "$CMD"` (the single
source of truth for rewrite rules), and on exit code 0 or 3 emits the replacement
`tool_input` with `.command` swapped, preserving all other arguments. Exit codes 1
(no equivalent) and 2 (deny rule) pass through — Vibe's own permission flow and bash
allowlist handle approval. Differences vs the Claude version: field casing
(`hook_specific_output`/`tool_input` instead of `hookSpecificOutput`/`updatedInput`)
and **no auto-approve equivalent** — Vibe hooks cannot bypass the permission prompt,
so `rtk` should be in the bash allowlist (`[tools.bash] allowlist` in
`~/.vibe/config.toml`, already the case).

## 8. Verification (programmatic probes)

```bash
cd <project>

# MCP servers connect and expose their tools (also checks agent/skill TOML parsing)
vibe -p "Without calling any tool, list the tool names you have." \
     --enabled-tools 'java-lsp*' --max-turns 1
vibe -p "Without calling any tool, list the tool names you have." \
     --enabled-tools 'ctx*' --max-turns 1

# Subagents and skills are discovered
vibe -p "Without calling any tool, list the subagents you can delegate to and the skills available." \
     --max-turns 1

# RTK hook rewrites live (expected answer: rtk git status)
vibe -p "Run 'git status' with the bash tool, then answer with only the exact command executed." \
     --enabled-tools bash --auto-approve --max-turns 6
```

The hook can also be unit-tested without a session:

```bash
echo '{"tool_input":{"command":"git status","timeout":30}}' \
  | ~/.vibe/hooks/rtk-rewrite-vibe.sh
# → {"hook_specific_output":{"tool_input":{"command":"rtk git status","timeout":30}},...}
```

## 9. Checklist: reproduce on another project

1. Trust the folder (launch `vibe` once in it, or add it to `~/.vibe/trusted_folders.toml`).
2. Create `<project>/.vibe/config.toml`; copy the two `[[mcp_servers]]` blocks and
   change `--workspace`, the jdtls `-data` dir, and (if needed) the context-mode path.
3. Copy `<project>/.vibe/agents/*.toml` from mansart; adapt each `description`
   (module names, TCK scripts, dependency whitelist) — keep `agent_type`,
   `enabled_tools`, and the `permission = "always"` blocks.
4. Create `<project>/.vibe/skills/` — `log-bug` and `log-bench` are copyable as-is
   (they resolve the sub-project from context); write project-specific guidance
   skills as needed.
5. Add the two sections to the project `AGENTS.md` (subagent/skill listing +
   context-mode routing).
6. Nothing to do for RTK — the user-level hook applies everywhere.
7. Run the §8 probes.

## 10. Lesson learned: jdtls fights Maven for `target/` (incident MANSART-008)

Running jdtls (via the `java-lsp` bridge) against a project that is **also built with
Maven CLI** has a sharp edge: jdtls imports the project through m2e and compiles with
**ECJ into the Maven output directories** (`target/classes`, `target/test-classes`).

Two failure modes were hit on 2026-08-13 (full trace: `BUG.md` MANSART-008):

1. **Stale broken classes.** Unlike javac, ECJ writes `.class` files even for code it
   could NOT compile, embedding `throw new Error("Unresolved compilation problems")`
   and **package-less descriptors** for unresolved types. Maven's incremental compiler
   then sees a `.class` newer than the source, skips recompilation, and surefire runs
   the corrupted bytecode. The fingerprint is unmistakable: a test dies with
   `TypeNotPresentException` / `ClassNotFoundException` on a class name **without a
   package**, or `java.lang.Error: Unresolved compilation problems`, while sources are
   green. `javap -v <class>` shows the embedded error string.
2. **Orphaned jdtls processes.** `vibe -p` (programmatic) runs may fail to kill their
   MCP child process groups — the CLI prints `Process group termination failed …
   falling back to simple terminate`. The leaked jdtls instances keep recompiling and
   re-corrupting `target/` DURING later Maven builds; wiping their `-data` directory
   while they run makes it worse (broken resolution → more ECJ garbage).

**The trap for the agent**: the runtime symptoms look module-related, so the model
"fixes" them with `opens`, `--add-reads`, or surefire `useModulePath` changes. That is
always wrong for this fingerprint — it violates the strict-modules charter and cannot
fix corrupted bytecode. The rule is codified in `AGENTS.md` > "Known pitfall".

**Recovery procedure**:

```bash
pkill -f mcp-language-server                      # stops bridges AND their jdtls children
rm -rf ~/.cache/jdtls/<project>-workspace          # only when no jdtls is running
git checkout -- <files touched by the wrong fix>   # if the agent already "fixed" modules
./mvnw -ntp -f <reactor>/pom.xml clean test        # clean purges the ECJ classes
```

**Prevention**: `mvn clean` before any test run that follows java-lsp editing activity;
after `vibe -p` probes, check `ps aux | grep jdtls` for orphans; treat any
`module-info.java`/surefire edit proposed in response to a test failure as suspect
unless the error is a genuine module error (`does not read`, `does not export`).

## 11. Local model: oMLX + Devstral Small 2 (MLX, Apple Silicon)

Motivation: the hosted Mistral API had repeated instability (HTTP 503). The fallback
is fully local inference on the M5 Max (128 GB) through **oMLX** (production MLX
server, `omlx` CLI + `oMLX.app`), which exposes an **OpenAI-compatible API** on
`127.0.0.1:8000`, protected by an API key.

### What was verified before wiring

1. **Tool-call parsing** — the make-or-break for any agent CLI. Devstral emits tool
   calls in Mistral's raw format (`[TOOL_CALLS]name[ARGS]{...}`, EOS-terminated); the
   server must convert them to OpenAI `tool_calls` JSON, otherwise Vibe hangs forever
   on "Generating…". Verified on oMLX with a direct `curl` carrying a `tools` array:
   the response came back with `finish_reason: "tool_calls"` and a parsed
   `tool_calls[]` entry. (For stock `mlx_lm.server`, this needs mlx-lm ≥ 0.30.7.)
2. **Model present**: `Devstral-Small-2-24B-Instruct-2512-4bit` already installed in
   `~/omlx-models` (profile: temp 0.2, 131k context), among 7 exposed models.

### Wiring (three pieces)

`~/.vibe/.env` (Vibe's credential file, already present):

```
OMLX_API_KEY=<the key from ~/.omlx/settings.json auth.api_key>
```

`~/.vibe/config.toml` (user-level — available to all projects):

```toml
[[providers]]
name = "omlx"
api_base = "http://127.0.0.1:8000/v1"
api_key_env_var = "OMLX_API_KEY"
api_style = "openai"
backend = "generic"

[[models]]
name = "Devstral-Small-2-24B-Instruct-2512-4bit"
provider = "omlx"
alias = "devstral-omlx"
# + aliases qwen-coder-omlx (Qwen3-Coder-30B-A3B 6bit), medium-omlx (Mistral-Medium-3.5-128B-4bit)
```

`<project>/.vibe/config.toml` (project-level — mansart runs local by default;
top-level keys must appear BEFORE the first `[[...]]` table in the file):

```toml
active_model = "qwen-next-omlx"
```

Remove that line (or use `/config` in-session) to go back to the hosted Mistral API.
E2E verified: `vibe -p` in mansart answered from the local model and oMLX's request
counter incremented accordingly.

### Model choice vs heat (M5 Max)

- `qwen-next-omlx` — **default for mansart**. Qwen3-Coder-Next 80B **MoE, ~3B active**,
  6-bit (~65 GB on disk; downloaded from `mlx-community/Qwen3-Coder-Next-6bit` into
  `~/omlx-models/mlx-community/`, then `omlx restart` to expose it). Big-model coding
  quality at small-model compute: only ~3B params fire per token, so it stays fast and
  cool despite the size. 6-bit chosen over 8-bit (84.7 GB) to leave ~60 GB headroom
  for KV cache, macOS, jdtls, and Maven; over 4-bit (44.9 GB) for quality. Tool-call
  parsing verified on oMLX (`finish_reason: tool_calls`).
- `devstral-omlx` — Devstral Small 2, dense 24B, 4-bit (~13 GB): Mistral's agentic
  coding tuning; lighter RAM footprint, but dense 24B ≈ more compute per token than
  the MoE above.
- `qwen-coder-omlx` — Qwen3-Coder-30B MoE ~3B active, 6-bit: smaller/cheaper fallback.
- `medium-omlx` — Mistral-Medium-3.5-128B 4-bit (~70 GB): for hard design questions
  only; heavy, slow, hottest. Not for routine agent loops.

Additional thermal habits: one Vibe session at a time (oMLX allows 8 concurrent
requests — parallel subagent fan-outs multiply load); the AGENTS.md context protocol
(small tracker, ctx routing, grep-not-load) also directly reduces prefill work, which
is where most of the heat comes from; macOS Low Power Mode is a coarse but effective
silencer for long unattended runs. oMLX's own guards were left at their defaults
(`memory_guard_tier: balanced`, `burst_decode_mode: balanced`, prompt cache enabled —
the cache visibly absorbs most of Vibe's repeated system prompt/AGENTS.md prefill).

### Caveats

- `omlx launch` has native integrations (claude, codex, opencode, …) but **not vibe**
  — hence the manual provider wiring above.
- If the oMLX server is not running (`omlx start`), Vibe sessions in mansart fail at
  the first model call; switch model via `/config` or start the server.
- Local models are weaker than the hosted flagships: keep tasks small (the M7 task
  granularity fits), lean on the validation gate (`./mvnw clean install`) to catch
  model mistakes, and escalate to `medium-omlx` or the hosted API for thorny design work.

## 12. Parallel setups: OpenCode and jcode (same oMLX backend)

Both alternative CLIs are wired to the same local stack so they can be trialed on
real tasks without touching the Vibe setup. `AGENTS.md` (directives, working
protocol, validation gate, pitfall) is read by all three tools — it is the shared
brain; only the tool-specific glue differs.

### OpenCode (verified E2E: `OK-OPENCODE` from Qwen3-Coder-Next via oMLX)

- **Provider/model** — `~/.config/opencode/opencode.json`: provider `omlx`
  (`npm: @ai-sdk/openai-compatible`, baseURL `http://127.0.0.1:8000/v1`, apiKey
  inline) with the 4 models; default `"model": "omlx/Qwen3-Coder-Next-6bit"`.
  Switch in-session with `/models`.
- **MCP** — `mcp.ctx` block (context-mode `node start.mjs`, same pinned path as
  Vibe). No `java-lsp` MCP: OpenCode has **built-in LSP** and will drive jdtls
  itself — which means the §10 ECJ pitfall applies to OpenCode too (its jdtls also
  compiles into `target/`; `pkill -f mcp-language-server` will NOT catch OpenCode's
  jdtls — use `pkill -f "eclipse.jdt.ls"`).
- **RTK** — global plugin `~/.config/opencode/plugins/rtk-rewrite.js` using the
  `tool.execute.before` hook: bash commands go through `rtk rewrite` (exit 0/3 →
  rewrite; 1/2 → pass through), mirroring the Vibe/Claude hooks.
- **Subagents** — `mansart/.opencode/agents/*.md` (frontmatter: `description`,
  `mode: subagent`, `permission: edit/bash deny` for the read-only ones): the same
  five specialists as Vibe. Invoke with `@jpms-guardian` etc.
- **Commands** — `mansart/.opencode/commands/log-bug.md` and `log-bench.md`
  (`/log-bug <context>`, `$ARGUMENTS` placeholder).
- **Auto-approve agent** — `~/.config/opencode/agents/auto-approve.md`: global
  primary agent with `permission: edit/bash/webfetch = allow` (equivalent of Vibe's
  `auto-approve`). Select with `opencode --agent auto-approve`, `opencode run
  --agent auto-approve '...'`, or Tab in the TUI. Its prompt re-asserts that
  AGENTS.md rules and the validation gate still apply. Verified headless: bash ran
  unprompted — and through the RTK plugin (`git status` → `rtk git status`).

### jcode (verified E2E: `JCODE-OK` from Qwen3-Coder-Next via oMLX)

- **Provider profile** — created with
  `jcode provider add omlx --overwrite --base-url http://127.0.0.1:8000/v1
  --model Qwen3-Coder-Next-6bit --api-key-stdin` (key piped from
  `~/.omlx/settings.json`). Written to `~/.jcode/config.toml`
  (`[providers.omlx]`, bearer auth, key in
  `~/Library/Application Support/jcode/provider-omlx.env`); the other three oMLX
  models added as `[[providers.omlx.models]]` entries.
- **Run** — `jcode --provider-profile omlx run '...'` or the TUI; switch models
  with `/model`.
- **Known quirk** — `jcode auth-test`'s `provider_smoke` probe hits `/v1/models`
  WITHOUT the bearer header and reports FAIL (HTTP 401) even though the real chat
  path authenticates fine. Judge the setup by `jcode run`, not by `auth-test`.
- **MCP** — jcode auto-discovers MCP servers (it had already cached the `outline`
  server schemas); its config surface for custom MCP entries was not needed for
  the trial. No hooks equivalent found yet → **no RTK rewrite under jcode**; the
  AGENTS.md RTK table is the only nudge there.
- jcode runs a background daemon (`jcode server`) — after config changes, restart
  it (`jcode server stop --force`, it respawns on next run) so profiles reload.

### Trial protocol suggestion

Run one well-scoped M7 task per tool on a clean git state, judge on: respect of the
validation gate, module-info discipline (MANSART-008-style temptations), token/heat
cost (oMLX `stats.json` deltas), and friction. `PERSISTENCE-STATUS.md` session-log
the outcome of each trial.

## References

- [Configuration — Mistral Docs](https://docs.mistral.ai/vibe/code/cli/configuration)
- [MCP servers — Mistral Docs](https://docs.mistral.ai/vibe/code/cli/mcp-servers)
- [Agents — Mistral Docs](https://docs.mistral.ai/vibe/code/cli/agents)
- [Hooks — Mistral Docs](https://docs.mistral.ai/vibe/code/cli/hooks)
- [mcp-language-server (LSP→MCP bridge)](https://github.com/isaacphi/mcp-language-server)
- [context-mode](https://github.com/mksglu/context-mode)
- [Skills & Agent Profiles — DeepWiki (mistral-vibe)](https://deepwiki.com/mistralai/mistral-vibe/3.8-skills-and-agent-profiles)
