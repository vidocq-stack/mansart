/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under
 * the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses a JPQL SELECT query string into an AST.
 *
 * <p>Supports: {@code SELECT [DISTINCT] e FROM Entity e [WHERE expr] [ORDER BY expr [ASC|DESC]]}.
 * WHERE expressions: field comparison with string literals ({@code =}, {@code <>}).</p>
 */
final class JpqlParser {

    private final String query;
    private int pos;

    /**
     * Creates a parser for the given JPQL string.
     *
     * @param query the JPQL query string
     */
    JpqlParser(String query) {
        this.query = query;
    }

    /**
     * Parses the query into an AST.
     *
     * <p>Supports single queries and set operations:
     * {@code SELECT [DISTINCT] e FROM Entity e [WHERE expr] [ORDER BY expr [ASC|DESC]]
     * [UNION|INTERSECT|EXCEPT SELECT ...]}.</p>
     *
     * @return the parsed query AST (or set operation with sub-queries)
     * @throws IllegalArgumentException if the query is not a supported SELECT
     */
    JpqlQuery parse() {
        skipWhitespace();
        if (!matchKeyword("SELECT")) {
            throw new IllegalArgumentException("Only SELECT queries are supported: " + query);
        }

        // Optional DISTINCT
        boolean distinct = matchKeyword("DISTINCT");

        // SELECT clause: alias.*
        String selectAlias = parseSelectClause();

        // FROM clause
        expectKeyword("FROM");
        String entityName = parseIdentifier();
        String fromAlias = entityName;
        skipWhitespace();
        if (pos < query.length() && (Character.isLetter(query.charAt(pos))
                || query.charAt(pos) == '_')) {
            fromAlias = parseIdentifier();
        }

        // WHERE clause
        List<JpqlPredicate> predicates = new ArrayList<>();
        if (matchKeyword("WHERE")) {
            parseWhereClause(predicates);
        }

        // ORDER BY clause
        List<JpqlOrderBy> orderBys = new ArrayList<>();
        if (matchKeyword("ORDER")) {
            expectKeyword("BY");
            parseOrderByClause(orderBys);
        }

        // Check for set operation (UNION, INTERSECT, EXCEPT)
        if (matchKeyword("UNION")) {
            return parseSetOperation(JpqlQuery.SetOpType.UNION,
                    entityName, selectAlias, fromAlias, distinct,
                    predicates, orderBys);
        }
        if (matchKeyword("INTERSECT")) {
            return parseSetOperation(JpqlQuery.SetOpType.INTERSECT,
                    entityName, selectAlias, fromAlias, distinct,
                    predicates, orderBys);
        }
        if (matchKeyword("EXCEPT")) {
            return parseSetOperation(JpqlQuery.SetOpType.EXCEPT,
                    entityName, selectAlias, fromAlias, distinct,
                    predicates, orderBys);
        }

        return new JpqlQuery(entityName, selectAlias, fromAlias, distinct,
                predicates, orderBys);
    }

    /**
     * Parses a set operation: the first query plus one or more subsequent
     * SELECT queries combined with the given set operation type.
     */
    private JpqlQuery parseSetOperation(JpqlQuery.SetOpType setOp,
                                        String entityName, String selectAlias,
                                        String fromAlias, boolean distinct,
                                        List<JpqlPredicate> predicates,
                                        List<JpqlOrderBy> orderBys) {
        // Build the first (head) query — its setOp is NONE
        JpqlQuery head = new JpqlQuery(entityName, selectAlias, fromAlias,
                distinct, predicates, orderBys);

        // Parse subsequent SELECT queries
        List<JpqlQuery> subQueries = new ArrayList<>();
        subQueries.add(head);

        while (true) {
            skipWhitespace();
            if (!matchKeyword("SELECT")) {
                break;
            }

            // Optional DISTINCT for sub-query
            boolean subDistinct = matchKeyword("DISTINCT");

            // SELECT clause
            String subSelectAlias = parseSelectClause();

            // FROM clause
            expectKeyword("FROM");
            String subEntityName = parseIdentifier();
            String subFromAlias = subEntityName;
            skipWhitespace();
            if (pos < query.length() && (Character.isLetter(query.charAt(pos))
                    || query.charAt(pos) == '_')) {
                subFromAlias = parseIdentifier();
            }

            // WHERE clause
            List<JpqlPredicate> subPredicates = new ArrayList<>();
            if (matchKeyword("WHERE")) {
                parseWhereClause(subPredicates);
            }

            // ORDER BY clause
            List<JpqlOrderBy> subOrderBys = new ArrayList<>();
            if (matchKeyword("ORDER")) {
                expectKeyword("BY");
                parseOrderByClause(subOrderBys);
            }

            subQueries.add(new JpqlQuery(subEntityName, subSelectAlias,
                    subFromAlias, subDistinct, subPredicates, subOrderBys));
        }

        return new JpqlQuery(null, null, null, false, List.of(), List.of(),
                setOp, subQueries);
    }

