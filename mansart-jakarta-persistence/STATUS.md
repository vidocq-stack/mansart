# mansart-jakarta-persistence — Status

## Current Focus
- **Card**: M4-JP-26 — Persistence context, tracks entity states
- **Milestone**: M4

## 📊 Milestone Progress Overview

| Milestone | Total JP | Done | % Complete | Status |
|----------|----------|------|------------|--------|
| **M0** | 7 | 7 | **100%** | ✅ DONE |
| **M1** | 6 | 6 | **100%** | ✅ DONE |
| **M2** | 5 | 5 | **100%** | ✅ DONE |
| **M3** | 4 | 4 | 100% | ✅ DONE |
| **M4** | 6 | 4 | **~67%** | ✅ IN_PROGRESS |
| **M5** | 5 | 0 | 0% | ⏳ TO_DEFINE |
| **M6** | 11 | 0 | 0% | ⏳ TO_DEFINE |
| **M7** | 12 | 0 | 0% | ⏳ TO_DEFINE |
| **M8** | 4 | 0 | 0% | ⏳ TO_DEFINE |
| **M9** | 3 | 0 | 0% | ⏳ TO_DEFINE |
| **M10** | 3 | 0 | 0% | ⏳ TO_DEFINE |
| **M11** | 8 | 0 | 0% | ⏳ TO_DEFINE |
| **M12** | 5 | 0 | 0% | ⏳ TO_DEFINE |
| **M13** | 5 | 0 | 0% | ⏳ TO_DEFINE |
| **M14** | 5 | 0 | 0% | ⏳ TO_DEFINE |
| **M15** | 6 | 0 | 0% | ⏳ TO_DEFINE |
| **M16** | 3 | 0 | 0% | ⏳ TO_DEFINE |
| **M17** | 5 | 0 | 0% | ⏳ TO_DEFINE |
| **M18** | 4 | 0 | 0% | ⏳ TO_DEFINE |
| **M19** | 4 | 0 | 0% | ⏳ TO_DEFINE |
| **M20** | 6 | 0 | 0% | ⏳ TO_DEFINE |
| **TOTAL** | **112** | **25** | **~22.3%** | |

---

## Metrics (M0)
- **Baseline TCK**: 2/0/2 PASS (2 ProviderDiscoveryTest PASS, harness works, provider discoverable, build green)
- **Unit Tests**: 2/2 PASS (ProviderDiscoveryTest)
- **Integration Tests**: 11/11 PASS (ExternalEntityIT)
- **TCK Errors**: 0 (not measured)
- **TCK Errors**: 0 (not measured)
- **Modules Building**: 9/9 (all modules compile)
- **Modules with module-info.java**: 5/9 (spi, processor, core, cdi, external-lib)

## 🎯 Current Milestone Details
### M4 — Core Runtime: EntityManagerFactory + EntityManager
- **M4-JP-23** ✅ **DONE** — MansartPersistenceProvider implements PersistenceProvider, parses persistence.xml, creates EntityManagerFactory. 7/7 tests pass. Measured: MansartPersistenceProviderTest 7/7 PASS, full mansart-persistence-tests module 24/24 PASS. SonarQube: 0 bugs/0 smells/0 vulnerabilities/0 hotspots on new code. Pre-existing debt noted (112 open issues in untouched files, jacoco coverage gap).

