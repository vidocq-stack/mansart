# mansart-jakarta-persistence — Detailed Plan

Jakarta Persistence 3.2 (classic JPA) implementation with zero external runtime dependencies beyond the Jakarta APIs, virtual-thread-native, static code generation instead of reflection, and strict Java modules.

## Mission

Deliver a **complete Jakarta Persistence 3.2** provider that:
- Passes the **official TCK** (269 client classes, ~1,745 test methods) with 100% reliability
- **Never uses runtime reflection** on user entity classes — all access is through code generated at compile time (APT), build time (Maven plugin), or bootstrap time (Class-File API fallback)
- **Binds to `mansart-transactions`** for transaction management (`ScopedValue`-based, virtual-thread-native)
- **Shares `mansart-data-dialect-spi`** with `mansart-jakarta-data` for SQL generation (H2 + PostgreSQL dialects, neutral AST)
- **Accepts a standard `DataSource`** from the application — `mansart-pool` is an optional convenience, never a dependency
- **Stays independent of `mansart-jakarta-data`** at runtime — both can coexist in the same application

**Locked design decisions** (from root `AGENTS.md` + `mansart-jpa` skill):
- Entity enhancement (lazy loading, dirty tracking, accessors) is **APT-generated at compile time** — no runtime bytecode loading, no Java agent
- **Three-tier code generation doctrine**: APT for compile-time sources, Maven plugin (Class-File API) for external JARs, runtime Class-File API as escape hatch
- **Strict Java modules** with minimal exports, `ScopedValue` instead of `ThreadLocal`
- **TDD discipline**: failing test first, smallest real implementation, no fake stubs

## Reactor Layout

```
mansart-jakarta-persistence/
├── pom.xml                              ← mansart-jakarta-persistence reactor
├── mansart-persistence-spi/            ← Metadata model + reuse of mansart-data-dialect-spi
├── mansart-persistence-processor/      ← APT (RELEASE_25): _Entity metamodel, descriptors, accessors
├── mansart-persistence-core/           ← EMF, EntityManager, persistence context, JPQL, Criteria, flush
├── mansart-persistence-maven-plugin/   ← Class-File API generation for entities in external jars
├── mansart-persistence-cdi/            ← CDI 4.1 Lite integration through Vauban
├── mansart-persistence-tests/          ← unit tests + integration tests
├── mansart-persistence-external-lib/   ← test fixture: a jar of entities APT never sees
├── mansart-persistence-external-it/    ← integration test proving tier-1 and tier-2 behave identically
└── mansart-persistence-tck/            ← OUT OF REACTOR — standalone POM 4.0.0, official TCK runner
```

### Module Data Flow

```
User Entity (annotated)
   │
   ├── APT (mansart-persistence-processor) → _Entity metamodel + accessors
   │       (if jakarta.persistence-api on classpath, also generates standard Entity_ static metamodel)
   │
   ├── Maven Plugin (mansart-persistence-maven-plugin) → Class-File API generation
   │       (for entities in dependency JARs — tier 2)
   │
   └── Runtime (Class-File API fallback) → hidden class generation
       (escape hatch for entities neither tier reached — tier 3)
   │
   ▼
mansart-persistence-spi (EntityModel, Attribute AST, Where, OrderBy, Pagination)
   │  ↑  reuses mansart-data-dialect-spi (Dialect, DialectFactory, SqlFragment)
   │
   ▼
mansart-persistence-core (EntityManagerFactory, EntityManager, PersistenceContext, 
                           Query execution, JPQL parsing, Criteria API, flush/refresh)
   │  ↑  binds to mansart-transactions (TransactionManager, UserTransaction, ScopedValue<Transaction>)
   │
   ▼
mansart-data-dialect-spi (H2Dialect, PostgresqlDialect) → JDBC → DataSource (supplied by application)
```

### Inter-Module Dependencies

