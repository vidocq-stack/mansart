# mansart-jakarta-persistence — Task Plan

Jakarta Persistence 3.2 (`jakarta.persistence:jakarta.persistence-api:3.2.0`) implementation with APT-generated entity enhancement, dialect-agnostic SQL generation, and a three-tier runtime (compile-time APT, Maven plugin, Class-File API fallback).

## Target Jakarta Persistence 3.2 Scope

| Spec Concept | Mansart Implementation |
|--------------|------------------------|
| `@Entity` | Detected by APT, generates `_Entity` + `Entity_` static metamodel |
| `@Id`, `@GeneratedValue` | Handled by APT and runtime |
| `@Column`, `@Table` | Handled by APT and runtime |
| `@Version` | Optimistic locking support |
| `@Transient` | Ignored by enhancement |
| `@Enumerated` | Enum storage handled by dialect |
| `@Embedded`, `@Embeddable` | Supported |
| `@ManyToOne`, `@OneToOne`, `@JoinColumn` | Basic relationship support |
| `EntityManagerFactory`, `EntityManager` | Lifecycle and basic operations |
| JPQL | Parser + execution |
| Criteria API | Full support |
| Named/Native Queries | Supported |
| Lifecycle Callbacks | Supported |
| Cascading, Orphan Removal | Supported |
| Fetch Types, Lazy Loading | Supported |
| Persistence Context + Caching | Basic support |
| Transaction Management | Integration with `mansart-transactions` |
| CDI Integration | Vauban BCE extension |

**Out of scope v1**: Collections (`@OneToMany`, `@ManyToMany`), `@MappedSuperclass`, inheritance (`@Inheritance`), `@Convert`/`AttributeConverter`, Stored Procedures, advanced caching.

## Sub-modules

```
mansart-jakarta-persistence/
├── pom.xml                              ← mansart-jakarta-persistence reactor
├── mansart-persistence-spi/             ← SPI: EntityModel, Attribute, Dialect contract
├── mansart-persistence-processor/       ← APT: metamodel (_Entity + Entity_) generation
├── mansart-persistence-core/            ← runtime: PersistenceProvider, EntityManagerFactory, EntityManager
├── mansart-persistence-maven-plugin/    ← Tier 2: compile-time enhancement via Maven plugin
├── mansart-persistence-cdi/             ← CDI integration (Vauban BCE)
├── mansart-persistence-tests/           ← unit + integration tests (H2 in-memory + Testcontainers PG)
├── mansart-persistence-external-lib/    ← external library integration tests
├── mansart-persistence-external-it/     ← external-it: tier-1 ≈ tier-2 proof
└── mansart-persistence-tck/             ← OUT OF reactor — Jakarta Persistence 3.2 TCK runner
```

### Inter-module Dependencies

```
spi ────────── re-exports mansart-data-dialect-spi (H2 + PostgreSQL dialects)
processor ── spi
core ───────── spi, mansart-transactions (ScopedValue-based TM)
maven-plugin ─ processor (scope provided; APT active only on user side)
cdi ────────── core, jakarta.cdi-api 4.1, jakarta.inject-api
tests ──────── core, cdi, processor (test); testcontainers (test, optional)
external-lib ─ core (test); external JPA providers (test)
external-it ── external-lib (test)
tck ────────── core, cdi, processor (out of reactor)
```

## Milestones

| Milestone | Goal | Ends When | Status |
|-----------|------|-----------|--------|
| M0 | Skeleton + Real TCK Harness Baseline | 0/X/1745 baseline with real setup*Data errors | TO_DEFINE |
| M1 | Entity Metamodel + APT Generation | APT generates _Entity + Entity_ correctly | TO_DEFINE |
| M2 | Maven Plugin (Tier 2) + External Library Support | external-it proves tier-1 ≈ tier-2 | TO_DEFINE |
| M3 | Runtime Class-File API (Tier 3) Fallback | escape hatch works, warnings logged | TO_DEFINE |
| M4 | Core Runtime: EntityManagerFactory + EntityManager | basic lifecycle + transaction integration works | TO_DEFINE |
| M5 | Object-Relational Mapping (Basic) | CRUD + basic annotations work | TO_DEFINE |
| M6 | Query Execution (JPQL) | JPQL parser + execution works | TO_DEFINE |
| M7 | Advanced Mapping | relationships, inheritance, enums work | TO_DEFINE |
| M8 | Entity Lifecycle + Callbacks | all lifecycle annotations supported | TO_DEFINE |
| M9 | Cascading + Orphan Removal | cascade types + orphan removal work | TO_DEFINE |
| M10 | Fetch Types + Lazy Loading | lazy loading + fetch types work | TO_DEFINE |
| M11 | Criteria API | full Criteria API supported | TO_DEFINE |
| M12 | Named Queries + Native Queries | named/native queries work | TO_DEFINE |
| M13 | Persistence Context + Caching | context lifecycle + caching work | TO_DEFINE |
| M14 | Transaction Management + Synchronization | full transaction integration | TO_DEFINE |
| M15 | CDI Integration (Vauban) | CDI injection + scoping work | TO_DEFINE |
| M16 | PostgreSQL Dialect Support | PostgreSQL works + TCK passes | TO_DEFINE |
| M17 | Error Handling + Exceptions | all JPA exceptions mapped correctly | TO_DEFINE |
| M18 | Validation + Schema Generation | schema validation + DDL generation work | TO_DEFINE |
| M19 | Performance + Benchmarks | BENCH.md + benchmarks established | TO_DEFINE |
| M20 | Official Jakarta Persistence 3.2 TCK | 1745/1745 PASS on H2 + PostgreSQL | TO_DEFINE |

