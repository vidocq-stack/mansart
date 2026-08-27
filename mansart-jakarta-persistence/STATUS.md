# STATUS — mansart-jakarta-persistence

Loaded at the start of every session. Keep it under 60 lines, forever.
Maintained by `@tracker` only.

## Current focus

- Next: M1 cards — expanding metadata/APT (JP-11+)
- M0 complete: 8 / 8 cards. M0 baseline: **992 tests, 990 errors, 0 failures, 2 skipped**
- JP-09 done: APT processes `@Entity`, generates JPA static metamodel (`ClassName_`).
  34/34 modules build, 25/25 unit tests pass.
- JP-10 done: `MetamodelImpl.entity(String)` implemented (name-based lookup).
  Fixed pre-existing compilation errors in `SingularAttributeImpl`, `ManagedTypeImpl`,
  `EntityTypeImpl` (return types for `getType()`, `getBindableJavaType()`, constructor casts).
  34/34 modules build, 28/28 unit tests pass (3 new).

## Numbers

| metric | value | measured |
| --- | --- | --- |
| TCK PASS / total | **not measured** (full suite) — sig subset: 992/992 (990 entity errors, 0 failures, 2 skipped, 1 JPASigTest) | 2026-08-26 |
| unit tests | 28 (ProviderDiscoveryTest + PersistenceUnitReaderTest + MansartPersistenceProcessorTest + MetamodelEntityNameTest) | 2026-08-27 |
| build | 34/34 (install -DskipTests) | 2026-08-27 |

TCK universe: 269 client classes, ~1 745 methods
(`jakarta.tck:persistence-tck-spec-tests:3.2.1`).

## Milestone

M0 (skeleton and harness) — 8 / 8 cards done.
M1 (metadata: APT) — 2 / 12 cards done (JP-09, JP-10).

## Session log

2026-08-26 12:00 — JP-06: PostgreSQL DDL extracted and stored as Maven resources.
Runner manages named PG container (`mansart-pg-tck`), executes official DDL (185 tables,
9 sprocs) before Maven. Container confirms 185 tables + 9 sprocs — DDL infrastructure
proven. TCK clients fail at `PMClientBase.setup` (stub provider throws
`UnsupportedOperationException`), NOT at `setup*Data` (tables exist, biggest error
source removed). 34/34 modules build.

2026-08-26 12:15 — JP-08: M0 baseline recorded — full-suite run (H2 in-memory +
PostgreSQL + signatures): **992 tests, 990 errors, 0 failures, 2 skipped**.
All errors are stub-provider (`UnsupportedOperationException` / `NullPointerException`
from `PMClientBase.setup`); no TCK table-level failures (schema verified in JP-06).
34/34 modules build.

2026-08-26 14:35 — JP-09: APT processes `@Entity`, generates JPA static metamodel
(`ClassName_`). 34/34 modules build, 25/25 tests pass (23 existing + 2 new).

2026-08-27 09:30 — JP-10: `MetamodelImpl.entity(String)` implemented via name-based
lookup map. Fixed pre-existing compilation errors in `SingularAttributeImpl` (return
types for `getType()`, `getBindableJavaType()`, constructor casts), `ManagedTypeImpl`
(class cast in constructor), `EntityTypeImpl` (`getBindableJavaType()` return type).
Unit test `MetamodelEntityNameTest` (3 tests) verifies `entity(String)` resolves by
name, throws for unknown names, and throws for non-entity types. 34/34 modules build,
28/28 unit tests pass (3 new). Full TCK client blocked by stub provider (JP-03).
