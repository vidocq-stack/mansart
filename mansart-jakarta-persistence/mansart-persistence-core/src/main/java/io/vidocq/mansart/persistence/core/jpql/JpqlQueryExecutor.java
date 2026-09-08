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
import java.util.Set;

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
        Map<String, JoinPath> aliasToJoinPath,
        List<BindParameter> bindParameters
    ) {
        PlanContext {
            Objects.requireNonNull(rootModel, "rootModel must not be null");
            aliasToModel = aliasToModel == null ? Map.of() : Map.copyOf(aliasToModel);
            aliasToJoinPath = aliasToJoinPath == null ? Map.of() : new java.util.HashMap<>(aliasToJoinPath);
            bindParameters = bindParameters == null ? List.of() : List.copyOf(bindParameters);
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

        // Check if this is an aggregate or scalar projection query
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
        PlanContext context = new PlanContext(dialectModel, aliasMap, java.util.Map.of(), List.of());

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
        PlanContext context = new PlanContext(dialectModel, aliasMap, java.util.Map.of(), List.of());

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
        
        // Process all declarations in FROM clause
        // Handle RangeVarDecl (root entity) and JoinDecl (joins)
        for (JpqlAst.IdentificationVarDeclaration decl : fromClause.declarations()) {
            if (decl instanceof JpqlAst.RangeVarDecl rangeVar) {
                // Range variable declaration (root entity or additional FROM entity)
                String alias = rangeVar.alias();
                if (alias != null) {
                    // For the first RangeVarDecl, use the provided rootModel
                    // For additional RangeVarDecls, we need to resolve the entity
                    EntityModel model;
                    if (aliasToModel.isEmpty()) {
                        // First RangeVarDecl - use the rootModel parameter
                        model = rootModel;
                    } else {
                        // Additional RangeVarDecl - resolve the entity class and adapt
                        Class<?> entityClass = callback.resolveEntityName(rangeVar.entityName());
                        io.vidocq.mansart.persistence.spi.EntityModel spiModel = callback.getEntityModel(entityClass);
                        model = adapter.adapt(spiModel);
                    }
                    aliasToModel.put(alias, model);
                    aliasToJoinPath.put(alias, null); // No join path for root entities
                }
            } else if (decl instanceof JpqlAst.JoinDecl joinDecl) {
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
        
        return new PlanContext(rootModel, aliasToModel, aliasToJoinPath, new java.util.ArrayList<>());
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
            // Handle IN with subquery: when items contains a single Subquery expression
            if (in.items().size() == 1 && in.items().getFirst() instanceof JpqlAst.Subquery subquery) {
                return translateInSubquery(in.expression(), subquery, in.negated(), context);
            }
            Attribute<?, ?> attr = resolveAttribute(in.expression(), context);
            var w = new io.vidocq.mansart.data.dialect.Where.In(attr, in.items().size());
            return in.negated() ? new io.vidocq.mansart.data.dialect.Where.Not(w) : w;
        }
        if (cond instanceof JpqlAst.Exists exists) {
            return translateExists(exists, context);
        }
        if (cond instanceof JpqlAst.AllAny allAny) {
            return translateAllAny(allAny, context);
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

    private io.vidocq.mansart.data.dialect.Where translateExists(JpqlAst.Exists exists, PlanContext context) {
        // Translate the subquery to SQL and parameters
        SqlAndParams subqueryResult = translateSubquery((JpqlAst.Subquery) exists.subquery(), context);
        
        // Create EXISTS or NOT EXISTS condition
        String sql = exists.negated()
            ? "NOT EXISTS " + subqueryResult.sql()
            : "EXISTS " + subqueryResult.sql();
        return new io.vidocq.mansart.data.dialect.Where.RawSql(sql);
    }

    private io.vidocq.mansart.data.dialect.Where translateAllAny(JpqlAst.AllAny allAny, PlanContext context) {
        // Translate the left expression
        String leftSql = renderScalarExpr(allAny.expression(), context);
        String operator = allAny.operator();
        
        // Translate the subquery to SQL and parameters
        SqlAndParams subqueryResult = translateSubquery((JpqlAst.Subquery) allAny.subquery(), context);
        
        // Create quantified comparison condition
        String sql = leftSql + " " + operator + " " + allAny.quantifier() + " " + subqueryResult.sql();
        return new io.vidocq.mansart.data.dialect.Where.RawSql(sql);
    }

    private io.vidocq.mansart.data.dialect.Where translateInSubquery(JpqlAst.Expression expression, JpqlAst.Subquery subquery, boolean negated, PlanContext context) {
        // Translate the left expression (the value to check)
        String leftSql = renderScalarExpr(expression, context);
        
        // Translate the subquery to SQL and parameters
        SqlAndParams subqueryResult = translateSubquery(subquery, context);
        
        // Create IN or NOT IN condition
        String sql = negated
            ? leftSql + " NOT IN " + subqueryResult.sql()
            : leftSql + " IN " + subqueryResult.sql();
        return new io.vidocq.mansart.data.dialect.Where.RawSql(sql);
    }

    private PlanContext buildSubqueryContext(JpqlAst.SelectStatement subStmt, PlanContext outerContext) {
        JpqlAst.FromClause fromClause = subStmt.from();
        JpqlAst.RangeVarDecl rangeVar = (JpqlAst.RangeVarDecl) fromClause.declarations().getFirst();
        Class<?> subEntityClass = callback.resolveEntityName(rangeVar.entityName());
        io.vidocq.mansart.persistence.spi.EntityModel subSpiModel = callback.getEntityModel(subEntityClass);
        EntityModel subRootModel = adapter.adapt(subSpiModel);
        
        // Build the subquery's own context (only includes its own FROM aliases)
        PlanContext subContext = buildPlanContext(fromClause, subRootModel, subEntityClass);
        
        // Merge outer context's alias maps into subquery context so correlated paths (e.g., e.department.id in subquery) can be resolved.
        // Only add outer context aliases that don't already exist in the subquery context.
        // This is critical for EXISTS, IN, ALL/ANY subqueries that reference outer query aliases.
        Map<String, EntityModel> mergedAliasToModel = new HashMap<>(subContext.aliasToModel());
        outerContext.aliasToModel().forEach((alias, model) -> {
            if (!mergedAliasToModel.containsKey(alias)) {
                mergedAliasToModel.put(alias, model);
            }
        });
        
        Map<String, JoinPath> mergedAliasToJoinPath = new HashMap<>(subContext.aliasToJoinPath());
        outerContext.aliasToJoinPath().forEach((alias, joinPath) -> {
            if (!mergedAliasToJoinPath.containsKey(alias)) {
                mergedAliasToJoinPath.put(alias, joinPath);
            }
        });
        
        // Create new bind parameters list for subquery (initially empty, since we inline all values in subquery WHERE)
        List<BindParameter> subqueryBindParams = new ArrayList<>();
        
        return new PlanContext(
            subContext.rootModel(),
            mergedAliasToModel,
            mergedAliasToJoinPath,
            subqueryBindParams
        );
    }

    private SqlAndParams translateSubquery(JpqlAst.Subquery subquery, PlanContext context) {
        // Recursively translate the subquery statement to SQL
        // We need to create a minimal query plan for the subquery
        JpqlAst.SelectStatement stmt = (JpqlAst.SelectStatement) subquery.statement();
        
        // For EXISTS, we only need the WHERE clause translated
        // For IN, ALL/ANY, we need the SELECT to return a single column
        // For scalar subqueries in comparisons, we need the single value
        
        // Build a minimal plan context for the subquery using its own FROM clause
        // This merges outer context aliases into the subquery context so correlated paths (e.g., e.department.id in subquery) can be resolved
        PlanContext subqueryContext = buildSubqueryContext(stmt, context);
        
        // Translate WHERE clause if present - inline all values to avoid bind parameter issues
        io.vidocq.mansart.data.dialect.Where where = stmt.where() == null 
            ? io.vidocq.mansart.data.dialect.Where.ALWAYS_TRUE 
            : translateSubqueryCondition(stmt.where().condition(), subqueryContext);
        
        // Translate ORDER BY if present (though typically not used in subqueries)
        OrderBy orderBy = translateOrderBy(stmt.orderBy(), subqueryContext);
        
        // Generate SQL fragment for the subquery
        // We use selectColumns for scalar subqueries, select for EXISTS
        SqlFragment sqlFragment;
        if (stmt.select().items().size() == 1) {
            // Single column - use selectColumns
            JpqlAst.Expression selectItem = stmt.select().items().getFirst();
            String columnSql;
            if (selectItem instanceof JpqlAst.Path path) {
                Attribute<?, ?> attr = resolveAttribute(path, subqueryContext);
                columnSql = "\"" + attr.columnName() + "\"";
            } else {
                // For other expressions, render as SQL
                columnSql = renderScalarExpr(selectItem, subqueryContext);
            }
            sqlFragment = dialect.selectColumns(
                subqueryContext.rootModel(),
                List.of(new Dialect.ProjectedColumn.Expr(columnSql)),
                where,
                orderBy,
                io.vidocq.mansart.data.dialect.Pagination.NONE
            );
        } else {
            // Multiple columns - use select
            sqlFragment = dialect.select(
                subqueryContext.rootModel(),
                where,
                orderBy,
                io.vidocq.mansart.data.dialect.Pagination.NONE
            );
        }
        
        // Return both SQL and parameters (empty for subqueries with inlined values)
        return new SqlAndParams("(" + sqlFragment.sql() + ")", Collections.emptyList());
    }
    
    private record SqlAndParams(String sql, List<BindParameter> bindParameters) {}

    private io.vidocq.mansart.data.dialect.Where translateComparison(JpqlAst.Comparison cmp, PlanContext context) {
        String op = cmp.operator();
        
        // Handle subquery on left side
        if (cmp.left() instanceof JpqlAst.Subquery leftSubquery) {
            SqlAndParams leftResult = translateSubquery(leftSubquery, context);
            String leftSql = leftResult.sql();
            String rightSql = renderScalarExpr(cmp.right(), context);
            return new io.vidocq.mansart.data.dialect.Where.RawSql(leftSql + " " + op + " " + rightSql);
        }
        
        // Handle subquery on right side
        if (cmp.right() instanceof JpqlAst.Subquery rightSubquery) {
            String leftSql = renderScalarExpr(cmp.left(), context);
            SqlAndParams rightResult = translateSubquery(rightSubquery, context);
            String rightSql = rightResult.sql();
            return new io.vidocq.mansart.data.dialect.Where.RawSql(leftSql + " " + op + " " + rightSql);
        }
        
        // Handle both sides being Path expressions (correlated subquery comparison)
        if (cmp.left() instanceof JpqlAst.Path && cmp.right() instanceof JpqlAst.Path) {
            String leftSql = renderScalarExpr(cmp.left(), context);
            String rightSql = renderScalarExpr(cmp.right(), context);
            return new io.vidocq.mansart.data.dialect.Where.RawSql(leftSql + " " + op + " " + rightSql);
        }
        
        // Handle non-path expressions (functions, literals, etc.) on left side
        if (!(cmp.left() instanceof JpqlAst.Path)) {
            String leftSql = renderScalarExpr(cmp.left(), context);
            String rightSql = renderScalarExpr(cmp.right(), context);
            return new io.vidocq.mansart.data.dialect.Where.RawSql(leftSql + " " + op + " " + rightSql);
        }
        
        // Handle regular path expression comparison
        Attribute<?, ?> attr = resolveAttribute(cmp.left(), context);
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

    /**
     * Translates a condition for use in a subquery, inlining all literal values
     * to avoid bind parameter issues. Returns a Where.RawSql with the complete
     * condition rendered as SQL.
     */
    private io.vidocq.mansart.data.dialect.Where translateSubqueryCondition(JpqlAst.Condition cond, PlanContext context) {
        if (cond instanceof JpqlAst.Comparison cmp) {
            // Handle both sides being Path expressions (correlated comparison)
            if (cmp.left() instanceof JpqlAst.Path && cmp.right() instanceof JpqlAst.Path) {
                String leftSql = renderScalarExpr(cmp.left(), context);
                String rightSql = renderScalarExpr(cmp.right(), context);
                String op = cmp.operator();
                return new io.vidocq.mansart.data.dialect.Where.RawSql(leftSql + " " + op + " " + rightSql);
            }
            
            String leftSql = renderScalarExpr(cmp.left(), context);
            String rightSql;
            
            // Handle Path on right side (correlated comparison)
            if (cmp.right() instanceof JpqlAst.Path) {
                rightSql = renderScalarExpr(cmp.right(), context);
            }
            // Handle Literal - inline the value
            else if (cmp.right() instanceof JpqlAst.Literal lit) {
                switch (lit.type()) {
                    case STRING -> rightSql = "'" + lit.value() + "'";
                    case INTEGER -> rightSql = lit.value();
                    case FLOAT -> rightSql = lit.value();
                    case BOOLEAN -> rightSql = lit.value().toUpperCase();
                    case NULL -> rightSql = "NULL";
                    default -> throw new UnsupportedOperationException("Literal type not supported in subquery: " + lit.type());
                }
            }
            // Handle NamedParam and PositionalParam - these should not occur in subqueries
            else if (cmp.right() instanceof JpqlAst.NamedParam || cmp.right() instanceof JpqlAst.PositionalParam) {
                throw new UnsupportedOperationException("Parameters not supported in subquery conditions");
            }
            // Handle Subquery - should not occur in subquery WHERE
            else if (cmp.right() instanceof JpqlAst.Subquery) {
                throw new UnsupportedOperationException("Subquery in subquery WHERE not supported");
            }
            // Handle other expressions
            else {
                rightSql = renderScalarExpr(cmp.right(), context);
            }
            
            String op = cmp.operator();
            return new io.vidocq.mansart.data.dialect.Where.RawSql(leftSql + " " + op + " " + rightSql);
        }
        
        if (cond instanceof JpqlAst.And and) {
            List<String> parts = new ArrayList<>();
            for (JpqlAst.Condition c : and.conditions()) {
                parts.add(((io.vidocq.mansart.data.dialect.Where.RawSql) translateSubqueryCondition(c, context)).sql());
            }
            return new io.vidocq.mansart.data.dialect.Where.RawSql(String.join(" AND ", parts));
        }
        
        if (cond instanceof JpqlAst.Or or) {
            List<String> parts = new ArrayList<>();
            for (JpqlAst.Condition c : or.conditions()) {
                parts.add(((io.vidocq.mansart.data.dialect.Where.RawSql) translateSubqueryCondition(c, context)).sql());
            }
            return new io.vidocq.mansart.data.dialect.Where.RawSql(String.join(" OR ", parts));
        }
        
        if (cond instanceof JpqlAst.Not not) {
            String innerSql = ((io.vidocq.mansart.data.dialect.Where.RawSql) translateSubqueryCondition(not.condition(), context)).sql();
            return new io.vidocq.mansart.data.dialect.Where.RawSql("NOT (" + innerSql + ")");
        }
        
        if (cond instanceof JpqlAst.IsNull isNull) {
            String exprSql = renderScalarExpr(isNull.expression(), context);
            String notSql = isNull.negated() ? "NOT " : "";
            return new io.vidocq.mansart.data.dialect.Where.RawSql(notSql + exprSql + " IS NULL");
        }
        
        if (cond instanceof JpqlAst.Between between) {
            String exprSql = renderScalarExpr(between.expression(), context);
            String lowSql = renderScalarExpr(between.low(), context);
            String highSql = renderScalarExpr(between.high(), context);
            String notSql = between.negated() ? "NOT " : "";
            return new io.vidocq.mansart.data.dialect.Where.RawSql(notSql + exprSql + " BETWEEN " + lowSql + " AND " + highSql);
        }
        
        if (cond instanceof JpqlAst.In in) {
            String exprSql = renderScalarExpr(in.expression(), context);
            String notSql = in.negated() ? "NOT " : "";
            
            if (in.items().size() == 1 && in.items().getFirst() instanceof JpqlAst.Subquery) {
                // IN with subquery - should not occur in subquery WHERE
                throw new UnsupportedOperationException("Subquery in IN clause not supported in subquery WHERE");
            }
            
            // Build list of inlined values
            List<String> valueList = new ArrayList<>();
            for (JpqlAst.Expression item : in.items()) {
                if (item instanceof JpqlAst.Literal lit) {
                    switch (lit.type()) {
                        case STRING -> valueList.add("'" + lit.value() + "'");
                        case INTEGER -> valueList.add(lit.value());
                        case FLOAT -> valueList.add(lit.value());
                        case BOOLEAN -> valueList.add(lit.value().toUpperCase());
                        case NULL -> valueList.add("NULL");
                        default -> throw new UnsupportedOperationException("Literal type not supported in IN: " + lit.type());
                    }
                } else {
                    valueList.add(renderScalarExpr(item, context));
                }
            }
            
            return new io.vidocq.mansart.data.dialect.Where.RawSql(notSql + exprSql + " IN (" + String.join(", ", valueList) + ")");
        }
        
        if (cond instanceof JpqlAst.Like like) {
            String exprSql = renderScalarExpr(like.expression(), context);
            String patternSql = renderScalarExpr(like.pattern(), context);
            String notSql = like.negated() ? "NOT " : "";
            String escapeSql = like.escape() != null ? " ESCAPE '" + like.escape() + "'" : "";
            return new io.vidocq.mansart.data.dialect.Where.RawSql(notSql + exprSql + " LIKE " + patternSql + escapeSql);
        }
        
        if (cond instanceof JpqlAst.Exists exists) {
            if (exists.subquery() instanceof JpqlAst.Subquery sub) {
                SqlAndParams subResult = translateSubquery(sub, context);
                String notSql = exists.negated() ? "NOT " : "";
                return new io.vidocq.mansart.data.dialect.Where.RawSql(notSql + "EXISTS " + subResult.sql());
            }
            throw new UnsupportedOperationException("EXISTS requires a subquery");
        }
        
        if (cond instanceof JpqlAst.AllAny allAny) {
            if (allAny.subquery() instanceof JpqlAst.Subquery sub) {
                String leftSql = renderScalarExpr(allAny.expression(), context);
                String opSql = " " + allAny.operator() + " " + allAny.quantifier() + " ";
                SqlAndParams subResult = translateSubquery(sub, context);
                return new io.vidocq.mansart.data.dialect.Where.RawSql(leftSql + opSql + subResult.sql());
            }
            throw new UnsupportedOperationException("ALL/ANY/SOME requires a subquery");
        }
        
        if (cond instanceof JpqlAst.IsEmpty isEmpty) {
            String exprSql = renderScalarExpr(isEmpty.expression(), context);
            String notSql = isEmpty.negated() ? "NOT " : "";
            return new io.vidocq.mansart.data.dialect.Where.RawSql(notSql + exprSql + " IS EMPTY");
        }
        
        if (cond instanceof JpqlAst.Member member) {
            String exprSql = renderScalarExpr(member.expression(), context);
            String notSql = member.negated() ? "NOT " : "";
            return new io.vidocq.mansart.data.dialect.Where.RawSql(notSql + exprSql + " MEMBER OF " + member.collection());
        }
        
        throw new UnsupportedOperationException("Cannot translate subquery condition: " + cond.getClass().getSimpleName());
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
                // For correlated subqueries, if the alias is from the outer context, we should not traverse
                // the relationship, but instead resolve the FK column on the current table.
                // This is because in JPQL, e.department.id means "the id of e's department", which in SQL
                // is the FK column on the employees table (department_id), not the id column on the departments table.
                // Only traverse if the currentModel is the referenced entity (i.e., we are in the subquery's own context).
                Class<?> targetEntityClass = refAttr.javaType();
                io.vidocq.mansart.persistence.spi.EntityModel targetSpiModel = callback.getEntityModel(targetEntityClass);
                EntityModel targetDialectModel = adapter.adapt(targetSpiModel);
                
                // If the currentModel is NOT the targetDialectModel, then we are in a correlated subquery
                // and should not traverse the relationship. Instead, we should return the FK column attribute
                // from the currentModel.
                if (!currentModel.equals(targetDialectModel)) {
                    // Return the FK column attribute from the currentModel
                    for (Object attrObj : currentModel.attributes()) {
                        @SuppressWarnings("unchecked")
                        Attribute<?, ?> a = (Attribute<?, ?>) attrObj;
                        if (a.name().equalsIgnoreCase(fieldName)) {
                            return a;
                        }
                    }
                    throw new IllegalArgumentException("Could not resolve attribute: " + fields + " in entity " + currentModel.tableName());
                }
                
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
        extractBindParameters(cond, params, null, context);
        return params;
    }

    private void extractBindParameters(JpqlAst.Condition cond, List<BindParameter> params, Attribute<?, ?> leftAttr, PlanContext outerContext) {
        extractBindParameters(cond, params, leftAttr, outerContext, outerContext);
    }

    private void extractBindParameters(JpqlAst.Condition cond, List<BindParameter> params, Attribute<?, ?> leftAttr, PlanContext outerContext, PlanContext subqueryContext) {
        if (cond == null) {
            return;
        }
        if (cond instanceof JpqlAst.Comparison cmp) {
            // Handle non-path expressions on left side (functions, literals, etc.)
            if (!(cmp.left() instanceof JpqlAst.Path)) {
                // Non-path comparison (e.g., function result) - parameters handled by renderScalarExpr
                // If right side is a Path, it's a correlated comparison, not a bind parameter
                if (cmp.right() instanceof JpqlAst.Path) {
                    return;
                }
                // If right side is a subquery, recurse into it to extract WHERE parameters
                if (cmp.right() instanceof JpqlAst.Subquery rightSubquery) {
                    JpqlAst.SelectStatement subStmt = (JpqlAst.SelectStatement) rightSubquery.statement();
                    if (subStmt.where() != null) {
                        // Use a fresh mutable list for the subquery's bind parameters
                        List<BindParameter> subqueryParams = new ArrayList<>();
                        PlanContext subCtx = buildSubqueryContext(subStmt, outerContext);
                        extractBindParameters(subStmt.where().condition(), subqueryParams, null, subCtx, subCtx);
                        // Do not add subqueryParams to the outer params list; they are already inlined in the subquery SQL
                    }
                } else {
                    extractBindParameterFromExpr(cmp.right(), null, params);
                }
                return;
            }
            // For path comparisons, extract bind parameters from the right side
            // The left side is already resolved as leftAttr
            if (cmp.right() instanceof JpqlAst.Subquery rightSubquery) {
                JpqlAst.SelectStatement subStmt = (JpqlAst.SelectStatement) rightSubquery.statement();
                if (subStmt.where() != null) {
                    // Use a fresh mutable list for the subquery's bind parameters
                    List<BindParameter> subqueryParams = new ArrayList<>();
                    PlanContext subCtx = buildSubqueryContext(subStmt, outerContext);
                    extractBindParameters(subStmt.where().condition(), subqueryParams, null, subCtx, subCtx);
                    // Do not add subqueryParams to the outer params list; they are already inlined in the subquery SQL
                }
            } else if (!(cmp.right() instanceof JpqlAst.Path)) {
                // Extract bind parameter from non-path right side
                extractBindParameterFromExpr(cmp.right(), leftAttr != null ? leftAttr : resolveAttribute(cmp.left(), outerContext), params);
            }
            return;
        }
        if (cond instanceof JpqlAst.And and) {
            and.conditions().forEach(c -> extractBindParameters(c, params, leftAttr, outerContext, subqueryContext));
            return;
        }
        if (cond instanceof JpqlAst.Or or) {
            or.conditions().forEach(c -> extractBindParameters(c, params, leftAttr, outerContext, subqueryContext));
            return;
        }
        if (cond instanceof JpqlAst.Not not) {
            extractBindParameters(not.condition(), params, leftAttr, outerContext, subqueryContext);
            return;
        }
        if (cond instanceof JpqlAst.IsNull) {
            return; // no bind parameters
        }
        if (cond instanceof JpqlAst.Between between) {
            Attribute<?, ?> attr = leftAttr != null ? leftAttr : resolveAttribute(between.expression(), outerContext);
            // low comes before high, matching dialect rendering order
            extractBindParameterFromExpr(between.low(), attr, params);
            extractBindParameterFromExpr(between.high(), attr, params);
            return;
        }
        if (cond instanceof JpqlAst.In in) {
            // Handle IN with subquery
            if (in.items().size() == 1 && in.items().getFirst() instanceof JpqlAst.Subquery sub) {
                JpqlAst.SelectStatement subStmt = (JpqlAst.SelectStatement) sub.statement();
                if (subStmt.where() != null) {
                    // Use a fresh mutable list for the subquery's bind parameters
                    List<BindParameter> subqueryParams = new ArrayList<>();
                    PlanContext subCtx = buildSubqueryContext(subStmt, outerContext);
                    extractBindParameters(subStmt.where().condition(), subqueryParams, null, subCtx, subCtx);
                    // Do not add subqueryParams to the outer params list; they are already inlined in the subquery SQL
                }
                return;
            }
            // Handle regular IN with values
            Attribute<?, ?> attr = leftAttr != null ? leftAttr : resolveAttribute(in.expression(), outerContext);
            for (JpqlAst.Expression item : in.items()) {
                extractBindParameterFromExpr(item, attr, params);
            }
            return;
        }
        if (cond instanceof JpqlAst.Like like) {
            Attribute<?, ?> attr = leftAttr != null ? leftAttr : resolveAttribute(like.expression(), outerContext);
            extractBindParameterFromExpr(like.pattern(), attr, params);
            return;
        }
        if (cond instanceof JpqlAst.Exists exists) {
            if (exists.subquery() instanceof JpqlAst.Subquery sub) {
                JpqlAst.SelectStatement subStmt = (JpqlAst.SelectStatement) sub.statement();
                if (subStmt.where() != null) {
                    // Use a fresh mutable list for the subquery's bind parameters
                    List<BindParameter> subqueryParams = new ArrayList<>();
                    PlanContext subCtx = buildSubqueryContext(subStmt, outerContext);
                    extractBindParameters(subStmt.where().condition(), subqueryParams, null, subCtx, subCtx);
                    // Do not add subqueryParams to the outer params list; they are already inlined in the subquery SQL
                }
            }
            return;
        }
        if (cond instanceof JpqlAst.AllAny allAny) {
            if (allAny.subquery() instanceof JpqlAst.Subquery sub) {
                JpqlAst.SelectStatement subStmt = (JpqlAst.SelectStatement) sub.statement();
                if (subStmt.where() != null) {
                    // Use a fresh mutable list for the subquery's bind parameters to avoid infinite recursion
                    List<BindParameter> subqueryParams = new ArrayList<>();
                    PlanContext subCtx = buildSubqueryContext(subStmt, outerContext);
                    extractBindParameters(subStmt.where().condition(), subqueryParams, null, subCtx, subCtx);
                    // Do not add subqueryParams to the outer params list; they are already inlined in the subquery SQL
                }
            }
            return;
        }
        if (cond instanceof JpqlAst.Member) {
            return;
        }
        if (cond instanceof JpqlAst.IsEmpty) {
            // no bind parameters
            return;
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
     * Returns true if any select item is an Aggregate, Func, or scalar expression (CASE, COALESCE, NULLIF).
     */
    private boolean isAggregateQuery(List<JpqlAst.Expression> selectItems) {
        return selectItems.stream()
            .anyMatch(expr -> expr instanceof JpqlAst.Aggregate
                || expr instanceof JpqlAst.Func
                || expr instanceof JpqlAst.CaseExpr
                || expr instanceof JpqlAst.SearchedCase
                || expr instanceof JpqlAst.Coalesce
                || expr instanceof JpqlAst.Nullif);
    }

    /**
     * Builds ProjectionInfo for each aggregate, function, or CASE expression in the SELECT clause.
     * Returns a list of projection info objects that describe how to materialize
     * the result set row.
     */
    private List<ProjectionInfo> buildProjections(List<JpqlAst.Expression> selectItems, PlanContext context) {
        List<ProjectionInfo> projections = new ArrayList<>();
        for (JpqlAst.Expression expr : selectItems) {
            if (expr instanceof JpqlAst.Aggregate agg) {
                projections.add(buildProjectionInfo(agg, context));
            } else if (expr instanceof JpqlAst.Func func) {
                if (Set.of("CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "LOCAL_DATE", "LOCAL_TIME", "LOCAL_DATETIME", "EXTRACT").contains(func.name().toUpperCase())) {
                    projections.add(buildFunctionProjection(func, context));
                } else {
                    projections.add(buildStringFunctionProjection(func, context));
                }
            } else if (expr instanceof JpqlAst.CaseExpr caseExpr) {
                projections.add(buildCaseProjection(caseExpr, context));
            } else if (expr instanceof JpqlAst.SearchedCase searchedCase) {
                projections.add(buildCaseProjection(searchedCase, context));
            } else if (expr instanceof JpqlAst.Coalesce coalesce) {
                projections.add(buildCaseProjection(coalesce, context));
            } else if (expr instanceof JpqlAst.Nullif nullif) {
                projections.add(buildCaseProjection(nullif, context));
            } else {
                throw new UnsupportedOperationException("Only aggregate, function, and CASE expressions are supported in scalar queries, got: " + expr.getClass().getSimpleName());
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

    /**
     * Builds a single ProjectionInfo for a string function expression.
     */
    private ProjectionInfo buildStringFunctionProjection(JpqlAst.Func func, PlanContext context) {
        String function = func.name().toUpperCase();
        List<JpqlAst.Expression> args = func.args();

        // Render each argument to SQL
        List<String> renderedArgs = new ArrayList<>();
        for (JpqlAst.Expression arg : args) {
            renderedArgs.add(renderFuncArg(arg, context));
        }

        // Determine the SQL expression and result type
        String columnSql;
        Class<?> resultType;

        switch (function) {
            case "CONCAT" -> {
                columnSql = "CONCAT(" + String.join(", ", renderedArgs) + ")";
                resultType = String.class;
            }
            case "SUBSTRING" -> {
                if (args.size() == 2) {
                    columnSql = "SUBSTRING(" + renderedArgs.get(0) + ", " + renderedArgs.get(1) + ")";
                } else if (args.size() == 3) {
                    columnSql = "SUBSTRING(" + renderedArgs.get(0) + ", " + renderedArgs.get(1) + ", " + renderedArgs.get(2) + ")";
                } else {
                    throw new UnsupportedOperationException("SUBSTRING requires 2 or 3 arguments, got: " + args.size());
                }
                resultType = String.class;
            }
            case "TRIM" -> {
                if (args.size() != 1) {
                    throw new UnsupportedOperationException("TRIM with spec/char FROM form not implemented");
                }
                columnSql = "TRIM(" + renderedArgs.get(0) + ")";
                resultType = String.class;
            }
            case "LOWER" -> {
                columnSql = "LOWER(" + renderedArgs.get(0) + ")";
                resultType = String.class;
            }
            case "UPPER" -> {
                columnSql = "UPPER(" + renderedArgs.get(0) + ")";
                resultType = String.class;
            }
            case "LENGTH" -> {
                columnSql = "LENGTH(" + renderedArgs.get(0) + ")";
                resultType = Integer.class;
            }
            case "LOCATE" -> {
                if (args.size() == 2) {
                    columnSql = "LOCATE(" + renderedArgs.get(0) + ", " + renderedArgs.get(1) + ")";
                } else if (args.size() == 3) {
                    columnSql = "LOCATE(" + renderedArgs.get(0) + ", " + renderedArgs.get(1) + ", " + renderedArgs.get(2) + ")";
                } else {
                    throw new UnsupportedOperationException("LOCATE requires 2 or 3 arguments, got: " + args.size());
                }
                resultType = Integer.class;
            }
            case "INDEX" -> {
                throw new UnsupportedOperationException("not implemented: INDEX");
            }
            default -> {
                throw new UnsupportedOperationException("not implemented: string function " + function);
            }
        }

        return new ProjectionInfo(function, false, columnSql, resultType);
    }

    /**
     * Builds a single ProjectionInfo for a function expression (datetime functions).
     */
    private ProjectionInfo buildFunctionProjection(JpqlAst.Func func, PlanContext context) {
        String function = func.name().toUpperCase();
        List<JpqlAst.Expression> args = func.args();

        // Render each argument to SQL
        List<String> renderedArgs = new ArrayList<>();
        for (JpqlAst.Expression arg : args) {
            renderedArgs.add(renderFuncArg(arg, context));
        }

        // Determine the SQL expression and result type
        String columnSql;
        Class<?> resultType;

        switch (function) {
            case "CURRENT_DATE" -> {
                columnSql = "CURRENT_DATE";
                resultType = java.sql.Date.class;
            }
            case "CURRENT_TIME" -> {
                columnSql = "CURRENT_TIME";
                resultType = java.sql.Time.class;
            }
            case "CURRENT_TIMESTAMP" -> {
                columnSql = "CURRENT_TIMESTAMP";
                resultType = java.sql.Timestamp.class;
            }
            case "LOCAL_DATE" -> {
                columnSql = "CURRENT_DATE";
                resultType = java.time.LocalDate.class;
            }
            case "LOCAL_TIME" -> {
                columnSql = "CURRENT_TIME";
                resultType = java.time.LocalTime.class;
            }
            case "LOCAL_DATETIME" -> {
                columnSql = "CURRENT_TIMESTAMP";
                resultType = java.time.LocalDateTime.class;
            }
            case "EXTRACT" -> {
                if (args.size() != 2) {
                    throw new UnsupportedOperationException("EXTRACT requires exactly 2 arguments (field and source), got: " + args.size());
                }
                String field = ((JpqlAst.Literal) args.get(0)).value().toUpperCase();
                // Validate field against known set
                if (!Set.of("YEAR", "MONTH", "DAY", "HOUR", "MINUTE", "SECOND").contains(field)) {
                    throw new UnsupportedOperationException("not implemented: EXTRACT field " + field);
                }
                String sourceSql = renderedArgs.get(1);
                columnSql = "EXTRACT(" + field + " FROM " + sourceSql + ")";
                resultType = Integer.class;
            }
            default -> {
                throw new UnsupportedOperationException("not implemented: function " + function);
            }
        }

        return new ProjectionInfo(function, false, columnSql, resultType);
    }

    /**
     * Builds a single ProjectionInfo for a CASE expression.
     */
    private ProjectionInfo buildCaseProjection(JpqlAst.CaseExpr caseExpr, PlanContext context) {
        String columnSql = renderCaseSql(caseExpr, context);
        return new ProjectionInfo("CASE", false, columnSql, Object.class);
    }

    /**
     * Builds a single ProjectionInfo for a searched CASE expression.
     */
    private ProjectionInfo buildCaseProjection(JpqlAst.SearchedCase searchedCase, PlanContext context) {
        String columnSql = renderCaseSql(searchedCase, context);
        return new ProjectionInfo("CASE", false, columnSql, Object.class);
    }

    /**
     * Builds a single ProjectionInfo for a COALESCE expression.
     */
    private ProjectionInfo buildCaseProjection(JpqlAst.Coalesce coalesce, PlanContext context) {
        String columnSql = renderCaseSql(coalesce, context);
        return new ProjectionInfo("COALESCE", false, columnSql, Object.class);
    }

    /**
     * Builds a single ProjectionInfo for a NULLIF expression.
     */
    private ProjectionInfo buildCaseProjection(JpqlAst.Nullif nullif, PlanContext context) {
        String columnSql = renderCaseSql(nullif, context);
        return new ProjectionInfo("NULLIF", false, columnSql, Object.class);
    }

    /**
     * Renders a CASE-like expression to its SQL string representation.
     */
    private String renderCaseSql(JpqlAst.Expression expr, PlanContext context) {
        if (expr instanceof JpqlAst.CaseExpr caseExpr) {
            // Simple CASE: CASE operand WHEN scalar THEN expr ... ELSE expr END
            StringBuilder sql = new StringBuilder("CASE ");
            sql.append(renderScalarExpr(caseExpr.operand(), context)).append(" ");
            for (JpqlAst.CaseExpr.WhenThen whenThen : caseExpr.whens()) {
                sql.append("WHEN ").append(renderScalarExpr(whenThen.when(), context))
                   .append(" THEN ").append(renderScalarExpr(whenThen.then(), context))
                   .append(" ");
            }
            sql.append("ELSE ").append(renderScalarExpr(caseExpr.elseExpr(), context)).append(" END");
            return sql.toString();
        } else if (expr instanceof JpqlAst.SearchedCase searchedCase) {
            // Searched CASE: CASE WHEN condition THEN expr ... ELSE expr END
            StringBuilder sql = new StringBuilder("CASE ");
            for (JpqlAst.SearchedCase.SearchedWhenThen whenThen : searchedCase.whens()) {
                sql.append("WHEN ").append(translateCondition(whenThen.when(), context))
                   .append(" THEN ").append(renderScalarExpr(whenThen.then(), context))
                   .append(" ");
            }
            sql.append("ELSE ").append(renderScalarExpr(searchedCase.elseExpr(), context)).append(" END");
            return sql.toString();
        } else if (expr instanceof JpqlAst.Coalesce coalesce) {
            // COALESCE(arg1, arg2, ...)
            StringBuilder sql = new StringBuilder("COALESCE(");
            for (int i = 0; i < coalesce.args().size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append(renderScalarExpr(coalesce.args().get(i), context));
            }
            sql.append(")");
            return sql.toString();
        } else if (expr instanceof JpqlAst.Nullif nullif) {
            // NULLIF(first, second)
            return "NULLIF(" + renderScalarExpr(nullif.first(), context) + ", " + renderScalarExpr(nullif.second(), context) + ")";
        } else {
            throw new UnsupportedOperationException("Unsupported CASE-like expression type: " + expr.getClass().getSimpleName());
        }
    }

    /**
     * Renders a scalar expression to its SQL string representation.
     */
    private String renderScalarExpr(JpqlAst.Expression expr, PlanContext context) {
        if (expr instanceof JpqlAst.Path path) {
            // Path expression: resolve to column name
            Attribute<?, ?> attr = resolveAttribute(path, context);
            String columnName = attr.columnName();
            return "\"" + columnName + "\"";
        } else if (expr instanceof JpqlAst.Literal lit) {
            // Literal value
            switch (lit.type()) {
                case STRING -> {
                    // Escape single quotes by doubling them
                    String escaped = lit.value().replace("'", "''");
                    return "'" + escaped + "'";
                }
                case INTEGER -> {
                    return lit.value();
                }
                case FLOAT -> {
                    return lit.value();
                }
                case BOOLEAN -> {
                    return Boolean.parseBoolean(lit.value()) ? "TRUE" : "FALSE";
                }
                case NULL -> {
                    return "NULL";
                }
                default -> throw new UnsupportedOperationException("Literal type not supported: " + lit.type());
            }
        } else if (expr instanceof JpqlAst.NamedParam) {
            throw new UnsupportedOperationException("not implemented: parameters in CASE expressions");
        } else if (expr instanceof JpqlAst.PositionalParam) {
            throw new UnsupportedOperationException("not implemented: parameters in CASE expressions");
        } else if (expr instanceof JpqlAst.Subquery subquery) {
            // Scalar subquery: translate to SQL and wrap in parentheses
            return translateSubquery(subquery, context).sql();
        } else if (expr instanceof JpqlAst.Binary bin) {
            // Binary expression: render both sides with operator
            return renderScalarExpr(bin.left(), context) + " " + bin.operator() + " " + renderScalarExpr(bin.right(), context);
        } else if (expr instanceof JpqlAst.Unary unary) {
            // Unary expression: render operator and operand
            return unary.operator() + " " + renderScalarExpr(unary.operand(), context);
        } else if (expr instanceof JpqlAst.Func func) {
            // Function call: render as FUNC(args)
            StringBuilder sql = new StringBuilder(func.name().toUpperCase()).append("(");
            for (int i = 0; i < func.args().size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append(renderScalarExpr(func.args().get(i), context));
            }
            sql.append(")");
            return sql.toString();
        } else if (expr instanceof JpqlAst.Aggregate agg) {
            // Aggregate: render as FUNC(DISTINCT arg) or FUNC(arg)
            StringBuilder sql = new StringBuilder(agg.function().toUpperCase()).append("(");
            if (agg.distinct()) {
                sql.append("DISTINCT ");
            }
            sql.append(renderScalarExpr(agg.argument(), context));
            sql.append(")");
            return sql.toString();
        } else if (expr instanceof JpqlAst.CaseExpr || expr instanceof JpqlAst.SearchedCase || 
                   expr instanceof JpqlAst.Coalesce || expr instanceof JpqlAst.Nullif) {
            // CASE expressions: delegate to renderCaseSql
            return renderCaseSql(expr, context);
        } else {
            throw new UnsupportedOperationException("Unsupported scalar expression type in CASE: " + expr.getClass().getSimpleName());
        }
    }

    /**
     * Renders a function argument to its SQL string representation.
     */
    private String renderFuncArg(JpqlAst.Expression arg, PlanContext context) {
        if (arg instanceof JpqlAst.Path path) {
            // Path expression: resolve to column name
            Attribute<?, ?> attr = resolveAttribute(path, context);
            String columnName = attr.columnName();
            return "\"" + columnName + "\"";
        } else if (arg instanceof JpqlAst.Literal lit) {
            // Literal value
            switch (lit.type()) {
                case STRING -> {
                    // Escape single quotes by doubling them
                    String escaped = lit.value().replace("'", "''");
                    return "'" + escaped + "'";
                }
                case INTEGER -> {
                    return lit.value();
                }
                case FLOAT -> {
                    return lit.value();
                }
                case BOOLEAN -> {
                    return Boolean.parseBoolean(lit.value()) ? "TRUE" : "FALSE";
                }
                case NULL -> {
                    return "NULL";
                }
                default -> throw new UnsupportedOperationException("Literal type not supported in function arguments: " + lit.type());
            }
        } else if (arg instanceof JpqlAst.NamedParam) {
            throw new UnsupportedOperationException("not implemented: parameters as function arguments");
        } else if (arg instanceof JpqlAst.PositionalParam) {
            throw new UnsupportedOperationException("not implemented: parameters as function arguments");
        } else {
            throw new UnsupportedOperationException("Unsupported expression type in function arguments: " + arg.getClass().getSimpleName());
        }
    }

}
