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

import java.util.List;

/**
 * JPQL function expression node.
 *
 * <p>Represents a function call in JPQL.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * COUNT(b)                         (aggregate function)
 * UPPER(b.title)                    (string function)
 * ABS(b.price)                     (numeric function)
 * CURRENT_DATE                      (date function, no arguments)
 * CONCAT(b.firstName, ' ', b.lastName) (string function with multiple args)
 * </pre>
 *
 * @param name the name of the function
 * @param arguments the list of arguments
 * @param distinct true if DISTINCT keyword is present (for aggregate functions)
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlFunctionExpr(String name, List<JpqlExpr> arguments, boolean distinct) implements JpqlExpr {

    /**
     * Creates a new function expression.
     *
     * @param name the name of the function
     * @param arguments the list of arguments
     * @param distinct true if DISTINCT keyword is present
     */
    public JpqlFunctionExpr {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("function name cannot be null or blank");
        }
        arguments = List.copyOf(arguments);
        name = name.toUpperCase();
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitFunctionExpr(this, parameter);
    }

    /**
     * Returns true if this is an aggregate function.
     *
     * @return true if aggregate function
     */
    public boolean isAggregate() {
        return AGGREGATE_FUNCTIONS.contains(name().toUpperCase());
    }

    /**
     * Returns true if this function has arguments.
     *
     * @return true if there are arguments
     */
    public boolean hasArguments() {
        return !arguments().isEmpty();
    }

    /**
     * Creates a function expression with the given name and arguments.
     *
     * @param name the function name
     * @param arguments the arguments
     * @return a new function expression
     */
    public static JpqlFunctionExpr of(String name, List<JpqlExpr> arguments) {
        return new JpqlFunctionExpr(name, arguments, false);
    }

    /**
     * Creates a DISTINCT function expression.
     *
     * @param name the function name
     * @param arguments the arguments
     * @return a new DISTINCT function expression
     */
    public static JpqlFunctionExpr distinct(String name, List<JpqlExpr> arguments) {
        return new JpqlFunctionExpr(name, arguments, true);
    }

    /**
     * Creates a function expression with no arguments.
     *
     * @param name the function name
     * @return a new function expression
     */
    public static JpqlFunctionExpr of(String name) {
        return new JpqlFunctionExpr(name, List.of(), false);
    }

    /**
     * Set of aggregate function names.
     */
    private static final java.util.Set<String> AGGREGATE_FUNCTIONS = java.util.Set.of(
        "AVG", "COUNT", "MAX", "MIN", "SUM",
        "COUNT_DISTINCT", "SUM_DISTINCT"
    );

    /**
     * Returns the function name in uppercase.
     *
     * @return the uppercase function name
     */
    public String functionName() {
        return name();
    }
}
