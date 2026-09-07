# AGENTS.md

Self-contained. Everything an agent needs is stated here or in a skill named
here — never deferred to another file. Auto-loaded by `vibe` for every session
under this directory, and appended to the system prompt as project
instructions, so **keep it short, dense and stable**: it sits at the front of
every request and is the block that benefits most from prefix caching. Edits
here invalidate that cache for every subsequent turn — change it deliberately.

`CLAUDE.md` exists for Claude Code sessions on this same repository. It is not
a prerequisite for anything below and no agent needs to read it.

## The context rule — read this first

**Target: ≤ 30 000 tokens of context per step.** Measured on 3–6 Sep 2026
across 10 sessions and 1 701 steps: **81 000**. Input was 93% of a $43.98
bill (138 M input tokens against 581 k output). Context volume *is* the cost;
the choice of model is about 1% of it.

Where that context came from, measured across 39 transcripts: `read_file` 42%,
`bash` 28%, `write_file`+`edit` 21%, and actual delegation — `task` returns —
**4.6%**. **72% of the primary context was delegable work the primary did
itself.** A subagent's reading is discarded when it returns; the primary's is
re-sent on every step until the session ends. That is the whole reason the
roster below exists.

**Agents read `docs/spec-notes/*.md`. Never the full spec text.** Start from
`docs/spec-notes/INDEX.md` and load **one** note. One condensed file per spec
chapter, **200 lines maximum** — past that it splits. If the note does not
answer the question, read the minimum span of the spec that does, then *write
the note*. The next session pays for the note, not the chapter.

Everything else volatile — file dumps, grep sweeps, build logs, stack traces —
goes to a **subagent**, whose context is discarded. What comes back is a
summary. Nothing bulky enters the primary context.

### Never, in any session

- **Never load the full specification text.** One note, from the index.
- **Never inspect archive contents** — no `jar tf`, `unzip -l`, `javap`. 42
  such calls were measured in three days; each dumps hundreds of entries that
  are then re-sent on every later turn. What a TCK jar contains belongs in
  `docs/spec-notes/`, written once.
- **Never `cat` a file over 200 lines.** Use `sed -n 'a,bp'` or `grep -n`.
- **Never `find … -exec`.** Vibe hardwires an approval prompt for it at any
  permission level, so it stops the session every time. `grep -rl "<pattern>"
  --include="*.java" .` does the same job in one process, without the prompt.
- **Never repeat a discovery command.** One session ran the identical
  `find … -name "EntityModel.java"` **75 times**, another the same command 17
  times — 228 steps for 5 useful results. When delegating, the parent passes
  **absolute paths** in the task prompt; a subagent missing a path stops and
  reports it instead of searching.
- **Never run `mvn`/`./mvnw` directly, and never pipe a command whose exit
  code matters.** See "Build" below.
- **One session = one card.** The most expensive session in the measurement
  ($19.55 — 44% of the total) chained 757 steps on a single subject without
  ever restarting. Finish the card, commit, start a fresh session.

The `mansart-context-guard` hook (`.vibe/hooks.toml`) refuses the shell forms of
these. It is a backstop, not the rule.

## Roster — delegate on these triggers, do not deliberate

| Agent | Model | Use it for |
| --- | --- | --- |
| `spec` | GLM 5.2, reasoning **off** | Primary. Normative reasoning, deciding contracts, writing `docs/spec-notes/`. Writes nowhere else. |
| `impl` | Mistral Small 4 | Every line of production code, once the contract is decided. Cannot touch `*-tck/`. |
| `recon` | Mistral Small 4 | "Where is X", grep, build/test output, summarising a failure. Read-only. |
| `verify` | Mistral Small 4 | Runs the build and the suites. Returns **only** numbers. Writes nothing, fixes nothing. |
| `thinker` | GLM 5.2, reasoning **on** | One hard decision, after two failed attempts at the same problem. The only agent that spends thinking tokens. |

