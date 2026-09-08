package io.vidocq.mansart.persistence.core.jpql;

import java.util.HashMap;
import java.util.Map;

/**
 * Lexical analyzer for JPQL queries.
 * <p>
 * Produces a stream of {@link Token} instances from a JPQL query string.
 * Handles keywords (case-insensitive), identifiers, literals, operators, and comments.
 */
final class JpqlLexer {
    
    private final String input;
    private int position;
    private int start;
    private Token current;
    
    // Map of JPQL keywords to their token types
    private static final Map<String, TokenType> KEYWORDS = createKeywordMap();
    
    JpqlLexer(String input) {
        this.input = input;
        this.position = 0;
        this.start = 0;
    }
    
    /**
     * Returns the next token from the input.
     *
     * @return the next token
     */
    Token nextToken() {
        skipWhitespaceAndComments();
        
        if (position >= input.length()) {
            return new Token(TokenType.EOF, "", position);
        }
        
        char currentChar = input.charAt(position);
        start = position;
        
        // Handle identifiers and keywords
        if (isIdentifierStart(currentChar)) {
            return readIdentifierOrKeyword();
        }
        
        // Handle string literals
        if (currentChar == '\'') {
            return readStringLiteral();
        }
        
        // Handle numeric literals
        if (Character.isDigit(currentChar)) {
            return readNumericLiteral();
        }
        
        // Handle operators and punctuation
        Token token = readOperatorOrPunctuation();
        if (token != null) {
            return token;
        }
        
        // Unknown character
        position++;
        return new Token(TokenType.UNKNOWN, String.valueOf(currentChar), start);
    }
    
