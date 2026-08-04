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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * JPQL literal expression node.
 *
 * <p>Represents a literal value in JPQL, such as strings, numbers, booleans, dates, and NULL.</p>
 *
 * @since 0.3.0-SNAPSHOT
 */
public sealed interface JpqlLiteralExpr extends JpqlExpr permits JpqlStringLiteral, JpqlNumericLiteral,
        JpqlBooleanLiteral, JpqlNullLiteral, JpqlTemporalLiteral {

    Object getValue();

    LiteralType type();

    enum LiteralType {
        STRING, INTEGER, LONG, FLOAT, DOUBLE, BIG_INTEGER, BIG_DECIMAL, BOOLEAN, NULL, DATE, TIME, TIMESTAMP
    }

    static JpqlStringLiteral of(String value) {
        return new JpqlStringLiteral(value);
    }

    static JpqlNumericLiteral of(int value) {
        return new JpqlNumericLiteral(value);
    }

    static JpqlNumericLiteral of(long value) {
        return new JpqlNumericLiteral(value);
    }

    static JpqlNumericLiteral of(double value) {
        return new JpqlNumericLiteral(value);
    }

    static JpqlBooleanLiteral of(boolean value) {
        return new JpqlBooleanLiteral(value);
    }

    static JpqlNullLiteral nullLiteral() {
        return JpqlNullLiteral.INSTANCE;
    }

    static JpqlTemporalLiteral of(LocalDate value) {
        return new JpqlTemporalLiteral(value, null, null);
    }

    static JpqlTemporalLiteral of(LocalTime value) {
        return new JpqlTemporalLiteral(null, value, null);
    }

    static JpqlTemporalLiteral of(LocalDateTime value) {
        return new JpqlTemporalLiteral(null, null, value);
    }
}

record JpqlStringLiteral(String value) implements JpqlLiteralExpr {
    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public LiteralType type() {
        return LiteralType.STRING;
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitStringLiteral(this, parameter);
    }

    @Override
    public String toString() {
        return "'" + escape(value) + "'";
    }

    private static String escape(String s) {
        return s.replace("'", "''");
    }
}

record JpqlNumericLiteral(Number value) implements JpqlLiteralExpr {
    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public LiteralType type() {
        if (value instanceof Integer) return LiteralType.INTEGER;
        if (value instanceof Long) return LiteralType.LONG;
        if (value instanceof Float) return LiteralType.FLOAT;
        if (value instanceof Double) return LiteralType.DOUBLE;
        if (value instanceof BigInteger) return LiteralType.BIG_INTEGER;
        if (value instanceof BigDecimal) return LiteralType.BIG_DECIMAL;
        throw new IllegalArgumentException("Unsupported numeric type: " + value.getClass());
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitNumericLiteral(this, parameter);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

record JpqlBooleanLiteral(boolean value) implements JpqlLiteralExpr {
    @Override
    public Object getValue() {
        return Boolean.valueOf(value);
    }

    @Override
    public LiteralType type() {
        return LiteralType.BOOLEAN;
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitBooleanLiteral(this, parameter);
    }

    @Override
    public String toString() {
        return value ? "TRUE" : "FALSE";
    }
}

record JpqlNullLiteral() implements JpqlLiteralExpr {
    static final JpqlNullLiteral INSTANCE = new JpqlNullLiteral();

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public LiteralType type() {
        return LiteralType.NULL;
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitNullLiteral(this, parameter);
    }

    @Override
    public String toString() {
        return "NULL";
    }
}

record JpqlTemporalLiteral(LocalDate date, LocalTime time, LocalDateTime timestamp) implements JpqlLiteralExpr {
    @Override
    public Object getValue() {
        if (date != null) return date;
        if (time != null) return time;
        if (timestamp != null) return timestamp;
        return null;
    }

    @Override
    public LiteralType type() {
        if (date != null) return LiteralType.DATE;
        if (time != null) return LiteralType.TIME;
        if (timestamp != null) return LiteralType.TIMESTAMP;
        throw new IllegalStateException("At least one temporal value must be set");
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitTemporalLiteral(this, parameter);
    }

    @Override
    public String toString() {
        if (date != null) return "DATE '" + date + "'";
        if (time != null) return "TIME '" + time + "'";
        if (timestamp != null) return "TIMESTAMP '" + timestamp + "'";
        return "";
    }
}
