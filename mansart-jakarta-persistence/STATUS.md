# STATUS — mansart-jakarta-persistence

Loaded at the start of every session. Keep it under 60 lines, forever.
Maintained by `@tracker` only.

## Current focus

- Next card: **JP-06** — the TCK schema comes from the official DDL
- JP-05 done: standalone POM (model 4.0.0, no parent), runner script, README. Full TCK run: **991 tests, 989 errors, 0 failures, 2 skipped** (all failing — expected for a stub provider).
- JP-04 done: `PersistenceUnitReader` parses `persistence.xml` using StAX;
  `PersistenceUnitInfoImpl` stores all fields. 22 unit tests pass.
- JP-03 done: `MansartPersistenceProvider` stub (all 6 methods throw `UnsupportedOperationException`), wired via JPMS `provides` + `META-INF/services`, ServiceLoader-discoverable. 2 unit tests pass.
- JP-01 done: reactor builds and installs, empty.

## Numbers

| metric | value | measured |
| --- | --- | --- |
| TCK PASS / total | not measured | — |
| unit tests | 24 (ProviderDiscoveryTest + PersistenceUnitReaderTest) | 2026-08-25 |
| build | 34/34 (install -DskipTests) | 2026-08-25 |

TCK universe: 269 client classes, ~1 745 methods
(`jakarta.tck:persistence-tck-spec-tests:3.2.1`).

## Milestone

M0 (skeleton and harness) — 5 / 8 cards done.

## Session log

2026-08-25 20:45 — JP-01: reactor builds and installs, empty.
Created 8 sub-module `pom.xml` (spi, processor, core, maven-plugin, cdi, tests,
external-lib, external-it), updated root `pom.xml` with module registration and
`dependencyManagement` entries. 34/34 modules build — `./mvnw -ntp install -DskipTests`.

2026-08-25 21:15 — JP-03: `MansartPersistenceProvider` stub. Implemented all 6
`PersistenceProvider` methods (each throws `UnsupportedOperationException`), wired
via JPMS `provides` + `META-INF/services/jakarta.persistence.spi.PersistenceProvider`
file. ECJ required `Map<?, ?>` signatures (not `Map<String, ?>`). 2 unit tests
pass — `ProviderDiscoveryTest` resolves provider via ServiceLoader and verifies
both `hasSize(1)` and `UnsupportedOperationException("not implemented:
createEntityManagerFactory")`. 34/34 modules build.

2026-08-25 23:30 — JP-04: `PersistenceUnitReader` reads `persistence.xml` via StAX
(`XMLInputFactory`/`XMLEventReader`), stores results in `PersistenceUnitInfoImpl`.
22 assertions cover: unit name, provider class, transaction-type (JTA/RESOURCE_LOCAL
default), managed class names, jta-data-source, non-jta-data-source, mapping-file,
jar-file (URL resolution), persistence-unit root URL, shared-cache-mode,
validation-mode, persistenceXMLSchemaVersion, properties (value attr and text body),
exclude-unlisted-classes, scope/qualifier annotations. 24/24 tests green.
34/34 modules build.

2026-08-26 00:30 — JP-05: out-of-reactor TCK runner skeleton. Created standalone
`mansart-persistence-tck/` module: `pom.xml` (model 4.0.0, no parent, profiles
`tck-run`/`tck-pg`/`tck-sig`), `run-official-tck-persistence-3.2.sh` (mirrors Data TCK
runner), `README.md`. Full TCK run resolves 991 test methods: **989 errors, 0 failures,
2 skipped** (all failing — expected for a stub provider). 34/34 modules build.
