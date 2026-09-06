/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import io.vidocq.mansart.persistence.core.context.MansartPersistenceContext;
import io.vidocq.mansart.persistence.core.runtime.MansartCallback;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Metamodel;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mansart implementation of Jakarta Persistence EntityManager.
 *
 * <p>Lifecycle is managed jointly with the owning {@link MansartEntityManagerFactory}:
 * when the factory closes, all tracked EMs are marked closed via {@link #markClosed()}.
 *
 * <p>Closed-state contract (per the {@code EntityManager.close()} Javadoc): after
 * {@link #close()} every method throws {@link IllegalStateException} except
 * {@link #isOpen()}, {@link #getProperties()} and {@link #getTransaction()}.
 *
 * <p>Entity-state operations (persist, find, merge, remove, refresh, flush, contains,
 * detach, clear, lock) are delegated to the {@link MansartPersistenceContext}; the
 * state machine itself is implemented in card M4-JP-26.
 */
public class MansartEntityManager implements EntityManager {

    private final MansartEntityManagerFactory entityManagerFactory;
    private final Map<String, Object> properties;
    private final AtomicBoolean open = new AtomicBoolean(true);
    private final ConcurrentHashMap<String, Object> propertyOverrides = new ConcurrentHashMap<>();
    private final MansartPersistenceContext persistenceContext;

    private volatile FlushModeType flushMode = FlushModeType.AUTO;
    private volatile CacheRetrieveMode cacheRetrieveMode = CacheRetrieveMode.USE;
    private volatile CacheStoreMode cacheStoreMode = CacheStoreMode.USE;

    public MansartEntityManager(MansartEntityManagerFactory entityManagerFactory, Map<String, Object> properties) {
        this(entityManagerFactory, properties, null);
    }

    public MansartEntityManager(MansartEntityManagerFactory entityManagerFactory, Map<String, Object> properties,
                               MansartCallback callback) {
        this.entityManagerFactory = entityManagerFactory;
        this.properties = properties;
        this.persistenceContext = new MansartPersistenceContext(callback);
    }

    private void ensureOpen() {
        if (!open.get()) {
            throw new IllegalStateException("EntityManager is closed");
        }
    }

    // ── Entity-state operations: delegated to the persistence context ────

    @Override public void persist(Object entity) { ensureOpen(); persistenceContext.persist(entity); }
    @Override public <T> T merge(T entity) { ensureOpen(); return persistenceContext.merge(entity); }
    @Override public void remove(Object entity) { ensureOpen(); persistenceContext.remove(entity); }
    @Override public <T> T find(Class<T> entityClass, Object primaryKey) { ensureOpen(); return persistenceContext.find(entityClass, primaryKey); }
    @Override public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> props) { ensureOpen(); return persistenceContext.find(entityClass, primaryKey, props); }
    @Override public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) { ensureOpen(); return persistenceContext.find(entityClass, primaryKey, lockMode); }
    @Override public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> props) { ensureOpen(); return persistenceContext.find(entityClass, primaryKey, lockMode, props); }
    @Override public <T> T find(Class<T> entityClass, Object primaryKey, FindOption... options) { ensureOpen(); return persistenceContext.find(entityClass, primaryKey, options); }
    @Override public <T> T find(EntityGraph<T> entityGraph, Object primaryKey, FindOption... options) { ensureOpen(); return persistenceContext.find(entityGraph, primaryKey, options); }
    @Override public <T> T getReference(Class<T> entityClass, Object primaryKey) { ensureOpen(); return persistenceContext.getReference(entityClass, primaryKey); }
    @Override public <T> T getReference(T entity) { ensureOpen(); return persistenceContext.getReference(entity); }
    @Override public void flush() { ensureOpen(); persistenceContext.flush(); }
    @Override public void refresh(Object entity) { ensureOpen(); persistenceContext.refresh(entity); }
    @Override public void refresh(Object entity, Map<String, Object> props) { ensureOpen(); persistenceContext.refresh(entity, props); }
    @Override public void refresh(Object entity, LockModeType lockMode) { ensureOpen(); persistenceContext.refresh(entity, lockMode); }
    @Override public void refresh(Object entity, LockModeType lockMode, Map<String, Object> props) { ensureOpen(); persistenceContext.refresh(entity, lockMode, props); }
    @Override public void refresh(Object entity, RefreshOption... options) { ensureOpen(); persistenceContext.refresh(entity, options); }
    @Override public void clear() { ensureOpen(); persistenceContext.clear(); }
    @Override public void detach(Object entity) { ensureOpen(); persistenceContext.detach(entity); }
    @Override public boolean contains(Object entity) { ensureOpen(); return persistenceContext.contains(entity); }
    @Override public void lock(Object entity, LockModeType lockMode) { ensureOpen(); persistenceContext.lock(entity, lockMode); }
    @Override public void lock(Object entity, LockModeType lockMode, Map<String, Object> props) { ensureOpen(); persistenceContext.lock(entity, lockMode, props); }
    @Override public void lock(Object entity, LockModeType lockMode, LockOption... options) { ensureOpen(); persistenceContext.lock(entity, lockMode, options); }
    @Override public LockModeType getLockMode(Object entity) { ensureOpen(); return persistenceContext.getLockMode(entity); }

    // ── Configuration: flush and cache modes ─────────────────────────────

    @Override public void setFlushMode(FlushModeType flushModeType) {
        ensureOpen();
        this.flushMode = flushModeType;
    }
    @Override public FlushModeType getFlushMode() {
        ensureOpen();
        return this.flushMode;
    }
    @Override public void setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        ensureOpen();
        this.cacheRetrieveMode = cacheRetrieveMode;
    }
    @Override public CacheRetrieveMode getCacheRetrieveMode() {
        ensureOpen();
        return this.cacheRetrieveMode;
    }
    @Override public void setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        ensureOpen();
        this.cacheStoreMode = cacheStoreMode;
    }
    @Override public CacheStoreMode getCacheStoreMode() {
        ensureOpen();
        return this.cacheStoreMode;
    }

    // ── Properties ──────────────────────────────────────────────────────

    @Override public void setProperty(String name, Object value) {
        ensureOpen();
        propertyOverrides.put(name, value);
    }

    /**
     * Returns the properties in effect for this EntityManager: the factory
     * properties overlaid with any overrides set via {@link #setProperty}.
     * Exempt from the closed-state contract, so this does not throw if the
     * EntityManager has been closed.
     */
    @Override public Map<String, Object> getProperties() {
        Map<String, Object> merged = new LinkedHashMap<>(properties == null ? Map.of() : properties);
        merged.putAll(propertyOverrides);
        return Collections.unmodifiableMap(merged);
    }

    // ── Query operations (JPQL/Criteria — M6, native — M12) ──────────────

    @Override public Query createQuery(String qlString) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createQuery"); }
    @Override public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createQuery"); }
    @Override public <T> TypedQuery<T> createQuery(CriteriaSelect<T> criteriaSelect) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createQuery"); }
    @Override public Query createQuery(CriteriaUpdate<?> criteriaUpdate) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createQuery"); }
    @Override public Query createQuery(CriteriaDelete<?> criteriaDelete) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createQuery"); }
    @Override public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createQuery"); }
    @Override public Query createNamedQuery(String name) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createNamedQuery"); }
    @Override public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createNamedQuery"); }
    @Override public <T> TypedQuery<T> createQuery(TypedQueryReference<T> typedQueryReference) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createQuery"); }
    @Override public Query createNativeQuery(String sqlString) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createNativeQuery"); }
    @Override public <T> Query createNativeQuery(String sqlString, Class<T> resultClass) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createNativeQuery"); }
    @Override public Query createNativeQuery(String sqlString, String resultSetMapping) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createNativeQuery"); }
    @Override public StoredProcedureQuery createNamedStoredProcedureQuery(String name) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createNamedStoredProcedureQuery"); }
    @Override public StoredProcedureQuery createStoredProcedureQuery(String procedureName) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createStoredProcedureQuery"); }
    @Override public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class<?>... resultClasses) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createStoredProcedureQuery"); }
    @Override public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createStoredProcedureQuery"); }

    // ── Transaction integration (M4-JP-27) / unwrap / delegate ────────────

    @Override public void joinTransaction() { ensureOpen(); throw new UnsupportedOperationException("not implemented: joinTransaction"); }
    @Override public boolean isJoinedToTransaction() { ensureOpen(); throw new UnsupportedOperationException("not implemented: isJoinedToTransaction"); }
    @Override public Object getDelegate() { ensureOpen(); return this; }
    @Override public <T> T unwrap(Class<T> cls) {
        ensureOpen();
        if (cls.isInstance(this)) {
            return cls.cast(this);
        }
        throw new PersistenceException("Cannot unwrap to " + cls.getName());
    }

    // ── Lifecycle ───────────────────────────────────────────────────────

    @Override public void close() {
        if (!open.compareAndSet(true, false)) {
            throw new IllegalStateException("EntityManager is closed");
        }
        entityManagerFactory.unregisterEntityManager(this);
    }
    @Override public boolean isOpen() {
        return open.get() && entityManagerFactory.isOpen();
    }

    /**
     * Marks this EntityManager as closed without deregistering from the factory.
     * Called by {@link MansartEntityManagerFactory#close()} when cascading close
     * to all tracked entity managers.
     */
    void markClosed() {
        open.set(false);
    }

    // ── Metadata accessors ──────────────────────────────────────────────

    @Override public EntityTransaction getTransaction() { throw new UnsupportedOperationException("not implemented: getTransaction"); }
    @Override public EntityManagerFactory getEntityManagerFactory() { ensureOpen(); return entityManagerFactory; }
    @Override public CriteriaBuilder getCriteriaBuilder() { ensureOpen(); throw new UnsupportedOperationException("not implemented: getCriteriaBuilder"); }
    @Override public Metamodel getMetamodel() { ensureOpen(); throw new UnsupportedOperationException("not implemented: getMetamodel"); }
    @Override public <T> EntityGraph<T> createEntityGraph(Class<T> entityClass) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createEntityGraph"); }
    @Override public EntityGraph<?> createEntityGraph(String graphName) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createEntityGraph"); }
    @Override public EntityGraph<?> getEntityGraph(String name) { ensureOpen(); throw new UnsupportedOperationException("not implemented: getEntityGraph"); }
    @Override public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) { ensureOpen(); throw new UnsupportedOperationException("not implemented: getEntityGraphs"); }
    @Override public <C> void runWithConnection(ConnectionConsumer<C> consumer) { ensureOpen(); throw new UnsupportedOperationException("not implemented: runWithConnection"); }
    @Override public <C, T> T callWithConnection(ConnectionFunction<C, T> function) { ensureOpen(); throw new UnsupportedOperationException("not implemented: callWithConnection"); }
}
