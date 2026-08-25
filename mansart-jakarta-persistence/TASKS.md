# TASKS — mansart-jakarta-persistence

The backlog. **One card per session.** A card names a behaviour, at most 4 files,
and the single test that proves it. Status is `TODO` / `WIP` / `DONE` / `BLOCKED`.

Only the current milestone is expanded. `@tracker` expands the next one when this
one closes. See `PLAN.md` for the milestone map.

Card template:

```
### JP-xx — <goal, as a behaviour>            [TODO]
deps:   JP-yy
files:  <max 4 paths>
proof:  <test path or TCK client>
notes:  <traps, one or two lines>
```

---

## M0 — skeleton and harness

### JP-01 — the reactor builds and installs, empty                      [DONE]
deps:   —
files:  `mansart-jakarta-persistence/pom.xml`, `mansart/pom.xml`
proof:  `./mvnw -ntp install -DskipTests` green from the mansart root
notes:  Copy the POM shape of `mansart-jakarta-data/pom.xml`. Add the new module
        to the root `<modules>`. No Java yet. Version `0.3.0-SNAPSHOT`.

### JP-01b — the quality loop runs end to end on an empty reactor       [TODO]
deps:   JP-01
files:  `mansart-jakarta-persistence/pom.xml` (already wired), `STATUS.md`
proof:  `/sonar` returns a real `SONAR:` block with a gate status read from the API
notes:  Sonar properties and `sonar-maven-plugin` are already in the sub-reactor
        POM. This card only proves the loop works: `docker start mansart-sonar`,
        wait for `/api/system/status` = UP, run `-Pquality ... verify sonar:sonar`,
        read the gate from the API. Expect a token to be required (SonarQube 26.5
        dropped anonymous analysis) — if 401, stop and tell the maintainer to
        export `SONAR_TOKEN`. Do this NOW, on an empty reactor: wiring quality on
        20 000 lines is a project, on 0 lines it is five minutes.

### JP-02 — the five reactor modules exist with module declarations     [TODO]
deps:   JP-01
files:  the five `pom.xml` + the five `module-info.java`
proof:  `./mvnw -ntp install -DskipTests` green; `@module-guardian` clean
notes:  `spi`, `processor`, `core`, `maven-plugin`, `cdi`. Empty `exports`,
        no `opens`. Drivers `provided`, test libs `test`.

### JP-03 — `Persistence.createEntityManagerFactory` finds our provider [TODO]
deps:   JP-02
files:  `core/.../MansartPersistenceProvider.java`, `core/module-info.java`,
        `tests/.../ProviderDiscoveryTest.java`
proof:  `mansart-persistence-tests` — a test that resolves the provider and gets
        an `UnsupportedOperationException("not implemented: createEntityManagerFactory")`
notes:  `provides jakarta.persistence.spi.PersistenceProvider with ...`. The
        failing-by-design exception is the correct placeholder here.

### JP-04 — `persistence.xml` is parsed without an XML dependency        [TODO]
deps:   JP-03
files:  `core/.../PersistenceUnitReader.java`, `core/.../PersistenceUnitInfoImpl.java`,
        `tests/.../PersistenceUnitReaderTest.java`
proof:  `PersistenceUnitReaderTest` — unit, transaction-type, provider, classes,
        properties, `jta-data-source`
notes:  `java.xml` is in the JDK; no external parser. Zero-dependency rule stands.

### JP-05 — the out-of-reactor TCK runner starts and reports zero        [TODO]
deps:   JP-03
files:  `mansart-persistence-tck/pom.xml`,
        `mansart-persistence-tck/run-official-tck-persistence-3.2.sh`,
        `mansart-persistence-tck/README.md`
proof:  the script runs one client and produces surefire reports with real
        integers (all failing is the expected result)
notes:  Standalone POM, `modelVersion 4.0.0`, **no `<parent>`**. Pin
        `jakarta.tck:persistence-tck-spec-tests:3.2.1`. Mirror
        `mansart-data-tck/run-official-tck-data-1.0.sh`, profiles `tck-run`,
        `tck-pg`, `tck-sig`.

### JP-06 — the TCK schema comes from the official DDL                   [TODO]
deps:   JP-05
files:  `mansart-persistence-tck/src/test/resources/...`, the runner script
proof:  `setup*Data` no longer fails for one chosen client on PostgreSQL
notes:  **The single biggest error source.** Load
        `persistence-tck/sql/<db>/<db>.ddl.persistence.sql` + `.sprocs.sql` from
        the TCK distribution. PostgreSQL first (usable verbatim); H2 needs a
        translated copy. Never teach the provider about TCK entities.

### JP-07 — signature test subset runs                                   [TODO]
deps:   JP-05
files:  `mansart-persistence-tck/pom.xml`, the runner script
proof:  `--sig` produces a real signature report
notes:  `jakarta.tck:sigtest-maven-plugin`. Failures here are expected until the
        API surface is complete; the point is that the gate exists.

### JP-08 — record the M0 baseline                                       [TODO]
deps:   JP-06, JP-07
files:  `STATUS.md`
proof:  a full-suite run, numbers written down
notes:  This baseline is what every later milestone is measured against. It will
        be close to 0/1745 and that is fine — an honest 0 beats an invented 40.

---

## M1 — metadata (expand after M0 closes)

Placeholder. `@tracker` expands this into cards when the M0 baseline is recorded.
Scope: APT reads `@Entity`, `@Id`, `@GeneratedValue`, `@Column`, `@Table`,
`@Embeddable`, `@MappedSuperclass`, `@Transient`, `@Basic`, `@Enumerated`,
`@Temporal`; emits `_Entity`, `EntityDescriptor`, accessors. Gate:
`core/metamodelapi` (16 clients).

## M2 … M9

See `PLAN.md`. Not expanded.

---

## Discovered work

Cards that came out of a session but did not belong to its card. Appended by
`@tracker`, never merged into an existing card.

_(empty)_
