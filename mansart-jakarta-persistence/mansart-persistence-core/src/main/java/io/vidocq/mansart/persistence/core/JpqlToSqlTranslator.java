/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under
 * the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import java.util.List;

import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.data.dialect.OrderBy;
import io.vidocq.mansart.data.dialect.Pagination;
import io.vidocq.mansart.data.dialect.SqlFragment;
import io.vidocq.mansart.data.dialect.Where;

/**
 * Translates a parsed {@link JpqlQuery} into a Dialect SELECT call.
 *
 * <p>Resolves the entity name to an {@link EntityModel}, translates
 * WHERE predicates into {@link Where} clauses, and translates
 * ORDER BY items into {@link OrderBy} clauses.
 *
 * <p>The H2 dialect embeds {@code ?} placeholders directly in the SQL
 * string for WHERE comparisons; the caller must bind the extracted
 * literal values to those placeholders.</p>
 */
final class JpqlToSqlTranslator {

    private final JpqlQuery query;
    private final EntityModel<?> entityModel;
    private final Dialect dialect;

    /**
     * Creates a translator for the given parsed query and entity model.
     *
     * @param query the parsed JPQL query
     * @param entityModel the entity model for the queried entity
     * @param dialect the SQL dialect
     */
    JpqlToSqlTranslator(JpqlQuery query, EntityModel<?> entityModel, Dialect dialect) {
        this.query = query;
        this.entityModel = entityModel;
        this.dialect = dialect;
    }

    /**
     * Returns the list of literal values extracted from WHERE predicates,
     * in the same order as {@code ?} placeholders appear in the SQL.
     *
     * @return list of literal values to bind
     */
    List<String> getLiteralValues() {
        List<JpqlPredicate> predicates = query.predicates();
        List<String> values = new java.util.ArrayList<>(predicates.size());
        for (JpqlPredicate p : predicates) {
            String v = p.value();
            // Skip positional parameters (?N) — they are bound via setParameter
            if (v.startsWith("?")) {
                continue;
            }
            values.add(v);
        }
        return values;
    }

    /**
     * Executes the translated query and returns the SQL fragment.
     *
     * @param pagination the pagination settings (offset, limit)
     * @return the SQL fragment with bind sites
     */
    SqlFragment translate(Pagination pagination) {
        Where where = buildWhere();
        OrderBy orderBy = buildOrderBy();
        return dialect.select(entityModel, where, orderBy, pagination);
    }

    /**
     * Translates a set operation (UNION, INTERSECT, EXCEPT) into SQL.
     *
     * <p>Each sub-query is translated independently using a dedicated
     * translator, and the results are combined with the set operation
     * keyword.</p>
     *
     * @param pagination the pagination settings (offset, limit)
     * @param subEntityModels the entity models for each sub-query
     * @param paginationList the pagination settings for each sub-query
     * @return the combined SQL fragment with bind sites
     */
    SqlFragment translateSetOperation(Pagination pagination,
                                      List<io.vidocq.mansart.data.dialect.EntityModel<?>> subEntityModels,
                                      List<Pagination> paginationList) {
        StringBuilder sb = new StringBuilder();
        java.util.List<SqlFragment.BindSite> allBinds =
                new java.util.ArrayList<>();

        List<JpqlQuery> subQueries = query.subQueries();
        for (int i = 0; i < subQueries.size(); i++) {
            JpqlQuery sq = subQueries.get(i);
            EntityModel<?> em = subEntityModels.get(i);
            Pagination sp = (i < paginationList.size()) ? paginationList.get(i) : Pagination.NONE;

            // Create a dedicated translator for this sub-query
            JpqlToSqlTranslator subTranslator =
                    new JpqlToSqlTranslator(sq, em, dialect);

            // Build the sub-query SQL using the dialect's select method
            SqlFragment subFragment = subTranslator.translate(sp);

            if (i > 0) {
                String op = switch (query.setOp()) {
                    case UNION -> " UNION ";
                    case INTERSECT -> " INTERSECT ";
                    case EXCEPT -> " EXCEPT ";
                    default -> throw new IllegalStateException("Unknown set op: " + query.setOp());
                };
                sb.append(op);
            }
            sb.append(subFragment.sql());

            // Collect bind sites from all sub-queries
            allBinds.addAll(subFragment.binds());
        }

        return new SqlFragment(sb.toString(), allBinds);
    }

    private Where buildWhere() {
        List<JpqlPredicate> predicates = query.predicates();
        if (predicates.isEmpty()) {
            return Where.ALWAYS_TRUE;
        }

        Where result = buildSingleWhere(predicates.get(0));
        for (int i = 1; i < predicates.size(); i++) {
            result = Where.and(result, buildSingleWhere(predicates.get(i)));
        }
        return result;
    }

