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

## Current Status (M4: EntityManager Implementation **COMPLETE**)

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

✅ **Completed for M2:**
- Static metamodel generation with `MansartMetamodelWriter`
- `Entity_` classes with `SingularAttribute`, `PluralAttribute`
- Attribute descriptors and type resolution
- JPA-standard metamodel generation via `JpaMetamodelWriter` (conditional on Jakarta Persistence API presence)

✅ **Completed for M3:**
- Core runtime implementation using Mansart Data components:
  - Integration with `RepositoryRuntime` for entity operations
  - Integration with `Dialect` for SQL generation
  - Integration with `ConnectionScope` for connection management
- Full `MansartEntityManager` implementation:
  - Persistence context (L1 cache) using `IdentityHashMap`
  - CRUD operations: `persist()`, `merge()`, `remove()`, `find()`, `refresh()`, `detach()`, `contains`
  - Entity state management and copy
  - Transaction management via `MansartEntityTransaction`
  - Entity model resolution via generated `_Entity` classes

✅ **Completed for M4:**
- `EntityModels` made public for runtime access (`lookup(Class)` method)
- `LazyHolder<T>` for thread-safe lazy loading (double-checked locking) in `mansart-data-core`
- Lazy loading enabled for REFERENCE attributes (`@ManyToOne`, `@OneToOne`) in processor
- Complete `MansartEntityManager` with all JPA lifecycle operations (100% of EntityManager interface)
- H2 DataSource creation for testing (in `DefaultMansartEntityManagerFactory`)
- Unit tests for `EntityCacheKey` and basic EntityManager operations
- Integration tests with real H2 database (CRUD, transactions, entity state)
- **All 38 tests pass** (0 failures)

🚀 **In Progress - M5: JPQL and Criteria API**
- Module `mansart-data-query` creation (AST, parser, execution)
- Extension of `Dialect` SPI with JPQL/Criteria rendering methods
- `MansartQuery` and `MansartTypedQuery` implementations
- `CriteriaBuilder`, `CriteriaQuery`, `Root`, `Path`, `Predicate` implementations
- Reuse of existing Mansart Data components:
  - `JdqlAst`/`JdqlParser` as base for JPQL AST
  - `Where`/`OrderBy`/`Pagination` for Criteria predicates
  - `RepositoryRuntime` for query execution
  - `RowMapper` for result mapping (extended for projections)

⏳ **Future Milestones:**
- M6: Complete JPQL parser (joins, subqueries, functions, expressions)
- M7: Transactions (L2 cache, optimistic locking with `@Version`)
- M8-M13: Lifecycle callbacks, inheritance strategies, TCK compliance, etc.

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

### Immediate (M5 - JPQL/Criteria API)
1. **Module Creation**: Create `mansart-data-query` module with:
   - `ast/` package: `JpqlAst` (sealed hierarchy), `JpqlParser`, `JpqlVisitor`
   - `criteria/` package: `MansartCriteriaBuilder`, `MansartCriteriaQuery`, `RootImpl`, `PathImpl`
   - `MansartQuery` and `MansartTypedQuery` implementations

2. **SPI Extension**: Extend `Dialect` interface (in `mansart-data-dialect-spi`) with:
   ```java
   SqlFragment renderJpql(String jpql, Map<String, Object> params, Class<?> resultType);
   SqlFragment renderCriteria(CriteriaQuery<?> query);
   ```

3. **Integration**: Update `MansartEntityManager` to use new query components:
   ```java
   @Override
   public Query createQuery(String jpql) {
       return new MansartQuery(jpql, this, dialect);
   }
   
   @Override
   public CriteriaBuilder getCriteriaBuilder() {
       return new MansartCriteriaBuilder(this, dialect);
   }
   ```

4. **Implementation**: 
   - JPQL parser (ANTLR or hand-written recursive descent)
   - AST to SQL visitor (reusing `Dialect` methods)
   - Criteria API builders (type-safe query construction)

### Short Term
- Complete JPQL parser (full JPA 3.2 syntax support)
- Add unit tests for JPQL parsing and execution
- Add integration tests with H2 for JPQL queries
- Extend `RowMapper` to support JPQL projections

### Medium Term
- M6: Complete Criteria API implementation
- M7: L2 cache implementation
- M8: Full inheritance support (SINGLE_TABLE, JOINED, TABLE_PER_CLASS)

## Documentation

- [Plan Detailed](PLAN.md) - Complete implementation plan with milestones
- [Jakarta Persistence 3.2 Specification](https://jakarta.ee/specifications/persistence/3.2/)
- [ClassFile API (JEP 484)](https://openjdk.org/jeps/484)
- [ScopedValue API (JEP 444)](https://openjdk.org/jeps/444)

## License

Disjunctive triple license: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later

See [LICENSE](../../LICENSE) for details.
