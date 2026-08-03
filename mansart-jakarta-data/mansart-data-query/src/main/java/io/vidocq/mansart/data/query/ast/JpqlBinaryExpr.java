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
 * JPQL binary expression node.
 *
 * <p>Represents a binary operation between two expressions.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * b.price + 10          (addition)
 * b.price * 1.1        (multiplication)
 * b.price > 100        (comparison - note: comparisons are typically represented as predicates)
 * b.firstName || ' ' || b.lastName  (string concatenation)
 * </pre>
 *
 * <p>Note: In JPQL, most binary operators are arithmetic or string operations. 
 * Comparison operations are typically represented as predicates (e.g., {@link JpqlComparisonPredicate}).</p>
 *
 * @param operator the binary operator
 * @param left the left expression
 * @param right the right expression
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlBinaryExpr(BinaryOperator operator, JpqlExpr left, JpqlExpr right) implements JpqlExpr {

    /**
     * Enumeration of binary operators.
     */
    public enum BinaryOperator {
        // Arithmetic
        ADD("+"),
        SUBTRACT("-"),
        MULTIPLY("*"),
        DIVIDE("/"),
        MOD("MOD"),
        
        // String
        CONCAT("||"),
        
        // Bitwise
        BITWISE_AND("&"),
        BITWISE_OR("|"),
        BITWISE_XOR("^"),
        BITWISE_SHIFT_LEFT("<<"),
        BITWISE_SHIFT_RIGHT(">>"),
        
        // Other
        COALESCE("COALESCE"),
        NULLIF("NULLIF");

        private final String symbol;

        BinaryOperator(String symbol) {
            this.symbol = symbol;
        }

        public String symbol() {
            return symbol;
        }

        /**
         * Returns true if this is an arithmetic operator.
         *
         * @return true if arithmetic
         */
        public boolean isArithmetic() {
            return this == ADD || this == SUBTRACT || this == MULTIPLY || this == DIVIDE || this == MOD;
        }

        /**
         * Returns true if this is a string operator.
         *
         * @return true if string operator
         */
        public boolean isString() {
            return this == CONCAT;
        }

        /**
         * Returns true if this is a bitwise operator.
         *
         * @return true if bitwise
         */
        public boolean isBitwise() {
            return this == BITWISE_AND || this == BITWISE_OR || this == BITWISE_XOR ||
                   this == BITWISE_SHIFT_LEFT || this == BITWISE_SHIFT_RIGHT;
        }
    }

    /**
     * Creates a new binary expression.
     *
     * @param operator the binary operator
     * @param left the left expression
     * @param right the right expression
     */
    public JpqlBinaryExpr {
        if (operator == null) {
            throw new IllegalArgumentException("operator cannot be null");
        }
        if (left == null) {
            throw new IllegalArgumentException("left expression cannot be null");
        }
        if (right == null) {
            throw new IllegalArgumentException("right expression cannot be null");
        }
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitBinaryExpr(this, parameter);
    }

    /**
     * Creates an addition expression.
     *
     * @param left the left expression
     * @param right the right expression
     * @return a new addition expression
     */
    public static JpqlBinaryExpr add(JpqlExpr left, JpqlExpr right) {
        return new JpqlBinaryExpr(BinaryOperator.ADD, left, right);
    }

    /**
     * Creates a subtraction expression.
     *
     * @param left the left expression
     * @param right the right expression
     * @return a new subtraction expression
     */
    public static JpqlBinaryExpr subtract(JpqlExpr left, JpqlExpr right) {
        return new JpqlBinaryExpr(BinaryOperator.SUBTRACT, left, right);
    }

    /**
     * Creates a multiplication expression.
     *
     * @param left the left expression
     * @param right the right expression
     * @return a new multiplication expression
     */
    public static JpqlBinaryExpr multiply(JpqlExpr left, JpqlExpr right) {
        return new JpqlBinaryExpr(BinaryOperator.MULTIPLY, left, right);
    }

    /**
     * Creates a division expression.
     *
     * @param left the left expression
     * @param right the right expression
     * @return a new division expression
     */
    public static JpqlBinaryExpr divide(JpqlExpr left, JpqlExpr right) {
        return new JpqlBinaryExpr(BinaryOperator.DIVIDE, left, right);
    }

    /**
     * Creates a string concatenation expression.
     *
     * @param left the left expression
     * @param right the right expression
     * @return a new concatenation expression
     */
    public static JpqlBinaryExpr concat(JpqlExpr left, JpqlExpr right) {
        return new JpqlBinaryExpr(BinaryOperator.CONCAT, left, right);
    }
}
