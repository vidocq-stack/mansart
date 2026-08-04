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
package io.vidocq.mansart.persistence.core;

import io.vidocq.mansart.data.dialect.Attribute;
import io.vidocq.mansart.data.dialect.Where;
import io.vidocq.mansart.data.query.ast.JpqlExpr;
import io.vidocq.mansart.data.query.ast.JpqlFunctionExpr;
import io.vidocq.mansart.data.query.ast.JpqlPathExpr;

import java.util.List;
import java.util.Map;

/**
 * Converts JPQL function expressions to Mansart Data Where predicates.
 * 
 * <p>M6 — JPQL function support for WHERE clauses.</p>
 */
public final class JpqlFunctionConverter {

    private JpqlFunctionConverter() {}

    /**
     * Set of string function names.
     */
    private static final Map<String, String> STRING_FUNCTIONS = Map.of(
        "UPPER", "UPPER",
        "LOWER", "LOWER",
        "TRIM", "TRIM",
        "CONCAT", "CONCAT",
        "SUBSTRING", "SUBSTRING",
        "LENGTH", "LENGTH"
    );

    /**
     * Set of numeric function names.
     */
    private static final Map<String, String> NUMERIC_FUNCTIONS = Map.of(
        "ABS", "ABS",
        "MOD", "MOD",
        "SQRT", "SQRT",
        "SIZE", "SIZE"
    );

    /**
     * Set of date function names.
     */
    private static final Map<String, String> DATE_FUNCTIONS = Map.of(
        "CURRENT_DATE", "CURRENT_DATE",
        "CURRENT_TIME", "CURRENT_TIME",
        "CURRENT_TIMESTAMP", "CURRENT_TIMESTAMP",
        "LOCAL_DATE", "LOCAL_DATE",
        "LOCAL_TIME", "LOCAL_TIME",
        "LOCAL_DATETIME", "LOCAL_DATETIME"
    );

    /**
     * Set of aggregate function names.
     */
    private static final Map<String, String> AGGREGATE_FUNCTIONS = Map.of(
        "AVG", "AVG",
        "COUNT", "COUNT",
        "MAX", "MAX",
        "MIN", "MIN",
        "SUM", "SUM"
    );

    /**
     * Checks if a function name is a known JPQL function.
     */
    public static boolean isKnownFunction(String functionName) {
        if (functionName == null) return false;
        String upperName = functionName.toUpperCase();
        return STRING_FUNCTIONS.containsKey(upperName) ||
               NUMERIC_FUNCTIONS.containsKey(upperName) ||
               DATE_FUNCTIONS.containsKey(upperName) ||
               AGGREGATE_FUNCTIONS.containsKey(upperName);
    }

    /**
     * Converts a JPQL function expression to a Where predicate.
     * For functions used in comparisons like "UPPER(b.name) = 'TEST'", wraps the inner
     * attribute with a Func where.
     *
     * @param func the function expression
     * @param context the query execution context
     * @return a Where predicate representing the function
     */
    public static Where convertFunction(JpqlFunctionExpr func, QueryExecutionContext context) {
        Attribute<?, ?> attr = convertFunctionToAttribute(func, context);
        // Wrap the attribute with Func
        Where inner = new Where.Eq(attr);
        return new Where.Func(func.name(), inner);
    }

    /**
     * Converts a JPQL function expression to an Attribute by extracting the attribute
     * from its first argument. For functions like UPPER(b.name), returns the Attribute for b.name.
     * The function information is preserved separately via Where.Func.
     *
     * @param func the function expression
     * @param context the query execution context
     * @return the Attribute from the first function argument
     */
    public static Attribute<?, ?> convertFunctionToAttribute(JpqlFunctionExpr func, QueryExecutionContext context) {
        String functionName = func.name();
        
        if (!isKnownFunction(functionName)) {
            throw new UnsupportedOperationException("Unknown JPQL function: " + functionName);
        }
        
        // For now, we only handle functions with exactly one argument
        // that is a path expression (e.g., UPPER(b.name))
        List<JpqlExpr> args = func.arguments();
        
        if (args.isEmpty()) {
            // Functions with no arguments (CURRENT_DATE, etc.) can't be converted to an attribute
            throw new UnsupportedOperationException(
                "Function " + functionName + " with no arguments cannot be converted to an attribute");
        }
        
        // Extract the attribute from the first argument
        // If the first argument is a path, we get its attribute
        // If it's a literal, we can't create an Attribute
        JpqlExpr firstArg = args.get(0);
        
        if (firstArg instanceof io.vidocq.mansart.data.query.ast.JpqlPathExpr path) {
            return context.resolvePath(path);
        } else if (firstArg instanceof JpqlFunctionExpr nestedFunc) {
            // Recursively extract from nested function
            return convertFunctionToAttribute(nestedFunc, context);
        }
        
        throw new UnsupportedOperationException(
            "Cannot convert function " + functionName + " to attribute: first argument is " + 
            firstArg.getClass().getSimpleName());
    }

    /**
     * Checks if a function is an aggregate function.
     */
    public static boolean isAggregateFunction(String functionName) {
        if (functionName == null) return false;
        return AGGREGATE_FUNCTIONS.containsKey(functionName.toUpperCase());
    }

    /**
     * Checks if a function is a string function.
     */
    public static boolean isStringFunction(String functionName) {
        if (functionName == null) return false;
        return STRING_FUNCTIONS.containsKey(functionName.toUpperCase());
    }

    /**
     * Checks if a function is a numeric function.
     */
    public static boolean isNumericFunction(String functionName) {
        if (functionName == null) return false;
        return NUMERIC_FUNCTIONS.containsKey(functionName.toUpperCase());
    }

    /**
     * Checks if a function is a date function.
     */
    public static boolean isDateFunction(String functionName) {
        if (functionName == null) return false;
        return DATE_FUNCTIONS.containsKey(functionName.toUpperCase());
    }
}
