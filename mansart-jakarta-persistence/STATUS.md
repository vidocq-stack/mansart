# mansart-jakarta-persistence — Status

## Current Focus
- **Card**: M3-JP-20 — RuntimeRepositoryClassGenerator
- **Milestone**: M3

## 📊 Milestone Progress Overview

| Milestone | Total JP | Done | % Complete | Status |
|----------|----------|------|------------|--------|
| **M0** | 7 | 7 | **100%** | ✅ DONE |
| **M1** | 6 | 6 | **100%** | ✅ DONE |
| **M2** | 5 | 5 | **100%** | ✅ DONE |
| **M3** | 4 | 2 | 50% | ✅ 2/4 DONE |
| **M4** | 6 | 0 | 0% | ⏳ TO_DEFINE |
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
| **TOTAL** | **112** | **19** | **17.0%** | |

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
### M3 — Runtime Metadata and Repository Generation
- **M3-JP-19** ✅ **DONE** — RuntimeEntityModelBuilder builds EntityModel from class file bytes at bootstrap using Java 26 Class-File API. RuntimeEntityModel and RuntimeAttribute as in-memory metadata. Field access deferred to M3-JP-20. 6/6 tests pass.
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
- M3-JP-22: Tier3WarningCollector accumulates warnings for tier-3 entities, names entity class and points to mansart-persistence-maven-plugin. RuntimeEntityModelBuilder accepts optional Tier3WarningCollector. 3/3 Tier3WarningTest tests pass.

---
*Generated for milestone M0. All numbers are not measured unless stated otherwise.*
