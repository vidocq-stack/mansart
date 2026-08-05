# AGENTS.md - Mansart Jakarta Persistence 3.2

Contributor guidance for agents working on the **Mansart Jakarta Persistence 3.2** implementation.
This file **extends and specializes** the root [../../AGENTS.md](../../AGENTS.md) for Jakarta Persistence-specific work.

## Terminology

Use **Jakarta Persistence** (or **JPA** for brevity in technical contexts) when referring to the
specification. Do **not** use "JPA 3.2" alone — always qualify with **Jakarta Persistence 3.2**
in prose, identifiers, or documentation.

**Exception**: `jakarta.persistence` package names and Java identifiers keep the `jakarta.persistence` form.

## Project Structure

```
mansart-jakarta-persistence/
├── mansart-persistence-api/           # Re-export of Jakarta Persistence API 3.2
├── mansart-persistence-spi/          # Internal SPI interfaces (not public API)
├── mansart-persistence-core/          # Core runtime implementation
├── mansart-persistence-processor/    # APT: static metamodel + bytecode enhancement (ClassFile API)
├── mansart-persistence-cdi/          # CDI 4.1 integration (Build Compatible Extension)
├── mansart-persistence-tests/        # Unit and integration tests (JUnit 6)
└── mansart-persistence-tck/           # Jakarta Persistence 3.2 TCK execution (TestNG)
```

## Architecture Principles

### 1. Static Code Generation (ClassFile API - JEP 484)

**Rule**: Generate as much as possible at **compile-time** using ClassFile API.
- Entity bytecode enhancement (dirty tracking, lazy loading proxies)
- Static metamodel classes (`Entity_`)
- Repository implementations (when applicable)

**Rationale**:
- GraalVM/Leyden AOT compatible
- Virtual Threads friendly (no ThreadLocal, use ScopedValue)
- Zero runtime reflection overhead
- Better security (no `--add-opens` required)

**Implementation**:
- `MansartPersistenceProcessor` (APT annotation processor)
- `EntityEnhancer` (ClassFile-based bytecode enhancer)
- `StaticMetamodelWriter` (generates `SingularAttribute`, `PluralAttribute`)

### 2. Virtual Threads Integration

**Rule**: Use `ScopedValue` for persistence context propagation, never `ThreadLocal`.

```java
// CORRECT
private static final ScopedValue<PersistenceContext> currentPersistenceContext = ScopedValue.newInstance();

// WRONG - do not use
private static final ThreadLocal<PersistenceContext> threadLocalContext = new ThreadLocal<>();
```

**Rationale**: Virtual Threads (JEP 444) have different semantics; `ScopedValue` is designed
for structured concurrency and provides proper nesting and cleanup.

### 3. JPMS / Java Modules

**Rule**: Respect terminology from root AGENTS.md - use **Java Modules** or **Java module**,
not **JPMS**.

Each module must have a `module-info.java` with:
- Minimal `exports` (only public APIs)
- No `opens` unless absolutely necessary (and documented)
- Explicit `requires` for all dependencies

**Current Status**: Module-info files exist but are temporarily disabled (`.bak` extension)
until all dependencies have proper module declarations.

### 4. Zero External Dependencies

**Rule**: Only depend on:
- Jakarta EE specifications (persistence, cdi, transaction, inject)
- Mansart modules (`mansart-data-*`, `mansart-pool`, `mansart-transactions`)
- Testing libraries (JUnit 6, AssertJ, H2, PostgreSQL, Testcontainers)

**Any new runtime dependency requires explicit justification** in the PR description.

## TCK Execution

### Prerequisites

1. **TCK must be built and installed locally** (not available on Maven Central):
   ```bash
   cd mansart-jakarta-persistence/mansart-persistence-tck
   ./setup-tck.sh  # Clones, builds, and installs Jakarta Persistence TCK 3.2.2-SNAPSHOT
   ```

2. **Verify installation**:
   ```bash
   ls ~/.m2/repository/jakarta/tck/persistence-tck-*/
   ```

### Running the TCK

