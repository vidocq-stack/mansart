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
/**
 * SPI interface for bytecode enhancement of JPA entities.
 * Implementations enhance entity classes at compile-time to add:
 * - Dirty tracking
 * - Lazy loading support
 * - Proxy generation for relationships
 */
package io.vidocq.mansart.persistence.spi;

import java.lang.invoke.MethodHandles;

/**
 * Interface for enhancing entity classes with JPA-specific bytecode.
 * This interface is used by the annotation processor to enhance entity classes
 * with additional functionality required by the JPA implementation.
 */
public interface PersistenceEnhancer {

    /**
     * Enhances the given entity class bytecode.
     *
     * @param entityClass the entity class to enhance
     * @param classBytes the original bytecode of the class
     * @param lookup the MethodHandles.Lookup for defining hidden classes
     * @return the enhanced bytecode, or null if no enhancement was performed
     */
    byte[] enhance(Class<?> entityClass, byte[] classBytes, MethodHandles.Lookup lookup);

    /**
     * Creates a lazy loading proxy for the given entity class.
     *
     * @param entityClass the entity class to create a proxy for
     * @param lookup the MethodHandles.Lookup for defining the proxy class
     * @return the proxy class
     */
    Class<?> createLazyProxy(Class<?> entityClass, MethodHandles.Lookup lookup);

    /**
     * Checks if the given class is already enhanced.
     *
     * @param clazz the class to check
     * @return true if the class is enhanced, false otherwise
     */
    boolean isEnhanced(Class<?> clazz);
}
