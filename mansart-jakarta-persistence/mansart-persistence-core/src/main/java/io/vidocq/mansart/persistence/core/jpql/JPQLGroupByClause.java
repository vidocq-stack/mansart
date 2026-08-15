/*
 * Copyright (c) 2026 Vidocq contributors
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.jpql;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents the GROUP BY clause in a JPQL query.
 *
 * <p>Milestone: M8-1 — GROUP BY support.
 */
public final class JPQLGroupByClause {

    private final List<JPQLExpression> expressions;

    /**
     * Creates a GROUP BY clause with the given expressions.
     *
     * @param expressions the expressions to group by
     */
    public JPQLGroupByClause(List<JPQLExpression> expressions) {
        this.expressions = Collections.unmodifiableList(
                Objects.requireNonNull(expressions, "expressions must not be null"));
    }

    /**
     * Returns the expressions in the GROUP BY clause.
     *
     * @return unmodifiable list of expressions
     */
    public List<JPQLExpression> expressions() {
        return expressions;
    }

    @Override
    public String toString() {
        return "GROUP BY " + expressions;
    }
}
