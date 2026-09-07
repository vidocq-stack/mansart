package io.vidocq.mansart.persistence.core.jpql;

/**
 * Token types for JPQL lexical analysis.
 */
public enum TokenType {
    // Special token types
    KEYWORD,
    
    // Keywords
    SELECT,
    FROM,
    WHERE,
    UPDATE,
    DELETE,
    SET,
    JOIN,
    INNER,
    LEFT,
    RIGHT,
    OUTER,
    CROSS,
    FETCH,
    AS,
    ON,
    DISTINCT,
    GROUP,
    BY,
    HAVING,
    ORDER,
    ASC,
    DESC,
    NULLS,
    FIRST,
    LAST,
    AND,
    OR,
    NOT,
    BETWEEN,
    IN,
    LIKE,
    IS,
    NULL,
    EMPTY,
    MEMBER,
    OF,
    EXISTS,
    ALL,
    ANY,
    SOME,
    CASE,
    WHEN,
    THEN,
    ELSE,
    END,
    COALESCE,
    NULLIF,
    NEW,
    TRUE,
    FALSE,
    COUNT,
    SUM,
    AVG,
    MIN,
    MAX,
    CONCAT,
    SUBSTRING,
    TRIM,
    LOWER,
    UPPER,
    LENGTH,
    LOCATE,
    INDEX,
    ABS,
    SQRT,
    MOD,
    CEILING,
    FLOOR,
    ROUND,
    EXP,
    LN,
    POWER,
    SIGN,
    CURRENT_DATE,
    CURRENT_TIME,
    CURRENT_TIMESTAMP,
    LOCAL_DATE,
    LOCAL_TIME,
    LOCAL_DATETIME,
    EXTRACT,
    FUNCTION,
    TYPE,
    TREAT,
    KEY,
    VALUE,
    ENTRY,
    
    // Identifiers and literals
    IDENTIFIER,
    STRING_LITERAL,
    INTEGER_LITERAL,
    FLOAT_LITERAL,
    
    // Parameters
    NAMED_PARAM,
    POSITIONAL_PARAM,
    
    // Operators
    EQ,           // =
    NE,           // <>
    LT,           // <
    LE,           // <=
    GT,           // >
    GE,           // >=
    PLUS,         // +
    MINUS,        // -
    MULTIPLY,     // *
    DIVIDE,       // /
    AND_OP,       // AND
    OR_OP,        // OR
    NOT_OP,
    
    // Punctuation
    LPAREN,       // (
    RPAREN,       // )
    COMMA,        // ,
    DOT,          // .
    COLON,        // :
    QUESTION,     // ?
    
    // Special JPQL clauses
    ESCAPE,
    
    // Special
    EOF,
    UNKNOWN;
}
