/*
 * Copyright (c) 2025 Vidocq contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.vidocq.mansart.persistence.core.jpql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.vidocq.mansart.data.dialect.Attribute;
import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.data.dialect.OrderBy;
import io.vidocq.mansart.data.dialect.SqlFragment;
import io.vidocq.mansart.persistence.core.dialect.DialectEntityModelAdapter;
import io.vidocq.mansart.persistence.core.jpql.JpqlAst;
import io.vidocq.mansart.persistence.core.runtime.MansartCallback;
import jakarta.persistence.PersistenceException;

/**
 * Plans a JPQL SELECT statement into a SQL fragment and bind parameters.
 */
public final class JpqlQueryExecutor {

    private final Dialect dialect;
    private final MansartCallback callback;
    private final DialectEntityModelAdapter adapter;

    public JpqlQueryExecutor(Dialect dialect, MansartCallback callback, DialectEntityModelAdapter adapter) {
        if (dialect == null) {
            throw new PersistenceException("dialect must not be null");
        }
        if (callback == null) {
            throw new PersistenceException("callback must not be null");
        }
        if (adapter == null) {
            throw new PersistenceException("adapter must not be null");
        }
        this.dialect = dialect;
        this.callback = callback;
        this.adapter = adapter;
    }

    public record QueryPlan(
        SqlFragment sqlFragment,
        Class<?> entityClass,
        io.vidocq.mansart.data.dialect.EntityModel dialectModel,
        List<BindParameter> bindParameters
    ) {
        public QueryPlan {
            Objects.requireNonNull(sqlFragment, "sqlFragment must not be null");
            Objects.requireNonNull(entityClass, "entityClass must not be null");
            Objects.requireNonNull(dialectModel, "dialectModel must not be null");
            bindParameters = List.copyOf(bindParameters == null ? List.of() : bindParameters);
        }
    }

    public record BindParameter(Object value, String paramName, Integer paramPosition, Attribute<?, ?> target) {
        public BindParameter {
            if (paramName == null && paramPosition == null && value == null) {
                throw new IllegalArgumentException("BindParameter must have at least one of: value, paramName, paramPosition");
            }
        }

        public boolean isNamed() { return paramName != null; }
        public boolean isPositional() { return paramPosition != null; }
        public boolean isLiteral() { return value != null && paramName == null && paramPosition == null; }
    }

    public QueryPlan plan(JpqlAst.SelectStatement stmt) {
        // Extract entity name from FROM clause
        var fromDecl = stmt.from().declarations().getFirst();
        if (!(fromDecl instanceof JpqlAst.RangeVarDecl rangeVar)) {
            throw new UnsupportedOperationException("Only simple range variable declarations are supported in FROM clause");
        }
        String entityName = rangeVar.entityName();

        // Resolve entity class
        Class<?> entityClass = callback.resolveEntityName(entityName);
        if (entityClass == null) {
            throw new IllegalArgumentException("Unknown entity name: " + entityName);
        }

        // Get SPI model and adapt to dialect model
        io.vidocq.mansart.persistence.spi.EntityModel spiModel = callback.getEntityModel(entityClass);
        EntityModel dialectModel = adapter.adapt(spiModel);

        // Translate WHERE clause
        io.vidocq.mansart.data.dialect.Where where = stmt.where() == null ? io.vidocq.mansart.data.dialect.Where.ALWAYS_TRUE : translateCondition(stmt.where().condition(), dialectModel);

        // Translate ORDER BY clause
        OrderBy orderBy = translateOrderBy(stmt.orderBy(), dialectModel);

        // Generate SQL fragment
        SqlFragment sqlFragment = dialect.select(dialectModel, where, orderBy, io.vidocq.mansart.data.dialect.Pagination.NONE);

        // Extract bind parameters from WHERE condition
        List<BindParameter> bindParameters = extractBindParameters(
            stmt.where() == null ? null : stmt.where().condition(),
            dialectModel
        );

        return new QueryPlan(sqlFragment, entityClass, dialectModel, bindParameters);
    }

