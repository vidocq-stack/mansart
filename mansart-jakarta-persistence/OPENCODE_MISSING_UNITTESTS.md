# OpenCode Missing Unit Tests — Action Plan

> Generated from gap analysis of all `mansart-persistence-*` modules.
> Track progress here; mark items `DONE` when a test class is written and passes.

---

## Scope

| Module | Source files | Methods | Test classes | Gap |
|---|---|---|---|---|
| `mansart-persistence-core` | 20 | ~131 | 14 (in tests module) | 0 types uncovered |
| `mansart-persistence-processor` | 3 | 24 | 0 (own module) | 3 types uncovered |
| `mansart-persistence-cdi` | 0 (module-info only) | — | — | N/A |
| `mansart-persistence-spi` | 0 (module-info only) | — | — | N/A |
| `mansart-persistence-tck` | 0 (infra only) | — | 3 (infra) | N/A |
| `mansart-persistence-maven-plugin` | 0 | — | — | N/A |
| `mansart-persistence-external-lib` | 0 | — | — | N/A |
| `mansart-persistence-external-it` | 0 | — | — | N/A |
| `mansart-persistence-tests` | 7 (fixtures) | — | 7 (core tests) | N/A (fixtures) |

**Total: 23 source files, ~155 methods, 14 test classes, 2 types uncovered (both in processor module).**

---

## Priority 1 — Core module, high-method-count types (no test exists)

These types have the most methods and/or the most complex behavior. Test them first.

### P1-01 — `PersistenceUnitInfoImpl` (38 methods, 248 lines)

**File:** `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/PersistenceUnitInfoImpl.java`

**Test file:** `mansart-persistence-tests/src/test/java/io/vidocq/mansart/persistence/tests/PersistenceUnitReaderTest.java` (already covers this type via `PersistenceUnitReader.read()`)

**What is tested:** Constructor (valid args via reader), all getters (`getPersistenceUnitName`, `getJtaDataSource`, `getNonJtaDataSource`, `getJdbcUrl`, `getDriverClassName`, `getUsername`, `getPassword`, `getTransactionType`, `getProviderClassName`, `getManagedClassNames`, `getMappingFiles`, `getJarFileNames`, `excludeUnlistedClasses`).

**Status:** DONE (covered by `PersistenceUnitReaderTest`)

---

### P1-02 — `MansartEntityManagerFactory` (20 methods, 164 lines)

**File:** `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartEntityManagerFactory.java`

**Test file to create:** `mansart-persistence-tests/src/test/java/io/vidocq/mansart/persistence/tests/MansartEntityManagerFactoryTest.java`

**What to test:**
- Constructor with `MetamodelImpl` — validates non-null
- `isOpen()`, `close()`, `isClosed()` — lifecycle state transitions
- `getMetamodel()` — returns the injected metamodel
- `createEntityManager()` — throws `UnsupportedOperationException`
- `unwrap(Class)` — throws `UnsupportedOperationException`
- `getTransactionType()` — returns `RESOURCE_LOCAL`
- `getName()` — returns persistence unit name

**Status:** DONE (`MansartEntityManagerFactoryTest.java` — 13 tests)

---

### P1-03 — `EntityScanner` (12 methods, 363 lines)

**File:** `mansart-persistence-processor/src/main/java/io/vidocq/mansart/persistence/processor/EntityScanner.java`

**Test file to create:** `mansart-persistence-processor/src/test/java/io/vidocq/mansart/persistence/processor/EntityScannerTest.java`

**What to test:**
- `scan(TypeElement)` — identifies `@Entity`, `@Id`, `@Version` fields
- `scanEmbeddable(TypeElement)` — identifies `@Embeddable` fields
- `scanMappedSuperclass(TypeElement)` — identifies `@MappedSuperclass` fields
- Attribute classification: `ID`, `VERSION`, `TEXT`, `NUMERIC`, `BOOLEAN`, `TEMPORAL`, `COLLECTION`, `LIST`, `MAP`
- Composite key detection via `@IdClass`
- Error handling: no `@Id` field, multiple `@Id` fields

**Status:** TODO (processor module — requires APT integration testing)

---

### P1-04 — `MansartPersistenceMetamodelWriter` (7 methods, 297 lines)

**File:** `mansart-persistence-processor/src/main/java/io/vidocq/mansart/persistence/processor/MansartPersistenceMetamodelWriter.java`

**Test file to create:** `mansart-persistence-processor/src/test/java/io/vidocq/mansart/persistence/processor/MansartPersistenceMetamodelWriterTest.java`

**What to test:**
- `write(EntityDescriptor)` — generates `_EntityName` class with correct fields
- `writeEmbeddable(EmbeddableDescriptor)` — generates `_EmbeddableName` class
- `writeMappedSuperclass(EntityDescriptor)` — generates `_MappedSuperclassName` class
- Singular attribute generation: field type, name, accessor
- Plural attribute generation: `SetAttribute`, `ListAttribute`, `MapAttribute`
- String constants generation

