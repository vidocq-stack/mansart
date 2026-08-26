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

## Reading the test source

The TCK test is the executable specification. Read the failing **method**:

```bash
unzip -p ~/.m2/repository/jakarta/tck/persistence-tck-spec-tests/3.2.1/\
persistence-tck-spec-tests-3.2.1-sources.jar \
  'ee/jakarta/tck/persistence/core/entitytest/persist/basic/Client.java' | sed -n '1,120p'
```

**`unzip` and `jar` are denied to `jpa-dev`** — not by convention, by permission.
This rule was written before and ignored: one card ran 34 `unzip` calls for
26 000 tokens and hit the context wall. So the path is now: `task` to
`spec-reader`, one precise question, and it returns the method plus its citation
in forty lines. It pays the extraction cost in its own context, which then dies.

If you *are* `spec-reader`: locate first with `unzip -l | grep`, extract the one
file, then `sed -n` the one method. Never the whole jar, never the whole class.

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
