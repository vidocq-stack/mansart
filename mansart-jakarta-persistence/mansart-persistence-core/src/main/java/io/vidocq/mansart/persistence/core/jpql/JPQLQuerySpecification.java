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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.jpql;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Parsed representation of the JPQL {@code SELECT <projection> FROM <fromClause>} body.
 *
 * <p>Milestone: M7-13 — supports single-entity {@code FROM} with alias and optional {@code WHERE}.
 * <p>Milestone: M8-1 — added GROUP BY and HAVING support.
 */
public final class JPQLQuerySpecification {

    private final JPQLSelectClause selectClause;
    private final JPQLFromClause fromClause;
    private final JPQLWhereClause whereClause;
    private final JPQLGroupByClause groupByClause;
    private final JPQLHavingClause havingClause;

    JPQLQuerySpecification(JPQLSelectClause selectClause, JPQLFromClause fromClause, 
                             JPQLWhereClause whereClause, JPQLGroupByClause groupByClause, 
                             JPQLHavingClause havingClause) {
        this.selectClause = Objects.requireNonNull(selectClause, "selectClause must not be null");
        this.fromClause = Objects.requireNonNull(fromClause, "fromClause must not be null");
        this.whereClause = whereClause;
        this.groupByClause = groupByClause;
        this.havingClause = havingClause;
    }

    public JPQLSelectClause selectClause() { return selectClause; }
    public JPQLFromClause fromClause()  { return fromClause; }
    public JPQLWhereClause whereClause() { return whereClause; }
    public JPQLGroupByClause groupByClause() { return groupByClause; }
    public JPQLHavingClause havingClause() { return havingClause; }

    @Override
    public String toString() {
        return "JPQLQuerySpecification{" +
                "select=" + selectClause +
                ", from=" + fromClause +
                ", where=" + whereClause +
                ", groupBy=" + groupByClause +
                ", having=" + havingClause +
                '}';
    }
}