`spec` decides and delegates; it does not type. Any code, sweep or log that
`impl`, `recon` or `verify` can produce must not be produced by `spec` — not
mainly for the price per token, but because whatever `spec` produces stays in
the primary context and is re-sent on every subsequent step.

**When delegating, always pass absolute paths.** A subagent that has to find
its own files is the single largest measured source of wasted steps.

**`verify` is the sole source of the numbers in `STATUS.md`.** Over three
days, 24 cards were marked DONE while the conformance counter stayed frozen at
2 passed / 0 failed / 2 errored. A measurement has to come from an agent that
cannot act on it.

Reasoning is **off by default**. Enable it for a session with
`/model glm-5-2-think`, and turn it back off when the hard part is over.

## Engineering contract — non-negotiable

- **Java 25**, pinned toolchain (`sdk env`), Maven 3.9.x.
- **TDD.** Failing test first, watch it fail, smallest real implementation,
  refactor. No implementation without a test that already failed for it.
- **Never edit a conformance test to make it pass.** A failing TCK or unit
  test is reported upward with FQCN, expected vs actual, and the suspected
  production line. No `@Disabled`, no weakened assertion, no narrowed
  parameter set. Enforced at tool level for `impl`; a rule for everyone.
- **No fake implementations.** Unimplemented throws
  `UnsupportedOperationException("not implemented: <what>")`. Never
  `null`/`0`/`false`/empty to quiet a test.
- **No reflection on user classes at runtime** — no `java.lang.reflect`,
  `Proxy`, or `MethodHandles` against a user type. Attribute access is
  generated at compile time (APT; Class-File API for the plugin/runtime
  tiers — see the `vidocq-codegen` skill).
- **No test-suite knowledge in main sources.** TCK class names, table names or
  special-cases live only in the `*-tck` module.
