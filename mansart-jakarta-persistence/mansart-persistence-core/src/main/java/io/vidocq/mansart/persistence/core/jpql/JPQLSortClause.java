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
 * Parsed representation of the JPQL {@code ORDER BY} clause.
 *
 * <p>Milestone: M7-13 — supports optional {@code ASC}/{@code DESC} ordering on one or more paths.
 */
public final class JPQLSortClause {

    /** Sentinel for a query with no {@code ORDER BY} clause. */
    public static final JPQLSortClause NONE = new JPQLSortClause(List.of());

    private final List<JPQLOrder> orders;

    JPQLSortClause(List<JPQLOrder> orders) {
        this.orders = Objects.requireNonNull(orders, "orders must not be null");
    }

    public List<JPQLOrder> orders() { return orders; }

    /** Returns whether this is the {@link #NONE} sort clause. */
    public boolean isEmpty() { return orders.isEmpty(); }

    @Override
    public String toString() {
        return "JPQLSortClause{" + orders + '}';
    }

    /** A single sort entry with direction. */
    public static final class JPQLOrder {
        private final JPQLExpression expression;
        private final SortDirection direction;

        public JPQLOrder(JPQLExpression expression, SortDirection direction) {
            this.expression = Objects.requireNonNull(expression, "expression must not be null");
            this.direction = Objects.requireNonNull(direction, "direction must not be null");
        }

        public JPQLExpression expression() { return expression; }
        public SortDirection direction() { return direction; }

        @Override
        public String toString() {
            return expression + " " + direction;
        }
    }

    /** Sort direction: ascending or descending. */
    public enum SortDirection { ASC, DESC }
}
