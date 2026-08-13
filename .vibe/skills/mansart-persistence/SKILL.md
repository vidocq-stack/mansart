---
name: mansart-persistence
description: |
  Guidance for working on Mansart Jakarta Persistence 3.2 implementation.
  Load this skill when implementing JPA 3.2 features, debugging entity management,
  or working with the Mansart Persistence codebase.
---

# Mansart Jakarta Persistence 3.2 Implementation Skill

You are working on **Mansart**'s implementation of **Jakarta Persistence 3.2** (JPA).
This skill provides architecture context, implementation patterns, and development workflow
for the Mansart Persistence module.

## Core Principles

### 1. Static Code Generation (APT first, Class-File API last resort)

**ALWAYS prefer compile-time generation over runtime reflection.**

Order of preference:

1. **APT** (`javax.annotation.processing`) — the default mechanism. Generate Java
   **sources** at compile time for:
   - Static metamodel (`_Entity` classes with `SingularAttribute`, `PluralAttribute`)
   - Entity support classes (dirty tracking, lazy loading)
   - Lazy-loading proxies as generated subclasses
2. **Class-File API** (JEP 484) — **last resort only**, when source-level generation
   cannot express the need (e.g. modifying already-compiled entity bytecode). Every
   use must carry a written justification. Never ASM/Byte Buddy/Javassist.

**Why:**
- GraalVM/Leyden AOT compatible
- Virtual Threads friendly
- Zero runtime reflection overhead
- Better security model
- Generated sources are readable, debuggable, and reviewable (unlike raw bytecode)

**Implementation locations:**
- `mansart-persistence-processor/` - APT annotation processor (sources emitted via `Filer`)
- `bytecode/` sub-package — justified Class-File API generators only (`ClassFile`,
  `ClassBuilder`, `MethodBuilder`, `FieldBuilder`)

### 2. Virtual Threads Integration (JEP 444)

**NEVER use ThreadLocal for persistence context propagation.**

```java
// CORRECT: Use ScopedValue
private static final ScopedValue<PersistenceContext> context = ScopedValue.newInstance();

// WRONG: ThreadLocal breaks with Virtual Threads
private static final ThreadLocal<PersistenceContext> context = new ThreadLocal<>();
```

**Key classes using ScopedValue:**
- `PersistenceContextHolder` in `mansart-persistence-core`
- `MansartEntityManager` for current EntityManager
- `MansartEntityTransaction` for transaction context

### 3. Zero Runtime Reflection

**Rule:** If you can generate it at compile time, do it. Never use reflection at runtime.

**Exceptions (must be documented):**
- ServiceLoader discovery (SPI)
- CDI integration (required by spec)
- Dynamic proxy generation (if ClassFile API insufficient)

### 4. TDD (mandatory)

Write the failing test FIRST, then the implementation, then refactor (red → green →
refactor). No implementation code without a motivating test. Unit tests live in
`mansart-persistence-tests` (JUnit + AssertJ); spec-level scenarios come from the TCK.

### 5. CDI integration via Vauban (Build Compatible Extension)

`mansart-persistence-cdi` integrates with **Vauban** (CDI 4.1 Lite) through a
**Build Compatible Extension** — never a Portable Extension. **Mirror the existing
`mansart-jakarta-data/mansart-data-cdi` module**: same BCE structure (discovery,
synthesis), same registration patterns, same test approach. Read that module before
writing any CDI code here.

## Project Structure

```
mansart-jakarta-persistence/
├── mansart-persistence-api/           # API re-export (jakarta.persistence)
├── mansart-persistence-spi/          # Internal SPI (not public)
├── mansart-persistence-core/          # Runtime implementation
│   ├── bootstrap/                     # PersistenceProvider, EMF
│   ├── runtime/                      # EntityManager, Query
│   ├── cache/                        # L1 cache implementation
│   ├── transaction/                  # Transaction management
│   └── enhancement/                  # Runtime enhancement fallback
├── mansart-persistence-processor/    # APT processor
│   ├── apt/                          # Annotation processor
│   ├── bytecode/                     # ClassFile-based generators
│   └── metamodel/                    # Static metamodel generation
├── mansart-persistence-cdi/          # CDI 4.1 integration
│   └── extension/                    # Build Compatible Extension
├── mansart-persistence-tests/        # Tests (JUnit 6)
│   ├── unit/                         # Unit tests
│   └── integration/                  # Integration tests (H2)
└── mansart-persistence-tck/           # TCK execution (TestNG)
```

## Key Implementation Patterns

### EntityManager Implementation

**File:** `mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/runtime/MansartEntityManager.java`

