---
name: mansart-jpa
description: Architecture, module layout and build commands for mansart-jakarta-persistence (Jakarta Persistence 3.2). Load at the start of any session that touches this sub-project.
user-invocable: false
---

# mansart-jakarta-persistence

Jakarta Persistence 3.2 for the Vidocq ecosystem. Sibling of the delivered
`mansart-jakarta-data` (Jakarta Data 1.0, TCK 74/74), `mansart-transactions`
(Jakarta Transactions 2.0) and `mansart-pool`.

## Reactor layout

```
mansart-jakarta-persistence/
  mansart-persistence-spi/            metadata model + reuse of mansart-data-dialect-spi
  mansart-persistence-processor/      APT (RELEASE_25): _Entity metamodel, descriptors, accessors
  mansart-persistence-core/           EMF, EntityManager, persistence context, JPQL, Criteria, flush
  mansart-persistence-maven-plugin/   Class-File API generation for entities in external jars
  mansart-persistence-cdi/            CDI 4.1 Lite integration through Vauban
  mansart-persistence-tests/          unit tests + Arquillian
  mansart-persistence-external-lib/   test fixture: a jar of entities APT never sees
  mansart-persistence-external-it/    integration test proving tier-1 and tier-2 behave identically
  mansart-persistence-tck/            OUT OF REACTOR — standalone POM, modelVersion 4.0.0
```

**The reference implementation for every one of those shapes already exists in
this repository**, one directory up, for Jakarta Data: `mansart-data-processor`,
`mansart-data-maven-plugin` (`GenerateExternalRepositoriesMojo`),
`mansart-data-external-lib`, `mansart-data-external-it`, `mansart-data-tck`.
Read those before inventing anything. Mirror their structure, their POM shape
and their naming.

Prior attempts at this module exist on other branches
(`feature/jakarteee-mansart-persistence-*`, `ybl/jpa-opencode`) and are
explicitly out of scope — do not read or cherry-pick from them. Design
decisions below are already locked; this attempt starts from a clean
`mansart-jakarta-persistence/` directory.

## What is shared, and what is not

- SQL generation goes through `mansart-data-dialect-spi` — the same neutral AST
  and the same `Dialect` / `DialectFactory` `ServiceLoader` contract, with the
  same H2 and PostgreSQL implementations. Never write inline SQL in
  `mansart-persistence-core`.
- Transactions bind to `mansart-transactions` (`ScopedValue`-based TM). Do not
  reimplement a transaction manager.
- The `DataSource` is supplied by the application. `mansart-pool` is an option,
  never a dependency.
- Jakarta Data and Jakarta Persistence stay independent at runtime. Adding a
  compile dependency from one to the other requires an explicit decision.

## Build

Always from the mansart root, with the pinned toolchain:

```bash
sdk env   # Java 25 + Maven 3.9.16
./mvnw -ntp install -DskipTests      # full build
./mvnw -ntp test                     # unit tests
./mvnw -ntp -pl mansart-jakarta-persistence/mansart-persistence-core test
```

Never `cat` a raw Maven log; grep it or read the surefire XML. (On this
machine, this project's bash commands are also routed through `rtk` — that
comes from this user's personal, machine-global `~/.vibe/hooks.toml`, not
from anything committed in this repository; don't assume it's present on
another machine or another contributor's setup.)

Generated sources land in `target/generated-sources/annotations/`. Generated
classes are prefixed `_` and annotated `@Generated`.

## Quality and security

Load the `vidocq-quality` skill — the SonarQube-on-Docker mechanism (container,
port, the `-Pquality`/`verify` gotcha, reading the gate via API) and the
security-review expectations are identical across every mansart sub-project
and documented there once. This project's specific Sonar `projectKey` is
**`vidocq-mansart-persistence`**.

## Non-negotiables (short form; the full contract is in the root `AGENTS.md`)

Java 25 · strict Java modules, minimal `exports`, no unjustified `opens` ·
generated code instead of reflection · virtual threads and `ScopedValue` ·
zero external runtime dependencies beyond the Jakarta APIs · TDD, failing test
first · English in code, Javadoc, Markdown and commits.

## Card Naming Convention

All implementation cards follow the pattern **`M<milestone>-JP-<number>`** to explicitly
bind each card to its milestone. For example:
- `M0-JP-01` — Parent pom.xml with 9 sub-modules (Milestone 0)
- `M1-JP-08` — APT Processor implementation (Milestone 1)

## Definition of done

A card is done when: build green on every module, unit tests green, `recon`
clean, and the TCK number for the card's client measured and recorded (via
`recon`) — in the same session. Nothing else counts as progress — not a
reduced error count, not a compiling stub.
