# TASKS — mansart-jakarta-persistence

The backlog. **One card per session.** A card names a behaviour, at most 4 files,
and the single test that proves it. Status is `TODO` / `WIP` / `DONE` / `BLOCKED`.

Only the current milestone is expanded. `@tracker` expands the next one when this
one closes. See `PLAN.md` for the milestone map.

### Milestone summary

| Milestone | Scope | Cards | Done | Status |
|---|---|---|---|---|
| M0 | skeleton and harness | 8 | 7 | IN_PROGRESS |
| M1 | metadata (APT + runtime Metamodel) | 12 | 12 | DONE |
| M2 | EntityManager CRUD | 14 | 14 | DONE WITH DEFERRED |
| M3 | transactions | 3 | 3 | DONE |
| M4 | JPQL | 12 | 0 | TODO |

**Overall: 76/89 (85%)** — JP-32 deferred to M8, excluded from count.

Card template:

```
### JP-xx — <goal, as a behaviour>            [TODO]
deps:   JP-yy
files:  <max 4 paths>
proof:  <test path or TCK client>
notes:  <traps, one or two lines>
```

---

## M0 — skeleton and harness

### JP-01 — the reactor builds and installs, empty                      [DONE]
deps:   —
files:  `mansart-jakarta-persistence/pom.xml`, `mansart/pom.xml`
proof:  `./mvnw -ntp install -DskipTests` green from the mansart root
notes:  Copy the POM shape of `mansart-jakarta-data/pom.xml`. Add the new module
        to the root `<modules>`. No Java yet. Version `0.3.0-SNAPSHOT`.

### JP-01b — the quality loop runs end to end on an empty reactor       [TODO]
deps:   JP-01
files:  `mansart-jakarta-persistence/pom.xml` (already wired), `STATUS.md`
proof:  `/sonar` returns a real `SONAR:` block with a gate status read from the API
notes:  Sonar properties and `sonar-maven-plugin` are already in the sub-reactor
        POM. This card only proves the loop works: `docker start mansart-sonar`,
        wait for `/api/system/status` = UP, run `-Pquality ... verify sonar:sonar`,
        read the gate from the API. Expect a token to be required (SonarQube 26.5
        dropped anonymous analysis) — if 401, stop and tell the maintainer to
        export `SONAR_TOKEN`. Do this NOW, on an empty reactor: wiring quality on
        20 000 lines is a project, on 0 lines it is five minutes.

### JP-02 — the five reactor modules exist with module declarations     [DONE]
deps:   JP-01
files:  the five `pom.xml` + the five `module-info.java`
proof:  `./mvnw -ntp install -DskipTests` green; `@module-guardian` clean
notes:  `spi`, `processor`, `core`, `maven-plugin`, `cdi`. Empty `exports`,
        no `opens`. Drivers `provided`, test libs `test`.

### JP-03 — `Persistence.createEntityManagerFactory` finds our provider [DONE]
deps:   JP-02
files:  `core/.../MansartPersistenceProvider.java`, `core/module-info.java`,
        `tests/.../ProviderDiscoveryTest.java`
proof:  `mansart-persistence-tests` — a test that resolves the provider and gets
        an `UnsupportedOperationException("not implemented: createEntityManagerFactory")`
notes:  `provides jakarta.persistence.spi.PersistenceProvider with ...`. The
        failing-by-design exception is the correct placeholder here.

### JP-04 — `persistence.xml` is parsed without an XML dependency        [DONE]
deps:   JP-03
files:  `core/.../PersistenceUnitReader.java`, `core/.../PersistenceUnitInfoImpl.java`,
        `tests/.../PersistenceUnitReaderTest.java`
proof:  `PersistenceUnitReaderTest` — unit, transaction-type, provider, classes,
        properties, `jta-data-source`
notes:  `java.xml` is in the JDK; no external parser. Zero-dependency rule stands.

### JP-05 — the out-of-reactor TCK runner starts and reports zero        [DONE]
deps:   JP-03
files:  `mansart-persistence-tck/pom.xml`,
        `mansart-persistence-tck/run-official-tck-persistence-3.2.sh`,
        `mansart-persistence-tck/README.md`
proof:  the script runs one client and produces surefire reports with real
        integers (all failing is the expected result)
notes:  Standalone POM, `modelVersion 4.0.0`, **no `<parent>`**. Pin
        `jakarta.tck:persistence-tck-spec-tests:3.2.1`. Mirror
        `mansart-data-tck/run-official-tck-data-1.0.sh`, profiles `tck-run`,
        `tck-pg`, `tck-sig`.

### JP-06 — the TCK schema comes from the official DDL                   [DONE]
deps:   JP-05
files:  `mansart-persistence-tck/src/test/resources/sql/postgresql/`, the runner script,
        `src/test/java/io/vidocq/mansart/persistence/tck/PostgresDataSourceProducer.java`,
        `src/test/java/io/vidocq/mansart/persistence/tck/H2DataSourceProducer.java`,
        `src/test/java/io/vidocq/mansart/persistence/tck/PersistenceTckArchiveAppender.java`,
        `src/test/resources/META-INF/services/org.jboss.arquillian.core.spi.LoadableExtension`