```bash
# From mansart-jakarta-persistence/

# Smoke test (default profile - verifies basic integration)
mvn -pl mansart-persistence-tck test

# Full TCK execution (H2 in-memory)
mvn -pl mansart-persistence-tck -Ptck test

# With PostgreSQL
mvn -pl mansart-persistence-tck -Ptck,pgsql test

# Single test class
mvn -pl mansart-persistence-tck -Ptck test -Dtest=EntityTest

# With custom database
mvn -pl mansart-persistence-tck -Ptck test \
    -Dtck.db.url=jdbc:h2:mem:custom-db \
    -Dtck.db.user=sa \
    -Dtck.db.password=""
```

### TCK Properties

| Property | Default | Description |
|----------|---------|-------------|
| `tck.db.url` | `jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1` | JDBC URL |
| `tck.db.user` | `sa` | Database username |
| `tck.db.password` | (empty) | Database password |
| `tck.db.driver` | `org.h2.Driver` | JDBC driver class |
| `mansart.provider` | `io.vidocq.mansart.persistence.core.bootstrap.MansartPersistenceProvider` | JPA provider |

### TCK Status (as of M6)

- **Target**: Jakarta Persistence 3.2 TCK 3.2.2-SNAPSHOT
- **Current**: TCK not yet fully passing
- **Priority**: Complete JPQL implementation (M6), then TCK compliance (M7+)
- **Reference**: See [M7-PLAN.md](M7-PLAN.md) for detailed TCK milestones

## Milestones and Current Work

### Completed Milestones

| Milestone | Date | Status | Deliverables |
|-----------|------|--------|--------------|
| M1 | 2026-05-XX | ✅ | Project structure, POM, basic SPI, skeleton implementations |
| M2 | 2026-05-XX | ✅ | Static metamodel generation (APT) |
| M3 | 2026-05-XX | ✅ | Core runtime (EntityManager, CRUD, L1 cache) |
| M4 | 2026-05-XX | ✅ | Integration tests, H2 DataSource, 38 tests pass |
| M5 | 2026-05-XX | ✅ | JPQL infrastructure, predicates, logical operators |

### In Progress - M6: Complete JPQL Implementation

**Focus**: Full JPQL 3.2 support
- [ ] GROUP BY and HAVING clauses
- [ ] JOIN syntax (INNER, LEFT, RIGHT, CROSS)
- [ ] Subqueries in FROM clause
- [ ] ALL/ANY/SOME predicates
- [ ] Additional JPQL functions (string, numeric, date/time, database)
- [ ] Query caching

### Planned - M7: Advanced Features

- Transactions with L2 cache
- Optimistic locking with `@Version`
- Full inheritance support (SINGLE_TABLE, JOINED, TABLE_PER_CLASS)
- TCK compliance (target: 100% pass rate)

### Future Milestones (M8-M13)

- Lifecycle callbacks
- Named queries and native queries
- Bean validation integration
- Performance optimization
- Full TCK certification

## Development Workflow

### 1. Build Process

```bash
# Full build (from mansart/)
mvn clean install -pl mansart-jakarta-persistence -DskipTests -Dlicense.skip=true

# Build with tests
mvn clean install -pl mansart-jakarta-persistence

# Incremental build (from mansart-jakarta-persistence/)
cd mansart-jakarta-persistence
mvn clean install -DskipTests
```

### 2. Test Strategy

**TDD Mandatory**: Write tests before implementation

```
┌─────────────────────────────┐
│  Unit Tests (JUnit 6)        │  ← Fast feedback, pure logic
│  mansart-persistence-tests/  │
└─────────────────────────────┘
           │
           ▼
┌─────────────────────────────┐
│  Integration Tests           │  ← H2 in-memory, full stack
│  mansart-persistence-tests/  │
└─────────────────────────────┘
           │
           ▼
┌─────────────────────────────┐
│  TCK Tests (TestNG)          │  ← Official Jakarta spec tests
│  mansart-persistence-tck/    │
└─────────────────────────────┘
```

### 3. Code Review Checklist

Before merging a PR that touches Jakarta Persistence implementation:

