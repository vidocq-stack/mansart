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
 * Enumeration of JPQL join types.
 *
 * <p>Represents the different types of joins supported in JPQL.</p>
 *
 * @since 0.3.0-SNAPSHOT
 */
public enum JoinType {
    /**
     * INNER JOIN - returns only rows where the join condition is met.
     */
    INNER("INNER JOIN"),
    
    /**
     * LEFT JOIN - returns all rows from the left table, and matched rows from the right table.
     */
    LEFT("LEFT JOIN"),
    
    /**
     * RIGHT JOIN - returns all rows from the right table, and matched rows from the left table.
     */
    RIGHT("RIGHT JOIN"),
    
    /**
     * CROSS JOIN - returns the Cartesian product of the tables.
     */
    CROSS("CROSS JOIN"),
    
    /**
     * LEFT JOIN FETCH - eager loading of the association.
     */
    LEFT_FETCH("LEFT JOIN FETCH"),
    
    /**
     * INNER JOIN FETCH - eager loading of the association (with inner join semantics).
     */
    INNER_FETCH("INNER JOIN FETCH");

    private final String jpqlKeyword;

    JoinType(String jpqlKeyword) {
        this.jpqlKeyword = jpqlKeyword;
    }

    /**
     * Returns the JPQL keyword for this join type.
     *
     * @return the JPQL keyword
     */
    public String jpqlKeyword() {
        return jpqlKeyword;
    }

    /**
     * Returns true if this is a FETCH join.
     *
     * @return true if this is a fetch join
     */
    public boolean isFetch() {
        return this == LEFT_FETCH || this == INNER_FETCH;
    }

    /**
     * Returns true if this is an OUTER join (LEFT or RIGHT).
     *
     * @return true if this is an outer join
     */
    public boolean isOuter() {
        return this == LEFT || this == RIGHT || this == LEFT_FETCH;
    }

    @Override
    public String toString() {
        return jpqlKeyword;
    }
}