    private void skipWhitespaceAndComments() {
        while (position < input.length()) {
            char c = input.charAt(position);
            
            // Skip whitespace
            if (Character.isWhitespace(c)) {
                position++;
                start = position;
                continue;
            }
            
            // Skip line comments (--)
            if (c == '-' && position + 1 < input.length() && input.charAt(position + 1) == '-') {
                while (position < input.length() && input.charAt(position) != '\n') {
                    position++;
                }
                if (position < input.length()) {
                    position++; // skip newline
                }
                start = position;
                continue;
            }
            
            // Skip block comments (/* */)
            if (c == '/' && position + 1 < input.length() && input.charAt(position + 1) == '*') {
                position += 2;
                while (position < input.length() - 1) {
                    if (input.charAt(position) == '*' && input.charAt(position + 1) == '/') {
                        position += 2;
                        start = position;
                        break;
                    }
                    position++;
                }
                continue;
            }
            
            break;
        }
    }
    
    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }
    
    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }
    
    private Token readIdentifierOrKeyword() {
        while (position < input.length() && isIdentifierPart(input.charAt(position))) {
            position++;
        }
        
        String text = input.substring(start, position);
        String upperText = text.toUpperCase();
        
        // Check if it's a keyword
        TokenType keyword = KEYWORDS.get(upperText);
        if (keyword != null) {
            return new Token(keyword, text, start);
        }
        
        // It's an identifier
        return new Token(TokenType.IDENTIFIER, text, start);
    }
    
    private Token readStringLiteral() {
        position++; // skip opening quote
        StringBuilder sb = new StringBuilder();
        
        while (position < input.length()) {
            char c = input.charAt(position);
            
            if (c == '\'') {
                // Check for escaped single quote (doubled quote: '')
                if (position + 1 < input.length() && input.charAt(position + 1) == '\'') {
                    sb.append('\'');
                    position += 2;
                } else {
                    // Closing quote
                    position++;
                    return new Token(TokenType.STRING_LITERAL, sb.toString(), start);
                }
            } else {
                sb.append(c);
                position++;
            }
        }
        
        // Ran off the end without a closing quote
        throw new IllegalArgumentException("Unterminated string literal at position " + start);
    }
    
    private Token readNumericLiteral() {
        boolean isFloat = false;
        
        while (position < input.length()) {
            char c = input.charAt(position);
            
            if (c == '.') {
                // Check if it's a decimal point or part of an identifier
                if (position + 1 < input.length() && Character.isDigit(input.charAt(position + 1))) {
                    isFloat = true;
                    position++;
                } else {
                    break;
                }
            } else if (Character.isDigit(c)) {
                position++;
            } else {
                break;
            }
        }
        
        String text = input.substring(start, position);
        if (isFloat) {
            return new Token(TokenType.FLOAT_LITERAL, text, start);
        } else {
            return new Token(TokenType.INTEGER_LITERAL, text, start);
        }
    }
    
    private Token readOperatorOrPunctuation() {
        char c = input.charAt(position);
        
        switch (c) {
            case '=':
                position++;
                return new Token(TokenType.EQ, "=", start);
            case '<':
                position++;
                if (position < input.length()) {
                    char next = input.charAt(position);
                    if (next == '>') {
                        position++;
                        return new Token(TokenType.NE, "<>", start);
                    } else if (next == '=') {
                        position++;
                        return new Token(TokenType.LE, "<=", start);
                    }
                }
                return new Token(TokenType.LT, "<", start);
            case '>':
                position++;
                if (position < input.length() && input.charAt(position) == '=') {
                    position++;
                    return new Token(TokenType.GE, ">=", start);
                }
                return new Token(TokenType.GT, ">", start);
            case '+':
                position++;
                return new Token(TokenType.PLUS, "+", start);
            case '-':
                position++;
                return new Token(TokenType.MINUS, "-", start);
            case '*':
                position++;
                return new Token(TokenType.MULTIPLY, "*", start);
            case '/':
                position++;
                return new Token(TokenType.DIVIDE, "/", start);
            case '(':
                position++;
                return new Token(TokenType.LPAREN, "(", start);
            case ')':
                position++;
                return new Token(TokenType.RPAREN, ")", start);
            case ',':
                position++;
                return new Token(TokenType.COMMA, ",", start);
            case '.':
                position++;
                return new Token(TokenType.DOT, ".", start);
            case ':':
                position++;
                // Check for named parameter
                if (position < input.length() && isIdentifierStart(input.charAt(position))) {
                    int paramStart = position;
                    while (position < input.length() && isIdentifierPart(input.charAt(position))) {
                        position++;
                    }
                    String name = input.substring(paramStart, position);
                    return new Token(TokenType.NAMED_PARAM, ":" + name, start, name);
                }
                return new Token(TokenType.COLON, ":", start);
            case '?':
                position++;
                // Check for positional parameter with number
                if (position < input.length() && Character.isDigit(input.charAt(position))) {
                    int numStart = position;
                    while (position < input.length() && Character.isDigit(input.charAt(position))) {
                        position++;
                    }
                    String numText = input.substring(numStart, position);
                    int num = Integer.parseInt(numText);
                    return new Token(TokenType.POSITIONAL_PARAM, "?" + numText, start, num);
                }
                return new Token(TokenType.QUESTION, "?", start);
            default:
                return null;
        }
    }
    
    private static Map<String, TokenType> createKeywordMap() {
        Map<String, TokenType> map = new HashMap<>();
        
        // SELECT, FROM, WHERE, etc.
        map.put("SELECT", TokenType.SELECT);
        map.put("FROM", TokenType.FROM);
        map.put("WHERE", TokenType.WHERE);
        map.put("UPDATE", TokenType.UPDATE);
        map.put("DELETE", TokenType.DELETE);
        map.put("SET", TokenType.SET);
        map.put("JOIN", TokenType.JOIN);
        map.put("INNER", TokenType.INNER);
        map.put("LEFT", TokenType.LEFT);
        map.put("RIGHT", TokenType.RIGHT);
        map.put("OUTER", TokenType.OUTER);
        map.put("CROSS", TokenType.CROSS);
        map.put("FETCH", TokenType.FETCH);
        map.put("AS", TokenType.AS);
        map.put("ON", TokenType.ON);
        map.put("DISTINCT", TokenType.DISTINCT);
        map.put("GROUP", TokenType.GROUP);
        map.put("BY", TokenType.BY);
        map.put("HAVING", TokenType.HAVING);
        map.put("ORDER", TokenType.ORDER);
        map.put("ASC", TokenType.ASC);
        map.put("DESC", TokenType.DESC);
        map.put("NULLS", TokenType.NULLS);
        map.put("FIRST", TokenType.FIRST);
        map.put("LAST", TokenType.LAST);
        map.put("AND", TokenType.AND);
        map.put("OR", TokenType.OR);
        map.put("NOT", TokenType.NOT);
        map.put("BETWEEN", TokenType.BETWEEN);
        map.put("IN", TokenType.IN);
        map.put("LIKE", TokenType.LIKE);
        map.put("IS", TokenType.IS);
        map.put("NULL", TokenType.NULL);
        map.put("EMPTY", TokenType.EMPTY);
        map.put("MEMBER", TokenType.MEMBER);
        map.put("OF", TokenType.OF);
        map.put("EXISTS", TokenType.EXISTS);
        map.put("ALL", TokenType.ALL);
        map.put("ANY", TokenType.ANY);
        map.put("SOME", TokenType.SOME);
        map.put("CASE", TokenType.CASE);
        map.put("WHEN", TokenType.WHEN);
        map.put("THEN", TokenType.THEN);
        map.put("ELSE", TokenType.ELSE);
        map.put("END", TokenType.END);
        map.put("COALESCE", TokenType.COALESCE);
        map.put("NULLIF", TokenType.NULLIF);
        map.put("NEW", TokenType.NEW);
        map.put("TRUE", TokenType.TRUE);
        map.put("FALSE", TokenType.FALSE);
        map.put("COUNT", TokenType.COUNT);
        map.put("SUM", TokenType.SUM);
        map.put("AVG", TokenType.AVG);
        map.put("MIN", TokenType.MIN);
        map.put("MAX", TokenType.MAX);
        map.put("ESCAPE", TokenType.ESCAPE);
        map.put("CURRENT_DATE", TokenType.CURRENT_DATE);
        map.put("CURRENT_TIME", TokenType.CURRENT_TIME);
        map.put("CURRENT_TIMESTAMP", TokenType.CURRENT_TIMESTAMP);
        map.put("LOCAL_DATE", TokenType.LOCAL_DATE);
        map.put("LOCAL_TIME", TokenType.LOCAL_TIME);
        map.put("LOCAL_DATETIME", TokenType.LOCAL_DATETIME);
        map.put("EXTRACT", TokenType.EXTRACT);
        
        return map;
    }
    
    /**
     * Returns the current token without consuming it.
     *
     * @return the current token
     */
    Token peek() {
        if (current == null) {
            current = nextToken();
        }
        return current;
    }
    
    /**
     * Consumes the current token and advances to the next one.
     *
     * @return the consumed token
     */
    Token consume() {
        Token token = peek();
        current = null;
        return token;
    }
    
    /**
     * Consumes the current token if it matches the expected type.
     *
     * @param expected the expected token type
     * @return the consumed token
     * @throws IllegalArgumentException if the token doesn't match
     */
    Token expect(TokenType expected) {
        Token token = peek();
        if (token.type() != expected) {
            throw new IllegalArgumentException("JPQL parse error at position " + token.position() + ": Expected " + expected + " but found " + token.type());
        }
        return consume();
    }
}
