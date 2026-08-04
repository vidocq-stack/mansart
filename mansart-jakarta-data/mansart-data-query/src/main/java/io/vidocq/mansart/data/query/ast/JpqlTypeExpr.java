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

/**
 * JPQL TYPE expression node.
 *
 * <p>Represents a TYPE expression in JPQL, which returns the type of an entity.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * TYPE(b) = Book
 * TYPE(b) IN (Book, Magazine)
 * </pre>
 *
 * @param expression the expression to get the type of
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlTypeExpr(JpqlExpr expression) implements JpqlExpr {

    /**
     * Creates a new TYPE expression.
     *
     * @param expression the expression to get the type of
     */
    public JpqlTypeExpr {
        if (expression == null) {
            throw new IllegalArgumentException("expression cannot be null");
        }
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitTypeExpr(this, parameter);
    }

    /**
     * Creates a TYPE expression.
     *
     * @param expression the expression
     * @return a new TYPE expression
     */
    public static JpqlTypeExpr of(JpqlExpr expression) {
        return new JpqlTypeExpr(expression);
    }
}
