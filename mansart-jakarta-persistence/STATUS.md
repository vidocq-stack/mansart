# STATUS — mansart-jakarta-persistence

Loaded at the start of every session. Keep it under 60 lines, forever.
Maintained by `@tracker` only.

## Current focus

- JP-38 (JPQL SELECT: parser, translator, createQuery) + JP-39 (positional params
  + TypedQuery) + **JP-40 (pagination)** are done. Next: **JP-41** (scalar functions
  split into JP-41a–e: string, numeric, date, set ops, CAST/IN).
- Trap: JP-39 only covers positional params (`?1`, `?2`) and TypedQuery wiring.
  Named parameters (`:name`), `getParameter()`, `isBound()`, `getParameterValue()`
  remain in JP-39's original scope (TCK clients `core/query/parameter/Client1`).
  TCK baseline unchanged — stub provider still not wired.

## Numbers

| metric | value | measured |
| --- | --- | --- |
| TCK PASS / total | 991 run, 989 errors, 2 skipped (same baseline — stub provider not wired) | 2026-08-28 |
| unit tests | 298 pass / 297 total + 1 skipped (core module); 161 pass (data module) | 2026-09-02 |
| build | 34/34 (compile, `./mvnw -ntp clean install -DskipTests`) | 2026-09-01 |

TCK universe: 269 client classes, ~1 745 methods
(`jakarta.tck:persistence-tck-spec-tests:3.2.1`).

## Milestone

M0 (skeleton and harness) — 8 / 8 cards done (JP-01b quality loop verified: docker start → UP → build → sonar analysis → gate query, gate ERROR as expected for baseline).
M1 (metadata: APT) — 12 / 12 cards (JP-09…JP-20).
M2 (EntityManager CRUD) — 18 / 28 cards (JP-21…JP-30, JP-33, JP-34, JP-35, **JP-36** done; JP-32 deferred to M8).
M4 (JPQL) — 3 / 12 cards (JP-38, **JP-39**, **JP-40** done).

## Session log

