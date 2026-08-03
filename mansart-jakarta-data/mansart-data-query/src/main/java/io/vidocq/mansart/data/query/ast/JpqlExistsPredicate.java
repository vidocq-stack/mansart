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
 * JPQL EXISTS predicate node.
 *
 * <p>Represents an EXISTS or NOT EXISTS predicate with a subquery.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * EXISTS (SELECT 1 FROM Order o WHERE o.book = b)
 * NOT EXISTS (SELECT 1 FROM Order o WHERE o.book = b)
 * </pre>
 *
 * @param not true if this is a NOT EXISTS predicate
 * @param subquery the subquery
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlExistsPredicate(boolean not, JpqlSelectStmt subquery) implements JpqlPredicate {

    /**
     * Creates a new EXISTS predicate.
     *
     * @param not true if this is a NOT EXISTS predicate
     * @param subquery the subquery
     */
    public JpqlExistsPredicate {
        if (subquery == null) {
            throw new IllegalArgumentException("subquery cannot be null");
        }
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitExistsPredicate(this, parameter);
    }

    /**
     * Returns true if this is a NOT EXISTS predicate.
     *
     * @return true if NOT EXISTS
     */
    public boolean isNotExists() {
        return not();
    }

    /**
     * Returns true if this is an EXISTS predicate (not NOT EXISTS).
     *
     * @return true if EXISTS
     */
    public boolean isExists() {
        return !not();
    }

    /**
     * Creates an EXISTS predicate.
     *
     * @param subquery the subquery
     * @return a new EXISTS predicate
     */
    public static JpqlExistsPredicate exists(JpqlSelectStmt subquery) {
        return new JpqlExistsPredicate(false, subquery);
    }

    /**
     * Creates a NOT EXISTS predicate.
     *
     * @param subquery the subquery
     * @return a new NOT EXISTS predicate
     */
    public static JpqlExistsPredicate notExists(JpqlSelectStmt subquery) {
        return new JpqlExistsPredicate(true, subquery);
    }
}
