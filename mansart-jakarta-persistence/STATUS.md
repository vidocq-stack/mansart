# STATUS — mansart-jakarta-persistence

Loaded at the start of every session. Keep it under 60 lines, forever.
Maintained by `@tracker` only.

## Current focus

- Next card: **JP-07** — the TCK signature test subset
- JP-06 done: DDL extracted from TCK distribution, runner script manages PostgreSQL
  container + executes DDL (185 tables, 9 sprocs confirmed in container).
  TCK clients still fail at `PMClientBase.setup` (stub provider), not `setup*Data`
  (tables exist — the biggest error source is removed). 34/34 modules build.
- JP-05 done: standalone POM (model 4.0.0, no parent), runner script, README. Full TCK run: **991 tests, 989 errors, 0 failures, 2 skipped** (all failing — expected for a stub provider).
- JP-04 done: `PersistenceUnitReader` parses `persistence.xml` via StAX;
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

M0 (skeleton and harness) — 6 / 8 cards done.

## Session log

2026-08-25 20:45 — JP-01: reactor builds and installs, empty (8 sub-modules, 34/34 build).

2026-08-25 21:15 — JP-03: `MansartPersistenceProvider` stub (6 methods all throw `UnsupportedOperationException`), wired via JPMS `provides` + `META-INF/services`. 2 unit tests pass. 34/34 build.

2026-08-25 23:30 — JP-04: `PersistenceUnitReader` parses `persistence.xml` via StAX. 24/24 tests green. 34/34 build.

2026-08-26 00:30 — JP-05: out-of-reactor TCK runner skeleton. Created standalone
`mansart-persistence-tck/` module: `pom.xml` (model 4.0.0, no parent, profiles
`tck-run`/`tck-pg`/`tck-sig`), `run-official-tck-persistence-3.2.sh` (mirrors Data TCK
runner), `README.md`. Full TCK run resolves 991 test methods: **989 errors, 0 failures,
2 skipped** (all failing — expected for a stub provider). 34/34 modules build.

2026-08-26 12:00 — JP-06: PostgreSQL DDL extracted from TCK distribution and stored
as Maven resources (`src/test/resources/sql/postgresql/`). Runner script modified to
manage a named PostgreSQL container (`mansart-pg-tck`) and execute the official DDL
(748 lines schema + 29 lines stored procedures) before Maven runs. PostgreSQL 17
container shows 185 tables and 9 stored procedures — DDL execution infrastructure
proven. TCK tests still fail at `PMClientBase.setup` (stub provider throws
`UnsupportedOperationException`), NOT at `setup*Data` (tables exist, the single
biggest error source is removed). 34/34 modules build.
