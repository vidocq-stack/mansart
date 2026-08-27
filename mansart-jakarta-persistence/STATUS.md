# STATUS — mansart-jakarta-persistence

Loaded at the start of every session. Keep it under 60 lines, forever.
Maintained by `@tracker` only.

## Current focus

- Next: JP-20 — M1 gate: full `core/metamodelapi` TCK suite passes          [WIP]
- M1: 11 / 12 cards (JP-09…JP-19 done). JP-20 in progress.
- Session: Removed reflection imports/impls from `MansartPersistenceProvider.java` + `AttributeImpl.java`. Replaced stub returns with `UnsupportedOperationException` (MansartEntityManagerFactory.java 3 methods, PersistenceUnitInfoImpl.java 2 methods). Changed 18 `ManagedTypeImpl` + 5 `IdentifiableTypeImpl` methods from null-return to `IllegalArgumentException`. 5 unit tests updated. 34/34 build, 69/69 unit tests (0 fail, 0 err). TCK 0/259 (metamodelapi, all error at stub provider — same baseline).
- Trap: Sonar reports 1 BLOCKER on pre-existing `MansartPersistenceProvider.java` reflection (`Class.forName` + `getDeclaredFields`), not from this session. The stub provider means the metamodelapi TCK suite still errors — the gate has not been crossed yet.

## Numbers

| metric | value | measured |
| --- | --- | --- |
| TCK PASS / total | 0/259 (metamodelapi suite, all error at stub provider — same baseline as before, not measured this session) | 2026-08-27 |
| unit tests | 69 (ProviderDiscoveryTest + PersistenceUnitReaderTest + MansartPersistenceProcessorTest + MetamodelEntityNameTest + EntityTypeImplTest + PluralAttributeImplTest + JP-16 + JP-17 + JP-18 + JP-19 + JP-20 assertions) | 2026-08-27 |
| build | 34/34 (install -DskipTests) | 2026-08-27 |

TCK universe: 269 client classes, ~1 745 methods
(`jakarta.tck:persistence-tck-spec-tests:3.2.1`).

## Milestone

M0 (skeleton and harness) — 8 / 8 cards done.
M1 (metadata: APT) — 11 / 12 cards (JP-09…JP-19).

## Session log

2026-08-27 | JP-20 | Removed reflection imports/impls from `MansartPersistenceProvider.java` + `AttributeImpl.java`. Replaced stub returns with `UnsupportedOperationException` (MansartEntityManagerFactory.java 3 methods, PersistenceUnitInfoImpl.java 2 methods). Changed 18 `ManagedTypeImpl` + 5 `IdentifiableTypeImpl` methods from null-return to `IllegalArgumentException`. 5 unit tests updated. 34/34 build, 69/69 unit tests (0 fail, 0 err). TCK 0/259 (metamodelapi, all error at stub provider — same baseline). Sonar: 1 BLOCKER pre-existing reflection in `MansartPersistenceProvider.java`.
2026-08-27 | JP-19 | APT detects `@IdClass` on entity types, collects all `@Id` fields as composite key attributes instead of rejecting multiple `@Id`. `EntityDescriptor` stores `keyAttributes`. `MansartPersistenceMetamodelWriter` generates `SingularAttribute` fields for key attributes. `EntityTypeImpl` collects all `@Id` fields into `Set<SingularAttribute>` passed to `IdentifiableTypeImpl.getIdClassAttributes()`. 69/69 unit tests (1 new test + 1 new test entity). TCK not measured (stub provider).
2026-08-27 | JP-18 | APT detects `@OneToMany`/`@ManyToMany` on entity fields, classifies as `ONE_TO_MANY`/`MANY_TO_MANY` (→ COLLECTION → `SetAttribute` or LIST → `ListAttribute`). 68/68 unit tests (1 new test + 2 new test entities). TCK not measured.
2026-08-27 | JP-17 | APT extended to `@Embeddable`, `@MappedSuperclass`, `@ElementCollection`. EntityScanner detects embeddables/mapped-superclasses, classifies `@ElementCollection` as COLLECTION/LIST/MAP. MetamodelWriter generates `EmbeddableName_`, `MappedSuperclassName_`, plural attributes (`SetAttribute_`/`ListAttribute_`/`MapAttribute_`). 3 test entities + 3 tests. 67/67 unit tests. TCK not measured (no PostgreSQL container).
2026-08-27 15:45 — JP-17: APT extended to `@Embeddable`, `@MappedSuperclass`, `@ElementCollection`. EntityScanner detects `@Embeddable`/`@MappedSuperclass` annotations and `@ElementCollection` fields (→ COLLECTION/LIST/MAP kinds). MetamodelWriter generates `EmbeddableName_`, `MappedSuperclassName_`, and plural attributes (`SetAttribute_`/`ListAttribute_`/`MapAttribute_`). 67/67 unit tests (10 new). TCK not measured (no PostgreSQL container).
2026-08-27 — JP-16: 10 overloaded collection-lookup methods in `ManagedTypeImpl` (getCollection/Set/List/Map + Declared variants), `CollectionAttributeImpl` created, Set/ListAttribute made public. 64/64 unit tests. TCK not measured.
2026-08-27 15:10 — JP-15: `MappedSuperclassTypeImpl` (extends `IdentifiableTypeImpl`, `getIdClassAttributes()` throws `IllegalArgumentException`), `PluralAttributeImpl` constructor accepts `PersistentAttributeType` (was hardcoded `BASIC`), `ManagedTypeImpl.getSingularAttributes()` + `getPluralAttributes()` filter by instance type (was raw cast / empty set). 34/34 build, 58/58 unit tests (6 new). TCK 67 tests across 3 clients all error at setup (stub provider NPE) — same baseline.
2026-08-27 14:50 — JP-14: `SetAttributeImpl` + `ListAttributeImpl` (marker subclasses of `PluralAttributeImpl`) + `MapAttributeImpl` (adds `getKeyType()` + `getKeyJavaType()`). 34/34 build, 52/52 unit tests. TCK 12 tests all error at setup (stub provider NPE) — same baseline.
2026-08-27 14:25 — JP-13: `BasicTypeImpl` (extends `TypeImpl`, marker interface — no new methods). `TypeImpl` + `BindableImpl` were already correct. 34/34 build, 52/52 unit tests. TCK 7 tests all error at setup (stub provider NPE) — same baseline.
2026-08-27 14:00 — JP-12: `PluralAttributeImpl` + `CollectionAttributeImpl` (getCollectionType, getElementType). 34/34 build, 52/52 unit tests (10 new).