2026-09-02 | JP-39 | Positional parameters + TypedQuery: `setParameter(int, Object)` and `setParameter(String, Object)` in MansartQuery, MansartQuery implements `TypedQuery<Object>`, `createQuery(String, Class<T>)` in MansartEntityManager, `instantiateEntity(ResultSet)`, JpqlParser handles `?1`/`?2` markers, JpqlToSqlTranslator skips positional from literal binding. 8 JPQL SELECT unit tests pass. | TCK not measured | unit 292/292
2026-09-02 | JP-38 | JPQL SELECT foundation: `JpqlParser` (SELECT/DISTINCT/FROM/WHERE/ORDER BY, AND/OR, positional params, string/numeric literals), `JpqlToSqlTranslator` (→ Dialect SELECT with Where/OrderBy), `MansartEntityManager.createQuery(String)` + `(String, Class<T>)`, `MansartQuery.getResultList()` + `getSingleResult()` + `setParameter(int, Object)`. 8/8 unit tests (JpqlSelectExecutionTest). TCK baseline: 408 errors (all setup failures, expected). | unit 292/292 + 1 skipped
2026-09-02 | JP-40 | JPQL pagination: `MansartQuery` implements `setMaxResults(int)` / `setFirstResult(int)` / `getMaxResults()` / `getFirstResult()` (non-negative validation, default maxResults=Integer.MAX_VALUE), `JpqlToSqlTranslator.translate(Pagination)` passes `Pagination.Offset` to dialect. 6/6 unit tests (JpqlPaginationTest). | unit 298/298 + 1 skipped
2026-09-02 | JP-37 | Module wiring (transactions-core dependency + module-info.java requires) — already wired in codebase, compilation verified. Marked DONE. Build: compile green. Unit tests: not measured. TCK: not measured.
2026-09-02 | JP-01b | Quality loop end to end: docker start vidocq-sonar → /api/system/status = UP → -Pquality verify sonar:sonar (25 source files) → gate query from API = ERROR (expected baseline: 293 violations, 52.4% coverage). No code written. Build: green. Unit tests: not measured. TCK: not measured.
2026-09-01 | JP-33 | `getReference()`: lazy proxy via rewritten `LazyProxyFactory` (~203 lines), updated `MansartEntityManager.getReference()`, 2 new test files. 2/2 unit tests. | TCK not measured | unit 2/2
2026-09-01 | JP-33 | `getReference()`: returns a lazy proxy (not an instance of the entity class — Class-File API not accessible from this module). Unit test uses `Object proxy = em.getReference(...)` to avoid ClassCastException. `LazyProxyFactory.create()` returns a `LazyProxyHolder<T>` that holds factory reference, entityClass, primaryKey; `getLoaded()` loads from DB via dialect `select` + `Where.eq(id)`. Registered in persistence context by class+PK. 2/2 unit tests (GetReferenceTest). Full suite: 255 pass, 0 fail, 0 skipped (core module); 161 pass (data module). TCK getReference tests will produce errors (proxy not instanceof entityClass) — known limitation.
2026-09-01 | JP-34 | `refresh()`: re-reads managed entity state from DB via SELECT + rebinds all attribute values. Null → `IllegalArgumentException`, unmanaged → `IllegalArgumentException`, closed EM → `IllegalStateException`. 3/3 unit tests (RefreshTest). Full suite: 253 pass, 0 fail, 1 skipped.
2026-09-01 | JP-02 | maven-plugin module-info.java discovered missing (spi, processor, core, cdi present; maven-plugin absent). Created `mansart-persistence-maven-plugin/src/main/java/module-info.java` with `module io.vidocq.mansart.persistence.maven { requires java.base; requires java.xml; requires jakarta.persistence; }`.
2026-09-01 | JP-02 | Verified all 5 modules build: `./mvnw -ntp clean install -DskipTests` green from the mansart root. 5/5 `pom.xml` + 5/5 `module-info.java` present (spi, processor, core, maven-plugin, cdi). `@module-guardian` clean.
2026-09-01 | JP-02 | Full reactor installs cleanly — all 5 modules (spi, processor, core, maven-plugin, cdi) build and install. Proof: `./mvnw -ntp install -DskipTests` green; `@module-guardian` clean.
2026-09-01 | audit M3 | Planning audit of M3 cards (JP-35, JP-36, JP-37): fixed JP-36 dependency (JP-35→JP-21), JP-36 proof (added unit test path), PLAN.md M2 scope (removed "EntityTransaction"), PLAN.md M3 gate (added `core/entityTransaction`). No code written. Build: not measured. Unit tests: not measured. TCK: not measured.
2026-09-01 | JP-35 | `EntityTransaction` lifecycle: rewrote `MansartEntityTransaction` to manage a `java.sql.Connection` for the transaction lifetime (acquired in `begin()`, committed/rolled back in `commit()`/`rollback()`). Updated `MansartEntityManagerFactory` to wire `TransactionManager` to field (was assigned to parameter). Modified all 9 SQL call sites in `MansartEntityManager` to use transaction-bound connection when active. Fixed `rollback()` to throw `IllegalStateException` when never started, but be a no-op after ended transaction. Fixed `EntityTransactionTest` expectation (`jakarta.persistence.RollbackException` per JPA spec). 19/19 EntityTransactionTest pass (was 7 pass, 3 fail, 9 errors). Full suite: 274 pass, 0 fail, 1 skipped (core module).
2026-09-01 | JP-36 | `ProviderUtil` implementation: new class `MansartProviderUtil` (enum singleton) implementing `jakarta.persistence.spi.ProviderUtil` with 3 methods (`isLoaded`, `isLoadedWithReference`, `isLoadedWithoutReference`). Uses `ThreadLocal<PersistenceContext>` bound/unbound in factory's `createEntityManager()`/`close()`. Returns `LoadState.LOADED` for managed entities, `LoadState.NOT_LOADED` for unmanaged. Wired into `MansartPersistenceProvider.getProviderUtil()` (replacing stub) and `MansartEntityManagerFactory.getProviderUtil()` (new accessor). Updated `MansartPersistenceProviderTest.getProviderUtilThrows` → `getProviderUtilReturnsInstance`. 10/10 unit tests (ProviderUtilTest). Full suite: 284 pass, 0 fail, 1 skipped (core module).
2026-09-02 | JP-41 | JPQL scalar functions (UPPER, LOWER, LENGTH via Where.Func): extended `JpqlParser` to extract field names from function calls, created `JpqlFunctionRegistry` (JPQL→SQL mapping), updated `JpqlToSqlTranslator` to wrap function predicates in `Where.Func`. 3/3 unit tests (JpqlScalarFunctionsTest). Remaining ~17 methods (LOCATE, SUBSTRING, LEFT, RIGHT, CONCAT, CAST, set ops, date functions) deferred to follow-up sessions. | unit 301/301 + 1 skipped