```
spi ────────── jakarta.persistence-api 3.2 (compile-only / requires static)
              + re-exports mansart-data-dialect-spi (Dialect, DialectFactory)
processor ──── api, dialect-spi          (Maven scope provided; APT active only on user side)
core ───────── spi, mansart-transactions (ScopedValue-based TM)
maven-plugin ── core, classfile-api
cdi ────────── core, jakarta.cdi-api 4.1, jakarta.inject-api
tests ──────── core, cdi, dialect-h2, processor (test)
tck ────────── core, cdi, dialect-h2     (out of reactor, standalone POM 4.0.0)
external-lib ── (no Mansart deps) — plain JPA entities in a JAR
external-it ── core, external-lib (test) — proves tier-1 ≈ tier-2
```

## Non-Goals

- **No reflection on user classes at runtime** — no `java.lang.reflect`, no `Proxy`, no `MethodHandles` lookup against user types
- **No fake implementations** — anything unimplemented throws `UnsupportedOperationException("not implemented: <what>")`
- **No test-suite knowledge in main sources** — TCK package/class names, table names, or special-cases forbidden outside `mansart-persistence-tck`
- **No inline SQL** — all SQL goes through `mansart-data-dialect-spi` AST and dialect contract
- **No second transaction manager** — binds exclusively to `mansart-transactions`
- **No runtime dependency on `mansart-jakarta-data`** — both modules remain independent

## Work Plan (Detailed Milestones)

### M0 — Skeleton + Real TCK Harness Baseline

**Goal**: Wire a real (even if `not implemented`-throwing) provider registered and reachable by the official TCK runner. Prove the harness works by running the TCK and getting **real `setup*Data` errors** (not harness failures). Establish a trustworthy baseline.

**Ends when**: `./run-official-tck-persistence-3.2.sh` runs to completion and reports a measured **0/X/1745** (0 pass, X error, 1745 total) — honest baseline where all failures are in `setup*Data` or provider wiring, not in the test framework itself.

**Cards** (expanded in TASKS.md):
- M0-JP-01: Parent `pom.xml` `mansart-jakarta-persistence` + 9 sub-modules (Maven Model 4.1.0, `.mvn/` marker)
- M0-JP-02: `module-info.java` for each module (strict Java modules, minimal exports, `provides/uses` for services)
- M0-JP-03: `mansart-persistence-spi` — EntityModel, Attribute hierarchy, re-export dialect SPI types
- M0-JP-04: `mansart-persistence-core` — skeleton `MansartPersistenceProvider` (implements `jakarta.persistence.spi.PersistenceProvider`), `MansartEntityManagerFactory`, `MansartEntityManager` (all throw `UnsupportedOperationException` for now)
- M0-JP-05: TCK infra — `mansart-persistence-tck` standalone POM 4.0.0, `run-official-tck-persistence-3.2.sh`, smoke harness
- M0-JP-06: Harness wiring — provider registered via `META-INF/services/jakarta.persistence.spi.PersistenceProvider`, TCK entity classes reach our provider
- M0-JP-07: Baseline TCK run — prove harness works, document 0/N/1745 with all errors in `setup*Data`

**Pitfalls to avoid**:
- Provider not discoverable by TCK (missing service file or wrong module name)
- Classpath issues between TCK modules and Mansart provider
- Java module system barriers (missing `requires` or `opens`)
- TCK expecting specific DDL — use TCK's own DDL, not Mansart-generated schema

### M1 — Entity Metamodel + APT Generation

**Goal**: APT processor generates rich static metamodel (`_Entity`) and standard JPA static metamodel (`Entity_`) from user entities annotated with `jakarta.persistence.*` annotations. Attribute access uses `MethodHandle` obtained once at `<clinit>` — no runtime reflection.

**Ends when**: 
- APT processor compiles and generates correct metamodel for test entities
- `mansart-persistence-processor` unit tests pass (metamodel generation + MethodHandle verification)
- Integration test: compile entity `Book` → `_Book` + `Book_` produced with correct table/column names, typed attributes, MethodHandles for get/set

**Cards**:
- M1-JP-08: `MansartPersistenceProcessor` (`SourceVersion.RELEASE_25`, `@SupportedAnnotationTypes` for JPA annotations)
- M1-JP-09: Entity scanning — detects `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`, `@Version`, `@ManyToOne`, `@OneToOne`, `@JoinColumn`, `@Enumerated`, `@Embedded`, `@Embeddable`
- M1-JP-10: `_Entity` generation — `EntityModel<T>`, typed `Attribute` subtypes (`IdAttribute`, `TextAttribute`, `NumericAttribute`, `TemporalAttribute`, `ReferenceAttribute`, `EnumAttribute`, `VersionAttribute`)
- M1-JP-11: Standard JPA static metamodel generation — `Entity_` with `SingularAttribute` fields (if `jakarta.persistence-api` on classpath)
- M1-JP-12: MethodHandle resolution — `MethodHandles.privateLookupIn` in `<clinit>`, never `setAccessible`
- M1-JP-13: SQL naming conventions — snake_case column names, plural snake_case table names, FK column naming