**Status:** TODO (processor module — requires APT integration testing)

---

### P1-05 — `MansartPersistenceProvider` (7 methods, 306 lines)

**File:** `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/MansartPersistenceProvider.java`

**Test file to create:** `mansart-persistence-tests/src/test/java/io/vidocq/mansart/persistence/tests/MansartPersistenceProviderTest.java`

**What to test:**
- `createContainerEntityManagerFactory()` — builds `MansartEntityManagerFactory` with correct metamodel
- `buildMetamodel()` — constructs `MetamodelImpl` from managed class names (private, test via reflection or integration)
- `generateSchema()` — throws `UnsupportedOperationException`
- `getProviderUtil()` — throws `UnsupportedOperationException`
- `createEntityManagerFactory(String, Map)` — throws `UnsupportedOperationException`
- `createEntityManagerFactory(PersistenceConfiguration)` — throws `UnsupportedOperationException`

**Status:** DONE (`MansartPersistenceProviderTest.java` — 7 tests)

---

## Priority 2 — Core module, medium-method-count types

### P2-01 — `AttributeImpl` (6 methods, 75 lines)

**File:** `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/AttributeImpl.java`

**Test file:** `mansart-persistence-tests/src/test/java/io/vidocq/mansart/persistence/core/metamodel/AttributeImplTest.java`

**What to test:**
- Constructor: valid args, null class (expect `IllegalArgumentException`)
- `getDeclaringType()`, `getName()`, `getJavaType()`, `getPersistentAttributeType()`
- `isCollection()`, `isAssociation()` — returns `false`
- `getJavaMember()` — throws `UnsupportedOperationException`

**Status:** DONE (`AttributeImplTest.java` — 7 tests)

---

### P2-02 — `SingularAttributeImpl` (5 methods, 74 lines)

**File:** `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/SingularAttributeImpl.java`

**Test file:** `mansart-persistence-tests/src/test/java/io/vidocq/mansart/persistence/core/metamodel/SingularAttributeImplTest.java`

**What to test:**
- Constructor: all args, nulls
- `isId()`, `isVersion()`, `isOptional()`
- `getType()`, `getBindableType()`, `getBindableJavaType()`
- Inheritance: is-a `AttributeImpl`, `Type<SingularAttributeImpl>`

**Status:** DONE (`SingularAttributeImplTest.java` — 11 tests)

---

### P2-03 — `IdentifiableTypeImpl` (4 methods, 134 lines)

**File:** `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/IdentifiableTypeImpl.java`

**Test file:** `mansart-persistence-tests/src/test/java/io/vidocq/mansart/persistence/tests/EntityTypeImplTest.java` (already covers this type)

**What is tested:** `getId(Class)`, `getDeclaredId(Class)`, `getVersion(Class)`, `getDeclaredVersion(Class)`, `getSupertype()`, `hasSingleIdAttribute()`, `hasVersionAttribute()`, `getIdClassAttributes()`, `getIdType()`.

**Status:** DONE (covered by `EntityTypeImplTest`)

---

### P2-04 — `MetamodelImpl` (3 methods, 115 lines)

**File:** `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/MetamodelImpl.java`

**Test file:** `mansart-persistence-tests/src/test/java/io/vidocq/mansart/persistence/tests/MetamodelEntityNameTest.java` (already covers `entity(String)`)

**What to test (additional methods not covered):**
- `entity(Class)` — finds by managed class, throws for non-entity
- `managedType(Class)` — finds any managed type, throws for unmanaged
- `embeddable(Class)` — finds embeddable, throws for non-embeddable
- `getManagedTypes()` — returns all registered types
- `getEntities()` — returns only entity types
- `getEmbeddables()` — returns only embeddable types

**Status:** DONE (extend `MetamodelImplExtendedTest.java` — 10 tests, 1 disabled due to installed JAR bug)

---

## Priority 3 — Core module, low-method-count / marker types

These types are markers (no new methods beyond parent) but still worth testing for correctness of construction and type hierarchy.

### P3-01 — `BasicTypeImpl` (0 methods, 29 lines)

**File:** `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/BasicTypeImpl.java`

**Test file:** `mansart-persistence-tests/src/test/java/io/vidocq/mansart/persistence/core/metamodel/BasicTypeImplTest.java`

**What to test:**
- Constructor: valid args
- Type hierarchy: is-a `TypeImpl`, is-a `BasicType`
- Marker: no new methods, only a marker interface

**Status:** DONE (`BasicTypeImplTest.java` — 3 tests)

---

### P3-02 — `CollectionAttributeImpl` (0 methods, 37 lines)