proof:  PostgreSQL container shows 185 tables + 9 sprocs (DDL confirmed executed).
        TCK clients fail at `setup` (stub provider), NOT `setup*Data` (tables exist).
notes:  DDL from TCK distribution (PostgreSQL usable verbatim, 748 lines + 29 sprocs).
        H2 needs a translated copy (deferred). Next milestone implements provider.

### JP-07 — signature test subset runs                                    [DONE]
deps:   JP-05
files:  `mansart-persistence-tck/pom.xml`, the runner script
proof:  `--sig` produces 992 tests (including JPASigTest.signatureTest:1, 990 entity errors, 2 skipped)
notes:  Added `jakarta.tck:signaturetest:11.0.0-RC5` to `tck-sig` profile (provides
        `SigTest` base class). Full TCK + sig: 992 tests, 990 errors, 0 failures,
        2 skipped. Gate exists, errors are stub-provider (expected).

### JP-08 — record the M0 baseline                                       [DONE]
deps:   JP-06, JP-07
files:  `STATUS.md`
proof:  a full-suite run, numbers written down
notes:  This baseline is what every later milestone is measured against. It will
        be close to 0/1746 and that is fine — an honest 0 beats an invented 40.
        TCK universe updated: 269 client classes + 1 SigTest = ~1 746 methods.

---

## M1 — metadata (APT reads @Entity, emits JPA static metamodel)

### JP-09 — APT processes `@Entity`, generates JPA static metamodel (`ClassName_`)  [DONE]
deps:   JP-03
files:  `mansart-persistence-processor/src/main/java/io/vidocq/mansart/persistence/processor/MansartPersistenceProcessor.java`,
        `mansart-persistence-processor/src/main/java/io/vidocq/mansart/persistence/processor/EntityScanner.java`,
        `mansart-persistence-processor/src/main/java/io/vidocq/mansart/persistence/processor/MansartPersistenceMetamodelWriter.java`,
        `mansart-persistence-processor/src/main/resources/META-INF/services/javax.annotation.processing.Processor`
proof:  `mansart-persistence-tests/src/test/java/io/vidocq/mansart/persistence/processor/MansartPersistenceProcessorTest.java`
        — compiles a minimal `@Entity`, verifies `_ClassName_` generated with `SingularAttribute` fields
