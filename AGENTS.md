# AGENTS.md

Contributor guidance for agents working on this repository. See also the companion `CLAUDE.md` file.

## Documentation (Antora) conventions

The project documentation lives in `docs/en` and `docs/fr` as Antora modules and is
aggregated by the **vidocq-docs** site, which provides a **shared UI bundle** (banner,
logo, fonts, colours, footer). **Never customise the documentation UI per project** —
all visual harmonisation is centralised in `vidocq-docs/ui-bundle`.

### Gold reference
**Vauban** is the reference implementation for documentation structure. Mirror its
`docs/en` + `docs/fr` layout when creating or updating docs. **Chappe** (HTTP server)
and **Vidocq** (runtime orchestrator) are *special cases*, not references: they are not
Jakarta EE / MicroProfile spec implementations.

### Repository layout
- `docs/en/antora.yml` → `name: <project>`, `title:`, `version: ~`, `nav:`, `lang: en`.
- `docs/fr/antora.yml` → `name: <project>-fr`, same `title`, `lang: fr`.
- Pages in `modules/ROOT/pages/`, navigation in `modules/ROOT/nav.adoc`, images in
  `modules/ROOT/images/`.
- **EN/FR parity**: every page exists in both languages with translated content.

### Canonical navigation (section order)
`index` → `getting-started` → `usage` → `concepts` → `internals` → `tck` →
`performance` → `reference` → `migration`

Multi-module projects (e.g. Vidocq, Mansart) may append `modules/*` / `sub-modules/*`
sub-pages after `migration`.

### TCK / Performance rule (not mutually exclusive)
- Every **spec implementation** — i.e. **all projects except Chappe and Vidocq** — MUST
  have a **`tck`** section documenting TCK coverage/status.
- Projects with a performance story (e.g. **Chappe**) keep their **`performance`** section.
- When **both** sections exist, order them **TCK first, then Performance**.
- **Chappe** and **Vidocq** do not require a `tck` section (not spec implementations).

### `index.adoc` structure
Follow Vauban's `index.adoc`: page title (`= <Project>`), `:description:`, a centred logo
(`image::<project>-logo.png[...,role=module-logo]`), a `[.lead]` paragraph, then
`== Origin of the name`, an `== At a glance` table, and ecosystem / quick-links sections.

### Logo
Provide `modules/ROOT/images/<project>-logo.png` (PNG), referenced from `index.adoc`.

> When you change these documentation rules, keep `AGENTS.md` and `CLAUDE.md` in sync.

## Terminology

Use **Java Modules** (or **Java module** for a single module) when referring to
the Java Platform Module System. Do **not** use the abbreviation **JPMS** — in
prose, identifiers, or documentation.

## Vibe subagents and skills (`.vibe/`)

When running under Mistral Vibe, this repository ships project-level configuration in `.vibe/`:

- **Subagents** (`.vibe/agents/*.toml`, delegate via the `task` tool):
  `jpms-guardian` (module-info.java audit), `classfile-codegen` (codegen review —
  APT first, Class-File API last resort), `virtual-threads-reviewer` (concurrency
  review), `dependency-gatekeeper`
  (pom.xml zero-deps review), `tck-runner` (Jakarta TCK execution and triage).
  Delegate proactively when the matching context applies.
- **Skills** (`.vibe/skills/*/SKILL.md`, slash commands): `/mansart-persistence`
  (implementation guidance), `/mansart-persistence-tck` (TCK workflow), `/log-bug`
  (append to `BUG.md`), `/log-bench` (append to `BENCH.md`).
- **MCP** (`.vibe/config.toml`): `java-lsp` — jdtls wrapped by `mcp-language-server`
  (jdtls speaks LSP, not MCP; never launch it directly as an MCP server).

## Shell Command Optimization with RTK

**Rule:** Always prefix shell commands with `rtk` to reduce context tokens and improve efficiency.

| Command Type       | Standard Command       | RTK Equivalent          | Benefit                          |
|--------------------|-------------------------|-------------------------|----------------------------------|
| Git operations     | `git status`           | `rtk git status`       | Compact, no pager, no colors     |
|                    | `git diff`             | `rtk git diff`         | Optimized diff output            |
| Maven builds       | `mvn test`             | `rtk mvn test`         | Only errors/warnings             |
|                    | `mvn compile`          | `rtk mvn compile`      | Filtered logs                    |
| File reading       | `cat file.java`        | `rtk read file.java`   | Strips comments/blank lines      |
| Directory listing  | `ls -la`               | `rtk ls`               | Clean, token-friendly output     |
| Test execution     | (any test command)     | `rtk test`             | Shows only failures              |
| Dependencies       | -                       | `rtk deps`             | Summarizes project dependencies |
| Errors only        | -                       | `rtk err <command>`    | Shows only errors/warnings      |

> **Tip:** Use `rtk --help` to discover all available optimized commands.