### M2 — Maven Plugin (Tier 2) + External Library Support

**Goal**: Maven plugin using Class-File API (JEP 484) generates the same metamodel shapes for entities compiled in external JARs (not visible to APT). Tier 1 and Tier 2 are behaviorally identical.

**Ends when**: 
- `mansart-persistence-maven-plugin` builds successfully
- `mansart-persistence-external-lib` contains entities compiled elsewhere
- `mansart-persistence-external-it` proves tier-1 and tier-2 entities behave identically

**Cards**:
- JP-14: `MansartPersistenceMojo` bound to `process-classes`, scans dependency classpath for `@Entity` classes
- JP-15: Class-File API parsing — reads class file bytes, extracts annotations, generates same `_Entity` + `Entity_` as APT
- JP-16: Lazy association proxies for external entities — real subclasses generated, never `java.lang.reflect.Proxy`
- JP-17: `external-lib` — JAR with test entities (no Mansart deps)
- JP-18: `external-it` — integration tests asserting tier-1 ≈ tier-2 behavior

### M3 — Runtime Class-File API (Tier 3) Fallback

**Goal**: Escape hatch at `EntityManagerFactory` bootstrap for entities neither APT nor Maven plugin reached. Generates hidden classes via `MethodHandles.Lookup.defineHiddenClass`. Logs a warning naming the class and pointing at the plugin.

**Ends when**: 
- Runtime code generation path works for edge cases
- Warning logged for tier-3 entities
- All existing tests still pass

**Cards**:
- JP-19: Runtime `EntityModelBuilder` — builds `EntityModel` from class file bytes at bootstrap
- JP-20: `RuntimeRepositoryClassGenerator` — generates hidden implementation classes using Class-File API
- JP-21: `MansartCallback` dispatch mechanism for hidden classes
- JP-22: Warning mechanism pointing to Maven plugin for tier-3 entities

### M4 — Core Runtime: EntityManagerFactory + EntityManager

**Goal**: Functional core runtime that can manage entity lifecycles (new, managed, detached, removed) and basic persistence operations, integrated with `mansart-transactions` for transaction management.

**Ends when**:
- `MansartEntityManagerFactory.createEntityManager()` returns a working `EntityManager`
- Entity lifecycle states are tracked correctly
- Transaction integration with `mansart-transactions` works (commit/rollback)
- Unit tests: basic CRUD operations work end-to-end

**Cards**:
- JP-23: `MansartPersistenceProvider` — implements `PersistenceProvider`, parses `persistence.xml`, creates `EntityManagerFactory`
- JP-24: `MansartEntityManagerFactory` — manages `EntityManager` instances, bootstrap, metadata
- JP-25: `MansartEntityManager` — implements `EntityManager` interface, delegates to persistence context
- JP-26: Persistence context — tracks entity states (NEW, MANAGED, DETACHED, REMOVED)
- JP-27: Transaction integration — binds to `mansart-transactions` via `ScopedValue<Transaction>`
- JP-28: Connection management — uses `ScopedValue<Connection>` (no `ThreadLocal`)

### M5 — Object-Relational Mapping (Basic)

**Goal**: Map entities to tables and vice versa. Support basic mapping annotations and generate correct SQL through `mansart-data-dialect-spi`.

**Ends when**:
- Basic CRUD operations (persist, find, merge, remove) work with simple entities
- `@Id`, `@GeneratedValue`, `@Column`, `@Table` annotations are respected
- Integration tests pass with H2 in-memory database

