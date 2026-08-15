/*
 * Copyright (c) 2026 Vidocq contributors
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.jpql;

import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.data.dialect.SqlFragment;
import io.vidocq.mansart.data.dialect.Where;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for JPQL query parsing and SQL generation.
 * 
 * <p>Caches the conversion from JPQL AST to Dialect.Where and SQL fragments
 * to avoid repeated parsing of the same queries.
 * 
 * <p>Cache key is based on:
 * - JPQL query string (hash code for equality)
 * - EntityModel (for type-specific queries)
 * - Dialect (different dialects produce different SQL)
 * 
 * <p>Milestone: M9-8 - Query caching optimization.
 */
public final class QueryCache {

    /**
     * Cache entry for a parsed JPQL query.
     */
    static final class CacheEntry {
        final Where where;
        final SqlFragment sqlFragment;
        final long creationTime;
        
        CacheEntry(Where where, SqlFragment sqlFragment) {
            this.where = where;
            this.sqlFragment = sqlFragment;
            this.creationTime = System.nanoTime();
        }
    }
    
    /**
     * Cache key combining query hash, entity class, and dialect.
     */
    private static final class CacheKey {
        private final int queryHash;
        private final Class<?> entityClass;
        private final String dialectName;
        private final int maxResults;
        private final int firstResult;
        
        CacheKey(JPQLQuery<?> query, Class<?> entityClass, Dialect dialect, int maxResults, int firstResult) {
            this.queryHash = System.identityHashCode(query);
            this.entityClass = entityClass;
            this.dialectName = dialect.name();
            this.maxResults = maxResults;
            this.firstResult = firstResult;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey)) return false;
            CacheKey other = (CacheKey) o;
            return queryHash == other.queryHash &&
                   entityClass.equals(other.entityClass) &&
                   dialectName.equals(other.dialectName) &&
                   maxResults == other.maxResults &&
                   firstResult == other.firstResult;
        }
        
        @Override
        public int hashCode() {
            int result = queryHash;
            result = 31 * result + entityClass.hashCode();
            result = 31 * result + dialectName.hashCode();
            result = 31 * result + maxResults;
            result = 31 * result + firstResult;
            return result;
        }
    }
    
    private final Map<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();
    private final int maxSize;
    
    // Statistics
    private volatile long hits = 0;
    private volatile long misses = 0;
    
    /**
     * Creates a new QueryCache with default maximum size (1000 entries).
     */
    public QueryCache() {
        this(1000);
    }
    
    /**
     * Creates a new QueryCache with the specified maximum size.
     *
     * @param maxSize maximum number of entries to cache
     */
    public QueryCache(int maxSize) {
        this.maxSize = maxSize;
    }
    
    /**
     * Gets a cached Where predicate and SQL fragment for the given query.
     *
     * @param query the JPQL query
     * @param entityModel the entity model
     * @param dialect the SQL dialect
     * @param converter the JPQL to SQL converter
     * @param maxResults maximum results (0 for no limit)
     * @param firstResult first result offset
     * @return cached entry, or null if not cached
     */
    public CacheEntry get(JPQLQuery<?> query, EntityModel<?> entityModel, Dialect dialect,
                         JpqlToSqlConverter converter, int maxResults, int firstResult) {
        CacheKey key = new CacheKey(query, entityModel.entityClass(), dialect, maxResults, firstResult);
        CacheEntry entry = cache.get(key);
        
        if (entry != null) {
            hits++;
            return entry;
        }
        misses++;
        return null;
    }
    
    /**
     * Stores a parsed query in the cache.
     *
     * @param query the JPQL query
     * @param entityModel the entity model
     * @param dialect the SQL dialect
     * @param where the converted Where predicate
     * @param sqlFragment the generated SQL fragment
     * @param maxResults maximum results (0 for no limit)
     * @param firstResult first result offset
     */
    public void put(JPQLQuery<?> query, EntityModel<?> entityModel, Dialect dialect,
                   Where where, SqlFragment sqlFragment, int maxResults, int firstResult) {
        // Check if we need to evict old entries
        if (cache.size() >= maxSize) {
            evictOldest();
        }
        
        CacheKey key = new CacheKey(query, entityModel.entityClass(), dialect, maxResults, firstResult);
        cache.put(key, new CacheEntry(where, sqlFragment));
    }
    
    /**
     * Clears all entries from the cache.
     */
    public void clear() {
        cache.clear();
    }
    
    /**
     * Returns the current number of cached entries.
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * Returns the cache hit count.
     */
    public long getHitCount() {
        return hits;
    }
    
    /**
     * Returns the cache miss count.
     */
    public long getMissCount() {
        return misses;
    }
    
    /**
     * Returns the cache hit ratio.
     */
    public double getHitRatio() {
        long total = hits + misses;
        return total == 0 ? 0.0 : (double) hits / total;
    }
    
    /**
     * Resets cache statistics.
     */
    public void resetStatistics() {
        hits = 0;
        misses = 0;
    }
    
    /**
     * Evicts an entry from the cache when it reaches maximum size.
     * Simple eviction: remove the first entry we find.
     */
    private void evictOldest() {
        // Simple eviction strategy: remove any entry
        // For a more sophisticated strategy, we'd need to track access times
        // The ConcurrentHashMap doesn't provide ordered iteration, 
        // but this simple approach works for basic eviction
        for (CacheKey key : cache.keySet()) {
            cache.remove(key);
            break;
        }
    }
    
    @Override
    public String toString() {
        return "QueryCache[size=" + cache.size() + 
               ", hits=" + hits + 
               ", misses=" + misses + 
               ", hitRatio=" + String.format("%.2f%%", getHitRatio() * 100) + "]";
    }
}