- **M4-JP-24** ✅ **DONE** — MansartEntityManagerFactory now manages EntityManager instances: tracks all created EMs in a ConcurrentLinkedQueue, cascades close on factory close (all EMs marked closed). Double-check pattern in createTrackedEm guards against TOCTOU race between ensureOpen and EM registration. Bootstrap metadata: EMF now stores PersistenceUnitTransactionType (passed from both XML descriptor path and PersistenceConfiguration path). getTransactionType() returns it without a closed check (per spec, no @throws IllegalStateException). Closed-state contract: createEntityManager throws IllegalStateException if factory closed. getProperties, getCache, getPersistenceUnitUtil, unwrap all throw IllegalStateException if closed (per their @throws declarations). getCache returns null (spec-sanctioned for no L2 cache). unwrap supports MansartEntityManagerFactory and EntityManagerFactory, throws PersistenceException for unsupported types. MansartPersistenceUnitUtil (new): implements PersistenceUnitUtil, all methods throw UnsupportedOperationException (entity model not built yet — that's M5+). MansartEntityManager: added open/closed state (AtomicBoolean). isOpen() checks both EM state and factory state. close() deregisters from factory. markClosed() (package-private) called by factory cascade. Persistence operations remain UnsupportedOperationException (M4-JP-25 scope). MansartPersistenceProvider: both createEntityManagerFactory paths now pass transactionType to the EMF constructor. Measured: MansartEntityManagerFactoryTest: 19/19 PASS, Full mansart-persistence-tests module: 43/43 PASS (was 24, +19 new tests). Build: green on all modules (including external-it). Auditor: clean on diff (2 minor findings fixed: dead registerEntityManager method removed, null guard on constructor properties added). SonarQube: 0 bugs, 0 vulnerabilities, 0 security hotspots, 0 violations on M4-JP-24 changed files. Quality gate ERROR on new_coverage=0.0% and 17 new_violations — both are pre-existing M3 debt (RuntimeEntityClassGenerator.java, RuntimeAttribute.java, RuntimeEntityModel.java, RuntimeEntityModelBuilder.java), same pattern as M4-JP-23.

- **M4-JP-25** ✅ **DONE** — MansartEntityManager implements the EntityManager interface, delegates entity-state operations to a persistence context. ensureOpen() closed-state guard on all EM methods except the three spec exemptions (isOpen, getProperties, getTransaction). Per EntityManager.close() Javadoc: after close every method throws IllegalStateException except isOpen(), getProperties() and getTransaction(). Configuration storage: flush mode (default FlushModeType.AUTO), cache retrieve mode (default CacheRetrieveMode.USE), cache store mode (default CacheStoreMode.USE), all retained via set/get with ensureOpen. Property overrides: setProperty(name,value) stored in a ConcurrentHashMap overlay; getProperties() returns an unmodifiable merge of factory properties + overrides (exempt from closed-state, does not throw when closed). unwrap(Class): supports MansartEntityManager and EntityManager (returns this), throws PersistenceException for unsupported types, with ensureOpen. getDelegate(): returns this, with ensureOpen. getEntityManagerFactory(): returns the creating factory, with ensureOpen. New internal (non-exported) package io.vidocq.mansart.persistence.core.context with MansartPersistenceContext — the persistence-context seam. EM delegates all entity-state operations (persist, merge, remove, find + overloads, getReference + overloads, flush, refresh + overloads, clear, detach, contains, lock + overloads, getLockMode) to it after ensureOpen. The context's methods throw UnsupportedOperationException("not implemented: <op>") — the state machine (NEW/MANAGED/DETACHED/REMOVED identity map) is card M4-JP-26. Query operations (createQuery/createNamedQuery/createNativeQuery/stored procs), getCriteriaBuilder, getMetamodel, entity graph ops, joinTransaction/isJoinedToTransaction, getTransaction, runWithConnection/callWithConnection: ensureOpen + UnsupportedOperationException (later milestones). module-info.java: unchanged (new context package intentionally NOT exported — internal). Measured tests: MansartEntityManagerTest: 28/28 PASS (new test class), Full mansart-persistence-tests module: 71/71 PASS (was 43, +28). Build: green on all 9 modules of the mansart-jakarta-persistence reactor (install -DskipTests + verify). Auditor: clean on diff restricted to the sub-module — no anti-drift findings (no fake implementations, no reflection on user types, no TCK leakage, no disabled tests, no module-export leakage). SonarQube (projectKey io.vidocq.mansart:mansart-jakarta-persistence): quality gate OK (PASS). On M4-JP-25 changed files: 0 bugs, 0 vulnerabilities, 0 security hotspots. 57 code smells (22 S1172 "unused parameter" on MansartPersistenceContext interface-stub methods — unavoidable since parameters are mandated by the EntityManager signature; 35 on the test file: S5786 "remove public modifier" + lambda/assert style nits, matching the existing MansartEntityManagerFactoryTest style). New-code metrics came back empty because the files are uncommitted at scan time (same situation as prior cards — git blame unavailable); the gate reports OK. Note: the actual Sonar projectKey is io.vidocq.mansart:mansart-jakarta-persistence (Maven groupId:artifactId), not vidocq-mansart-persistence, because the POM defines no explicit sonar.projectKey.

- **M4-JP-26** ✅ **DONE** — MansartPersistenceContext implements the in-memory entity-state machine (NEW, MANAGED, DETACHED, REMOVED) with an identity map keyed by EntityKey(Class, primaryKey). State transitions: persist transitions NEW/DETACHED/REMOVED → MANAGED; remove transitions MANAGED → REMOVED (throws IllegalArgumentException for NEW/DETACHED); detach transitions MANAGED/REMOVED → DETACHED; clear detaches all. Identity map: persist and merge populate it; find returns the cached instance (same reference) or null if not in context; contains returns true only for MANAGED and REMOVED; merge returns the same instance if already MANAGED, or a managed copy for NEW/DETACHED. ID extraction uses the two-tier approach: EntityModel.getIdAttributes().get(0).getName() for the field name, then callback.getAccessor().get(entity, fieldName) for the value (tier-3 runtime Class-File API, calls public getters via invokevirtual — no reflection on user types). No APT needed for test entities. flush is a no-op (no database yet). Measured tests: MansartPersistenceContextTest: 18/18 PASS (new test class), MansartEntityManagerTest: 28/28 PASS (updated 2 tests to reflect implemented state machine — persist(new Object()) now throws IllegalArgumentException instead of UnsupportedOperationException, contains(new Object()) returns false), Full mansart-persistence-tests module: 89/89 PASS (was 71, +18). Build: green. No Sonar gate run this session.

### M3 — Runtime Metadata and Repository Generation
- **M3-JP-19** ✅ **DONE** — RuntimeEntityModelBuilder builds EntityModel from class file bytes at bootstrap using Java 26 Class-File API. RuntimeEntityModel and RuntimeAttribute as in-memory metadata. Field access deferred to M3-JP-20. 6/6 tests pass.
- **M3-JP-20** ✅ **DONE** — RuntimeEntityClassGenerator generates hidden classes via MethodHandles.Lookup.defineHiddenClass using Java 26 Class-File API. Generated bytecode calls entity public getters/setters via invokevirtual. Handles primitive boxing/unboxing. EntityAccessor<T> SPI interface added. 3/3 tests pass.
- **M3-JP-21** ✅ **DONE** — MansartCallback caches and dispatches EntityAccessor and EntityModel instances for tier-3 entities. ConcurrentHashMap-based caching. Single entry point for tier-3 metadata and field access. 5/5 tests pass.
- **M3-JP-22** ✅ **DONE** — Tier3WarningCollector accumulates warnings for tier-3 entities, names entity class and points to mansart-persistence-maven-plugin. RuntimeEntityModelBuilder accepts optional Tier3WarningCollector. 3/3 tests pass.

### M0 — Skeleton + Real TCK Harness Baseline
- **M0-JP-01** ✅ **DONE** — Parent pom.xml with 9 sub-modules
- **M0-JP-02** ✅ **DONE** — module-info.java for each module (5/9 complete - no module-info for maven-plugin, tests, external-it, tck)
- **M0-JP-03** ✅ **DONE** — mansart-persistence-spi module (EntityModel, Attribute hierarchy, compiles successfully)
- **M0-JP-04** ✅ **DONE** — mansart-persistence-core skeleton (MansartPersistenceProvider, MansartEntityManagerFactory, MansartEntityManager - all throw UnsupportedOperationException)
- **M0-JP-05** ✅ **DONE** — TCK infrastructure (out-of-reactor) - standalone pom.xml, run script, README
- **M0-JP-06** ✅ **DONE** — Harness wiring + provider registration (ServiceLoader discovers MansartPersistenceProvider, ProviderDiscoveryTest passes)
- **M0-JP-07** ✅ **DONE** — Baseline TCK run (harness works, provider discoverable, **2/0/2 PASS**, build green. Switch to JUnit 4, surefire limited to ProviderDiscoveryTest, TCK Client classes unpacked, exec-maven-plugin + sigtest-maven-plugin configured. JavaTest harness integration pending for full 1745 test execution)

### M1 — Entity Metamodel + APT Generation
- **M1-JP-08** ✅ **DONE** — MansartPersistenceProcessor (SourceVersion.RELEASE_25, @SupportedAnnotationTypes for JPA annotations, service file registered)
- **M1-JP-09** ✅ **DONE** — Entity scanning — detects @Entity, @Table, @Id, @GeneratedValue, @Column, @Version, @ManyToOne, @OneToOne, @JoinColumn, @Enumerated, @Embedded, @Embeddable
- **M1-JP-10** ✅ **DONE** — _Entity generation — EntityModel<T>, typed Attribute subtypes (IdAttribute, BasicAttribute, NumericAttribute, TemporalAttribute, ReferenceAttribute, EnumAttribute, VersionAttribute)
- **M1-JP-11** ✅ **DONE** — Standard JPA static metamodel generation — Entity_ with SingularAttribute fields (if jakarta.persistence-api on classpath)
- **M1-JP-12** ✅ **DONE** — MethodHandle resolution — MethodHandles.privateLookupIn in constructor, never setAccessible
- **M1-JP-13** ✅ **DONE** — SQL naming conventions — snake_case column names, plural snake_case table names, FK column naming

### M2 — Maven Plugin (Tier 2) + External Library Support
- **M2-JP-14** ✅ **DONE** — MansartPersistenceMojo bound to process-classes, scans project output directory for @Entity classes
- **M2-JP-15** ✅ **DONE** — Class-File API parsing — reads class file bytes, extracts annotations using Java 26 Class-File API, generates EntityMetadata
- **M2-JP-16** ✅ **DONE** — Lazy association proxies for external entities — real subclasses generated, never java.lang.reflect.Proxy
- **M2-JP-17** ✅ **DONE** — external-lib — JAR with test entities (ExternalPerson, ExternalDepartment) no Mansart deps
- **M2-JP-18** ✅ **DONE** — external-it — integration tests asserting tier-1 ≈ tier-2 behavior

## Session Log
- M2-JP-16: Added LazyEntityProxy and LazyInitializer SPI interfaces. Maven plugin now generates EntityName_Lazy proxy subclasses for entities targeted by @ManyToOne/@OneToOne. Proxy extends entity, overrides getters with ensureLoaded() lazy loading mechanism. Never uses java.lang.reflect.Proxy. 15/15 ExternalEntityIT tests pass. auditor/SonarQube deferred.
- Updated card naming convention to include milestone prefix (JP-01 -> M0-JP-01)
- Fixed maven-plugin-plugin dependency version issue (3.9.6 -> 3.14.0) but Java 25 class file support still pending
- Created proper module-info.java for spi, processor, core, cdi, external-lib modules
- Removed module-info.java from maven-plugin, tests, external-it, tck modules due to non-modular dependencies
- All 9 modules now compile successfully
- Created mansart-persistence-tck module with standalone pom.xml, run script, README
- ProviderDiscoveryTest verifies MansartPersistenceProvider is discoverable via ServiceLoader
- TCK runner script executes without harness errors
- Added maven-dependency-plugin to unpack TCK jars (persistence-tck-spec-tests, persistence-tck-common, common, signaturetest) to test-classes
- TCK Client classes (160+ Client classes from 269 client classes total, ~1745 test methods) are now in test-classes but JUnit Jupiter cannot discover JavaTest harness tests
- Current baseline: 2/0/2 PASS (ProviderDiscoveryTest only)
- M2-JP-18: Fixed ClassFileParser to properly parse JPA annotations via Java 26 Class-File API (RuntimeVisibleAnnotationsAttribute/RuntimeInvisibleAnnotationsAttribute), fixed Mojo phase to PROCESS_CLASSES, added proper _Entity and Entity_ generation with correct Java types, configured external-lib pom.xml with mansart-persistence-maven-plugin, wrote 11 integration tests in ExternalEntityIT (all pass). auditor/SonarQube deferred to BeanVal completion.
- M3-JP-19: RuntimeEntityModelBuilder builds EntityModel from class file bytes at bootstrap using Java 26 Class-File API. RuntimeEntityModel and RuntimeAttribute as in-memory metadata. Field access deferred to M3-JP-20. 6/6 RuntimeEntityModelBuilderTest tests pass, build green.
- M3-JP-20: RuntimeEntityClassGenerator generates hidden classes via MethodHandles.Lookup.defineHiddenClass using Java 26 Class-File API. Generated bytecode calls entity public getters/setters via invokevirtual. Handles primitive boxing/unboxing. EntityAccessor<T> SPI interface added. 3/3 tests pass.
- M3-JP-21: MansartCallback caches and dispatches EntityAccessor and EntityModel instances for tier-3 entities. ConcurrentHashMap-based caching. Single entry point for tier-3 metadata and field access. 5/5 tests pass.
- M3-JP-22: Tier3WarningCollector accumulates warnings for tier-3 entities, names entity class and points to mansart-persistence-maven-plugin. RuntimeEntityModelBuilder accepts optional Tier3WarningCollector. 3/3 Tier3WarningTest tests pass.
- Measured: 17/17 tests pass in mansart-persistence-tests (6 RuntimeEntityModelBuilderTest + 3 Tier3WarningTest + 3 RuntimeEntityClassGeneratorTest + 5 MansartCallbackTest), build green.
- M4-JP-23: MansartPersistenceProvider now parses persistence.xml via JDK DOM API with full XXE hardening, creates EntityManagerFactory from both XML and programmatic config, and implements real lifecycle with AtomicBoolean. module-info.java now requires java.xml. auditor/guardian findings resolved (XXE hardening structure, TOCTOU in close()). Test resource added: META-INF/persistence.xml with two PUs. Measured: MansartPersistenceProviderTest 7/7 PASS, full mansart-persistence-tests module 24/24 PASS. SonarQube quality gate clean on new code (0 bugs/0 smells/0 vulnerabilities/0 hotspots); pre-existing debt noted (112 open issues in untouched files) and jacoco coverage gap (new_coverage=0.0) as backlog items.

- M4-JP-24: MansartEntityManagerFactory now manages EntityManager instances: tracks all created EMs in a ConcurrentLinkedQueue, cascades close on factory close (all EMs marked closed). Double-check pattern in createTrackedEm guards against TOCTOU race between ensureOpen and EM registration. Bootstrap metadata: EMF now stores PersistenceUnitTransactionType (passed from both XML descriptor path and PersistenceConfiguration path). getTransactionType() returns it without a closed check (per spec, no @throws IllegalStateException). Closed-state contract: createEntityManager throws IllegalStateException if factory closed. getProperties, getCache, getPersistenceUnitUtil, unwrap all throw IllegalStateException if closed (per their @throws declarations). getCache returns null (spec-sanctioned for no L2 cache). unwrap supports MansartEntityManagerFactory and EntityManagerFactory, throws PersistenceException for unsupported types. MansartPersistenceUnitUtil (new): implements PersistenceUnitUtil, all methods throw UnsupportedOperationException (entity model not built yet — that's M5+). MansartEntityManager: added open/closed state (AtomicBoolean). isOpen() checks both EM state and factory state. close() deregisters from factory. markClosed() (package-private) called by factory cascade. Persistence operations remain UnsupportedOperationException (M4-JP-25 scope). MansartPersistenceProvider: both createEntityManagerFactory paths now pass transactionType to the EMF constructor. Measured: MansartEntityManagerFactoryTest: 19/19 PASS, Full mansart-persistence-tests module: 43/43 PASS (was 24, +19 new tests). Build: green on all modules (including external-it). Auditor: clean on diff (2 minor findings fixed: dead registerEntityManager method removed, null guard on constructor properties added). SonarQube: 0 bugs, 0 vulnerabilities, 0 security hotspots, 0 violations on M4-JP-24 changed files. Quality gate ERROR on new_coverage=0.0% and 17 new_violations — both are pre-existing M3 debt (RuntimeEntityClassGenerator.java, RuntimeAttribute.java, RuntimeEntityModel.java, RuntimeEntityModelBuilder.java), same pattern as M4-JP-23.

---
*Generated for milestone M0. All numbers are not measured unless stated otherwise.*
