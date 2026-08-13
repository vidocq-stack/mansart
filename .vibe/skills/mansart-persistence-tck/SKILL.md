---
name: mansart-persistence-tck
description: |
  Guidance for executing and debugging the Jakarta Persistence 3.2 TCK for Mansart.
  Load this skill when running TCK tests, analyzing failures, or setting up TCK environment.
---

# Mansart Jakarta Persistence 3.2 TCK Skill

You are working with the **Jakarta Persistence 3.2 Technology Compatibility Kit (TCK)**
for the Mansart implementation. This skill provides guidance for TCK setup, execution,
failure analysis, and compliance tracking.

## TCK Overview

### What is the TCK?

The **Jakarta Persistence TCK 3.2.2** is the official compliance test suite for Jakarta Persistence 3.2.
- **Not available on Maven Central** - must be built from source
- **Version**: 3.2.2-SNAPSHOT (as of August 2026)
- **Repository**: [jakartaee/persistence](https://github.com/jakartaee/persistence/tree/main/tck)
- **Test Framework**: TestNG + Arquillian
- **Scope**: 1000+ tests covering all JPA 3.2 features

### TCK Structure

```
jakartaee/persistence/tck/
├── pom.xml                    # TCK parent POM
├── tck-parent/               # Parent for all TCK modules
├── tck-common/               # Common utilities and base classes
├── tck-spec-tests/           # Specification compliance tests
│   ├── src/test/java/
│   │   └── com/sun/ts/tests/jpa21/  # Test classes
│   │       ├── core/        # Core JPA functionality
│   │       ├── ee/          # Enterprise features
│   │       ├── persistence/ # Persistence context tests
│   │       ├── query/       # JPQL and Criteria tests
│   │       ├── relationships/ # Entity relationships
│   │       ├── xml/         # XML mapping tests
│   │       └── ...
├── tck-dbprocedures/         # Database procedures tests (optional)
└── tck-cdi/                  # CDI integration tests
```

### TCK Test Categories

| Category | Description | Approx. Tests |
|----------|-------------|--------------|
| **Core** | Basic JPA operations (persist, merge, remove, find) | 200+ |
| **Persistence Context** | PC lifecycle, propagation, types | 100+ |
| **Entity Mappings** | @Entity, @Table, @Id, @GeneratedValue, etc. | 150+ |
| **Relationships** | @OneToMany, @ManyToOne, @OneToOne, @ManyToMany | 200+ |
| **Inheritance** | SINGLE_TABLE, JOINED, TABLE_PER_CLASS | 50+ |
| **JPQL** | Query language parsing and execution | 150+ |
| **Criteria API** | Type-safe query construction | 100+ |
| **Named Queries** | @NamedQuery, @NamedNativeQuery | 50+ |
| **Native Queries** | SQL queries | 50+ |
| **Lifecycle Callbacks** | @PrePersist, @PostLoad, etc. | 30+ |
| **Listeners** | Entity listeners | 20+ |
| **Locking** | Optimistic and pessimistic locking | 30+ |
| **Caching** | L1 and L2 cache | 30+ |
| **Transactions** | Transaction management | 50+ |
| **Validation** | Bean validation integration | 20+ |

## TCK Setup

### Step 1: Clone and Build TCK

The TCK must be built and installed in your local Maven repository before running.

```bash
# From mansart-jakarta-persistence/mansart-persistence-tck/
cd mansart-jakarta-persistence/mansart-persistence-tck

# Run setup script (recommended)
./setup-tck.sh

# Or manually:
git clone https://github.com/jakartaee/persistence.git tck-upstream
cd tck-upstream/tck
mvn clean install -DskipTests
```

**What setup-tck.sh does:**
1. Clones the jakartaee/persistence repository to `tck-upstream/`
2. Checks out the main branch
3. Builds the TCK with `mvn clean install -DskipTests`
4. Installs artifacts to `~/.m2/repository/jakarta/tck/`

### Step 2: Verify Installation

```bash
# Check TCK artifacts are installed
ls ~/.m2/repository/jakarta/tck/persistence-tck-*/

# Should see:
# persistence-tck-parent/
# persistence-tck-common/
# persistence-tck-spec-tests/
# persistence-tck-dbprocedures/

# Check versions
mvn dependency:tree -pl mansart-persistence-tck | grep jakarta.tck
```

### Step 3: Verify TCK Version

```bash
# In mansart-persistence-tck/pom.xml, check:
<dependency>
    <groupId>jakarta.tck</groupId>
    <artifactId>persistence-tck-spec-tests</artifactId>
    <version>3.2.2-SNAPSHOT</version>  # Must match built version
    <scope>test</scope>
</dependency>
```

## TCK Execution

### Quick Start

```bash
# From mansart-jakarta-persistence/

# Smoke test (default) - verifies basic integration
mvn -pl mansart-persistence-tck test

# Full TCK on H2 (in-memory)
mvn -pl mansart-persistence-tck -Ptck test

# Full TCK on PostgreSQL
mvn -pl mansart-persistence-tck -Ptck,pgsql test
```

### Maven Profiles

| Profile | Description | Activation |
|---------|-------------|------------|
| `smoke` | Default. Runs only smoke tests (1-2 tests) | Active by default |
| `tck` | Runs full TCK suite | `-Ptck` |
| `pgsql` | Configures PostgreSQL instead of H2 | `-Ptck,pgsql` |

### Configuration Properties

**Database Configuration:**
```bash
# H2 (default)
-Dtck.db.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
-Dtck.db.user=sa
-Dtck.db.password=""
-Dtck.db.driver=org.h2.Driver

# PostgreSQL
-Dtck.db.url=jdbc:postgresql://localhost:5432/tck_db
-Dtck.db.user=tck_user
-Dtck.db.password=tck_password
-Dtck.db.driver=org.postgresql.Driver

# MySQL
-Dtck.db.url=jdbc:mysql://localhost:3306/tck_db
-Dtck.db.user=tck_user
-Dtck.db.password=tck_password
-Dtck.db.driver=com.mysql.cj.jdbc.Driver
```

**Persistence Provider Configuration:**
```bash
-Dmansart.provider=io.vidocq.mansart.persistence.core.bootstrap.MansartPersistenceProvider
-Djakarta.persistence.provider=io.vidocq.mansart.persistence.core.bootstrap.MansartPersistenceProvider
-Djavax.persistence.provider=io.vidocq.mansart.persistence.core.bootstrap.MansartPersistenceProvider
```

**TCK Configuration:**
```bash
-Dtck.concurrent=true              # Enable concurrent test execution
-Dplatform.mode=standalone        # Run in standalone mode (not in container)
-Dpersistence.unit.name=JPATCK    # Default persistence unit name
-Dpersistence.unit.name.2=JPATCK2 # Second persistence unit
-Dvehicle=standalone              # Test vehicle
-Dpersistence.second.level.caching.supported=false  # L2 cache not yet supported
```

### Common Commands

```bash
# Run full TCK with H2
mvn -pl mansart-persistence-tck -Ptck test

# Run full TCK with PostgreSQL
mvn -pl mansart-persistence-tck -Ptck,pgsql test

# Run specific test class
mvn -pl mansart-persistence-tck -Ptck test -Dtest=EntityTest

# Run specific test method
mvn -pl mansart-persistence-tck -Ptck test -Dtest=EntityTest#testPersist

# Run tests matching pattern
mvn -pl mansart-persistence-tck -Ptck test -Dtest="*EntityTest"

# Run with custom database
mvn -pl mansart-persistence-tck -Ptck test \
    -Dtck.db.url=jdbc:h2:mem:custom \
    -Dtck.db.user=sa \
    -Dtck.db.password=""

# Run with debugging enabled
mvn -pl mansart-persistence-tck -Ptck test \
    -Dmansart.sql.log=true \
    -Dmansart.jpql.debug=true

# Generate test report
mvn -pl mansart-persistence-tck -Ptck test \
    -Dsurfire.reports.directory=target/tck-reports
```

## TCK Test Structure

### How TCK Tests Work

1. **Test Class**: Extends `com.sun.ts.tests.jpa21.common.JPATestBase`
2. **Setup**: Creates persistence.xml and deploys to container
3. **Execution**: Runs test methods via TestNG
4. **Cleanup**: Removes test data after each test

### Key TCK Base Classes

| Class | Purpose |
|-------|---------|
| `JPATestBase` | Base class for all JPA tests |
| `PersistenceTestBase` | Tests persistence.xml configuration |
| `EntityTest` | Tests entity mapping and CRUD |
| `QueryTest` | Tests JPQL queries |
| `RelationshipTest` | Tests entity relationships |
| `InheritanceTest` | Tests inheritance strategies |

### persistence.xml Configuration

The TCK uses `META-INF/persistence.xml` files packaged in the test JAR. Mansart provides:

**File:** `mansart-persistence-tck/src/test/resources/META-INF/persistence.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="https://jakarta.ee/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence
                 https://jakarta.ee/xml/ns/persistence/persistence_3_2.xsd"
             version="3.2">
    <persistence-unit name="JPATCK" transaction-type="RESOURCE_LOCAL">
        <provider>io.vidocq.mansart.persistence.core.bootstrap.MansartPersistenceProvider</provider>
        <class>com.sun.ts.tests.jpa21.core.ee.assignable.GetEntityMBeanTest$AssignableMBeanEntity</class>
        <!-- Hundreds more entity classes -->
        <properties>
            <property name="jakarta.persistence.jdbc.url" value="${tck.db.url}"/>
            <property name="jakarta.persistence.jdbc.user" value="${tck.db.user}"/>
            <property name="jakarta.persistence.jdbc.password" value="${tck.db.password}"/>
            <property name="jakarta.persistence.jdbc.driver" value="${tck.db.driver}"/>
            <property name="jakarta.persistence.schema-generation.database.action" value="drop-and-create"/>
            <property name="jakarta.persistence.schema-generation.create-database-schemas" value="true"/>
        </properties>
    </persistence-unit>
</persistence>
```

**Note:** The TCK generates its own persistence.xml files dynamically. The above is just a reference.

## Analyzing TCK Failures

### Understanding TCK Output

```
===============================================
Standalone Tests
Total tests run: 1248, Passes: 1200, Failures: 48, Skips: 0
===============================================

Failed tests:
  testPersistEntityWithGeneratedId(com.sun.ts.tests.jpa21.core.ee.JPATest)
    Expected: entity with generated ID
    Actual: javax.persistence.PersistenceException: Unable to persist entity

  testFindEntityById(com.sun.ts.tests.jpa21.persistence.EntityManagerTest)
    Expected: entity found by ID
    Actual: javax.persistence.EntityNotFoundException: Unable to find entity
```

### Common Failure Patterns

#### 1. Entity Mapping Issues

**Symptoms:**
- `IllegalArgumentException: Entity class not found`
- `PersistenceException: Unable to map entity`
- `MappingException: @Id not found`

**Root Causes:**
- APT processor not running on TCK entity classes
- Entity class not enhanced
- Missing annotations in generated metamodel

**Debug Steps:**
```bash
# Check if APT processor ran
find mansart-persistence-tck/target/classes -name "*_Entity.class" | head

# Check processor logs
mvn -pl mansart-persistence-tck -Ptck test -Dmansart.apt.debug=true

# Check generated sources
ls -la mansart-persistence-tck/target/generated-sources/annotations/
```

**Solutions:**
- Ensure processor is on annotation processor path in pom.xml
- Add `<annotationProcessorPaths>` to maven-compiler-plugin
- Check processor doesn't skip TCK classes

#### 2. Persistence Provider Not Found

**Symptoms:**
- `No Persistence provider for EntityManager`
- `No PersistenceProvider found`

**Root Causes:**
- Service file missing: `META-INF/services/jakarta.persistence.spi.PersistenceProvider`
- Provider class not in classpath
- Wrong provider class name

**Debug Steps:**
```bash
# Check service file exists in mansart-persistence-core
find mansart-persistence-core -path "*/META-INF/services/*" -type f

# Check service file content
cat mansart-persistence-core/src/main/resources/META-INF/services/jakarta.persistence.spi.PersistenceProvider

# Verify provider class exists
find mansart-persistence-core -name "MansartPersistenceProvider.class"
```

**Solutions:**
- Ensure service file exists and contains correct class name
- Ensure provider class is public and has no-arg constructor
- Check provider implements `jakarta.persistence.spi.PersistenceProvider`

#### 3. SQL Syntax Errors

**Symptoms:**
- `SQLSyntaxErrorException: syntax error`
- `H2DatabaseException: Column not found`
- `PSQLException: ERROR: syntax error`

**Root Causes:**
- JPQL to SQL conversion error
- Missing dialect support for SQL feature
- Wrong table/column names

**Debug Steps:**
```bash
# Enable SQL logging
mvn -pl mansart-persistence-tck -Ptck test \
    -Dmansart.sql.log=true \
    -Dmansart.sql.log.level=DEBUG \
    -Dtest=SpecificFailingTest

# Check JPQL parsing
mvn -pl mansart-persistence-tck -Ptck test \
    -Dmansart.jpql.debug=true \
    -Dtest=SpecificFailingTest
```

**Solutions:**
- Check `JpqlToRuntimeConverter` for the failing JPQL
- Update dialect implementation (H2Dialect, PostgresqlDialect)
- Add missing SQL feature support

#### 4. Lazy Loading / Proxy Issues

**Symptoms:**
- `LazyInitializationException`
- `NullPointerException` when accessing lazy field
- Proxy class not found

**Root Causes:**
- Proxy not generated by APT processor
- ScopedValue not set in virtual thread context
- Entity not enhanced for lazy loading

**Debug Steps:**
```bash
# Check proxy generation
find mansart-persistence-tck/target/classes -name "*$*Proxy*.class" | head

# Enable enhancement debugging
mvn -pl mansart-persistence-tck -Ptck test \
    -Dmansart.enhancement.debug=true \
    -Dtest=SpecificFailingTest
```

**Solutions:**
- Verify APT processor generates proxies for `@ManyToOne`, `@OneToOne`
- Check ScopedValue usage in EntityManager
- Ensure proxy classes are in same package as entity

#### 5. Transaction Issues

**Symptoms:**
- `TransactionRequiredException`
- `IllegalStateException: No active transaction`
- Transaction not committed/rolled back

**Root Causes:**
- Missing transaction management
- Wrong transaction type (RESOURCE_LOCAL vs JTA)
- Transaction not started before persist/merge

**Debug Steps:**
```bash
# Check transaction management
mvn -pl mansart-persistence-tck -Ptck test \
    -Dmansart.transaction.debug=true \
    -Dtest=SpecificFailingTest

# Verify TCK persistence.xml has correct transaction type
# Most TCK tests use RESOURCE_LOCAL
```

**Solutions:**
- Ensure `MansartEntityTransaction` properly manages transactions
- Check transaction type in persistence.xml matches provider
- Verify `begin()`, `commit()`, `rollback()` are called correctly

#### 6. Cache Issues

**Symptoms:**
- `EntityNotFoundException` after persist
- Duplicate inserts
- Stale data returned

**Root Causes:**
- L1 cache not working
- Entity not added to cache on persist
- Cache invalidation not working

**Debug Steps:**
```bash
# Enable cache debugging
mvn -pl mansart-persistence-tck -Ptck test \
    -Dmansart.cache.debug=true \
    -Dtest=SpecificFailingTest
```

**Solutions:**
- Check `EntityCache` implementation in mansart-persistence-core
- Verify cache key generation
- Ensure cache is cleared on clear() and evict()

#### 7. Query / JPQL Issues

**Symptoms:**
- `IllegalArgumentException: Invalid JPQL`
- Wrong results from query
- Query timeout

**Root Causes:**
- JPQL parser not handling syntax
- Parameter binding not working
- Result mapping incorrect

**Debug Steps:**
```bash
# Enable JPQL debugging
mvn -pl mansart-persistence-tck -Ptck test \
    -Dmansart.jpql.debug=true \
    -Dmansart.sql.log=true \
    -Dtest=SpecificQueryTest

# Check which JPQL is failing (look at test source)
```

**Solutions:**
- Implement missing JPQL feature in `JpqlParser` and `JpqlToRuntimeConverter`
- Check parameter binding in `QueryExecutionContext`
- Verify result mapping in `RowMapper`

### Failure Analysis Workflow

1. **Identify the failing test(s)**
   ```bash
   # Run TCK and save report
   mvn -pl mansart-persistence-tck -Ptck test > tck-output.txt 2>&1
   
   # Find failing tests
   grep -E "(FAILED|ERROR)" tck-output.txt
   ```

2. **Run single failing test**
   ```bash
   mvn -pl mansart-persistence-tck -Ptck test -Dtest=FailingTestClass
   ```

3. **Check test source code**
   - TCK tests are in `~/.m2/repository/jakarta/tck/persistence-tck-spec-tests/.../sources/`
   - Or clone TCK repo and check: `tck-upstream/tck/tck-spec-tests/src/test/java/`

4. **Enable all debugging**
   ```bash
   mvn -pl mansart-persistence-tck -Ptck test \
       -Dtest=FailingTestClass \
       -Dmansart.debug=true \
       -Dmansart.sql.log=true \
       -Dmansart.jpql.debug=true \
       -Dmansart.apt.debug=true \
       -Dmansart.enhancement.debug=true \
       -Dmansart.transaction.debug=true \
       -Dmansart.cache.debug=true
   ```

5. **Analyze stack trace**
   - Look for Mansart classes in stack trace
   - Identify which component is failing
   - Check if it's a Mansart bug or TCK configuration issue

6. **Check TCK requirements**
   - Read JPA spec section that the test is testing
   - Check if feature is implemented in Mansart
   - Check [M7-PLAN.md](../mansart-jakarta-persistence/M7-PLAN.md) for planned features

## Current TCK Status

### As of August 5, 2026 (M6)

**Overall Status**: TCK not yet passing - implementation in progress

### Implemented Features (TCK should pass)

| Feature | Status | TCK Coverage |
|---------|--------|--------------|
| Basic entity mapping (@Entity, @Id, @Table) | ✅ | ~50 tests |
| CRUD operations (persist, merge, remove, find) | ✅ | ~100 tests |
| Basic JPQL (SELECT, WHERE, simple predicates) | ✅ | ~30 tests |
| Simple entity relationships | ✅ | ~40 tests |
| L1 cache | ✅ | ~20 tests |
| Transaction management (RESOURCE_LOCAL) | ✅ | ~30 tests |

### Missing Features (TCK will fail)

| Feature | Status | Priority | Impact |
|---------|--------|----------|--------|
| GROUP BY and HAVING | ❌ Not implemented | Critical (M6) | High |
| JOIN syntax (explicit) | ❌ Partial | Critical (M6) | High |
| Subqueries in FROM | ❌ Not implemented | High (M6) | High |
| ALL/ANY/SOME predicates | ❌ Not implemented | High (M6) | Medium |
| Additional JPQL functions | ❌ Partial | Medium (M6) | Medium |
| Inheritance strategies | ❌ Partial | Medium (M7) | Medium |
| L2 cache | ❌ Not implemented | Low (M7) | Low |
| Named queries | ❌ Partial | Medium (M7) | Medium |
| Criteria API | ❌ Not started | Low (M8) | Low |
| Bean validation integration | ❌ Not started | Low (M8) | Low |

### Expected Failures by Category

| Category | Expected Failures | Reason |
|----------|-------------------|--------|
| JPQL | 100+ | Missing advanced features |
| Query | 50+ | Criteria API not implemented |
| Relationships | 20+ | Some relationship types not fully supported |
| Inheritance | 50+ | Inheritance strategies incomplete |
| Caching | 10+ | L2 cache not implemented |
| Validation | 20+ | Bean validation not integrated |

## Tracking TCK Progress

### Run TCK and Save Results

```bash
# Create TCK report directory
mkdir -p mansart-jakarta-persistence/tck-reports

# Run TCK and save output
mvn -pl mansart-persistence-tck -Ptck test \
    > mansart-jakarta-persistence/tck-reports/$(date +%Y-%m-%d)-tck-output.txt \
    2>&1

# Extract summary
TODAY=$(date +%Y-%m-%d)
mvn -pl mansart-persistence-tck -Ptck test 2>&1 | \
    grep -E "(Total tests run|Passes|Failures|Skips)" \
    > mansart-jakarta-persistence/tck-reports/${TODAY}-summary.txt

# Extract failing tests
grep -A 5 "Failed tests:" mansart-jakarta-persistence/tck-reports/${TODAY}-tck-output.txt \
    > mansart-jakarta-persistence/tck-reports/${TODAY}-failures.txt
```

### Compare with Previous Run

```bash
# Compare today's results with yesterday's
diff mansart-jakarta-persistence/tck-reports/$(date -d "yesterday" +%Y-%m-%d)-summary.txt \
     mansart-jakarta-persistence/tck-reports/$(date +%Y-%m-%d)-summary.txt

# Count failures over time
for file in mansart-jakarta-persistence/tck-reports/*-summary.txt; do
    echo "$(basename $file): $(grep 'Failures:' $file | awk '{print $2}')"
done
```

### Update TCK.md Documentation

After each TCK run, update the TCK status documentation:

```bash
# In mansart-jakarta-persistence/TCK.md (create if doesn't exist)
# Update with current pass/fail counts and analysis
```

**Template for TCK.md:**
```markdown
# Mansart Jakarta Persistence 3.2 :: TCK Conformance Report

**Date**: YYYY-MM-DD  
**Milestone**: M6/M7  
**Status**: IN PROGRESS / VALIDATED  

## Executive Summary

Mansart passes **X/1248** Jakarta Persistence 3.2 TCK tests.

| Category | Tests | Pass | Fail | % Pass |
|----------|--------|------|------|--------|
| Core | 200 | X | Y | Z% |
| Persistence Context | 100 | X | Y | Z% |
| Entity Mappings | 150 | X | Y | Z% |
| Relationships | 200 | X | Y | Z% |
| JPQL | 150 | X | Y | Z% |
| Criteria API | 100 | X | Y | Z% |
| Inheritance | 50 | X | Y | Z% |
| **Total** | **1248** | **X** | **Y** | **Z%** |

## Top 10 Failing Tests

1. testXxx (Category) - Reason
2. testYyy (Category) - Reason
...

## Recent Progress

- [YYYY-MM-DD] Fixed Z tests in Category (details)
- [YYYY-MM-DD] Implemented Feature (details)
...
```

## TCK Tips and Tricks

### Speed Up TCK Execution

```bash
# Run only specific test groups
mvn -pl mansart-persistence-tck -Ptck test -Dgroups=core
mvn -pl mansart-persistence-tck -Ptck test -Dgroups=query
mvn -pl mansart-persistence-tck -Ptck test -Dgroups=relationships

# Run TCK in parallel
mvn -pl mansart-persistence-tck -Ptck test -DthreadCount=4

# Skip slow tests (temporarily)
mvn -pl mansart-persistence-tck -Ptck test -DexcludeGroups=slow
```

### Debug Specific Test

```bash
# Find test in TCK source
grep -r "testPersistEntityWithGeneratedId" tck-upstream/tck/tck-spec-tests/src/

# Run with full debugging
mvn -pl mansart-persistence-tck -Ptck test \
    -Dtest=com.sun.ts.tests.jpa21.core.ee.JPATest#testPersistEntityWithGeneratedId \
    -Dmansart.debug=true

# Attach remote debugger
mvn -pl mansart-persistence-tck -Ptck test \
    -Dmaven.surefire.debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"
# Then attach with IDE to port 5005
```

### Bypass TCK for Development

```bash
# Run your own test that mimics TCK behavior
# In mansart-persistence-tests/src/test/java/

@Entity
public class MyTestEntity {
    @Id @GeneratedValue
    private Long id;
    private String name;
    // getters, setters
}

@ExtendWith(...)
public class MyTckLikeTest {
    @Test
    public void testLikeTck() {
        Map<String, Object> props = new HashMap<>();
        props.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:test");
        
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test", props);
        EntityManager em = emf.createEntityManager();
        
        // Test logic here
        MyTestEntity entity = new MyTestEntity();
        entity.setName("test");
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        em.close();
        emf.close();
    }
}
```

### Custom persistence.xml for Testing

```xml
<!-- mansart-persistence-tests/src/test/resources/META-INF/persistence.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="https://jakarta.ee/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence
                 https://jakarta.ee/xml/ns/persistence/persistence_3_2.xsd"
             version="3.2">
    <persistence-unit name="test" transaction-type="RESOURCE_LOCAL">
        <provider>io.vidocq.mansart.persistence.core.bootstrap.MansartPersistenceProvider</provider>
        <class>com.example.MyTestEntity</class>
        <properties>
            <property name="jakarta.persistence.jdbc.url" value="jdbc:h2:mem:test"/>
            <property name="jakarta.persistence.jdbc.user" value="sa"/>
            <property name="jakarta.persistence.jdbc.password" value=""/>
            <property name="jakarta.persistence.jdbc.driver" value="org.h2.Driver"/>
            <property name="jakarta.persistence.schema-generation.database.action" value="drop-and-create"/>
        </properties>
    </persistence-unit>
</persistence>
```

## TCK Configuration Reference

### pom.xml Configuration

**Key configuration in mansart-persistence-tck/pom.xml:**

```xml
<properties>
    <tck.version>3.2.2-SNAPSHOT</tck.version>
    <tck.db.url>jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1</tck.db.url>
    <tck.db.user>sa</tck.db.user>
    <tck.db.password></tck.db.password>
    <tck.db.driver>org.h2.Driver</tck.db.driver>
    <mansart.provider>io.vidocq.mansart.persistence.core.bootstrap.MansartPersistenceProvider</mansart.provider>
</properties>

<dependencies>
    <!-- TCK dependencies -->
    <dependency>
        <groupId>jakarta.tck</groupId>
        <artifactId>persistence-tck-common</artifactId>
        <version>${tck.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>jakarta.tck</groupId>
        <artifactId>persistence-tck-spec-tests</artifactId>
        <version>${tck.version}</version>
        <scope>test</scope>
    </dependency>
    
    <!-- Mansart dependencies -->
    <dependency>
        <groupId>io.vidocq.mansart</groupId>
        <artifactId>mansart-persistence-api</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>io.vidocq.mansart</groupId>
        <artifactId>mansart-persistence-core</artifactId>
        <version>${project.version}</version>
    </dependency>
    <dependency>
        <groupId>io.vidocq.mansart</groupId>
        <artifactId>mansart-persistence-cdi</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <!-- Database drivers -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
</dependencies>
```

### surefire-plugin Configuration for TCK

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.3.1</version>
    <configuration>
        <systemPropertyVariables>
            <jakarta.persistence.provider>${mansart.provider}</jakarta.persistence.provider>
            <javax.persistence.provider>${mansart.provider}</javax.persistence.provider>
            <javax.persistence.jdbc.url>${tck.db.url}</javax.persistence.jdbc.url>
            <javax.persistence.jdbc.user>${tck.db.user}</javax.persistence.jdbc.user>
            <javax.persistence.jdbc.password>${tck.db.password}</javax.persistence.jdbc.password>
            <javax.persistence.jdbc.driver>${tck.db.driver}</javax.persistence.jdbc.driver>
            <persistence.provider>${mansart.provider}</persistence.provider>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

### TCK Profile Configuration

```xml
<profile>
    <id>tck</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <forkCount>0</forkCount>
                    <includes>
                        <include>**/*Client*.*</include>
                        <include>**/*JPASigTest*.*</include>
                    </includes>
                    <excludes>
                        <exclude>**/SigTest*.java</exclude>
                        <exclude>**/signaturetest/*.java</exclude>
                    </excludes>
                    <dependenciesToScan>
                        <dependency>jakarta.tck:persistence-tck-spec-tests</dependency>
                        <dependency>jakarta.tck:persistence-tck-common</dependency>
                    </dependenciesToScan>
                    <systemPropertyVariables>
                        <!-- All TCK properties here -->
                        <tck.concurrent>true</tck.concurrent>
                        <platform.mode>standalone</platform.mode>
                        <persistence.unit.name>JPATCK</persistence.unit.name>
                        <persistence.unit.name.2>JPATCK2</persistence.unit.name.2>
                        <vehicle>standalone</vehicle>
                        <persistence.second.level.caching.supported>false</persistence.second.level.caching.supported>
                    </systemPropertyVariables>
                    <trimStackTrace>false</trimStackTrace>
                    <redirectTestOutputToFile>false</redirectTestOutputToFile>
                </configuration>
            </plugin>
        </plugins>
    </build>
    <dependencies>
        <dependency>
            <groupId>org.testng</groupId>
            <artifactId>testng</artifactId>
            <version>7.10.2</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</profile>
```

## References

### Jakarta Persistence TCK
- [GitHub Repository](https://github.com/jakartaee/persistence/tree/main/tck)
- [TCK Documentation](https://github.com/jakartaee/persistence/blob/main/tck/README.md)
- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/)

### Mansart Persistence
- [README.md](../mansart-jakarta-persistence/README.md)
- [AGENTS.md](../mansart-jakarta-persistence/AGENTS.md)
- [M7-PLAN.md](../mansart-jakarta-persistence/M7-PLAN.md)
- [Current Implementation Status](../mansart-jakarta-persistence/README.md#current-status)

### Testing
- [TestNG Documentation](https://testng.org/doc/documentation-main.html)
- [Arquillian Documentation](https://arquillian.org/)
- [AssertJ Documentation](https://assertj.github.io/doc/)

### Troubleshooting
- [Mansart Debugging Guide](../mansart-jakarta-persistence/AGENTS.md#debugging-tck-failures)
- [Common TCK Issues](#common-failure-patterns)
