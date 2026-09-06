# mansart-jakarta-persistence — Status

## Current Focus
- **Card**: M5-JP-30 — ID generation (AUTO, IDENTITY, SEQUENCE, TABLE strategies) — DONE
- **Milestone**: M5 (in progress)

## Milestone Progress Overview

| Milestone | Total JP | Done | % Complete | Status |
|----------|----------|------|------------|--------|
| M0 | 7 | 7 | 100% | DONE |
| M1 | 6 | 6 | 100% | DONE |
| M2 | 5 | 5 | 100% | DONE |
| M3 | 4 | 4 | 100% | DONE |
| M4 | 6 | 6 | 100% | DONE |
| M5 | 5 | 2 | 40% | IN_PROGRESS |
| M6 | 12 | 0 | 0% | TODO |
| M7 | 12 | 0 | 0% | TODO |
| M8 | 4 | 0 | 0% | TODO |
| M9 | 3 | 0 | 0% | TODO |
| M10 | 3 | 0 | 0% | TODO |
| M11 | 8 | 0 | 0% | TODO |
| M12 | 5 | 0 | 0% | TODO |
| M13 | 5 | 0 | 0% | TODO |
| M14 | 5 | 0 | 0% | TODO |
| M15 | 6 | 0 | 0% | TODO |
| M16 | 3 | 0 | 0% | TODO |
| M17 | 5 | 0 | 0% | TODO |
| M18 | 4 | 0 | 0% | TODO |
| M19 | 4 | 0 | 0% | TODO |
| M20 | 6 | 0 | 0% | TODO |
| **TOTAL** | **112** | **30** | **26.8%** | |

---

## Metrics
- **Baseline TCK**: 2/0/2 PASS (ProviderDiscoveryTest — harness works, provider discoverable)
- **Unit Tests**: 242/242 PASS (mansart-persistence-tests module)
- **Integration Tests**: 11/11 PASS (ExternalEntityIT)
- **Modules Building**: 9/9 (all modules compile)
- **Modules with module-info.java**: 5/9 (spi, processor, core, cdi, external-lib)

---

## Card Status

### M0 — Skeleton + Real TCK Harness Baseline
- M0-JP-01 — Parent pom.xml with 9 sub-modules — DONE
- M0-JP-02 — module-info.java for each module — DONE
- M0-JP-03 — mansart-persistence-spi module — DONE
- M0-JP-04 — mansart-persistence-core skeleton — DONE
- M0-JP-05 — TCK infrastructure (out-of-reactor) — DONE
- M0-JP-06 — Harness wiring + provider registration — DONE
- M0-JP-07 — Baseline TCK run — DONE

### M1 — Entity Metamodel + APT Generation
- M1-JP-08 — MansartPersistenceProcessor — DONE
- M1-JP-09 — Entity scanning — DONE
- M1-JP-10 — _Entity generation — DONE
- M1-JP-11 — Standard JPA static metamodel generation — DONE
- M1-JP-12 — MethodHandle resolution — DONE
- M1-JP-13 — SQL naming conventions — DONE

### M2 — Maven Plugin (Tier 2) + External Library Support
- M2-JP-14 — MansartPersistenceMojo — DONE
- M2-JP-15 — Class-File API parsing — DONE
- M2-JP-16 — Lazy association proxies — DONE
- M2-JP-17 — external-lib — DONE
- M2-JP-18 — external-it — DONE

### M3 — Runtime Class-File API (Tier 3) Fallback
- M3-JP-19 — RuntimeEntityModelBuilder — DONE
- M3-JP-20 — RuntimeEntityClassGenerator — DONE
- M3-JP-21 — MansartCallback — DONE
- M3-JP-22 — Tier3WarningCollector — DONE

### M4 — Core Runtime: EntityManagerFactory + EntityManager
- M4-JP-23 — MansartPersistenceProvider — DONE
- M4-JP-24 — MansartEntityManagerFactory — DONE
- M4-JP-25 — MansartEntityManager — DONE
- M4-JP-26 — Persistence context state machine — DONE
- M4-JP-27 — Transaction integration — DONE
- M4-JP-28 — Connection management — DONE

### M5 — Object-Relational Mapping (Basic)
- M5-JP-29 — EntityMapper (CRUD via dialect SPI) — DONE
- M5-JP-30 — ID generation (AUTO, IDENTITY, SEQUENCE, TABLE) — DONE
- M5-JP-31 — Column mapping — TODO
- M5-JP-32 — Table mapping — TODO
- M5-JP-33 — Integration tests with H2 — TODO

### M6 — Query Execution (JPQL)
- M6-JP-34 — JPQL parser — TODO
- M6-JP-35 — SELECT queries — TODO
- M6-JP-36 — FROM clause — TODO
- M6-JP-37 — WHERE expressions — TODO
- M6-JP-38 — UPDATE queries — TODO
- M6-JP-39 — DELETE queries — TODO
- M6-JP-40 — Named and positional parameters — TODO
- M6-JP-41 — Aggregate functions — TODO
- M6-JP-42 — String functions — TODO
- M6-JP-43 — Date/time functions — TODO
- M6-JP-44 — CASE expressions — TODO
- M6-JP-45 — Subqueries — TODO

### M7 — Advanced Mapping
- M7-JP-46 — @ManyToOne, @OneToOne — TODO
- M7-JP-47 — @OneToMany, @ManyToMany — TODO
- M7-JP-48 — @JoinColumn, @JoinTable — TODO
- M7-JP-49 — @Inheritance — TODO
- M7-JP-50 — @DiscriminatorColumn, @DiscriminatorValue — TODO
- M7-JP-51 — @Embedded, @Embeddable — TODO
- M7-JP-52 — @Enumerated — TODO
- M7-JP-53 — @Temporal — TODO
- M7-JP-54 — @Lob — TODO
- M7-JP-55 — @Version — TODO
- M7-JP-56 — @Transient — TODO
- M7-JP-57 — @Convert — TODO