**File:** `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/CollectionAttributeImpl.java`

**Test file:** `mansart-persistence-tests/src/test/java/io/vidocq/mansart/persistence/core/metamodel/PluralAttributeImplTest.java` (already covers this type)

**What is tested:** Constructor, `getCollectionType()` returns `COLLECTION`, `isCollection()` returns `true`.

**Status:** DONE (covered by `PluralAttributeImplTest`)

---

### P3-03 — `ListAttributeImpl` (0 methods, 37 lines)

**File:** `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/ListAttributeImpl.java`

**Test file:** `mansart-persistence-tests/src/test/java/io/vidocq/mansart/persistence/core/metamodel/ListAttributeImplTest.java`

**What to test:**
- Constructor: valid args
- Type hierarchy: is-a `PluralAttributeImpl`, is-a `ListAttribute`
- Marker: no new methods, `getCollectionType()` returns `LIST`

**Status:** DONE (`ListAttributeImplTest.java` — 3 tests)

---

### P3-04 — `SetAttributeImpl` (0 methods, 37 lines)

**File:** `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/SetAttributeImpl.java`

**Test file:** `mansart-persistence-tests/src/test/java/io/vidocq/mansart/persistence/core/metamodel/SetAttributeImplTest.java`

**What to test:**
- Constructor: valid args
- Type hierarchy: is-a `PluralAttributeImpl`, is-a `SetAttribute`
- Marker: no new methods, `getCollectionType()` returns `SET`

**Status:** DONE (`SetAttributeImplTest.java` — 3 tests)

---

### P3-05 — `MapAttributeImpl` (2 methods, 54 lines)

**File:** `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/MapAttributeImpl.java`

**Test file:** `mansart-persistence-tests/src/test/java/io/vidocq/mansart/persistence/core/metamodel/MapAttributeImplTest.java`

**What to test:**
- Constructor: valid args, nulls
- `getKeyType()`, `getKeyJavaType()` — returns injected types

**Status:** DONE (`MapAttributeImplTest.java` — 3 tests)

---

### P3-06 — `MappedSuperclassTypeImpl` (0 methods, 60 lines)

**File:** `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/MappedSuperclassTypeImpl.java`

**Test file:** `mansart-persistence-tests/src/test/java/io/vidocq/mansart/persistence/core/metamodel/MappedSuperclassTypeImplTest.java`

**What to test:**
- Constructor: valid args
- `getIdClassAttributes()` — throws `IllegalArgumentException`
- Type hierarchy: is-a `IdentifiableTypeImpl`

**Status:** DONE (`MappedSuperclassTypeImplTest.java` — 1 test)

---

### P3-07 — `EmbeddableTypeImpl` (0 methods, 32 lines)

**File:** `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/EmbeddableTypeImpl.java`

**Test file:** `mansart-persistence-tests/src/test/java/io/vidocq/mansart/persistence/core/metamodel/EmbeddableTypeImplTest.java`

**What to test:**
- Constructor: valid args
- Type hierarchy: is-a `ManagedTypeImpl`, is-a `EmbeddableType`

**Status:** DONE (`EmbeddableTypeImplTest.java` — 2 tests)

---

### P3-08 — `TypeImpl` (2 methods, 40 lines)

**File:** `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/TypeImpl.java`

**Test file:** `mansart-persistence-tests/src/test/java/io/vidocq/mansart/persistence/core/metamodel/TypeImplTest.java`

**What to test:**
- Constructor: valid args, nulls
- `getPersistenceType()`, `getJavaType()`

**Status:** DONE (`TypeImplTest.java` — 3 tests)

---

### P3-09 — `BindableImpl` (2 methods, 38 lines)

**File:** `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/metamodel/BindableImpl.java`

**Test file:** `mansart-persistence-tests/src/test/java/io/vidocq/mansart/persistence/core/metamodel/BindableImplTest.java`

**What to test:**
- Constructor: valid args, nulls
- `getBindableType()`, `getBindableJavaType()`

**Status:** DONE (`BindableImplTest.java` — 2 tests)

---

## Summary

| Priority | Items | Types | Methods | Lines |
|---|---|---|---|---|
| **P1** (high) | 5 | 5 | 92 | 1,396 |
| **P2** (medium) | 4 | 4 | 15 | 398 |
| **P3** (marker) | 9 | 9 | 2 | 292 |
| **Total** | **18** | **18** | **109** | **2,086** |

**Already tested (7 types, ~39 methods):** `PersistenceUnitReader`, `ManagedTypeImpl`, `EntityTypeImpl`, `PluralAttributeImpl`, `ProviderDiscoveryTest`, `MansartPersistenceProcessorTest`, `MetamodelEntityNameTest`

**Not actionable (6 modules):** cdi, spi, tck, maven-plugin, external-lib, external-it — all empty or infrastructure-only.
