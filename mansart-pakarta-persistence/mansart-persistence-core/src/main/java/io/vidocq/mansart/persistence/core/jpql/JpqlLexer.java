package io.vidocq.mansart.persistence.core.jpql;

import java.util.HashMap;
import java.util.Map;

/**
 * Lexical analyser for JPQL.
 * <p>
 * Produces a stream of {@link Token} instances from a JPQL query string.
 * Handles keywords (case-insensitive), identifiers, string literals, numeric literals,
 * operators, parameters, and punctuation. Skips whitespace and comments.
 */
final class JpqlLexer {
    
    private static final Map<String, TokenType> KEYWORDS;
    
    static {
        KEYWORDS = new HashMap<>();
        // SELECT, FROM, WHERE, UPDATE, DELETE, SET, JOIN, INNER, LEFT, RIGHT, OUTER, CROSS
        KEYWORDS.put("select", TokenType.SELECT);
        KEYWORDS.put("from", TokenType.FROM);
        KEYWORDS.put("where", TokenType.WHERE);
        KEYWORDS.put("update", TokenType.UPDATE);
        KEYWORDS.put("delete", TokenType.DELETE);
        KEYWORDS.put("set", TokenType.SET);
        KEYWORDS.put("join", TokenType.JOIN);
        KEYWORDS.put("inner", TokenType.INNER);
        KEYWORDS.put("left", TokenType.LEFT);
        KEYWORDS.put("right", TokenType.RIGHT);
        KEYWORDS.put("outer", TokenType.OUTER);
        KEYWORDS.put("cross", TokenType.CROSS);
        KEYWORDS.put("fetch", TokenType.FETCH);
        KEYWORDS.put("as", TokenType.AS);
        KEYWORDS.put("on", TokenType.ON);
        KEYWORDS.put("distinct", TokenType.DISTINCT);
        KEYWORDS.put("group", TokenType.GROUP);
        KEYWORDS.put("by", TokenType.BY);
        KEYWORDS.put("having", TokenType.HAVING);
        KEYWORDS.put("order", TokenType.ORDER);
        KEYWORDS.put("asc", TokenType.ASC);
        KEYWORDS.put("desc", TokenType.DESC);
        KEYWORDS.put("nulls", TokenType.NULLS);
        KEYWORDS.put("first", TokenType.FIRST);
        KEYWORDS.put("last", TokenType.LAST);
        KEYWORDS.put("and", TokenType.AND);
        KEYWORDS.put("or", TokenType.OR);
        KEYWORDS.put("not", TokenType.NOT);
        KEYWORDS.put("between", TokenType.BETWEEN);
        KEYWORDS.put("in", TokenType.IN);
        KEYWORDS.put("like", TokenType.LIKE);
        KEYWORDS.put("is", TokenType.IS);
        KEYWORDS.put("null", TokenType.NULL);
        KEYWORDS.put("empty", TokenType.EMPTY);
        KEYWORDS.put("member", TokenType.MEMBER);
        KEYWORDS.put("of", TokenType.OF);
        KEYWORDS.put("exists", TokenType.EXISTS);
        KEYWORDS.put("all", TokenType.ALL);
        KEYWORDS.put("any", TokenType.ANY);
        KEYWORDS.put("some", TokenType.SOME);
        KEYWORDS.put("case", TokenType.CASE);
        KEYWORDS.put("when", TokenType.WHEN);
        KEYWORDS.put("then", TokenType.THEN);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("end", TokenType.END);
        KEYWORDS.put("coalesce", TokenType.COALESCE);
        KEYWORDS.put("nullif", TokenType.NULLIF);
        KEYWORDS.put("new", TokenType.NEW);
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
        KEYWORDS.put("count", TokenType.COUNT);
        KEYWORDS.put("sum", TokenType.SUM);
        KEYWORDS.put("avg", TokenType.AVG);
        KEYWORDS.put("min", TokenType.MIN);
        KEYWORDS.put("max", TokenType.MAX);
        KEYWORDS.put("concat", TokenType.CONCAT);
        KEYWORDS.put("substring", TokenType.SUBSTRING);
        KEYWORDS.put("trim", TokenType.TRIM);
        KEYWORDS.put("lower", TokenType.LOWER);
        KEYWORDS.put("upper", TokenType.UPPER);
        KEYWORDS.put("length", TokenType.LENGTH);
        KEYWORDS.put("locate", TokenType.LOCATE);
        KEYWORDS.put("index", TokenType.INDEX);
        KEYWORDS.put("abs", TokenType.ABS);
        KEYWORDS.put("sqrt", TokenType.SQRT);
        KEYWORDS.put("mod", TokenType.MOD);
        KEYWORDS.put("ceiling", TokenType.CEILING);
        KEYWORDS.put("floor", TokenType.FLOOR);
        KEYWORDS.put("round", TokenType.ROUND);
        KEYWORDS.put("exp", TokenType.EXP);
        KEYWORDS.put("ln", TokenType.LN);
        KEYWORDS.put("power", TokenType.POWER);
        KEYWORDS.put("sign", TokenType.SIGN);
        KEYWORDS.put("current_date", TokenType.CURRENT_DATE);
        KEYWORDS.put("current_time", TokenType.CURRENT_TIME);
        KEYWORDS.put("current_timestamp", TokenType.CURRENT_TIMESTAMP);
        KEYWORDS.put("local_date", TokenType.LOCAL_DATE);
        KEYWORDS.put("local_time", TokenType.LOCAL_TIME);
        KEYWORDS.put("local_datetime", TokenType.LOCAL_DATETIME);
        KEYWORDS.put("extract", TokenType.EXTRACT);
        KEYWORDS.put("function", TokenType.FUNCTION);
        KEYWORDS.put("type", TokenType.TYPE);
        KEYWORDS.put("treat", TokenType.TREAT);
        KEYWORDS.put("key", TokenType.KEY);
        KEYWORDS.put("value", TokenType.VALUE);
        KEYWORDS.put("entry", TokenType.ENTRY);
    }
    