    private String parseSelectClause() {
        skipWhitespace();
        String alias = parseIdentifier();
        skipWhitespace();
        if (matchKeyword("AS")) {
            alias = parseIdentifier();
        }
        return alias;
    }

    private void parseWhereClause(List<JpqlPredicate> predicates) {
        parsePredicate(predicates);
        skipWhitespace();
        while (matchKeyword("AND") || matchKeyword("OR")) {
            parsePredicate(predicates);
            skipWhitespace();
        }
    }

    private void parsePredicate(List<JpqlPredicate> predicates) {
        skipWhitespace();
        
        // Check for scalar function call: FUNC_NAME(args...)
        FuncInfo funcInfo = parseFunctionCall();
        String function = funcInfo != null ? funcInfo.function() : null;
        String firstArg = funcInfo != null ? funcInfo.firstArg() : null;
        List<String> args = funcInfo != null ? funcInfo.allArgs() : List.of();
        
        // If we parsed a function, the first argument is already extracted
        // Otherwise, parse the field name normally
        String fieldName;
        if (firstArg == null) {
            skipWhitespace();
            String alias = null;
            if (pos < query.length() && (Character.isLetter(query.charAt(pos))
                    || query.charAt(pos) == '_')) {
                // Peek: is the next char after the identifier a dot?
                int start = pos;
                while (pos < query.length() && (Character.isLetterOrDigit(query.charAt(pos))
                        || query.charAt(pos) == '_')) {
                    pos++;
                }
                skipWhitespace();
                if (pos < query.length() && query.charAt(pos) == '.') {
                    alias = query.substring(start, pos - 1);
                } else {
                    pos = start;
                }
            }
            
            // Now parse the field name
            skipWhitespace();
            if (alias != null) {
                expect(".");
                fieldName = parseIdentifier();
            } else {
                fieldName = parseIdentifier();
            }
        } else {
            fieldName = firstArg;
        }

        skipWhitespace();
        String op;
        if (match("=")) {
            op = "=";
        } else if (match("<>")) {
            op = "<>";
        } else if (match(">=")) {
            op = ">=";
        } else if (match("<=")) {
            op = "<=";
        } else if (match(">")) {
            op = ">";
        } else if (match("<")) {
            op = "<";
        } else {
            throw new IllegalArgumentException("Unsupported WHERE operator at position " + pos);
        }

        skipWhitespace();
        String value = parseLiteral();
        predicates.add(new JpqlPredicate(fieldName, op, value, function, args));
    }

    /**
     * Holds a parsed function name, its first field name, and all arguments.
     */
    private record FuncInfo(String function, String firstArg, List<String> allArgs) {

        /**
         * Convenience constructor with no arguments.
         */
        FuncInfo(String function, String firstArg) {
            this(function, firstArg, List.of());
        }
    }

