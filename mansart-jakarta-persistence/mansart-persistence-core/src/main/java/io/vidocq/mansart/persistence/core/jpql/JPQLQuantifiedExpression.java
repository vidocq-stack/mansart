/*
 * Copyright (c) 2026 Vidocq contributors
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.jpql;

import java.util.Objects;

/**
 * Represents quantified expressions: ALL, ANY, SOME.
 *
 * <p>Milestone: M8-4 — ALL/ANY/SOME predicates support.
 * <p>These are used in expressions like: WHERE e.salary > ALL (SELECT ...)
 */
public final class JPQLQuantifiedExpression extends JPQLExpression {

    /** The type of quantified expression */
    public enum Quantifier {
        ALL,
        ANY,
        SOME
    }

    private final JPQLExpression leftExpression;
    private final JPQLComparator comparator;
    private final Quantifier quantifier;
    private final JPQLSubquery subquery;

    /**
     * Creates a new quantified expression.
     *
     * @param leftExpression the left side of the comparison
     * @param comparator the comparison operator
     * @param quantifier the quantifier (ALL, ANY, SOME)
     * @param subquery the subquery
     */
    public JPQLQuantifiedExpression(JPQLExpression leftExpression, JPQLComparator comparator,
                                     Quantifier quantifier, JPQLSubquery subquery) {
        this.leftExpression = Objects.requireNonNull(leftExpression, "leftExpression must not be null");
        this.comparator = Objects.requireNonNull(comparator, "comparator must not be null");
        this.quantifier = Objects.requireNonNull(quantifier, "quantifier must not be null");
        this.subquery = Objects.requireNonNull(subquery, "subquery must not be null");
    }

    public JPQLExpression leftExpression() { return leftExpression; }
    public JPQLComparator comparator() { return comparator; }
    public Quantifier quantifier() { return quantifier; }
    public JPQLSubquery subquery() { return subquery; }

    @Override
    public String toString() {
        return leftExpression + " " + comparator + " " + quantifier + " " + subquery;
    }
}
