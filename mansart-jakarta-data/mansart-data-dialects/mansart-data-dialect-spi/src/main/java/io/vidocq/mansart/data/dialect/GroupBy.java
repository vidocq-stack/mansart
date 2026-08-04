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
package io.vidocq.mansart.data.dialect;

import java.util.List;

/**
 * Backend-neutral GROUP BY clause representation.
 * 
 * <p>Represents the expressions to group query results by. Used with aggregation functions
 * and HAVING clauses.</p>
 * 
 * <p>M7 — GROUP BY support for JPQL queries.</p>
 */
public record GroupBy(List<Attribute<?, ?>> expressions) {

    public static final GroupBy NONE = new GroupBy(List.of());

    public GroupBy { 
        expressions = List.copyOf(expressions); 
    }

    public boolean isEmpty() { 
        return expressions.isEmpty(); 
    }

    /**
     * Creates a GroupBy with a single expression.
     */
    public static GroupBy of(Attribute<?, ?> expr) {
        return new GroupBy(List.of(expr));
    }

    /**
     * Creates a GroupBy with multiple expressions.
     */
    public static GroupBy of(List<Attribute<?, ?>> expressions) {
        return new GroupBy(expressions);
    }
}
