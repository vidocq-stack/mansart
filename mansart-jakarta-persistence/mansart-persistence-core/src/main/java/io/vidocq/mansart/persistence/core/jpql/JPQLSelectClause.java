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
 * Parsed representation of the JPQL {@code SELECT <expression>} clause.
 *
 * <p>Milestone: M7-13 — supports single entity projection (e.g. {@code SELECT p FROM Person p}).
 */
public final class JPQLSelectClause {

    private final JPQLExpression expression;
    private final List<JPQLExpression> expressions;

    /**
     * Creates a select clause with a single expression.
     *
     * @param expression the select expression (entity reference or path expression)
     */
    public JPQLSelectClause(JPQLExpression expression) {
        this.expression = Objects.requireNonNull(expression, "expression must not be null");
        this.expressions = List.of(expression);
    }

    /**
     * Creates an N-ary select clause.
     *
     * @param expressions the list of select expressions
     */
    public JPQLSelectClause(List<JPQLExpression> expressions) {
        Objects.requireNonNull(expressions, "expressions must not be null");
        if (expressions.isEmpty()) {
            throw new IllegalArgumentException("expressions must not be empty");
        }
        this.expression = expressions.get(0);
        this.expressions = expressions;
    }

    public JPQLExpression expression() { return expression; }
    public List<JPQLExpression> expressions() { return expressions; }

    @Override
    public String toString() {
        return "JPQLSelectClause{" +
                "expression=" + expression +
                ", expressions=" + expressions +
                '}';
    }
}
