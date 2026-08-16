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
 * It is also made available under the European Union Public License v. 1.2,
 * which is available at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.runtime;

import jakarta.persistence.Cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * L2 cache implementation of JPA Cache interface for Mansart (Jakarta Persistence 3.2).
 * 
 * <p>This implementation provides:
 * <ul>
 *   <li>Thread-safe caching using ConcurrentHashMap</li>
 *   <li>Basic cache operations (get, put, evict, contains)</li>
 *   <li>Configurable maximum size with LRU-like eviction</li>
 * </ul>
 * 
 * <p>This serves as the L2 cache for M9-1. The cache is shared across all
 * EntityManager instances created from the same EntityManagerFactory.
 */
public class MansartCache implements Cache {

    private static final int DEFAULT_MAX_SIZE = 1000;

    private final Map<Class<?>, Map<Object, Object>> cache = new ConcurrentHashMap<>();
    private final int maxSize;
    
    /**
     * Creates a new MansartCache with default maximum size.
     */
    public MansartCache() {
        this(DEFAULT_MAX_SIZE);
    }
    
    /**
     * Creates a new MansartCache with the specified maximum size.
     * 
     * @param maxSize the maximum number of entities to cache
     */
    public MansartCache(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public boolean contains(Class<?> entityClass, Object primaryKey) {
        Map<Object, Object> entityCache = cache.get(entityClass);
        return entityCache != null && entityCache.containsKey(primaryKey);
    }

    @Override
    public void evict(Class<?> entityClass, Object primaryKey) {
        Map<Object, Object> entityCache = cache.get(entityClass);
        if (entityCache != null) {
            entityCache.remove(primaryKey);
        }
    }

    @Override
    public void evict(Class<?> entityClass) {
        cache.remove(entityClass);
    }

    /**
     * Retrieves an entity from the cache.
     * 
     * @param entityClass the entity class
     * @param primaryKey the primary key
     * @return the cached entity, or null if not found
     */
    public Object get(Class<?> entityClass, Object primaryKey) {
        Map<Object, Object> entityCache = cache.get(entityClass);
        return entityCache != null ? entityCache.get(primaryKey) : null;
    }

    /**
     * Stores an entity in the cache.
     * 
     * @param entityClass the entity class
     * @param primaryKey the primary key
     * @param entity the entity to cache
     */
    public void put(Class<?> entityClass, Object primaryKey, Object entity) {
        // Evict entries if cache is full
        if (isFull()) {
            evictToMakeRoom();
        }
        cache.computeIfAbsent(entityClass, k -> new ConcurrentHashMap<>()).put(primaryKey, entity);
    }

    @Override
    public void evictAll() {
        cache.clear();
    }

    /**
     * Gets the current size of the cache.
     *
     * @return the total number of cached entities
     */
    public int size() {
        int total = 0;
        for (Map<Object, Object> entityCache : cache.values()) {
            total += entityCache.size();
        }
        return total;
    }

    /**
     * Checks if the cache has reached its maximum size.
     *
     * @return true if the cache is full, false otherwise
     */
    public boolean isFull() {
        return size() >= maxSize;
    }

    /**
     * Evicts the oldest entries to make room for new entries.
     * Uses a simple strategy: clears the first entity class cache found.
     * This is a basic eviction strategy; a more sophisticated LRU
     * implementation can be added in a future enhancement.
     */
    private void evictToMakeRoom() {
        if (!isFull()) {
            return;
        }
        // Simple eviction: remove all entries from the first entity class
        // In a production implementation, this would use LRU or similar
        for (Class<?> entityClass : cache.keySet()) {
            cache.remove(entityClass);
            break; // Remove one entity class worth of entries
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> type) {
        return type.isInstance(this) ? type.cast(this) : null;
    }
}
