# STATUS — mansart-jakarta-persistence

Loaded at the start of every session. Keep it under 60 lines, forever.
Maintained by `@tracker` only.

## Current focus

- Next: M1 cards — expanding metadata/APT (JP-14+)
- M0 complete: 8 / 8 cards. M0 baseline: **992 tests, 990 errors, 0 failures, 2 skipped**
- JP-09 done: APT processes `@Entity`, generates JPA static metamodel (`ClassName_`). 34/34 build.
- JP-10 done: `MetamodelImpl.entity(String)` via name-based lookup. 34/34 build, 28/28 tests (3 new).
- JP-11 done: `EntityTypeImpl` + `IdentifiableTypeImpl` (getId, getVersion, getSupertype, etc). 34/34 build, 42/42 tests (14 new).
- JP-12 done: `PluralAttributeImpl` + `CollectionAttributeImpl` (getCollectionType, getElementType).
  Restructured: `AttributeImpl` extends `TypeImpl` (not `BindableImpl`). 34/34 build, 52/52 tests (10 new).
- JP-13 done: `BasicTypeImpl` extends `TypeImpl` (marker interface — no new methods). 34/34 build, 52/52 tests.

## Numbers

| metric | value | measured |
| --- | --- | --- |
| TCK PASS / total | **not measured** (full suite) — sig subset: 992/992 (990 entity errors, 0 failures, 2 skipped, 1 JPASigTest) | 2026-08-26 |
| unit tests | 52 (ProviderDiscoveryTest + PersistenceUnitReaderTest + MansartPersistenceProcessorTest + MetamodelEntityNameTest + EntityTypeImplTest + PluralAttributeImplTest) | 2026-08-27 |
| build | 34/34 (install -DskipTests) | 2026-08-27 |

TCK universe: 269 client classes, ~1 745 methods
(`jakarta.tck:persistence-tck-spec-tests:3.2.1`).

## Milestone

M0 (skeleton and harness) — 8 / 8 cards done.
M1 (metadata: APT) — 6 / 12 cards done (JP-09, JP-10, JP-11, JP-12, JP-13, JP-14).

## Session log

2026-08-27 14:00 — JP-12: `PluralAttributeImpl` + `CollectionAttributeImpl` (getCollectionType, getElementType, isCollection, getBindableType, getBindableJavaType). Restructured: `AttributeImpl` extends `TypeImpl`. 34/34 build, 52/52 unit tests (10 new).

2026-08-27 14:25 — JP-13: `BasicTypeImpl` (extends `TypeImpl`, marker interface — no new methods). `TypeImpl` + `BindableImpl` were already correct. 34/34 build, 52/52 unit tests. TCK 7 tests all error at setup (stub provider NPE) — same baseline.

2026-08-27 14:50 — JP-14: `SetAttributeImpl` + `ListAttributeImpl` (marker subclasses of `PluralAttributeImpl`) + `MapAttributeImpl` (adds `getKeyType()` + `getKeyJavaType()`). 34/34 build, 52/52 unit tests. TCK 12 tests all error at setup (stub provider NPE) — same baseline.

2026-08-26 14:35 — JP-09: APT processes `@Entity`, generates JPA static metamodel
(`ClassName_`). 34/34 modules build, 25/25 tests pass (23 existing + 2 new).

2026-08-27 09:30 — JP-10: `MetamodelImpl.entity(String)` implemented via name-based
lookup map. Fixed pre-existing compilation errors in `SingularAttributeImpl` (return
types for `getType()`, `getBindableJavaType()`, constructor casts), `ManagedTypeImpl`
(class cast in constructor), `EntityTypeImpl` (`getBindableJavaType()` return type).
Unit test `MetamodelEntityNameTest` (3 tests) verifies `entity(String)` resolves by
name, throws for unknown names, and throws for non-entity types. 34/34 modules build,
28/28 unit tests pass (3 new). Full TCK client blocked by stub provider (JP-03).

2026-08-27 10:00 — JP-11: `EntityTypeImpl` and `IdentifiableTypeImpl` fully implemented
  (getId, getDeclaredId, getVersion, getDeclaredVersion, getIdClassAttributes, getIdType,
  getDeclaredAttributes). Fixed `ManagedTypeImpl.getDeclaredAttributes()` to return
  `Set.copyOf(declaredAttributes)` (was casting `List` to `Set`, causing `ClassCastException`).
  Made `TypeImpl` constructor public for test accessibility. Unit test `EntityTypeImplTest`
  (14 tests) covers all implemented methods. 34/34 modules build, 42/42 unit tests pass
  (14 new).
