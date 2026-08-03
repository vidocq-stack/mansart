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
 * JPQL NOT predicate node.
 *
 * <p>Represents a negation (logical NOT) of a predicate.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * NOT b.active = true
 * NOT (b.price > 100)
 * </pre>
 *
 * @param predicate the predicate to negate
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlNotPredicate(JpqlPredicate predicate) implements JpqlPredicate {

    /**
     * Creates a new NOT predicate.
     *
     * @param predicate the predicate to negate
     */
    public JpqlNotPredicate {
        if (predicate == null) {
            throw new IllegalArgumentException("predicate cannot be null");
        }
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitNotPredicate(this, parameter);
    }

    /**
     * Creates a NOT predicate.
     *
     * @param predicate the predicate to negate
     * @return a new NOT predicate
     */
    public static JpqlNotPredicate not(JpqlPredicate predicate) {
        return new JpqlNotPredicate(predicate);
    }

    /**
     * Returns the negated predicate.
     *
     * @return the predicate being negated
     */
    public JpqlPredicate operand() {
        return predicate();
    }
}
