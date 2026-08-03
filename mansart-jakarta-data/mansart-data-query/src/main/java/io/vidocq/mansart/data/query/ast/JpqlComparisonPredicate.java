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
 * JPQL comparison predicate node.
 *
 * <p>Represents a comparison between two expressions using an operator.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * b.price > 100
 * b.title = 'John'
 * b.author IS NULL
 * b.author IS NOT NULL
 * </pre>
 *
 * <p>Note: IS NULL and IS NOT NULL are special cases of comparison predicates 
 * where the right expression is absent (represented as null).</p>
 *
 * @param operator the comparison operator
 * @param left the left expression
 * @param right the right expression (null for IS NULL/IS NOT NULL)
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlComparisonPredicate(
    JpqlPredicate.ComparisonOperator operator,
    JpqlExpr left,
    JpqlExpr right
) implements JpqlPredicate {

    /**
     * Creates a new comparison predicate.
     *
     * @param operator the comparison operator
     * @param left the left expression
     * @param right the right expression (can be null for IS NULL/IS NOT NULL)
     */
    public JpqlComparisonPredicate {
        if (operator == null) {
            throw new IllegalArgumentException("operator cannot be null");
        }
        if (left == null) {
            throw new IllegalArgumentException("left expression cannot be null");
        }
        // For IS NULL and IS NOT NULL, right must be null
        if ((operator == ComparisonOperator.IS_NULL || operator == ComparisonOperator.IS_NOT_NULL) && right != null) {
            throw new IllegalArgumentException(
                operator + " predicate cannot have a right expression");
        }
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitComparisonPredicate(this, parameter);
    }

    /**
     * Returns true if this is an IS NULL predicate.
     *
     * @return true if IS NULL
     */
    public boolean isNull() {
        return operator() == ComparisonOperator.IS_NULL;
    }

    /**
     * Returns true if this is an IS NOT NULL predicate.
     *
     * @return true if IS NOT NULL
     */
    public boolean isNotNull() {
        return operator() == ComparisonOperator.IS_NOT_NULL;
    }

    /**
     * Returns true if this is an equality comparison.
     *
     * @return true if equality comparison
     */
    public boolean isEquality() {
        return operator() == ComparisonOperator.EQUAL || operator() == ComparisonOperator.NOT_EQUAL;
    }

    /**
     * Returns true if this is a relational comparison (<, <=, >, >=).
     *
     * @return true if relational comparison
     */
    public boolean isRelational() {
        return operator() == ComparisonOperator.LESS_THAN ||
               operator() == ComparisonOperator.LESS_THAN_OR_EQUAL ||
               operator() == ComparisonOperator.GREATER_THAN ||
               operator() == ComparisonOperator.GREATER_THAN_OR_EQUAL;
    }

    /**
     * Creates an equality predicate (left = right).
     *
     * @param left the left expression
     * @param right the right expression
     * @return a new equality predicate
     */
    public static JpqlComparisonPredicate equal(JpqlExpr left, JpqlExpr right) {
        return new JpqlComparisonPredicate(ComparisonOperator.EQUAL, left, right);
    }

    /**
     * Creates a not-equal predicate (left <> right).
     *
     * @param left the left expression
     * @param right the right expression
     * @return a new not-equal predicate
     */
    public static JpqlComparisonPredicate notEqual(JpqlExpr left, JpqlExpr right) {
        return new JpqlComparisonPredicate(ComparisonOperator.NOT_EQUAL, left, right);
    }

    /**
     * Creates a less-than predicate (left < right).
     *
     * @param left the left expression
     * @param right the right expression
     * @return a new less-than predicate
     */
    public static JpqlComparisonPredicate lessThan(JpqlExpr left, JpqlExpr right) {
        return new JpqlComparisonPredicate(ComparisonOperator.LESS_THAN, left, right);
    }

    /**
     * Creates a less-than-or-equal predicate (left <= right).
     *
     * @param left the left expression
     * @param right the right expression
     * @return a new less-than-or-equal predicate
     */
    public static JpqlComparisonPredicate lessThanOrEqual(JpqlExpr left, JpqlExpr right) {
        return new JpqlComparisonPredicate(ComparisonOperator.LESS_THAN_OR_EQUAL, left, right);
    }

    /**
     * Creates a greater-than predicate (left > right).
     *
     * @param left the left expression
     * @param right the right expression
     * @return a new greater-than predicate
     */
    public static JpqlComparisonPredicate greaterThan(JpqlExpr left, JpqlExpr right) {
        return new JpqlComparisonPredicate(ComparisonOperator.GREATER_THAN, left, right);
    }

    /**
     * Creates a greater-than-or-equal predicate (left >= right).
     *
     * @param left the left expression
     * @param right the right expression
     * @return a new greater-than-or-equal predicate
     */
    public static JpqlComparisonPredicate greaterThanOrEqual(JpqlExpr left, JpqlExpr right) {
        return new JpqlComparisonPredicate(ComparisonOperator.GREATER_THAN_OR_EQUAL, left, right);
    }

    /**
     * Creates an IS NULL predicate.
     *
     * @param expression the expression to test for null
     * @return a new IS NULL predicate
     */
    public static JpqlComparisonPredicate isNull(JpqlExpr expression) {
        return new JpqlComparisonPredicate(ComparisonOperator.IS_NULL, expression, null);
    }

    /**
     * Creates an IS NOT NULL predicate.
     *
     * @param expression the expression to test for not null
     * @return a new IS NOT NULL predicate
     */
    public static JpqlComparisonPredicate isNotNull(JpqlExpr expression) {
        return new JpqlComparisonPredicate(ComparisonOperator.IS_NOT_NULL, expression, null);
    }
}
