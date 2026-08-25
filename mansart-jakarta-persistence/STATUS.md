# STATUS — mansart-jakarta-persistence

Loaded at the start of every session. Keep it under 60 lines, forever.
Maintained by `@tracker` only.

## Current focus

- Next card: **JP-02** — the five reactor modules exist with module declarations.
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

M0 (skeleton and harness) — 1 / 8 cards done.

## Session log

2026-08-25 20:45 — JP-01: reactor builds and installs, empty.
Created 8 sub-module `pom.xml` (spi, processor, core, maven-plugin, cdi, tests,
external-lib, external-it), updated root `pom.xml` with module registration and
`dependencyManagement` entries. 34/34 modules build — `./mvnw -ntp install -DskipTests`.
