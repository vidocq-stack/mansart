# Mansart Jakarta Persistence 3.2

Mansart implementation of Jakarta Persistence 3.2 (JPA) for the Vidocq ecosystem.

## Overview

This module provides a complete implementation of the Jakarta Persistence 3.2 specification, 
integrated with the Vidocq ecosystem. It builds upon the existing Mansart Data infrastructure
and provides:

- Full JPA 3.2 API support
- Compile-time bytecode enhancement using ClassFile API (JEP 484)
- Static metamodel generation
- CDI 4.1 integration via Build Compatible Extensions
- Virtual Threads support using ScopedValue
- GraalVM/Leyden compatibility

## Module Structure

```
mansart-jakarta-persistence/
├── mansart-persistence-api/           # Re-export of Jakarta Persistence API 3.2
├── mansart-persistence-spi/          # Internal SPI interfaces
├── mansart-persistence-core/          # Core runtime implementation
├── mansart-persistence-processor/    # APT: static metamodel + bytecode enhancement
├── mansart-persistence-cdi/          # CDI 4.1 integration
├── mansart-persistence-tests/        # Unit and integration tests
└── mansart-persistence-tck/           # Jakarta Persistence 3.2 TCK (test only)
```

## Runtime Extensions

```
vidocq-runtime-extensions-jakartaee-web/
├── vidocq-runtime-mansart-persistence-extension/     # Vidocq integration
└── vidocq-runtime-mansart-persistence-extension-codegen/ # APT bundle
```

## Current Status (M1: Skeleton)

✅ **Completed for M1:**
- Project structure and POM configuration
- Basic SPI interfaces (PersistenceEnhancer, ProxyFactory, PersistenceDialect)
- Skeleton implementations of core JPA interfaces:
  - `MansartPersistenceProvider`
  - `MansartEntityManagerFactory` / `DefaultMansartEntityManagerFactory`
  - `MansartProviderUtil`
- APT processor skeleton (`MansartPersistenceProcessor`)
- CDI Build Compatible Extension skeleton (`MansartPersistenceExtension`)
- Vidocq Runtime integration extension (`MansartPersistenceIntegrationExtension`)
- All modules compile successfully

⏳ **To be implemented in future milestones:**
- M2: Static metamodel generation
- M3: Core runtime (CRUD operations, persistence context)
- M4: Bytecode enhancement (dirty tracking, lazy loading)
- M5-M6: JPQL parser and Criteria API
- M7: Transactions and cache
- M8-M13: Lifecycle, inheritance, TCK, etc.

## Building

```bash
# Build from mansart directory
cd mansart
mvn clean install -pl mansart-jakarta-persistence -DskipTests -Dlicense.skip=true

# Build runtime extensions
cd ../vidocq/vidocq-runtime-extensions/vidocq-runtime-extensions-jakartaee-web
mvn clean install -pl vidocq-runtime-mansart-persistence-extension,vidocq-runtime-mansart-persistence-extension-codegen -DskipTests -Dlicense.skip=true
```

## Architecture Decisions

### Bytecode Enhancement
- **Strategy**: Compile-time bytecode generation using ClassFile API (JEP 484)
- **Rationale**: 
  - No Java Agent required (unlike Hibernate)
  - GraalVM/Leyden compatible (AOT-friendly)
  - Virtual Threads friendly (ScopedValue usage)
  - Better performance (no runtime reflection)

### Dirty Tracking
- **Strategy**: Bitmask per class + field-level tracking
- **Rationale**: Memory efficient, fast to check, compatible with bytecode enhancement

### Lazy Loading
- **Strategy**: ScopedValue<PersistenceContext> + Dynamic proxies
- **Rationale**: Virtual Threads compatible (no ThreadLocal), non-blocking, natural JPA context integration

### Module System
- **Status**: Module-info.java files created but temporarily disabled (moved to .bak)
- **Rationale**: Will be enabled when all dependencies have proper module declarations

## Dependencies

### Jakarta EE
- `jakarta.persistence-api:3.2.0`
- `jakarta.cdi-api:4.1.0`
- `jakarta.transaction-api:2.0.1`
- `jakarta.inject-api:2.0.1`

### Mansart
- `mansart-data-core:0.3.0-SNAPSHOT`
- `mansart-data-dialect-spi:0.3.0-SNAPSHOT`
- `mansart-data-dialect-h2:0.3.0-SNAPSHOT`
- `mansart-data-dialect-postgresql:0.3.0-SNAPSHOT`

### Testing
- `junit-jupiter:6.0.3`
- `assertj-core:3.27.0`
- `h2:2.3.232`
- `postgresql:42.7.4`

## Next Steps

1. **M2: Static Metamodel Generation**
   - Implement `StaticMetamodelWriter` in processor
   - Generate `Entity_` classes with `SingularAttribute`, `PluralAttribute`
   - Handle inheritance, relationships
   - Add unit tests for metamodel generation

2. **M3: Core Runtime**
   - Implement `EntityManagerImpl` with basic CRUD
   - Implement `PersistenceContextImpl` with IdentityMap
   - Integrate with Mansart Data dialects
   - Add integration tests

3. **M4: Bytecode Enhancement**
   - Implement `EnhancedClassGenerator` using ClassFile API
   - Add dirty tracking (bitmask)
   - Add lazy loading support
   - Generate proxy classes

## Documentation

- [Plan Detailed](PLAN.md) - Complete implementation plan with milestones
- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/)
- [ClassFile API (JEP 484)](https://openjdk.org/jeps/484)
- [ScopedValue API (JEP 444)](https://openjdk.org/jeps/444)

## License

Disjunctive triple license: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later

See [LICENSE](../../LICENSE) for details.
