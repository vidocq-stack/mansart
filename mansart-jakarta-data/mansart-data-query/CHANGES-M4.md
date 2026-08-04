# M4 - JPQL Parser Fixes

## Summary

Fixed all failing tests in the JpqlParser module. The parser now correctly handles:
- Complex queries with JOIN, LEFT JOIN, INNER JOIN
- Comparison predicates in WHERE and HAVING clauses
- Parenthesized predicates
- EXISTS and NOT EXISTS subqueries
- Numeric literals (including decimals like 1.1)
- Path expressions (e.g., b.price, b.author.name)
- Function calls (COUNT(), etc.)
- SELECT * and COUNT(*)
- AND/OR/NOT logical operators

## Technical Changes

### JpqlParser.java

#### Predicate Parsing
- **Fixed**: `parseComparisonExpression()` no longer consumes comparison operators in expression context
  - Comparison operators (=