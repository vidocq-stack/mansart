---
name: mansart-jpa
description: Architecture, module layout and build commands for mansart-jakarta-persistence (Jakarta Persistence 3.2). Load at the start of any session that touches this sub-project.
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
this repository**, one directory up, for Jakarta Data:
`mansart-data-processor`, `mansart-data-maven-plugin`
(`GenerateExternalRepositoriesMojo`), `mansart-data-external-lib`,
`mansart-data-external-it`, `mansart-data-tck`. Read those before inventing
anything. Mirror their structure, their POM shape and their naming.

Do **not** read the abandoned `feature/jakarteee-mansart-persistence-*` branches.
They drifted into stubs and reflection and are explicitly out of scope.

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

Route every one of those through the `ctx` tools. A raw Maven log is several
thousand tokens and tells you nothing a grep would not.

Generated sources land in `target/generated-sources/annotations/`. Generated
classes are prefixed `_` and annotated `@Generated`.

## Quality: SonarQube on the local Docker instance

Container `mansart-sonar` (`sonarqube:community` 26.5), host port **9001**, project
key `vidocq-mansart-persistence`. It has **no volume mounted** — `docker stop`/`start`
are safe, `docker rm` destroys the history. Never remove it.

```bash
docker start mansart-sonar                       # then wait for /api/system/status = UP
./mvnw -ntp -Pquality -pl mansart-jakarta-persistence/<module> -am verify \
  org.sonarsource.scanner.maven:sonar-maven-plugin:sonar
```

`verify`, not `test`: JaCoCo's report is bound to `verify`, and `-Pquality` is what
activates JaCoCo at all (it lives in a profile in `vidocq-parent`). Without both,
Sonar reports 0 % coverage and you will chase a phantom.

Never read the scanner log — query the API
(`/api/qualitygates/project_status`, `/api/issues/search?inNewCodePeriod=true`),
which returns a few hundred bytes. Delegate the whole thing to `@sonar-runner` via
`/sonar`; `/gate` already does this for the module a card touched.

**Only issues on new code block a card.** Existing debt is a separate backlog. At
milestone close, run `/sonar all` and record the gate status in `STATUS.md` next to
the TCK number.

## Non-negotiables (short form; the full list is in AGENTS.md)

Java 25 · strict Java modules, minimal `exports`, no unjustified `opens` ·
generated code instead of reflection · virtual threads and `ScopedValue` ·
zero external runtime dependencies beyond the Jakarta APIs · TDD, failing test
first · English in code, Javadoc, Markdown and commits.

## Definition of done

A card is done when `/gate` returns `GATE: PASS` **in the same session**: build
green on every module, unit tests green, `@auditor` clean, and the TCK number for
the card's client measured and recorded. Nothing else counts as progress — not a
reduced error count, not a compiling stub.
