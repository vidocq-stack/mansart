# TASKS — mansart-jakarta-persistence

The backlog. **One card per session.** A card names a behaviour, at most 4 files,
and the single test that proves it. Status is `TODO` / `WIP` / `DONE` / `BLOCKED`.

Only the current milestone is expanded. `@tracker` expands the next one when this
one closes. See `PLAN.md` for the milestone map.

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

### JP-14 — `MapAttribute`, `ListAttribute`, `SetAttribute` impls  [TODO]
deps:   JP-13
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/MapAttributeImpl.java`,
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/ListAttributeImpl.java`,
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/SetAttributeImpl.java`
proof:  TCK `core/metamodelapi/mapattribute/Client`, `listattribute/Client`, `setattribute/Client`
notes:  11 test methods.

### JP-15 — `EmbeddableType`, `MappedSuperclassType`, `pluralAttribute` impls  [TODO]
deps:   JP-14
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/EmbeddableTypeImpl.java`,
        `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/MappedSuperclassTypeImpl.java`
proof:  TCK `core/metamodelapi/embeddabletype/Client`, `mappedsuperclasstype/Client`, `pluralattribute/Client`
notes:  67 test methods across 3 clients. Largest single card in M1.

### JP-16 — `ManagedType` concrete impl (getDeclaredSingular/Plural/Collection attributes)  [TODO]
deps:   JP-15
files:  `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/ManagedTypeImpl.java`
proof:  TCK `core/metamodelapi/managedtype/Client`
notes:  52 test methods. The most complex runtime type — delegates to SingularAttributeImpl, PluralAttributeImpl.

### JP-17 — APT: `@Embeddable`, `@MappedSuperclass`, `@OneToMany`, `@ManyToMany`  [TODO]
deps:   JP-09
files:  `mansart-persistence-processor/src/main/java/io/vidocq/mansart/persistence/processor/EntityScanner.java` (extended),
        `mansart-persistence-processor/src/main/java/io/vidocq/mansart/persistence/processor/MansartPersistenceMetamodelWriter.java` (extended)
proof:  TCK `core/metamodelapi/embeddabletype/Client` (51 tests), `identitytype/Client` (64 tests)
notes:  Extends JP-09 scanner + writer. EmbeddableType, MappedSuperclassType, MapAttribute, SetAttribute.

### JP-18 — APT: `@OneToMany`/`@ManyToMany` → `PluralAttribute` generation  [TODO]
deps:   JP-17
files:  `mansart-persistence-processor/src/main/java/io/vidocq/mansart/persistence/processor/EntityScanner.java` (extended)
proof:  TCK `core/metamodelapi/collectionattribute/Client`, `listattribute/Client`, `setattribute/Client`
notes:  PluralAttribute generation in static metamodel (collectionType, elementType).

### JP-19 — APT: `@IdClass` composite key support  [TODO]
deps:   JP-17
files:  `mansart-persistence-processor/src/main/java/io/vidocq/mansart/persistence/processor/EntityScanner.java` (extended)
proof:  TCK `core/metamodelapi/entitytype/Client.getIdClassAttributes()`
notes:  DID2Employee entity with composite key (firstName + lastName).

### JP-20 — M1 gate: full `core/metamodelapi` TCK suite passes  [TODO]
deps:   JP-10, JP-11, JP-12, JP-13, JP-14, JP-15, JP-16, JP-17, JP-18, JP-19
files:  STATUS.md (update TCK numbers)
proof:  TCK `--sig` or entity-only run: `core/metamodelapi` — 257 methods PASS
notes:  Full-suite run. 16 Client classes, 257 test methods. Gate for M1.

## M2 … M9

See `PLAN.md`. Not expanded.

---

## Discovered work

Cards that came out of a session but did not belong to its card. Appended by
`@tracker`, never merged into an existing card.

_(empty)_
