# STATUS — mansart-jakarta-persistence

Loaded at the start of every session. Keep it under 60 lines, forever.
Maintained by `@tracker` only.

## Current focus

- Next: JP-17 — APT: `@Embeddable`, `@MappedSuperclass`, `@OneToMany`, `@ManyToMany`
- M0 complete: 8 / 8 cards. M1 now 8 / 12 (JP-09…JP-16 done).
- JP-16 done: 10 collection-lookup overloads in `ManagedTypeImpl` (getCollection/Set/List/Map
  + Declared variants), `CollectionAttributeImpl` created, Set/ListAttribute made public.
  64/64 unit tests. TCK not measured.
- Trap: JP-17 re-enters the APT processor (EntityScanner + MetamodelWriter) — the same
  files touched by JP-09. Must not regress the JP-09 metamodel output.

## Numbers

| metric | value | measured |
| --- | --- | --- |
| TCK PASS / total | **not measured** (full suite) — sig subset: 992/992 (990 entity errors, 0 failures, 2 skipped, 1 JPASigTest) | 2026-08-26 |
| unit tests | 64 (ProviderDiscoveryTest + PersistenceUnitReaderTest + MansartPersistenceProcessorTest + MetamodelEntityNameTest + EntityTypeImplTest + PluralAttributeImplTest + JP-16 assertions) | 2026-08-27 |
| build | 34/34 (install -DskipTests) | 2026-08-27 |

TCK universe: 269 client classes, ~1 745 methods
(`jakarta.tck:persistence-tck-spec-tests:3.2.1`).

## Milestone

M0 (skeleton and harness) — 8 / 8 cards done.
M1 (metadata: APT) — 8 / 12 cards done (JP-09…JP-16).

## Session log

2026-08-27 — JP-16: 10 overloaded collection-lookup methods in `ManagedTypeImpl` (getCollection/Set/List/Map + Declared variants), `CollectionAttributeImpl` created, Set/ListAttribute made public. 64/64 unit tests. TCK not measured.
2026-08-27 15:10 — JP-15: `MappedSuperclassTypeImpl` (extends `IdentifiableTypeImpl`, `getIdClassAttributes()` throws `IllegalArgumentException`), `PluralAttributeImpl` constructor accepts `PersistentAttributeType` (was hardcoded `BASIC`), `ManagedTypeImpl.getSingularAttributes()` + `getPluralAttributes()` filter by instance type (was raw cast / empty set). 34/34 build, 58/58 unit tests (6 new). TCK 67 tests across 3 clients all error at setup (stub provider NPE) — same baseline.
2026-08-27 14:50 — JP-14: `SetAttributeImpl` + `ListAttributeImpl` (marker subclasses of `PluralAttributeImpl`) + `MapAttributeImpl` (adds `getKeyType()` + `getKeyJavaType()`). 34/34 build, 52/52 unit tests. TCK 12 tests all error at setup (stub provider NPE) — same baseline.
2026-08-27 14:25 — JP-13: `BasicTypeImpl` (extends `TypeImpl`, marker interface — no new methods). `TypeImpl` + `BindableImpl` were already correct. 34/34 build, 52/52 unit tests. TCK 7 tests all error at setup (stub provider NPE) — same baseline.
2026-08-27 14:00 — JP-12: `PluralAttributeImpl` + `CollectionAttributeImpl` (getCollectionType, getElementType). 34/34 build, 52/52 unit tests (10 new).
