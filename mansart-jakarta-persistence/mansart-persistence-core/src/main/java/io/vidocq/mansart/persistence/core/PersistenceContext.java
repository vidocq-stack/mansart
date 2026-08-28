/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Tracks managed entity instances for a single {@code EntityManager}.
 *
 * <p>Serves as the identity map: given an entity instance, returns whether
 * it is managed by this context. Future iterations add class+ID lookup.</p>
 */
final class PersistenceContext {

    /** Entity instance → entity class, keyed by identity. */
    private final Map<Object, Class<?>> managedEntities;

    /** Flag indicating whether this context is open. */
    private volatile boolean open;

    PersistenceContext() {
        this.managedEntities = new IdentityHashMap<>();
        this.open = true;
    }

    /**
     * Register an entity as managed.
     *
     * @param entity the entity instance
     */
    void register(Object entity) {
        managedEntities.put(entity, entity.getClass());
    }

    /**
     * Check whether an entity instance is managed by this context.
     *
     * @param entity the entity instance
     * @return {@code true} if managed
     */
    boolean contains(Object entity) {
        return managedEntities.containsKey(entity);
    }

    /**
     * Remove an entity from the persistence context.
     *
     * @param entity the entity instance
     */
    void unregister(Object entity) {
        managedEntities.remove(entity);
    }

    /**
     * Clear all managed entities from this context.
     */
    void clear() {
        managedEntities.clear();
    }

    /**
     * Close this persistence context, invalidating all managed entities.
     */
    void close() {
        open = false;
        managedEntities.clear();
    }

    /**
     * Check whether this context is still open.
     *
     * @return {@code true} if not closed
     */
    boolean isOpen() {
        return open;
    }
}