    private final String input;
    private int position;
    private Token current;
    
    JpqlLexer(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        this.input = input;
        this.position = 0;
        this.current = null;
    }
    
    /**
     * Returns the next token without consuming it.
     */
    Token peek() {
        if (current == null) {
            current = nextToken();
        }
        return current;
    }
    
    /**
     * Advances to the next token and returns it.
     */
    Token consume() {
        if (current != null) {
            Token result = current;
            current = null;
            return result;
        }
        return nextToken();
    }
    
    /**
     * Advances past the current token if it matches the expected type.
     * @throws IllegalArgumentException if the current token does not match
     */
    void expect(TokenType expected) {
        Token token = consume();
        if (token.type() != expected) {
            throw error("Expected " + expected + " but found " + token.type() + " ('" + token.text() + "')");
        }
    }
    
    /**
     * Creates a parse error at the current position.
     */
    IllegalArgumentException error(String message) {
        return new IllegalArgumentException("JPQL parse error at position " + position + ": " + message);
    }
    
    /**
     * Advances to the next token.
     */
    private Token nextToken() {
        skipWhitespaceAndComments();
        
        if (position >= input.length()) {
            return new Token(TokenType.EOF, "", position);
        }
        
        char c = input.charAt(position);
        
        // String literal
        if (c == '\'') {
            return scanStringLiteral();
        }
        
        // Identifier or keyword
        if (isIdentifierStart(c)) {
            return scanIdentifierOrKeyword();
        }
        
        // Number literal
        if (Character.isDigit(c)) {
            return scanNumber();
        }
        
        // Named parameter
        if (c == ':') {
            return scanNamedParameter();
        }
        
        // Positional parameter
        if (c == '?') {
            return scanPositionalParameter();
        }
        
        // Operators and punctuation
        switch (c) {
            case '=':
                position++;
                return new Token(TokenType.EQ, "=", position - 1);
            case '<':
                position++;
                if (position < input.length() && input.charAt(position) == '>') {
                    position++;
                    return new Token(TokenType.NE, "<>", position - 2);
                } else if (position < input.length() && input.charAt(position) == '=') {
                    position++;
                    return new Token(TokenType.LE, "<=", position - 2);
                }
                return new Token(TokenType.LT, "<", position - 1);
            case '>':
                position++;
                if (position < input.length() && input.charAt(position) == '=') {
                    position++;
                    return new Token(TokenType.GE, ">=", position - 2);
                }
                return new Token(TokenType.GT, ">", position - 1);
            case '+':
                position++;
                return new Token(TokenType.PLUS, "+", position - 1);
            case '-':
                position++;
                return new Token(TokenType.MINUS, "-", position - 1);
            case '*':
                position++;
                return new Token(TokenType.MULTIPLY, "*", position - 1);
            case '/':
                position++;
                return new Token(TokenType.DIVIDE, "/", position - 1);
            case '(': // LPAREN
                position++;
                return new Token(TokenType.LPAREN, "(", position - 1);
            case ')': // RPAREN
                position++;
                return new Token(TokenType.RPAREN, ")", position - 1);
            case ',': // COMMA
                position++;
                return new Token(TokenType.COMMA, ",", position - 1);
            case '.': // DOT
                position++;
                return new Token(TokenType.DOT, ".", position - 1);
            case ':': // COLON (already handled above, but keep for completeness)
                position++;
                return new Token(TokenType.COLON, ":", position - 1);
            case '?': // QUESTION (already handled above)
                position++;
                return new Token(TokenType.QUESTION, "?", position - 1);
            default:
                position++;
                return new Token(TokenType.UNKNOWN, String.valueOf(c), position - 1);
        }
    }
    
