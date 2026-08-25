# AGENTS.md

Contributor guidance for agents working on this repository. See also the companion `CLAUDE.md` file.

## What this repository is

Mansart is the persistence layer of the Vidocq ecosystem, four independent
runtime building blocks:

| sub-project | spec | state |
| --- | --- | --- |
| `mansart-jakarta-data` | Jakarta Data 1.0 | delivered, TCK 74/74 |
| `mansart-transactions` | Jakarta Transactions 2.0 | delivered, TCK smoke 5/5 |
| `mansart-pool` | virtual-thread-native JDBC pool | delivered |
| `mansart-jakarta-persistence` | Jakarta Persistence 3.2 | **in progress** |

Prerequisites: **Java 25** + **Maven 3.9.16**, pinned by `.sdkmanrc` (`sdk env`).
Build from the mansart root: `./mvnw -ntp install -DskipTests`, then `./mvnw test`.

## Engineering non-negotiables

These are not style preferences. A change that breaks one of them is rejected
regardless of whether it makes a test pass.

1. **Generated code, never reflection.** Three tiers, in order: APT for the
   user's sources; a Maven plugin using the **Class-File API** (`java.lang.classfile`,
   JEP 484) for entities arriving in external jars; runtime Class-File API into a
   hidden class as an extreme fallback that logs a warning. Banned outright:
   `java.lang.reflect.Proxy`, ASM, Byte Buddy, cglib, `Field.setAccessible`,
   `MethodHandles` resolution against a user class on a hot path.
2. **Strict Java modules.** Every module has a `module-info.java` with minimal
   `exports`, no unjustified `opens`, symmetric `provides`/`uses`, no split
   packages, no classpath fallback.
3. **Zero external runtime dependencies** beyond the Jakarta APIs the module
   implements. JDBC drivers stay `provided`; Testcontainers, JUnit, Arquillian
   stay `test`. A new runtime dependency needs written justification in the PR.
4. **TDD.** Failing test first, then the smallest real implementation, then
   refactor. No exceptions.
5. **Virtual threads** for all I/O, `Executors.newVirtualThreadPerTaskExecutor()`
   by default, `ScopedValue` instead of `ThreadLocal`, and never a blocking call
   inside a `synchronized` block.
6. **No fake implementations.** Anything unimplemented throws
   `UnsupportedOperationException("not implemented: <what>")`. Returning `null`,
   `0`, `false` or an empty collection to quieten a test is forbidden. Moving a
   TCK test from ERROR to FAIL is not progress.
7. **No TCK knowledge in main sources.** `ee.jakarta.tck.*`, `com.sun.ts.*`, TCK
   table or class names live only in the TCK runner module.
8. **English everywhere**: code, Javadoc, comments, symbol names, Markdown,
   commits, issues. French is only for conversation with the maintainer.

## Traceability

- Reproducible bugs go in `BUG.md` at this root: short id, absolute date,
  symptom, minimal repro, hypothesis, status.
- Every performance figure goes in `BENCH.md`: absolute date, hardware and JVM,
  the exact command, raw results, delta versus the previous run. No performance
  number may appear in a README or a commit message without a matching entry.
- Commits: Conventional Commits with the task id in the subject, GPG-signed,
  `Signed-off-by`, plus a `Co-Authored-By:` trailer naming the AI tool used.
  Author and committer remain the human.

## Working on mansart-jakarta-persistence

Read `mansart-jakarta-persistence/STATUS.md` first, then take exactly **one**
`JP-xx` card from `TASKS.md`. The milestone map is in `PLAN.md`.

This sub-project is developed with OpenCode on a local model. The agents,
commands and skills that encode the protocol are in `.opencode/`, and the whole
setup — model, context calibration, decomposition strategy — is documented in
`PREPARE_OPENCODE.md`. Read that before changing anything under `.opencode/`.

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
