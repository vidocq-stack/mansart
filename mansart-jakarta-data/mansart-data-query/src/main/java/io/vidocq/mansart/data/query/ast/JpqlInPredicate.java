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

import java.util.List;

/**
 * JPQL IN predicate node.
 *
 * <p>Represents an IN or NOT IN predicate.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * b.category IN ('Fiction', 'Sci-Fi')
 * b.id NOT IN (1, 2, 3)
 * b.author IN (SELECT a FROM Author a WHERE a.active = true)
 * </pre>
 *
 * @param expression the expression to test
 * @param not true if this is a NOT IN predicate
 * @param items the list of values or subquery
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlInPredicate(
    JpqlExpr expression,
    boolean not,
    JpqlInItems items
) implements JpqlPredicate {

    /**
     * Creates a new IN predicate.
     *
     * @param expression the expression to test
     * @param not true if this is a NOT IN predicate
     * @param items the list of values or subquery
     */
    public JpqlInPredicate {
        if (expression == null) {
            throw new IllegalArgumentException("expression cannot be null");
        }
        if (items == null) {
            throw new IllegalArgumentException("items cannot be null");
        }
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitInPredicate(this, parameter);
    }

    /**
     * Returns true if this is a NOT IN predicate.
     *
     * @return true if NOT IN
     */
    public boolean isNotIn() {
        return not();
    }

    /**
     * Returns true if this is an IN predicate (not NOT IN).
     *
     * @return true if IN
     */
    public boolean isIn() {
        return !not();
    }

    /**
     * Creates an IN predicate with a list of expressions.
     *
     * @param expression the expression to test
     * @param expressions the list of expressions
     * @return a new IN predicate
     */
    public static JpqlInPredicate in(JpqlExpr expression, List<JpqlExpr> expressions) {
        return new JpqlInPredicate(expression, false, new JpqlInItems.ExpressionList(expressions));
    }

    /**
     * Creates a NOT IN predicate with a list of expressions.
     *
     * @param expression the expression to test
     * @param expressions the list of expressions
     * @return a new NOT IN predicate
     */
    public static JpqlInPredicate notIn(JpqlExpr expression, List<JpqlExpr> expressions) {
        return new JpqlInPredicate(expression, true, new JpqlInItems.ExpressionList(expressions));
    }

    /**
     * Creates an IN predicate with a subquery.
     *
     * @param expression the expression to test
     * @param subquery the subquery
     * @return a new IN predicate with subquery
     */
    public static JpqlInPredicate in(JpqlExpr expression, JpqlSelectStmt subquery) {
        return new JpqlInPredicate(expression, false, new JpqlInItems.Subquery(subquery));
    }

    /**
     * Creates a NOT IN predicate with a subquery.
     *
     * @param expression the expression to test
     * @param subquery the subquery
     * @return a new NOT IN predicate with subquery
     */
    public static JpqlInPredicate notIn(JpqlExpr expression, JpqlSelectStmt subquery) {
        return new JpqlInPredicate(expression, true, new JpqlInItems.Subquery(subquery));
    }
}

/**
 * Represents the items in an IN predicate (either a list of expressions or a subquery).
 */
sealed interface JpqlInItems {
    /**
     * A list of expressions.
     */
    record ExpressionList(List<JpqlExpr> expressions) implements JpqlInItems {
        public ExpressionList {
            expressions = List.copyOf(expressions);
        }
    }

    /**
     * A subquery.
     */
    record Subquery(JpqlSelectStmt query) implements JpqlInItems {
    }
}