## Expanded Cards for M0 (current milestone)

### M0-JP-01 — Parent pom.xml with 9 sub-modules
- deps:   —
- files:  mansart-jakarta-persistence/pom.xml, .mvn/
- proof:  mvn clean install -DskipTests
- notes:  Maven Model 4.1.0, compiler 4.0.0-beta-4, Java 25 toolchain

### M0-JP-02 — module-info.java for each module
- deps:   —
- files:  mansart-persistence-{spi,processor,core,maven-plugin,cdi,tests,external-lib,external-it,tck}/module-info.java
- proof:  mvn compile on each module
- notes:  strict Java modules, minimal exports, provides/uses for services

### M0-JP-03 — mansart-persistence-spi module
- deps:   —
- files:  mansart-persistence-spi/pom.xml, module-info.java, package-info.java, EntityModel.java, Attribute.java (hierarchy)
- proof:  mvn install on spi module
- notes:  re-exports mansart-data-dialect-spi types, defines common metadata model

### M0-JP-04 — mansart-persistence-core skeleton
- deps:   M0-JP-03
- files:  mansart-persistence-core/pom.xml, module-info.java, MansartPersistenceProvider.java, MansartEntityManagerFactory.java, MansartEntityManager.java
- proof:  mvn compile on core module
- notes:  all methods throw UnsupportedOperationException("not implemented: <method>"), service provider registered

### M0-JP-05 — TCK infrastructure (out-of-reactor)
- deps:   M0-JP-04
- files:  mansart-persistence-tck/pom.xml (standalone, modelVersion 4.0.0), run-official-tck-persistence-3.2.sh, README.md
- proof:  script runs without harness errors
- notes:  no parent POM, stacks with tck-run, tck-pg, tck-sig profiles

### M0-JP-06 — Harness wiring + provider registration
- deps:   M0-JP-04, M0-JP-05
- files:  mansart-persistence-core/src/main/resources/META-INF/services/jakarta.persistence.spi.PersistenceProvider
- proof:  TCK discovers Mansart provider
- notes:  provider must be reachable, service file format: io.vidocq.mansart.persistence.core.MansartPersistenceProvider

### M0-JP-07 — Baseline TCK run
- deps:   M0-JP-05, M0-JP-06
- files:  (no new files, just execution)
- proof:  ./run-official-tck-persistence-3.2.sh reports 0/X/1745 with all errors in setup*Data or provider wiring
- notes:  document baseline in STATUS.md, X should be mostly setup*Data errors, not harness failures

## Later Milestones (TO_DEFINE)

### M1 — Entity Metamodel + APT Generation
- M1-JP-08: `MansartPersistenceProcessor` (`SourceVersion.RELEASE_25`, `@SupportedAnnotationTypes` for JPA annotations)
- M1-JP-09: Entity scanning — detects `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`, `@Version`, `@ManyToOne`, `@OneToOne`, `@JoinColumn`, `@Enumerated`, `@Embedded`, `@Embeddable`
- M1-JP-10: `_Entity` generation — `EntityModel<T>`, typed `Attribute` subtypes (`IdAttribute`, `TextAttribute`, `NumericAttribute`, `TemporalAttribute`, `ReferenceAttribute`, `EnumAttribute`, `VersionAttribute`)
- M1-JP-11: Standard JPA static metamodel generation — `Entity_` with `SingularAttribute` fields (if `jakarta.persistence-api` on classpath)
- M1-JP-12: MethodHandle resolution — `MethodHandles.privateLookupIn` in `<clinit>`, never `setAccessible`
- M1-JP-13: SQL naming conventions — snake_case column names, plural snake_case table names, FK column naming

