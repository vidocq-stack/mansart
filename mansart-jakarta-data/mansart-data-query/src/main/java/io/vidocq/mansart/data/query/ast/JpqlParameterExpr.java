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

import java.util.Optional;

/**
 * JPQL parameter expression node.
 *
 * <p>Represents a named or positional parameter in JPQL.</p>
 *
 * <p>Example JPQL:</p>
 * <pre>
 * :price          (named parameter)
 * ?1              (positional parameter)
 * </pre>
 *
 * @param name the parameter name (for named parameters)
 * @param position the parameter position (for positional parameters, 1-based)
 * @since 0.3.0-SNAPSHOT
 */
public record JpqlParameterExpr(Optional<String> name, Optional<Integer> position) implements JpqlExpr {

    /**
     * Creates a new parameter expression.
     *
     * @param name the parameter name (for named parameters)
     * @param position the parameter position (for positional parameters, 1-based)
     */
    public JpqlParameterExpr {
        // Exactly one of name or position must be present
        if (name.isPresent() && position.isPresent()) {
            throw new IllegalArgumentException("Parameter cannot be both named and positional");
        }
        if (name.isEmpty() && position.isEmpty()) {
            throw new IllegalArgumentException("Parameter must be either named or positional");
        }
        if (position.isPresent() && position.get() < 1) {
            throw new IllegalArgumentException("Position must be >= 1");
        }
    }

    @Override
    public <R, P> R accept(JpqlVisitor<R, P> visitor, P parameter) {
        return visitor.visitParameterExpr(this, parameter);
    }

    /**
     * Returns true if this is a named parameter.
     *
     * @return true if named parameter
     */
    public boolean isNamed() {
        return name().isPresent();
    }

    /**
     * Returns true if this is a positional parameter.
     *
     * @return true if positional parameter
     */
    public boolean isPositional() {
        return position().isPresent();
    }

    /**
     * Returns the parameter name, or throws if not a named parameter.
     *
     * @return the parameter name
     * @throws IllegalStateException if not a named parameter
     */
    public String getName() {
        return name.orElseThrow(() -> new IllegalStateException("Not a named parameter"));
    }

    /**
     * Returns the parameter position (1-based), or throws if not a positional parameter.
     *
     * @return the parameter position
     * @throws IllegalStateException if not a positional parameter
     */
    public int getPosition() {
        return position.orElseThrow(() -> new IllegalStateException("Not a positional parameter"));
    }

    /**
     * Creates a named parameter expression.
     *
     * @param name the parameter name
     * @return a new named parameter expression
     */
    public static JpqlParameterExpr named(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Parameter name cannot be null or blank");
        }
        return new JpqlParameterExpr(Optional.of(name), Optional.empty());
    }

    /**
     * Creates a positional parameter expression.
     *
     * @param position the parameter position (1-based)
     * @return a new positional parameter expression
     */
    public static JpqlParameterExpr positional(int position) {
        if (position < 1) {
            throw new IllegalArgumentException("Position must be >= 1");
        }
        return new JpqlParameterExpr(Optional.empty(), Optional.of(position));
    }

    @Override
    public String toString() {
        if (isNamed()) {
            return ":" + name();
        } else {
            return "?" + position();
        }
    }
}
