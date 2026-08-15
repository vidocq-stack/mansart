/*
 * Copyright (c) 2026 Vidocq contributors
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.jpql;

import java.util.Objects;

/**
 * Represents a subquery in JPQL.
 *
 * <p>Milestone: M8-3 — Subqueries in FROM support.
 */
public final class JPQLSubquery extends JPQLExpression {

    private final JPQLQuery<?> query;
    private final String correlationVariable;

    /**
     * Creates a new subquery.
     *
     * @param query the subquery
     * @param correlationVariable the correlation variable (may be null)
     */
    public JPQLSubquery(JPQLQuery<?> query, String correlationVariable) {
        this.query = Objects.requireNonNull(query, "query must not be null");
        this.correlationVariable = correlationVariable;
    }

    public JPQLQuery<?> query() { return query; }
    public String correlationVariable() { return correlationVariable; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        sb.append(query);
        if (correlationVariable != null) {
            sb.append(" AS ").append(correlationVariable);
        }
        sb.append(")");
        return sb.toString();
    }
}