**Responsibilities:**
- L1 cache (IdentityHashMap-based)
- CRUD operations (persist, merge, remove, find)
- Entity state management
- Query execution
- Transaction coordination

**Current status:** 
- Basic CRUD: ✅ Complete
- L1 cache: ✅ Implemented
- Entity state tracking: ✅ Implemented
- Transaction integration: ✅ Basic support

### JPQL Implementation

**Architecture:**
```
JPQL String
    │
    ▼
JpqlParser (ANTLR or hand-written)
    │
    ▼
JpqlAst (sealed hierarchy)
    │
    ▼
JpqlToRuntimeConverter
    │
    ▼
RepositoryRuntime Query (mansart-data-core)
    │
    ▼
Dialect-specific SQL (H2, PostgreSQL)
```

**Key files:**
- `JpqlParser.java` - Parses JPQL to AST
- `JpqlToRuntimeConverter.java` - Converts AST to runtime query objects
- `MansartQuery.java` / `MansartTypedQuery.java` - Query implementations
- `QueryExecutionContext.java` - Execution context with parameter binding

**M6 Focus (Current):**
- GROUP BY and HAVING support
- Explicit JOIN syntax (INNER, LEFT, RIGHT, CROSS)
- Subqueries in FROM clause
- ALL/ANY/SOME predicates
- Additional JPQL functions

### Static Metamodel Generation

**File:** `mansart-persistence-processor/src/main/java/io/vidocq/mansart/persistence/processor/metamodel/StaticMetamodelWriter.java`

**Input:** Entity class with JPA annotations
**Output:** `_Entity` class with:
- `SingularAttribute` for basic attributes
- `PluralAttribute` for collections
- Proper type resolution
- Attribute navigation support

**Example:**
```java
// Input
@Entity
public class Person {
    @Id
    private Long id;
    private String name;
    @ManyToOne
    private Department department;
}

// Generated Output
@StaticMetamodel(Person.class)
public class Person_ {
    public static volatile SingularAttribute<Person, Long> id;
    public static volatile SingularAttribute<Person, String> name;
    public static volatile SingularAttribute<Person, Department> department;
}
```

### Bytecode Enhancement

> **Last resort.** Prefer APT-generated support classes / subclasses for dirty tracking
> and lazy loading. Keep this Class-File-API path only where bytecode-level modification
> of already-compiled classes is unavoidable, with a written justification.

**File:** `mansart-persistence-processor/src/main/java/io/vidocq/mansart/persistence/processor/bytecode/EntityEnhancer.java`

**Enhancements applied:**
1. **Dirty tracking:** Bitmask field + field-level tracking
2. **Lazy loading:** Proxy generation for `@ManyToOne`, `@OneToOne`
3. **Persistence awareness:** `EntityMetadata` field in enhanced classes
4. **Change detection:** Automatically detect changes for flush

**Implementation approach:**
- Uses ClassFile API (JEP 484) to modify class bytecode
- Adds synthetic fields and methods
- Maintains original class structure

## Development Workflow

### 1. Building

```bash
# From mansart/ directory
mvn clean install -pl mansart-jakarta-persistence -DskipTests -Dlicense.skip=true

# From mansart-jakarta-persistence/ directory
mvn clean install -DskipTests

# With tests
mvn clean install
```

### 2. Running Tests

```bash
# Unit tests only
mvn test -pl mansart-persistence-core

# Integration tests (H2 in-memory)
mvn test -pl mansart-persistence-tests

# Specific test class
mvn test -pl mansart-persistence-core -Dtest=MansartEntityManagerTest
```

### 3. TCK Execution

```bash
# First, install TCK (one time)
cd mansart-jakarta-persistence/mansart-persistence-tck
./setup-tck.sh

# Smoke test (verifies basic integration)
mvn -pl mansart-persistence-tck test

# Full TCK (H2 in-memory)
mvn -pl mansart-persistence-tck -Ptck test

# With PostgreSQL
mvn -pl mansart-persistence-tck -Ptck,pgsql test

# Single TCK test
mvn -pl mansart-persistence-tck -Ptck test -Dtest=EntityTest
```

### 4. Debugging

**Common debug flags:**
```bash
# SQL logging
-Dmansart.sql.log=true -Dmansart.sql.log.level=DEBUG

# APT processor debugging
-Dmansart.apt.debug=true

# JPQL parsing debugging
-Dmansart.jpql.debug=true

# Entity enhancement logging
-Dmansart.enhancement.debug=true

# Full debug
-Dmansart.debug=true
```

## Current Milestone: M6 - Complete JPQL Implementation

