/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under
 * the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import java.util.Map;

/**
 * Maps JPQL scalar function names to their SQL equivalents.
 *
 * <p>Used by {@link JpqlToSqlTranslator} to wrap a column comparison
 * in a {@code Where.Func(fn, inner)} node. Supported functions:
 * {@code UPPER}, {@code LOWER}, {@code LENGTH}, {@code ABS}, {@code SQRT}.
 * Additional functions ({@code LOCATE}, {@code SUBSTRING}, {@code LEFT},
 * {@code RIGHT}, {@code CONCAT}, {@code CAST}) are handled as raw SQL
 * fragments passed through the literal value field.</p>
 */
final class JpqlFunctionRegistry {

    private static final Map<String, String> FUNCTION_MAP = Map.of(
            "UPPER", "UPPER",
            "LOWER", "LOWER",
            "LENGTH", "CHAR_LENGTH",
            "ABS", "ABS",
            "SQRT", "SQRT"
    );

    /**
     * Returns the SQL function name for the given JPQL function name,
     * or {@code null} if the function is not a known scalar function
     * that maps to {@code Where.Func}.
     *
     * @param jpqlFunction the JPQL function name (e.g. "UPPER")
     * @return the SQL function name, or null for non-Func functions
     */
    static String sqlFunction(String jpqlFunction) {
        return FUNCTION_MAP.get(jpqlFunction.toUpperCase());
    }

    /**
     * Returns {@code true} if the given JPQL function name maps to a
     * {@code Where.Func} node (i.e. a unary scalar function applied
     * to the column).
     *
     * @param jpqlFunction the JPQL function name
     * @return {@code true} if this is a Func-compatible function
     */
    static boolean isFuncFunction(String jpqlFunction) {
        return sqlFunction(jpqlFunction) != null;
    }
}
