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
 * JPQL unary expression node.
 *
 * <p>Represents a unary operation on an expression.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * -b.price          (negation)
 * +b.quantity       (positive)
 * ABS(b.price)      (note: ABS is actually a function, not a unary operator)
 * </pre>
 *
 * @param operator the unary operator
 * @param operand the operand expression
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlUnaryExpr(UnaryOperator operator, JpqlExpr operand) implements JpqlExpr {

    /**
     * Enumeration of unary operators.
     */
    public enum UnaryOperator {
        NEGATE("-"),
        POSITIVE("+");

        private final String symbol;

        UnaryOperator(String symbol) {
            this.symbol = symbol;
        }

        public String symbol() {
            return symbol;
        }
    }

    /**
     * Creates a new unary expression.
     *
     * @param operator the unary operator
     * @param operand the operand expression
     */
    public JpqlUnaryExpr {
        if (operator == null) {
            throw new IllegalArgumentException("operator cannot be null");
        }
        if (operand == null) {
            throw new IllegalArgumentException("operand cannot be null");
        }
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitUnaryExpr(this, parameter);
    }

    /**
     * Creates a negation expression.
     *
     * @param operand the operand to negate
     * @return a new negation expression
     */
    public static JpqlUnaryExpr negate(JpqlExpr operand) {
        return new JpqlUnaryExpr(UnaryOperator.NEGATE, operand);
    }

    /**
     * Creates a positive expression.
     *
     * @param operand the operand
     * @return a new positive expression
     */
    public static JpqlUnaryExpr positive(JpqlExpr operand) {
        return new JpqlUnaryExpr(UnaryOperator.POSITIVE, operand);
    }

    /**
     * Returns true if this is a negation.
     *
     * @return true if negation
     */
    public boolean isNegation() {
        return operator() == UnaryOperator.NEGATE;
    }

    /**
     * Returns true if this is a positive operator.
     *
     * @return true if positive
     */
    public boolean isPositive() {
        return operator() == UnaryOperator.POSITIVE;
    }
}