### Priority 1: GROUP BY and HAVING (Critical)

**Files to modify:**
- `GroupBy.java` (new) - Group by clause representation
- `JpqlToRuntimeConverter.convertGroupBy()` - JPQL to runtime conversion
- `JpqlToRuntimeConverter.convertHaving()` - HAVING predicate conversion
- `QueryExecutionParams` - Add groupBy and having fields
- `H2Dialect.select()` - Add GROUP BY and HAVING SQL generation
- `PostgresqlDialect.select()` - Add GROUP BY and HAVING SQL generation

**Test files to add:**
- `GroupByTest.java` - Unit tests for GROUP BY functionality
- `HavingTest.java` - Unit tests for HAVING functionality

### Priority 2: JOIN Syntax (Critical)

**Files to modify:**
- `JoinType.java` (new) - JOIN types enum
- `JoinExpression.java` (new) - JOIN ... ON representation
- `JpqlToRuntimeConverter.convertJoin()` - JPQL JOIN conversion
- `QueryExecutionContext` - Enhance for explicit joins
- `H2Dialect.select()` - Render explicit JOINs
- `PostgresqlDialect.select()` - Render explicit JOINs

**Test files to add:**
- `JoinSyntaxTest.java` - Tests for all JOIN types
- `JoinOnTest.java` - Tests for JOIN ... ON conditions

### Priority 3: Subqueries in FROM (High)

**Files to modify:**
- `JpqlToRuntimeConverter.convertFromClause()` - Handle subqueries
- `QueryExecutionContext` - Add subquery alias management
- `RepositoryRuntime` - Support derived tables from subqueries

### Priority 4: ALL/ANY/SOME Predicates (High)

**Files to modify:**
- `Where.java` - Add All/Any/Some record types
- `JpqlToRuntimeConverter.convertAllAnySomePredicate()` - Implementation
- `H2Dialect.renderPredicate()` - Add ALL/ANY/SOME cases
- `PostgresqlDialect.renderPredicate()` - Add ALL/ANY/SOME cases

## Common Tasks

### Adding a New JPQL Feature

1. **Parse**: Add grammar rule in `JpqlParser` or extend existing rule
2. **AST**: Create new AST node type (sealed class hierarchy)
3. **Convert**: Implement conversion in `JpqlToRuntimeConverter`
4. **Runtime**: Extend `RepositoryRuntime` or `Where` to support new feature
5. **Dialect**: Add SQL rendering in all dialect implementations
6. **Test**: Add unit tests and integration tests

### Adding a New Entity Feature

1. **APT**: Extend `MansartPersistenceProcessor` to detect new annotation
2. **Enhancement**: Prefer APT-generated support code; only as a last resort add
   bytecode enhancement in `EntityEnhancer` (Class-File API, justified)
3. **Runtime**: Update `MansartEntityManager` to handle new feature
4. **Metamodel**: Update `StaticMetamodelWriter` if metamodel affected
5. **Test**: Add test entities and test cases

### Adding Database Dialect Support

1. **SPI**: Implement `MansartDialect` (extends mansart-data-dialect-spi)
2. **Factory**: Register in `META-INF/services/io.vidocq.mansart.data.dialect.spi.Dialect`
3. **Functions**: Implement all JPQL functions for the dialect
4. **Types**: Add type mappings for dialect-specific types
5. **Tests**: Add dialect-specific tests

## Code Style and Conventions

### Naming Conventions

- **Classes**: PascalCase, descriptive names
- **Methods**: camelCase, verb-first (`persistEntity`, `findById`, `convertToRuntime`)
- **Fields**: camelCase, descriptive
- **Constants**: UPPER_SNAKE_CASE
- **Packages**: lowercase, hierarchical (`io.vidocq.mansart.persistence.core.runtime`)

### Code Formatting

- Follow existing style in the file you're modifying
- Use 4-space indentation
- Braces on same line for methods, next line for classes/interfaces
- Line length: 120 characters max (but prefer readability)

### Javadoc

- **ALL public APIs must have Javadoc**
- **ALL public methods must have Javadoc**
- Use `{@code}` for code references
- Use `{@link}` for cross-references
- Use `@implNote` for implementation notes
- Use `@apiNote` for API usage notes

**Example:**
```java
/**
 * Persists the given entity to the database.
 *
 * <p>The entity will be added to the persistence context and scheduled for insertion
 * on the next flush. If the entity already exists in the database (has an ID),
 * it will be merged instead.
 *
 * @param entity the entity to persist, must not be {@code null}
 * @return the persisted entity (same instance)
 * @throws IllegalArgumentException if entity is {@code null}
 * @throws IllegalStateException if this EntityManager is closed
 * @implNote This implementation uses bytecode-enhanced entities for dirty tracking,
 *          so no runtime reflection is required for change detection.
 */
public <T> T persist(T entity) {
    // implementation
}
```

