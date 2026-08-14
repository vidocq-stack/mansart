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

import java.util.Objects;

/**
 * Abstract syntax tree node for a JPQL {@code SELECT} query.
 *
 * <p>Milestone: M7-13 — supports {@code SELECT <entity> FROM <Entity> <alias> WHERE <predicate>}.
 */
public final class JPQLQuery<V> {

    private final JPQLQuerySpecification specification;
    private final boolean isTyped;
    private final Class<V> resultClass;
    private final JPQLSortClause sortClause;
    private final int maxResults;
    private final int firstResult;

    /**
     * Creates a new {@code JPQLQuery} for typed entity queries.
     *
     * @param specification the parsed query specification
     * @param resultClass   the typed result class (usually the entity class)
     */
    public JPQLQuery(JPQLQuerySpecification specification, Class<V> resultClass) {
        this(specification, true, Objects.requireNonNull(resultClass, "resultClass must not be null"),
                JPQLSortClause.NONE, 0, 0);
    }

    /**
     * Creates a new {@code JPQLQuery} with pagination options.
     */
    private JPQLQuery(JPQLQuerySpecification specification, boolean isTyped, Class<V> resultClass,
                      JPQLSortClause sortClause, int maxResults, int firstResult) {
        this.specification = Objects.requireNonNull(specification, "specification must not be null");
        this.isTyped = isTyped;
        this.resultClass = resultClass;
        this.sortClause = sortClause;
        this.maxResults = maxResults;
        this.firstResult = firstResult;
    }

    public JPQLQuerySpecification specification() { return specification; }
    public boolean isTyped() { return isTyped; }
    public Class<V> resultClass() { return resultClass; }
    public JPQLSortClause sortClause() { return sortClause; }

    /**
     * Returns a new query with the top N results restriction applied.
     *
     * @param maxResults the maximum number of results
     * @return a new {@code JPQLQuery} with maxResults set
     */
    public JPQLQuery<V> withMaxResults(int maxResults) {
        Objects.checkIndex(maxResults, Integer.MAX_VALUE);
        return new JPQLQuery<>(specification, isTyped, resultClass, sortClause, maxResults, firstResult);
    }

    /**
     * Returns a new query with the first result offset applied.
     *
     * @param firstResult the index of the first result (0-based)
     * @return a new {@code JPQLQuery} with firstResult set
     */
    public JPQLQuery<V> withFirstResult(int firstResult) {
        Objects.checkIndex(firstResult, Integer.MAX_VALUE);
        return new JPQLQuery<>(specification, isTyped, resultClass, sortClause, maxResults, firstResult);
    }

    @Override
    public String toString() {
        return "JPQLQuery{" +
                "specification=" + specification +
                ", isTyped=" + isTyped +
                ", resultClass=" + resultClass +
                ", sortClause=" + sortClause +
                ", maxResults=" + maxResults +
                ", firstResult=" + firstResult +
                '}';
    }
}
