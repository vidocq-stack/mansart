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

package io.vidocq.mansart.data.query.ast;

/**
 * JPQL LIKE predicate node.
 *
 * <p>Represents a LIKE pattern matching predicate.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * b.title LIKE 'J%'
 * b.title LIKE '%Doe'
 * b.title LIKE '%John%'
 * b.title NOT LIKE 'A%'
 * </pre>
 *
 * @param expression the expression to match
 * @param pattern the pattern to match against
 * @param not true if this is a NOT LIKE predicate
 * @param escapeChar the escape character, or null if not specified
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlLikePredicate(
    JpqlExpr expression,
    JpqlExpr pattern,
    boolean not,
    Character escapeChar
) implements JpqlPredicate {

    /**
     * Creates a new LIKE predicate.
     *
     * @param expression the expression to match
     * @param pattern the pattern to match against
     * @param not true if this is a NOT LIKE predicate
     * @param escapeChar the escape character, or null if not specified
     */
    public JpqlLikePredicate {
        if (expression == null) {
            throw new IllegalArgumentException("expression cannot be null");
        }
        if (pattern == null) {
            throw new IllegalArgumentException("pattern cannot be null");
        }
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitLikePredicate(this, parameter);
    }

    /**
     * Returns true if this is a NOT LIKE predicate.
     *
     * @return true if NOT LIKE
     */
    public boolean isNotLike() {
        return not();
    }

    /**
     * Returns true if this is a LIKE predicate (not NOT LIKE).
     *
     * @return true if LIKE
     */
    public boolean isLike() {
        return !not();
    }

    /**
     * Creates a LIKE predicate.
     *
     * @param expression the expression to match
     * @param pattern the pattern
     * @return a new LIKE predicate
     */
    public static JpqlLikePredicate like(JpqlExpr expression, JpqlExpr pattern) {
        return new JpqlLikePredicate(expression, pattern, false, null);
    }

    /**
     * Creates a NOT LIKE predicate.
     *
     * @param expression the expression to match
     * @param pattern the pattern
     * @return a new NOT LIKE predicate
     */
    public static JpqlLikePredicate notLike(JpqlExpr expression, JpqlExpr pattern) {
        return new JpqlLikePredicate(expression, pattern, true, null);
    }

    /**
     * Creates a LIKE predicate with an escape character.
     *
     * @param expression the expression to match
     * @param pattern the pattern
     * @param escapeChar the escape character
     * @return a new LIKE predicate with escape
     */
    public static JpqlLikePredicate likeWithEscape(JpqlExpr expression, JpqlExpr pattern, char escapeChar) {
        return new JpqlLikePredicate(expression, pattern, false, escapeChar);
    }
}
