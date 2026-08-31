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
 * <p>Serves as the identity map (instance → class) and also supports
 * class+ID lookup for entity retrieval by primary key.</p>
 */
final class PersistenceContext {

    /** Entity instance → entity class, keyed by identity. */
    private final Map<Object, Class<?>> managedEntities;

    /** Entity class → (ID → entity instance), for class+ID lookup. */
    private final Map<Class<?>, Map<Object, Object>> registeredById;

    /** Flag indicating whether this context is open. */
    private volatile boolean open;

    PersistenceContext() {
        this.managedEntities = new IdentityHashMap<>();
        this.registeredById = new IdentityHashMap<>();
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
        // Also remove from the class+ID lookup by scanning all registered entities.
        Class<?> cls = managedEntities.values().stream()
                .filter(c -> c == entity.getClass())
                .findFirst().orElse(null);
        if (cls != null) {
            Map<Object, Object> byId = registeredById.get(cls);
            if (byId != null) {
                // Remove the entry whose value is this entity instance (identity match).
                for (java.util.Map.Entry<Object, Object> entry : byId.entrySet()) {
                    if (entry.getValue() == entity) {
                        byId.remove(entry.getKey());
                        break;
                    }
                }
                if (byId.isEmpty()) {
                    registeredById.remove(cls);
                }
            }
        }
    }

    /**
     * Register an entity by its class and ID for lookup.
     *
     * @param entityClass the entity class
     * @param id          the entity's primary key value
     * @param entity      the entity instance
     */
    void registerById(Class<?> entityClass, Object id, Object entity) {
        managedEntities.put(entity, entityClass);
        registeredById.computeIfAbsent(entityClass, k -> new IdentityHashMap<>())
                .put(id, entity);
    }

    /**
     * Look up an entity by its class and ID.
     *
     * @param entityClass the entity class
     * @param id          the primary key value
     * @return the managed entity instance, or {@code null} if not found
     */
    Object lookupById(Class<?> entityClass, Object id) {
        Map<Object, Object> byId = registeredById.get(entityClass);
        return byId == null ? null : byId.get(id);
    }

    /**
     * Clear all managed entities from this context.
     */
    void clear() {
        managedEntities.clear();
        registeredById.clear();
    }

    /**
     * Close this persistence context, invalidating all managed entities.
     */
    void close() {
        open = false;
        managedEntities.clear();
        registeredById.clear();
    }

    /**
     * Check whether this context is still open.
     *
     * @return {@code true} if not closed
     */
    boolean isOpen() {
        return open;
    }

    /**
     * Return all registered entity classes and their ID→entity maps.
     *
     * @return the class-to-ID-map registry
     */
    java.util.Map<Class<?>, java.util.Map<Object, Object>> registeredById() {
        return registeredById;
    }
}