**Cards**:
- JP-29: `EntityMapper` — maps entity instances to SQL INSERT/UPDATE/DELETE via dialect SPI
- JP-30: ID generation — `AUTO`, `IDENTITY`, `SEQUENCE`, `TABLE` strategies
- JP-31: Column mapping — name, nullable, unique, length, precision, scale
- JP-32: Table mapping — name, schema, catalog
- JP-33: Integration tests with H2 — basic CRUD, ID generation, column constraints

### M6 — Query Execution (JPQL)

**Goal**: Parse and execute JPQL queries through the dialect SPI. Support the full JPQL grammar defined by Jakarta Persistence 3.2.

**Ends when**: 
- JPQL parser handles all required constructs
- JPQL → AST → dialect SQL transformation works correctly
- Query execution returns correct results

**Cards**:
- JP-34: JPQL parser — recursive descent, hand-written (no ANTLR)
- JP-35: SELECT queries — projection, DISTINCT, WHERE, GROUP BY, HAVING, ORDER BY
- JP-36: FROM clause — entity names, joins (INNER, LEFT, RIGHT, CROSS), fetch joins
- JP-37: WHERE expressions — comparisons, logical operators, IN, BETWEEN, LIKE, IS NULL, IS EMPTY, functions
- JP-38: UPDATE queries — SET clause, WHERE
- JP-39: DELETE queries — WHERE
- JP-40: Named parameters (`:param`) and positional parameters (`?1`)
- JP-41: Aggregate functions — COUNT, SUM, AVG, MIN, MAX
- JP-42: String functions — CONCAT, SUBSTRING, TRIM, LOWER, UPPER, LENGTH, LOCATE, INDEX
- JP-43: Date/time functions — CURRENT_DATE, CURRENT_TIME, CURRENT_TIMESTAMP, EXTRACT
- JP-44: CASE expressions
- JP-45: Subqueries — scalar, IN, EXISTS, ALL, ANY

### M7 — Advanced Mapping

**Goal**: Support advanced JPA mapping features: relationships, inheritance, embedded objects, enums, etc.

**Ends when**:
- All mapping annotations are supported
- Integration tests cover complex object graphs

**Cards**:
- JP-46: `@ManyToOne`, `@OneToOne` — owning side, foreign key mapping
- JP-47: `@OneToMany`, `@ManyToMany` — collections, join tables
- JP-48: `@JoinColumn`, `@JoinTable` — custom join column/table names
- JP-49: `@Inheritance` — SINGLE_TABLE, JOINED, TABLE_PER_CLASS strategies
- JP-50: `@DiscriminatorColumn`, `@DiscriminatorValue`
- JP-51: `@Embedded`, `@Embeddable` — composite value types
- JP-52: `@Enumerated` — STRING, ORDINAL
- JP-53: `@Temporal` — DATE, TIME, TIMESTAMP
- JP-54: `@Lob` — BLOB, CLOB
- JP-55: `@Version` — optimistic locking
- JP-56: `@Transient` — non-persistent fields
- JP-57: `@Convert` — attribute converters

### M8 — Entity Lifecycle + Callbacks

**Goal**: Support JPA lifecycle callbacks and listeners.

**Ends when**:
- All lifecycle annotations are supported
- Callback ordering is correct
- Integration tests verify callback execution

**Cards**:
- JP-58: `@PrePersist`, `@PostPersist`, `@PreRemove`, `@PostRemove`, `@PreUpdate`, `@PostUpdate`, `@PostLoad`
- JP-59: `@EntityListeners` — class-level and method-level listeners
- JP-60: Callback ordering — entity listeners before entity callbacks
- JP-61: Inherited callbacks

### M9 — Cascading + Orphan Removal

**Goal**: Support cascade operations and orphan removal.

**Ends when**:
- Cascade types (ALL, PERSIST, MERGE, REMOVE, REFRESH, DETACH) work correctly
- Orphan removal is implemented
- Integration tests verify behavior

**Cards**:
- JP-62: `@CascadeType` — all cascade types
- JP-63: `orphanRemoval = true`
- JP-64: Cascade ordering and edge cases

### M10 — Fetch Types + Lazy Loading

**Goal**: Support lazy loading and fetch type control.

**Ends when**:
- Lazy loading works for relationships
- Fetch types (LAZY, EAGER) are respected
- Integration tests verify no N+1 queries

