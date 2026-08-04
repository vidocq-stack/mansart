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
 * Base interface for JPQL expression nodes.
 *
 * <p>An expression is any part of a JPQL query that evaluates to a value.
 * Expressions can be used in SELECT clauses, WHERE clauses, HAVING clauses, etc.</p>
 *
 * <p>Supported expression types:</p>
 * <ul>
 *   <li>{@link JpqlPathExpr} - Navigation through entity attributes (e.g., "b.author.name")</li>
 *   <li>{@link JpqlLiteralExpr} - Literal values (strings, numbers, booleans, null)</li>
 *   <li>{@link JpqlFunctionExpr} - Function calls (e.g., "COUNT(b)", "UPPER(b.title)")</li>
 *   <li>{@link JpqlBinaryExpr} - Binary operations (e.g., "b.price + 10", "b.price > 100")</li>
 *   <li>{@link JpqlUnaryExpr} - Unary operations (e.g., "-b.price", "NOT b.active")</li>
 *   <li>{@link JpqlCaseExpr} - CASE expressions</li>
 *   <li>{@link JpqlTypeExpr} - TYPE expressions</li>
 *   <li>{@link JpqlParameterExpr} - Named or positional parameters</li>
 * </ul>
 *
 * @since 0.3.0-SNAPSHOT
 */
public sealed interface JpqlExpr extends JpqlNode 
    permits JpqlPathExpr, JpqlLiteralExpr, JpqlFunctionExpr, JpqlBinaryExpr, 
            JpqlUnaryExpr, JpqlCaseExpr, JpqlTypeExpr, JpqlParameterExpr {

    /**
     * Returns true if this expression represents a path navigation (entity attribute access).
     *
     * @return true if this is a path expression
     */
    default boolean isPath() {
        return this instanceof JpqlPathExpr;
    }

    /**
     * Returns true if this expression represents a literal value.
     *
     * @return true if this is a literal expression
     */
    default boolean isLiteral() {
        return this instanceof JpqlLiteralExpr;
    }
}
