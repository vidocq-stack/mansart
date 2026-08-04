/*
 * Copyright (c) ${year} Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

package io.vidocq.mansart.data.query.ast;

/**
 * JPQL BETWEEN predicate node.
 *
 * <p>Represents a BETWEEN predicate.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * b.price BETWEEN 10 AND 100
 * b.publishedDate BETWEEN DATE '2020-01-01' AND DATE '2024-12-31'
 * b.title NOT BETWEEN 'A' AND 'M'
 * </pre>
 *
 * @param expression the expression to test
 * @param not true if this is a NOT BETWEEN predicate
 * @param lower the lower bound
 * @param upper the upper bound
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlBetweenPredicate(
    JpqlExpr expression,
    boolean not,
    JpqlExpr lower,
    JpqlExpr upper
) implements JpqlPredicate {

    /**
     * Creates a new BETWEEN predicate.
     *
     * @param expression the expression to test
     * @param not true if this is a NOT BETWEEN predicate
     * @param lower the lower bound
     * @param upper the upper bound
     */
    public JpqlBetweenPredicate {
        if (expression == null) {
            throw new IllegalArgumentException("expression cannot be null");
        }
        if (lower == null) {
            throw new IllegalArgumentException("lower bound cannot be null");
        }
        if (upper == null) {
            throw new IllegalArgumentException("upper bound cannot be null");
        }
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitBetweenPredicate(this, parameter);
    }

    /**
     * Returns true if this is a NOT BETWEEN predicate.
     *
     * @return true if NOT BETWEEN
     */
    public boolean isNotBetween() {
        return not();
    }

    /**
     * Returns true if this is a BETWEEN predicate (not NOT BETWEEN).
     *
     * @return true if BETWEEN
     */
    public boolean isBetween() {
        return !not();
    }

    /**
     * Creates a BETWEEN predicate.
     *
     * @param expression the expression to test
     * @param lower the lower bound
     * @param upper the upper bound
     * @return a new BETWEEN predicate
     */
    public static JpqlBetweenPredicate between(JpqlExpr expression, JpqlExpr lower, JpqlExpr upper) {
        return new JpqlBetweenPredicate(expression, false, lower, upper);
    }

    /**
     * Creates a NOT BETWEEN predicate.
     *
     * @param expression the expression to test
     * @param lower the lower bound
     * @param upper the upper bound
     * @return a new NOT BETWEEN predicate
     */
    public static JpqlBetweenPredicate notBetween(JpqlExpr expression, JpqlExpr lower, JpqlExpr upper) {
        return new JpqlBetweenPredicate(expression, true, lower, upper);
    }
}
