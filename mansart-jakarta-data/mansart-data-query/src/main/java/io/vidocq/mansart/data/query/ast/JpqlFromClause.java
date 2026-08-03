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
 * JPQL FROM clause node.
 *
 * <p>Represents the FROM clause of a SELECT query, containing entity declarations 
 * and join specifications.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * FROM Book b JOIN b.author a LEFT JOIN a.address addr
 * </pre>
 *
 * <p>This corresponds to:</p>
 * <pre>
 * JpqlFromClause([
 *     JpqlFromItem("Book", "b", []),
 *     JpqlFromItem("Author", "a", [JpqlJoin("b", "author", INNER)]),
 *     JpqlFromItem("Address", "addr", [JpqlJoin("a", "address", LEFT)])
 * ])
 * </pre>
 *
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlFromClause(List<JpqlFromItem> items) implements JpqlClause {

    /**
     * Creates a new FROM clause with the given items.
     *
     * @param items the from items (entity declarations and joins)
     */
    public JpqlFromClause {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("FROM clause must have at least one item");
        }
        items = List.copyOf(items);
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitFromClause(this, parameter);
    }

    /**
     * Returns the first from item (the root entity).
     *
     * @return the root entity declaration
     */
    public JpqlFromItem root() {
        return items().get(0);
    }

    /**
     * Returns true if this clause contains any joins.
     *
     * @return true if there are joins
     */
    public boolean hasJoins() {
        return items().size() > 1 || items().get(0).joins().size() > 0;
    }

    /**
     * Finds a from item by its identifier.
     *
     * @param identifier the identifier to search for
     * @return the from item, or null if not found
     */
    public JpqlFromItem findByIdentifier(String identifier) {
        for (JpqlFromItem item : items()) {
            if (item.identifier().equals(identifier)) {
                return item;
            }
            for (JpqlJoin join : item.joins()) {
                if (join.identifier().equals(identifier)) {
                    // Note: joins are flattened in the items list, so this may not be needed
                    // but we keep it for completeness
                }
            }
        }
        return null;
    }
}
