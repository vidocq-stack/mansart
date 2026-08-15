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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.jpql;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Recursive-descent parser for JPQL strings.
 *
 * <p>Milestone: M7-13 — parses {@code SELECT <entity> FROM <Entity> <alias> WHERE <predicate>}
 * queries. Accepts an entity-to-class mapping so the parser can resolve entity names to
 * actual {@code Class} objects at parse time.
 *
 * <p>Supported grammar (simplified):
 * <pre>{@code
 * query        ::= SELECT selectClause FROM fromClause [WHERE whereClause] [GROUP BY groupByClause] [HAVING havingClause]
 * selectClause ::= entityRef | pathExpr | expressionList
 * fromClause   ::= invocation
 * invocation   ::= entityName identificationVariable
 * whereClause  ::= booleanExpression
 * groupByClause ::= expression (',' expression)*
 * havingClause ::= booleanExpression
 * booleanExpr  ::= atom (AND booleanExpr | OR booleanExpr)*
 * atom         ::= comparison | notAtom | nullCheck
 * comparison   ::= expression comparator expression
 * notAtom      ::= NOT atom
 * nullCheck    ::= expression IS [NOT] NULL
 * comparator   ::= '=' | '<>' | '!=' | '<' | '<=' | '>' | '>=' | 'LIKE'
 * expression   ::= pathExpr | literal | param
 * literal      ::= STRING | NUMBER | TRUE | FALSE | NULL
 * param        ::= ':' identifier | '?'
 * }</pre>
 */
public final class JPQLParser {

    private final Map<String, Class<?>> entityNames;

    /**
     * Creates a JPQL parser with the given entity name-to-class mapping.
     *
     * @param entityNames map of entity names/simple class names to entity classes
     */
    public JPQLParser(Map<String, Class<?>> entityNames) {
        this.entityNames = Objects.requireNonNull(entityNames, "entityNames must not be null");
    }

    /**
     * Parses a JPQL query string into an AST.
     *
     * @param qlString the JPQL query to parse
     * @return the parsed query AST
     * @throws JPQLException if the query cannot be parsed
     */
    @SuppressWarnings("unchecked")
    public <V> JPQLQuery<V> parse(String qlString) {
        return parse(qlString, null);
    }

    /**
     * Parses a JPQL query string into a typed AST.
     *
     * @param qlString     the JPQL query to parse
     * @param userClass    the expected typed result class, or {@code null} for untyped
     * @return the parsed query AST
     * @throws JPQLException if the query cannot be parsed
     */
    @SuppressWarnings("unchecked")
    public <V> JPQLQuery<V> parse(String qlString, Class<V> userClass) {
        if (qlString == null || qlString.isBlank()) {
            throw new JPQLException("Query must not be null or blank", qlString);
        }
        var tokenizer = new JPQLTokenizer(qlString);
        var tokens = tokenizer.tokenize();
        if (tokens.isEmpty()) {
            throw new JPQLException("Empty query", qlString);
        }
        var parser = new Parser(qlString, tokens, entityNames);
        var spec = parser.parseBody();
        Class<?> entityType = spec.fromClause().entityType();
        var typedResult = userClass != null && userClass.isAssignableFrom(entityType)
                ? userClass : entityType;
        return new JPQLQuery<>(spec, (Class<V>) typedResult);
    }

    /* ---- Internal parser state ---- */

    private static final class Parser {
        private final String ql;
        private final List<JPQLTokenizer.Token> tokens;
        private final Map<String, Class<?>> entityNames;
        private int pos;

        Parser(String ql, List<JPQLTokenizer.Token> tokens, Map<String, Class<?>> entityNames) {
            this.ql = ql;
            this.tokens = tokens;
            this.entityNames = entityNames;
        }

        /* ---- Token access ---- */

        private JPQLTokenizer.Token peek() {
            return pos < tokens.size() ? tokens.get(pos) : null;
        }

        private JPQLTokenizer.Token consume() {
            var tok = peek();
            if (tok == null) {
                throw new JPQLException("Unexpected end of query, expected a token", ql);
            }
            pos++;
            return tok;
        }

