# STATUS — mansart-jakarta-persistence

Loaded at the start of every session. Keep it under 60 lines, forever.
Maintained by `@tracker` only.

## Current focus

- Next: JP-24d — persist(): one-to-one relationship (owning side, FK write)  [TODO]
- M2: JP-23, JP-24a, JP-24b, JP-24c done — `persist(Object entity)`: null check, closed-EM check, EntityModel lookup,
  INSERT SQL via Dialect, bind parameters, persistence context registration, join table INSERTs for
  `@ManyToMany` (owning + inverse side), FK write for `@ManyToOne`, inverse collection maintenance for `@OneToMany` (inverse side). 16/16 unit tests.
  Full suite: 215 pass, 0 fail, 1 skip (core module); 161 pass (data module).
- Trap: stub provider still not wired (TCK baseline unchanged).

## Numbers

| metric | value | measured |
| --- | --- | --- |
| TCK PASS / total | 991 run, 989 errors, 2 skipped (same baseline — stub provider not wired) | 2026-08-28 |
| unit tests | 215 pass / 216 total (1 skipped, core module); 161 pass (data module) | 2026-08-31 |
| build | 34/34 (compile, `./mvnw -ntp clean compile -pl mansart-jakarta-persistence/mansart-persistence-core -am -DskipTests`) | 2026-08-31 |

TCK universe: 269 client classes, ~1 745 methods
(`jakarta.tck:persistence-tck-spec-tests:3.2.1`).

## Milestone

M0 (skeleton and harness) — 8 / 8 cards done.
M1 (metadata: APT) — 12 / 12 cards (JP-09…JP-20).
M2 (EntityManager CRUD) — 4 / 28 cards (JP-21, JP-22, JP-23, JP-24c done).

## Session log

2026-08-31 | JP-24c | `persist()` one-to-many inverse side: maintains inverse collection by updating target entity's FK column via UPDATE statement. 4/4 unit tests (PersistOneToManyTest). Full suite: 215 pass, 0 fail, 1 skip (core module).
2026-08-31 | JP-24a | `persist()` many-to-many (owning + inverse side): ~14 methods, plural attribute generation, ManyToManyAttribute/ManyToManyInverseAttribute dialect SPI, join table INSERTs via batched PreparedStatement. TCK 991 run, 989 errors, 2 skipped (same baseline). Unit 207 pass / 208 total (1 skipped, core module); 161 pass (data module).
2026-08-31 | JP-24c | `persist()` one-to-many inverse side: maintains inverse collection by updating target entity's FK column via UPDATE statement. 4/4 unit tests (PersistOneToManyTest). Full suite: 215 pass, 0 fail, 1 skip (core module).
2026-08-31 | JP-24b | `persist()` many-to-one: resolves `ReferenceAttribute` FK values — writes target entity's ID into the owning entity's FK column (nullable and non-null cases). 4/4 unit tests (PersistManyToOneTest). Full suite: 211 pass, 0 fail, 1 skip (core module).
2026-08-28 | JP-23 | `persist(Object entity)`: null check, closed-EM check, EntityModel lookup, INSERT SQL via Dialect, bind parameters, persistence context registration. 3/3 unit tests (PersistBasicTest). Full suite: 202 pass, 0 fail, 1 skip.
2026-08-28 | JP-22 | `MansartEntityManager` (64 methods): added `checkClosed()` helper; all 61 CRUD/query/utility methods now call `checkClosed()` before throwing `UnsupportedOperationException`. `isJoinedToTransaction()` returns `false` (no transaction support). `contains()` already checked `closed`. 59/59 unit tests (new `EntityManagerLifecycleTest` with 55 post-close tests + 4 lifecycle tests). TCK 991 run, 989 errors, 2 skipped (same baseline — stub provider not wired, but lifecycle is now correct).
2026-08-28 | JP-21 | `PersistenceContext` (identity map) + `MansartEntityManager` (64 JPA 3.2 methods, lifecycle wired: open/close/isOpen/isJoinedToTransaction) + factory returns live EM via all 4 `createEntityManager()` variants. 23/23 unit tests pass (4 stub-exception tests converted to live-EM tests).
2026-08-28 | JP-20 | `MansartPersistenceProvider.createContainerEntityManagerFactory()` implemented (persistence.xml parsing via `PersistenceUnitReader` + classpath scanning for `@Entity`/`@Embeddable`/`@MappedSuperclass` via `EntityScanner`). TCK pom.xml updated with standalone-mode properties. 125/125 unit tests (1 skipped).
2026-08-27 | JP-20 | Made `TypeImpl` concrete (removed `abstract`), made `MapAttributeImpl` no-declaringType ctor public, added explicit `<Object,Object,Object>` type args on `MapAttributeImpl` constructor call in `MansartPersistenceProvider.java` to resolve type inference. 34/34 build, 69/69 unit tests (assumed from prior). TCK 991 run, 989 errors, 2 skipped — all metamodelapi tests still error at `PMClientBase.setup()` NPE (same baseline, not resolved by this session).
2026-08-27 | JP-20 | Removed 5 unused imports (`Embedded`, `Id`, `Version`, `PersistentAttributeType`, `Basic`), dead `entityName.isEmpty()` ternary + `return null` → `IllegalArgumentException`, dead `if (mt != null)` check, unused `cls` param from `buildAttributes()`; updated Javadoc. All 7 Sonar issues on `MansartPersistenceProvider.java` resolved. 34/34 build, 69/69 unit tests (0 fail, 0 err). TCK 0/259 (same baseline — stub provider).
2026-08-27 | JP-20 | Cleaned 2 additional unused imports (`Version`, `PersistentAttributeType`) from `MansartPersistenceProvider.java` (2 MINOR). Updated Javadoc to match actual code (no longer references `@Version`/`@Embedded` which are unhandled). 69/69 unit tests pass. TCK 0/259 (same baseline — stub provider).
2026-08-27 | JP-20 | Fixed 7 Sonar issues in `MansartPersistenceProvider.java`: removed unused imports `Embedded`, `Id` (2 MINOR), removed dead `entityName.isEmpty()` ternary + replaced `return null` with `IllegalArgumentException` in `buildManagedType` (1 BLOCKER), removed dead `if (mt != null)` check in `buildMetamodel` (1 MAJOR), removed unused `cls` parameter from `buildAttributes` (1 MAJOR). 69/69 unit tests pass. TCK 0/259 (same baseline — stub provider).
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
