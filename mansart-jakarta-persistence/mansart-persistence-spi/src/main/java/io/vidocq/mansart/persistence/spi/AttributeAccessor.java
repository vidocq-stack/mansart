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
package io.vidocq.mansart.persistence.spi;

/**
 * Compile-time-generated accessor for a single persistent attribute.
 *
 * <p>Holds a pre-resolved {@link java.lang.invoke.MethodHandle} (or
 * {@link java.lang.invoke.VarHandle}) obtained at compile time by the Mansart
 * APT processor.  The runtime never uses {@code java.lang.reflect.Field}
 * on entity state (DEBT-05).
 *
 * <p>Implemented by the Mansart APT processor.
 */
public interface AttributeAccessor {

    /**
     * Returns the attribute name.
     *
     * @return the attribute name
     */
    String name();

    /**
     * Returns the Java type of this attribute.
     *
     * @return the attribute type
     */
    Class<?> type();

    /**
     * Reads the attribute value from the entity instance.
     *
     * @param entity the entity instance
     * @return the attribute value
     */
    Object read(Object entity);

    /**
     * Writes a value to the attribute on the entity instance.
     *
     * @param entity the entity instance
     * @param value the new value
     */
    void write(Object entity, Object value);
}