    private OrderBy translateOrderBy(JpqlAst.OrderByClause orderByClause, EntityModel dialectModel) {
        if (orderByClause == null || orderByClause.items().isEmpty()) {
            return OrderBy.NONE;
        }

        List<io.vidocq.mansart.data.dialect.OrderBy.Order> orders = new ArrayList<>();
        for (JpqlAst.OrderByItem item : orderByClause.items()) {
            Attribute<?, ?> attr = resolveAttribute(item.expression(), dialectModel);
            if (item.ascending()) {
                orders.add(io.vidocq.mansart.data.dialect.OrderBy.Order.asc(attr));
            } else {
                orders.add(io.vidocq.mansart.data.dialect.OrderBy.Order.desc(attr));
            }
        }
        return new OrderBy(orders);
    }

    private io.vidocq.mansart.data.dialect.Where translateCondition(JpqlAst.Condition cond, EntityModel dialectModel) {
        if (cond == null) {
            return io.vidocq.mansart.data.dialect.Where.ALWAYS_TRUE;
        }
        if (cond instanceof JpqlAst.Comparison cmp) {
            return translateComparison(cmp, dialectModel);
        }
        if (cond instanceof JpqlAst.And and) {
            List<io.vidocq.mansart.data.dialect.Where> children = and.conditions().stream().map(c -> translateCondition(c, dialectModel)).toList();
            return new io.vidocq.mansart.data.dialect.Where.And(children);
        }
        if (cond instanceof JpqlAst.Or or) {
            List<io.vidocq.mansart.data.dialect.Where> children = or.conditions().stream().map(c -> translateCondition(c, dialectModel)).toList();
            return new io.vidocq.mansart.data.dialect.Where.Or(children);
        }
        if (cond instanceof JpqlAst.Not not) {
            return new io.vidocq.mansart.data.dialect.Where.Not(translateCondition(not.condition(), dialectModel));
        }
        if (cond instanceof JpqlAst.IsNull isNull) {
            Attribute<?, ?> attr = resolveAttribute(isNull.expression(), dialectModel);
            return isNull.negated() ? new io.vidocq.mansart.data.dialect.Where.IsNotNull(attr) : new io.vidocq.mansart.data.dialect.Where.IsNull(attr);
        }
        throw new UnsupportedOperationException("Condition type not supported: " + cond.getClass().getSimpleName());
    }

    private io.vidocq.mansart.data.dialect.Where translateComparison(JpqlAst.Comparison cmp, EntityModel dialectModel) {
        Attribute<?, ?> attr = resolveAttribute(cmp.left(), dialectModel);
        String op = cmp.operator();
        if ("=".equals(op)) {
            return new io.vidocq.mansart.data.dialect.Where.Eq(attr);
        }
        if ("!=".equals(op) || "<>".equals(op)) {
            return new io.vidocq.mansart.data.dialect.Where.NotEq(attr);
        }
        if ("<".equals(op)) {
            return new io.vidocq.mansart.data.dialect.Where.Lt(attr);
        }
        if ("<=".equals(op)) {
            return new io.vidocq.mansart.data.dialect.Where.Lte(attr);
        }
        if (">".equals(op)) {
            return new io.vidocq.mansart.data.dialect.Where.Gt(attr);
        }
        if (">=".equals(op)) {
            return new io.vidocq.mansart.data.dialect.Where.Gte(attr);
        }
        if ("LIKE".equalsIgnoreCase(op)) {
            return new io.vidocq.mansart.data.dialect.Where.Like(attr);
        }
        throw new UnsupportedOperationException("Comparison operator not supported: " + op);
    }

    private Attribute<?, ?> resolveAttribute(JpqlAst.Expression expr, EntityModel dialectModel) {
        if (!(expr instanceof JpqlAst.Path path)) {
            throw new UnsupportedOperationException("Only simple path expressions are supported for attribute resolution, got: " + expr.getClass().getSimpleName());
        }

        // For now, ignore the idVar (alias) and resolve fields directly on the dialectModel
        // path.idVar() is the identification variable (alias) from the FROM clause
        // path.fields() contains the attribute path segments
        List<String> fields = path.fields();
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("Path expression has no fields");
        }