### M8 — Entity Lifecycle + Callbacks
- M8-JP-58 — Lifecycle callback annotations — TODO
- M8-JP-59 — @EntityListeners — TODO
- M8-JP-60 — Callback ordering — TODO
- M8-JP-61 — Inherited callbacks — TODO

### M9 — Cascading + Orphan Removal
- M9-JP-62 — @CascadeType — TODO
- M9-JP-63 — orphanRemoval — TODO
- M9-JP-64 — Cascade ordering and edge cases — TODO

### M10 — Fetch Types + Lazy Loading
- M10-JP-65 — @FetchType LAZY/EAGER — TODO
- M10-JP-66 — Lazy loading proxies — TODO
- M10-JP-67 — Fetch join hints — TODO

### M11 — Criteria API
- M11-JP-68 — CriteriaBuilder — TODO
- M11-JP-69 — CriteriaQuery — TODO
- M11-JP-70 — Root, Join, Fetch, Path — TODO
- M11-JP-71 — Predicate — TODO
- M11-JP-72 — Order — TODO
- M11-JP-73 — GroupBy, Having — TODO
- M11-JP-74 — Subquery — TODO
- M11-JP-75 — Metamodel-based queries — TODO

### M12 — Named Queries + Native Queries
- M12-JP-76 — @NamedQuery — TODO
- M12-JP-77 — @NamedNativeQuery — TODO
- M12-JP-78 — @SqlResultSetMapping — TODO
- M12-JP-79 — Query hints — TODO
- M12-JP-80 — Native query execution — TODO

### M13 — Persistence Context + Caching
- M13-JP-81 — Persistence context propagation — TODO
- M13-JP-82 — First-level cache — TODO
- M13-JP-83 — Cache interface — TODO
- M13-JP-84 — SharedCacheMode — TODO
- M13-JP-85 — Cache invalidation and eviction — TODO

### M14 — Transaction Management + Synchronization
- M14-JP-86 — Local transaction (RESOURCE_LOCAL) — TODO
- M14-JP-87 — JTA transaction — TODO
- M14-JP-88 — TransactionSynchronizationRegistry — TODO
- M14-JP-89 — Synchronization callbacks — TODO
- M14-JP-90 — Transaction timeout and isolation — TODO

### M15 — CDI Integration (Vauban)
- M15-JP-91 — MansartPersistenceExtension — TODO
- M15-JP-92 — Producer for EM/EMF/Provider — TODO
- M15-JP-93 — @PersistenceContext injection — TODO
- M15-JP-94 — @PersistenceUnit injection — TODO
- M15-JP-95 — CDI scoping — TODO
- M15-JP-96 — Integration tests with Vauban — TODO

### M16 — PostgreSQL Dialect Support
- M16-JP-97 — PostgreSQL dialect — TODO
- M16-JP-98 — PostgreSQL-specific features — TODO
- M16-JP-99 — Integration tests with Testcontainers PG — TODO

### M17 — Error Handling + Exceptions
- M17-JP-100 — SQLState mapping — TODO
- M17-JP-101 — PersistenceException hierarchy — TODO
- M17-JP-102 — Constraint violation handling — TODO
- M17-JP-103 — Transaction rollback handling — TODO
- M17-JP-104 — Entity not found / illegal state — TODO

### M18 — Validation + Schema Generation
- M18-JP-105 — Schema validation — TODO
- M18-JP-106 — Schema generation — TODO
- M18-JP-107 — persistence.xml schema generation properties — TODO
- M18-JP-108 — Integration tests with schema generation — TODO

### M19 — Performance + Benchmarks
- M19-JP-109 — BENCH.md baseline — TODO
- M19-JP-110 — JMH benchmarks — TODO
- M19-JP-111 — Virtual thread pinning verification — TODO
- M19-JP-112 — Performance tuning — TODO

### M20 — Official Jakarta Persistence 3.2 TCK
- M20-JP-113 — TCK infrastructure — TODO
- M20-JP-114 — TCK EntityTests — TODO
- M20-JP-115 — TCK PersistenceTests — TODO
- M20-JP-116 — TCK QueryTests — TODO
- M20-JP-117 — TCK SignatureTests — TODO
- M20-JP-118 — Full TCK run 1745/1745 — TODO

---

## Session Log
- M0: skeleton + 9 sub-modules, TCK harness baseline 2/0/2 PASS
- M1: APT metamodel generation (_Entity + Entity_), 17/17 tests
- M2: Maven plugin tier-2 + external-it 15/15, proves tier-1 ~ tier-2
- M3: runtime Class-File API fallback, 17/17 tests
- M4-JP-23: PersistenceProvider parses persistence.xml, 7/7 tests, Sonar clean on new code
- M4-JP-24: EMF manages EMs, cascade close, 19/19 tests (43/43 module)
- M4-JP-25: EM delegates to persistence context, 28/28 tests (71/71 module)
- M4-JP-26: persistence context state machine + identity map, 18/18 tests (89/89 module)
- M4-JP-27: transaction integration (RESOURCE_LOCAL + JTA), 8/8 tests (221/221 module), Sonar OK
- M4-JP-28: connection management (runWithConnection/callWithConnection), 223/223 module, Sonar OK
- M5-JP-29: EntityMapper CRUD via dialect SPI, 239/239 module, Sonar clean on changed files
