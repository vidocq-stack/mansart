# mansart-jakarta-persistence — Plan

Jakarta Persistence 3.2 for Vidocq, implemented with a **local** model
(Qwen3.6-35B-A3B-MTPLX on oMLX) driving OpenCode. The engineering target is the
official TCK; the secondary target is to show that a spec of this size can be
implemented by a local model when the work is decomposed properly.

## Architecture

```
                    application entities (@Entity)
                              │
        ┌─────────────────────┴────────────────────────┐
        │ tier 1: APT                    tier 2: Maven plugin
        │ mansart-persistence-processor  mansart-persistence-maven-plugin
        │ (sources)                      (external jars, Class-File API)
        └─────────────────────┬────────────────────────┘
                              ▼
                    _Entity metamodel · EntityDescriptor · accessors · lazy proxies
                              │
                              ▼
                    mansart-persistence-core
                    EMF · EntityManager · persistence context · flush · JPQL · Criteria
                              │
              ┌───────────────┼────────────────┐
              ▼               ▼                ▼
   mansart-data-dialect-spi   mansart-transactions   javax.sql.DataSource
   (H2, PostgreSQL)           (ScopedValue TM)       (mansart-pool optional)
```

Reactor:

| module | role |
| --- | --- |
| `mansart-persistence-spi` | metadata model; reuses `mansart-data-dialect-spi` |
| `mansart-persistence-processor` | APT, `SourceVersion.RELEASE_25` |
| `mansart-persistence-core` | runtime |
| `mansart-persistence-maven-plugin` | Class-File API generation for external jars |
| `mansart-persistence-cdi` | CDI 4.1 Lite through Vauban |
| `mansart-persistence-tests` | unit + Arquillian |
| `mansart-persistence-external-lib` | fixture jar of entities APT never sees |
| `mansart-persistence-external-it` | proves tier 1 ≡ tier 2 |
| `mansart-persistence-tck` | **out of reactor**, standalone POM `4.0.0` |

Every one of those shapes has a working precedent one directory up, in
`mansart-jakarta-data`. Mirror it. The abandoned
`feature/jakarteee-mansart-persistence-*` branches are out of scope and must not
be consulted.

## Milestones

Each milestone ends on a **measured** TCK number for its named packages. A
milestone is not done because the code compiles.

| id | scope | TCK gate (client packages) | clients |
| --- | --- | --- | --- |
| **M0** | reactor skeleton, module declarations, `PersistenceProvider` discovery, out-of-reactor TCK runner, official DDL bootstrap | harness runs; signature test subset | — |
| **M1** | metadata: APT reads `@Entity/@Id/@Column/@Table/@Embeddable`, emits `_Entity`, `EntityDescriptor`, accessors | `core/metamodelapi` | 16 |
| **M2** | `EntityManager` CRUD: persist, find, remove, merge, identity map, flush ordering, `EntityTransaction` | `core/entitytest`, `se/entityManager` | 21 |
| **M3** | transactions: bind `mansart-transactions`, resource-local + JTA, `@PersistenceContext` through CDI | `jpa22/se`, `se/pluggability` | 3 |
| **M4** | JPQL: parser → dialect AST → SQL; named and dynamic queries; parameters; result mapping | `jpa22/query`, `core/query` | 2+ |
| **M5** | relationships: `@OneToMany/@ManyToOne/@ManyToMany/@OneToOne`, owning side, cascade, derived ids, lazy proxies (Class-File API) | `core/relationship`, `core/derivedid` | 23 |
| **M6** | inheritance, attribute overrides, lifecycle callbacks, listeners | `core/inheritance`, `core/override`, `core/callback` | 20 |
| **M7** | Criteria API | `core/criteriaapi` | 3 |
| **M8** | schema generation, locking, versioning, second-level cache, types | `se/schemaGeneration`, `core/lock`, `core/versioning`, `se/cache`, `core/types` | 26 |
| **M9** | annotation sweep, repeatable annotations, `StoredProcedureQuery`, `PersistenceUtil`, full-suite run on H2 and PostgreSQL | `core/annotations`, `jpa22/repeatable`, remainder | 35+ |

269 client classes, ~1 745 test methods in
`jakarta.tck:persistence-tck-spec-tests:3.2.1`.

## Decomposition rule

The milestone is the unit of *planning*. The **card** (`JP-xx` in `TASKS.md`) is
the unit of *work*, and it is sized for one 53k-token session:

- one goal, stated as a behaviour, not as a file to write;
- at most **4 files** touched;
- exactly one test that proves it, named by path or by TCK client;
- explicit dependencies on other cards.

Only the next milestone is expanded into cards. When a milestone completes,
`@tracker` expands the following one. Planning ten milestones' worth of cards up
front would be fiction, and reading them would cost context every session.

## Explicit non-goals

Multi-tenancy · reactive / R2DBC · NoSQL backends · Bean Validation integration
beyond what the TCK requires · a query cache · Hibernate-compatible extensions.
