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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime registry for APT-generated {@link EntityMetadata} implementations.
 *
 * <p>Each generated metadata class (e.g. {@code BookMetadata}) registers itself
 * in a static initializer block:
 * <pre>{@code
 * static {
 *     EntityMetadataRegistry.register(Book.class, new BookMetadata());
 * }
 * }</pre>
 *
 * <p>The persistence core looks up metadata at runtime via
 * {@link #getMetadata(Class)}.
 *
 * <p>(DEBT-05: bridges generated metadata to the runtime.)
 */
public final class EntityMetadataRegistry {

    private static final Map<Class<?>, EntityMetadata> REGISTRY = new ConcurrentHashMap<>();

    private EntityMetadataRegistry() {}

    /**
     * Registers an entity metadata instance.
     *
     * @param entityClass the entity class
     * @param metadata the generated metadata instance
     */
    public static void register(Class<?> entityClass, EntityMetadata metadata) {
        REGISTRY.put(entityClass, metadata);
    }

    /**
     * Looks up entity metadata for the given class.
     *
     * @param entityClass the entity class
     * @return the metadata, or {@code null} if not registered
     */
    public static EntityMetadata getMetadata(Class<?> entityClass) {
        return REGISTRY.get(entityClass);
    }

    /**
     * Returns all registered entity classes.
     */
    public static java.util.Set<Class<?>> registeredEntityClasses() {
        return REGISTRY.keySet();
    }
}
