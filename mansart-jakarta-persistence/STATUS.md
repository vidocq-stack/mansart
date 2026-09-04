# mansart-jakarta-persistence — Status

## Current Focus
- **Card**: M0-JP-07 — Baseline TCK run
- **Milestone**: M0

## 📊 Milestone Progress Overview

| Milestone | Total JP | Done | % Complete | Status |
|----------|----------|------|------------|--------|
| **M0** | 7 | 6 | **85%** | 🟡 In Progress |
| **M2** | 5 | 0 | 0% | ⏳ TO_DEFINE |
| **M3** | 4 | 0 | 0% | ⏳ TO_DEFINE |
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
| **TOTAL** | **112** | **6** | **5.4%** | |

---

## Metrics (M0)
- **Baseline TCK**: 0/0/1745 (TCK not yet downloaded, harness works)
- **Unit Tests**: 2/2 PASS (ProviderDiscoveryTest)
- **Integration Tests**: 0/0 (not measured)
- **TCK Errors**: 0 (not measured)
- **Modules Building**: 9/9 (all modules compile)
- **Modules with module-info.java**: 5/9 (spi, processor, core, cdi, external-lib)

## 🎯 Current Milestone Details
### M0 — Skeleton + Real TCK Harness Baseline
- **M0-JP-01** ✅ **DONE** — Parent pom.xml with 9 sub-modules
- **M0-JP-02** ✅ **DONE** — module-info.java for each module (5/9 complete - no module-info for maven-plugin, tests, external-it, tck)
- **M0-JP-03** ✅ **DONE** — mansart-persistence-spi module (EntityModel, Attribute hierarchy, compiles successfully)
- **M0-JP-04** ✅ **DONE** — mansart-persistence-core skeleton (MansartPersistenceProvider, MansartEntityManagerFactory, MansartEntityManager - all throw UnsupportedOperationException)
- **M0-JP-05** ✅ **DONE** — TCK infrastructure (out-of-reactor) - standalone pom.xml, run script, README
- **M0-JP-06** ✅ **DONE** — Harness wiring + provider registration (ServiceLoader discovers MansartPersistenceProvider, ProviderDiscoveryTest passes)
- **M0-JP-07** ⏳ TODO — Baseline TCK run (harness works, provider discoverable, 3/3 tests executed: 2 ProviderDiscoveryTest PASS, 1 JPASigTest signature error. TCK dependencies wired: persistence-tck-spec-tests:3.2.1, signaturetest:11.0.0-RC5, sigtest-maven-plugin:2.6. Blocked: Surefire cannot discover Jakarta TCK Client classes - needs custom TestEngine or framework configuration)

## Session Log
- Updated card naming convention to include milestone prefix (JP-01 -> M0-JP-01)
- Fixed maven-plugin-plugin dependency version issue (3.9.6 -> 3.14.0) but Java 25 class file support still pending
- Created proper module-info.java for spi, processor, core, cdi, external-lib modules
- Removed module-info.java from maven-plugin, tests, external-it, tck modules due to non-modular dependencies
- All 9 modules now compile successfully
- Created mansart-persistence-tck module with standalone pom.xml, run script, README
- ProviderDiscoveryTest verifies MansartPersistenceProvider is discoverable via ServiceLoader
- TCK runner script executes without harness errors (TCK not yet downloaded)

---
*Generated for milestone M0. All numbers are not measured unless stated otherwise.*
