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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.jpql;

import java.util.List;
import java.util.Objects;

/**
 * Abstract expression — everything that can appear in a JOIN, WHERE or HAVING expression.
 *
 * <p>Milestone: M7-13 — supports path navigations, literals, parameters,
 * and boolean operators (AND, OR, NOT, comparisons).
 */
public abstract class JPQLExpression {

    /**
     * A named path navigation, e.g. {@code p.name} or {@code p.address.city}.
     */
    public static final class PathExpression extends JPQLExpression {
        private final String identificationVariable;
        private final List<String> fieldPath;

        public PathExpression(String identificationVariable, List<String> fieldPath) {
            this.identificationVariable = Objects.requireNonNull(identificationVariable, "identificationVariable must not be null");
            this.fieldPath = Objects.requireNonNull(fieldPath, "fieldPath must not be null");
        }

        public String identificationVariable() { return identificationVariable; }
        public List<String> fieldPath() { return fieldPath; }

        @Override
        public String toString() {
            return "PathExpression{" + identificationVariable + "." + String.join(".", fieldPath) + "}";
        }
    }

    /** A literal value (string, number, boolean, null). */
    public abstract static class Literal extends JPQLExpression {
        public Object value() { throw new UnsupportedOperationException(); }
    }

    /** A string literal, e.g. {@code 'Paris'}. */
    public static final class StringLiteral extends Literal {
        private final String value;

        public StringLiteral(String value) {
            this.value = Objects.requireNonNull(value, "value must not be null");
        }

        @Override public Object value() { return value; }

        @Override
        public String toString() {
            return "Literal.STRING('" + value + "')";
        }
    }

    /** A numeric literal. */
    public static final class NumberLiteral extends Literal {
        private final Number value;

        public NumberLiteral(Number value) {
            this.value = Objects.requireNonNull(value, "value must not be null");
        }

        @Override public Number value() { return value; }

        @Override
        public String toString() {
            return "Literal.NUMBER(" + value + ")";
        }
    }

    /** A boolean literal: {@code TRUE} or {@code FALSE}. */
    public static final class BooleanLiteral extends Literal {
        private final boolean value;

        public BooleanLiteral(boolean value) { this.value = value; }

        @Override public Boolean value() { return value; }

        @Override
        public String toString() {
            return "Literal.BOOLEAN(" + value + ")";
        }
    }

    /** A null literal: {@code NULL}. */
    public static final class NullLiteral extends Literal {
        public static final NullLiteral INSTANCE = new NullLiteral();

        private NullLiteral() {}

        @Override public Object value() { return null; }

        @Override
        public String toString() {
            return "Literal.NULL";
        }
    }

    /** A parameter reference: {@code :name} or {@code ?}. */
    public static final class ParameterExpression extends JPQLExpression {
        private final String name;

        public ParameterExpression(String name) {
            this.name = Objects.requireNonNull(name, "parameter name must not be null or empty");
        }

        public String name() { return name; }

        @Override
        public String toString() {
            return "ParameterExpression(" + name + ")";
        }
    }

    /* ---- Boolean expression subtypes ---- */

    /**
     * A simple comparison: left vs. right with a comparator operator.
     */
    public static final class Comparison extends JPQLExpression {
        private final JPQLExpression left;
        private final JPQLComparator operator;
        private final JPQLExpression right;

        public Comparison(JPQLExpression left, JPQLComparator operator, JPQLExpression right) {
            this.left = Objects.requireNonNull(left, "left must not be null");
            this.operator = Objects.requireNonNull(operator, "operator must not be null");
            this.right = Objects.requireNonNull(right, "right must not be null");
        }

        public JPQLExpression left()  { return left; }
        public JPQLComparator operator() { return operator; }
        public JPQLExpression right() { return right; }

        @Override
        public String toString() {
            return "Comparison(" + left + " " + operator + " " + right + ")";
        }
    }

    /**
     * Null check: {@code path IS NULL} or {@code path IS NOT NULL}.
     */
    public static final class NullCheck extends JPQLExpression {
        private final JPQLExpression path;
        private final NullCheckType nullCheckType;

        public NullCheck(JPQLExpression path, NullCheckType nullCheckType) {
            this.path = Objects.requireNonNull(path, "path must not be null");
            this.nullCheckType = Objects.requireNonNull(nullCheckType, "nullCheckType must not be null");
        }

        public JPQLExpression path() { return path; }
        public NullCheckType nullCheckType() { return nullCheckType; }

        @Override
        public String toString() {
            return "NullCheck(" + path + " " + nullCheckType + ")";
        }

        public enum NullCheckType { IS_NULL, IS_NOT_NULL }
    }

    /** Boolean conjunction: {@code expr1 AND expr2}. */
    public static final class And extends JPQLExpression {
        private final JPQLExpression left;
        private final JPQLExpression right;

        public And(JPQLExpression left, JPQLExpression right) {
            this.left = Objects.requireNonNull(left, "left must not be null");
            this.right = Objects.requireNonNull(right, "right must not be null");
        }

        public JPQLExpression left() { return left; }
        public JPQLExpression right() { return right; }

        @Override
        public String toString() {
            return "And(" + left + " AND " + right + ")";
        }
    }

    /** Boolean disjunction: {@code expr1 OR expr2}. */
    public static final class Or extends JPQLExpression {
        private final JPQLExpression left;
        private final JPQLExpression right;

        public Or(JPQLExpression left, JPQLExpression right) {
            this.left = Objects.requireNonNull(left, "left must not be null");
            this.right = Objects.requireNonNull(right, "right must not be null");
        }

        public JPQLExpression left() { return left; }
        public JPQLExpression right() { return right; }

        @Override
        public String toString() {
            return "Or(" + left + " OR " + right + ")";
        }
    }

    /** Boolean negation: {@code NOT expr}. */
    public static final class Not extends JPQLExpression {
        private final JPQLExpression child;

        public Not(JPQLExpression child) {
            this.child = Objects.requireNonNull(child, "child must not be null");
        }

        public JPQLExpression child() { return child; }

        @Override
        public String toString() {
            return "NOT(" + child + ")";
        }
    }
}
