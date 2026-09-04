# Mansart Persistence TCK

Official Jakarta Persistence 3.2 TCK runner for Mansart implementation.

## Overview

This module runs the **official Jakarta Persistence 3.2 TCK** (Technology Compatibility Kit) against the Mansart Persistence implementation. It is a **standalone module** (out-of-reactor) with its own POM 4.0.0.

## Structure

```
mansart-persistence-tck/
├── pom.xml                              ← Standalone POM 4.0.0, no parent
├── run-official-tck-persistence-3.2.sh ← Runner script
└── README.md                            ← This file
```

## Prerequisites

- Java 25
- Maven 3.9.x
- All Mansart persistence modules installed (`mvn install` from parent)
- H2 database driver (for `tck-run` profile)
- PostgreSQL JDBC driver (for `tck-pg` profile)

## Quick Start

### 1. Install all Mansart modules

```bash
cd mansart-jakarta-persistence
mvn clean install -DskipTests
```

### 2. Run TCK with H2 (default)

```bash
cd mansart-persistence-tck
./run-official-tck-persistence-3.2.sh
```

### 3. Run TCK with PostgreSQL

```bash
./run-official-tck-persistence-3.2.sh tck-pg
```

### 4. Run Signature Tests only

```bash
./run-official-tck-persistence-3.2.sh tck-sig
```

## Profiles

| Profile | Database | Description |
|---------|----------|-------------|
| `tck-run` | H2 in-memory | Default profile, runs all TCK tests |
| `tck-pg` | PostgreSQL | Runs TCK against PostgreSQL |
| `tck-sig` | - | Runs signature tests only |

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `TCK_HOME` | `target/tck/jakarta-persistence-tck-3.2.0` | Path to extracted TCK distribution |
| `TCK_VERSION` | `3.2.0` | TCK version to download |
| `MVN_OPTS` | `-q` | Maven command line options |

### Database Configuration

**H2 (tck-run profile):**
```properties
tck.db=h2
tck.db.url=jdbc:h2:mem:test;DB_CLOSE_DELAY=-1
tck.db.user=sa
tck.db.password=
```

**PostgreSQL (tck-pg profile):**
```properties
tck.db=postgresql
tck.db.url=jdbc:postgresql://localhost:5432/test
tck.db.user=test
tck.db.password=test
```

## TCK Results

The TCK contains **269 client classes** with approximately **1,745 test methods**.

### Baseline Goal (M0)

Establish a trustworthy baseline with **0/X/1745** where:
- **0** = pass count (stub implementation)
- **X** = error count (should be mostly `setup*Data` errors)
- **1745** = total test count

All failures should be in **setup/data** or **provider wiring**, not harness failures.

### Expected Progression

| Milestone | Target | Description |
|-----------|--------|-------------|
| M0 | 0/X/1745 | Baseline with stubs |
| M1-M3 | N/N/1745 | Entity metamodel + code generation |
| M4 | N/N/1745 | Core runtime (EMF + EM) |
| M5-M8 | N/N/1745 | ORM, Query, Lifecycle |
| M20 | 1745/1745 | **100% PASS** |

## Provider Registration

The Mansart provider is registered via ServiceLoader:

**File:** `mansart-persistence-core/src/main/resources/META-INF/services/jakarta.persistence.spi.PersistenceProvider`

**Content:**
```
io.vidocq.mansart.persistence.core.MansartPersistenceProvider
```

## TCK Classpath

The TCK requires the following on its classpath:

1. **Jakarta Persistence API** (`jakarta.persistence-api:3.2.0`)
2. **Mansart Persistence Core** (`mansart-persistence-core`)
3. **Mansart Persistence SPI** (`mansart-persistence-spi`)
4. **Mansart Persistence CDI** (`mansart-persistence-cdi`)
5. **Mansart Data Dialect SPI** (`mansart-data-dialect-spi`)
6. **Mansart Data Dialect H2** (`mansart-data-dialect-h2`)
7. **Mansart Transactions Core** (`mansart-transactions-core`)
8. **Database Driver** (H2 or PostgreSQL)
9. **JUnit 5** (for test execution)

## Troubleshooting

### "Provider not found" errors

Ensure the service file exists and is in the correct location:
```bash
cat mansart-persistence-core/src/main/resources/META-INF/services/jakarta.persistence.spi.PersistenceProvider
```

Should output:
```
io.vidocq.mansart.persistence.core.MansartPersistenceProvider
```

### "Class not found" errors

Verify all modules are installed:
```bash
cd mansart-jakarta-persistence
mvn clean install -DskipTests -pl "!mansart-persistence-maven-plugin,!mansart-persistence-tests,!mansart-persistence-external-it"
```

### TCK ZIP not found

The TCK distribution needs to be downloaded. Run:
```bash
cd mansart-persistence-tck
mvn dependency:unpack-dependencies
```

This will download and extract the TCK ZIP to `target/tck/`.

## Building

```bash
cd mansart-persistence-tck
mvn clean compile
```

## Clean

```bash
mvn clean
```

## References

- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specs/persistence/3.2/)
- [Jakarta Persistence TCK](https://github.com/eclipse-ee4j/jakartaee-tck/tree/master/persistence)
- [Mansart Persistence Core](../mansart-persistence-core)
- [Mansart Persistence SPI](../mansart-persistence-spi)
