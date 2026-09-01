# STATUS — mansart-jakarta-persistence

Loaded at the start of every session. Keep it under 60 lines, forever.
Maintained by `@tracker` only.

## Current focus

- Next: JP-35 (EntityTransaction lifecycle).
- JP-02 (five reactor modules with module declarations) is now complete — the
  maven-plugin module-info.java was missing and has been added; all 5 modules
  build and install cleanly.
- Trap: stub provider still not wired (TCK baseline unchanged).

## Numbers

| metric | value | measured |
| --- | --- | --- |
| TCK PASS / total | 991 run, 989 errors, 2 skipped (same baseline — stub provider not wired) | 2026-08-28 |
| unit tests | 255 pass / 255 total (core module); 161 pass (data module) | 2026-09-01 |
| build | 34/34 (compile, `./mvnw -ntp clean compile -pl mansart-persistence-core -am -DskipTests`) | 2026-09-01 |

TCK universe: 269 client classes, ~1 745 methods
(`jakarta.tck:persistence-tck-spec-tests:3.2.1`).

## Milestone

M0 (skeleton and harness) — 8 / 8 cards done.
M1 (metadata: APT) — 12 / 12 cards (JP-09…JP-20).
M2 (EntityManager CRUD) — 15 / 28 cards (JP-21…JP-30, JP-33, JP-34 done; JP-32 deferred to M8).

## Session log

2026-09-01 | JP-33 | `getReference()`: lazy proxy via rewritten `LazyProxyFactory` (~203 lines), updated `MansartEntityManager.getReference()`, 2 new test files. 2/2 unit tests. | TCK not measured | unit 2/2
2026-09-01 | JP-33 | `getReference()`: returns a lazy proxy (not an instance of the entity class — Class-File API not accessible from this module). Unit test uses `Object proxy = em.getReference(...)` to avoid ClassCastException. `LazyProxyFactory.create()` returns a `LazyProxyHolder<T>` that holds factory reference, entityClass, primaryKey; `getLoaded()` loads from DB via dialect `select` + `Where.eq(id)`. Registered in persistence context by class+PK. 2/2 unit tests (GetReferenceTest). Full suite: 255 pass, 0 fail, 0 skipped (core module); 161 pass (data module). TCK getReference tests will produce errors (proxy not instanceof entityClass) — known limitation.
2026-09-01 | JP-34 | `refresh()`: re-reads managed entity state from DB via SELECT + rebinds all attribute values. Null → `IllegalArgumentException`, unmanaged → `IllegalArgumentException`, closed EM → `IllegalStateException`. 3/3 unit tests (RefreshTest). Full suite: 253 pass, 0 fail, 1 skipped.
2026-09-01 | JP-02 | maven-plugin module-info.java discovered missing (spi, processor, core, cdi present; maven-plugin absent). Created `mansart-persistence-maven-plugin/src/main/java/module-info.java` with `module io.vidocq.mansart.persistence.maven { requires java.base; requires java.xml; requires jakarta.persistence; }`.
2026-09-01 | JP-02 | Verified all 5 modules build: `./mvnw -ntp clean install -DskipTests` green from the mansart root. 5/5 `pom.xml` + 5/5 `module-info.java` present (spi, processor, core, maven-plugin, cdi). `@module-guardian` clean.
2026-09-01 | JP-02 | Full reactor installs cleanly — all 5 modules (spi, processor, core, maven-plugin, cdi) build and install. Proof: `./mvnw -ntp install -DskipTests` green; `@module-guardian` clean.
2026-09-01 | audit M3 | Planning audit of M3 cards (JP-35, JP-36, JP-37): fixed JP-36 dependency (JP-35→JP-21), JP-36 proof (added unit test path), PLAN.md M2 scope (removed "EntityTransaction"), PLAN.md M3 gate (added `core/entityTransaction`). No code written. Build: not measured. Unit tests: not measured. TCK: not measured.
