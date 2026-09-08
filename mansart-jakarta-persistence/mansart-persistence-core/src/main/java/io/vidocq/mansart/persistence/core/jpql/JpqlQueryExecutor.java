/*
 * Copyright (c) 2025 Vidocq contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.vidocq.mansart.persistence.core.jpql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.vidocq.mansart.data.dialect.Attribute;
import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.data.dialect.OrderBy;
import io.vidocq.mansart.data.dialect.SqlFragment;
import io.vidocq.mansart.data.dialect.attribute.JoinPath;
import io.vidocq.mansart.data.dialect.attribute.JoinedAttribute;
import io.vidocq.mansart.data.dialect.attribute.ReferenceAttribute;
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
        List<BindParameter> bindParameters,
        boolean aggregate,
        List<ProjectionInfo> projections
    ) {
        public QueryPlan {
            Objects.requireNonNull(sqlFragment, "sqlFragment must not be null");
            Objects.requireNonNull(entityClass, "entityClass must not be null");
            Objects.requireNonNull(dialectModel, "dialectModel must not be null");
            bindParameters = List.copyOf(bindParameters == null ? List.of() : bindParameters);
            projections = List.copyOf(projections == null ? List.of() : projections);
        }
    }

    public record ProjectionInfo(String function, boolean distinct, String columnSql, Class<?> resultType) {}

    public record UpdatePlan(
        String sql,
        Class<?> entityClass,
        EntityModel dialectModel,
        List<BindParameter> bindParameters
    ) {
        public UpdatePlan {
            Objects.requireNonNull(sql, "sql must not be null");
            Objects.requireNonNull(entityClass, "entityClass must not be null");
            Objects.requireNonNull(dialectModel, "dialectModel must not be null");
            bindParameters = List.copyOf(bindParameters == null ? List.of() : bindParameters);
        }
    }

    public record DeletePlan(
        String sql,
        Class<?> entityClass,
        EntityModel dialectModel,
        List<BindParameter> bindParameters
    ) {
        public DeletePlan {
            Objects.requireNonNull(sql, "sql must not be null");
            Objects.requireNonNull(entityClass, "entityClass must not be null");
            Objects.requireNonNull(dialectModel, "dialectModel must not be null");
            bindParameters = List.copyOf(bindParameters == null ? List.of() : bindParameters);
        }
    }

    private record PlanContext(
        EntityModel rootModel,
        Map<String, EntityModel> aliasToModel,
        Map<String, JoinPath> aliasToJoinPath
    ) {}

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

        // Build alias-to-model and alias-to-joinPath maps
        PlanContext context = buildPlanContext(stmt.from(), dialectModel, entityClass);

        // Translate WHERE clause
        io.vidocq.mansart.data.dialect.Where where = stmt.where() == null 
            ? io.vidocq.mansart.data.dialect.Where.ALWAYS_TRUE 
            : translateCondition(stmt.where().condition(), context);

        // If there are explicit joins but no WHERE clause, inject a tautology that references the joins
        // so the dialect includes them in the SQL.
        if (stmt.where() == null && hasExplicitJoins(context)) {
            where = injectJoinTautology(where, context);
        }

        // Translate ORDER BY clause
        OrderBy orderBy = translateOrderBy(stmt.orderBy(), context);

        // Check if this is an aggregate query
        boolean isAggregate = isAggregateQuery(stmt.select().items());
        List<ProjectionInfo> projections = isAggregate ? buildProjections(stmt.select().items(), context) : List.of();

        // Generate SQL fragment
        SqlFragment sqlFragment;
        if (isAggregate) {
            // For aggregate queries, use selectColumns with projected columns
            List<Dialect.ProjectedColumn> projectedColumns = projections.stream()
                .map(proj -> (Dialect.ProjectedColumn) new Dialect.ProjectedColumn.Expr(proj.columnSql()))
                .toList();
            sqlFragment = dialect.selectColumns(
                context.rootModel(),
                projectedColumns,
                where,
                orderBy,
                io.vidocq.mansart.data.dialect.Pagination.NONE
            );
        } else {
            sqlFragment = dialect.select(
                context.rootModel(),
                where,
                orderBy,
                io.vidocq.mansart.data.dialect.Pagination.NONE
            );
        }

        // Extract bind parameters from WHERE condition
        List<BindParameter> bindParameters = extractBindParameters(
            stmt.where() == null ? null : stmt.where().condition(),
            context
        );

        return new QueryPlan(sqlFragment, entityClass, context.rootModel(), bindParameters, isAggregate, projections);
    }

    public UpdatePlan plan(JpqlAst.UpdateStatement stmt) {
        JpqlAst.UpdateClause update = stmt.update();
        String entityName = update.entityName();

        Class<?> entityClass = callback.resolveEntityName(entityName);
        if (entityClass == null) {
            throw new IllegalArgumentException("Unknown entity name: " + entityName);
        }

        io.vidocq.mansart.persistence.spi.EntityModel spiModel = callback.getEntityModel(entityClass);
        EntityModel dialectModel = adapter.adapt(spiModel);

        String alias = update.alias();
        Map<String, EntityModel> aliasMap = new HashMap<>();
        if (alias != null && !alias.isEmpty()) {
            aliasMap.put(alias, dialectModel);
        }
        PlanContext context = new PlanContext(dialectModel, aliasMap, java.util.Map.of());

        // Resolve SET items to attributes
        List<Attribute<?, ?>> setAttrs = new ArrayList<>();
        List<BindParameter> setBindParams = new ArrayList<>();
        for (JpqlAst.SetItem item : update.setItems()) {
            Attribute<?, ?> attr = findAttribute(dialectModel, item.field());
            setAttrs.add(attr);
            Object value = extractExpressionValue(item.value());
            String paramName = null;
            Integer paramPosition = null;
            if (item.value() instanceof JpqlAst.NamedParam np) {
                paramName = np.name();
            } else if (item.value() instanceof JpqlAst.PositionalParam pp) {
                paramPosition = pp.parameter();
            }
            setBindParams.add(new BindParameter(value, paramName, paramPosition, attr));
        }

        // Translate WHERE clause
        io.vidocq.mansart.data.dialect.Where where = stmt.where() == null
            ? io.vidocq.mansart.data.dialect.Where.ALWAYS_TRUE
            : translateCondition(stmt.where().condition(), context);

        // Generate SQL via dialect.updateSet
        SqlFragment sqlFragment = dialect.updateSet(dialectModel, setAttrs, where);

        // Extract WHERE bind parameters
        List<BindParameter> whereBindParams = extractBindParameters(
            stmt.where() == null ? null : stmt.where().condition(),
            context
        );

        // SET bind params come first (matching SQL column order), then WHERE params
        List<BindParameter> allParams = new ArrayList<>(setBindParams);
        allParams.addAll(whereBindParams);

        return new UpdatePlan(sqlFragment.sql(), entityClass, dialectModel, allParams);
    }

    public DeletePlan plan(JpqlAst.DeleteStatement stmt) {
        String entityName = stmt.delete().entityName();

        Class<?> entityClass = callback.resolveEntityName(entityName);
        if (entityClass == null) {
            throw new IllegalArgumentException("Unknown entity name: " + entityName);
        }

        io.vidocq.mansart.persistence.spi.EntityModel spiModel = callback.getEntityModel(entityClass);
        EntityModel dialectModel = adapter.adapt(spiModel);

        String alias = stmt.delete().alias();
        Map<String, EntityModel> aliasMap = new HashMap<>();
        if (alias != null && !alias.isEmpty()) {
            aliasMap.put(alias, dialectModel);
        }
        PlanContext context = new PlanContext(dialectModel, aliasMap, java.util.Map.of());

        // Translate WHERE clause
        io.vidocq.mansart.data.dialect.Where where = stmt.where() == null
            ? io.vidocq.mansart.data.dialect.Where.ALWAYS_TRUE
            : translateCondition(stmt.where().condition(), context);

        // Generate SQL via dialect.delete
        SqlFragment sqlFragment = dialect.delete(dialectModel, where);

        // Extract WHERE bind parameters
        List<BindParameter> bindParameters = extractBindParameters(
            stmt.where() == null ? null : stmt.where().condition(),
            context
        );

        return new DeletePlan(sqlFragment.sql(), entityClass, dialectModel, bindParameters);
    }

    private PlanContext buildPlanContext(JpqlAst.FromClause fromClause, EntityModel rootModel, Class<?> rootEntityClass) {
        Map<String, EntityModel> aliasToModel = new HashMap<>();
        Map<String, JoinPath> aliasToJoinPath = new HashMap<>();
        
        // Add root alias
        String rootAlias = fromClause.declarations().getFirst() instanceof JpqlAst.RangeVarDecl rangeVar 
            ? rangeVar.alias() 
            : null;
        if (rootAlias != null) {
            aliasToModel.put(rootAlias, rootModel);
            aliasToJoinPath.put(rootAlias, null); // Root has no join path
        }
        
        // Process JOIN declarations
        for (JpqlAst.IdentificationVarDeclaration decl : fromClause.declarations()) {
            if (decl instanceof JpqlAst.JoinDecl joinDecl) {
                // Check join type support
                if (joinDecl.type() != JpqlAst.JoinType.INNER) {
                    throw new PersistenceException("not implemented: " + joinDecl.type() + " join");
                }
                
                // Parse join path: e.g., "e.department" -> root alias "e", attribute "department"
                String[] pathParts = joinDecl.path().split("\\.");
                if (pathParts.length != 2) {
                    throw new IllegalArgumentException("Invalid join path: " + joinDecl.path() + ". Expected format: <alias>.<attribute>");
                }
                
                String sourceAlias = pathParts[0];
                String relationName = pathParts[1];
                
                // Look up source model
                EntityModel sourceModel = aliasToModel.get(sourceAlias);
                if (sourceModel == null) {
                    throw new IllegalArgumentException("Unknown alias in join: " + sourceAlias);
                }
                
                // Find the relationship attribute
                Attribute<?, ?> attr = findAttribute(sourceModel, relationName);
                if (!(attr instanceof ReferenceAttribute<?, ?> refAttr)) {
                    throw new IllegalArgumentException("Attribute is not a relationship: " + relationName);
                }
                
                // Build JoinPath step
                Class<?> targetEntityClass = refAttr.javaType();
                io.vidocq.mansart.persistence.spi.EntityModel targetSpiModel = callback.getEntityModel(targetEntityClass);
                EntityModel targetModel = adapter.adapt(targetSpiModel);
                
                // Get join column info
                String foreignKeyColumn = refAttr.columnName();
                String referencedColumn = refAttr.referencedColumn();
                
                JoinPath.Step step = new JoinPath.Step(
                    refAttr.name(),
                    foreignKeyColumn,
                    referencedColumn,
                    targetModel.tableName(),
                    targetModel.schema(),
                    targetEntityClass
                );
                
                JoinPath joinPath = aliasToJoinPath.get(sourceAlias);
                if (joinPath == null) {
                    joinPath = JoinPath.of(step);
                } else {
                    joinPath = joinPath.append(step);
                }
                
                // Store in maps
                aliasToModel.put(joinDecl.alias(), targetModel);
                aliasToJoinPath.put(joinDecl.alias(), joinPath);
            }
        }
        
        return new PlanContext(rootModel, aliasToModel, aliasToJoinPath);
    }

    /**
     * Returns true if there are explicit joins (non-root aliases with a non-null JoinPath).
     */
    private boolean hasExplicitJoins(PlanContext context) {
        for (Map.Entry<String, JoinPath> e : context.aliasToJoinPath().entrySet()) {
            if (e.getValue() != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Injects a tautology that references all explicit join paths, forcing the dialect to include them.
     */
    private io.vidocq.mansart.data.dialect.Where injectJoinTautology(
            io.vidocq.mansart.data.dialect.Where where,
            PlanContext context) {
        List<io.vidocq.mansart.data.dialect.Where> children = new ArrayList<>();
        children.add(where);
        
        // Get the first attribute from the root model to use for the tautology
        Attribute<?, ?> rootAttr = null;
        for (Object attrObj : context.rootModel().attributes()) {
            if (attrObj instanceof Attribute<?, ?> attr && !"class".equals(attr.name())) {
                rootAttr = attr;
                break;
            }
        }
        
        for (Map.Entry<String, JoinPath> e : context.aliasToJoinPath().entrySet()) {
            if (e.getValue() != null && rootAttr != null) {
                JoinedAttribute<?, ?> joinedAttr = new JoinedAttribute<>(rootAttr, e.getValue(), context.rootModel().entityClass());
                children.add(new io.vidocq.mansart.data.dialect.Where.IsNotNull(joinedAttr));
            }
        }
        
        return new io.vidocq.mansart.data.dialect.Where.And(children);
    }

    private OrderBy translateOrderBy(JpqlAst.OrderByClause orderByClause, PlanContext context) {
        if (orderByClause == null || orderByClause.items().isEmpty()) {
            return OrderBy.NONE;
        }

        List<io.vidocq.mansart.data.dialect.OrderBy.Order> orders = new ArrayList<>();
        for (JpqlAst.OrderByItem item : orderByClause.items()) {
            Attribute<?, ?> attr = resolveAttribute(item.expression(), context);
            if (item.ascending()) {
                orders.add(io.vidocq.mansart.data.dialect.OrderBy.Order.asc(attr));
            } else {
                orders.add(io.vidocq.mansart.data.dialect.OrderBy.Order.desc(attr));
            }
        }
        return new OrderBy(orders);
    }

    private io.vidocq.mansart.data.dialect.Where translateCondition(JpqlAst.Condition cond, PlanContext context) {
        if (cond == null) {
            return io.vidocq.mansart.data.dialect.Where.ALWAYS_TRUE;
        }
        if (cond instanceof JpqlAst.Comparison cmp) {
            return translateComparison(cmp, context);
        }
        if (cond instanceof JpqlAst.And and) {
            List<io.vidocq.mansart.data.dialect.Where> children = and.conditions().stream().map(c -> translateCondition(c, context)).toList();
            return new io.vidocq.mansart.data.dialect.Where.And(children);
        }
        if (cond instanceof JpqlAst.Or or) {
            List<io.vidocq.mansart.data.dialect.Where> children = or.conditions().stream().map(c -> translateCondition(c, context)).toList();
            return new io.vidocq.mansart.data.dialect.Where.Or(children);
        }
        if (cond instanceof JpqlAst.Not not) {
            return new io.vidocq.mansart.data.dialect.Where.Not(translateCondition(not.condition(), context));
        }
        if (cond instanceof JpqlAst.IsNull isNull) {
            Attribute<?, ?> attr = resolveAttribute(isNull.expression(), context);
            return isNull.negated() ? new io.vidocq.mansart.data.dialect.Where.IsNotNull(attr) : new io.vidocq.mansart.data.dialect.Where.IsNull(attr);
        }
        if (cond instanceof JpqlAst.Between between) {
            Attribute<?, ?> attr = resolveAttribute(between.expression(), context);
            var w = new io.vidocq.mansart.data.dialect.Where.Between(attr);
            return between.negated() ? new io.vidocq.mansart.data.dialect.Where.Not(w) : w;
        }
        if (cond instanceof JpqlAst.In in) {
            Attribute<?, ?> attr = resolveAttribute(in.expression(), context);
            var w = new io.vidocq.mansart.data.dialect.Where.In(attr, in.items().size());
            return in.negated() ? new io.vidocq.mansart.data.dialect.Where.Not(w) : w;
        }
        if (cond instanceof JpqlAst.Like like) {
            Attribute<?, ?> attr = resolveAttribute(like.expression(), context);
            var w = new io.vidocq.mansart.data.dialect.Where.Like(attr);
            return like.negated() ? new io.vidocq.mansart.data.dialect.Where.Not(w) : w;
        }
        if (cond instanceof JpqlAst.IsEmpty isEmpty) {
            Attribute<?, ?> attr = resolveAttribute(isEmpty.expression(), context);
            return isEmpty.negated()
                ? new io.vidocq.mansart.data.dialect.Where.IsNotNull(attr)
                : new io.vidocq.mansart.data.dialect.Where.IsNull(attr);
        }
        throw new UnsupportedOperationException("Condition type not supported: " + cond.getClass().getSimpleName());
    }

    private io.vidocq.mansart.data.dialect.Where translateComparison(JpqlAst.Comparison cmp, PlanContext context) {
        Attribute<?, ?> attr = resolveAttribute(cmp.left(), context);
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

    private Attribute<?, ?> resolveAttribute(JpqlAst.Expression expr, PlanContext context) {
        if (!(expr instanceof JpqlAst.Path path)) {
            throw new UnsupportedOperationException("Only simple path expressions are supported for attribute resolution, got: " + expr.getClass().getSimpleName());
        }

        List<String> fields = path.fields();
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("Path expression has no fields");
        }

        String alias = path.idVar();
        EntityModel currentModel = context.aliasToModel().getOrDefault(alias, context.rootModel());
        JoinPath currentJoinPath = context.aliasToJoinPath().get(alias); // may be null for root

        // Walk the field segments
        for (int i = 0; i < fields.size(); i++) {
            String fieldName = fields.get(i);
            Attribute<?, ?> attr = findAttribute(currentModel, fieldName);
            
            if (attr instanceof ReferenceAttribute<?, ?> refAttr && i < fields.size() - 1) {
                // This is a relationship attribute, not the leaf — traverse the join
                // Get the target entity model
                Class<?> targetEntityClass = refAttr.javaType();
                io.vidocq.mansart.persistence.spi.EntityModel targetSpiModel = callback.getEntityModel(targetEntityClass);
                EntityModel targetDialectModel = adapter.adapt(targetSpiModel);
                
                // Build the JoinPath step
                JoinPath.Step step = new JoinPath.Step(
                    refAttr.name(),
                    refAttr.columnName(),
                    refAttr.referencedColumn(),
                    targetDialectModel.tableName(),
                    targetDialectModel.schema(),
                    targetEntityClass
                );
                
                // Append step to current join path
                currentJoinPath = currentJoinPath == null ? JoinPath.of(step) : currentJoinPath.append(step);
                currentModel = targetDialectModel;
            } else if (i == fields.size() - 1) {
                // Leaf attribute — wrap in JoinedAttribute if we have a join path
                if (currentJoinPath != null) {
                    return new JoinedAttribute<>(attr, currentJoinPath, context.rootModel().entityClass());
                }
                return attr;
            } else {
                // Non-reference intermediate field — shouldn't happen for valid JPQL
                throw new UnsupportedOperationException("Cannot traverse non-reference attribute: " + fieldName);
            }
        }

        // Should not reach here
        throw new IllegalArgumentException("Could not resolve attribute: " + fields);
    }

    private Attribute<?, ?> findAttribute(EntityModel model, String attrName) {
        for (Object attrObj : model.attributes()) {
            @SuppressWarnings("unchecked")
            Attribute<?, ?> attr = (Attribute<?, ?>) attrObj;
            if (attr.name().equalsIgnoreCase(attrName)) {
                return attr;
            }
        }
        throw new IllegalArgumentException("Attribute not found: " + attrName + " in entity " + model.tableName());
    }

    private List<JpqlQueryExecutor.BindParameter> extractBindParameters(JpqlAst.Condition cond, PlanContext context) {
        if (cond == null) {
            return Collections.emptyList();
        }
        List<BindParameter> params = new ArrayList<>();
        extractBindParameters(cond, params, context);
        return params;
    }

    private void extractBindParameters(JpqlAst.Condition cond, List<BindParameter> params, Attribute<?, ?> leftAttr, PlanContext context) {
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
            and.conditions().forEach(c -> extractBindParameters(c, params, leftAttr, context));
            return;
        }
        if (cond instanceof JpqlAst.Or or) {
            or.conditions().forEach(c -> extractBindParameters(c, params, leftAttr, context));
            return;
        }
        if (cond instanceof JpqlAst.Not not) {
            extractBindParameters(not.condition(), params, leftAttr, context);
            return;
        }
        if (cond instanceof JpqlAst.IsNull) {
            // no bind parameters
            return;
        }
        if (cond instanceof JpqlAst.Between between) {
            // low comes before high, matching dialect rendering order
            extractBindParameterFromExpr(between.low(), leftAttr, params);
            extractBindParameterFromExpr(between.high(), leftAttr, params);
            return;
        }
        if (cond instanceof JpqlAst.In in) {
            for (JpqlAst.Expression item : in.items()) {
                extractBindParameterFromExpr(item, leftAttr, params);
            }
            return;
        }
        if (cond instanceof JpqlAst.Like like) {
            extractBindParameterFromExpr(like.pattern(), leftAttr, params);
            return;
        }
        if (cond instanceof JpqlAst.IsEmpty) {
            // no bind parameters
            return;
        }
        throw new UnsupportedOperationException("Cannot extract bind parameters from condition: " + cond.getClass().getSimpleName());
    }

    private void extractBindParameters(JpqlAst.Condition cond, List<BindParameter> params, PlanContext context) {
        if (cond == null) {
            return;
        }
        if (cond instanceof JpqlAst.Comparison cmp) {
            Attribute<?, ?> leftAttr = resolveAttribute(cmp.left(), context);
            extractBindParameters(cmp, params, leftAttr, context);
            return;
        }
        if (cond instanceof JpqlAst.And and) {
            and.conditions().forEach(c -> extractBindParameters(c, params, context));
            return;
        }
        if (cond instanceof JpqlAst.Or or) {
            or.conditions().forEach(c -> extractBindParameters(c, params, context));
            return;
        }
        if (cond instanceof JpqlAst.Not not) {
            extractBindParameters(not.condition(), params, context);
            return;
        }
        if (cond instanceof JpqlAst.IsNull) {
            return; // no bind parameters
        }
        if (cond instanceof JpqlAst.Between between) {
            Attribute<?, ?> attr = resolveAttribute(between.expression(), context);
            // low comes before high, matching dialect rendering order
            extractBindParameterFromExpr(between.low(), attr, params);
            extractBindParameterFromExpr(between.high(), attr, params);
            return;
        }
        if (cond instanceof JpqlAst.In in) {
            Attribute<?, ?> attr = resolveAttribute(in.expression(), context);
            for (JpqlAst.Expression item : in.items()) {
                extractBindParameterFromExpr(item, attr, params);
            }
            return;
        }
        if (cond instanceof JpqlAst.Like like) {
            Attribute<?, ?> attr = resolveAttribute(like.expression(), context);
            extractBindParameterFromExpr(like.pattern(), attr, params);
            return;
        }
        if (cond instanceof JpqlAst.IsEmpty) {
            return; // no bind parameters
        }
        throw new UnsupportedOperationException("Cannot extract bind parameters from condition: " + cond.getClass().getSimpleName());
    }

    private void extractBindParameterFromExpr(JpqlAst.Expression expr, Attribute<?, ?> attr,
            List<BindParameter> params) {
        Object value = extractExpressionValue(expr);
        String paramName = null;
        Integer paramPosition = null;
        if (expr instanceof JpqlAst.NamedParam np) {
            paramName = np.name();
        } else if (expr instanceof JpqlAst.PositionalParam pp) {
            paramPosition = pp.parameter();
        }
        params.add(new BindParameter(value, paramName, paramPosition, attr));
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

    /**
     * Returns true if any select item is an Aggregate expression.
     */
    private boolean isAggregateQuery(List<JpqlAst.Expression> selectItems) {
        return selectItems.stream()
            .anyMatch(expr -> expr instanceof JpqlAst.Aggregate);
    }

    /**
     * Builds ProjectionInfo for each aggregate expression in the SELECT clause.
     * Returns a list of projection info objects that describe how to materialize
     * the result set row.
     */
    private List<ProjectionInfo> buildProjections(List<JpqlAst.Expression> selectItems, PlanContext context) {
        List<ProjectionInfo> projections = new ArrayList<>();
        for (JpqlAst.Expression expr : selectItems) {
            if (expr instanceof JpqlAst.Aggregate agg) {
                projections.add(buildProjectionInfo(agg, context));
            } else {
                throw new UnsupportedOperationException("Only aggregate expressions are supported in aggregate queries, got: " + expr.getClass().getSimpleName());
            }
        }
        return projections;
    }

    /**
     * Builds a single ProjectionInfo for an aggregate expression.
     */
    private ProjectionInfo buildProjectionInfo(JpqlAst.Aggregate aggregate, PlanContext context) {
        String function = aggregate.function().toUpperCase();
        boolean distinct = aggregate.distinct();
        JpqlAst.Expression arg = aggregate.argument();

        // Determine the SQL expression for this aggregate
        String columnSql;
        Class<?> resultType;

        if (arg instanceof JpqlAst.Path path) {
            // Path expression: e.field or e (for COUNT(*))
            if (path.fields().isEmpty()) {
                // COUNT(e) where e is just the alias
                if ("COUNT".equals(function)) {
                    columnSql = "COUNT(*)";
                    resultType = Long.class;
                } else {
                    throw new UnsupportedOperationException("Aggregate function " + function + " with entity reference (*) is only supported for COUNT");
                }
            } else {
                // e.field - resolve to column name
                Attribute<?, ?> attr = resolveAttribute(arg, context);
                String columnName = attr.columnName();

                // Build SQL: FUNCTION(DISTINCT "column")
                String distinctSql = distinct ? "DISTINCT " : "";
                if ("COUNT".equals(function)) {
                    columnSql = "COUNT(" + distinctSql + "\"" + columnName + "\")";
                    resultType = Long.class;
                } else if ("SUM".equals(function)) {
                    columnSql = function + "(" + distinctSql + "\"" + columnName + "\")";
                    // SUM returns Long for integer columns
                    resultType = Long.class;
                } else if ("AVG".equals(function)) {
                    columnSql = function + "(" + distinctSql + "\"" + columnName + "\")";
                    resultType = Double.class;
                } else if ("MIN".equals(function)) {
                    columnSql = function + "(" + "\"" + columnName + "\")";
                    resultType = attr.javaType();
                } else if ("MAX".equals(function)) {
                    columnSql = function + "(" + "\"" + columnName + "\")";
                    resultType = attr.javaType();
                } else {
                    throw new UnsupportedOperationException("Aggregate function not supported: " + function);
                }
            }
        } else {
            throw new UnsupportedOperationException("Only path expressions are supported as aggregate arguments, got: " + arg.getClass().getSimpleName());
        }

        return new ProjectionInfo(function, distinct, columnSql, resultType);
    }

}