### M2 — Maven Plugin (Tier 2) + External Library Support
- M2-JP-14: `MansartPersistenceMojo` bound to `process-classes`, scans dependency classpath for `@Entity` classes
- M2-JP-15: Class-File API parsing — reads class file bytes, extracts annotations, generates same `_Entity` + `Entity_` as APT
- M2-JP-16: Lazy association proxies for external entities — real subclasses generated, never `java.lang.reflect.Proxy`
- M2-JP-17: `external-lib` — JAR with test entities (no Mansart deps)
- M2-JP-18: `external-it` — integration tests asserting tier-1 ≈ tier-2 behavior

### M3 — Runtime Class-File API (Tier 3) Fallback
- M3-JP-19: Runtime `EntityModelBuilder` — builds `EntityModel` from class file bytes at bootstrap
- M3-JP-20: `RuntimeRepositoryClassGenerator` — generates hidden implementation classes using Class-File API
- M3-JP-21: `MansartCallback` dispatch mechanism for hidden classes
- M3-JP-22: Warning mechanism pointing to Maven plugin for tier-3 entities

### M4 — Core Runtime: EntityManagerFactory + EntityManager
- M4-JP-23: `MansartPersistenceProvider` — implements `PersistenceProvider`, parses `persistence.xml`, creates `EntityManagerFactory`
- M4-JP-24: `MansartEntityManagerFactory` — manages `EntityManager` instances, bootstrap, metadata
- M4-JP-25: `MansartEntityManager` — implements `EntityManager` interface, delegates to persistence context
- M4-JP-26: Persistence context — tracks entity states (NEW, MANAGED, DETACHED, REMOVED)
- M4-JP-27: Transaction integration — binds to `mansart-transactions` via `ScopedValue<Transaction>`
- M4-JP-28: Connection management — uses `ScopedValue<Connection>` (no `ThreadLocal`)

### M5 — Object-Relational Mapping (Basic)
- M5-JP-29: `EntityMapper` — maps entity instances to SQL INSERT/UPDATE/DELETE via dialect SPI
- M5-JP-30: ID generation — `AUTO`, `IDENTITY`, `SEQUENCE`, `TABLE` strategies
- M5-JP-31: Column mapping — name, nullable, unique, length, precision, scale
- M5-JP-32: Table mapping — name, schema, catalog
- M5-JP-33: Integration tests with H2 — basic CRUD, ID generation, column constraints

### M6 — Query Execution (JPQL)
- M6-JP-34: JPQL parser — recursive descent, hand-written (no ANTLR)
- M6-JP-35: SELECT queries — projection, DISTINCT, WHERE, GROUP BY, HAVING, ORDER BY
- M6-JP-36: FROM clause — entity names, joins (INNER, LEFT, RIGHT, CROSS), fetch joins
- M6-JP-37: WHERE expressions — comparisons, logical operators, IN, BETWEEN, LIKE, IS NULL, IS EMPTY, functions
- M6-JP-38: UPDATE queries — SET clause, WHERE
- M6-JP-39: DELETE queries — WHERE
- M6-JP-40: Named parameters (`:param`) and positional parameters (`?1`)
- M6-JP-41: Aggregate functions — COUNT, SUM, AVG, MIN, MAX
- M6-JP-42: String functions — CONCAT, SUBSTRING, TRIM, LOWER, UPPER, LENGTH, LOCATE, INDEX
- M6-JP-43: Date/time functions — CURRENT_DATE, CURRENT_TIME, CURRENT_TIMESTAMP, EXTRACT
- M6-JP-44: CASE expressions
- M6-JP-45: Subqueries — scalar, IN, EXISTS, ALL, ANY

### M7 — Advanced Mapping
- M7-JP-46: `@ManyToOne`, `@OneToOne` — owning side, foreign key mapping
- M7-JP-47: `@OneToMany`, `@ManyToMany` — collections, join tables
- M7-JP-48: `@JoinColumn`, `@JoinTable` — custom join column/table names
- M7-JP-49: `@Inheritance` — SINGLE_TABLE, JOINED, TABLE_PER_CLASS strategies
- M7-JP-50: `@DiscriminatorColumn`, `@DiscriminatorValue`
- M7-JP-51: `@Embedded`, `@Embeddable` — composite value types
- M7-JP-52: `@Enumerated` — STRING, ORDINAL
- M7-JP-53: `@Temporal` — DATE, TIME, TIMESTAMP
- M7-JP-54: `@Lob` — BLOB, CLOB
- M7-JP-55: `@Version` — optimistic locking
- M7-JP-56: `@Transient` — non-persistent fields
- M7-JP-57: `@Convert` — attribute converters

### M8 — Entity Lifecycle + Callbacks
- M8-JP-58: `@PrePersist`, `@PostPersist`, `@PreRemove`, `@PostRemove`, `@PreUpdate`, `@PostUpdate`, `@PostLoad`
- M8-JP-59: `@EntityListeners` — class-level and method-level listeners
- M8-JP-60: Callback ordering — entity listeners before entity callbacks
- M8-JP-61: Inherited callbacks

