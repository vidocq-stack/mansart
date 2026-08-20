---
name: mansart-persistence-tck
description: How the official Jakarta Persistence 3.2 TCK is wired and run for mansart (out-of-reactor runner, Maven profiles, Arquillian/Vauban harness, official DDL scripts), how to read results and classify failures. Load for any TCK run, triage or harness change.
---

# Jakarta Persistence 3.2 TCK for mansart

Runner: `mansart-jakarta-persistence/mansart-persistence-tck/` — **out of the reactor**
(standalone Model 4.0.0 POM, no `<parent>`). Never `mvn -pl` it from a parent; always
`cd` there. Artifacts already in `~/.m2`: `jakarta.tck:persistence-tck-spec-tests:3.2.1`
(+ `-sources.jar`), `jakarta.tck:persistence-tck-dist:3.2.1` (`.zip` with `sql/`,
docs) — do not re-download.

## Harness layout (`src/test`)

- `io/vidocq/vauban/tck/*` — Arquillian container for Vauban (`VaubanDeployableContainer`
  with a ByteArray class loader serving ShrinkWrap resources, incl.
  `META-INF/services/jakarta.persistence.spi.PersistenceProvider`).
- `io/vidocq/mansart/persistence/tck/*` — `MansartTckArchiveAppender` (adds Mansart to
  every deployment), `H2DataSourceProducer`, `PostgresDataSourceProducer`
  (Testcontainers), `MansartPersistenceSmokeTest`, `SimpleEntity`.
- `resources/META-INF/persistence.xml` — units `JPATCK`/`JPATCK2` (H2
  `jdbc:h2:mem:test;DB_CLOSE_DELAY=-1`, `schema-generation.database.action=create`).
- `resources/tck-suite.xml` (smoke, TestNG), `resources/tck-full-suite.xml`,
  `resources/arquillian.xml`.

Profiles (`pom.xml`): default = smoke; `tck-run` (TestNG suite + TCK deps); `tck-pg`
(PostgreSQL, Docker); `tck-sig` (signature tests); `tck-full` = surefire
`dependenciesToScan` over `ee/jakarta/tck/persistence/core/**/Client*.class` (JUnit 5
engine) — this is what produced the 1745-test runs in the tracker.

## Run

```bash
cd mansart-jakarta-persistence/mansart-persistence-tck
./run-official-tck-persistence-3.2.sh --smoke                 # harness only (3 tests)
./run-official-tck-persistence-3.2.sh -Dtest=ee.jakarta.tck.persistence.core.entitytest.persist.basic.Client
./run-official-tck-persistence-3.2.sh --all                   # tck-run suite on H2
PG=1 ./run-official-tck-persistence-3.2.sh --full             # + signature, PostgreSQL
mvn -ntp -Ptck-full test -Dtest='ee.jakarta.tck.persistence.core.*.Client'   # full scan (very long)
```

Always through `ctx_batch_execute`. Results: `target/surefire-reports/TEST-*.xml`.
Aggregate, never cat:

```bash
python3 - <<'EOF'
import glob,re,collections;t=collections.Counter()
for f in glob.glob('target/surefire-reports/TEST-*.xml'):
    m=re.search(r'tests="(\d+)".*?errors="(\d+)".*?skipped="(\d+)".*?failures="(\d+)"',open(f,errors='ignore').read(1500),re.S)
    if m:
        a,b,c,d=map(int,m.groups());t.update(tests=a,errors=b,skipped=c,failures=d)
print(dict(t))
EOF
grep -h -o '<error message="[^"]\{0,100\}' target/surefire-reports/TEST-*.xml | sed 's/[0-9]\+/N/g' | sort | uniq -c | sort -rn | head -15
```

## Reading the failures (state on 2026-08-19: 1745 tests, 2 PASS, 1586 errors)

| Pattern | Bucket | Meaning / fix location |
| --- | --- | --- |
| `setupCustomerData/ProductData/AliasData/… failed` (~750) | harness/schema | TCK entities' tables missing or wrong. The TCK **expects its schema to be created by the harness from the official DDL** shipped in the dist zip: `persistence-tck/sql/<db>/<db>.ddl.persistence.sql` and `….sprocs.sql` (derby, postgresql, mysql, oracle, db2, mssqlserver, sybase — no H2 file). Fix in the runner: run the DDL once per DB before the suite, set `schema-generation.database.action=none`. PostgreSQL profile can use the script verbatim; H2 needs a translated copy kept under `src/test/resources/sql/h2/`. **Never** teach the provider about TCK classes (`MansartSchemaManager.createTablesForKnownClasses` is the anti-pattern). |
| `Column "X" not found`, `Table not found` | harness/schema or dialect quoting | Same as above, or identifier quoting/case in the H2 dialect. |
| `UnsupportedOperationException`, NPE on a Mansart return value | unimplemented API | Real implementation work; name the method; TDD in core. |
| Assertion with actual vs expected values | behaviour bug | Implementation bug → BUG.md candidate. |
| `ClassNotFoundException` without package, "Unresolved compilation problems" | stale ECJ classes | `mvn clean`. |
| Provider not found / `NoProviderException` | harness | class-loader/services wiring in `VaubanDeployableContainer`. |

Rules: work **one `Client` class at a time** to PASS; the only progress metric is
PASS/Total; "N fewer errors" is noise. Read the test source (`-sources.jar`, see
`@spec-reader`) before changing the implementation. Smoke must stay 3/3.
