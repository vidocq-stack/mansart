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

### JP-01 — Parent pom.xml with 9 sub-modules            [TODO]
- deps:   —
- files:  mansart-jakarta-persistence/pom.xml, .mvn/
- proof:  mvn clean install -DskipTests
- notes:  Maven Model 4.1.0, compiler 4.0.0-beta-4, Java 25 toolchain

### JP-02 — module-info.java for each module            [TODO]
- deps:   —
- files:  mansart-persistence-{spi,processor,core,maven-plugin,cdi,tests,external-lib,external-it,tck}/module-info.java
- proof:  mvn compile on each module
- notes:  strict Java modules, minimal exports, provides/uses for services

### JP-03 — mansart-persistence-spi module                [TODO]
- deps:   —
- files:  mansart-persistence-spi/pom.xml, module-info.java, package-info.java, EntityModel.java, Attribute.java (hierarchy)
- proof:  mvn install on spi module
- notes:  re-exports mansart-data-dialect-spi types, defines common metadata model

### JP-04 — mansart-persistence-core skeleton              [TODO]
- deps:   JP-03
- files:  mansart-persistence-core/pom.xml, module-info.java, MansartPersistenceProvider.java, MansartEntityManagerFactory.java, MansartEntityManager.java
- proof:  mvn compile on core module
- notes:  all methods throw UnsupportedOperationException("not implemented: <method>"), service provider registered

### JP-05 — TCK infrastructure (out-of-reactor)         [TODO]
- deps:   JP-04
- files:  mansart-persistence-tck/pom.xml (standalone, modelVersion 4.0.0), run-official-tck-persistence-3.2.sh, README.md
- proof:  script runs without harness errors
- notes:  no parent POM, stacks with tck-run, tck-pg, tck-sig profiles

### JP-06 — Harness wiring + provider registration        [TODO]
- deps:   JP-04, JP-05
- files:  mansart-persistence-core/src/main/resources/META-INF/services/jakarta.persistence.spi.PersistenceProvider
- proof:  TCK discovers Mansart provider
- notes:  provider must be reachable, service file format: io.vidocq.mansart.persistence.core.MansartPersistenceProvider

### JP-07 — Baseline TCK run                          [TODO]
- deps:   JP-05, JP-06
- files:  (no new files, just execution)
- proof:  ./run-official-tck-persistence-3.2.sh reports 0/X/1745 with all errors in setup*Data or provider wiring
- notes:  document baseline in STATUS.md, X should be mostly setup*Data errors, not harness failures

## Later Milestones (TO_DEFINE)

### M1 — Entity Metamodel + APT Generation
- APT generates `_Entity` and `Entity_` static metamodel from `@Entity`, `@Id`, `@Column`, `@Table`, `@Version`, `@Enumerated`, `@Embedded`, `@Embeddable`, `@ManyToOne`, `@OneToOne`, `@JoinColumn`
- MethodHandles-based attribute accessors (no runtime reflection)
- Validation: compile-time errors for invalid mappings

### M2 — Maven Plugin (Tier 2) + External Library Support
- Maven plugin (`mansart-persistence-maven-plugin`) performs compile-time enhancement
- external-it: proves tier-1 (APT) ≈ tier-2 (Maven plugin) behavior
- Integration with `mansart-transactions` for transaction management

### M3 — Runtime Class-File API (Tier 3) Fallback
- Class-File API fallback for environments without annotation processing
- Warnings logged when tier-3 is used
- No reflection on user classes at runtime

### M4 — Core Runtime: EntityManagerFactory + EntityManager
- `MansartPersistenceProvider`, `MansartEntityManagerFactory`, `MansartEntityManager`
- Basic lifecycle: create, close, transaction integration with `mansart-transactions`
- All methods throw `UnsupportedOperationException("not implemented: <method>")` initially

### M5 — Object-Relational Mapping (Basic)
- CRUD operations: persist, merge, remove, find, refresh
- Basic annotations: `@Entity`, `@Id`, `@GeneratedValue`, `@Column`, `@Table`, `@Version`
- Basic JPQL parsing and execution

### M6 — Query Execution (JPQL)
- JPQL parser (subset of JPQL 3.2)
- Execution engine: SELECT, UPDATE, DELETE
- Parameter binding and result mapping

### M7 — Advanced Mapping
- Relationships: `@OneToOne`, `@ManyToOne`, `@OneToMany`, `@ManyToMany` (basic)
- Inheritance: `@Inheritance` (single table, joined, table per class)
- Enums: `@Enumerated(STRING/ORDINAL)`
- Embedded objects: `@Embedded`, `@Embeddable`

### M8 — Entity Lifecycle + Callbacks
- Lifecycle annotations: `@PrePersist`, `@PostPersist`, `@PreUpdate`, `@PostUpdate`, `@PreRemove`, `@PostRemove`, `@PostLoad`
- Entity listener classes
- Callback methods on entity classes

### M9 — Cascading + Orphan Removal
- Cascade types: `PERSIST`, `MERGE`, `REMOVE`, `REFRESH`, `DETACH`
- Orphan removal: `orphanRemoval = true`

### M10 — Fetch Types + Lazy Loading
- Fetch types: `FetchType.LAZY`, `FetchType.EAGER`
- Proxy-based lazy loading (no runtime reflection)
- Batch fetching

### M11 — Criteria API
- Full Criteria API support
- Type-safe query construction
- Metamodel-based queries

### M12 — Named Queries + Native Queries
- Named JPQL queries: `@NamedQuery`
- Named native queries: `@NamedNativeQuery`
- Dynamic query execution

### M13 — Persistence Context + Caching
- Persistence context lifecycle
- First-level cache
- Basic second-level cache (optional, pluggable)

### M14 — Transaction Management + Synchronization
- Integration with `mansart-transactions`
- Transaction synchronization callbacks
- `Synchronization` interface support

### M15 — CDI Integration (Vauban)
- CDI producer for `EntityManagerFactory` and `EntityManager`
- Vauban BCE extension for automatic discovery
- Injection support: `@PersistenceContext`, `@PersistenceUnit`

### M16 — PostgreSQL Dialect Support
- PostgreSQL dialect implementation
- Testcontainers-based integration tests
- TCK passes on PostgreSQL

### M17 — Error Handling + Exceptions
- Mapping SQLState to JPA exceptions
- `PersistenceException`, `EntityNotFoundException`, `NonUniqueResultException`, `OptimisticLockingFailureException`, etc.
- Exception translation

### M18 — Validation + Schema Generation
- Bean Validation integration (optional)
- Schema generation: DDL creation and validation
- `jakarta.persistence.schema-generation.*` properties

### M19 — Performance + Benchmarks
- JMH benchmarks for common operations
- Performance regression tests
- BENCH.md documenting results

### M20 — Official Jakarta Persistence 3.2 TCK
- 1745/1745 PASS on H2 + PostgreSQL
- Signature tests
- Full TCK suite execution
