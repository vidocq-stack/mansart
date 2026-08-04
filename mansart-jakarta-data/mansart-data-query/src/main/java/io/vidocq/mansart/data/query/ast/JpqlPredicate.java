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
 * Base interface for JPQL predicate nodes.
 *
 * <p>A predicate is a condition that evaluates to a boolean value. Predicates are used 
 * in WHERE and HAVING clauses, and can be combined with logical operators (AND, OR, NOT).</p>
 *
 * <p>Supported predicate types:</p>
 * <ul>
 *   <li>{@link JpqlComparisonPredicate} - Comparison operators (=, <>, <, <=, >, >=, IS NULL, IS NOT NULL)</li>
 *   <li>{@link JpqlLikePredicate} - LIKE pattern matching</li>
 *   <li>{@link JpqlInPredicate} - IN expression</li>
 *   <li>{@link JpqlBetweenPredicate} - BETWEEN expression</li>
 *   <li>{@link JpqlAndPredicate} - Logical AND</li>
 *   <li>{@link JpqlOrPredicate} - Logical OR</li>
 *   <li>{@link JpqlNotPredicate} - Logical NOT</li>
 *   <li>{@link JpqlExistsPredicate} - EXISTS subquery</li>
 *   <li>{@link JpqlAllAnySomePredicate} - ALL/ANY/SOME subquery</li>
 * </ul>
 *
 * <p>This interface is inspired by the existing {@code Where} sealed interface 
 * from Mansart Data ({@link io.vidocq.mansart.data.dialect.spi.Where}).</p>
 *
 * @since 0.3.0-SNAPSHOT
 */
public sealed interface JpqlPredicate extends JpqlNode 
    permits JpqlComparisonPredicate, JpqlLikePredicate, JpqlInPredicate, 
            JpqlBetweenPredicate, JpqlAndPredicate, JpqlOrPredicate, JpqlNotPredicate,
            JpqlExistsPredicate, JpqlAllAnySomePredicate {

    /**
     * Enumeration of comparison operators.
     */
    enum ComparisonOperator {
        EQUAL("="),
        NOT_EQUAL("<>"),
        LESS_THAN("<"),
        LESS_THAN_OR_EQUAL("<="),
        GREATER_THAN(">"),
        GREATER_THAN_OR_EQUAL(">="),
        IS_NULL("IS NULL"),
        IS_NOT_NULL("IS NOT NULL");

        private final String symbol;

        ComparisonOperator(String symbol) {
            this.symbol = symbol;
        }

        public String symbol() {
            return symbol;
        }
    }

    /**
     * Returns true if this predicate is a conjunction (AND).
     *
     * @return true if this is an AND predicate
     */
    default boolean isConjunction() {
        return this instanceof JpqlAndPredicate;
    }

    /**
     * Returns true if this predicate is a disjunction (OR).
     *
     * @return true if this is an OR predicate
     */
    default boolean isDisjunction() {
        return this instanceof JpqlOrPredicate;
    }

    /**
     * Returns true if this predicate is a negation (NOT).
     *
     * @return true if this is a NOT predicate
     */
    default boolean isNegation() {
        return this instanceof JpqlNotPredicate;
    }

    /**
     * Returns true if this predicate is a comparison.
     *
     * @return true if this is a comparison predicate
     */
    default boolean isComparison() {
        return this instanceof JpqlComparisonPredicate;
    }

    /**
     * Returns true if this predicate is a LIKE predicate.
     *
     * @return true if this is a LIKE predicate
     */
    default boolean isLike() {
        return this instanceof JpqlLikePredicate;
    }

    /**
     * Returns true if this predicate is an IN predicate.
     *
     * @return true if this is an IN predicate
     */
    default boolean isIn() {
        return this instanceof JpqlInPredicate;
    }

    /**
     * Returns true if this predicate is a BETWEEN predicate.
     *
     * @return true if this is a BETWEEN predicate
     */
    default boolean isBetween() {
        return this instanceof JpqlBetweenPredicate;
    }
}
