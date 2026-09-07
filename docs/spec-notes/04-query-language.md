# Spec note 04 — JPQL: query language (chapter 4)

Jakarta Persistence 3.2, chapter 4. Condensed from the spec. Focus: BNF
grammar, statement types, expression hierarchy. This note drives the
JPQL parser (M6-JP-34) and the SELECT/UPDATE/DELETE cards that follow.

## Statement types (§4.2)

Three top-level statements:
```
select_statement ::= select_clause from_clause [where_clause] [groupby_clause] [having_clause] [orderby_clause]
update_statement  ::= update_clause [where_clause]
delete_statement ::= delete_clause [where_clause]
```

## BNF — core productions (§4.12)

### Select statement
```
select_statement   ::= [select_clause] from_clause [where_clause] [groupby_clause] [having_clause] [orderby_clause]
select_clause      ::= SELECT [DISTINCT] select_item {, select_item}
select_item         ::= identification_variable | path_expression | aggregate_expr | constructor_expr | scalar_expr
constructor_expr    ::= NEW constructor_name ( select_item {, select_item} )
```

### FROM clause (§4.4)
```
from_clause        ::= FROM identification_var_declaration {, identification_var_declaration}
identification_var_declaration ::= range_var_decl | join_decl
range_var_decl     ::= entity_name [AS] identification_variable
join_decl          ::= [join_type] JOIN [FETCH] path_expression [AS] identification_variable
join_type          ::= INNER | LEFT [OUTER] | RIGHT [OUTER] | CROSS
```
- Identification variables: Java identifier, not a reserved keyword.
- Path expression: `identification_variable.{field_name}+` — dot-separated.
- Fetch joins: `JOIN FETCH` — no alias allowed on fetch target in strict JPQL.

### WHERE clause (§4.5–4.6)
```
where_clause       ::= WHERE conditional_expression
conditional_expr   ::= conditional_term { OR conditional_term }
conditional_term   ::= conditional_factor { AND conditional_factor }
conditional_factor ::= [NOT] conditional_primary
conditional_primary ::= simple_cond | ( conditional_expr )
simple_cond        ::= comparison_expr | between_expr | in_expr | like_expr
                     | null_cmp_expr | empty_cmp_expr | collection_member_expr
                     | exists_expr | all_or_any_expr
```

### Comparison expressions (§4.6.3)
```
comparison_expr   ::= scalar_expr comparison_op scalar_expr
                     | entity_expr (IS [NOT] NULL)?
comparison_op     ::= = | <> | < | <= | > | >=
```

### BETWEEN, IN, LIKE (§4.6.4–4.6.6)
```
between_expr      ::= scalar_expr [NOT] BETWEEN scalar_expr AND scalar_expr
in_expr           ::= path_expr [NOT] IN ( in_item {, in_item} | subquery )
in_item           ::= literal | input_param
like_expr         ::= scalar_expr [NOT] LIKE pattern [ESCAPE escape_char]
```

### NULL / EMPTY / MEMBER / EXISTS (§4.6.7–4.6.10)
```
null_cmp_expr     ::= {path_expr | input_param} IS [NOT] NULL
empty_cmp_expr    ::= collection_path_expr IS [NOT] EMPTY
collection_member ::= entity_expr [NOT] MEMBER [OF] collection_path_expr
exists_expr       ::= [NOT] EXISTS ( subquery )
```

### ALL / ANY / subqueries (§4.6.11–4.6.12)
```
all_or_any_expr   ::= scalar_expr comparison_op {ALL | ANY | SOME} ( subquery )
subquery          ::= select_statement  -- without orderby on some dialects
```

### Scalar expressions (§4.7)
```
scalar_expr       ::= arithmetic_expr | string_expr | datetime_expr
                     | path_expr | literal | input_param | case_expr
                     | func_expr | entity_type_expr
arithmetic_expr   ::= arithmetic_term { (+ | -) arithmetic_term }
arithmetic_term   ::= arithmetic_factor { (* | / | MOD) arithmetic_factor }
arithmetic_factor ::= [ (+|-) ] arithmetic_primary
arithmetic_primary ::= literal | input_param | path_expr | ( arithmetic_expr )
                     | func_expr | case_expr
```

### Literals (§4.7.1)
```
literal           ::= integer_literal | float_literal | string_literal
                     | boolean_literal | enum_literal | null_literal
string_literal    ::= 'string'  -- single-quoted, '' escapes a quote
boolean_literal   ::= TRUE | FALSE
null_literal       ::= NULL
```

### Input parameters (§4.7.4)
```
input_param        ::= named_param | positional_param
named_param        ::= :identifier
positional_param   ::= ?integer
```

