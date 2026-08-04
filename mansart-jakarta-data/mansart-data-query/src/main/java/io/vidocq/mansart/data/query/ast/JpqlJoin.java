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

import java.util.Optional;

/**
 * JPQL JOIN specification node.
 *
 * <p>Represents a join from one entity to another in the FROM clause.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * JOIN b.author a
 * LEFT JOIN b.author a
 * JOIN FETCH b.items
 * LEFT JOIN b.author a ON a.active = true
 * </pre>
 *
 * @param path the path expression from the parent entity to the joined entity
 * @param identifier the identifier (alias) for the joined entity
 * @param type the type of join
 * @param onCondition optional ON condition for the join
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlJoin(JpqlPathExpr path, String identifier, JoinType type, Optional<JpqlPredicate> onCondition) {

    /**
     * Creates a new JOIN specification.
     *
     * @param path the path expression
     * @param identifier the identifier (alias)
     * @param type the type of join
     * @param onCondition optional ON condition
     */
    public JpqlJoin {
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("identifier cannot be null or blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
        }
    }

    /**
     * Creates a simple JOIN (INNER JOIN) without an ON condition.
     *
     * @param path the path expression
     * @param identifier the identifier
     * @return a new JOIN specification
     */
    public static JpqlJoin innerJoin(JpqlPathExpr path, String identifier) {
        return new JpqlJoin(path, identifier, JoinType.INNER, Optional.empty());
    }

    /**
     * Creates a LEFT JOIN without an ON condition.
     *
     * @param path the path expression
     * @param identifier the identifier
     * @return a new LEFT JOIN specification
     */
    public static JpqlJoin leftJoin(JpqlPathExpr path, String identifier) {
        return new JpqlJoin(path, identifier, JoinType.LEFT, Optional.empty());
    }

    /**
     * Creates a LEFT JOIN FETCH (for eager loading).
     *
     * @param path the path expression
     * @return a new LEFT JOIN FETCH specification
     */
    public static JpqlJoin leftFetchJoin(JpqlPathExpr path) {
        return new JpqlJoin(path, path.path(), JoinType.LEFT_FETCH, Optional.empty());
    }

    /**
     * Returns true if this join has an ON condition.
     *
     * @return true if ON condition is present
     */
    public boolean hasOnCondition() {
        return onCondition.isPresent();
    }

    /**
     * Returns the ON condition, or throws if not present.
     *
     * @return the ON condition
     * @throws IllegalStateException if no ON condition
     */
    public JpqlPredicate getOnCondition() {
        return onCondition.orElseThrow(() -> new IllegalStateException("No ON condition for this join"));
    }
}
