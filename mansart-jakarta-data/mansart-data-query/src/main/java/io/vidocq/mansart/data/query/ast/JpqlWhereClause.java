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
 * JPQL WHERE clause node.
 *
 * <p>Represents the WHERE clause of a JPQL query, containing the filter condition.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * WHERE b.price > 100 AND b.author.name LIKE 'J%'
 * </pre>
 *
 * @param predicate the predicate (condition) for filtering
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlWhereClause(JpqlPredicate predicate) implements JpqlClause {

    /**
     * Creates a new WHERE clause.
     *
     * @param predicate the predicate (condition) for filtering
     */
    public JpqlWhereClause {
        if (predicate == null) {
            throw new IllegalArgumentException("predicate cannot be null");
        }
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitWhereClause(this, parameter);
    }

    /**
     * Creates a WHERE clause with a simple predicate.
     *
     * @param predicate the predicate
     * @return a new WHERE clause
     */
    public static JpqlWhereClause of(JpqlPredicate predicate) {
        return new JpqlWhereClause(predicate);
    }
}
