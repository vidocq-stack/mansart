# mansart-persistence 3.2 — Live status tracker

Compact scoreboard read at the START of every Vibe session and updated at the END
(protocol: `AGENTS.md` > "Working protocol"). The master plan is
`MANSART_PERSISTENCE_3_2.md` — **never load it whole**; grep only the section for the
current task. Bugs → `BUG.md` (`/log-bug`). Perf numbers → `BENCH.md` (`/log-bench`).

**Validation gate**: a checkbox below may only be ticked after a green FULL build from
the mansart root — `./mvnw -ntp clean install` — with BUILD SUCCESS on every module.

## Current focus

- **Milestone**: M7 — Bootstrap & Core JPA
- **Current task**: M7-2 — Implement mansart-persistence-api
- **Next up**: M7-3 (persistence-spi), M7-4 (PersistenceProvider), M7-5 (EntityManagerFactory)
- **Blockers**: none

## M7 — Bootstrap & Core JPA (critical)

- [x] M7-1 Create mansart-jakarta-persistence module structure (POMs, module-info)
- [ ] M7-2 Implement mansart-persistence-api
- [ ] M7-3 Implement mansart-persistence-spi
- [ ] M7-4 Implement MansartPersistenceProvider
- [ ] M7-5 Implement EntityManagerFactory
- [ ] M7-6 Implement MansartEntityManager (CRUD)
- [ ] M7-7 Implement L1 cache
- [ ] M7-8 Implement EntityState management
- [ ] M7-9 Integrate existing dialects (mansart-data-dialect-spi/h2/postgresql)
- [ ] M7-10 Implement JPA annotations parsing (APT)
- [ ] M7-11 Implement @Id, @GeneratedValue
- [ ] M7-12 Implement entity mappings
- [ ] M7-13 Implement basic JPQL (SELECT, WHERE)
- [ ] M7-14 Implement simple relationships
- [ ] M7-15 Implement transaction management (mansart-transactions integration)
- [ ] M7-16 Create mansart-persistence-processor
- [ ] M7-17 Static metamodel generation (APT)
- [ ] M7-18 Create mansart-persistence-tests
- [ ] M7-19 Configure mansart-persistence-tck (out-of-reactor)
- [ ] M7-20 Run TCK smoke
- [ ] M7-21 Fix TCK Core failures

**M7 deliverable**: full structure, Core JPA ~80%, TCK 400+ PASS.

## M8 — Advanced JPQL & Criteria API

- [ ] M8-1 GROUP BY and HAVING
- [ ] M8-2 JOIN syntax (INNER, LEFT, RIGHT)
- [ ] M8-3 Subqueries in FROM
- [ ] M8-4 ALL/ANY/SOME predicates
- [ ] M8-5 Additional JPQL functions
- [ ] M8-6 Criteria API
- [ ] M8-7 Inheritance SINGLE_TABLE
- [ ] M8-8 Inheritance JOINED
- [ ] M8-9 Inheritance TABLE_PER_CLASS
- [ ] M8-10 @OneToMany, @ManyToMany
- [ ] M8-11 Lazy loading (APT-generated proxy subclasses)
- [ ] M8-12 Dirty tracking (APT-generated support classes)
- [ ] M8-13 Complete APT processor
- [ ] M8-14 Named queries
- [ ] M8-15 Native queries
- [ ] M8-16 Complete tests
- [ ] M8-17 Run TCK by category
- [ ] M8-18 Fix TCK failures

**M8 deliverable**: full JPQL, Criteria API, TCK 1000+ PASS.

## M9 — Finalization

- [ ] M9-1 L2 cache (optional)
- [ ] M9-2 Bean Validation integration
- [ ] M9-3 Lifecycle callbacks
- [ ] M9-4 Entity listeners
- [ ] M9-5 Locking (optimistic / pessimistic)
- [ ] M9-6 Stored procedures
- [ ] M9-7 mansart-persistence-cdi (Vauban BCE, mirror mansart-data-cdi)
- [ ] M9-8 Optimizations
- [ ] M9-9 Full documentation (Antora, en + fr)
- [ ] M9-10 Full TCK

**M9 deliverable**: complete implementation, TCK 100% conformance.

## TCK scoreboard

| Date | Suite / category | Pass / Total | Notes |
| ---- | ---------------- | ------------ | ----- |
| —    | —                | —            | first run pending (M7-20) |

## Session log (newest first, one line per session)

<!-- Format: YYYY-MM-DD — <task id> — <outcome: what changed, tests state, bugs logged> -->
- 2026-08-13 — incident — MANSART-008: jdtls ECJ classes corrupted mansart-transactions `target/`; reverted Vibe's opens/add-reads workaround, killed orphaned jdtls, clean rebuild green (88 tests). Pitfall rule added to AGENTS.md.
- 2026-08-13 — M7-1 — mansart-jakarta-persistence module structure created: POMs, module-info.java, service file; all modules compile successfully
- 2026-08-13 — setup — Vibe configuration completed (agents, skills, hooks, MCP); tracker created.
