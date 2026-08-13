/*
 * Copyright (c) 2024-2025 Vidocq. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package io.vidocq.mansart.persistence.core.cache;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * L1 cache implementation using IdentityHashMap for entity storage.
 * <p>
 * This cache stores entities using IdentityHashMap to maintain entity
 * identity across persistence operations.
 */
public class EntityCache {

    private final Map<Object, Object> cache = new IdentityHashMap<>();

    /**
     * Retrieves an entity from the cache by class and identifier.
     *
     * @param entityClass the entity class
     * @param id the entity identifier
     * @param <T> the entity type
     * @return the cached entity, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> entityClass, Object id) {
        return (T) cache.get(new CacheKey(entityClass, id));
    }

    /**
     * Stores an entity in the cache with the given identifier.
     *
     * @param entity the entity to cache
     * @param id the entity identifier
     */
    public void put(Object entity, Object id) {
        cache.put(entity, id);
    }

    /**
     * Removes an entity from the cache.
     *
     * @param entity the entity to evict
     */
    public void evict(Object entity) {
        cache.remove(entity);
    }

    /**
     * Clears all entities from the cache.
     */
    public void clear() {
        cache.clear();
    }

    private static class CacheKey {
        private final Class<?> entityClass;
        private final Object id;

        CacheKey(Class<?> entityClass, Object id) {
            this.entityClass = entityClass;
            this.id = id;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof CacheKey)) {
                return false;
            }
            CacheKey other = (CacheKey) obj;
            return entityClass == other.entityClass && id.equals(other.id);
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(entityClass) + id.hashCode();
        }
    }
}
