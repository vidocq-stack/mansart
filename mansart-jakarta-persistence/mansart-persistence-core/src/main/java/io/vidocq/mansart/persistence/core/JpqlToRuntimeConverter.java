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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import io.vidocq.mansart.data.dialect.*;
import io.vidocq.mansart.data.dialect.OrderBy.Order;
import io.vidocq.mansart.data.dialect.Where.*;
import io.vidocq.mansart.data.query.ast.*;

import java.util.*;

/**
 * Converts JPQL AST nodes to Mansart Data Runtime types.
 * 
 * M5 — Query execution support.
 */
public final class JpqlToRuntimeConverter {

    private JpqlToRuntimeConverter() {}

    public static QueryExecutionParams convert(JpqlSelectStmt stmt, MansartEntityManager entityManager,
                                               Map<String, Object> namedParameters,
                                               Map<Integer, Object> positionalParameters) {
        QueryExecutionContext context = new QueryExecutionContext(entityManager, stmt.fromClause());
        Where where = convertWhere(
            stmt.whereClause().map(JpqlWhereClause::predicate).orElse(null), context);
        GroupBy groupBy = convertGroupBy(
            stmt.groupByClause().map(JpqlGroupByClause::expressions).orElse(List.of()), context);
        Where having = convertHaving(
            stmt.havingClause().map(JpqlHavingClause::predicate).orElse(null), context);
        OrderBy orderBy = convertOrderBy(
            stmt.orderByClause().map(JpqlOrderByClause::items).orElse(List.of()), context);
        return new QueryExecutionParams(context.getRootEntityModel(), where, groupBy, having, orderBy, context,
                                        namedParameters, positionalParameters);
    }

    /**
     * Convenience method for conversion without parameters.
     */
    public static QueryExecutionParams convert(JpqlSelectStmt stmt, MansartEntityManager entityManager) {
        return convert(stmt, entityManager, Map.of(), Map.of());
    }

    public static Where convertWhere(JpqlPredicate predicate, QueryExecutionContext context) {
        if (predicate == null) return Where.ALWAYS_TRUE;
        
        return switch (predicate) {
            case io.vidocq.mansart.data.query.ast.JpqlAndPredicate and -> convertAndPredicate(and, context);
            case io.vidocq.mansart.data.query.ast.JpqlOrPredicate or -> convertOrPredicate(or, context);
            case io.vidocq.mansart.data.query.ast.JpqlNotPredicate not -> convertNotPredicate(not, context);
            case io.vidocq.mansart.data.query.ast.JpqlComparisonPredicate comp -> convertComparisonPredicate(comp, context);
            case io.vidocq.mansart.data.query.ast.JpqlExistsPredicate exists -> convertExistsPredicate(exists, context);
            case io.vidocq.mansart.data.query.ast.JpqlInPredicate in -> convertInPredicate(in, context);
            case io.vidocq.mansart.data.query.ast.JpqlLikePredicate like -> convertLikePredicate(like, context);
            case io.vidocq.mansart.data.query.ast.JpqlBetweenPredicate between -> convertBetweenPredicate(between, context);
            case io.vidocq.mansart.data.query.ast.JpqlAllAnySomePredicate allAnySome -> throw new UnsupportedOperationException("ALL/ANY/SOME not yet supported");
            default -> throw new IllegalArgumentException("Unknown predicate type: " + predicate.getClass().getSimpleName());
        };
    }

    private static Where convertAndPredicate(io.vidocq.mansart.data.query.ast.JpqlAndPredicate and, QueryExecutionContext context) {
        return Where.and(and.predicates().stream().map(p -> convertWhere(p, context)).toArray(Where[]::new));
    }

    private static Where convertOrPredicate(io.vidocq.mansart.data.query.ast.JpqlOrPredicate or, QueryExecutionContext context) {
        return Where.or(or.predicates().stream().map(p -> convertWhere(p, context)).toArray(Where[]::new));
    }