        private void checkAndConsume(JPQLTokenizer.Type expected) {
            var tok = peek();
            if (tok == null) {
                throw new JPQLException("Unexpected end of query, expected " + expected, ql);
            }
            if (tok.type != expected) {
                throw new JPQLException(
                        "Expected " + expected + " at position " + pos
                                + " but found " + tok.type, ql);
            }
            pos++;
        }

        /* ---- Clause parsers ---- */

        /** {@code SELECT selectClause FROM fromClause [WHERE whereClause] [GROUP BY groupByClause] [HAVING havingClause]} */
        JPQLQuerySpecification parseBody() {
            checkAndConsume(JPQLTokenizer.Type.SELECT);
            var selectClause = parseSelectClause();
            checkAndConsume(JPQLTokenizer.Type.FROM);
            var fromClause = parseInvocation();
            var whereClause = JPQLWhereClause.none();
            if (peek() != null && peek().type == JPQLTokenizer.Type.WHERE) {
                consume();
                whereClause = new JPQLWhereClause(parseBooleanExpression());
            }
            var groupByClause = parseGroupByClause();
            var havingClause = parseHavingClause();
            return new JPQLQuerySpecification(selectClause, fromClause, whereClause, groupByClause, havingClause);
        }

        /** selectExpr (',' selectExpr)* */
        JPQLSelectClause parseSelectClause() {
            var first = parseExpression();
            java.util.List<JPQLExpression> expressions = new java.util.ArrayList<>();
            expressions.add(first);
            while (peek() != null && peek().value.equals(",")) {
                consume(); // skip ','
                expressions.add(parseExpression());
            }
            return expressions.size() == 1
                    ? new JPQLSelectClause(first)
                    : new JPQLSelectClause(expressions);
        }

        /** entityName identificationVariable [JOIN ...]* */
        JPQLFromClause parseInvocation() {
            var entityToken = consume();
            if (entityToken.type != JPQLTokenizer.Type.IDENTIFIER) {
                throw new JPQLException(
                        "Expected entity name after FROM, found " + entityToken.type, ql);
            }

            Class<?> entityType = resolveEntityType(entityToken.value);
            String identificationVariable = entityToken.value;
            
            // Check for an explicit alias: FROM Entity p
            if (peek() != null && peek().type == JPQLTokenizer.Type.IDENTIFIER) {
                var aliasToken = consume();
                identificationVariable = aliasToken.value;
            }
            
            // Parse JOIN clauses
            List<JPQLJoin> joins = parseJoins();
            
            return new JPQLFromClause(entityType, identificationVariable, joins);
        }

        /** Parses zero or more JOIN clauses */
        List<JPQLJoin> parseJoins() {
            List<JPQLJoin> joins = new java.util.ArrayList<>();
            while (peek() != null && peek().type == JPQLTokenizer.Type.JOIN) {
                joins.add(parseJoin());
            }
            return joins;
        }

        /** Parses a JOIN clause: [INNER | LEFT [OUTER] | RIGHT [OUTER]] JOIN entityExpression [identificationVariable] [ON expression] */
        JPQLJoin parseJoin() {
            // Parse join type (default is INNER)
            JPQLJoinType joinType = JPQLJoinType.INNER;
            if (peek() != null) {
                // Check for LEFT or RIGHT
                if (peek().type == JPQLTokenizer.Type.LEFT) {
                    consume();
                    joinType = JPQLJoinType.LEFT;
                    // Check for OUTER
                    if (peek() != null && peek().type == JPQLTokenizer.Type.OUTER) {
                        consume();
                        joinType = JPQLJoinType.LEFT_OUTER;
                    }
                } else if (peek().type == JPQLTokenizer.Type.RIGHT) {
                    consume();
                    joinType = JPQLJoinType.RIGHT;
                    // Check for OUTER
                    if (peek() != null && peek().type == JPQLTokenizer.Type.OUTER) {
                        consume();
                        joinType = JPQLJoinType.RIGHT_OUTER;
                    }
                } else if (peek().type == JPQLTokenizer.Type.INNER) {
                    consume();
                    joinType = JPQLJoinType.INNER;
                }
            }
            
            // Consume JOIN keyword
            checkAndConsume(JPQLTokenizer.Type.JOIN);
            
            // Parse entity expression (e.g., e.orders or Order)
            JPQLExpression entityExpr = parseExpression();
            
            // Parse optional identification variable
            String identificationVariable = null;
            if (peek() != null && peek().type == JPQLTokenizer.Type.IDENTIFIER) {
                identificationVariable = peek().value;
                consume();
            }
            
            // Parse optional ON clause
            JPQLExpression onExpression = null;
            if (peek() != null && peek().type == JPQLTokenizer.Type.ON) {
                consume();
                onExpression = parseBooleanExpression();
            }
            
            return new JPQLJoin(joinType, entityExpr, identificationVariable, onExpression);
        }

