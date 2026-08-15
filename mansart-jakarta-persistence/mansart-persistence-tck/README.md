# mansart-persistence-tck

Runner for the **official Jakarta Persistence 3.2 TCK** (`jakarta.persistence:persistence-tck:3.2.0`).

## Why this module is outside the main reactor

Module in standalone `modelVersion 4.0.0` (without `<parent>`). Reason documented in the workspace root `CLAUDE.md`: ShrinkWrap Maven Resolver 3.3 (transitive from the TCK) doesn't know how to parse Maven Model 4.1.0 POMs. As long as upstream ShrinkWrap doesn't support Model 4.1, this module remains detached.

## Status

| Component | Status |
| --- | --- |
| Standalone POM Model 4.0.0 + Arquillian/TestNG/JUnit5 deps + official TCK | Configured (M7-19) |
| `run-official-tck-persistence-3.2.sh` script | TODO (M7-19) |
| Mansart BCE (`mansart-persistence-cdi`) wires JPA in Vauban | TODO (M9-7) |
| Arquillian Vauban connector (ported from `vauban-tck-runner`) | TODO |
| Official TCK suite 3.2.0 — resolved from Maven Central | TODO (M7-20) |
| Runtime impl generation | TODO |

## TCK artifact installation

**Status**: The official Jakarta Persistence 3.2 TCK is **NOT available on Maven Central**. 

Two options to obtain it:

### Option 1: Use existing snapshot (limited)
The `jakarta.tck:persistence-tck-spec-tests:3.2.2-SNAPSHOT` artifact is available in local M2 from a previous build.
However, it appears to be incomplete (only contains entity classes and signature test).

### Option 2: Build from source (recommended for M7-22)
```bash
# Clone the official Jakarta Persistence repository (contains TCK)
git clone https://github.com/jakartaee/persistence.git
cd persistence

# Check out the TCK release tag
git checkout 3.2.1-TCK-RELEASE

# Build and install the TCK
mvn clean install -DskipTests

# The TCK artifacts will be installed to your local M2 automatically
```

**Note**: The snapshot currently in local M2 (`3.2.2-SNAPSHOT`) was built from the Eclipse EE4J fork, not the official Jakarta repository.
It may be incomplete. For full TCK coverage (400+ tests), build from the official source.

```xml
<dependency>
    <groupId>jakarta.tck</groupId>
    <artifactId>persistence-tck-dist</artifactId>
    <version>3.2.2-SNAPSHOT</version>
    <type>jar</type>
</dependency>
```

## Launch

```bash
# Smoke harness (Vauban + Mansart, default, without the TCK)
./run-official-tck-persistence-3.2.sh --smoke

# Official TCK EntityTests suite on H2 in-memory (default)
./run-official-tck-persistence-3.2.sh
# = mvn -Ptck-run test

# Official TCK EntityTests suite on PostgreSQL (Testcontainers)
./run-official-tck-persistence-3.2.sh --pg
# = mvn -Ptck-run,tck-pg test

# SignatureTests only (on H2)
./run-official-tck-persistence-3.2.sh --sig
# = mvn -Ptck-run,tck-sig test

# Full suite: EntityTests + SignatureTests (H2 by default, or PG=1 for PostgreSQL)
./run-official-tck-persistence-3.2.sh --full
PG=1 ./run-official-tck-persistence-3.2.sh --full

# Target a specific test
./run-official-tck-persistence-3.2.sh -Dtest=EntityTests#testFindAll
```

Prerequisites:
- **Java 25** + **Maven 3.9.16** (sub-project `.sdkmanrc`; `cd mansart-jakarta-persistence && sdk env`)
- `mansart-jakarta-persistence` built and installed (`mvn install -DskipTests` from `mansart-jakarta-persistence/` — the script does it automatically if needed)
- For `--pg`: **Docker** (Docker Desktop, OrbStack, colima…). Image `postgres:17-alpine` downloaded on first run.
- TCK 3.2.0 resolved automatically from Maven Central (`jakarta.persistence:persistence-tck:3.2.0`).

## PostgreSQL mode

The Maven profile `tck-pg` (combinable with `tck-run`) substitutes all DataSource wiring:

- Activates `PostgresDataSourceProducer` (Testcontainers `postgres:17-alpine`) instead of `H2DataSourceProducer`.
- Substitutes `mansart-data-dialect-postgresql` for `mansart-data-dialect-h2` in the ShrinkWrap deployment.
- The system property `mansart.tck.dialect=pg` is set by the profile and read by `MansartTckArchiveAppender`.

The PostgreSQL container is started once per JVM (static singleton, `Runtime.addShutdownHook` for cleanup). No manual setup.

## SignatureTests mode

The Maven profile `tck-sig` (combinable with `tck-run` and `tck-pg`) activates binary signature verification of all `jakarta.persistence.*` packages. The official runner (CTS heritage / sigtest):

- compares the API present on the deployment classpath to the canonical signature `jakarta.persistence.sig_21` shipped in TCK jar 3.2.0;
- requires a writable directory to cache JDK modules (automatically set to `target/jimage-cache`).

**Java 25-specific configuration** (already in the pom in the `tck-sig` profile):

- `--add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED` + `--add-opens` of the same package: the sigtest runner does `setAccessible` on internal JDK annotations (`@Stable`), refused by default on Java 25.
- `-Djava.specification.version=21`: TCK 3.2.0 only ships `jakarta.persistence.sig_17` and `jakarta.persistence.sig_21`; on Java 25 the runner looks for `jakarta.persistence.sig_25` (NPE). Forcing version to 21 is safe — Jakarta Persistence 3.2 was frozen under Java 21, the API hasn't evolved since.

## Harness architecture

```
┌──────────────────────────────────────────────┐
│  Official TCK Jakarta Persistence 3.2 (TestNG) │
│  ─ scenarios annotated @Entity, etc.         │
└──────────────────────┬───────────────────────┘
                       │ Arquillian deployment
                       ▼
┌──────────────────────────────────────────────┐
│  Vauban embedded (CDI 4.1)                   │
│  ─ discovers Mansart BCE                     │
│  ─ creates synthetic beans for each        │
│    scanned @Entity                           │
└──────────────────────┬───────────────────────┘
                       │ inject
                       ▼
┌──────────────────────────────────────────────┐
│  Mansart Persistence (mansart-persistence-core)│
│  → DialectFactory ServiceLoader               │
│  → H2 in-memory                              │
└──────────────────────────────────────────────┘
```
