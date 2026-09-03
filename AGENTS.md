# AGENTS.md

Contributor guidance for agents working on this repository.

This file is **self-contained for `vibe`** — everything an agent needs to
work correctly is stated here or in a skill it points to, not deferred to
`mansart/CLAUDE.md` (a Claude-Code-specific file with no reason for a
Mistral Vibe session to read it, and no guarantee it ever will just
because an instruction says "see CLAUDE.md" — an unread pointer is not a
loaded rule). `CLAUDE.md` exists for Claude Code sessions on this same
repository and may duplicate some of the content below; it is not a
prerequisite for anything here.

Auto-loaded by `vibe` for every session started under this directory — no
wiring needed, Vibe discovers `AGENTS.md` by walking the directory tree.
Kept at the **workspace root, generalized across every mansart sub-module**
(`mansart-jakarta-data`, `mansart-transactions`, `mansart-pool`,
`mansart-jakarta-persistence`) rather than duplicated per sub-module —
Vibe reads exactly one `AGENTS.md` chain per session, so the rules that
apply everywhere live here once.

## Engineering contract

Non-negotiable for any mansart sub-module, delivered or in progress:

- **Java 25**, the pinned toolchain (`sdk env` in the sub-project root) —
  Maven 3.9.x. Never target or accept code that requires a newer/older
  release.
- **TDD.** Write the failing test first, watch it fail, then the smallest
  real implementation, then refactor. No implementation code without a
  failing test that already existed for it.
- **No reflection on user classes at runtime.** No `java.lang.reflect`, no
  `Proxy`, no `MethodHandles` lookup against a user type. Field/attribute
  access goes through code generated at compile time (APT, or the
  Class-File API for the Maven-plugin/runtime-fallback tiers — see the
  `vidocq-codegen` skill for the full three-tier doctrine).
- **No fake implementations.** Anything unimplemented throws
  `UnsupportedOperationException("not implemented: <what>")`. Never return
  `null`/`0`/`false`/an empty collection to make a test look quieter than
  it is — that is not progress, and `auditor` rejects it.
- **No test-suite knowledge in main sources.** TCK package/class names,
  table names, or "known test case" special-cases are forbidden outside
  the dedicated `*-tck` module for that sub-project.
- **Strict Java modules, zero external runtime dependencies beyond the
  relevant Jakarta APIs, virtual threads, `ScopedValue` instead of
  `ThreadLocal`** (a platform thread / `ThreadLocal` is a documented
  exception, not a default — justify it in the card's notes when used).
  Delegate to `guardian` before a `module-info.java` or `pom.xml` change
  you are unsure about.
- **The SonarQube quality gate must pass on new code before a card is
  `DONE`** — bugs, code smells, vulnerabilities, and security hotspots
  alike, not just coverage. See the `vidocq-quality` skill for the
  mechanism (shared Docker instance, one `projectKey` per sub-project) and
  for what counts as a security review, including known-vulnerable
  dependencies. Pre-existing debt is a separate backlog; a card answers
  for the code it wrote.
- **English everywhere**: code, Javadoc, Markdown, commit messages.
  Conventional Commits, signed off (DCO), with a `Co-Authored-By:` trailer
  naming the model that wrote the change.
- **The only real progress metric for a spec implementation is the
  official TCK PASS/total**, measured by the `tck-runner` subagent — never
  a card count, never a compiling-but-unverified stub, and never a number
  one agent reports about its own work without another agent (`tck-runner`
  or `auditor`) independently measuring it.

## Working session protocol

1. Read the active sub-module's `STATUS.md` (current focus + last log
   lines) and `TASKS.md` (pick exactly **one** card, `TODO`, deps `DONE`).
   Announce its id. Never work two cards in one session. **If those files
   don't exist yet for the sub-module you're working in, that's the
   session's task**: run `/bootstrap-plan` (see that skill) to generate
   `PLAN.md`/`TASKS.md`/`STATUS.md` before picking any implementation card.
2. Sanity-check the card against `PLAN.md`'s milestone table before
   committing to it — the plan may be imperfect. If it belongs to a later
   milestone, or depends on something not built yet, delegate to `tracker`
   to mark it `BLOCKED` with a one-line reason and take the next eligible
   card instead of working around it with a stub.
3. TDD loop: state the failing test **first**, watch it fail, write the
   smallest real implementation — delegate the actual writing to `coder`
   for anything beyond a trivial edit — then refactor.
4. Before marking a card `DONE`: build green, unit tests green (surefire,
   not console), `auditor` clean on the diff restricted to the sub-module
   directory, the SonarQube quality gate green on new code (delegate to
   `guardian`, see `vidocq-quality`), and — if the card names a TCK
   client — `tck-runner`'s measured number.
