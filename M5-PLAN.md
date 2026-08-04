# M5 - Query Implementation Plan

## Objective
Implement JPQL query support in MansartEntityManager using the fixed JpqlParser.

## Status
- **M4 Complete**: All JpqlParser tests pass (36/36)
- **M4 Complete**: EntityManager basic operations work (persist, merge, remove, find)
- **Ready for M5**: Query implementation

## Current State

### Working
- `MansartEntityManager` with RepositoryRuntime integration
- `JpqlParser` parses all JPQL constructs:
  - SELECT queries with JOIN, LEFT JOIN, INNER JOIN
  - WHERE clauses with AND/OR/NOT
  - Parenthesized predicates
  - EXISTS/NOT EXISTS subqueries
  - Comparison operators (=, <>, <, <=, >, >=)
  - IS NULL, IS NOT NULL
  - LIKE, IN, BETWEEN
  - GROUP BY, HAVING, ORDER BY
  - Function calls (COUNT, etc.)
  - Path expressions
  - Numeric, string, boolean literals
  - Named and positional parameters

### Not Implemented
- `EntityManager.createQuery(String qlString)` - throws UnsupportedOperationException
- `EntityManager.createQuery(CriteriaQuery)` - throws UnsupportedOperationException
- `EntityManager.createNamedQuery(String name)` - throws UnsupportedOperationException
- `EntityManager.createNativeQuery(String sqlString)` - throws UnsupportedOperationException
- Criteria API implementation

## M5 Tasks

### Phase 1: Basic JPQL Query Support (Priority: HIGH)
- [ ] Implement `MansartQuery` class implementing `jakarta.persistence.Query`
- [ ] Implement `MansartTypedQuery<T>` class implementing `jakarta.persistence.TypedQuery<T>`
- [ ] Implement `EntityManager.createQuery(String qlString)`
  - Parse JPQL string using `JpqlParser.parse()`
  - Return `MansartQuery` instance
- [ ] Implement `EntityManager.createQuery(String qlString, Class<T> resultClass)`
  - Parse and return `MansartTypedQuery<T>`
- [ ] Basic query execution via RepositoryRuntime

### Phase 2: Query Result Processing (Priority: HIGH)
- [ ] Map parsed JPQL to RepositoryRuntime operations
- [ ] Handle SELECT clause projection
- [ ] Handle WHERE clause filtering
- [ ] Handle JOIN operations
- [ ] Handle ORDER BY
- [ ] Handle GROUP BY and HAVING
- [ ] Handle aggregate functions (COUNT, SUM, AVG, MIN, MAX)

### Phase 3: Advanced Query Features (Priority: MEDIUM)
- [ ] Implement named parameters binding (`setParameter(String name, Object value)`)
- [ ] Implement positional parameters binding (`setParameter(int position, Object value)`)
- [ ] Implement result pagination (`setFirstResult`, `setMaxResults`)
- [ ] Implement flush mode
- [ ] Implement lock modes
- [ ] Implement hint processing

### Phase 4: Criteria API (Priority: MEDIUM)
- [ ] Implement `MansartCriteriaBuilder`
- [ ] Implement `MansartCriteriaQuery<T>`
- [ ] Implement `MansartRoot<T>` for FROM clause
- [ ] Implement `MansartPath<T>` for path navigation
- [ ] Implement `MansartPredicate` for WHERE conditions
- [ ] Implement `MansartExpression` for SELECT expressions
- [ ] Implement `MansartJoin` for JOIN operations

### Phase 5: Named Queries (Priority: LOW)
- [ ] Implement `@NamedQuery` annotation processing
- [ ] Implement `EntityManager.createNamedQuery(String name)`
- [ ] Store named queries in EntityManagerFactory

### Phase 6: Native Queries (Priority: LOW)
- [ ] Implement `EntityManager.createNativeQuery(String sqlString)`
- [ ] Direct SQL execution via DataSource

