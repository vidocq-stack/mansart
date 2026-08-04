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

import java.util.List;

/**
 * JPQL ORDER BY clause node.
 *
 * <p>Represents the ORDER BY clause of a JPQL query, containing the sort specifications.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * ORDER BY b.title ASC, b.price DESC
 * </pre>
 *
 * @param items the list of order by items
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlOrderByClause(List<JpqlOrderByItem> items) implements JpqlClause {

    /**
     * Creates a new ORDER BY clause.
     *
     * @param items the list of order by items
     */
    public JpqlOrderByClause {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("ORDER BY clause must have at least one item");
        }
        items = List.copyOf(items);
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitOrderByClause(this, parameter);
    }

    /**
     * Creates an ORDER BY clause with a single item.
     *
     * @param expression the expression to sort by
     * @param ascending true for ASC, false for DESC
     * @return a new ORDER BY clause
     */
    public static JpqlOrderByClause of(JpqlExpr expression, boolean ascending) {
        return new JpqlOrderByClause(List.of(new JpqlOrderByItem(expression, ascending)));
    }

    /**
     * Creates an ORDER BY clause with multiple items.
     *
     * @param items the order by items
     * @return a new ORDER BY clause
     */
    public static JpqlOrderByClause of(List<JpqlOrderByItem> items) {
        return new JpqlOrderByClause(items);
    }
}
