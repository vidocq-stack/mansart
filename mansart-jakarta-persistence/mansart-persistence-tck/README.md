# mansart-persistence-tck

Runner for the **official Jakarta Persistence 3.2 TCK** (`jakarta.tck:persistence-tck-spec-tests:3.2.1`).

## Why this module is outside the main reactor

Module in standalone `modelVersion 4.0.0` (without `<parent>`). Reason documented in the workspace root `CLAUDE.md`: ShrinkWrap Maven Resolver 3.3 (transitive from the TCK) doesn't know how to parse Maven Model 4.1.0 POMs. As long as upstream ShrinkWrap doesn't support Model 4.1, this module remains detached.

## Status (as of 2026-08-25)

| Component | Status |
| --- | --- |
| Standalone POM Model 4.0.0 + Arquillian/TestNG deps + official TCK | ✅ JP-05 |
| `run-official-tck-persistence-3.2.sh` script | ✅ JP-05 |
| Mansart provider stub (`MansartPersistenceProvider`) | ✅ JP-03 |
| `persistence.xml` parsing (`PersistenceUnitReader`) | ✅ JP-04 |
| TCK harness resolves 269 client classes (~1 745 methods) | ✅ JP-05 |
| Schema from official DDL (PostgreSQL + H2) | ⏳ JP-06 |
| SignatureTests | ⏳ JP-07 |
| Baseline full-suite run | ⏳ JP-08 |

## TCK artifact installation

**Good news**: `jakarta.tck:persistence-tck-spec-tests:3.2.1` **is on Maven Central**. No manual install needed — the `-Ptck-run` profile downloads it automatically.

```xml
<dependency>
    <groupId>jakarta.tck</groupId>
    <artifactId>persistence-tck-spec-tests</artifactId>
    <version>3.2.1</version>
</dependency>
```

The TCK depends on `jakarta.tck:persistence-tck-common:3.2.1` for shared test base classes (`PMClientBase`) and common entities (schema30 package).

## Launch

```bash
# Official Jakarta Persistence 3.2 TCK spec-tests on H2 in-memory (default)
./run-official-tck-persistence-3.2.sh
# = mvn -Ptck-run test

# Run a single client (e.g. persistence-core annotations basic tests on H2)
./run-official-tck-persistence-3.2.sh -Dtest=AnnotationsBasicTest

# Official TCK spec-tests on PostgreSQL (Testcontainers)
./run-official-tck-persistence-3.2.sh --pg
# = mvn -Ptck-run,tck-pg test

# SignatureTests only (on H2)
./run-official-tck-persistence-3.2.sh --sig
# = mvn -Ptck-run,tck-sig test

# Full suite: spec-tests + SignatureTests (H2 by default, or PG=1 for PostgreSQL)
./run-official-tck-persistence-3.2.sh --full
PG=1 ./run-official-tck-persistence-3.2.sh --full
```

Prerequisites:
- **Java 25** + **Maven 3.9.16** (sub-project `.sdkmanrc`; `cd mansart-jakarta-persistence && sdk env`)
- `mansart-jakarta-persistence` built and installed (`mvn install -DskipTests` from `mansart-jakarta-persistence/` — the script does it automatically if needed)
- For `--pg`: **Docker** (Docker Desktop, OrbStack, colima…). Image `postgres:17-alpine` downloaded on first run.
- TCK 3.2.1 resolved automatically from Maven Central (`jakarta.tck:persistence-tck-spec-tests:3.2.1`).

## PostgreSQL mode

The Maven profile `tck-pg` (combinable with `tck-run`) substitutes all DataSource wiring:

- Activates `PostgresDataSourceProducer` (Testcontainers `postgres:17-alpine`) instead of `H2DataSourceProducer`.
- Substitutes `mansart-data-dialect-postgresql` for `mansart-data-dialect-h2` in the ShrinkWrap deployment.
- The system property `mansart.tck.dialect=pg` is set by the profile and read by the TCK archive appender.

The PostgreSQL container is started once per JVM (static singleton, `Runtime.addShutdownHook` for cleanup). No manual setup.

## SignatureTests mode

The Maven profile `tck-sig` (combinable with `tck-run` and `tck-pg`) activates binary signature verification of all `jakarta.persistence.*` packages. The official runner (CTS heritage / sigtest-maven-plugin):

- compares the API present on the deployment classpath to the canonical signature `jakarta.persistence.sig_25` shipped in the TCK jar;
- requires a writable directory to cache JDK modules (automatically set to `target/jimage-cache`).

**Java 25-specific configuration** (already in the pom in the `tck-sig` profile):

- `--add-exports java.base/jdk.internal.vm.annotation=ALL-UNNAMED` + `--add-opens` of the same package: the sigtest runner does `setAccessible` on internal JDK annotations (`@Stable`), refused by default on Java 25.
- `-Djava.specification.version=25`: Jakarta Persistence 3.2 is finalized under Java 25, so the signature file `jakarta.persistence.sig_25` should be present in the TCK distribution.

## Harness architecture

```
┌──────────────────────────────────────────────┐
│  Official TCK Jakarta Persistence 3.2 (TestNG)│
│  ─ scenarios annotated @Entity, @Id, etc.    │
└──────────────────────┬───────────────────────┘
                        │ Arquillian deployment
                        ▼
┌──────────────────────────────────────────────┐
│  Vauban embedded (CDI 4.1)                   │
│  ─ discovers Mansart BCE                     │
│  ─ creates synthetic beans for EntityManager  │
│    and EntityManagerFactory                   │
└──────────────────────┬───────────────────────┘
                        │ inject
                        ▼
┌──────────────────────────────────────────────┐
│  MansartPersistenceProvider (stub)           │
│  → EntityManagerFactory (throws UNSUPPORTED)  │
│  → JDBC / H2 in-memory                      │
└──────────────────────────────────────────────┘
```

## TCK scope

The `jakarta.tck:persistence-tck-spec-tests:3.2.1` TCK contains **269 `*Client` classes** across 16 packages (annotations, entitytest, metamodelapi, schemaGeneration, derivedid, relationship, override, callback, types, cache, inheritance, criteriaapi, template, pluggability), covering roughly **1 745 test methods**. The distribution is led by annotation coverage (25 clients in `core/annotations` alone), entity lifecycle tests (20 in `core/entitytest`), and the metamodel API (16 clients).

## Roadmap

- **JP-05** ✅ delivered — standalone POM + runner script + TCK resolution.
- **JP-06** ⏳ — TCK schema from official DDL (PostgreSQL first, then H2 translation).
- **JP-07** ⏳ — SignatureTests suite (Maven profile `tck-sig`).
- **JP-08** ⏳ — Record the M0 baseline (full-suite run, numbers written down).
