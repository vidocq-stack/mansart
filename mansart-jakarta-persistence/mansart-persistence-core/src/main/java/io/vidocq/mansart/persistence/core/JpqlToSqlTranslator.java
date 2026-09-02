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
     * @return the SQL fragment with bind sites
     */
    SqlFragment translate() {
        Where where = buildWhere();
        OrderBy orderBy = buildOrderBy();
        return dialect.select(entityModel, where, orderBy, Pagination.NONE);
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
        java.util.Optional<? extends io.vidocq.mansart.data.dialect.Attribute<?, ?>> attrOpt =
                entityModel.attribute(predicate.fieldName());
        if (attrOpt.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unknown field in WHERE: " + predicate.fieldName());
        }
        @SuppressWarnings("unchecked")
        io.vidocq.mansart.data.dialect.Attribute<?, ?> attr =
                (io.vidocq.mansart.data.dialect.Attribute<?, ?>) (Object) attrOpt.get();

        String op = predicate.op();
        if ("=".equals(op)) {
            return Where.eq(attr);
        } else if ("<>".equals(op)) {
            return new Where.NotEq(attr);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported WHERE operator: " + op);
        }
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
