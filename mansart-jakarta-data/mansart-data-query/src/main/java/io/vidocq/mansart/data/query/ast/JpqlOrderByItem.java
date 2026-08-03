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
 * JPQL ORDER BY item node.
 *
 * <p>Represents a single sort specification in an ORDER BY clause.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * b.title ASC
 * b.price DESC
 * </pre>
 *
 * @param expression the expression to sort by
 * @param ascending true for ASC, false for DESC
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlOrderByItem(JpqlExpr expression, boolean ascending) {

    /**
     * Creates a new ORDER BY item.
     *
     * @param expression the expression to sort by
     * @param ascending true for ASC, false for DESC
     */
    public JpqlOrderByItem {
        if (expression == null) {
            throw new IllegalArgumentException("expression cannot be null");
        }
    }

    /**
     * Creates an ascending ORDER BY item.
     *
     * @param expression the expression to sort by
     * @return a new ascending ORDER BY item
     */
    public static JpqlOrderByItem asc(JpqlExpr expression) {
        return new JpqlOrderByItem(expression, true);
    }

    /**
     * Creates a descending ORDER BY item.
     *
     * @param expression the expression to sort by
     * @return a new descending ORDER BY item
     */
    public static JpqlOrderByItem desc(JpqlExpr expression) {
        return new JpqlOrderByItem(expression, false);
    }

    /**
     * Returns the sort direction as a string.
     *
     * @return "ASC" or "DESC"
     */
    public String direction() {
        return ascending ? "ASC" : "DESC";
    }
}
