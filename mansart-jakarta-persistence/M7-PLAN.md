/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * or any later version, which is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * It is also made available under the European Union Public Licence v. 1.2,
 * which is available at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

# M7: Advanced JPQL Features

## Objectives

M7 extends M6's JPQL implementation with advanced query features: GROUP BY, HAVING, JOIN syntax, 
subqueries in FROM clause, ALL/ANY/SOME predicates, and additional JPQL functions. This milestone 
delivers comprehensive JPQL support for production use.

## Priority 1: GROUP BY and HAVING Support (Critical)

### Current State
- GROUP BY clause parses but is not processed
- HAVING clause parses but is not processed
- Aggregation functions work but results are not grouped

### Target Implementation
1. Parse GROUP BY expressions from JPQL AST
2. Parse HAVING predicate from JPQL AST
3. Create GroupBy AST representation
4. Pass GROUP BY and HAVING to RepositoryRuntime
5. Integrate with dialect SELECT generation

### Files to Create/Modify
- `GroupBy.java` (new) - Group by clause representation
- `JpqlToRuntimeConverter.convertGroupBy()` - convert JPQL GROUP BY to GroupBy
- `JpqlToRuntimeConverter.convertHaving()` - convert JPQL HAVING to Where
- `QueryExecutionParams` - add groupBy and having fields
- `H2Dialect.select()` - add GROUP BY and HAVING SQL generation
- `PostgresqlDialect.select()` - add GROUP BY and HAVING SQL generation

## Priority 2: JOIN Syntax Support (Critical)

### Current State
- JOIN syntax parses but creates implicit joins via path resolution
- No explicit JOIN ... ON syntax support
- No LEFT OUTER JOIN, RIGHT OUTER JOIN, etc.

### Target Implementation
1. Parse JOIN expressions from JPQL AST (JpqlJoin)
2. Support JOIN types: INNER, LEFT OUTER, RIGHT OUTER, FULL OUTER, CROSS
3. Support JOIN ON conditions
4. Support JOIN FETCH (eager loading hint)
5. Create JoinType enum
6. Integrate with QueryExecutionContext for alias management

### Files to Create/Modify
- `JoinType.java` (new) - enum for JOIN types
- `JoinExpression.java` (new) - represents a JOIN ... ON clause
- `JpqlToRuntimeConverter.convertJoin()` - convert JPQL JOIN to JoinExpression
- `QueryExecutionContext` - enhance to handle explicit joins
- `H2Dialect.select()` - render explicit JOINs
- `PostgresqlDialect.select()` - render explicit JOINs

## Priority 3: Subquery Support in FROM Clause (High)

### Current State
- Subqueries in EXISTS are supported
- Subqueries in FROM clause parse but are not processed
- No subquery alias support

### Target Implementation
1. Detect subqueries in FROM clause (JpqlDerivedTable)
2. Assign aliases to subqueries
3. Execute subquery and use results as derived table
4. Support correlated subqueries in FROM
5. Integrate with QueryExecutionContext

### Files to Modify
- `JpqlToRuntimeConverter.convertFromClause()` - handle subqueries
- `QueryExecutionContext` - add subquery alias management
- `RepositoryRuntime` - support derived tables from subqueries

## Priority 4: ALL/ANY/SOME Predicate Implementation (High)

### Current State
- ALL/ANY/SOME predicates parse but throw UnsupportedOperationException
- JpqlAllAnySomePredicate exists in AST but is not converted

### Target Implementation
1. Convert JpqlAllAnySomePredicate to Where.All/Any/Some
2. Add Where.All, Where.Any, Where.Some record types
3. Support comparison with subquery: `= ALL`, `> ANY`, `= SOME`
4. Render to SQL: `= ALL (subquery)`, `> ANY (subquery)`, etc.
5. Update all dialects

### Files to Create/Modify
- `Where.java` - add All/Any/Some record types
- `JpqlToRuntimeConverter.convertAllAnySomePredicate()` - implementation
- `H2Dialect.renderPredicate()` - add ALL/ANY/SOME cases
- `PostgresqlDialect.renderPredicate()` - add ALL/ANY/SOME cases
- `Joins.java` - add All/Any/Some to walkWhere()
- `WhereBinder.java` - add All/Any/Some to bind()

## Priority 5: Additional JPQL Functions (Medium)

### Current State
- Basic functions are detected and classified
- Function rendering is dialect-specific
- Some functions may not have proper parameter handling

### Target Functions to Add
- **String**: LOCATE, REPLACE, RTRIM, LTRIM
- **Numeric**: CEILING, FLOOR, EXP, LN, LOG, POWER, ROUND, SIGN
- **Date/Time**: EXTRACT, SECOND, MINUTE, HOUR, DAYOFWEEK, DAYOFMONTH, DAYOFYEAR, WEEK, MONTH, YEAR
- **Database**: INDEX (JPA 3.2), KEY (JPA 3.2), VALUE (JPA 3.2)
- **Type**: INSTANCE

