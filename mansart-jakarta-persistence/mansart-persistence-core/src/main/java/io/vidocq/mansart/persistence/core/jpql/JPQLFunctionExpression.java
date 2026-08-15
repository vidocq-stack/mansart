/*
 * Copyright (c) 2026 Vidocq contributors
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.jpql;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a JPQL function call.
 *
 * <p>Milestone: M8-5 — Additional JPQL functions support.
 * <p>Examples: LOWER(e.name), CONCAT(e.firstName, e.lastName), SIZE(e.items)
 */
public final class JPQLFunctionExpression extends JPQLExpression {

    private final String functionName;
    private final List<JPQLExpression> arguments;

    /**
     * Creates a new function expression.
     *
     * @param functionName the name of the function (e.g., "LOWER", "CONCAT", "SIZE")
     * @param arguments the function arguments
     */
    public JPQLFunctionExpression(String functionName, List<JPQLExpression> arguments) {
        this.functionName = Objects.requireNonNull(functionName, "functionName must not be null");
        this.arguments = Collections.unmodifiableList(
                Objects.requireNonNull(arguments, "arguments must not be null"));
    }

    public String functionName() { return functionName; }
    public List<JPQLExpression> arguments() { return arguments; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(functionName).append("(");
        boolean first = true;
        for (JPQLExpression arg : arguments) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(arg);
            first = false;
        }
        sb.append(")");
        return sb.toString();
    }
}
