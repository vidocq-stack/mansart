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

/**
 * JPQL OR predicate node.
 *
 * <p>Represents a disjunction (logical OR) of multiple predicates.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * b.category = 'Fiction' OR b.category = 'Sci-Fi'
 * b.price < 10 OR b.price > 100
 * </pre>
 *
 * @param predicates the list of predicates to OR together
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlOrPredicate(List<JpqlPredicate> predicates) implements JpqlPredicate {

    /**
     * Creates a new OR predicate.
     *
     * @param predicates the list of predicates to OR together
     */
    public JpqlOrPredicate {
        if (predicates == null || predicates.isEmpty()) {
            throw new IllegalArgumentException("OR predicate must have at least one predicate");
        }
        if (predicates.size() == 1) {
            throw new IllegalArgumentException("OR predicate must have at least two predicates");
        }
        predicates = List.copyOf(predicates);
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitOrPredicate(this, parameter);
    }

    /**
     * Creates an OR predicate from two predicates.
     *
     * @param left the left predicate
     * @param right the right predicate
     * @return a new OR predicate
     */
    public static JpqlOrPredicate of(JpqlPredicate left, JpqlPredicate right) {
        return new JpqlOrPredicate(List.of(left, right));
    }

    /**
     * Creates an OR predicate from a list of predicates.
     *
     * @param predicates the predicates to OR together
     * @return a new OR predicate
     */
    public static JpqlOrPredicate of(List<JpqlPredicate> predicates) {
        if (predicates.size() == 1) {
            return (JpqlOrPredicate) predicates.get(0);
        }
        return new JpqlOrPredicate(predicates);
    }

    /**
     * Returns the leftmost predicate.
     *
     * @return the first predicate
     */
    public JpqlPredicate left() {
        return predicates().get(0);
    }

    /**
     * Returns the rightmost predicate.
     *
     * @return the last predicate
     */
    public JpqlPredicate right() {
        return predicates().get(predicates().size() - 1);
    }
}
