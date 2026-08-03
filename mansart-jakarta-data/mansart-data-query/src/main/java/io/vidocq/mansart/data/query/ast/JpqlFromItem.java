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
 * JPQL FROM item node.
 *
 * <p>Represents an entity declaration in the FROM clause, potentially with joins.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * Book b JOIN b.author a
 * </pre>
 *
 * <p>This corresponds to:</p>
 * <pre>
 * JpqlFromItem(
 *     entityName = "Book",
 *     identifier = "b",
 *     joins = [JpqlJoin(path = "b.author", identifier = "a", type = INNER)]
 * )
 * </pre>
 *
 * @param entityName the name of the entity (class name)
 * @param identifier the identifier (alias) for this entity
 * @param joins the list of joins from this entity
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlFromItem(String entityName, String identifier, List<JpqlJoin> joins) {

    /**
     * Creates a new FROM item.
     *
     * @param entityName the name of the entity
     * @param identifier the identifier (alias)
     * @param joins the list of joins from this entity
     */
    public JpqlFromItem {
        if (entityName == null || entityName.isBlank()) {
            throw new IllegalArgumentException("entityName cannot be null or blank");
        }
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("identifier cannot be null or blank");
        }
        joins = List.copyOf(joins);
    }

    /**
     * Returns true if this from item has any joins.
     *
     * @return true if there are joins
     */
    public boolean hasJoins() {
        return !joins().isEmpty();
    }

    /**
     * Returns the join types present in this from item.
     *
     * @return list of join types
     */
    public List<JoinType> joinTypes() {
        return joins().stream().map(JpqlJoin::type).toList();
    }
}