### Functions (§4.7.7)
- String: `CONCAT(s1,s2)`, `SUBSTRING(s,start[,len])`, `TRIM([spec][char] FROM s)`,
  `LOWER(s)`, `UPPER(s)`, `LENGTH(s)`, `LOCATE(sub,s[,start])`, `INDEX(col)`
- Arithmetic: `ABS(n)`, `SQRT(n)`, `MOD(n,m)`, `CEILING(n)`, `FLOOR(n)`,
  `ROUND(n[,s])`, `EXP(n)`, `LN(n)`, `POWER(n,m)`, `SIGN(n)`
- Datetime: `CURRENT_DATE`, `CURRENT_TIME`, `CURRENT_TIMESTAMP`,
  `LOCAL_DATE`, `LOCAL_TIME`, `LOCAL_DATETIME`, `EXTRACT(field FROM source)`
- Aggregate: `COUNT([DISTINCT] x)`, `MAX(x)`, `MIN(x)`, `SUM(x)`, `AVG([DISTINCT] x)`
- `FUNCTION(name, args...)` — user-defined DB function call.

### CASE expressions (§4.7.10)
```
case_expr         ::= simple_case | searched_case | coalesce | nullif
simple_case       ::= CASE case_operand { WHEN scalar_expr THEN scalar_expr }+ ELSE scalar_expr END
searched_case     ::= CASE { WHEN cond_expr THEN scalar_expr }+ ELSE scalar_expr END
coalesce          ::= COALESCE(scalar_expr {, scalar_expr})
nullif            ::= NULLIF(scalar_expr, scalar_expr)
```

### GROUP BY / HAVING / ORDER BY (§4.8–4.10)
```
groupby_clause    ::= GROUP BY groupby_item {, groupby_item}
groupby_item      ::= path_expr | scalar_expr
having_clause     ::= HAVING conditional_expr
orderby_clause    ::= ORDER BY orderby_item {, orderby_item}
orderby_item      ::= scalar_expr [ASC | DESC] [NULLS (FIRST | LAST)]
```

### UPDATE / DELETE (§4.11)
```
update_clause     ::= UPDATE entity_name [AS] identification_variable
                     SET update_item {, update_item}
update_item       ::= [identification_variable.]{field_name} = scalar_expr
delete_clause     ::= DELETE FROM entity_name [AS] identification_variable
```

## Reserved keywords (§4.4.1)

Keywords are case-insensitive in JPQL. Not all are reserved (some are
"reserved for future use" per the spec), but the parser must recognise
them as keywords, not identifiers. Key set: SELECT, FROM, WHERE, UPDATE,
DELETE, SET, JOIN, INNER, LEFT, RIGHT, OUTER, CROSS, FETCH, AS, ON,
DISTINCT, WHERE, GROUP, BY, HAVING, ORDER, ASC, DESC, NULLS, FIRST, LAST,
AND, OR, NOT, BETWEEN, IN, LIKE, IS, NULL, EMPTY, MEMBER, OF, EXISTS,
ALL, ANY, SOME, CASE, WHEN, THEN, ELSE, END, COALESCE, NULLIF, NEW,
TRUE, FALSE, COUNT, SUM, AVG, MIN, MAX, CONCAT, SUBSTRING, TRIM, LOWER,
UPPER, LENGTH, LOCATE, INDEX, ABS, SQRT, MOD, CEILING, FLOOR, ROUND,
EXP, LN, POWER, SIGN, CURRENT_DATE, CURRENT_TIME, CURRENT_TIMESTAMP,
LOCAL_DATE, LOCAL_TIME, LOCAL_DATETIME, EXTRACT, FROM, FUNCTION, TYPE,
TREAT, KEY, VALUE, ENTRY, GROUP, ORDER, HAVING.

## Mansart parser contract (M6-JP-34)

### Scope — parser only

M6-JP-34 produces a **parse tree** (AST), not SQL. The AST references
entity and attribute *names* as strings — resolution to `EntityModel` /
`Attribute` happens in the execution layer (M6-JP-35+). The parser must:

1. Tokenise (lexer): keywords, identifiers, string/integer/float
   literals, operators, named/positional parameters.
2. Parse into a sealed-interface AST rooted at `JpqlStatement`.
3. Reject malformed input with `IllegalArgumentException` containing
   the position and a message.

### AST shape (new package `io.vidocq.mansart.persistence.core.jpql`)

