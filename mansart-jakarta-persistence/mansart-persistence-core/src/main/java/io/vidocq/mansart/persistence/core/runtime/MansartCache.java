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

import java.util.HashMap;
import java.util.Map;

/**
 * Basic implementation of JPA Cache interface for Mansart (Jakarta Persistence 3.2).
 * 
 * <p>This is a simple in-memory L2 cache implementation for M8-18.
 * It provides basic cache functionality to unblock TCK CacheTests.
 * 
 * <p>Note: Full L2 cache with invalidation, eviction, and query cache
 * will be implemented in M9-1.
 */
public class MansartCache implements Cache {

    private final Map<Class<?>, Map<Object, Object>> cache = new HashMap<>();

    public MansartCache() {
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

    @Override
    public void evictAll() {
        cache.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> type) {
        return type.isInstance(this) ? type.cast(this) : null;
    }
}
