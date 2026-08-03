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
 * JPQL CASE expression node.
 *
 * <p>Represents a CASE expression (conditional expression) in JPQL.</p>
 *
 * <p>There are two forms of CASE:</p>
 * <pre>
 * -- Simple CASE
 * CASE b.category
 *     WHEN 'Fiction' THEN 'F'
 *     WHEN 'Sci-Fi' THEN 'S'
 *     ELSE 'Other'
 * END
 *
 * -- Searched CASE
 * CASE
 *     WHEN b.price > 100 THEN 'Expensive'
 *     WHEN b.price > 50 THEN 'Moderate'
 *     ELSE 'Cheap'
 * END
 * </pre>
 *
 * @param expression the expression to compare (empty for searched CASE)
 * @param whenThenList the list of WHEN-THEN pairs
 * @param elseExpr the ELSE expression (empty if not present)
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlCaseExpr(
    Optional<JpqlExpr> expression,
    List<JpqlWhenThen> whenThenList,
    Optional<JpqlExpr> elseExpr
) implements JpqlExpr {

    /**
     * Creates a new CASE expression.
     *
     * @param expression the expression to compare (empty for searched CASE)
     * @param whenThenList the list of WHEN-THEN pairs
     * @param elseExpr the ELSE expression (empty if not present)
     */
    public JpqlCaseExpr {
        if (whenThenList == null || whenThenList.isEmpty()) {
            throw new IllegalArgumentException("CASE expression must have at least one WHEN-THEN pair");
        }
        whenThenList = List.copyOf(whenThenList);
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitCaseExpr(this, parameter);
    }

    /**
     * Returns true if this is a simple CASE (with an expression).
     *
     * @return true if simple CASE
     */
    public boolean isSimpleCase() {
        return expression().isPresent();
    }

    /**
     * Returns true if this is a searched CASE (without an expression).
     *
     * @return true if searched CASE
     */
    public boolean isSearchedCase() {
        return expression().isEmpty();
    }

    /**
     * Returns true if this CASE has an ELSE clause.
     *
     * @return true if ELSE clause is present
     */
    public boolean hasElse() {
        return elseExpr().isPresent();
    }

    /**
     * WHEN-THEN pair.
     */
    public record JpqlWhenThen(JpqlPredicate when, JpqlExpr then) {
        /**
         * Creates a new WHEN-THEN pair.
         *
         * @param when the WHEN condition
         * @param then the THEN expression
         */
        public JpqlWhenThen {
            if (when == null) {
                throw new IllegalArgumentException("WHEN condition cannot be null");
            }
            if (then == null) {
                throw new IllegalArgumentException("THEN expression cannot be null");
            }
        }
    }
}