```
JpqlStatement
  ├── SelectStatement(select, from, where, groupBy, having, orderBy)
  ├── UpdateStatement(update, where)
  └── DeleteStatement(delete, where)

SelectClause(distinct, items)
FromClause(declarations)
WhereClause(expr)          -- nullable
GroupByClause(items)
HavingClause(expr)
OrderByClause(items)

RangeVarDecl(entityName, alias)
JoinDecl(type, fetch, path, alias)
JoinType enum: INNER, LEFT, RIGHT, CROSS

OrderByItem(expr, asc, nullsFirst)

Expression (sealed)
  ├── Path(idVar, fields)         -- e.g. "e.name"
  ├── Literal(value, type)
  ├── NamedParam(name)            -- :name
  ├── PositionalParam(position)   -- ?1
  ├── Binary(op, left, right)     -- arithmetic + comparison
  ├── Unary(op, operand)
  ├── Func(name, args)            -- any function call
  ├── Aggregate(func, distinct, arg)  -- COUNT, SUM, AVG, MIN, MAX
  ├── CaseExpr(operand, whens, elseExpr)
  ├── SearchedCase(whens, elseExpr)
  ├── Coalesce(args)
  ├── Nullif(a, b)
  ├── NewExpr(constructor, args)  -- constructor expression
  └── Subquery(selectStatement)

Condition (sealed) — top-level conditional expression
  ├── Comparison(op, left, right)
  ├── Between(expr, low, high, negated)
  ├── In(expr, items, negated)    -- items: list or Subquery
  ├── Like(expr, pattern, escape, negated)
  ├── IsNull(expr, negated)
  ├── IsEmpty(expr, negated)
  ├── Member(expr, collection, negated)
  ├── Exists(subquery, negated)
  ├── AllAny(expr, op, quantifier, subquery)
  ├── And(conditions)
  ├── Or(conditions)
  └── Not(condition)
```

### Tokeniser contract

- Keywords: case-insensitive match.
- Identifiers: Java identifier rules (`[a-zA-Z_$][a-zA-Z0-9_$]*`).
- String literals: single-quoted, `''` escapes a single quote.
- Numeric: integer and floating-point (with optional exponent).
- Operators: `=`, `<>`, `<`, `<=`, `>`, `>=`, `+`, `-`, `*`, `/`.
- Punctuation: `(`, `)`, `,`, `.`, `:`, `?`.
- Skip whitespace and comments (`--` line, `/* */` block).

### Error handling

`IllegalArgumentException` with message format:
`"JPQL parse error at position N: <message>"` — no custom exception type
in this card (keep it minimal; a dedicated `JpqlException` can come later
when the execution layer needs to distinguish parse vs semantic errors).

### Tests (TDD) — all in `mansart-persistence-core` test tree

Test class `JpqlParserTest` in package `io.vidocq.mansart.persistence.core.jpql`:
1. `SELECT e FROM Employee e` — simplest select, range var, alias.
2. `SELECT e FROM Employee e WHERE e.name = :name` — named param, path,
   comparison.
3. `SELECT e FROM Employee e WHERE e.salary > 50000 AND e.active = TRUE`
   — logical AND, numeric/boolean literals.
4. `SELECT e FROM Employee e WHERE e.name LIKE 'J%' ESCAPE '\'` — LIKE.
5. `SELECT e FROM Employee e WHERE e.age BETWEEN 25 AND 65` — BETWEEN.
6. `SELECT e FROM Employee e WHERE e.dept IN ('A', 'B', 'C')` — IN list.
7. `SELECT DISTINCT e.name FROM Employee e ORDER BY e.name ASC` —
   DISTINCT, ORDER BY.
8. `SELECT COUNT(e) FROM Employee e` — aggregate.
9. `UPDATE Employee e SET e.salary = e.salary * 1.1 WHERE e.active = TRUE`
   — UPDATE.
10. `DELETE FROM Employee e WHERE e.active = FALSE` — DELETE.
11. `SELECT e FROM Employee e JOIN e.department d WHERE d.name = 'Eng'`
    — JOIN.
12. Invalid: `SELECT FROM Employee` — parse error with position.

### Files to create

```
mansart-persistence-core/src/main/java/io/vidocq/mansart/persistence/core/jpql/
  ├── JpqlParser.java         -- public entry: JpqlStatement parse(String)
  ├── JpqlLexer.java          -- tokeniser
  ├── Token.java              -- token type + value + position
  ├── TokenType.java          -- enum of token kinds
  ├── JpqlAst.java            -- sealed interfaces/records for the AST
  └── package-info.java
mansart-persistence-core/src/test/java/io/vidocq/mansart/persistence/core/jpql/
  └── JpqlParserTest.java
```

### Out of scope for M6-JP-34

- AST → `Where`/`OrderBy` translation (M6-JP-35).
- Execution / binding to dialect SPI (M6-JP-35).
- Subquery parsing is structurally supported in the AST but not
  exercised by tests until M6-JP-45.
- CASE expression parsing: AST node exists, tested minimally.
