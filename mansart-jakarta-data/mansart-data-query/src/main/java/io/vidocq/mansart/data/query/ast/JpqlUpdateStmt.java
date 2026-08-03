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
import java.util.Optional;

/**
 * JPQL UPDATE statement node.
 *
 * <p>Represents an UPDATE query in JPQL.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * UPDATE Book b SET b.price = b.price * 1.1 WHERE b.category = 'Fiction'
 * </pre>
 *
 * @param entityName the name of the entity to update
 * @param identifier the identifier (alias) for the entity
 * @param setClauses the SET clauses
 * @param whereClause optional WHERE clause
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlUpdateStmt(
    String entityName,
    String identifier,
    List<JpqlSetClause> setClauses,
    Optional<JpqlWhereClause> whereClause
) implements JpqlStmt {

    /**
     * Creates a new UPDATE statement.
     *
     * @param entityName the name of the entity to update
     * @param identifier the identifier (alias) for the entity
     * @param setClauses the SET clauses
     * @param whereClause optional WHERE clause
     */
    public JpqlUpdateStmt {
        if (entityName == null || entityName.isBlank()) {
            throw new IllegalArgumentException("entityName cannot be null or blank");
        }
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("identifier cannot be null or blank");
        }
        if (setClauses == null || setClauses.isEmpty()) {
            throw new IllegalArgumentException("UPDATE statement must have at least one SET clause");
        }
        setClauses = List.copyOf(setClauses);
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitUpdate(this, parameter);
    }

    /**
     * Returns true if this UPDATE has a WHERE clause.
     *
     * @return true if WHERE clause is present
     */
    public boolean hasWhere() {
        return whereClause().isPresent();
    }
}

/**
 * JPQL SET clause node.
 *
 * <p>Represents a SET clause in an UPDATE statement.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * SET b.price = b.price * 1.1
 * </pre>
 *
 * @param path the path expression to update
 * @param value the new value
 */
record JpqlSetClause(JpqlPathExpr path, JpqlExpr value) {
    /**
     * Creates a new SET clause.
     *
     * @param path the path expression to update
     * @param value the new value
     */
    public JpqlSetClause {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
    }
}