    private void skipWhitespaceAndComments() {
        while (position < input.length()) {
            char c = input.charAt(position);
            
            // Whitespace
            if (Character.isWhitespace(c)) {
                position++;
                continue;
            }
            
            // Line comment
            if (c == '-' && position + 1 < input.length() && input.charAt(position + 1) == '-') {
                while (position < input.length() && input.charAt(position) != '\n') {
                    position++;
                }
                continue;
            }
            
            // Block comment
            if (c == '/' && position + 1 < input.length() && input.charAt(position + 1) == '*') {
                position += 2;
                while (position < input.length() - 1) {
                    if (input.charAt(position) == '*' && input.charAt(position + 1) == '/') {
                        position += 2;
                        break;
                    }
                    position++;
                }
                continue;
            }
            
            break;
        }
    }
    
    private Token scanStringLiteral() {
        int start = position;
        position++; // skip opening quote
        
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        
        while (position < input.length()) {
            char c = input.charAt(position);
            
            if (escaped) {
                sb.append(c);
                escaped = false;
                position++;
            } else if (c == '\\') {
                escaped = true;
                position++;
            } else if (c == '\'') {
                position++; // skip closing quote
                return new Token(TokenType.STRING_LITERAL, sb.toString(), start);
            } else {
                sb.append(c);
                position++;
            }
        }
        
        throw error("Unterminated string literal");
    }
    
    private Token scanIdentifierOrKeyword() {
        int start = position;
        
        while (position < input.length() && isIdentifierPart(input.charAt(position))) {
            position++;
        }
        
        String text = input.substring(start, position);
        TokenType keywordType = KEYWORDS.get(text.toLowerCase());
        
        if (keywordType != null) {
            return new Token(keywordType, text, start);
        }
        
        return new Token(TokenType.IDENTIFIER, text, start);
    }
    
    private Token scanNumber() {
        int start = position;
        boolean hasDecimal = false;
        boolean hasExponent = false;
        
        // Integer part
        while (position < input.length() && Character.isDigit(input.charAt(position))) {
            position++;
        }
        
        // Decimal part
        if (position < input.length() && input.charAt(position) == '.') {
            hasDecimal = true;
            position++;
            while (position < input.length() && Character.isDigit(input.charAt(position))) {
                position++;
            }
        }
        
        // Exponent part
        if (position < input.length() && (input.charAt(position) == 'e' || input.charAt(position) == 'E')) {
            hasExponent = true;
            position++;
            if (position < input.length() && (input.charAt(position) == '+' || input.charAt(position) == '-')) {
                position++;
            }
            while (position < input.length() && Character.isDigit(input.charAt(position))) {
                position++;
            }
        }
        
        String text = input.substring(start, position);
        TokenType type = hasDecimal || hasExponent ? TokenType.FLOAT_LITERAL : TokenType.INTEGER_LITERAL;
        return new Token(type, text, start);
    }
    
    private Token scanNamedParameter() {
        int start = position;
        position++; // skip ':'
        
        if (position >= input.length() || !isIdentifierStart(input.charAt(position))) {
            throw error("Invalid named parameter");
        }
        
        while (position < input.length() && isIdentifierPart(input.charAt(position))) {
            position++;
        }
        
        String text = input.substring(start, position);
        return new Token(TokenType.NAMED_PARAM, text, start);
    }
    
    private Token scanPositionalParameter() {
        int start = position;
        position++; // skip '?'
        
        int numStart = position;
        while (position < input.length() && Character.isDigit(input.charAt(position))) {
            position++;
        }
        
        if (position == numStart) {
            throw error("Invalid positional parameter");
        }
        
        String text = input.substring(start, position);
        return new Token(TokenType.POSITIONAL_PARAM, text, start);
    }
    
    private boolean isIdentifierStart(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '$';
    }
    
    private boolean isIdentifierPart(char c) {
        return isIdentifierStart(c) || (c >= '0' && c <= '9');
    }
}
