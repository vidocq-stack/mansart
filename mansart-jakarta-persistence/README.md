# Mansart :: Jakarta Persistence 3.2

Mansart implementation of **Jakarta Persistence 3.2** specification.

## Module Structure

This reactor contains the following modules:

```
mansart-jakarta-persistence/
├── pom.xml                          # Parent POM for all persistence modules
├── mansart-persistence-api/         # Public API extensions (M7-2)
│   ├── pom.xml
│   └── src/main/java/io/vidocq/mansart/persistence/
│       └── PersistenceExtensions.java
├── mansart-persistence-spi/         # Service Provider Interface (M7-3)
│   ├── pom.xml
│   └── src/main/java/io/vidocq/mansart/persistence/spi/
│       └── PersistenceSpi.java
├── mansart-persistence-core/        # Core runtime implementation (M7-4+)
│   ├── pom.xml
│   ├── src/main/java/io/vidocq/mansart/persistence/core/
│   │   └── EntityState.java
│   └── src/main/resources/META-INF/services/
│       └── jakarta.persistence.spi.PersistenceProvider
├── mansart-persistence-processor/   # APT processor (M7-16)
│   └── pom.xml
├── mansart-persistence-cdi/         # CDI integration (M7-17, M9-7)
│   └── pom.xml
├── mansart-persistence-tests/       # Unit tests (M7-18)
│   └── pom.xml
└── mansart-persistence-tck/         # TCK runner (M7-19, out-of-reactor)
    └── pom.xml
```

## Build Status

| Module | Status | Notes |
|--------|--------|-------|
| mansart-persistence-api | ✅ Compiles | Stub implementation |
| mansart-persistence-spi | ✅ Compiles | Stub implementation |
| mansart-persistence-core | ✅ Compiles | Minimal structure, EntityState enum |
| mansart-persistence-processor | ⏳ Not started | APT for metamodel generation |
| mansart-persistence-cdi | ⏳ Not started | CDI Build Compatible Extension |
| mansart-persistence-tests | ⏳ Not started | Unit test infrastructure |
| mansart-persistence-tck | ⏳ Not started | TCK configuration |

## Getting Started

```bash
# Build all modules
cd mansart
mvn clean compile -pl mansart-jakarta-persistence -am -DskipTests

# Build a specific module
mvn clean compile -pl mansart-jakarta-persistence/mansart-persistence-api
```

## Implementation Milestones

### M7 - Bootstrap & Core JPA (Current)

- **M7-1** ✅ Create module structure
- **M7-2** Implement persistence-api
- **M7-3** Implement persistence-spi
- **M7-4** Implement MansartPersistenceProvider
- **M7-5** Implement EntityManagerFactory
- **M7-6** Implement MansartEntityManager (CRUD)
- **M7-7** Implement L1 Cache
- **M7-8** Implement EntityState management
- **M7-9** Integrate dialects
- **M7-10** Implement JPA annotations parsing
- **M7-11** Implement @Id, @GeneratedValue
- **M7-12** Implement entity mappings
- **M7-13** Implement basic JPQL
- **M7-14** Implement simple relationships
- **M7-15** Implement transaction management
- **M7-16** Create mansart-persistence-processor
- **M7-17** Static metamodel generation
- **M7-18** Create mansart-persistence-tests
- **M7-19** Configure mansart-persistence-tck
- **M7-20** Run TCK smoke
- **M7-21** Fix TCK Core failures

### M8 - Advanced JPQL & Criteria API

See `MANSART_PERSISTENCE_3_2.md` for full roadmap.

## Design Principles

This implementation follows the **Vidocq philosophy**:

1. **Java Modules**: Strict module system with minimal exports
2. **Virtual Threads**: ScopedValue only, never ThreadLocal
3. **Zero Reflection**: Static code generation (APT) for metamodel, entity support
4. **CDI Integration**: Build Compatible Extension via Vauban
5. **Reuse**: Leverage existing mansart-data dialects and mansart-transactions

## Key Interfaces

- `io.vidocq.mansart.persistence.core.MansartPersistenceProvider` - JPA entry point
- `io.vidocq.mansart.persistence.core.MansartEntityManagerFactory` - EMF implementation
- `io.vidocq.mansart.persistence.core.MansartEntityManager` - EM implementation

## Service Discovery

The `META-INF/services/jakarta.persistence.spi.PersistenceProvider` file registers
`io.vidocq.mansart.persistence.core.MansartPersistenceProvider` as the JPA provider.

## Dependencies

### Compile-time

- Jakarta Persistence API 3.2
- Jakarta Transaction API 2.0
- mansart-data-dialect-spi
- mansart-data-dialect-h2
- mansart-transactions-core

### Provided

- H2 Database (via mansart-data-dialect-h2)
- PostgreSQL JDBC (optional, via mansart-data-dialect-postgresql)

## License

Triple-licensed: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
