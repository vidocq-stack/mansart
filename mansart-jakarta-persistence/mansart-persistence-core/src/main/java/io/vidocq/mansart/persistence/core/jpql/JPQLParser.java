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
 * query        ::= SELECT selectClause FROM fromClause [WHERE whereClause]
 * selectClause ::= entityRef | pathExpr | expressionList
 * fromClause   ::= invocation
 * invocation   ::= entityName identificationVariable
 * whereClause  ::= booleanExpression
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

        /** {@code SELECT selectClause FROM fromClause [WHERE whereClause]} */
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
            return new JPQLQuerySpecification(selectClause, fromClause, whereClause);
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

        /** entityName identificationVariable */
        JPQLFromClause parseInvocation() {
            var entityToken = consume();
            if (entityToken.type != JPQLTokenizer.Type.IDENTIFIER) {
                throw new JPQLException(
                        "Expected entity name after FROM, found " + entityToken.type, ql);
            }

            Class<?> entityType = resolveEntityType(entityToken.value);
            // Check for an explicit alias: FROM Entity p
            if (peek() != null && peek().type == JPQLTokenizer.Type.IDENTIFIER) {
                consume(); // skip the alias token (we use entityToken.value as the alias)
            }
            return new JPQLFromClause(entityType, entityToken.value);
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

        /** comparison | NOT atom | nullCheck */
        JPQLExpression parseAtom() {
            // NOT expr
            if (peek() != null && peek().type == JPQLTokenizer.Type.NOT) {
                consume();
                var child = parseAtom();
                return new JPQLExpression.Not(child);
            }

            var left = parseExpression();

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

            // comparison
            if (peek() != null && peek().type == JPQLTokenizer.Type.OP_CMP) {
                var opToken = consume();
                var op = parseComparator(opToken.value);
                var right = parseExpression();
                return new JPQLExpression.Comparison(left, op, right);
            }

            return left;
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

        /** expression ::= pathExpr | literal | parameter */
        JPQLExpression parseExpression() {
            var tok = peek();
            if (tok == null) {
                throw new JPQLException("Unexpected end of expression", ql);
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
