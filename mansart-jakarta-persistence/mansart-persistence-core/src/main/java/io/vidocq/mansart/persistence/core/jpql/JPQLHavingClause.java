/*
 * Copyright (c) 2026 Vidocq contributors
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.jpql;

import java.util.Objects;

/**
 * Represents the HAVING clause in a JPQL query.
 *
 * <p>Milestone: M8-1 — HAVING support.
 */
public final class JPQLHavingClause {

    private final JPQLExpression expression;

    /**
     * Creates a HAVING clause with the given expression.
     *
     * @param expression the HAVING expression
     */
    public JPQLHavingClause(JPQLExpression expression) {
        this.expression = Objects.requireNonNull(expression, "expression must not be null");
    }

    /**
     * Returns the expression in the HAVING clause.
     *
     * @return the HAVING expression
     */
    public JPQLExpression expression() {
        return expression;
    }

    @Override
    public String toString() {
        return "HAVING " + expression;
    }
}