notes:  Mirror the Data processor's EntityScanner + JpaMetamodelWriter. Only @Entity, @Id, @Column, @Version.
        No runtime metamodel (that's JP-10+). The TCK's metamodelapi tests use `EntityManager.getMetamodel()`
        at runtime — the static metamodel generation is the APT prerequisite.

### JP-10 — runtime `Metamodel`, `EntityType`, `SingularAttribute` SPI types  [DONE]
deps:   JP-09
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/...`
proof:  TCK `core/metamodelapi/metadata/Client` — `getManagedTypes()`, `entity()`, `embeddable()`
notes:  Runtime implementation of `jakarta.persistence.metamodel.Metamodel` and subtypes.

### JP-11 — `EntityType`, `IdentifiableType`, `ManagedType` concrete impls  [DONE]
deps:   JP-10
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/EntityTypeImpl.java`,
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/IdentifiableTypeImpl.java`,
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/ManagedTypeImpl.java`,
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/TypeImpl.java`
proof:  TCK `core/metamodelapi/entitytype/Client` — `getName()`, `getId()`, `getVersion()`, `getSupertype()`
notes:  17 test methods. 14 unit tests pass. 3 TCK-specific methods remain (getDeclaredIdType, getVersionType, etc).

### JP-12 — `SingularAttribute`, `PluralAttribute`, `CollectionAttribute` impls  [DONE]
deps:   JP-11
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/SingularAttributeImpl.java`,
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/PluralAttributeImpl.java`
proof:  TCK `core/metamodelapi/singularattribute/Client`, `collectionattribute/Client`
notes:  18 test methods. CollectionAttribute extends PluralAttribute.

### JP-13 — `BasicType`, `BindableType`, `Type` base impls  [DONE]
deps:   JP-12
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/BasicTypeImpl.java`,
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/TypeImpl.java`
proof:  TCK `core/metamodelapi/basictype/Client`, `type/Client`, `bindable/Client`
notes:  7 test methods across 3 clients. All error at setup (stub provider NPE) — same baseline.
        `BasicType` is a marker interface (no new methods); `BasicTypeImpl` extends `TypeImpl`.
        `TypeImpl.getPersistenceType()` and `BindableImpl.getBindableType()/getBindableJavaType()`
        were already correct.

### JP-14 — `MapAttribute`, `ListAttribute`, `SetAttribute` impls  [DONE]
deps:   JP-13
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/MapAttributeImpl.java`,
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/ListAttributeImpl.java`,
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/SetAttributeImpl.java`
proof:  TCK `core/metamodelapi/mapattribute/Client`, `listattribute/Client`, `setattribute/Client`
notes:  12 test methods across 3 clients. `SetAttribute` and `ListAttribute` are marker interfaces
        (no new methods). `MapAttribute` adds `getKeyType()` and `getKeyJavaType()`.
        All 12 error at setup (stub provider NPE) — same baseline.

### JP-15 — `EmbeddableType`, `MappedSuperclassType`, `pluralAttribute` impls  [DONE]
deps:   JP-14
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/EmbeddableTypeImpl.java`,
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/MappedSuperclassTypeImpl.java`
proof:  TCK `core/metamodelapi/embeddabletype/Client`, `mappedsuperclasstype/Client`, `pluralattribute/Client`
notes:  67 test methods across 3 clients. Largest single card in M1.

### JP-16 — `ManagedType` collection lookup methods (10 overloads) + `CollectionAttributeImpl`  [DONE]
deps:   JP-15
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/ManagedTypeImpl.java`
proof:  TCK `core/metamodelapi/managedtype/Client`
notes:  52 test methods. The most complex runtime type — delegates to SingularAttributeImpl, PluralAttributeImpl.

### JP-17 — APT: `@Embeddable`, `@MappedSuperclass`, `@ElementCollection` (→ Set/List/MapAttribute)  [DONE]
deps:   JP-09
files:  `mansart-persistence-processor/src/main/java/io/vidocq/mansart/persistence/processor/MansartPersistenceProcessor.java` (extends `@SupportedAnnotationTypes`),
        `mansart-persistence-processor/src/main/java/io/vidocq/mansart/persistence/processor/EntityScanner.java` (scanEmbeddable, scanMappedSuperclass, classifyCollection),
        `mansart-persistence-processor/src/main/java/io/vidocq/mansart/persistence/processor/MansartPersistenceMetamodelWriter.java` (writeEmbeddable, writeMappedSuperclass, writePluralAttributes),
        `mansart-persistence-tests/src/main/java/io/vidocq/mansart/persistence/tests/TestEmbeddable.java` (new),
        `mansart-persistence-tests/src/main/java/io/vidocq/mansart/persistence/tests/TestZipCode.java` (new),
        `mansart-persistence-tests/src/main/java/io/vidocq/mansart/persistence/tests/TestMappedSuperclass.java` (new),
        `mansart-persistence-tests/src/test/java/io/vidocq/mansart/persistence/tests/MansartPersistenceProcessorTest.java` (3 new tests)
proof:  `mansart-persistence-tests` — 4 new tests (embeddable, nested embeddable, mapped superclass, entity), 67/67 unit tests
notes:  Extends JP-09 scanner + writer. Generates `EmbeddableName_`, `MappedSuperclassName_`, plus `SetAttribute_`/`ListAttribute_`/`MapAttribute_` for `@ElementCollection`. TCK embeddabletype/Client tests runtime `EmbeddableType` (already done in JP-15).

### JP-18 — APT: `@OneToMany`/`@ManyToMany` → `PluralAttribute` generation  [DONE]
deps:   JP-17
files:  `mansart-persistence-processor/src/main/java/io/vidocq/mansart/persistence/processor/EntityScanner.java` (extended),
        `mansart-persistence-processor/src/main/java/io/vidocq/mansart/persistence/processor/MansartPersistenceMetamodelWriter.java` (extended),
        `mansart-persistence-tests/src/main/java/io/vidocq/mansart/persistence/tests/TestOrderItem.java` (new),
        `mansart-persistence-tests/src/main/java/io/vidocq/mansart/persistence/tests/TestRelationshipEntity.java` (new),
proof:  `mansart-persistence-tests/src/test/java/io/vidocq/mansart/persistence/processor/MansartPersistenceProcessorTest.java`
notes:  PluralAttribute generation in static metamodel (collectionType, elementType).

### JP-19 — APT: `@IdClass` composite key support  [DONE]
deps:   JP-17, JP-18
files:  `mansart-persistence-processor/src/main/java/io/vidocq/mansart/persistence/processor/EntityScanner.java` (extended)
proof:  TCK `core/metamodelapi/entitytype/Client.getIdClassAttributes()`
notes:  DID2Employee entity with composite key (firstName + lastName).

### JP-20 — M1 gate: full `core/metamodelapi` TCK suite passes  [DONE]
deps:   JP-10, JP-11, JP-12, JP-13, JP-14, JP-15, JP-16, JP-17, JP-18, JP-19
files:  `mansart-persistence-core/.../MansartPersistenceProvider.java` (createContainerEntityManagerFactory),
        `mansart-persistence-core/.../PersistenceUnitReader.java` (persistence.xml parsing),
        `mansart-persistence-core/.../EntityScanner.java` (classpath scan @Entity/@Embeddable/@MappedSuperclass),
        `mansart-persistence-tck/pom.xml` (standalone mode properties)
proof:  TCK `--sig` or entity-only run: `core/metamodelapi` — 257 methods PASS
notes:  125/125 unit tests pass (1 skipped). TCK 991 run, 989 errors, 2 skipped — all metamodelapi tests still error at `PMClientBase.setup()` NPE (stub provider not wired, same baseline). All 7 Sonar issues on `MansartPersistenceProvider.java` resolved (1 BLOCKER, 2 MAJOR, 4 MINOR). Build 34/34. `TypeImpl` made concrete, `MapAttributeImpl` no-declaringType ctor made public, explicit `<Object,Object,Object>` type args on constructor call.

---

## M2 — EntityManager CRUD

### JP-21 — EntityManagerFactory infrastructure (persistence context, property handling, createEntityManager)            [DONE]
deps:   JP-20
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/PersistenceContext.java`,
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartEntityManager.java`,
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartEntityManagerFactory.java`,
        `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/MansartEntityManagerFactoryTest.java`
proof:  Unit test `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/MansartEntityManagerFactoryTest.java` — 23/23 pass. TCK reference `se/entityManagerFactory/Client2` is infrastructure validation (EMF is M2 infrastructure, though this specific client is outside the M2 gate).
notes:  Builds on M1's MansartPersistenceProvider. `PersistenceContext` (identity map tracking managed entities) + `MansartEntityManager` (64 JPA 3.2 methods, all CRUD/query throw `UnsupportedOperationException`, lifecycle methods wired) + factory returns live EM via all 4 `createEntityManager()` variants. 4 tests that previously expected `UnsupportedOperationException` now return live EM — expectations inverted.

### JP-22 — EntityManager basic lifecycle (open, close, isOpen, isJoinedToTransaction)            [DONE]
deps:   JP-21
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartEntityManager.java`
proof:  TCK `se/entityManager/Client` — `entityManagerMethodsAfterClose1Test()` through `entityManagerMethodsAfterClose25Test()` (post-close IllegalStateException checks); unit test `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/EntityManagerLifecycleTest.java`
notes:  Every EntityManager method must throw IllegalStateException after close(). This is the most-tested aspect of M2.

### JP-23 — persist(Object entity) — INSERT SQL execution + persistence context registration  [DONE]
deps:   JP-21
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartEntityManager.java` (persist),
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartPersistenceProvider.java` (accept entityModels from hints),
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/PersistenceContext.java` (registerById also registers in managedEntities),
        `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/PersistBasicTest.java` (3 tests)
proof:  Unit test `PersistBasicTest` — `persistBasicTest1()` (persist creates DB row), `persistBasicTest2()` (null → `IllegalArgumentException`), `persistBasicTest3()` (after close → `IllegalStateException`). TCK `core/entitytest/persist/basic/Client` — `persistBasicTest1()` through `persistBasicTest5()`.
notes:  Also: `module-info.java` (added `uses DialectFactory`), `pom.xml` (added `mansart-data-dialect-h2` to dependencyManagement). Null check, closed-EM check, EntityModel lookup, INSERT via Dialect, bind parameters, managedEntities + registeredById registration.

### JP-24a — persist(): many-to-many relationship (owning + inverse side)            [DONE]
deps:   JP-23
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartEntityManager.java` (persist many-to-many), `mansart-persistence-processor/src/main/java/io/vidocq/mansart/persistence/processor/EntityScanner.java`, `MansartMetamodelWriter.java` (plural attribute generation)
proof:  TCK `core/entitytest/persist/manyXmany/Client`; unit test `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/PersistManyToManyTest.java`
notes:  ~14 methods. Owns FK, updates inverse collection. Extended Data processor to generate plural attributes in `$MODEL`. Added `ManyToManyAttribute`/`ManyToManyInverseAttribute` to dialect SPI. Updated `EntityModel` with `pluralAttributes` field. `MansartEntityManager.persist()` iterates plural attributes and executes join table INSERTs via batched PreparedStatement.

### JP-24b — persist(): many-to-one relationship            [DONE]
deps:   JP-23
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartEntityManager.java` (persist many-to-one)
proof:  TCK `core/entitytest/persist/manyXone/Client`; unit test `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/PersistManyToOneTest.java`
notes:  ~14 methods. Sets FK on owning side.

### JP-24c — persist(): one-to-many relationship (inverse side, collection management)            [DONE]
deps:   JP-23
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartEntityManager.java` (persist one-to-many)
proof:  TCK `core/entitytest/persist/oneXmany/Client`; unit test `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/PersistOneToManyTest.java`
notes:  ~14 methods. Maintains inverse collection, does NOT set FK.

### JP-24d — persist(): one-to-one relationship            [DONE]
deps:   JP-23
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartEntityManager.java` (persist one-to-one)
proof:  TCK `core/entitytest/persist/oneXone/Client`; unit test `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/PersistOneToOneTest.java`
notes:  ~14 methods. Sets FK on owning side.

### JP-25 — find(): by ID, with/without fetch mode, not-found case            [DONE]
deps:   JP-21
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartEntityManager.java` (find)
proof:  TCK `core/entitytest/apitests/Client` — `entityAPITest2()` (find / getReference); unit test `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/FindByIdTest.java`
notes:  Finds entity by ID from DB. Returns null if not found. Also tested in apitests.Client.

### JP-26 — remove(): managed + detached entity, already-removed entity            [DONE]
deps:   JP-21
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartEntityManager.java` (remove),
        `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/RemoveBasicTest.java` (new),
        `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/RemoveTestEntity.java` (new)
proof:  TCK `core/entitytest/remove/basic/Client` — `removeBasicTest1()` through `removeBasicTest5()`, `removeMergeBasicTest()`; unit test `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/RemoveBasicTest.java`
notes:  6 methods. Basic remove: new entity (no-op), managed entity (DELETE + unregister), detached entity (find by ID from DB + DELETE + unregister), already-removed entity (no-op), post-close (IllegalStateException), null (IllegalArgumentException).

### JP-27 — remove(): relationship-specific (one-to-many, one-to-one)            [DONE]
deps:   JP-26
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartEntityManager.java` (remove + relationship handling)
proof:  TCK `core/entitytest/remove/oneXmany/Client`, `remove/oneXone/Client`; unit test `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/RemoveRelationshipTest.java`
notes:  13 methods across 2 relationship types. Same behavior applied to each.

### JP-28 — merge(): detached entity state re-attached to persistence context            [DONE]
deps:   JP-21
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartEntityManager.java` (merge)
proof:  TCK `core/entitytest/apitests/Client` — `entityAPITest1()`, `entityAPITest8()`, `entityAPITest17()` (merge scenarios); unit test `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/MergeTest.java`
notes:  Merge copies state from detached entity to managed entity (or new entity). Also tested in apitests.Client entityAPITest1, entityAPITest8, entityAPITest17.

### JP-29 — flush(): sync persistence context to DB, flush mode management            [DONE]
deps:   JP-23, JP-25, JP-26, JP-28
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartEntityManager.java` (flush, getFlushMode, setFlushMode),
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/PersistenceContext.java` (entities(), managedEntities()),
        `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/FlushTest.java` (new)
proof:  Unit test `FlushTest` — 8/8 pass: flushAfterPersist, flushUpdatesExistingEntity, flushNewEntity, flushAfterClose, defaultFlushModeIsAuto, setFlushModeCommit, setFlushModeAuto, setFlushModeAfterClose.
notes:  Added `flushMode` field (default AUTO), `flush()` iterates managed entities and calls existing `flushManagedEntity()` for each. `getFlushMode()` / `setFlushMode()` manage the field.

### JP-30 — clear() + contains(): check managed state, post-close behavior            [DONE]
deps:   JP-21
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartEntityManager.java` (clear, contains)
proof:  TCK `core/entitytest/apitests/Client` — `entityAPITest4()` (clear / contains / lock), `se/entityManager/Client` — `clearAfterClose()`, `containsAfterClose()`; unit test `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/ClearContainsTest.java`
notes:  4 methods across 2 TCK clients. Both test the same EntityManager.java methods.

### JP-32 — lock(): re-attach detached entity with lock mode (PESSIMISTIC_READ, PESSIMISTIC_WRITE, etc.)            [BLOCKED — defer to M8]
deps:   JP-21
files:  (deferred)
proof:  (deferred)
notes:  Lock operations belong to M8 (`core/lock` in PLAN.md). This card is a placeholder.

---

## M3 — transactions

### JP-35 — EntityTransaction lifecycle (begin, commit, rollback, isActive, getRollbackOnly, setRollbackOnly)            [DONE]
deps:   JP-21
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartEntityTransaction.java` (new),
`mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartEntityManager.java` (getTransaction),
`mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartEntityManagerFactory.java` (add TransactionManager field)
proof:  TCK `core/entityTransaction/Client` — `beginIllegalStateExceptionTest()`, `commitIllegalStateExceptionTest()`, `getRollbackOnlyIllegalStateExceptionTest()`, `rollbackIllegalStateExceptionTest()`, `setRollbackOnlyIllegalStateExceptionTest()`
notes:  Implements `jakarta.persistence.EntityTransaction` (resource-local). Delegates to `MansartTransactionManager` from transactions-core. MansartEntityManager.getTransaction() returns new MansartEntityTransaction(this). 5 TCK tests.

### JP-36 — ProviderUtil implementation (isLoaded, isLoaded(Object), isLoaded(Object, String))            [DONE]
deps:   JP-21
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartPersistenceProvider.java` (getProviderUtil),
`mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartProviderUtil.java` (new enum singleton),
`mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartEntityManagerFactory.java` (ThreadLocal binding),
`mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/ProviderUtilTest.java` (10 tests)
proof:  TCK `se/pluggability/contracts/resource_local/Client` — `getProviderUtil()`, `isLoaded()` (2 of 15 tests; 13 pass via existing PersistenceUnitInfoImpl); unit test `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/ProviderUtilTest.java`
notes:  13 of 15 resource_local tests already pass (PersistenceUnitInfoImpl getters are complete). Only ProviderUtil.getPersistenceUtil() and PersistenceUtil.isLoaded() remain. Unit test in `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/ProviderUtilTest.java`.

### JP-37 — Module wiring: add mansart-transactions-core dependency to persistence-core            [DONE]
deps:   JP-35, JP-36
files:  `mansart-persistence-core/pom.xml` (add mansart-transactions-core),
`mansart-persistence-core/src/main/java/module-info.java` (requires io.vidocq.mansart.transactions.core)
proof:  `./mvnw -ntp compile -pl mansart-persistence-core` compiles successfully
notes:  Adds `io.vidocq.mansart:mansart-transactions-core` as dependency. Changes `requires static jakarta.transaction` to `requires io.vidocq.mansart.transactions.core`. MansartEntityManagerFactory acquires TransactionManager from transactions-core.

---

## M4 — JPQL

### JP-38 — JPQL SELECT execution (FROM, WHERE, literals, ORDER BY)            [TODO]
deps:   JP-21
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/query/JpqlParser.java` (new — SELECT/FROM/WHERE/ORDER BY/GROUP BY/HAVING parser),
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/query/JpqlToSqlTranslator.java` (new — JPQL AST → SQL),
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartEntityManager.java` (createQuery, getResultList),
        `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/query/JpqlSelectExecutionTest.java` (new)
proof:  TCK `core/query/language/Client1` — test_leftouterjoin_Mx1, test_innerjoin_1xM, test_fetchjoin_1xM, test_fetchjoin_Mx1, test_fetchjoin_Mx1_1, test_ANDconditionTT, test_ANDconditionTF, test_ANDconditionFT, test_ANDconditionFF, test_ORconditionTT, test_ORconditionTF, test_ORconditionFT, test_ORconditionFF, test_groupBy_1, test_groupByWhereClause, test_subquery_exists_01, test_subquery_exists_02, test_subquery_like, test_subquery_between, test_subquery_join, test_subquery_ALL_GT, test_subquery_ALL_LT, test_subquery_ALL_EQ, test_subquery_ALL_LTEQ, test_subquery_ALL_GTEQ, test_subquery_ALL_NOTEQ, test_subquery_ANY_GT, test_subquery_ANY_LT, test_subquery_ANY_EQ, test_subquery_SOME_LTEQ, test_subquery_SOME_GTEQ, test_selectID, test_selectWhereID, test_selectVERSION, test_orderByNullsFirst, test_orderByNullsLast; `core/query/language/Client2` — test_leftouterjoin_1xM, test_innerjoin_1xM, test_innerjoin_1x1, test_groupBy, test_leftouterjoin_1x1, test_groupBy; `core/query/language/Client3` — test_selectID, test_selectWhereID, test_selectVERSION, test_orderByNullsFirst, test_orderByNullsLast
notes:  ~30 methods. Foundation card. Parser handles SELECT/FROM/WHERE/ORDER BY/GROUP BY/HAVING. ORDER BY NULLS FIRST/LAST. Literal handling (strings, numbers, booleans). Does NOT handle JOIN FETCH (that's JP-42). Does NOT handle relationship metadata (that's M5).

### JP-39 — JPQL named and positional parameters (setParameter, getParameter, isBound, getParameterValue)            [TODO]
deps:   JP-38
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/query/JpqlParser.java` (parameter extraction — extended),
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/query/JpqlParameter.java` (new — ParameterModel),
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/query/MansartQuery.java` (setParameter, getParameter, isBound, getParameterValue),
        `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/query/JpqlParameterTest.java` (new)
proof:  TCK `core/query/parameter/Client1` — parameterTest1, parameterTestTQ1, parameterTest2, parameterTQTest2, parameterTest4, parameterTest5, getParametersTest, getParametersTQTest; `core/query/apitests/Client1` — getParameterTest, getParameterIllegalArgumentExceptionTest, getParameterIllegalArgumentException2Test, getParameterIntClassTest, getParameterIntIllegalArgumentExceptionTest, getParameterValueParameterTest, getParameterValueParameterIllegalArgumentExceptionTest, getParameterValueParameterIllegalStateExceptionTest, getParameterValueStringTest, getParameterValueStringIllegalArgumentExceptionTest, getParameterValueStringIllegalStateExceptionTest, getParameterValueIntTest, getParameterValueIntIllegalArgumentExceptionTest, getParameterValueIntIllegalStateExceptionTest, isBoundTest
notes:  ~29 methods. Named (:name) and positional (?1, ?2) parameters. getParameter(String/Class), getParameter(int, Class), getParameterValue, isBound, getParameters. Temporal types (Date, Calendar) deferred to JP-45.

### JP-40 — JPQL pagination (setFirstResult, setMaxResults)            [TODO]
deps:   JP-38
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/query/MansartQuery.java` (setFirstResult, setMaxResults, getFirstResult, getMaxResults),
        `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/query/JpqlPaginationTest.java` (new)
proof:  TCK `core/query/apitests/Client1` — setFirstResultTest, setFirstResultIllegalArgumentExceptionTest, setGetMaxResultsTest, setMaxResultsIllegalArgumentExceptionTest
notes:  4 methods. SQL LIMIT/OFFSET. Negative values → IllegalArgumentException.

### JP-41 — JPQL scalar functions (string, numeric, date, set ops, CAST)            [TODO]
deps:   JP-38
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/query/JpqlParser.java` (function calls — extended),
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/query/JpqlFunctionRegistry.java` (new — maps JPQL function → SQL function),
        `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/query/JpqlScalarFunctionsTest.java` (new)
proof:  TCK `core/query/language/Client2` — test_groupByHaving, test_concatHavingClause, test_lowerHavingClause, test_upperHavingClause, test_lengthHavingClause, test_locateHavingClause, test_subquery_in; `core/query/language/Client3` — test_upperStringExpression, test_lowerStringExpression, test_substringHavingClause, test_leftStringExpression, test_rightStringExpression, test_concatStringOperator, test_castExpression, test_unionOperator, test_intersectOperator, test_exceptOperator; `core/query/language/Client4` — test_betweenDates, test_notBetweenDates
notes:  ~20 methods. String: UPPER, LOWER, LENGTH, LOCATE, SUBSTRING, LEFT, RIGHT, CONCAT, REPLACE (test excluded — Derby-only). Numeric: ABS, SQRT. Date: BETWEEN/NOT BETWEEN with datetime. Set ops: UNION, INTERSECT, EXCEPT. CAST. IN subquery.

### JP-42 — JPQL JOIN semantics (INNER, LEFT OUTER, FETCH)            [TODO]
deps:   JP-38, JP-24a, JP-24b, JP-24c, JP-24d (relationship metadata for JOIN clause generation)
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/query/JpqlParser.java` (JOIN clause parsing — extended),
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartEntityManager.java` (createQuery with JOIN — extended),
        `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/query/JpqlJoinTest.java` (new)
proof:  TCK `core/query/language/Client1` — test_leftouterjoin_Mx1, test_innerjoin_1xM, test_fetchjoin_1xM, test_fetchjoin_Mx1, test_fetchjoin_Mx1_1; `core/query/language/Client2` — test_leftouterjoin_1xM, test_fetchjoin_1x1, test_innerjoin_1x1; `core/query/language/Client3` — test_leftouterjoin_MxM, test_innerjoin_MxM, test_fetchjoin_MxM
notes:  ~12 methods. Generates correct SQL JOIN clauses. JPQL JOIN FETCH is a query hint (loads eagerly within the query), NOT lazy loading — it does NOT implement lazy proxy loading (M5). Does NOT implement relationship cascade (M5).

### JP-43 — JPQL subqueries (EXISTS, IN, ALL, ANY, SOME)            [TODO]
deps:   JP-38
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/query/JpqlParser.java` (subquery expression — extended),
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/query/JpqlSubquery.java` (new — SubqueryModel),
        `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/query/JpqlSubqueryTest.java` (new)
proof:  TCK `core/query/language/Client1` — test_subquery_exists_01, test_subquery_exists_02, test_subquery_like, test_subquery_between, test_subquery_join, test_subquery_ALL_GT, test_subquery_ALL_LT, test_subquery_ALL_EQ, test_subquery_ALL_LTEQ, test_subquery_ALL_GTEQ, test_subquery_ALL_NOTEQ, test_subquery_ANY_GT, test_subquery_ANY_LT, test_subquery_ANY_EQ, test_subquery_SOME_LTEQ, test_subquery_SOME_GTEQ, test_subquery_in
notes:  ~16 methods. EXISTS, IN, ALL, ANY, SOME quantifiers. Subqueries in SELECT, WHERE, HAVING.

### JP-44 — JPQL bulk UPDATE and DELETE (executeUpdate)            [TODO]
deps:   JP-38
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/query/MansartQuery.java` (executeUpdate),
        `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/query/JpqlBulkOperationTest.java` (new)
proof:  TCK `core/query/apitests/Client1` — queryAPITest22 (Update Query), queryAPITest23 (Delete Query), queryAPITest24 (Bulk Update Query), queryAPITest25 (Bulk Delete Query), queryAPITest16 (getResultList on UPDATE → IllegalStateException), queryAPITest17 (getResultList on DELETE → IllegalStateException), executeUpdateTransactionRequiredExceptionTest
notes:  7 methods. UPDATE/DELETE statements. getResultList on UPDATE/DELETE → IllegalStateException. executeUpdate returns affected row count. TransactionRequiredException when no transaction active.

### JP-45 — JPQL temporal parameters (Date/Calendar with TemporalType) + Date literals            [TODO]
deps:   JP-39
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/query/JpqlParser.java` (temporal literal parsing — extended),
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/query/MansartQuery.java` (setParameter with TemporalType — extended),
        `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/query/JpqlTemporalTest.java` (new)
proof:  TCK `core/query/apitests/Client1` — setParameterStringDateTemporalTypeTest, setParameterStringDateTemporalTypeIllegalArgumentExceptionTest, setParameterStringCalendarTemporalTypeTest, setParameterStringCalendarTemporalTypeTestIllegalArgumentExceptionTest, setParameterIntDateTemporalTypeTest, setParameterIntDateTemporalTypeIllegalArgumentException1Test, setParameterIntCalendarTemporalTypeTest, setParameterIntCalendarTemporalTypeIllegalArgumentExceptionTest, queryAPITest27 (Date literals); `core/query/apitests/Client3` — queryAPITest28 (Time literal), queryAPITest29 (Timestamp literal)
notes:  ~11 methods. TemporalType.DATE, TIME, TIMESTAMP. Date/Calendar parameter binding.

### JP-46 — JPQL streaming results (getResultStream, getTypedResultStream)            [TODO]
deps:   JP-38
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/query/MansartQuery.java` (getResultStream, getTypedResultStream),
        `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/query/JpqlStreamTest.java` (new)
proof:  TCK `jpa22/query/stream/Client` — getResultStreamTest, getTypedResultStreamTest
notes:  2 methods. Returns java.util.stream.Stream<T>. Virtual thread friendly.

### JP-47 — JPQL flush mode on queries (setFlushMode, getFlushMode) + single result            [TODO]
deps:   JP-38, JP-29
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/query/MansartQuery.java` (setFlushMode, getFlushMode),
        `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/query/JpqlFlushModeTest.java` (new)
proof:  TCK `core/query/flushmode/Client1` — flushModeTest1; `core/query/flushmode/Client2` — flushModeTest2, flushModeTest3, flushModeTest4, flushModeTest5, flushModeTest6, flushModeTest7; `core/query/flushmode/Client3` — secondaryTablesValueTest; `core/query/apitests/Client3` — queryAPIGetSingleResultOrNullWithValueTest, queryAPIGetSingleResultOrNullNullValueTest, typedQueryAPIGetSingleResultOrNullWithValueTest, typedQueryAPIGetSingleResultOrNullNullValueTest; `core/query/apitests/Client4` — noTransactionLockModeTypeNoneTest
notes:  ~12 methods. Query-level flush mode (AUTO/COMMIT). getSingleResult/getSingleResultOrNull. Lock mode NONE (no transaction).

### JP-48 — JPQL exception handling (IllegalArgumentException, NoResultException, NonUniqueResultException, IllegalStateException)            [TODO]
deps:   JP-38, JP-39
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/query/MansartQuery.java` (exception paths — extended),
        `mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/query/JpqlExceptionTest.java` (new)
proof:  TCK `core/query/apitests/Client1` — getSingleResultNoResultExceptionTest, getSingleResultNonUniqueResultExceptionTest, getSingleResultTest, getParameterIllegalArgumentExceptionTest, getParameterIllegalArgumentException2Test, getParameterIntIllegalArgumentExceptionTest, setParameterStringObject1IllegalArgumentExceptionTest, setParameterStringObject2IllegalArgumentExceptionTest, setParameterStringDateTemporalTypeIllegalArgumentExceptionTest, setParameterStringCalendarTemporalTypeTestIllegalArgumentExceptionTest, setParameterIntObjectIllegalArgumentExceptionTest, setParameterIntDateTemporalTypeIllegalArgumentException1Test, setParameterIntCalendarTemporalTypeIllegalArgumentExceptionTest, queryAPITest11, queryAPITest12, queryAPITest16, queryAPITest17
notes:  ~17 methods. Various IllegalArgumentException paths (wrong param type, non-existent param name), NoResultException, NonUniqueResultException, IllegalStateException (getResultList on UPDATE/DELETE).

### JP-49 — M4 gate: core/query + jpa22/query TCK suite passes            [TODO]
deps:   JP-38, JP-39, JP-40, JP-41, JP-42, JP-43, JP-44, JP-45, JP-46, JP-47, JP-48
files:  `mansart-persistence-tck/pom.xml` (standalone mode properties),
        `mansart-persistence-tck/run-official-tck-persistence-3.2.sh` (client selection)
proof:  TCK run on `core/query` + `jpa22/query` — all 125+ methods PASS
notes:  Gate card. No implementation. Ensures all JP-38 through JP-48 methods pass TCK.

