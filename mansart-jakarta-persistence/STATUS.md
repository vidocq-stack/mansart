# STATUS — mansart-jakarta-persistence

Loaded at the start of every session. Keep it under 60 lines, forever.
Maintained by `@tracker` only.

## Current focus

- Next card: **JP-04** — `Persistence.createEntityManagerFactory` with `Map` → `PersistenceUnitInfo` → `EntityManager`
- JP-03 done: `MansartPersistenceProvider` stub (all 6 methods throw `UnsupportedOperationException`), wired via JPMS `provides` + `META-INF/services`, ServiceLoader-discoverable. 2 unit tests pass.
- JP-01 done: reactor builds and installs, empty.

## Numbers

| metric | value | measured |
| --- | --- | --- |
| TCK PASS / total | not measured | — |
| unit tests | 2 (ProviderDiscoveryTest) | 2026-08-25 |
| build | 34/34 (install -DskipTests) | 2026-08-25 |

TCK universe: 269 client classes, ~1 745 methods
(`jakarta.tck:persistence-tck-spec-tests:3.2.1`).

## Milestone

M0 (skeleton and harness) — 3 / 8 cards done.

## Session log

2026-08-25 20:45 — JP-01: reactor builds and installs, empty.
Created 8 sub-module `pom.xml` (spi, processor, core, maven-plugin, cdi, tests,
external-lib, external-it), updated root `pom.xml` with module registration and
`dependencyManagement` entries. 34/34 modules build — `./mvnw -ntp install -DskipTests`.

2026-08-25 21:15 — JP-03: `MansartPersistenceProvider` stub. Implemented all 6
`PersistenceProvider` methods (each throws `UnsupportedOperationException`), wired
via JPMS `provides` + `META-INF/services/jakarta.persistence.spi.PersistenceProvider`
file. ECJ required `Map<?, ?>` signatures (not `Map<String, ?>`). 2 unit tests
pass — `ProviderDiscoveryTest` resolves provider via ServiceLoader and verifies
both `hasSize(1)` and `UnsupportedOperationException("not implemented:
createEntityManagerFactory")`. 34/34 modules build.
