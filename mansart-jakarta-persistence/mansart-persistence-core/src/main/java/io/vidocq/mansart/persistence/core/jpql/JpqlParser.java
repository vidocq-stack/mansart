package io.vidocq.mansart.persistence.core.jpql;

import java.util.ArrayList;
import java.util.List;

import static io.vidocq.mansart.persistence.core.jpql.JpqlAst.*;

/**
 * Recursive-descent JPQL parser.
 * <p>
 * Produces an AST rooted at {@link JpqlStatement} from a JPQL query string.
 * Throws {@link IllegalArgumentException} with position information on parse errors.
 */
public final class JpqlParser {
    
    private JpqlLexer lexer;
    
    /**
     * Parses a JPQL query string and returns the AST.
     *
     * @param query the JPQL query string
     * @return the parsed statement AST
     * @throws IllegalArgumentException if the query cannot be parsed
     */
    public JpqlStatement parse(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("JPQL parse error at position 0: Query cannot be null or blank");
        }
        
        this.lexer = new JpqlLexer(query);
        
        try {
            JpqlStatement stmt = parseStatement();
            // Ensure we consumed the entire input
            if (peek().type() != TokenType.EOF) {
                throw error("Unexpected token after end of statement");
            }
            return stmt;
        } catch (IllegalArgumentException e) {
            // Already has position info, just rethrow
            throw e;
        } catch (Exception e) {
            throw error("Unexpected error: " + e.getMessage());
        }
    }
    
    private JpqlStatement parseStatement() {
        Token first = peek();
        
        if (first.type() == TokenType.SELECT) {
            return parseSelectStatement();
        } else if (first.type() == TokenType.UPDATE) {
            return parseUpdateStatement();
        } else if (first.type() == TokenType.DELETE) {
            return parseDeleteStatement();
        } else {
            throw error("Expected SELECT, UPDATE, or DELETE but found " + first.type());
        }
    }
    
    private SelectStatement parseSelectStatement() {
        int startPos = peek().position();
        expect(TokenType.SELECT);
        
        // Parse SELECT clause
        boolean distinct = false;
        List<Expression> selectItems = new ArrayList<>();
        
        if (peek().type() == TokenType.DISTINCT) {
            distinct = true;
            consume();
        }
        
        // Parse select items (can be multiple comma-separated)
        selectItems.add(parseSelectItem());
        while (peek().type() == TokenType.COMMA) {
            consume(); // consume comma
            selectItems.add(parseSelectItem());
        }
        
        SelectClause selectClause = new SelectClause(distinct, selectItems);
        
        // Parse FROM clause (required)
        FromClause fromClause = parseFromClause();
        
        // Parse optional clauses
        WhereClause whereClause = null;
        if (peek().type() == TokenType.WHERE) {
            whereClause = parseWhereClause();
        }
        
        GroupByClause groupByClause = null;
        if (peek().type() == TokenType.GROUP) {
            expect(TokenType.GROUP);
            expect(TokenType.BY);
            List<Expression> groupByItems = new ArrayList<>();
            groupByItems.add(parseExpression());
            while (peek().type() == TokenType.COMMA) {
                consume();
                groupByItems.add(parseExpression());
            }
            groupByClause = new GroupByClause(groupByItems);
        }
        
        HavingClause havingClause = null;
        if (peek().type() == TokenType.HAVING) {
            havingClause = parseHavingClause();
        }
        
        OrderByClause orderByClause = null;
        if (peek().type() == TokenType.ORDER) {
            orderByClause = parseOrderByClause();
        }
        
        return new SelectStatement(
            selectClause,
            fromClause,
            whereClause,
            groupByClause,
            havingClause,
            orderByClause,
            startPos
        );
    }
    
    private Expression parseSelectItem() {
        // For now, just parse as expression
        // In full JPQL this could be identification_variable, path_expression, aggregate_expr, etc.
        return parseExpression();
    }
    
    private FromClause parseFromClause() {
        expect(TokenType.FROM);
        List<IdentificationVarDeclaration> declarations = new ArrayList<>();
        declarations.add(parseIdentificationVarDeclaration());
        
        while (peek().type() == TokenType.COMMA || isJoinType(peek().type()) || peek().type() == TokenType.JOIN) {
            if (peek().type() == TokenType.COMMA) {
                consume(); // consume comma
            }
            declarations.add(parseIdentificationVarDeclaration());
        }
        
        return new FromClause(declarations);
    }
    
    private IdentificationVarDeclaration parseIdentificationVarDeclaration() {
        int startPos = peek().position();
        
        // Could be range var decl or join decl
        if (peek().type() == TokenType.IDENTIFIER) {
            // Try to parse as range var decl first
            String entityName = consume().text();
            String alias = null;
            
            // Optional AS keyword
            if (peek().type() == TokenType.AS) {
                consume();
            }
            
            if (peek().type() == TokenType.IDENTIFIER) {
                alias = consume().text();
            } else {
                throw error("Expected identification variable after entity name");
            }
            
            return new RangeVarDecl(entityName, alias, startPos);
        } else if (isJoinType(peek().type()) || peek().type() == TokenType.JOIN) {
            return parseJoinDecl();
        } else {
            throw error("Expected identification variable declaration");
        }
    }
    
    private boolean isJoinType(TokenType type) {
        return type == TokenType.INNER || type == TokenType.LEFT || type == TokenType.RIGHT || type == TokenType.CROSS;
    }
    
    private JoinDecl parseJoinDecl() {
        int startPos = peek().position();
        JoinType type = null;
        boolean fetch = false;
        
        // Parse optional join type
        if (isJoinType(peek().type())) {
            if (peek().type() == TokenType.INNER) {
                type = JoinType.INNER;
                consume();
            } else if (peek().type() == TokenType.LEFT) {
                type = JoinType.LEFT;
                consume();
                if (peek().type() == TokenType.OUTER) {
                    consume();
                }
            } else if (peek().type() == TokenType.RIGHT) {
                type = JoinType.RIGHT;
                consume();
                if (peek().type() == TokenType.OUTER) {
                    consume();
                }
            } else if (peek().type() == TokenType.CROSS) {
                type = JoinType.CROSS;
                consume();
            }
        }
        
        // In JPQL, bare JOIN is equivalent to INNER JOIN
        if (type == null) {
            type = JoinType.INNER;
        }
        
        expect(TokenType.JOIN);
        
        // Parse optional FETCH
        if (peek().type() == TokenType.FETCH) {
            fetch = true;
            consume();
        }
        
        // Parse path expression
        String path = parsePathExpression();
        
        String alias = null;
        // Optional AS keyword
        if (peek().type() == TokenType.AS) {
            consume();
        }
        
        if (peek().type() == TokenType.IDENTIFIER) {
            alias = consume().text();
        }
        
        return new JoinDecl(type, fetch, path, alias, startPos);
    }
    
    private String parsePathExpression() {
        StringBuilder path = new StringBuilder();
        path.append(consume().text()); // First identifier
        
        while (peek().type() == TokenType.DOT) {
            consume(); // consume dot
            if (peek().type() != TokenType.IDENTIFIER) {
                throw error("Expected identifier after dot in path expression");
            }
            path.append('.').append(consume().text());
        }
        
        return path.toString();
    }
    
    private WhereClause parseWhereClause() {
        expect(TokenType.WHERE);
        Condition condition = parseConditionalExpression();
        return new WhereClause(condition);
    }
    
    private GroupByClause parseGroupByClause() {
        expect(TokenType.GROUP);
        expect(TokenType.BY);
        List<Expression> items = new ArrayList<>();
        items.add(parseExpression());
        while (peek().type() == TokenType.COMMA) {
            consume();
            items.add(parseExpression());
        }
        return new GroupByClause(items);
    }
    
    private HavingClause parseHavingClause() {
        expect(TokenType.HAVING);
        Condition condition = parseConditionalExpression();
        return new HavingClause(condition);
    }
    
    private OrderByClause parseOrderByClause() {
        expect(TokenType.ORDER);
        expect(TokenType.BY);
        List<OrderByItem> items = new ArrayList<>();
        items.add(parseOrderByItem());
        while (peek().type() == TokenType.COMMA) {
            consume();
            items.add(parseOrderByItem());
        }
        return new OrderByClause(items);
    }
    
    private OrderByItem parseOrderByItem() {
        Expression expr = parseExpression();
        boolean ascending = true;
        Boolean nullsFirst = null;
        
        if (peek().type() == TokenType.ASC) {
            consume();
            ascending = true;
        } else if (peek().type() == TokenType.DESC) {
            consume();
            ascending = false;
        }
        
        if (peek().type() == TokenType.NULLS) {
            consume();
            if (peek().type() == TokenType.FIRST) {
                nullsFirst = true;
                consume();
            } else if (peek().type() == TokenType.LAST) {
                nullsFirst = false;
                consume();
            } else {
                throw error("Expected FIRST or LAST after NULLS");
            }
        }
        
        return new OrderByItem(expr, ascending, nullsFirst);
    }
    
    private UpdateStatement parseUpdateStatement() {
        int startPos = peek().position();
        expect(TokenType.UPDATE);
        
        String entityName = consume().text();
        String alias = null;
        
        // Optional AS
        if (peek().type() == TokenType.AS) {
            consume();
        }
        
        if (peek().type() == TokenType.IDENTIFIER) {
            alias = consume().text();
        }
        
        UpdateClause updateClause = new UpdateClause(entityName, alias, startPos);
        
        expect(TokenType.SET);
        
        // Parse SET items (for now just parse as expression, full SET clause parsing would go here)
        // We'll just consume tokens until WHERE or end
        while (peek().type() != TokenType.WHERE && peek().type() != TokenType.EOF) {
            consume();
        }
        
        WhereClause whereClause = null;
        if (peek().type() == TokenType.WHERE) {
            whereClause = parseWhereClause();
        }
        
        return new UpdateStatement(updateClause, whereClause, startPos);
    }
    
    private DeleteStatement parseDeleteStatement() {
        int startPos = peek().position();
        expect(TokenType.DELETE);
        expect(TokenType.FROM);
        
        String entityName = consume().text();
        String alias = null;
        
        // Optional AS
        if (peek().type() == TokenType.AS) {
            consume();
        }
        
        if (peek().type() == TokenType.IDENTIFIER) {
            alias = consume().text();
        }
        
        DeleteClause deleteClause = new DeleteClause(entityName, alias, startPos);
        
        WhereClause whereClause = null;
        if (peek().type() == TokenType.WHERE) {
            whereClause = parseWhereClause();
        }
        
        return new DeleteStatement(deleteClause, whereClause, startPos);
    }
    
    private Condition parseConditionalExpression() {
        // Parse conditional_term
        Condition term = parseConditionalTerm();
        
        // Parse { OR conditional_term }
        while (peek().type() == TokenType.OR) {
            consume();
            Condition right = parseConditionalTerm();
            term = new Or(List.of(term, right), term.position());
        }
        
        return term;
    }
    
    private Condition parseConditionalTerm() {
        // Parse conditional_factor
        Condition factor = parseConditionalFactor();
        
        // Parse { AND conditional_factor }
        while (peek().type() == TokenType.AND) {
            consume();
            Condition right = parseConditionalFactor();
            factor = new And(List.of(factor, right), factor.position());
        }
        
        return factor;
    }
    
    private Condition parseConditionalFactor() {
        int startPos = peek().position();
        
        if (peek().type() == TokenType.NOT) {
            consume();
            Condition primary = parseConditionalPrimary();
            return new Not(primary, startPos);
        }
        
        return parseConditionalPrimary();
    }
    
    private Condition parseConditionalPrimary() {
        int startPos = peek().position();
        
        if (peek().type() == TokenType.LPAREN) {
            consume();
            Condition expr = parseConditionalExpression();
            expect(TokenType.RPAREN);
            return expr;
        }
        
        return parseSimpleCondition(startPos);
    }
    
    private Condition parseSimpleCondition(int startPos) {
        // Parse comparison_expr, between_expr, in_expr, like_expr, etc.
        Expression left = parseScalarExpression();
        
        if (peek().type() == TokenType.EQ || peek().type() == TokenType.NE || 
            peek().type() == TokenType.LT || peek().type() == TokenType.LE ||
            peek().type() == TokenType.GT || peek().type() == TokenType.GE) {
            
            String op = consume().text();
            Expression right = parseScalarExpression();
            return new Comparison(op, left, right, startPos);
            
        } else {
            boolean negated = false;
            if (peek().type() == TokenType.NOT) {
                consume();
                negated = true;
            }
            if (peek().type() == TokenType.BETWEEN) {
                consume();
                Expression low = parseScalarExpression();
                expect(TokenType.AND);
                Expression high = parseScalarExpression();
                return new Between(left, low, high, negated, startPos);
            } else if (peek().type() == TokenType.IN) {
                consume();
                expect(TokenType.LPAREN);
                List<Expression> items = new ArrayList<>();
                items.add(parseExpression());
                while (peek().type() == TokenType.COMMA) {
                    consume();
                    items.add(parseExpression());
                }
                expect(TokenType.RPAREN);
                return new In(left, items, negated, startPos);
            } else if (peek().type() == TokenType.LIKE) {
                consume();
                Expression patternExpr = parseScalarExpression();
                Character escape = null;
                if (peek().type() == TokenType.ESCAPE) {
                    consume();
                    // Parse escape character
                    if (peek().type() == TokenType.STRING_LITERAL && peek().text().length() == 1) {
                        escape = peek().text().charAt(0);
                        consume();
                    } else {
                        throw error("Expected string literal for ESCAPE character");
                    }
                }
                return new Like(left, patternExpr, escape, negated, startPos);
            } else if (negated) {
                throw error("Expected BETWEEN, IN, or LIKE after NOT");
            } else if (peek().type() == TokenType.IS) {
                consume();
                boolean isNegated = false;
                if (peek().type() == TokenType.NOT) {
                    consume();
                    isNegated = true;
                }
                if (peek().type() == TokenType.NULL) {
                    consume();
                    return new IsNull(left, isNegated, startPos);
                } else if (peek().type() == TokenType.EMPTY) {
                    consume();
                    return new IsEmpty(left, isNegated, startPos);
                } else {
                    throw error("Expected NULL or EMPTY after IS");
                }
            } else if (peek().type() == TokenType.MEMBER) {
                consume();
                boolean memberNegated = false;
                if (peek().type() == TokenType.NOT) {
                    consume();
                    memberNegated = true;
                }
                expect(TokenType.OF);
                String collection = parsePathExpression();
                return new Member(left, collection, memberNegated, startPos);
            } else if (peek().type() == TokenType.EXISTS) {
                consume();
                boolean existsNegated = false;
                if (peek().type() == TokenType.NOT) {
                    consume();
                    existsNegated = true;
                }
                expect(TokenType.LPAREN);
                JpqlStatement subquery = parseSelectStatement();
                expect(TokenType.RPAREN);
                return new Exists(new Subquery(subquery, startPos), existsNegated, startPos);
            } else {
                throw error("Expected comparison operator or conditional expression");
            }
        }
    }
    
    private Expression parseScalarExpression() {
        return parseArithmeticExpression();
    }
    
    private Expression parseArithmeticExpression() {
        Expression term = parseArithmeticTerm();
        
        while (peek().type() == TokenType.PLUS || peek().type() == TokenType.MINUS) {
            String op = consume().text();
            Expression right = parseArithmeticTerm();
            term = new Binary(op, term, right, term.position());
        }
        
        return term;
    }
    
    private Expression parseArithmeticTerm() {
        Expression factor = parseArithmeticFactor();
        
        while (peek().type() == TokenType.MULTIPLY || peek().type() == TokenType.DIVIDE) {
            String op = consume().text();
            Expression right = parseArithmeticFactor();
            factor = new Binary(op, factor, right, factor.position());
        }
        
        return factor;
    }
    
    private Expression parseArithmeticFactor() {
        int startPos = peek().position();
        
        if (peek().type() == TokenType.PLUS || peek().type() == TokenType.MINUS) {
            String op = consume().text();
            Expression operand = parseArithmeticPrimary();
            return new Unary(op, operand, startPos);
        }
        
        return parseArithmeticPrimary();
    }
    
    private Expression parseArithmeticPrimary() {
        int startPos = peek().position();
        
        if (peek().type() == TokenType.LPAREN) {
            consume();
            Expression expr = parseArithmeticExpression();
            expect(TokenType.RPAREN);
            return expr;
        }
        
        return parseAtomicExpression(startPos);
    }
    
    private Expression parseAtomicExpression(int startPos) {
        Token token = peek();
        
        switch (token.type()) {
            case COUNT:
            case SUM:
            case AVG:
            case MIN:
            case MAX:
                String aggFuncName = consume().text().toUpperCase();
                boolean aggDistinct = false;
                if (peek().type() == TokenType.DISTINCT) {
                    aggDistinct = true;
                    consume();
                }
                expect(TokenType.LPAREN);
                Expression aggArg = parseExpression();
                expect(TokenType.RPAREN);
                return new Aggregate(aggFuncName, aggDistinct, aggArg, startPos);
            case IDENTIFIER:
                // Could be path expression, function call, or identification variable
                String first = consume().text();
                
                if (peek().type() == TokenType.DOT) {
                    // Path expression
                    List<String> fields = new ArrayList<>();
                    fields.add(first);
                    while (peek().type() == TokenType.DOT) {
                        consume();
                        if (peek().type() != TokenType.IDENTIFIER) {
                            throw error("Expected identifier after dot in path");
                        }
                        fields.add(consume().text());
                    }
                    return new Path(first, fields.subList(1, fields.size()), startPos);
                } else if (peek().type() == TokenType.LPAREN) {
                    // Function call
                    consume(); // consume '('
                    List<Expression> args = new ArrayList<>();
                    if (peek().type() != TokenType.RPAREN) {
                        args.add(parseExpression());
                        while (peek().type() == TokenType.COMMA) {
                            consume();
                            args.add(parseExpression());
                        }
                    }
                    expect(TokenType.RPAREN);
                    return new Func(first, args, startPos);
                } else {
                    // Identification variable
                    return new Path(first, List.of(), startPos);
                }
            
            case STRING_LITERAL:
                String strValue = consume().text();
                return new Literal(strValue, LiteralType.STRING, startPos);
            
            case INTEGER_LITERAL:
                String intValue = consume().text();
                return new Literal(intValue, LiteralType.INTEGER, startPos);
            
            case FLOAT_LITERAL:
                String floatValue = consume().text();
                return new Literal(floatValue, LiteralType.FLOAT, startPos);
            
            case NAMED_PARAM:
                String paramName = consume().text().substring(1); // strip leading ':'
                return new NamedParam(paramName, startPos);
            
            case POSITIONAL_PARAM:
                String paramText = consume().text();
                int paramPos = Integer.parseInt(paramText.substring(1)); // strip leading '?'
                return new PositionalParam(paramPos, startPos);
            
            case TRUE:
                consume();
                return new Literal("TRUE", LiteralType.BOOLEAN, startPos);
            
            case FALSE:
                consume();
                return new Literal("FALSE", LiteralType.BOOLEAN, startPos);
            
            case NULL:
                consume();
                return new Literal("NULL", LiteralType.NULL, startPos);
            
            default:
                throw error("Unexpected token in expression: " + token.type());
        }
    }
    
    private Expression parseExpression() {
        // For now, delegate to scalar expression
        // Full expression parsing would handle more cases
        return parseScalarExpression();
    }
    
    private Token peek() {
        return lexer.peek();
    }
    
    private Token consume() {
        return lexer.consume();
    }
    
    private void expect(TokenType expected) {
        Token token = consume();
        if (token.type() != expected) {
            throw error("Expected " + expected + " but found " + token.type() + " ('" + token.text() + "')");
        }
    }
    
    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException("JPQL parse error at position " + peek().position() + ": " + message);
    }
}
