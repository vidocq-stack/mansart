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

## References

- [Configuration — Mistral Docs](https://docs.mistral.ai/vibe/code/cli/configuration)
- [MCP servers — Mistral Docs](https://docs.mistral.ai/vibe/code/cli/mcp-servers)
- [Agents — Mistral Docs](https://docs.mistral.ai/vibe/code/cli/agents)
- [Hooks — Mistral Docs](https://docs.mistral.ai/vibe/code/cli/hooks)
- [mcp-language-server (LSP→MCP bridge)](https://github.com/isaacphi/mcp-language-server)
- [context-mode](https://github.com/mksglu/context-mode)
- [Skills & Agent Profiles — DeepWiki (mistral-vibe)](https://deepwiki.com/mistralai/mistral-vibe/3.8-skills-and-agent-profiles)