### M9 — Cascading + Orphan Removal
- M9-JP-62: `@CascadeType` — all cascade types
- M9-JP-63: `orphanRemoval = true`
- M9-JP-64: Cascade ordering and edge cases

### M10 — Fetch Types + Lazy Loading
- M10-JP-65: `@FetchType(LAZY)` and `@FetchType(EAGER)`
- M10-JP-66: Lazy loading proxies — generated subclasses, not `java.lang.reflect.Proxy`
- M10-JP-67: Fetch join hints

### M11 — Criteria API
- M11-JP-68: `CriteriaBuilder` — root query construction
- M11-JP-69: `CriteriaQuery` — SELECT, UPDATE, DELETE
- M11-JP-70: `Root`, `Join`, `Fetch`, `Path` — navigation
- M11-JP-71: `Predicate` — WHERE conditions
- M11-JP-72: `Order` — ORDER BY
- M11-JP-73: `GroupBy`, `Having` — aggregation
- M11-JP-74: `Subquery` — nested queries
- M11-JP-75: Metamodel-based queries using `Entity_` static metamodel

### M12 — Named Queries + Native Queries
- M12-JP-76: `@NamedQuery`, `@NamedQueries` — JPQL named queries
- M12-JP-77: `@NamedNativeQuery`, `@NamedNativeQueries` — native SQL named queries
- M12-JP-78: `@SqlResultSetMapping`, `@EntityResult`, `@ColumnResult`, `@FieldResult`
- M12-JP-79: Query hints — `Query.setHint`
- M12-JP-80: Native query execution with result mapping

### M13 — Persistence Context + Caching
- M13-JP-81: Persistence context propagation — per transaction, extended
- M13-JP-82: First-level cache — entity identity map within persistence context
- M13-JP-83: `Cache` interface and `CacheRetrieveMode`, `CacheStoreMode`
- M13-JP-84: `SharedCacheMode` — NONE, ALL, ENABLE_SELECTIVE, DISABLE_SELECTIVE, UNSPECIFIED
- M13-JP-85: Cache invalidation and eviction

### M14 — Transaction Management + Synchronization
- M14-JP-86: Local transaction support (RESOURCE_LOCAL)
- M14-JP-87: JTA transaction support (JTA)
- M14-JP-88: `TransactionSynchronizationRegistry` integration
- M14-JP-89: Synchronization callbacks for JPA events
- M14-JP-90: Transaction timeout and isolation level

### M15 — CDI Integration (Vauban)
- M15-JP-91: `MansartPersistenceExtension` — BCE for CDI integration
- M15-JP-92: Producer for `EntityManager`, `EntityManagerFactory`, `PersistenceProvider`
- M15-JP-93: `@PersistenceContext` injection support
- M15-JP-94: `@PersistenceUnit` injection support
- M15-JP-95: CDI scoping — `@ApplicationScoped`, `@RequestScoped`, `@TransactionScoped`
- M15-JP-96: Integration tests with Vauban

### M16 — PostgreSQL Dialect Support
- M16-JP-97: PostgreSQL dialect implementation (reusing `mansart-data-dialect-spi`)
- M16-JP-98: PostgreSQL-specific features (RETURNING, ON CONFLICT, etc.)
- M16-JP-99: Integration tests with Testcontainers PostgreSQL

### M17 — Error Handling + Exceptions
- M17-JP-100: SQLState mapping to JPA exceptions (`EntityExistsException`, `OptimisticLockException`, etc.)
- M17-JP-101: `PersistenceException` hierarchy
- M17-JP-102: Constraint violation handling
- M17-JP-103: Transaction rollback exception handling
- M17-JP-104: Entity not found, illegal state, illegal argument exceptions

### M18 — Validation + Schema Generation
- M18-JP-105: Schema validation — validates entity mappings against database schema
- M18-JP-106: Schema generation — generates DDL from entity mappings
- M18-JP-107: `persistence.xml` schema generation properties
- M18-JP-108: Integration tests with schema generation

### M19 — Performance + Benchmarks
- M19-JP-109: BENCH.md with baseline benchmarks
- M19-JP-110: JMH benchmarks for common operations
- M19-JP-111: Virtual thread pinning verification
- M19-JP-112: Performance tuning and optimization

### M20 — Official Jakarta Persistence 3.2 TCK
- M20-JP-113: TCK infrastructure — standalone POM, runner scripts, profiles
- M20-JP-114: TCK EntityTests — all entity-related tests pass
- M20-JP-115: TCK PersistenceTests — all persistence-related tests pass
- M20-JP-116: TCK QueryTests — all query-related tests pass
- M20-JP-117: TCK SignatureTests — all signature tests pass
- M20-JP-118: Full TCK run — 1745/1745 PASS on H2 and PostgreSQL
