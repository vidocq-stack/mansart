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

/**
 * Base interface for JPQL clause nodes.
 *
 * <p>A clause is a major component of a JPQL statement.</p>
 *
 * <p>Supported clause types:</p>
 * <ul>
 *   <li>{@link JpqlSelectClause} - SELECT clause (projections)</li>
 *   <li>{@link JpqlFromClause} - FROM clause (entity declarations and joins)</li>
 *   <li>{@link JpqlWhereClause} - WHERE clause (filter conditions)</li>
 *   <li>{@link JpqlGroupByClause} - GROUP BY clause</li>
 *   <li>{@link JpqlHavingClause} - HAVING clause</li>
 *   <li>{@link JpqlOrderByClause} - ORDER BY clause</li>
 * </ul>
 *
 * @since 0.3.0-SNAPSHOT
 */
public sealed interface JpqlClause extends JpqlNode 
    permits JpqlSelectClause, JpqlFromClause, JpqlWhereClause, 
            JpqlGroupByClause, JpqlHavingClause, JpqlOrderByClause {
}
