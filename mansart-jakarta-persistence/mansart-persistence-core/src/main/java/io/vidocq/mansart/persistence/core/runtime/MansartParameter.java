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
package io.vidocq.mansart.persistence.core.runtime;

import jakarta.persistence.Parameter;

/**
 * Mansart implementation of {@link Parameter} interface.
 *
 * <p>Represents a query parameter (either named or positional).
 */
public class MansartParameter<T> implements Parameter<T> {

    private final String name;
    private final int position;
    private final Class<T> type;

    /**
     * Creates a named parameter.
     *
     * @param name the parameter name
     * @param type the parameter type
     */
    public MansartParameter(String name, Class<T> type) {
        this(name, -1, type);
    }

    /**
     * Creates a positional parameter.
     *
     * @param position the parameter position (1-based)
     * @param type the parameter type
     */
    public MansartParameter(int position, Class<T> type) {
        this(null, position, type);
    }

    /**
     * Creates a parameter with both name and position.
     *
     * @param name the parameter name (may be null for positional parameters)
     * @param position the parameter position (1-based, or -1 for named parameters)
     * @param type the parameter type
     */
    public MansartParameter(String name, int position, Class<T> type) {
        this.name = name;
        this.position = position;
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Integer getPosition() {
        return position >= 0 ? position : null;
    }

    @Override
    public Class<T> getParameterType() {
        return type;
    }

    @Override
    public String toString() {
        if (name != null) {
            return "MansartParameter[name=" + name + ", type=" + type + "]";
        } else {
            return "MansartParameter[position=" + position + ", type=" + type + "]";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MansartParameter<?> that = (MansartParameter<?>) o;
        if (position != that.position) return false;
        if (name != null) {
            return name.equals(that.name) && type.equals(that.type);
        } else {
            return position == that.position && type.equals(that.type);
        }
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : position;
        result = 31 * result + type.hashCode();
        return result;
    }
}