    private static Where convertNotPredicate(io.vidocq.mansart.data.query.ast.JpqlNotPredicate not, QueryExecutionContext context) {
        return new Where.Not(convertWhere(not.operand(), context));
    }

    private static Where convertComparisonPredicate(io.vidocq.mansart.data.query.ast.JpqlComparisonPredicate comp, QueryExecutionContext context) {
        // Check if left expression is a function
        if (comp.left() instanceof io.vidocq.mansart.data.query.ast.JpqlFunctionExpr funcExpr) {
            return convertFunctionComparison(funcExpr, comp.operator(), context);
        }
        
        Attribute<?, ?> attr = extractAttribute(comp.left(), context);
        return switch (comp.operator()) {
            case EQUAL -> new Eq(attr);
            case NOT_EQUAL -> new NotEq(attr);
            case LESS_THAN -> new Lt(attr);
            case LESS_THAN_OR_EQUAL -> new Lte(attr);
            case GREATER_THAN -> new Gt(attr);
            case GREATER_THAN_OR_EQUAL -> new Gte(attr);
            case IS_NULL -> new IsNull(attr);
            case IS_NOT_NULL -> new IsNotNull(attr);
        };
    }

    /**
     * Converts a function comparison predicate like "UPPER(b.name) = 'TEST'".
     */
    private static Where convertFunctionComparison(io.vidocq.mansart.data.query.ast.JpqlFunctionExpr funcExpr,
                                                   io.vidocq.mansart.data.query.ast.JpqlPredicate.ComparisonOperator operator,
                                                   QueryExecutionContext context) {
        Attribute<?, ?> attr = JpqlFunctionConverter.convertFunctionToAttribute(funcExpr, context);
        return switch (operator) {
            case EQUAL -> new Eq(attr);
            case NOT_EQUAL -> new NotEq(attr);
            case LESS_THAN -> new Lt(attr);
            case LESS_THAN_OR_EQUAL -> new Lte(attr);
            case GREATER_THAN -> new Gt(attr);
            case GREATER_THAN_OR_EQUAL -> new Gte(attr);
            case IS_NULL -> new IsNull(attr);
            case IS_NOT_NULL -> new IsNotNull(attr);
        };
    }

    private static Where convertExistsPredicate(io.vidocq.mansart.data.query.ast.JpqlExistsPredicate exists, QueryExecutionContext context) {
        // M6 — Convert EXISTS predicate
        // Get the dialect from the entity manager
        MansartEntityManager entityManager = context.getEntityManager();
        Dialect dialect = entityManager.getDialect();
        
        // Convert the subquery to QueryExecutionParams
        // Note: The subquery uses its own entity model and where clause
        QueryExecutionParams subqueryParams = convert(exists.subquery(), entityManager);
        
        // Render the subquery to SQL (without pagination for subqueries)
        SqlFragment subquerySql = dialect.select(
            subqueryParams.entityModel(),
            subqueryParams.where(),
            subqueryParams.groupBy(),
            subqueryParams.having(),
            subqueryParams.orderBy(),
            Pagination.NONE
        );
        
        // Create the EXISTS Where predicate
        return exists.not() 
            ? Where.notExists(subquerySql)
            : Where.exists(subquerySql);
    }

    private static Where convertInPredicate(io.vidocq.mansart.data.query.ast.JpqlInPredicate in, QueryExecutionContext context) {
        Attribute<?, ?> attr = extractAttribute(in.expression(), context);
        
        // Get the arity (number of values) from the IN items
        // JpqlInItems is package-private, so we use reflection to access it
        int arity = getInItemsArity(in);
        
        Where where = new Where.In(attr, arity);
        return in.not() ? new Where.Not(where) : where;
    }

