package io.vidocq.mansart.persistence.core.jpql;

/**
 * A lexical token in JPQL.
 *
 * @param type     the token type
 * @param text     the exact text of the token (for literals, the unescaped value)
 * @param position the 0-based character position in the input where the token starts
 * @param keyword  the keyword name if this is a keyword token, otherwise null
 * @param paramName the parameter name if this is a named parameter, otherwise null
 * @param paramIndex the parameter index if this is a positional parameter, otherwise 0
 */
public record Token(
    TokenType type,
    String text,
    int position,
    String keyword,
    String paramName,
    int paramIndex
) {
    
    /**
     * Canonical constructor.
     */
    public Token {
        if (text == null) {
            throw new IllegalArgumentException("Token text cannot be null");
        }
        if (keyword == null) {
            keyword = "";
        }
        if (paramName == null) {
            paramName = "";
        }
        if (paramIndex < 0) {
            paramIndex = 0;
        }
    }
    
    /**
     * Minimal constructor for non-keyword, non-parameter tokens.
     */
    public Token(TokenType type, String text, int position) {
        this(type, text, position, null, null, 0);
    }
    
    /**
     * Constructor for keyword tokens.
     */
    public Token(TokenType type, String text, int position, TokenType keywordType) {
        this(type, text, position, text.toUpperCase(), null, 0);
    }
    
    /**
     * Constructor for named parameter tokens.
     */
    public Token(TokenType type, String text, int position, String paramName) {
        this(type, text, position, null, paramName, 0);
    }
    
    /**
     * Constructor for positional parameter tokens.
     */
    public Token(TokenType type, String text, int position, int paramIndex) {
        this(type, text, position, null, null, paramIndex);
    }
    
    /**
     * Returns true if this token is a keyword.
     */
    public boolean isKeyword() {
        return !keyword.isEmpty();
    }
    
    /**
     * Returns the keyword name (uppercase) if this is a keyword token.
     */
    public String keywordName() {
        return keyword;
    }
}