        // Look up attribute by name (case-insensitive) in dialectModel
        String attrName = fields.getFirst();
        for (Object attrObj : dialectModel.attributes()) {
            @SuppressWarnings("unchecked")
            io.vidocq.mansart.data.dialect.Attribute<?, ?> attr = (io.vidocq.mansart.data.dialect.Attribute<?, ?>) attrObj;
            if (attr.name().equalsIgnoreCase(attrName)) {
                return attr;
            }
        }

        throw new IllegalArgumentException("Attribute not found: " + attrName);
    }

    private List<JpqlQueryExecutor.BindParameter> extractBindParameters(JpqlAst.Condition cond, EntityModel dialectModel) {
        if (cond == null) {
            return Collections.emptyList();
        }
        List<BindParameter> params = new ArrayList<>();
        extractBindParameters(cond, params, dialectModel);
        return params;
    }

    private void extractBindParameters(JpqlAst.Condition cond, List<BindParameter> params, Attribute<?, ?> leftAttr, EntityModel dialectModel) {
        if (cond instanceof JpqlAst.Comparison cmp) {
            // Extract value from right-hand side expression
            Object value = extractExpressionValue(cmp.right());
            String paramName = null;
            Integer paramPosition = null;
            if (cmp.right() instanceof JpqlAst.NamedParam np) {
                paramName = np.name();
            } else if (cmp.right() instanceof JpqlAst.PositionalParam pp) {
                paramPosition = pp.parameter();
            }
            params.add(new BindParameter(value, paramName, paramPosition, leftAttr));
            return;
        }
        if (cond instanceof JpqlAst.And and) {
            and.conditions().forEach(c -> extractBindParameters(c, params, leftAttr, dialectModel));
            return;
        }
        if (cond instanceof JpqlAst.Or or) {
            or.conditions().forEach(c -> extractBindParameters(c, params, leftAttr, dialectModel));
            return;
        }
        if (cond instanceof JpqlAst.Not not) {
            extractBindParameters(not.condition(), params, leftAttr, dialectModel);
            return;
        }
        if (cond instanceof JpqlAst.IsNull) {
            // no bind parameters
            return;
        }
        throw new UnsupportedOperationException("Cannot extract bind parameters from condition: " + cond.getClass().getSimpleName());
    }

    private void extractBindParameters(JpqlAst.Condition cond, List<BindParameter> params, EntityModel dialectModel) {
        if (cond == null) {
            return;
        }
        if (cond instanceof JpqlAst.Comparison cmp) {
            Attribute<?, ?> leftAttr = resolveAttribute(cmp.left(), dialectModel);
            extractBindParameters(cmp, params, leftAttr, dialectModel);
            return;
        }
        if (cond instanceof JpqlAst.And and) {
            and.conditions().forEach(c -> extractBindParameters(c, params, dialectModel));
            return;
        }
        if (cond instanceof JpqlAst.Or or) {
            or.conditions().forEach(c -> extractBindParameters(c, params, dialectModel));
            return;
        }
        if (cond instanceof JpqlAst.Not not) {
            extractBindParameters(not.condition(), params, dialectModel);
            return;
        }
        if (cond instanceof JpqlAst.IsNull) {
            return; // no bind parameters
        }
        throw new UnsupportedOperationException("Cannot extract bind parameters from condition: " + cond.getClass().getSimpleName());
    }

    private Object extractExpressionValue(JpqlAst.Expression expr) {
        if (expr instanceof JpqlAst.Literal lit) {
            switch (lit.type()) {
                case STRING -> { return lit.value(); }
                case INTEGER -> { return Long.parseLong(lit.value()); }
                case FLOAT -> { return Double.parseDouble(lit.value()); }
                case BOOLEAN -> { return Boolean.parseBoolean(lit.value()); }
                case NULL -> { return null; }
                default -> throw new UnsupportedOperationException("Literal type not supported: " + lit.type());
            }
        }
        if (expr instanceof JpqlAst.NamedParam) {
            return null;
        }
        if (expr instanceof JpqlAst.PositionalParam) {
            return null;
        }
        throw new UnsupportedOperationException("Cannot extract value from expression: " + expr.getClass().getSimpleName());
    }
}