    /**
     * Gets the arity (number of values) from IN predicate items using reflection.
     * JpqlInItems is package-private in mansart-data-query, so we need reflection.
     */
    private static int getInItemsArity(io.vidocq.mansart.data.query.ast.JpqlInPredicate in) {
        try {
            // Get the items field from JpqlInPredicate
            var itemsField = io.vidocq.mansart.data.query.ast.JpqlInPredicate.class.getMethod("items");
            Object items = itemsField.invoke(in);
            
            // Check if it's an ExpressionList or Subquery by class name
            String itemsClassName = items.getClass().getName();
            
            if (itemsClassName.contains("ExpressionList")) {
                // ExpressionList has expressions() method
                var expressionsMethod = items.getClass().getMethod("expressions");
                java.util.List<?> expressions = (java.util.List<?>) expressionsMethod.invoke(items);
                return expressions.size();
            } else if (itemsClassName.contains("Subquery")) {
                // For subqueries, we don't know the arity at this level
                return 1;
            }
        } catch (Exception e) {
            // Fallback to default arity
        }
        
        // Default arity if we can't determine
        return 1;
    }

    private static Where convertLikePredicate(io.vidocq.mansart.data.query.ast.JpqlLikePredicate like, QueryExecutionContext context) {
        Where where = new Like(extractAttribute(like.expression(), context));
        return like.not() ? new Where.Not(where) : where;
    }

    private static Where convertBetweenPredicate(io.vidocq.mansart.data.query.ast.JpqlBetweenPredicate between, QueryExecutionContext context) {
        Where where = Where.between(extractAttribute(between.expression(), context));
        return between.not() ? new Where.Not(where) : where;
    }

    private static OrderBy convertOrderBy(List<io.vidocq.mansart.data.query.ast.JpqlOrderByItem> items, QueryExecutionContext context) {
        if (items.isEmpty()) return OrderBy.NONE;
        return new OrderBy(items.stream().map(item -> {
            Attribute<?, ?> attr = extractAttribute(item.expression(), context);
            return new Order(attr, item.ascending() ? Order.Direction.ASC : Order.Direction.DESC);
        }).toList());
    }

    @SuppressWarnings("unchecked")
    private static GroupBy convertGroupBy(List<io.vidocq.mansart.data.query.ast.JpqlExpr> expressions, QueryExecutionContext context) {
        if (expressions.isEmpty()) return GroupBy.NONE;
        List<Attribute<?, ?>> attrs = new ArrayList<>();
        for (io.vidocq.mansart.data.query.ast.JpqlExpr expr : expressions) {
            attrs.add(extractAttribute(expr, context));
        }
        return new GroupBy(attrs);
    }

    private static Where convertHaving(io.vidocq.mansart.data.query.ast.JpqlPredicate predicate, QueryExecutionContext context) {
        // HAVING clause uses the same predicate types as WHERE
        // Just convert it the same way
        return predicate != null ? convertWhere(predicate, context) : Where.ALWAYS_TRUE;
    }

    private static Attribute<?, ?> extractAttribute(io.vidocq.mansart.data.query.ast.JpqlExpr expr, QueryExecutionContext context) {
        if (expr instanceof io.vidocq.mansart.data.query.ast.JpqlPathExpr path) return context.resolvePath(path);
        if (expr instanceof io.vidocq.mansart.data.query.ast.JpqlFunctionExpr func) {
            // For functions, try to extract the attribute from the first argument
            // if it's a simple function like UPPER(b.name)
            if (!func.arguments().isEmpty()) {
                return extractAttribute(func.arguments().get(0), context);
            }
            // For functions with no arguments like CURRENT_DATE, we can't extract an attribute
            throw new UnsupportedOperationException(
                "Cannot extract attribute from function: " + func.name());
        }
        throw new IllegalArgumentException("Cannot extract attribute from: " + expr.getClass().getSimpleName());
    }

    public record QueryExecutionParams(
        EntityModel<?> entityModel,
        Where where,
        GroupBy groupBy,
        Where having,
        OrderBy orderBy,
        QueryExecutionContext context,
        Map<String, Object> namedParameters,
        Map<Integer, Object> positionalParameters
    ) {
        @SuppressWarnings("unchecked")
        public <E> EntityModel<E> castEntityModel() { return (EntityModel<E>) entityModel; }
    }
}
