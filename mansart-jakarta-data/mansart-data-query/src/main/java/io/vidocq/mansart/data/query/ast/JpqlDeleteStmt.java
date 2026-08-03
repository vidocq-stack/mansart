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

import java.util.Optional;

/**
 * JPQL DELETE statement node.
 *
 * <p>Represents a DELETE query in JPQL.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * DELETE FROM Book b WHERE b.price < 10
 * </pre>
 *
 * @param entityName the name of the entity to delete from
 * @param identifier the identifier (alias) for the entity
 * @param whereClause optional WHERE clause
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlDeleteStmt(
    String entityName,
    String identifier,
    Optional<JpqlWhereClause> whereClause
) implements JpqlStmt {

    /**
     * Creates a new DELETE statement.
     *
     * @param entityName the name of the entity to delete from
     * @param identifier the identifier (alias) for the entity
     * @param whereClause optional WHERE clause
     */
    public JpqlDeleteStmt {
        if (entityName == null || entityName.isBlank()) {
            throw new IllegalArgumentException("entityName cannot be null or blank");
        }
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("identifier cannot be null or blank");
        }
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitDelete(this, parameter);
    }

    /**
     * Returns true if this DELETE has a WHERE clause.
     *
     * @return true if WHERE clause is present
     */
    public boolean hasWhere() {
        return whereClause().isPresent();
    }
}