        /** GROUP BY expression (',' expression)* */
        JPQLGroupByClause parseGroupByClause() {
            if (peek() == null || peek().type != JPQLTokenizer.Type.GROUP) {
                return null;
            }
            consume(); // GROUP
            checkAndConsume(JPQLTokenizer.Type.BY);
            var expressions = new java.util.ArrayList<JPQLExpression>();
            expressions.add(parseExpression());
            while (peek() != null && peek().value.equals(",")) {
                consume(); // skip ','
                expressions.add(parseExpression());
            }
            return new JPQLGroupByClause(expressions);
        }

        /** HAVING booleanExpression */
        JPQLHavingClause parseHavingClause() {
            if (peek() == null || peek().type != JPQLTokenizer.Type.HAVING) {
                return null;
            }
            consume(); // HAVING
            var expression = parseBooleanExpression();
            return new JPQLHavingClause(expression);
        }

        /* ---- WHERE clause: AND/OR disjunction ---- */

        /** {@code atom (AND/OR atom)*} */
        JPQLExpression parseBooleanExpression() {
            var result = parseAtom();
            while (peek() != null && peek().type == JPQLTokenizer.Type.AND) {
                consume(); // AND
                var right = parseAtom();
                result = new JPQLExpression.And(result, right);
            }
            while (peek() != null && peek().type == JPQLTokenizer.Type.OR) {
                consume(); // OR
                var right = parseAtom();
                result = new JPQLExpression.Or(result, right);
            }
            return result;
        }

        /** comparison | NOT atom | nullCheck | quantifiedExpression | exists */
        JPQLExpression parseAtom() {
            // EXISTS (subquery)
            if (peek() != null && peek().type == JPQLTokenizer.Type.EXISTS) {
                consume();
                JPQLSubquery subquery = parseSubqueryAsExpression();
                return new JPQLExpression.ExistsExpression(subquery);
            }

            // NOT expr
            if (peek() != null && peek().type == JPQLTokenizer.Type.NOT) {
                consume();
                var child = parseAtom();
                return new JPQLExpression.Not(child);
            }

            var left = parseExpression();

            // Check for quantified expressions: expression > ALL (subquery)
            if (peek() != null && peek().type == JPQLTokenizer.Type.OP_CMP) {
                var opToken = peek();
                var op = parseComparator(opToken.value);
                consume(); // consume the comparison operator
                
                // Check for ALL, ANY, or SOME
                if (peek() != null && (peek().type == JPQLTokenizer.Type.ALL || 
                                       peek().type == JPQLTokenizer.Type.ANY || 
                                       peek().type == JPQLTokenizer.Type.SOME)) {
                    var quantifierToken = consume();
                    JPQLQuantifiedExpression.Quantifier quantifier = switch (quantifierToken.type) {
                        case ALL -> JPQLQuantifiedExpression.Quantifier.ALL;
                        case ANY -> JPQLQuantifiedExpression.Quantifier.ANY;
                        case SOME -> JPQLQuantifiedExpression.Quantifier.SOME;
                        default -> throw new JPQLException("Unexpected quantifier: " + quantifierToken.type, ql);
                    };
                    
                    // Parse subquery
                    JPQLSubquery subquery = parseSubqueryAsExpression();
                    return new JPQLQuantifiedExpression(left, op, quantifier, subquery);
                }
                
                // Regular comparison
                var right = parseExpression();
                return new JPQLExpression.Comparison(left, op, right);
            }

            // IS [NOT] NULL
            if (peek() != null && peek().type == JPQLTokenizer.Type.IS) {
                consume();
                boolean negated = peek() != null && peek().type == JPQLTokenizer.Type.NOT;
                if (negated) consume();
                checkAndConsume(JPQLTokenizer.Type.NULL);
                var type = negated
                        ? JPQLExpression.NullCheck.NullCheckType.IS_NOT_NULL
                        : JPQLExpression.NullCheck.NullCheckType.IS_NULL;
                return new JPQLExpression.NullCheck(left, type);
            }

            return left;
        }

