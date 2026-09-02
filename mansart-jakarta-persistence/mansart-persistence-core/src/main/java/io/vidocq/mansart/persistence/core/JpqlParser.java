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
     * @return the parsed query AST
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

        return new JpqlQuery(entityName, selectAlias, fromAlias, distinct,
                predicates, orderBys);
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
        
        // Check for scalar function call: FUNC_NAME(field)
        FunctionInfo funcInfo = parseFunctionCall();
        String function = funcInfo != null ? funcInfo.function() : null;
        String fieldName = funcInfo != null ? funcInfo.fieldName() : null;
        
        // If we parsed a function, the field name is already extracted
        // Otherwise, parse the field name normally
        if (fieldName == null) {
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
        }

        skipWhitespace();
        String op;
        if (match("=")) {
            op = "=";
        } else if (match("<>")) {
            op = "<>";
        } else {
            throw new IllegalArgumentException("Unsupported WHERE operator at position " + pos);
        }

        skipWhitespace();
        String value = parseLiteral();
        predicates.add(new JpqlPredicate(fieldName, op, value, function));
    }

    /**
     * Holds a parsed function name and its extracted field name.
     */
    private record FunctionInfo(String function, String fieldName) {}

    /**
     * Attempts to parse a scalar function call at the current position.
     *
     * <p>Returns a {@link FunctionInfo} with the function name (uppercased)
     * and the extracted field name (e.g. {@code "UPPER", "e.name"} from
     * {@code UPPER(e.name)}), or {@code null} if not a recognized function
     * call, leaving the position unchanged.</p>
     */
    private FunctionInfo parseFunctionCall() {
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
        
        // Only register as a function if it's a known scalar function
        if (!JpqlFunctionRegistry.isFuncFunction(funcName)) {
            // Not a recognized function — restore position
            pos = savePos;
            return null;
        }
        
        // Extract the field name from within the function call.
        // For Where.Func functions, the first argument is the column.
        String fieldName = extractFieldNameFromFuncArgs();
        
        // Skip to closing paren (consume remaining args)
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
        
        return new FunctionInfo(funcName.toUpperCase(), fieldName);
    }

    /**
     * Extracts the field name from the first argument of a function call.
     * Handles alias.fieldName patterns and nested function calls.
     * Returns only the field name (not the alias), since the translator
     * looks up attributes by name.
     */
    private String extractFieldNameFromFuncArgs() {
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
                // No dot — this is just a field name (or another function)
                pos = start;
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
                return extractFieldNameFromFuncArgs();
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