**Cards**:
- JP-65: `@FetchType(LAZY)` and `@FetchType(EAGER)`
- JP-66: Lazy loading proxies — generated subclasses, not `java.lang.reflect.Proxy`
- JP-67: Fetch join hints

### M11 — Criteria API

**Goal**: Support the full Criteria API for type-safe query construction.

**Ends when**:
- Criteria API queries work correctly
- Integration tests cover all major constructs

**Cards**:
- JP-68: `CriteriaBuilder` — root query construction
- JP-69: `CriteriaQuery` — SELECT, UPDATE, DELETE
- JP-70: `Root`, `Join`, `Fetch`, `Path` — navigation
- JP-71: `Predicate` — WHERE conditions
- JP-72: `Order` — ORDER BY
- JP-73: `GroupBy`, `Having` — aggregation
- JP-74: `Subquery` — nested queries
- JP-75: Metamodel-based queries using `Entity_` static metamodel

### M12 — Named Queries + Native Queries

**Goal**: Support named queries and native SQL queries.

**Ends when**:
- Named queries (JPQL and native) work correctly
- Native query support is complete
- Integration tests verify functionality

**Cards**:
- JP-76: `@NamedQuery`, `@NamedQueries` — JPQL named queries
- JP-77: `@NamedNativeQuery`, `@NamedNativeQueries` — native SQL named queries
- JP-78: `@SqlResultSetMapping`, `@EntityResult`, `@ColumnResult`, `@FieldResult`
- JP-79: Query hints — `Query.setHint`
- JP-80: Native query execution with result mapping

### M13 — Persistence Context + Caching

**Goal**: Support persistence context management and caching.

**Ends when**:
- Persistence context lifecycle is correct
- First-level cache works correctly
- Second-level cache support is implemented

**Cards**:
- JP-81: Persistence context propagation — per transaction, extended
- JP-82: First-level cache — entity identity map within persistence context
- JP-83: `Cache` interface and `CacheRetrieveMode`, `CacheStoreMode`
- JP-84: `SharedCacheMode` — NONE, ALL, ENABLE_SELECTIVE, DISABLE_SELECTIVE, UNSPECIFIED
- JP-85: Cache invalidation and eviction

### M14 — Transaction Management + Synchronization

**Goal**: Full integration with `mansart-transactions` and JTA.

**Ends when**:
- Transaction management works correctly with both local and JTA transactions
- Synchronization callbacks are properly integrated

**Cards**:
- JP-86: Local transaction support (RESOURCE_LOCAL)
- JP-87: JTA transaction support (JTA)
- JP-88: `TransactionSynchronizationRegistry` integration
- JP-89: Synchronization callbacks for JPA events
- JP-90: Transaction timeout and isolation level

### M15 — CDI Integration (Vauban)

**Goal**: Full CDI 4.1 integration via BCE (Build Compatible Extension).

**Ends when**:
- CDI module works with Vauban
- Repository injection works in CDI context
- Integration tests pass

**Cards**:
- JP-91: `MansartPersistenceExtension` — BCE for CDI integration
- JP-92: Producer for `EntityManager`, `EntityManagerFactory`, `PersistenceProvider`
- JP-93: `@PersistenceContext` injection support
- JP-94: `@PersistenceUnit` injection support
- JP-95: CDI scoping — `@ApplicationScoped`, `@RequestScoped`, `@TransactionScoped`
- JP-96: Integration tests with Vauban

### M16 — PostgreSQL Dialect Support

**Goal**: Full PostgreSQL dialect support alongside H2.

**Ends when**:
- PostgreSQL dialect works correctly
- Integration tests pass on PostgreSQL
- TCK passes on PostgreSQL

**Cards**:
- JP-97: PostgreSQL dialect implementation (reusing `mansart-data-dialect-spi`)
- JP-98: PostgreSQL-specific features (RETURNING, ON CONFLICT, etc.)
- JP-99: Integration tests with Testcontainers PostgreSQL

### M17 — Error Handling + Exceptions

**Goal**: Proper error handling and Jakarta Persistence exception mapping.

**Ends when**:
- All Jakarta Persistence exceptions are properly thrown
- SQL errors are mapped to appropriate JPA exceptions
- Integration tests verify error handling