        /** Parses a subquery that appears after ALL/ANY/SOME */
        JPQLSubquery parseSubqueryAsExpression() {
            if (peek() == null || peek().type != JPQLTokenizer.Type.LPAREN) {
                throw new JPQLException("Expected subquery after " + (peek() != null ? peek().type : "end"), ql);
            }
            
            consume(); // consume '('
            
            // Check if this is a SELECT query
            if (peek() == null || peek().type != JPQLTokenizer.Type.SELECT) {
                throw new JPQLException("Expected SELECT in subquery", ql);
            }
            
            // Parse the subquery
            JPQLQuery<?> subquery = parseSubquery();
            
            // The subquery should have consumed everything up to the matching ')'
            // For now, we assume it did
            
            return new JPQLSubquery(subquery, null);
        }

        private JPQLComparator parseComparator(String symbol) {
            return switch (symbol) {
                case "=" -> JPQLComparator.EQUALS;
                case "<>", "!=" -> JPQLComparator.NOT_EQUALS;
                case "<" -> JPQLComparator.LESS_THAN;
                case "<=" -> JPQLComparator.LESS_THAN_EQUAL;
                case ">" -> JPQLComparator.GREATER_THAN;
                case ">=" -> JPQLComparator.GREATER_THAN_EQUAL;
                case "LIKE" -> JPQLComparator.LIKE;
                default -> throw new JPQLException(
                        "Unknown comparison operator '" + symbol + "'", ql);
            };
        }

        /* ---- Expression parsers ---- */

        /** expression ::= pathExpr | literal | parameter | subquery | function */
        JPQLExpression parseExpression() {
            var tok = peek();
            if (tok == null) {
                throw new JPQLException("Unexpected end of expression", ql);
            }

            // Check for subquery: (SELECT ...)
            if (tok.type == JPQLTokenizer.Type.LPAREN) {
                // Try to parse a subquery
                JPQLExpression subqueryResult = parseSubqueryExpression();
                if (subqueryResult != null) {
                    return subqueryResult;
                }
            }

            // Check for function call: identifier followed by '('
            if (tok.type == JPQLTokenizer.Type.IDENTIFIER && pos + 1 < tokens.size()) {
                var nextTok = tokens.get(pos + 1);
                if (nextTok.type == JPQLTokenizer.Type.LPAREN) {
                    return parseFunctionExpression();
                }
            }

            return switch (tok.type) {
                case STRING_LITERAL -> {
                    consume();
                    yield new JPQLExpression.StringLiteral(tok.value);
                }
                case NUMBER_LITERAL -> {
                    consume();
                    String val = tok.value;
                    Number num;
                    try {
                        num = Long.parseLong(val);
                    } catch (NumberFormatException e) {
                        num = Double.parseDouble(val);
                    }
                    yield new JPQLExpression.NumberLiteral(num);
                }
                case TRUE -> { consume(); yield new JPQLExpression.BooleanLiteral(true); }
                case FALSE -> { consume(); yield new JPQLExpression.BooleanLiteral(false); }
                case NULL -> { consume(); yield JPQLExpression.NullLiteral.INSTANCE; }
                case QUESTION_MARK -> {
                    consume();
                    yield new JPQLExpression.ParameterExpression(tok.value);
                }
                case IDENTIFIER, PATH_EXPRESSION -> parsePathExpression();
                default -> throw new JPQLException(
                        "Unexpected token " + tok.type + " at position " + pos, ql);
            };
        }

        /** Parses a function expression: FUNCTION(arg1, arg2, ...) */
        JPQLExpression parseFunctionExpression() {
            var funcToken = consume();
            if (funcToken.type != JPQLTokenizer.Type.IDENTIFIER) {
                throw new JPQLException("Expected function name", ql);
            }
            String functionName = funcToken.value;
            
            checkAndConsume(JPQLTokenizer.Type.LPAREN);
            
            java.util.List<JPQLExpression> arguments = new java.util.ArrayList<>();
            if (peek() == null || peek().type != JPQLTokenizer.Type.RPAREN) {
                // Parse arguments
                arguments.add(parseExpression());
                while (peek() != null && peek().value.equals(",")) {
                    consume(); // skip ','
                    arguments.add(parseExpression());
                }
            }
            
            checkAndConsume(JPQLTokenizer.Type.RPAREN);
            
            return new JPQLFunctionExpression(functionName, arguments);
        }

