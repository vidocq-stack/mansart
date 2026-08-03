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

import java.util.List;
import java.util.Optional;

/**
 * JPQL SELECT statement node.
 *
 * <p>Represents a complete SELECT query in JPQL.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * SELECT b.title, b.author.name FROM Book b WHERE b.price > 100 ORDER BY b.title
 * </pre>
 *
 * <p>This corresponds to:</p>
 * <pre>
 * JpqlSelectStmt(
 *     selectClause = JpqlSelectClause([JpqlPathExpr("b.title"), JpqlPathExpr("b.author.name")]),
 *     fromClause = JpqlFromClause([JpqlFromItem("Book", "b", [])]),
 *     whereClause = Optional[JpqlWhereClause(JpqlBinaryExpr(>, JpqlPathExpr("b.price"), JpqlLiteralExpr(100)))],
 *     groupByClause = Optional.empty(),
 *     havingClause = Optional.empty(),
 *     orderByClause = Optional[JpqlOrderByClause([JpqlOrderByItem(JpqlPathExpr("b.title"), ASC)])]
 * )
 * </pre>
 *
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlSelectStmt(
    JpqlSelectClause selectClause,
    JpqlFromClause fromClause,
    Optional<JpqlWhereClause> whereClause,
    Optional<JpqlGroupByClause> groupByClause,
    Optional<JpqlHavingClause> havingClause,
    Optional<JpqlOrderByClause> orderByClause
) implements JpqlStmt {

    /**
     * Creates a new SELECT statement with the given clauses.
     *
     * @param selectClause the SELECT clause (projections)
     * @param fromClause the FROM clause (entity declarations)
     * @param whereClause the WHERE clause (filter), or empty
     * @param groupByClause the GROUP BY clause, or empty
     * @param havingClause the HAVING clause, or empty
     * @param orderByClause the ORDER BY clause, or empty
     */
    public JpqlSelectStmt {
        // Validate required clauses
        if (selectClause == null) {
            throw new IllegalArgumentException("selectClause cannot be null");
        }
        if (fromClause == null) {
            throw new IllegalArgumentException("fromClause cannot be null");
        }
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitSelect(this, parameter);
    }

    /**
     * Returns true if this is a DISTINCT select.
     *
     * @return true if DISTINCT keyword is present
     */
    public boolean isDistinct() {
        return selectClause().distinct();
    }

    /**
     * Returns the list of expressions in the SELECT clause.
     *
     * @return the select expressions
     */
    public List<JpqlExpr> selectExpressions() {
        return selectClause().expressions();
    }

    /**
     * Returns the from items (entities and joins).
     *
     * @return the from items
     */
    public List<JpqlFromItem> fromItems() {
        return fromClause().items();
    }

    /**
     * Returns the root entity declaration (the first from item).
     *
     * @return the root entity
     */
    public JpqlFromItem rootEntity() {
        return fromItems().get(0);
    }

    /**
     * Returns true if this query has a WHERE clause.
     *
     * @return true if WHERE clause is present
     */
    public boolean hasWhere() {
        return whereClause().isPresent();
    }

    /**
     * Returns true if this query has a GROUP BY clause.
     *
     * @return true if GROUP BY clause is present
     */
    public boolean hasGroupBy() {
        return groupByClause().isPresent();
    }

    /**
     * Returns true if this query has a HAVING clause.
     *
     * @return true if HAVING clause is present
     */
    public boolean hasHaving() {
        return havingClause().isPresent();
    }

    /**
     * Returns true if this query has an ORDER BY clause.
     *
     * @return true if ORDER BY clause is present
     */
    public boolean hasOrderBy() {
        return orderByClause().isPresent();
    }
}
