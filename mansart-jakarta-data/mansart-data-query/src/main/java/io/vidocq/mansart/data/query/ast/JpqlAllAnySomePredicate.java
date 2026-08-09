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
 * JPQL ALL/ANY/SOME predicate node.
 *
 * <p>Represents a quantified comparison with a subquery using ALL, ANY, or SOME.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * b.price > ALL (SELECT o.price FROM Order o WHERE o.book = b)
 * b.price > ANY (SELECT o.price FROM Order o WHERE o.book = b)
 * b.price > SOME (SELECT o.price FROM Order o WHERE o.book = b)
 * b.price = ALL (SELECT 10, 20, 30)
 * </pre>
 *
 * @param expression the expression to compare
 * @param operator the comparison operator (=, <>, <, <=, >, >=)
 * @param quantifier the quantifier (ALL, ANY, SOME)
 * @param subquery the subquery or expression list
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlAllAnySomePredicate(
    JpqlExpr expression,
    JpqlPredicate.ComparisonOperator operator,
    Quantifier quantifier,
    JpqlQuantifiedExpression subquery
) implements JpqlPredicate {

    /**
     * Enumeration of quantifiers.
     */
    public enum Quantifier {
        ALL,
        ANY,
        SOME
    }

    /**
     * Creates a new ALL/ANY/SOME predicate.
     *
     * @param expression the expression to compare
     * @param quantifier the quantifier
     * @param subquery the subquery or expression list
     */
    public JpqlAllAnySomePredicate {
        if (expression == null) {
            throw new IllegalArgumentException("expression cannot be null");
        }
        if (quantifier == null) {
            throw new IllegalArgumentException("quantifier cannot be null");
        }
        if (subquery == null) {
            throw new IllegalArgumentException("subquery cannot be null");
        }
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitAllAnySomePredicate(this, parameter);
    }

    /**
     * Returns true if this is an ALL predicate.
     *
     * @return true if ALL
     */
    public boolean isAll() {
        return quantifier() == Quantifier.ALL;
    }

    /**
     * Returns true if this is an ANY predicate.
     *
     * @return true if ANY
     */
    public boolean isAny() {
        return quantifier() == Quantifier.ANY || quantifier() == Quantifier.SOME;
    }

    /**
     * Returns true if this is a SOME predicate.
     *
     * @return true if SOME
     */
    public boolean isSome() {
        return quantifier() == Quantifier.SOME;
    }

    /**
     * Returns the subquery if this quantified expression is a subquery.
     *
     * @return the subquery, or null if this is an expression list
     */
    public JpqlSelectStmt getSubquery() {
        if (subquery instanceof JpqlQuantifiedExpression.Subquery sub) {
            return sub.query();
        }
        return null;
    }

    /**
     * Returns true if this quantified expression is a subquery.
     *
     * @return true if subquery
     */
    public boolean hasSubquery() {
        return subquery instanceof JpqlQuantifiedExpression.Subquery;
    }

    /**
     * Returns true if this quantified expression is an expression list.
     *
     * @return true if expression list
     */
    public boolean hasExpressionList() {
        return subquery instanceof JpqlQuantifiedExpression.ExpressionList;
    }

    /**
     * Creates an ALL predicate.
     *
     * @param expression the expression to compare
     * @param operator the comparison operator
     * @param subquery the subquery
     * @return a new ALL predicate
     */
    public static JpqlAllAnySomePredicate all(JpqlExpr expression, JpqlPredicate.ComparisonOperator operator, JpqlSelectStmt subquery) {
        return new JpqlAllAnySomePredicate(expression, operator, Quantifier.ALL, new JpqlQuantifiedExpression.Subquery(subquery));
    }

    /**
     * Creates an ANY predicate.
     *
     * @param expression the expression to compare
     * @param operator the comparison operator
     * @param subquery the subquery
     * @return a new ANY predicate
     */
    public static JpqlAllAnySomePredicate any(JpqlExpr expression, JpqlPredicate.ComparisonOperator operator, JpqlSelectStmt subquery) {
        return new JpqlAllAnySomePredicate(expression, operator, Quantifier.ANY, new JpqlQuantifiedExpression.Subquery(subquery));
    }

    /**
     * Creates a SOME predicate.
     *
     * @param expression the expression to compare
     * @param operator the comparison operator
     * @param subquery the subquery
     * @return a new SOME predicate
     */
    public static JpqlAllAnySomePredicate some(JpqlExpr expression, JpqlPredicate.ComparisonOperator operator, JpqlSelectStmt subquery) {
        return new JpqlAllAnySomePredicate(expression, operator, Quantifier.SOME, new JpqlQuantifiedExpression.Subquery(subquery));
    }

    /**
     * Creates an ALL predicate with EQUAL operator.
     *
     * @param expression the expression to compare
     * @param subquery the subquery
     * @return a new ALL predicate with EQUAL operator
     */
    public static JpqlAllAnySomePredicate all(JpqlExpr expression, JpqlSelectStmt subquery) {
        return all(expression, JpqlPredicate.ComparisonOperator.EQUAL, subquery);
    }

    /**
     * Creates an ANY predicate with EQUAL operator.
     *
     * @param expression the expression to compare
     * @param subquery the subquery
     * @return a new ANY predicate with EQUAL operator
     */
    public static JpqlAllAnySomePredicate any(JpqlExpr expression, JpqlSelectStmt subquery) {
        return any(expression, JpqlPredicate.ComparisonOperator.EQUAL, subquery);
    }

    /**
     * Creates a SOME predicate with EQUAL operator.
     *
     * @param expression the expression to compare
     * @param subquery the subquery
     * @return a new SOME predicate with EQUAL operator
     */
    public static JpqlAllAnySomePredicate some(JpqlExpr expression, JpqlSelectStmt subquery) {
        return some(expression, JpqlPredicate.ComparisonOperator.EQUAL, subquery);
    }
}

/**
 * Represents the quantified expression (subquery or expression list).
 */
sealed interface JpqlQuantifiedExpression {
    /**
     * A subquery.
     */
    record Subquery(JpqlSelectStmt query) implements JpqlQuantifiedExpression {
    }

    /**
     * An expression list (for ALL with literal values).
     */
    record ExpressionList(java.util.List<JpqlExpr> expressions) implements JpqlQuantifiedExpression {
        public ExpressionList {
            expressions = java.util.List.copyOf(expressions);
        }
    }
}
