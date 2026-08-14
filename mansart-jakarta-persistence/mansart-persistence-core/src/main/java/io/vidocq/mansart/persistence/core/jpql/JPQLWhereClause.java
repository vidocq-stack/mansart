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

import java.util.List;
import java.util.Objects;

/**
 * Parsed representation of the JPQL {@code WHERE <boolean_expression>} clause.
 *
 * <p>Milestone: M7-13 — supports simple binary comparisons, {@code IS NULL},
 * {@code IS NOT NULL}, {@code LIKE}, and boolean operators {@code AND}, {@code OR}.
 */
public final class JPQLWhereClause {

    private final JPQLExpression predicate;

    JPQLWhereClause(JPQLExpression predicate) {
        this.predicate = predicate;
    }

    public JPQLExpression predicate() { return predicate; }

    @Override
    public String toString() {
        return "JPQLWhereClause{predicate=" + predicate + '}';
    }

    public static JPQLWhereClause none() {
        return new JPQLWhereClause(null);
    }

    /**
     * Returns whether this clause has no WHERE portion (i.e. selects all rows).
     *
     * @return {@code true} if no WHERE clause was present
     */
    public boolean isNone() {
        return predicate == null;
    }
}
