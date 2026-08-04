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
 * JPQL SELECT clause node.
 *
 * <p>Represents the SELECT clause of a JPQL query, containing the projections.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * SELECT DISTINCT b.title, b.author.name
 * SELECT b
 * SELECT COUNT(b), SUM(b.price)
 * </pre>
 *
 * @param distinct true if DISTINCT keyword is present
 * @param expressions the list of expressions to select
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlSelectClause(boolean distinct, List<JpqlExpr> expressions) implements JpqlClause {

    /**
     * Creates a new SELECT clause.
     *
     * @param distinct true if DISTINCT keyword is present
     * @param expressions the list of expressions to select
     */
    public JpqlSelectClause {
        if (expressions == null || expressions.isEmpty()) {
            throw new IllegalArgumentException("SELECT clause must have at least one expression");
        }
        expressions = List.copyOf(expressions);
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitSelectClause(this, parameter);
    }

    /**
     * Creates a SELECT clause with a single expression.
     *
     * @param expression the expression to select
     * @return a new SELECT clause
     */
    public static JpqlSelectClause of(JpqlExpr expression) {
        return new JpqlSelectClause(false, List.of(expression));
    }

    /**
     * Creates a DISTINCT SELECT clause with a single expression.
     *
     * @param expression the expression to select
     * @return a new DISTINCT SELECT clause
     */
    public static JpqlSelectClause distinct(JpqlExpr expression) {
        return new JpqlSelectClause(true, List.of(expression));
    }

    /**
     * Creates a SELECT clause with multiple expressions.
     *
     * @param expressions the expressions to select
     * @return a new SELECT clause
     */
    public static JpqlSelectClause of(List<JpqlExpr> expressions) {
        return new JpqlSelectClause(false, expressions);
    }

    /**
     * Returns true if this SELECT clause selects a single entity (not projections).
     *
     * @return true if selecting a single entity
     */
    public boolean isSingleEntitySelect() {
        return expressions().size() == 1 && expressions().get(0) instanceof JpqlPathExpr path && path.isSimple();
    }

    /**
     * Returns true if this SELECT clause contains aggregate functions.
     *
     * @return true if there are aggregate functions
     */
    public boolean hasAggregates() {
        return expressions().stream().anyMatch(e -> e instanceof JpqlFunctionExpr func && func.isAggregate());
    }
}
