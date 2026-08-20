---
name: mansart-persistence
description: Architecture map, build/test commands, patterns and forbidden shortcuts for mansart-jakarta-persistence (Jakarta Persistence 3.2 implementation in the Vidocq ecosystem). Load when implementing or debugging EntityManager, queries, Criteria, metamodel, schema, APT processor or CDI integration.
---

# mansart-jakarta-persistence — working map

Target: Jakarta Persistence 3.2, official TCK green, Vidocq charter (strict Java Modules,
zero external deps, APT codegen, virtual threads, TDD). Master plan:
`MANSART_PERSISTENCE_3_2.md` (grep a section, never load whole). Live tracker:
`PERSISTENCE-STATUS.md`. Remediation backlog: `PERSISTENCE-DEBT.md`.

## Modules (reactor `mansart-jakarta-persistence/pom.xml`)

| Module | Java module | Role |
| --- | --- | --- |
| `mansart-persistence-api` | `io.vidocq.mansart.persistence.api` | Thin re-export / API helpers. |
| `mansart-persistence-spi` | `io.vidocq.mansart.persistence.spi` | Internal SPI: `EntityMetadata`, `AttributeMetadata`, … consumed by core, produced by APT. |
| `mansart-persistence-core` | `io.vidocq.mansart.persistence.core` | Runtime. Packages: `bootstrap` (`MansartPersistenceProvider`, `SimplePersistenceUnitInfo`), `runtime` (`MansartEntityManagerFactory`, `MansartEntityManager` ~1.8k lines, `MansartQuery`, `MansartNativeQuery`, `MansartStoredProcedureQuery`, `MansartSchemaManager`, `MansartMetamodel`, `IdGenerator`, `LifecycleCallbackManager`, `MansartEntityGraph`), `jpql` (`JPQLTokenizer` → `JPQLParser` → `JpqlToSqlConverter` → `JpqlExecutor`), `criteria` (`MansartCriteriaBuilder/Query/Update/Delete`), `cache`, `mapping`. Depends on `mansart-data-dialect-spi/h2/postgresql` (shared SQL dialects) and `mansart-transactions-core`. |
| `mansart-persistence-processor` | `io.vidocq.mansart.persistence.processor` | APT (`RELEASE_25`): `parse/*Parser` (Id, Basic, Column, GeneratedValue, relationships, inheritance, NamedQuery) → `metadata/EntityMetadataGenerator`, `StaticMetamodelGenerator` → `writer`. Generates `_Entity` metamodel + `EntityMetadata` implementations as Java sources. |
| `mansart-persistence-cdi` | `io.vidocq.mansart.persistence.cdi` | Vauban Build Compatible Extension + producers (mirror of `mansart-data-cdi`). |
| `mansart-persistence-tests` | — | Integration tests (H2). |
| `mansart-persistence-tck` | — | **Out of reactor.** Official TCK runner (see skill `mansart-persistence-tck`). |

Unit tests today live in `mansart-persistence-core/src/test` (`BasePersistenceTest` +
13 test classes: CRUD, relationships, inheritance, lazy, dirty, locking, callbacks,
listeners, named/native/stored-procedure queries, JPQL GROUP BY). Test entities are
currently in `core/src/main/.../testentities` and exported — known debt, do not add more
there; new test entities go to `src/test`.

## Commands (from `mansart/`, Java 25 + Maven 3.9.16 via `sdk env`)

```bash
./mvnw -ntp -pl mansart-jakarta-persistence/mansart-persistence-core test -Dtest=LockingTest   # one unit test
./mvnw -ntp -pl mansart-jakarta-persistence/mansart-persistence-core -am install -DskipTests   # rebuild core + deps
./mvnw -ntp clean install            # VALIDATION GATE (all modules, through ctx tools)
```

Always route Maven output through `ctx_batch_execute`/`ctx_execute`; grep for
`BUILD SUCCESS|BUILD FAILURE|Tests run:.*Failures: [1-9]|ERROR\]`. Stale ECJ classes
(jdtls) → package-less `ClassNotFoundException` / "Unresolved compilation problems" →
`mvn clean`, never a module-config workaround.

## Patterns to follow

- **Per-entity behaviour = APT-generated source.** Metadata (`EntityMetadata`), static
  metamodel (`_Entity`), lazy-loading subclasses, dirty-tracking support, lifecycle
  callback dispatch: all emitted by `mansart-persistence-processor` via `Filer`, discovered
  at runtime through `ServiceLoader`/registries — never `Class.forName` of generated
  names, never reflection on user classes. Class-File API only with written justification
  (ask `@classfile-codegen`).
- **SQL goes through the dialect SPI** (`mansart-data-dialect-spi` AST + `Dialect`);
  identifier quoting is the dialect's job (H2 vs PostgreSQL), never inline `"` or
  backticks in core.
- **Schema generation** (`MansartSchemaManager`) works from the registered
  `EntityMetadata` of the persistence unit (`<class>` list / scanned) — never from a
  hard-coded class list.
- **Unimplemented = `throw new UnsupportedOperationException("...")`**; implemented
  = tested. No `return null` placeholders, no "non-null stub" objects.
- **Transactions** via `mansart-transactions` (`ScopedValue`, no `ThreadLocal`);
  JDBC work on virtual threads, never inside `synchronized`.
- **Spec contract first**: ask `@spec-reader` (API Javadoc 3.2.0 + TCK sources) before
  implementing a method; exceptions (`IllegalArgumentException` vs
  `IllegalStateException` vs `TransactionRequiredException`) are part of the contract.

## Forbidden (found in the code base, being removed — see PERSISTENCE-DEBT.md)

`java.lang.reflect.Proxy` for Criteria objects · `MethodHandles` callback invocation ·
TCK class/table names in runtime (`MansartSchemaManager.createTablesForKnownClasses`)
· `return null` bodies in `MansartEntityManager`/criteria · test entities exported from
the production module · ticking tracker boxes on "fewer errors".

## Debug

`-Djakarta.persistence.schema-generation.database.action=…` is honoured; for SQL tracing
add a temporary `System.err` in the dialect executor and remove it before commit
(no logging framework dependency allowed). H2 console: `jdbc:h2:mem:<name>;DB_CLOSE_DELAY=-1`.