    /**
     * Attempts to parse a scalar function call at the current position.
     *
     * <p>For unary functions (UPPER, LOWER, LENGTH), returns the function name
     * and the first (field) argument. For multi-argument functions
     * (LOCATE, SUBSTRING, LEFT, RIGHT, CONCAT), returns the function name,
     * the first argument, and a list of all arguments.</p>
     */
    private FuncInfo parseFunctionCall() {
        skipWhitespace();
        int savePos = pos;
        
        // Check for function name followed by '('
        if (!matchIdentifier()) {
            return null;
        }
        String funcName = query.substring(savePos, pos);
        skipWhitespace();
        if (!match("(")) {
            // Not a function call — restore position
            pos = savePos;
            return null;
        }
        
        // Extract all arguments from the function call.
        // Special handling for EXTRACT(field FROM expr): the SQL standard
        // uses "FROM" as a separator between the extraction field and the
        // target expression, rather than commas.
        List<String> args = new ArrayList<>();
        String firstArg = parseFunctionArg();
        args.add(firstArg);
        
        // For EXTRACT, check for the "FROM" keyword after the first argument.
        // This handles the SQL standard EXTRACT(YEAR FROM col) syntax.
        if ("EXTRACT".equals(funcName.toUpperCase())) {
            skipWhitespace();
            if (matchKeyword("FROM")) {
                args.add(parseFunctionArg());
            }
        } else {
            // Non-EXTRACT: parse comma-separated arguments
            skipWhitespace();
            while (match(",")) {
                skipWhitespace();
                args.add(parseFunctionArg());
                skipWhitespace();
            }
        }
        
        // Skip to closing paren (consume remaining depth)
        int depth = 1;
        while (pos < query.length() && depth > 0) {
            char c = query.charAt(pos);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
            pos++;
        }
        
        // Only register as a function if it's a known scalar function
        // (unary functions map to Where.Func; multi-arg functions
        // are handled as raw SQL fragments via the arguments list)
        if (JpqlFunctionRegistry.isFuncFunction(funcName)) {
            return new FuncInfo(funcName.toUpperCase(), firstArg);
        }
        
        // Multi-argument function — store all arguments
        return new FuncInfo(funcName.toUpperCase(), firstArg, args);
    }

    /**
     * Parses a single function argument: a field reference (alias.field)
     * or a literal (string, number, or positional parameter).
     */
    private String parseFunctionArg() {
        skipWhitespace();
        
        // String literal
        if (pos < query.length() && query.charAt(pos) == '\'') {
            return parseStringLiteral();
        }
        
        // Positional parameter ?N
        if (pos < query.length() && query.charAt(pos) == '?') {
            return parsePositionalParam();
        }
        
        // Numeric literal
        if (pos < query.length() && Character.isDigit(query.charAt(pos))) {
            return parseNumericLiteral();
        }
        
        // Field reference: alias.fieldName or fieldName
        if (pos < query.length() && (Character.isLetter(query.charAt(pos))
                || query.charAt(pos) == '_')) {
            return parseFieldRef();
        }
        
        // Fallback: return empty string (will fail in translator)
        return "";
    }

    private String parseStringLiteral() {
        pos++; // skip opening quote
        int start = pos;
        while (pos < query.length() && query.charAt(pos) != '\'') {
            pos++;
        }
        String literal = query.substring(start, pos);
        if (pos < query.length()) {
            pos++; // skip closing quote
        }
        return literal;
    }

    private String parsePositionalParam() {
        pos++; // skip ?
        int start = pos;
        while (pos < query.length() && Character.isDigit(query.charAt(pos))) {
            pos++;
        }
        if (pos > start) {
            return "?".concat(query.substring(start, pos));
        }
        return "?";
    }

    private String parseNumericLiteral() {
        int start = pos;
        while (pos < query.length() && (Character.isDigit(query.charAt(pos))
                || query.charAt(pos) == '.' || query.charAt(pos) == '-')) {
            pos++;
        }
        return query.substring(start, pos);
    }

    private String parseFieldRef() {
        skipWhitespace();
        // Check for alias.fieldName pattern
        if (pos < query.length() && (Character.isLetter(query.charAt(pos))
                || query.charAt(pos) == '_')) {
            int start = pos;
            while (pos < query.length() && (Character.isLetterOrDigit(query.charAt(pos))
                    || query.charAt(pos) == '_')) {
                pos++;
            }
            skipWhitespace();
            if (pos < query.length() && query.charAt(pos) == '.') {
                // Has alias.fieldName — return only the field name
                pos++; // skip dot
                skipWhitespace();
                return parseIdentifier();
            } else {
                // Bare identifier (e.g. "YEAR" in EXTRACT(YEAR FROM ...)): return it
                return query.substring(start, pos);
            }
        }
        
        // Check for nested function call — skip it and try to get the field from inside
        if (pos < query.length() && (Character.isLetter(query.charAt(pos))
                || query.charAt(pos) == '_')) {
            int start = pos;
            while (pos < query.length() && (Character.isLetterOrDigit(query.charAt(pos))
                    || query.charAt(pos) == '_')) {
                pos++;
            }
            skipWhitespace();
            if (pos < query.length() && query.charAt(pos) == '(') {
                // Nested function — skip to closing paren and try again
                int depth = 1;
                while (pos < query.length() && depth > 0) {
                    char c = query.charAt(pos);
                    if (c == '(') depth++;
                    else if (c == ')') depth--;
                    pos++;
                }
                // Try to extract field from inside the nested call
                return parseFieldRef();
            }
        }
        
        // Fallback: return empty string (will fail in translator)
        return "";
    }