- [ ] All existing tests pass (`mvn test` in mansart-jakarta-persistence)
- [ ] New tests added for new functionality (TDD principle)
- [ ] License headers present on all new files
- [ ] No new runtime dependencies without justification
- [ ] JPMS module-info updated (or .bak file updated)
- [ ] Documentation updated (README.md, this file if conventions changed)
- [ ] TCK smoke test passes (`mvn -pl mansart-persistence-tck test`)
- [ ] Full TCK run attempted and results documented

## Key Implementation Classes

### Core Runtime (`mansart-persistence-core`)

| Class | Responsibility |
|-------|----------------|
| `MansartPersistenceProvider` | JPA PersistenceProvider implementation |
| `DefaultMansartEntityManagerFactory` | EntityManagerFactory implementation |
| `MansartEntityManager` | EntityManager implementation (L1 cache, CRUD) |
| `MansartEntityTransaction` | Transaction management |
| `QueryExecutionContext` | JPQL parsing and execution context |
| `MansartQuery` / `MansartTypedQuery` | Query implementation |
| `JpqlToRuntimeConverter` | Converts JPQL AST to runtime query |

### APT Processor (`mansart-persistence-processor`)

| Class | Responsibility |
|-------|----------------|
| `MansartPersistenceProcessor` | Main APT processor |
| `StaticMetamodelWriter` | Generates static metamodel classes |
| `EntityEnhancer` | Bytecode enhancement for dirty tracking |
| `ProxyGenerator` | Generates lazy loading proxies |

### CDI Integration (`mansart-persistence-cdi`)

| Class | Responsibility |
|-------|----------------|
| `MansartPersistenceExtension` | CDI Build Compatible Extension |
| `PersistenceContextProducer` | Produces @PersistenceContext beans |
| `EntityManagerProducer` | Produces EntityManager beans |

## Debugging TCK Failures

### Common Issues

1. **Entity not found / Class not enhanced**
   - Check `META-INF/persistence.xml` in test archive
   - Verify APT processor is on annotation processor path
   - Check processor logs for errors

2. **Persistence provider not found**
   - Verify `jakarta.persistence.provider` property is set
   - Check `MansartPersistenceProvider` is in classpath
   - Ensure `META-INF/services/jakarta.persistence.spi.PersistenceProvider` exists

3. **SQL syntax errors**
   - Check dialect implementation for the target database
   - Verify JPQL to SQL conversion in `JpqlToRuntimeConverter`
   - Enable SQL logging: `-Dmansart.sql.log=true`

4. **Lazy loading / proxy issues**
   - Verify ScopedValue usage in persistence context
   - Check proxy generation in APT processor
   - Ensure proper equals/hashCode implementation on enhanced entities

### Debug Flags

```bash
# Enable verbose SQL logging
-Dmansart.sql.log=true -Dmansart.sql.log.level=DEBUG

# Enable APT processor debugging
-Dmansart.apt.debug=true

# Enable JPQL parsing debugging
-Dmansart.jpql.debug=true

# Enable entity enhancement logging
-Dmansart.enhancement.debug=true
```

## Documentation Standards

Follow the root AGENTS.md documentation conventions:
- Antora modules in `docs/en` and `docs/fr`
- Navigation order: index → getting-started → usage → concepts → internals → tck → reference → migration
- EN/FR parity: every page exists in both languages

**Jakarta Persistence-specific additions**:
- Include TCK section documenting current pass rate and failures
- Document JPQL support matrix
- Document entity enhancement requirements and limitations

## Skill Integration

When working on Mansart Jakarta Persistence, the following skills are available:

- `/mansart-persistence` - General Jakarta Persistence implementation guidance
- `/mansart-persistence-tck` - TCK execution and failure analysis
- `/log-bug` (inherited) - Log bugs to BUG.md
- `/log-bench` (inherited) - Log benchmarks to BENCH.md

## References

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/)
- [Jakarta Persistence TCK Repository](https://github.com/jakartaee/persistence/tree/main/tck)
- [ClassFile API (JEP 484)](https://openjdk.org/jeps/484)
- [ScopedValue API (JEP 444)](https://openjdk.org/jeps/444)
- [Mansart ROADMAP.md](../ROADMAP.md)
- [M7-PLAN.md](M7-PLAN.md) (current milestone)
