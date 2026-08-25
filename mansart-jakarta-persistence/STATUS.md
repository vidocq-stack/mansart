# STATUS — mansart-jakarta-persistence

Loaded at the start of every session. Keep it under 60 lines, forever.
Maintained by `@tracker` only.

## Current focus

- Next card: **JP-03** — `Persistence.createEntityManagerFactory` finds our provider
- JP-02 done: five reactor modules have JPMS module declarations (core needs CDI API dependency fix; maven-plugin is classpath-based)
- JP-01 done: reactor builds and installs, empty.

## Numbers

| metric | value | measured |
| --- | --- | --- |
| TCK PASS / total | not measured | — |
| unit tests | not measured | — |
| build | 34/34 (install -DskipTests) | 2026-08-25 |

TCK universe: 269 client classes, ~1 745 methods
(`jakarta.tck:persistence-tck-spec-tests:3.2.1`).

## Milestone

M0 (skeleton and harness) — 2 / 8 cards done.

## Session log

2026-08-25 20:45 — JP-01: reactor builds and installs, empty.
Created 8 sub-module `pom.xml` (spi, processor, core, maven-plugin, cdi, tests,
external-lib, external-it), updated root `pom.xml` with module registration and
`dependencyManagement` entries. 34/34 modules build — `./mvnw -ntp install -DskipTests`.

2026-08-25 21:03 — JP-02: five reactor modules with JPMS module declarations.
Created `module-info.java` for spi, processor, core, cdi. Deleted maven-plugin's
module-info.java (Maven JARs lack module-info.class; classpath-based). Fixed
core's `pom.xml`: added `jakarta.enterprise.cdi-api` dependency so Jakarta
Transaction API's transitive `jakarta.cdi` resolves. Fixed cdi module:
`requires jakarta.cdi;` (not `jakarta.enterprise.cdi`). 34/34 build.
