# M6: Complete JPQL Implementation Plan

## Objectives

M6 builds on M5's JPQL query execution foundation to provide a complete, production-ready JPQL implementation.

## Priority 1: Entity Name Resolution (Critical)

### Current State
- `QueryExecutionContext.tryResolveEntityClass()` uses hardcoded package scanning
- Fails for entities not in known packages
- No integration with JPA PersistenceUnit metadata

### Target Implementation
1. Create `EntityNameResolver` class in persistence-core
2. Register entity classes during EntityManagerFactory initialization
3. Support both simple names (from `@Entity`) and fully qualified names
4. Map entity names to EntityModel instances

### Files to Create/Modify
- `EntityNameResolver.java` (new)
- `MansartEntityManagerFactory` - add entity registration
- `QueryExecutionContext` - use EntityNameResolver instead of package scanning

## Priority 2: Parameter Binding (Critical)

### Current State
- `AbstractMansartQuery` stores `namedParameters` and `positionalParameters`
- Parameters are NOT passed to `RepositoryRuntime.queryList()`
- All queries execute with empty parameter lists

### Target Implementation
1. Extract bound parameters from query
2. Map named parameters to their values
3. Map positional parameters to their values
4. Pass parameters to `RepositoryRuntime.queryList()`

### Files to Modify
- `JpqlToRuntimeConverter.QueryExecutionParams` - add parameters field
- `MansartQuery.executeSelectQuery()` - extract and pass parameters
- `MansartTypedQuery.executeSelectQuery()` - extract and pass parameters

## Priority 3: Relationship Path Resolution (High)

### Current State
- `QueryExecutionContext.resolvePath()` throws UnsupportedOperationException for relationships
- Cannot resolve `b.author.name` where `author` is a `@ManyToOne` relationship

### Target Implementation
1. Use EntityModel to resolve relationship attributes
2. Navigate through relationships to find target entity
3. Return Attribute for the final path component
4. Support nested relationships (e.g., `b.author.address.city`)

### Files to Modify
- `QueryExecutionContext.resolvePath()` - implement relationship navigation

## Priority 4: IN Predicate Support (High)

### Current State
- `JpqlToRuntimeConverter.convertInPredicate()` returns `Where.ALWAYS_TRUE`
- No value list extraction from JPQL

### Target Implementation
1. Extract value list from JPQL IN clause
2. Create `Where.In` with proper arity
3. Bind parameter values to IN clause
4. Pass values to RepositoryRuntime

### Files to Modify
- `JpqlToRuntimeConverter.convertInPredicate()`

## Priority 5: EXISTS Subquery Support (Medium)

### Current State
- `JpqlToRuntimeConverter.convertExistsPredicate()` returns `Where.ALWAYS_TRUE`

### Target Implementation
1. Parse subquery from EXISTS predicate
2. Execute subquery independently
3. Create proper EXISTS condition
4. Integrate with main query WHERE clause

### Files to Modify
- `JpqlToRuntimeConverter.convertExistsPredicate()`

## Priority 6: JPQL Function Support (Medium)

### Target Functions
- String: UPPER, LOWER, TRIM, CONCAT, SUBSTRING, LENGTH
- Numeric: ABS, MOD, SQRT, SIZE (collections)
- Date: CURRENT_DATE, CURRENT_TIME, CURRENT_TIMESTAMP
- Aggregation: COUNT, MAX, MIN, AVG, SUM

### Files to Create/Modify
- `JpqlFunctionConverter.java` (new)
- `JpqlToRuntimeConverter` - add function conversion

## Task Breakdown

### Week 1: Entity Resolution & Parameters
- [ ] Create EntityNameResolver with entity name -> Class mapping
- [ ] Integrate entity registration in DefaultMansartEntityManagerFactory
- [ ] Update QueryExecutionContext to use EntityNameResolver
- [ ] Add parameter extraction from AbstractMansartQuery to QueryExecutionParams
- [ ] Pass parameters to RepositoryRuntime.queryList()

### Week 2: Relationships & IN Predicate
- [ ] Implement relationship path resolution in QueryExecutionContext
- [ ] Extract IN value list from JPQL AST
- [ ] Create Where.In with proper arity
- [ ] Bind IN parameter values

### Week 3: EXISTS & Functions
- [ ] Parse and execute EXISTS subqueries
- [ ] Implement JpqlFunctionConverter
- [ ] Add function support to WHERE clauses
- [ ] Add function support to SELECT clauses

## Testing Strategy

1. **Entity Resolution Tests**
   - Test simple entity names
   - Test fully qualified entity names
   - Test @Entity(name = "CustomName") mapping
   - Test entities in different packages

2. **Parameter Binding Tests**
   - Test named parameters
   - Test positional parameters
   - Test mixed named and positional
   - Test parameter types (String, Integer, Date, etc.)

3. **Relationship Tests**
   - Test single-level relationships
   - Test multi-level relationships
   - Test WHERE clauses on relationship attributes
   - Test ORDER BY on relationship attributes

4. **IN Predicate Tests**
   - Test IN with literal values
   - Test IN with parameter
   - Test NOT IN

5. **EXISTS Tests**
   - Test EXISTS with subquery
   - Test NOT EXISTS
   - Test correlated subqueries

6. **Function Tests**
   - Test each function category
   - Test functions in WHERE
   - Test functions in SELECT
   - Test nested functions

## Dependencies

- `mansart-data-query:0.3.0-SNAPSHOT` - JPQL AST (already available)
- `mansart-data-dialect-spi:0.3.0-SNAPSHOT` - Where.In, function support
- `mansart-data-core:0.3.0-SNAPSHOT` - RepositoryRuntime with parameter support

## Success Criteria

- [ ] All Priority 1 tasks complete and tested
- [ ] All Priority 2 tasks complete and tested
- [ ] At least 50% of Priority 3 tasks complete
- [ ] All existing tests still pass
- [ ] No compilation errors
- [ ] All license headers present
