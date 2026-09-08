package io.vidocq.mansart.persistence.core.jpql;

import java.util.List;

/**
 * Sealed interfaces and records for the JPQL Abstract Syntax Tree (AST).
 * Rooted at {@link JpqlStatement} with three implementations:
 * {@link SelectStatement}, {@link UpdateStatement}, {@link DeleteStatement}.
 */
public final class JpqlAst {
    private JpqlAst() {}

    public sealed interface JpqlStatement permits SelectStatement, UpdateStatement, DeleteStatement {
        int position();
    }

    public record SelectStatement(
        SelectClause select, FromClause from, WhereClause where,
        GroupByClause groupBy, HavingClause having, OrderByClause orderBy,
        int position
    ) implements JpqlStatement {}

    public record UpdateStatement(UpdateClause update, WhereClause where, int position) implements JpqlStatement {}

    public record DeleteStatement(DeleteClause delete, WhereClause where, int position) implements JpqlStatement {}

    public record SelectClause(boolean distinct, List<Expression> items) {}

    public record FromClause(List<IdentificationVarDeclaration> declarations) {}

    public record WhereClause(Condition condition) {}

    public record GroupByClause(List<Expression> items) {}

    public record HavingClause(Condition condition) {}

    public record OrderByClause(List<OrderByItem> items) {}

    public sealed interface IdentificationVarDeclaration permits RangeVarDecl, JoinDecl {
        int position();
    }

    public record RangeVarDecl(String entityName, String alias, int position) implements IdentificationVarDeclaration {}

    public record JoinDecl(JoinType type, boolean fetch, String path, String alias, int position) implements IdentificationVarDeclaration {}

    public enum JoinType { INNER, LEFT, RIGHT, CROSS }

    public record OrderByItem(Expression expression, boolean ascending, Boolean nullsFirst) {}

    public record UpdateClause(String entityName, String alias, int position) {}

    public record DeleteClause(String entityName, String alias, int position) {}

    public sealed interface Expression permits
        Path, Literal, NamedParam, PositionalParam, Binary, Unary,
        Func, Aggregate, CaseExpr, SearchedCase, Coalesce, Nullif, NewExpr, Subquery {
        int position();
    }

    public record Path(String idVar, List<String> fields, int position) implements Expression {
        public Path {
            if (idVar == null || idVar.isEmpty()) {
                throw new IllegalArgumentException("idVar cannot be null or empty");
            }
            if (fields == null) {
                fields = List.of();
            }
        }
    }

    public enum LiteralType { STRING, INTEGER, FLOAT, BOOLEAN, NULL }

    public record Literal(String value, LiteralType type, int position) implements Expression {}

    public record NamedParam(String name, int position) implements Expression {}

    public record PositionalParam(int parameter, int position) implements Expression {}

    public record Binary(String operator, Expression left, Expression right, int position) implements Expression {}

    public record Unary(String operator, Expression operand, int position) implements Expression {}

    public record Func(String name, List<Expression> args, int position) implements Expression {}

    public record Aggregate(String function, boolean distinct, Expression argument, int position) implements Expression {}

    public record CaseExpr(Expression operand, List<WhenThen> whens, Expression elseExpr, int position) implements Expression {
        public record WhenThen(Expression when, Expression then) {}
    }

    public record SearchedCase(List<SearchedWhenThen> whens, Expression elseExpr, int position) implements Expression {
        public record SearchedWhenThen(Condition when, Expression then) {}
    }

    public record Coalesce(List<Expression> args, int position) implements Expression {}

    public record Nullif(Expression first, Expression second, int position) implements Expression {}

    public record NewExpr(String constructor, List<Expression> args, int position) implements Expression {}

    public record Subquery(JpqlStatement statement, int position) implements Expression {}

    public sealed interface Condition permits
        Comparison, Between, In, Like, IsNull, IsEmpty, Member,
        Exists, AllAny, And, Or, Not {
        int position();
    }

    public record Comparison(String operator, Expression left, Expression right, int position) implements Condition {}

    public record Between(Expression expression, Expression low, Expression high, boolean negated, int position) implements Condition {}

    public record In(Expression expression, List<Expression> items, boolean negated, int position) implements Condition {}

    public record Like(Expression expression, Expression pattern, Character escape, boolean negated, int position) implements Condition {}

    public record IsNull(Expression expression, boolean negated, int position) implements Condition {}

    public record IsEmpty(Expression expression, boolean negated, int position) implements Condition {}

    public record Member(Expression expression, String collection, boolean negated, int position) implements Condition {}

    public record Exists(Expression subquery, boolean negated, int position) implements Condition {}

    public record AllAny(Expression expression, String operator, String quantifier, Expression subquery, int position) implements Condition {}

    public record And(List<Condition> conditions, int position) implements Condition {}

    public record Or(List<Condition> conditions, int position) implements Condition {}

    public record Not(Condition condition, int position) implements Condition {}
}