5. Delegate to `tracker` to update `STATUS.md`/`TASKS.md` with the real,
   measured numbers, then commit. `tracker` owns those files; do not
   hand-edit them mid-session.

## Delegate on these triggers — do not deliberate

The roster (`.vibe/agents/*.toml`: `dev`, `coder`, `spec-reader`,
`tck-runner`, `auditor`, `tracker`, `guardian`, `thinker`) is generic —
not named after, or specific to, any one spec or sub-module. Reuse it
as-is for a different mansart sub-module or a different project entirely:
only what's under "Current active work" below, and a project-architecture
skill like `mansart-jpa`, change per target.

- Anything out of a spec/TCK source → `spec-reader`. One question, one
  cited answer.
- "Where is X", "what already exists" → `explore` (Vibe's builtin).
- A full TCK client run → `tck-runner`.
- A review of your own diff → `auditor`.
- A `module-info.java`/`pom.xml`/concurrency question, a Sonar quality
  gate, or a security review (new dependency, untrusted input,
  hotspot) → `guardian`. See `vidocq-quality`.
- Stuck twice on the same problem → `thinker`. A fresh, isolated context
  beats a third attempt in a context that already contains two failures.
- Well-specified implementation work (test is written, behaviour is
  clear, ≤4 files) → `coder`. See `model-discipline`: this is also the
  cheaper model, and the primary agent's job is deciding, not typing.
- Generating/regenerating a sub-module's `PLAN.md`/`TASKS.md`/`STATUS.md`,
  for this target or a new one → the `bootstrap-plan` skill
  (`/bootstrap-plan [directory] [spec]`).

## Definition of done

A card is done when the validation in step 4 above passes **in the same
session**. Nothing else counts — not a reduced error count, not a
compiling stub, not "should pass now."

## Current active work: mansart-jakarta-persistence

**Spec**: Jakarta Persistence 3.2 (classic JPA) — the third of four
persistence building blocks in this repository, alongside the delivered
`mansart-jakarta-data` (Jakarta Data 1.0, declarative repositories, TCK
74/74 PASS), `mansart-transactions` (Jakarta Transactions 2.0), and
`mansart-pool` (virtual-thread-native JDBC pool, optional, fully
decoupled). Locked design decisions for this module specifically:

- Shares `mansart-data-dialect-spi` (H2 + PostgreSQL dialects, the neutral
  `Where`/`Attribute` AST) and the static-metamodel convention with
  `mansart-jakarta-data` — never write inline SQL, never reinvent the
  dialect contract.
- Entity enhancement (lazy loading, dirty tracking, accessors) is
  APT-generated at compile time — no runtime bytecode loading, no Java
  agent. Full three-tier doctrine in the `vidocq-codegen` skill.
- Binds to `mansart-transactions` (the `ScopedValue`-based TM) for
  transaction management — never a second transaction manager.
- The `DataSource` is supplied by the application; `mansart-pool` is an
  optional convenience, never a dependency.

Load the `mansart-jpa` skill before writing anything in that directory;
`mansart-jpa-tck` before touching the TCK; `vidocq-codegen` before writing
anything that would otherwise use reflection; `vidocq-quality` before or
when interpreting a Sonar gate. **Do not read or cherry-pick from**
`feature/jakarteee-mansart-persistence-*` or `ybl/jpa-opencode` — prior
attempts at this module, explicitly out of scope.

## Documentation (Antora) conventions

The project documentation lives in `docs/en` as an Antora component and is
aggregated by the **vidocq-docs** site, which provides a **shared UI bundle** (banner,
logo, fonts, colours, footer). **Never customise the documentation UI per project** —
all visual harmonisation is centralised in `vidocq-docs/ui-bundle`.

### Gold reference
**Vauban** is the reference implementation for documentation structure. Mirror its
`docs/en` layout when creating or updating docs. **Chappe** (HTTP server)
and **Vidocq** (runtime orchestrator) are *special cases*, not references: they are not
Jakarta EE / MicroProfile spec implementations.

### Repository layout
- `docs/en/antora.yml` → `name: <project>`, `title:`, versioned per branch (`dev` prerelease on `main`, `'<version>'` on `docs/<version>`), `project-version` attribute, `nav:`, `lang: en`.
- Pages in `modules/ROOT/pages/`, navigation in `modules/ROOT/nav.adoc`, images in
  `modules/ROOT/images/`.
- **English-only** (ADR 0004 in vidocq-docs): no French mirror — do not reintroduce one.

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