**Cards**:
- JP-100: SQLState mapping to JPA exceptions (`EntityExistsException`, `OptimisticLockException`, etc.)
- JP-101: `PersistenceException` hierarchy
- JP-102: Constraint violation handling
- JP-103: Transaction rollback exception handling
- JP-104: Entity not found, illegal state, illegal argument exceptions

### M18 — Validation + Schema Generation

**Goal**: Support schema validation and generation.

**Ends when**:
- Schema validation works correctly
- Schema generation produces correct DDL
- Integration tests verify functionality

**Cards**:
- JP-105: Schema validation — validates entity mappings against database schema
- JP-106: Schema generation — generates DDL from entity mappings
- JP-107: `persistence.xml` schema generation properties
- JP-108: Integration tests with schema generation

### M19 — Performance + Benchmarks

**Goal**: Performance optimization and benchmarks.

**Ends when**:
- Performance benchmarks are established
- BENCH.md documents performance characteristics
- No pinning under virtual threads

**Cards**:
- JP-109: BENCH.md with baseline benchmarks
- JP-110: JMH benchmarks for common operations
- JP-111: Virtual thread pinning verification
- JP-112: Performance tuning and optimization

### M20 — Official Jakarta Persistence 3.2 TCK

**Goal**: Pass the complete official TCK with 100% reliability.

**Ends when**: **1745/1745 PASS** (269 client classes, ~1,745 test methods)

**Cards**:
- JP-113: TCK infrastructure — standalone POM, runner scripts, profiles
- JP-114: TCK EntityTests — all entity-related tests pass
- JP-115: TCK PersistenceTests — all persistence-related tests pass
- JP-116: TCK QueryTests — all query-related tests pass
- JP-117: TCK SignatureTests — all signature tests pass
- JP-118: Full TCK run — 1745/1745 PASS on H2 and PostgreSQL

## Card Naming Convention

All implementation cards follow the pattern **`M<milestone>-JP-<number>`** to explicitly bind each card to its milestone. For example:
- `M0-JP-01` — Parent pom.xml with 9 sub-modules (Milestone 0)
- `M1-JP-08` — APT Processor implementation (Milestone 1)

## Decomposition Rules

- **Milestone = planning unit** — each milestone ends on a measured number (TCK PASS/total for named client packages, or the equivalent spec-conformance signal)
- **Card = work unit** — ≤4 files, one proving test, explicit dependencies
- **Only the current milestone gets expanded into cards** — `tracker` expands the next milestone when the current one closes
- **TDD discipline** — failing test first, watch it fail, smallest real implementation, then refactor
- **No fake progress** — an honest 0/1745 baseline is worth more than an invented 40/1745 with stubs

## Definition of Done

A card is done when (in the same session):
1. Build green on every module (`mvn clean install -DskipTests`)
2. Unit tests green (`mvn test`)
3. `auditor` clean on the diff restricted to the sub-module directory
4. SonarQube quality gate green on new code (see `vidocq-quality` skill, projectKey: `vidocq-mansart-persistence`)
5. If the card names a TCK client — `tck-runner`'s measured number recorded

A milestone is done when **all its cards are DONE** and its ending number is verified.

## Locked Decisions

| # | Decision | Impact |
| --- | --- | --- |
| 1 | **Three-tier code generation** | APT (compile-time) + Maven plugin (build-time) + Runtime Class-File API (bootstrap-time) |
| 2 | **Zero reflection on user classes** | All access via generated code, never `java.lang.reflect` or `Proxy` |
| 3 | **Strict Java modules** | Minimal exports, `ScopedValue` instead of `ThreadLocal` |
| 4 | **Shared dialect SPI** | Reuse `mansart-data-dialect-spi` with `mansart-jakarta-data` |
| 5 | **Transaction binding** | Exclusive use of `mansart-transactions` for TM |
| 6 | **DataSource independence** | Application supplies `DataSource`, `mansart-pool` is optional |
| 7 | **Runtime independence** | No runtime dependency on `mansart-jakarta-data` |
| 8 | **TDD discipline** | Failing test first, smallest real implementation |
| 9 | **Honest metrics** | Only PASS counts, stubs forbidden, `auditor` enforces |

---

*This file is maintained by the planning process. For current execution status, see `STATUS.md` and `TASKS.md`.*