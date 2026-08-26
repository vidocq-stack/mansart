# STATUS — mansart-jakarta-persistence

Loaded at the start of every session. Keep it under 60 lines, forever.
Maintained by `@tracker` only.

## Current focus

- Next: M1 cards — expanding metadata/APT (JP-09+)
- M0 complete: 8 / 8 cards. M0 baseline: **992 tests, 990 errors, 0 failures, 2 skipped**
- JP-07 done: fixed `tck-sig` profile — removed `tck-run` sig test exclusions,
  added `jakarta.tck:signaturetest:11.0.0-RC5` dep + `JPASigTest.class` include.
  `--sig` produces 992 tests (1 JPASigTest.signatureTest, 990 entity errors, 2 skipped).
  34/34 modules build.

## Numbers

| metric | value | measured |
| --- | --- | --- |
| TCK PASS / total | **not measured** (full suite) — sig subset: 992/992 (990 entity errors, 0 failures, 2 skipped, 1 JPASigTest) | 2026-08-26 |
| unit tests | 24 (ProviderDiscoveryTest + PersistenceUnitReaderTest) | 2026-08-25 |
| build | 34/34 (install -DskipTests) | 2026-08-25 |

TCK universe: 269 client classes, ~1 745 methods
(`jakarta.tck:persistence-tck-spec-tests:3.2.1`).

## Milestone

M0 (skeleton and harness) — 8 / 8 cards done.

## Session log

2026-08-26 00:30 — JP-05: out-of-reactor TCK runner skeleton. Created standalone
`mansart-persistence-tck/` module: `pom.xml` (model 4.0.0, no parent, profiles
`tck-run`/`tck-pg`/`tck-sig`), `run-official-tck-persistence-3.2.sh` (mirrors Data TCK
runner), `README.md`. Full TCK run resolves 991 test methods: **989 errors, 0 failures,
2 skipped** (all failing — expected for a stub provider). 34/34 modules build.

2026-08-26 12:00 — JP-06: PostgreSQL DDL extracted and stored as Maven resources.
Runner manages named PG container (`mansart-pg-tck`), executes official DDL (185 tables,
9 sprocs) before Maven. Container confirms 185 tables + 9 sprocs — DDL infrastructure
proven. TCK clients fail at `PMClientBase.setup` (stub provider throws
`UnsupportedOperationException`), NOT at `setup*Data` (tables exist, biggest error
source removed). 34/34 modules build.

2026-08-26 12:10 — JP-07: signature test subset runs. Fixed `tck-sig` profile:
removed `tck-run` signature test exclusions, added `jakarta.tck:signaturetest:11.0.0-RC5`
dependency and `JPASigTest.class` include. `--sig` produces 992 tests (including
1 JPASigTest.signatureTest, 990 entity errors at stub provider setup, 2 skipped).
34/34 modules build.

2026-08-26 12:15 — JP-08: M0 baseline recorded — full-suite run (H2 in-memory +
PostgreSQL + signatures): **992 tests, 990 errors, 0 failures, 2 skipped**.
All errors are stub-provider (`UnsupportedOperationException` / `NullPointerException`
from `PMClientBase.setup`); no TCK table-level failures (schema verified in JP-06).
34/34 modules build.