### Error Handling

- **Use specific exception types** (don't throw generic RuntimeException)
- **Include context in error messages**
- **Wrap lower-level exceptions** with JPA-standard exceptions where appropriate

**Common JPA exceptions:**
- `IllegalArgumentException` - Invalid argument
- `IllegalStateException` - Invalid state (closed EM, no transaction)
- `EntityExistsException` - Entity already exists
- `EntityNotFoundException` - Entity not found
- `OptimisticLockException` - Optimistic lock conflict
- `PessimisticLockException` - Pessimistic lock conflict
- `TransactionRequiredException` - Transaction required
- `PersistenceException` - General persistence error

## Testing Guidelines

### Test Structure

```
mansart-persistence-tests/
├── src/test/java/
│   └── io/vidocq/mansart/persistence/tests/
│       ├── unit/                          # Unit tests (no DB)
│       │   ├── entity/                   # Entity-related tests
│       │   ├── query/                    # Query-related tests
│       │   └── runtime/                  # Runtime tests
│       └── integration/                  # Integration tests (H2)
│           ├── entity/                   # Entity integration tests
│           ├── query/                    # Query integration tests
│           └── transaction/              # Transaction tests
```

### Test Naming

- **Unit tests**: `ClassNameTest` (e.g., `MansartEntityManagerTest`)
- **Integration tests**: `ClassNameIntegrationTest` (e.g., `EntityManagerIntegrationTest`)
- **Test methods**: `testMethodName` or `methodName_whenCondition_expectResult`

### Test Style

**Use AssertJ for assertions:**
```java
import static org.assertj.core.api.Assertions.*;

// Instead of:
assertEquals(expected, actual);

// Use:
assertThat(actual).isEqualTo(expected);

// For exceptions:
assertThatThrownBy(() -> methodThatThrows())
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("expected message");
```

**Test data builders:**
```java
// Use builders for complex test data
Person person = PersonBuilder.create()
    .withId(1L)
    .withName("John Doe")
    .withDepartment(department)
    .build();
```

## Troubleshooting

### Common Issues and Solutions

**1. Entity not enhanced**
- **Symptom**: Lazy loading doesn't work, dirty tracking fails
- **Check**: APT processor is on annotation processor path
- **Check**: Processor logs for errors
- **Fix**: Ensure `mansart-persistence-processor` is in annotation processor path

**2. Persistence provider not found**
- **Symptom**: `No Persistence provider for EntityManager`
- **Check**: `META-INF/services/jakarta.persistence.spi.PersistenceProvider` exists
- **Check**: `jakarta.persistence.provider` property is set
- **Fix**: Add service file or set system property

**3. SQL syntax errors**
- **Symptom**: Database reports SQL syntax error
- **Check**: Enable SQL logging (`-Dmansart.sql.log=true`)
- **Check**: JPQL to SQL conversion in `JpqlToRuntimeConverter`
- **Fix**: Update dialect implementation for the target database

**4. ClassNotFoundException for generated classes**
- **Symptom**: Metamodel classes or enhanced classes not found
- **Check**: APT processor ran successfully
- **Check**: Generated classes are in target/classes
- **Fix**: Clean and rebuild (`mvn clean install`)

**5. Lazy loading fails**
- **Symptom**: Proxy initialization fails or returns null
- **Check**: ScopedValue is properly set for the current virtual thread
- **Check**: Proxy generation in APT processor
- **Fix**: Verify `ScopedValue.where()` usage in EntityManager operations

## References

### Specification
- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/)
- [JPA 3.2 Javadoc](https://jakarta.ee/specifications/persistence/3.2/apidocs/)

### Implementation
- [Mansart Jakarta Persistence README.md](../mansart-jakarta-persistence/README.md)
- [M7-PLAN.md](../mansart-jakarta-persistence/M7-PLAN.md)
- [AGENTS.md](../mansart-jakarta-persistence/AGENTS.md)

### Java Features
- [ClassFile API (JEP 484)](https://openjdk.org/jeps/484)
- [ScopedValue API (JEP 444)](https://openjdk.org/jeps/444)
- [Virtual Threads (JEP 425)](https://openjdk.org/jeps/425)

### Related
- [Mansart Data Implementation](../mansart-jakarta-data/README.md)
- [Vidocq Ecosystem](https://vidocq.dev)
