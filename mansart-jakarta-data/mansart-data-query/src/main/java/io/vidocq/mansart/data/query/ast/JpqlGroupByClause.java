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

import java.util.List;

/**
 * JPQL GROUP BY clause node.
 *
 * <p>Represents the GROUP BY clause of a JPQL query.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * GROUP BY b.author, b.category
 * </pre>
 *
 * @param expressions the expressions to group by
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlGroupByClause(List<JpqlExpr> expressions) implements JpqlClause {

    /**
     * Creates a new GROUP BY clause.
     *
     * @param expressions the expressions to group by
     */
    public JpqlGroupByClause {
        if (expressions == null || expressions.isEmpty()) {
            throw new IllegalArgumentException("GROUP BY clause must have at least one expression");
        }
        expressions = List.copyOf(expressions);
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitGroupByClause(this, parameter);
    }

    /**
     * Creates a GROUP BY clause with a single expression.
     *
     * @param expression the expression to group by
     * @return a new GROUP BY clause
     */
    public static JpqlGroupByClause of(JpqlExpr expression) {
        return new JpqlGroupByClause(List.of(expression));
    }

    /**
     * Creates a GROUP BY clause with multiple expressions.
     *
     * @param expressions the expressions to group by
     * @return a new GROUP BY clause
     */
    public static JpqlGroupByClause of(List<JpqlExpr> expressions) {
        return new JpqlGroupByClause(expressions);
    }
}
