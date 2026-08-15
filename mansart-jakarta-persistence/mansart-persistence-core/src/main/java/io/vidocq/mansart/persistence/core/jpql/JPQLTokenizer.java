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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Light tokenizer for JPQL strings.
 *
 * <p>Milestone: M7-13 — splits query strings into typed tokens for the recursive-descent parser.
 * Handles whitespace, comments, string literals, numbers, identifiers, and comparison operators.
 */
final class JPQLTokenizer {

    /** Token types emitted during lexing. */
    enum Type {
        SELECT, FROM, WHERE, GROUP, BY, HAVING, JOIN, INNER, LEFT, RIGHT, OUTER, ON, AS,
        ALL, ANY, SOME, EXISTS,
        AND, OR, NOT, IS, NULL, LIKE, TRUE, FALSE,
        IDENTIFIER, PATH_EXPRESSION, STRING_LITERAL, NUMBER_LITERAL,
        OP_CMP, LPAREN, RPAREN, QUESTION_MARK, STAR
    }

    /** A single token: its type and associated text value. */
    static class Token {
        final Type type;
        final String value;

        Token(Type type, String value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public String toString() {
            return "Token{" + type + ", '" + value + "'}";
        }
    }

    private static final String[] KEYWORDS = {
            "SELECT", "FROM", "WHERE", "GROUP", "BY", "HAVING", "JOIN", "INNER", "LEFT", "RIGHT", "OUTER", "ON", "AS",
            "ALL", "ANY", "SOME", "EXISTS",
            "AND", "OR", "NOT", "IS", "NULL", "LIKE"
    };

    private static final Map<String, Type> KEYWORD_TYPE_MAP;
    static {
        Map<String, Type> map = new java.util.HashMap<>();
        for (String kw : KEYWORDS) map.put(kw, typeForKeyword(kw));
        KEYWORD_TYPE_MAP = Map.copyOf(map);
    }

    private static Type typeForKeyword(String kw) {
        return switch (kw.toUpperCase()) {
            case "SELECT" -> Type.SELECT;
            case "FROM"   -> Type.FROM;
            case "WHERE"  -> Type.WHERE;
            case "GROUP"  -> Type.GROUP;
            case "BY"     -> Type.BY;
            case "HAVING" -> Type.HAVING;
            case "JOIN"   -> Type.JOIN;
            case "INNER"  -> Type.INNER;
            case "LEFT"   -> Type.LEFT;
            case "RIGHT"  -> Type.RIGHT;
            case "OUTER"  -> Type.OUTER;
            case "ON"     -> Type.ON;
            case "AS"     -> Type.AS;
            case "ALL"    -> Type.ALL;
            case "ANY"    -> Type.ANY;
            case "SOME"   -> Type.SOME;
            case "EXISTS" -> Type.EXISTS;
            case "AND"    -> Type.AND;
            case "OR"     -> Type.OR;
            case "NOT"    -> Type.NOT;
            case "IS"     -> Type.IS;
            case "NULL"   -> Type.NULL;
            case "LIKE"   -> Type.LIKE;
            case "TRUE"   -> Type.TRUE;
            case "FALSE"  -> Type.FALSE;
            default       -> Type.IDENTIFIER;
        };
    }

    private static boolean isWordBoundary(String str, int idx) {
        return idx >= str.length() || !Character.isJavaIdentifierPart(str.charAt(idx));
    }

    private static boolean matchesKeyword(int pos, String str, String keyword) {
        if (pos + keyword.length() > str.length()) return false;
        String slice = str.substring(pos, pos + keyword.length());
        if (!slice.equalsIgnoreCase(keyword)) return false;
        if (!isWordBoundary(str, pos + keyword.length())) return false;
        return true;
    }

    private final String query;
    private int pos;
    private final List<Token> tokens;

    JPQLTokenizer(String query) {
        this.query = Objects.requireNonNull(query, "query must not be null");
        this.tokens = new ArrayList<>();
    }