- **Strict Java modules, zero external runtime dependencies** beyond the
  relevant Jakarta APIs. **Virtual threads**, `ScopedValue` over
  `ThreadLocal` (a platform thread is a documented exception, justified in the
  card's notes).
- **SonarQube quality gate green on new code** before a card is `DONE` — bugs,
  smells, vulnerabilities *and* security hotspots. See `vidocq-quality`.
- **English everywhere.** Conventional Commits, DCO sign-off, `Co-Authored-By:`
  naming the model that wrote the change.
- **The only progress metric is the official TCK PASS/total**, measured — never
  a card count, never a compiling stub, never a number an agent reports about
  its own work.

## Session protocol

1. Read the active sub-module's `STATUS.md` and `TASKS.md`. Take exactly **one**
   `TODO` card whose deps are `DONE`. Announce its id. Never two per session.
   If those files do not exist, that *is* the session: run `/bootstrap-plan`.
2. Sanity-check the card against `PLAN.md`. Wrong milestone or missing
   dependency → mark it `BLOCKED` with a one-line reason, take the next card.
   Do not work around it with a stub.
3. Settle the contract (cite the spec section or `docs/spec-notes/` file) →
   state the failing test → delegate the writing to `impl`, **passing absolute
   paths**.
4. Before `DONE`: delegate to `verify`. It runs `./scripts/verify.sh` and
   returns build green/red and tests passed/failed/errored with a delta. No
   other source of numbers is acceptable. Then the Sonar gate on new code.
5. Write `STATUS.md`/`TASKS.md` with `verify`'s figures verbatim, commit, then
   start a **fresh session** for the next card.

The whole loop is automated by the `next-card` skill: `/next-card`.

## Build — through the scripts, never `mvn` directly

```bash
sdk env                                                  # Java 25 + Maven 3.9.x
./scripts/build.sh                                       # clean install -DskipTests
./scripts/build.sh -pl mansart-jakarta-persistence/mansart-persistence-core test
./scripts/verify.sh                                      # tests + the numeric report
```

**Why this is not a preference.** Of 262 Maven invocations logged over three
days, every one recorded `exit_code: 0` — while 38 outputs contained
BUILD FAILURE, 3 compilation errors and 17 test failures. The pattern used
everywhere was `mvn … 2>&1 | tail -50`, and in a pipeline `$?` is the *last*
command's status: `tail` always succeeds. Maven's result was destroyed before
anyone could read it, so "only commit if the build passes" was not a checkable
rule.

The scripts `set -euo pipefail`, keep the full output in
`.agent-logs/build.log` / `.agent-logs/verify.log`, print the last 60 lines,
and **propagate Maven's exit code**. They end with a machine-readable verdict
line (`BUILD_RESULT=…`, `BUILD=… tests=… passed=… failed=…`).

Maven runs in batch mode with **stdin closed** and under a **wall-clock limit**
(`BUILD_TIMEOUT`, default 1800s). A run that exceeds it is killed and reported
as `BUILD_RESULT=TIMEOUT` — a killed run is not a failure of the code, and its
counts are never recorded. If a run seems stuck, `tail -f .agent-logs/*.log`
shows whether it is progressing, and `docker info` is worth checking first:
`mansart-persistence-tests` uses Testcontainers.

**Never pipe a build command — the scripts included.** `./scripts/build.sh |
tee …` throws the exit code away exactly as `mvn | tail` did. Run the script
bare; it already prints the last 60 lines and ends with a `BUILD_RESULT=` line.

**Never write to `/tmp`.** The full log is already inside the project at
`.agent-logs/build.log` and `.agent-logs/verify.log`. Anything under `/tmp`
is outside the workdir, so it costs an approval prompt on every single call —
and duplicates a file you already have. Need more than the printed tail?
`tail -n 200 .agent-logs/build.log` or `grep -n ERROR .agent-logs/build.log`.
Never `cat file | tail`: that reads the whole file to show you its end.

TCK runs: see the `mansart-jpa-tck` skill (out-of-reactor runner, profiles
`tck-run` / `tck-pg`) — invoked through `verify`.

## Layout

```
mansart-jakarta-data/        Jakarta Data 1.0        — delivered, TCK 74/74
mansart-transactions/        Jakarta Transactions 2.0 — delivered
mansart-pool/                virtual-thread JDBC pool — delivered, optional
mansart-jakarta-persistence/ Jakarta Persistence 3.2  — IN PROGRESS
  ├── mansart-persistence-spi/ core/ processor/ cdi/ tests/
  ├── mansart-persistence-maven-plugin/  external-lib/  external-it/
  └── mansart-persistence-tck/           <- never written to by an agent
docs/spec-notes/             condensed spec, one file per chapter
```

## Current active work: mansart-jakarta-persistence

Jakarta Persistence 3.2 (classic JPA). Locked design decisions:

- Shares `mansart-data-dialect-spi` (H2 + PostgreSQL dialects, the neutral
  `Where`/`Attribute` AST) and the static-metamodel convention with
  `mansart-jakarta-data`. Never inline SQL, never reinvent the dialect
  contract.
- Entity enhancement (lazy loading, dirty tracking, accessors) is APT-generated
  at compile time. No runtime bytecode, no Java agent. Doctrine:
  `vidocq-codegen`.
- Transactions bind to `mansart-transactions` (`ScopedValue`-based TM). Never a
  second transaction manager.
- The `DataSource` comes from the application; `mansart-pool` is optional.

Skills: `mansart-jpa` before writing in that directory, `mansart-jpa-tck`
before touching the TCK, `vidocq-codegen` before anything that would otherwise
use reflection, `vidocq-quality` for the Sonar gate, `model-discipline` when a
session feels expensive.

**Out of scope**: `feature/jakarteee-mansart-persistence-*` and
`ybl/jpa-opencode` are prior failed attempts. Do not read or cherry-pick.

## Terminology

Say **Java Modules** (or **Java module**). Never **JPMS** — in prose,
identifiers or documentation.

Documentation (Antora) conventions live in `docs/en/` and in `CLAUDE.md`; load
them only when a card actually touches the doc site.