    private void parseOrderByClause(List<JpqlOrderBy> orderBys) {
        parseOrderByItem(orderBys);
        skipWhitespace();
        while (match(",")) {
            parseOrderByItem(orderBys);
            skipWhitespace();
        }
    }

    private void parseOrderByItem(List<JpqlOrderBy> orderBys) {
        skipWhitespace();
        // Check if there's an alias
        skipWhitespace();
        String alias = null;
        if (pos < query.length() && (Character.isLetter(query.charAt(pos))
                || query.charAt(pos) == '_')) {
            int start = pos;
            while (pos < query.length() && (Character.isLetterOrDigit(query.charAt(pos))
                    || query.charAt(pos) == '_')) {
                pos++;
            }
            skipWhitespace();
            if (pos < query.length() && query.charAt(pos) == '.') {
                alias = query.substring(start, pos - 1);
            } else {
                pos = start;
            }
        }
        
        // Parse the field name
        skipWhitespace();
        String fieldName;
        if (alias != null) {
            expect(".");
            fieldName = parseIdentifier();
        } else {
            fieldName = parseIdentifier();
        }

        skipWhitespace();
        String direction = "ASC";
        if (matchKeyword("ASC")) {
            direction = "ASC";
        } else if (matchKeyword("DESC")) {
            direction = "DESC";
        }

        orderBys.add(new JpqlOrderBy(fieldName, direction));
    }

    // -- token helpers ------------------------------------------------------

    private boolean matchKeyword(String keyword) {
        skipWhitespace();
        int remaining = query.length() - pos;
        if (remaining >= keyword.length()
                && query.regionMatches(true, pos, keyword, 0, keyword.length())
                && (pos + keyword.length() >= query.length()
                    || !Character.isLetterOrDigit(query.charAt(pos + keyword.length())))) {
            pos += keyword.length();
            return true;
        }
        return false;
    }

    private boolean match(String s) {
        skipWhitespace();
        int remaining = query.length() - pos;
        if (remaining >= s.length() && query.startsWith(s, pos)) {
            pos += s.length();
            return true;
        }
        return false;
    }

    private boolean matchIdentifier() {
        skipWhitespace();
        if (pos < query.length() && (Character.isLetter(query.charAt(pos))
                || query.charAt(pos) == '_')) {
            int start = pos;
            while (pos < query.length() && (Character.isLetterOrDigit(query.charAt(pos))
                    || query.charAt(pos) == '_')) {
                pos++;
            }
            return true;
        }
        return false;
    }

    private String parseIdentifier() {
        skipWhitespace();
        int start = pos;
        while (pos < query.length() && (Character.isLetterOrDigit(query.charAt(pos))
                || query.charAt(pos) == '_')) {
            pos++;
        }
        return query.substring(start, pos);
    }

    private String parseLiteral() {
        skipWhitespace();
        if (pos < query.length() && query.charAt(pos) == '\'') {
            pos++; // skip opening quote
            int start = pos;
            while (pos < query.length() && query.charAt(pos) != '\'') {
                pos++;
            }
            String literal = query.substring(start, pos);
            if (pos < query.length()) {
                pos++; // skip closing quote
            }
            return literal;
        }
        // Positional parameter ?N
        if (pos < query.length() && query.charAt(pos) == '?') {
            pos++; // skip ?
            int start = pos;
            while (pos < query.length() && Character.isDigit(query.charAt(pos))) {
                pos++;
            }
            if (pos > start) {
                return "?".concat(query.substring(start, pos));
            }
            return "?";
        }
        // Numeric literal
        int start = pos;
        while (pos < query.length() && (Character.isDigit(query.charAt(pos))
                || query.charAt(pos) == '.' || query.charAt(pos) == '-')) {
            pos++;
        }
        return query.substring(start, pos);
    }

    private void skipWhitespace() {
        while (pos < query.length() && Character.isWhitespace(query.charAt(pos))) {
            pos++;
        }
    }

    private void expect(String s) {
        if (!match(s)) {
            throw new IllegalArgumentException("Expected '" + s + "' at position " + pos + " in: " + query);
        }
    }

    private void expectKeyword(String keyword) {
        if (!matchKeyword(keyword)) {
            throw new IllegalArgumentException("Expected '" + keyword + "' at position " + pos + " in: " + query);
        }
    }
}