    /**
     * Tokenizes the JPQL query string, returning an unmodifiable list of tokens.
     *
     * @return the list of tokens
     */
    List<Token> tokenize() {
        pos = 0;
        while (pos < query.length()) {
            char c = query.charAt(pos);

            // Skip whitespace
            if (Character.isWhitespace(c)) {
                pos++;
                continue;
            }
            // Skip single-line comments (-- style)
            if (c == '-' && pos + 1 < query.length() && query.charAt(pos + 1) == '-') {
                while (pos < query.length() && query.charAt(pos) != '\n') pos++;
                continue;
            }
            // Skip multi-line comments (/* ... */)
            if (c == '/' && pos + 1 < query.length() && query.charAt(pos + 1) == '*') {
                pos += 2;
                while (pos + 1 < query.length()
                        && !(query.charAt(pos) == '*' && query.charAt(pos + 1) == '/')) {
                    pos++;
                }
                if (pos + 1 < query.length()) pos += 2;
                continue;
            }

            // Two-character comparison operators
            if (pos + 1 < query.length()) {
                String two = query.substring(pos, pos + 2);
                if (switch (two) {
                    case "<>", "!=" , "<=", ">=" -> true;
                    default -> false;
                }) {
                    tokens.add(new Token(Type.OP_CMP, two));
                    pos += 2;
                    continue;
                }
            }

            // Path expression: identifier.identifier
            if (pos + 2 < query.length() && query.charAt(pos + 1) == '.'
                    && query.charAt(pos + 2) != '.'
                    && Character.isJavaIdentifierStart(c)) {
                tokens.add(readPathExpression());
                continue;
            }

            // Keywords
            for (String kw : KEYWORDS) {
                if (matchesKeyword(pos, query, kw)) {
                    tokens.add(new Token(typeForKeyword(kw), kw));
                    pos += kw.length();
                    break;
                }
            }

            // Single-character punctuation/ops
            switch (c) {
                case '*'     -> tokens.add(new Token(Type.STAR, "*"));
                case '='     -> tokens.add(new Token(Type.OP_CMP, "="));
                case '<'     -> tokens.add(new Token(Type.OP_CMP, "<"));
                case '>'     -> tokens.add(new Token(Type.OP_CMP, ">"));
                case '('     -> tokens.add(new Token(Type.LPAREN, "("));
                case ')'     -> tokens.add(new Token(Type.RPAREN, ")"));
                case '?'     -> tokens.add(new Token(Type.QUESTION_MARK, "?"));
                case '\''    -> tokens.add(readStringLiteral());
                default -> {
                    if (Character.isJavaIdentifierStart(c)) {
                        tokens.add(readIdentifier());
                    } else if (Character.isDigit(c)) {
                        tokens.add(readNumber());
                    } else {
                        throw new JPQLException(
                                "Unexpected character '" + c + "' at position " + pos, query);
                    }
                }
            }
        }
        return Collections.unmodifiableList(tokens);
    }

    /**
     * Returns the token at the given index (for parser peek).
     */
    Token get(int index) { return tokens.get(index); }

    /**
     * Returns the current token index in the token stream.
     */
    int index() { return pos; }

    /**
     * Sets the current token index (evancer iterator-like).
     */
    void setCurrentIndex(int index) { this.pos = index; }

    private Token readIdentifier() {
        int start = pos;
        while (pos < query.length() && Character.isJavaIdentifierPart(query.charAt(pos))) {
            pos++;
        }
        String text = query.substring(start, pos);
        return new Token(safeKeyword(text), text);
    }

    private Type safeKeyword(String text) {
        for (String kw : KEYWORDS) {
            if (text.equalsIgnoreCase(kw)) return typeForKeyword(kw);
        }
        return Type.IDENTIFIER;
    }

    private Token readPathExpression() {
        StringBuilder sb = new StringBuilder();
        while (pos < query.length() && Character.isJavaIdentifierPart(query.charAt(pos))) {
            sb.append(query.charAt(pos));
            pos++;
        }
        while (pos + 1 < query.length() && query.charAt(pos) == '.'
                && query.charAt(pos + 1) != '.'
                && Character.isJavaIdentifierPart(query.charAt(pos + 1))) {
            pos++;
            while (pos < query.length() && Character.isJavaIdentifierPart(query.charAt(pos))) {
                sb.append(query.charAt(pos));
                pos++;
            }
        }
        return new Token(Type.PATH_EXPRESSION, sb.toString());
    }

    private Token readNumber() {
        int start = pos;
        StringBuilder sb = new StringBuilder();
        while (pos < query.length()) {
            char c = query.charAt(pos);
            if (c == '.') {
                if (pos + 1 < query.length() && Character.isDigit(query.charAt(pos + 1))) {
                    sb.append(c);
                    pos++;
                    continue;
                }
                break;
            }
            if (c == 'e' || c == 'E') {
                sb.append(c);
                pos++;
                if (pos < query.length()
                        && (query.charAt(pos) == '+' || query.charAt(pos) == '-')) {
                    sb.append(query.charAt(pos));
                    pos++;
                }
                continue;
            }
            if (Character.isDigit(c)) {
                sb.append(c);
                pos++;
            } else break;
        }
        return new Token(Type.NUMBER_LITERAL, sb.toString());
    }

    private Token readStringLiteral() {
        int start = pos;
        pos++;
        StringBuilder sb = new StringBuilder();
        while (pos < query.length()) {
            char c = query.charAt(pos);
            if (c == '\'') {
                if (pos + 1 < query.length() && query.charAt(pos + 1) == '\'') {
                    sb.append('\'');
                    pos += 2;
                } else {
                    pos++;
                    break;
                }
            } else {
                sb.append(c);
                pos++;
            }
        }
        if (pos >= query.length()) {
            throw new JPQLException(
                    "Unterminated string literal starting at position " + start, query);
        }
        return new Token(Type.STRING_LITERAL, sb.toString());
    }
}