        /** Parses a subquery expression: (SELECT ...) [AS alias] */
        JPQLExpression parseSubqueryExpression() {
            if (peek() == null || peek().type != JPQLTokenizer.Type.LPAREN) {
                return null;
            }
            
            // Save current position in case this isn't a subquery
            int startPos = pos;
            
            try {
                consume(); // consume '('
                
                // Check if this is a SELECT query
                if (peek() == null || peek().type != JPQLTokenizer.Type.SELECT) {
                    // Not a subquery, restore position
                    pos = startPos;
                    return null;
                }
                
                // Parse the subquery
                JPQLQuery<?> subquery = parseSubquery();
                
                // Check for AS alias
                String alias = null;
                if (peek() != null && peek().type == JPQLTokenizer.Type.AS) {
                    consume();
                    if (peek() != null && peek().type == JPQLTokenizer.Type.IDENTIFIER) {
                        alias = peek().value;
                        consume();
                    }
                } else if (peek() != null && peek().type == JPQLTokenizer.Type.IDENTIFIER) {
                    // Could be an alias without AS
                    alias = peek().value;
                    consume();
                }
                
                // Consume closing parenthesis - but we might have already consumed it in parseSubquery
                // Actually, parseSubquery will consume up to the end of the query
                // We need to handle the closing parenthesis separately
                
                return new JPQLSubquery(subquery, alias);
            } catch (JPQLException e) {
                // Not a subquery, restore position
                pos = startPos;
                return null;
            }
        }

        /** Parses a complete subquery starting with SELECT */
        JPQLQuery<?> parseSubquery() {
            checkAndConsume(JPQLTokenizer.Type.SELECT);
            var selectClause = parseSelectClause();
            checkAndConsume(JPQLTokenizer.Type.FROM);
            var fromClause = parseInvocation();
            var whereClause = JPQLWhereClause.none();
            if (peek() != null && peek().type == JPQLTokenizer.Type.WHERE) {
                consume();
                whereClause = new JPQLWhereClause(parseBooleanExpression());
            }
            var groupByClause = parseGroupByClause();
            var havingClause = parseHavingClause();
            
            // For subqueries, we need to find the closing parenthesis
            // This is a simplified approach - we'll look for the matching ')'
            // In a full implementation, we'd need to handle nested parentheses
            
            // For now, just create the query specification and return
            JPQLQuerySpecification spec = new JPQLQuerySpecification(
                selectClause, fromClause, whereClause, groupByClause, havingClause);
            return new JPQLQuery<>(spec, Object.class);
        }

        /** pathExpr ::= identificationVariable ('.' fieldName)* */
        JPQLExpression parsePathExpression() {
            if (peek() == null || peek().type != JPQLTokenizer.Type.IDENTIFIER) {
                throw new JPQLException(
                        "Expected identifier, found "
                                + (peek() != null ? peek().type : "end of query"), ql);
            }
            var idTok = consume();
            String first = idTok.value;
            if (first == null) first = "";

            // Read path components: .fieldName
            java.util.List<String> fields = new java.util.ArrayList<>();
            while (peek() != null && peek().value.equals(".")) {
                consume(); // skip '.'
                if (peek() != null && peek().type == JPQLTokenizer.Type.IDENTIFIER) {
                    var ftok = consume();
                    fields.add(ftok.value);
                } else {
                    throw new JPQLException("Expected field name after '.'", ql);
                }
            }

            return new JPQLExpression.PathExpression(first, fields);
        }

        private Class<?> resolveEntityType(String name) {
            Class<?> entityType = entityNames.get(name);
            if (entityType == null) {
                // Capitalize first letter
                String capitalized = name.substring(0, 1).toUpperCase() + name.substring(1);
                entityType = entityNames.get(capitalized);
            }
            if (entityType == null) {
                throw new JPQLException(
                        "Unknown entity '" + name + "'"
                                + (entityNames.isEmpty() ? ""
                                        : (". Available: " + entityNames.keySet())), ql);
            }
            return entityType;
        }
    }
}