### Files to Modify
- `JpqlFunctionConverter` - add new function classifications
- `H2Dialect` - add function rendering for new functions
- `PostgresqlDialect` - add function rendering for new functions

## Priority 6: Performance Optimization (Medium)

### Current State
- Each query creates new AST and conversion
- No caching of parsed queries
- No query plan caching

### Target Implementation
1. Query cache for parsed JPQL (configurable)
2. Query plan cache (converted Where/OrderBy)
3. Prepared statement caching
4. Batch query execution support

### Files to Create/Modify
- `QueryCache.java` (new) - cache for parsed queries
- `MansartEntityManager` - add query cache configuration
- `MansartQuery` - use cached query plans

## Task Breakdown

### Week 1: GROUP BY & HAVING
- [ ] Create GroupBy class to represent GROUP BY clause
- [ ] Implement JpqlToRuntimeConverter.convertGroupBy()
- [ ] Implement JpqlToRuntimeConverter.convertHaving()
- [ ] Update QueryExecutionParams to include groupBy and having
- [ ] Update H2Dialect to render GROUP BY and HAVING
- [ ] Update PostgresqlDialect to render GROUP BY and HAVING
- [ ] Add GROUP BY and HAVING tests

### Week 2: JOIN Syntax
- [ ] Create JoinType enum
- [ ] Create JoinExpression class
- [ ] Implement JpqlToRuntimeConverter.convertJoin()
- [ ] Update QueryExecutionContext to handle explicit joins
- [ ] Update H2Dialect to render JOIN syntax
- [ ] Update PostgresqlDialect to render JOIN syntax
- [ ] Add JOIN syntax tests

### Week 3: Subqueries in FROM & ALL/ANY/SOME
- [ ] Implement subquery support in FROM clause
- [ ] Add subquery alias management to QueryExecutionContext
- [ ] Add Where.All/Any/Some record types
- [ ] Implement JpqlToRuntimeConverter.convertAllAnySomePredicate()
- [ ] Update all dialects to render ALL/ANY/SOME
- [ ] Add ALL/ANY/SOME tests
- [ ] Add FROM subquery tests

### Week 4: Additional Functions & Performance
- [ ] Add remaining JPQL functions
- [ ] Update dialect renderers for new functions
- [ ] Implement query caching
- [ ] Add performance tests

## Testing Strategy

### 1. GROUP BY & HAVING Tests
- Test single-column GROUP BY
- Test multi-column GROUP BY
- Test GROUP BY with aggregation functions
- Test HAVING with single condition
- Test HAVING with multiple conditions
- Test GROUP BY with HAVING
- Test ORDER BY with GROUP BY

### 2. JOIN Syntax Tests
- Test INNER JOIN
- Test LEFT OUTER JOIN
- Test RIGHT OUTER JOIN
- Test CROSS JOIN
- Test JOIN with ON condition
- Test multiple JOINs
- Test JOIN FETCH

### 3. FROM Subquery Tests
- Test simple subquery in FROM
- Test subquery with alias
- Test correlated subquery in FROM
- Test multiple subqueries in FROM
- Test JOIN with subquery

### 4. ALL/ANY/SOME Tests
- Test = ALL (subquery)
- Test > ANY (subquery)
- Test = SOME (subquery)
- Test with literal values (where supported)
- Test NOT with ALL/ANY/SOME

### 5. Additional Function Tests
- Test each new function
- Test functions in WHERE
- Test functions in SELECT
- Test functions in GROUP BY
- Test functions in HAVING

### 6. Performance Tests
- Test query cache hit/miss
- Test prepared statement reuse
- Test batch execution

## Dependencies

- `mansart-data-query:0.3.0-SNAPSHOT` - JPQL AST (JpqlJoin, JpqlGroupByClause, JpqlHavingClause)
- `mansart-data-dialect-spi:0.3.0-SNAPSHOT` - Extended Where types, GroupBy
- `mansart-data-core:0.3.0-SNAPSHOT` - RepositoryRuntime with grouping support

## Success Criteria

- [ ] GROUP BY and HAVING clauses fully functional
- [ ] JOIN syntax (INNER, LEFT, RIGHT, CROSS) fully functional
- [ ] Subqueries in FROM clause functional
- [ ] ALL/ANY/SOME predicates functional
- [ ] All new JPQL functions supported
- [ ] Query caching implemented
- [ ] All existing tests still pass
- [ ] No compilation errors
- [ ] All license headers present
- [ ] At least 200 total tests in persistence-core