### Phase 7: Update/Delete Queries (Priority: MEDIUM)
- [ ] Implement UPDATE queries via `JpqlUpdateStmt`
- [ ] Implement DELETE queries via `JpqlDeleteStmt`
- [ ] Map to RepositoryRuntime update/delete operations

## Implementation Strategy

### Step 1: Create Query Infrastructure
```
mansart-jakarta-persistence/mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/
├── MansartQuery.java           # Base Query implementation
├── MansartTypedQuery.java      # TypedQuery implementation
├── MansartQueryImpl.java       # Internal implementation
├── QueryExecutor.java          # Executes parsed queries
└── QueryResultMapper.java       # Maps results to entities/DTOs
```

### Step 2: Integrate JpqlParser
```java
// In MansartEntityManager.createQuery()
public Query createQuery(String jpqlString) {
    JpqlStmt stmt = JpqlParser.parse(jpqlString);
    if (stmt instanceof JpqlSelectStmt) {
        return new MansartQueryImpl((JpqlSelectStmt) stmt, this);
    } else if (stmt instanceof JpqlUpdateStmt) {
        return new MansartUpdateQueryImpl((JpqlUpdateStmt) stmt, this);
    } else if (stmt instanceof JpqlDeleteStmt) {
        return new MansartDeleteQueryImpl((JpqlDeleteStmt) stmt, this);
    }
    throw new IllegalArgumentException("Unsupported JPQL statement");
}
```

### Step 3: Map JPQL AST to RepositoryRuntime
The `JpqlSelectStmt` contains:
- selectExpressions: List<JpqlExpr>
- fromClause: JpqlFromClause (with fromItems and joins)
- whereClause: Optional<JpqlWhereClause>
- groupByClause: Optional<JpqlGroupByClause>
- havingClause: Optional<JpqlHavingClause>
- orderByClause: Optional<JpqlOrderByClause>

Need to map these to RepositoryRuntime operations.

## Dependencies

- ✅ mansart-jakarta-data/mansart-data-query (JpqlParser - DONE)
- ✅ mansart-jakarta-data/mansart-data-core (RepositoryRuntime - EXISTS)
- ✅ mansart-jakarta-persistence/mansart-persistence-core (MansartEntityManager - EXISTS)

## Testing Strategy

1. Create `MansartQueryTest.java` with tests for:
   - Simple SELECT queries
   - Queries with WHERE clause
   - Queries with JOIN
   - Queries with parameters
   - Queries with ORDER BY
   - Queries with GROUP BY and HAVING
   - UPDATE queries
   - DELETE queries
   - Named queries

2. Extend `MansartEntityManagerIntegrationTest.java` with query tests

## Success Criteria

- [ ] All existing tests continue to pass
- [ ] New query tests pass
- [ ] JPQL queries execute correctly against H2
- [ ] Parameters binding works
- [ ] Results are correctly mapped to entities

## Estimated Effort

- Phase 1: 2-4 hours
- Phase 2: 4-8 hours
- Phase 3: 2-4 hours
- Phase 4: 8-16 hours (Criteria API is complex)
- Phase 5: 2-4 hours
- Phase 6: 2-4 hours
- Phase 7: 2-4 hours

**Total: ~16-44 hours depending on scope**

## Priority Order

1. **Phase 1 + 2** (Basic JPQL SELECT) - Enable basic query support
2. **Phase 7** (UPDATE/DELETE) - Complete DML support
3. **Phase 3** (Advanced features) - Parameters, pagination
4. **Phase 4** (Criteria API) - Type-safe querying
5. **Phase 5 + 6** (Named/Native) - Nice to have

## Next Steps

Start with Phase 1:
1. Create `MansartQuery` interface/implementation
2. Implement `EntityManager.createQuery(String)` 
3. Add basic execution logic
4. Test with simple SELECT queries

---

*Branch: feature/mansart-jakarta-persistence-working*
*Created: 2026-08-03*
*Status: Ready to start*
