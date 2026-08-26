---
name: mansart-jpa-tck
description: How to run, read and debug the official Jakarta Persistence 3.2 TCK against mansart. Load before any TCK work.
---

# Jakarta Persistence 3.2 TCK

The TCK is the only progress metric for this sub-project.

## Scale (measured, 2026-08-25)

`jakarta.tck:persistence-tck-spec-tests:3.2.1` in the local M2 contains
**269 `*Client` classes**, roughly 1 745 test methods. Distribution of the
clients, largest first:

| package | clients |
| --- | --- |
| `core/annotations` | 25 |
| `core/entitytest` | 20 |
| `core/metamodelapi` | 16 |
| `se/schemaGeneration` | 14 |
| `core/derivedid` | 12 |
| `core/relationship` | 11 |
| `core/override` | 10 |
| `jpa22/repeatable` | 7 |
| `core/callback` | 6 |
| `se/cache`, `core/types` | 5 each |
| `core/inheritance` | 4 |
| `core/criteriaapi` | 3 |
| everything else | 1–2 each |

Available artifacts in `~/.m2/repository/jakarta/tck/`:
`persistence-tck-dist` (3.2.1, 3.2.2-SNAPSHOT), `persistence-tck-spec-tests`
(3.2.0-SNAPSHOT, 3.2.1, 3.2.2-SNAPSHOT, 4.0.0-SNAPSHOT), `persistence-tck-common`,
`dbprocedures`, `sigtest-maven-plugin`. Pin **3.2.1**; the SNAPSHOTs move.

## Runner

`mansart-jakarta-persistence/mansart-persistence-tck/` is **out of the reactor**:
standalone `pom.xml`, `modelVersion 4.0.0`, no `<parent>`. Same rule as
`mansart-data-tck` and `mansart-transactions-tck`.

Copy the shape of
`mansart-jakarta-data/mansart-data-tck/run-official-tck-data-1.0.sh`:
`sdk env` from `../.sdkmanrc`, verify the mansart reactor is installed in the
local M2 and install it if not, then dispatch on the first argument. Target
script: `run-official-tck-persistence-3.2.sh`, with

```bash
./run-official-tck-persistence-3.2.sh                 # default: H2 in-memory
./run-official-tck-persistence-3.2.sh -Dtest=<Client> # one client
./run-official-tck-persistence-3.2.sh --pg            # PostgreSQL via Testcontainers
./run-official-tck-persistence-3.2.sh --sig           # signature tests
./run-official-tck-persistence-3.2.sh all             # full suite (long)
```

Maven profiles: `tck-run`, `tck-pg`, `tck-sig`, mirroring the Data runner.

## Reading results — never from the console

Parse the surefire reports, not the log:

```bash
grep -h -o 'tests="[0-9]*" *errors="[0-9]*" *skipped="[0-9]*" *failures="[0-9]*"' \
  target/surefire-reports/*.xml
```

Then aggregate. Report `PASS / FAIL / ERROR / SKIPPED` out of total as integers
you actually read. A test that ERRORs is not closer to passing than one that
FAILs — do not present a shift from ERROR to FAIL as progress.

## Reading the test source — search, do not unzip

The TCK test is the executable specification. But the jars are big (833 test
classes, 6.2 MB) and unzipping them into a coding session is what hit the context
wall once. So the sources are **pre-extracted and indexed** for search.

**One-time extraction** (per checkout; redo if the TCK version changes):

```bash
mkdir -p .tck-ref
unzip -oq ~/.m2/.../persistence-tck-spec-tests-3.2.1-sources.jar -d .tck-ref/tck-tests
unzip -oq ~/.m2/.../jakarta.persistence-api-3.2.0-sources.jar   -d .tck-ref/spec-api
unzip -oq ~/.m2/.../persistence-tck-common-3.2.1-sources.jar    -d .tck-ref/tck-common
python3 .opencode/build-tck-index.py          # -> .tck-ref/tck-index.md, 1110 sections
```

`.tck-ref/` is gitignored. `build-tck-index.py` turns the tree into one
heading-sectioned markdown so `ctx_index` chunks it one section per file.

**Per session**: `/next` runs `ctx_index(path: ".tck-ref/tck-index.md",
source: "JPA32-TCK")` — nothing enters context.

**To read a test or a spec type**, in order:

1. `ctx_search(queries: ["cascade persist OneToMany propagate", ...],
   source: "JPA32-TCK", limit: 2)` — returns the relevant files. Measured
   selective at full scale: a behavioural query lands the test, an API query lands
   the annotation/enum.
2. only if the search is empty: `grep -rn` / `sed -n` over `.tck-ref/`.
3. only if `.tck-ref/` is absent: `unzip -p <jar> '<one path>' | sed -n '<method>'`
   — never the whole jar.

`unzip` and `jar` are **denied to `jpa-dev`**; this reading is `@spec-reader`'s job,
which is why the coding agent delegates spec questions to it.

## The schema trap

The largest single cause of mass errors is `setup*Data failed`: the harness is
expected to create the schema from the **official DDL shipped in the TCK
distribution** (`persistence-tck/sql/<db>/<db>.ddl.persistence.sql` and the
matching `.sprocs.sql`), not from anything mansart generates. Fix that in the
runner — PostgreSQL first, since the official DDL is usable verbatim; H2 needs a
translated copy kept under the runner's test resources.

Do **not** "fix" it by teaching the provider about TCK entities.

## Forbidden

- Referencing `ee.jakarta.tck.*`, `com.sun.ts.*`, TCK table names or TCK class
  names anywhere outside `mansart-persistence-tck/`.
- Growing an exclude list, adding `@Disabled`, or narrowing `-Dtest=` in a
  committed pom to improve a ratio.
- Reporting a number you did not measure in the current session.