    private Where buildSingleWhere(JpqlPredicate predicate) {
        String func = predicate.function();
        List<String> args = predicate.arguments();

        // Multi-argument function (LOCATE, SUBSTRING, LEFT, RIGHT, CONCAT)
        // Check this FIRST, before trying to look up fieldName as an attribute
        // (for multi-arg functions, fieldName is the first argument, which may be a literal)
        if (!args.isEmpty() && !JpqlFunctionRegistry.isFuncFunction(func)) {
            // Special case: EXTRACT(field FROM expr) — the first argument is the
            // extraction field name (a string literal like "YEAR"), the rest are
            // column references. Emit a Where.Extract node.
            if ("EXTRACT".equals(func)) {
                return buildExtractFunc(args, predicate.op());
            }
            return buildMultiArgFunc(func, args, predicate.op());
        }

        // For unary functions and non-function predicates, look up fieldName as an attribute
        java.util.Optional<? extends io.vidocq.mansart.data.dialect.Attribute<?, ?>> attrOpt =
                entityModel.attribute(predicate.fieldName());
        if (attrOpt.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unknown field in WHERE: " + predicate.fieldName());
        }
        @SuppressWarnings("unchecked")
        io.vidocq.mansart.data.dialect.Attribute<?, ?> attr =
                (io.vidocq.mansart.data.dialect.Attribute<?, ?>) (Object) attrOpt.get();

        // If this predicate has a scalar function wrapper (UPPER, LOWER, etc.),
        // wrap the comparison in Where.Func
        if (func != null && JpqlFunctionRegistry.isFuncFunction(func)) {
            return new Where.Func(JpqlFunctionRegistry.sqlFunction(func), buildSingleWhereBase(attr, predicate.op()));
        }
        
        return buildSingleWhereBase(attr, predicate.op());
    }

    /**
     * Builds a Where node for an EXTRACT date/time extraction function.
     * The first argument is the extraction field name (e.g. "YEAR"),
     * the remaining arguments are column references.
     */
    private Where buildExtractFunc(List<String> args, String op) {
        // First arg is the extraction field name (string literal)
        String field = args.get(0);
        // Remaining args are column references
        if (args.size() < 2) {
            throw new IllegalArgumentException("EXTRACT requires at least 2 arguments: field and column");
        }
        java.util.Optional<? extends io.vidocq.mansart.data.dialect.Attribute<?, ?>> attrOpt =
                entityModel.attribute(args.get(1));
        if (attrOpt.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unknown column in EXTRACT: " + args.get(1));
        }
        @SuppressWarnings("unchecked")
        io.vidocq.mansart.data.dialect.Attribute<?, ?> attr =
                (io.vidocq.mansart.data.dialect.Attribute<?, ?>) (Object) attrOpt.get();
        return new Where.Extract(field, attr);
    }

    /**
     * Builds a Where node for a multi-argument function.
     * Each argument is either a column reference (resolved to an Attribute)
     * or a literal value (String).
     */
    private Where buildMultiArgFunc(String func, List<String> args, String op) {
        List<Object> whereArgs = new java.util.ArrayList<>(args.size());
        for (String arg : args) {
            // Always try to resolve as attribute first (parser strips alias prefix).
            // Fall back to literal only if attribute lookup fails.
            java.util.Optional<? extends io.vidocq.mansart.data.dialect.Attribute<?, ?>> attrOpt =
                    entityModel.attribute(arg);
            if (attrOpt.isPresent()) {
                @SuppressWarnings("unchecked")
                io.vidocq.mansart.data.dialect.Attribute<?, ?> attr =
                        (io.vidocq.mansart.data.dialect.Attribute<?, ?>) (Object) attrOpt.get();
                whereArgs.add(attr);
            } else {
                // Literal value: string (no quotes), number, or positional param
                whereArgs.add(arg);
            }
        }
        return new Where.MultiArgFunc(func, whereArgs, opToSql(op));
    }

    private Where buildSingleWhereBase(io.vidocq.mansart.data.dialect.Attribute<?, ?> attr, String op) {
        if ("=".equals(op)) {
            return Where.eq(attr);
        } else if ("<>".equals(op)) {
            return new Where.NotEq(attr);
        } else if (">=".equals(op)) {
            return new Where.Gte(attr);
        } else if ("<=".equals(op)) {
            return new Where.Lte(attr);
        } else if (">".equals(op)) {
            return new Where.Gt(attr);
        } else if ("<".equals(op)) {
            return new Where.Lt(attr);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported WHERE operator: " + op);
        }
    }

    /** Map a JPQL operator to the SQL comparison string with a positional parameter. */
    private static String opToSql(String op) {
        return switch (op) {
            case "="      -> " = ?";
            case "<>"     -> " <> ?";
            case ">="     -> " >= ?";
            case "<="     -> " <= ?";
            case ">"      -> " > ?";
            case "<"      -> " < ?";
            case "LIKE"   -> " LIKE ?";
            case "BETWEEN" -> " BETWEEN ? AND ?";
            case "IS NULL"    -> " IS NULL";
            case "IS NOT NULL" -> " IS NOT NULL";
            default -> throw new IllegalArgumentException("Unsupported operator: " + op);
        };
    }

    private OrderBy buildOrderBy() {
        List<JpqlOrderBy> orderBys = query.orderBys();
        if (orderBys.isEmpty()) {
            return OrderBy.NONE;
        }

        java.util.List<OrderBy.Order> orders = new java.util.ArrayList<>();
        for (JpqlOrderBy orderBy : orderBys) {
            java.util.Optional<? extends io.vidocq.mansart.data.dialect.Attribute<?, ?>> attrOpt =
                    entityModel.attribute(orderBy.fieldName());
            if (attrOpt.isEmpty()) {
                throw new IllegalArgumentException(
                        "Unknown field in ORDER BY: " + orderBy.fieldName());
            }
            @SuppressWarnings("unchecked")
            io.vidocq.mansart.data.dialect.Attribute<?, ?> attr =
                    (io.vidocq.mansart.data.dialect.Attribute<?, ?>) (Object) attrOpt.get();
            if ("DESC".equals(orderBy.direction())) {
                orders.add(OrderBy.Order.desc(attr));
            } else {
                orders.add(OrderBy.Order.asc(attr));
            }
        }
        return new OrderBy(orders);
    }
}
