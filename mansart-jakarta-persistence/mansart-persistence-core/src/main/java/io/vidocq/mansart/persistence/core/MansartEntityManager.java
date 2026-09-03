/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Metamodel;
import java.util.List;
import java.util.Map;

/**
 * Mansart implementation of Jakarta Persistence EntityManager.
 * All methods throw UnsupportedOperationException - stub for JP-01.
 */
public class MansartEntityManager implements EntityManager {

    private final MansartEntityManagerFactory entityManagerFactory;
    private final Map<String, Object> properties;

    public MansartEntityManager(MansartEntityManagerFactory entityManagerFactory, Map<String, Object> properties) {
        this.entityManagerFactory = entityManagerFactory;
        this.properties = properties;
    }

    @Override public void persist(Object entity) { throw new UnsupportedOperationException("not implemented: persist"); }
    @Override public <T> T merge(T entity) { throw new UnsupportedOperationException("not implemented: merge"); }
    @Override public void remove(Object entity) { throw new UnsupportedOperationException("not implemented: remove"); }
    @Override public <T> T find(Class<T> entityClass, Object primaryKey) { throw new UnsupportedOperationException("not implemented: find"); }
    @Override public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) { throw new UnsupportedOperationException("not implemented: find"); }
    @Override public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) { throw new UnsupportedOperationException("not implemented: find"); }
    @Override public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) { throw new UnsupportedOperationException("not implemented: find"); }
    @Override public <T> T find(Class<T> entityClass, Object primaryKey, FindOption... options) { throw new UnsupportedOperationException("not implemented: find"); }
    @Override public <T> T find(EntityGraph<T> entityGraph, Object primaryKey, FindOption... options) { throw new UnsupportedOperationException("not implemented: find"); }
    @Override public <T> T getReference(Class<T> entityClass, Object primaryKey) { throw new UnsupportedOperationException("not implemented: getReference"); }
    @Override public <T> T getReference(T entity) { throw new UnsupportedOperationException("not implemented: getReference"); }
    @Override public void flush() { throw new UnsupportedOperationException("not implemented: flush"); }
    @Override public void setFlushMode(FlushModeType flushModeType) { throw new UnsupportedOperationException("not implemented: setFlushMode"); }
    @Override public FlushModeType getFlushMode() { throw new UnsupportedOperationException("not implemented: getFlushMode"); }
    @Override public void lock(Object entity, LockModeType lockMode) { throw new UnsupportedOperationException("not implemented: lock"); }
    @Override public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) { throw new UnsupportedOperationException("not implemented: lock"); }
    @Override public void lock(Object entity, LockModeType lockMode, LockOption... options) { throw new UnsupportedOperationException("not implemented: lock"); }
    @Override public void refresh(Object entity) { throw new UnsupportedOperationException("not implemented: refresh"); }
    @Override public void refresh(Object entity, Map<String, Object> properties) { throw new UnsupportedOperationException("not implemented: refresh"); }
    @Override public void refresh(Object entity, LockModeType lockMode) { throw new UnsupportedOperationException("not implemented: refresh"); }
    @Override public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) { throw new UnsupportedOperationException("not implemented: refresh"); }
    @Override public void refresh(Object entity, RefreshOption... options) { throw new UnsupportedOperationException("not implemented: refresh"); }
    @Override public void clear() { throw new UnsupportedOperationException("not implemented: clear"); }
    @Override public void detach(Object entity) { throw new UnsupportedOperationException("not implemented: detach"); }
    @Override public boolean contains(Object entity) { throw new UnsupportedOperationException("not implemented: contains"); }
    @Override public LockModeType getLockMode(Object entity) { throw new UnsupportedOperationException("not implemented: getLockMode"); }
    @Override public void setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) { throw new UnsupportedOperationException("not implemented: setCacheRetrieveMode"); }
    @Override public void setCacheStoreMode(CacheStoreMode cacheStoreMode) { throw new UnsupportedOperationException("not implemented: setCacheStoreMode"); }
    @Override public CacheRetrieveMode getCacheRetrieveMode() { throw new UnsupportedOperationException("not implemented: getCacheRetrieveMode"); }
    @Override public CacheStoreMode getCacheStoreMode() { throw new UnsupportedOperationException("not implemented: getCacheStoreMode"); }
    @Override public void setProperty(String name, Object value) { throw new UnsupportedOperationException("not implemented: setProperty"); }
    @Override public Map<String, Object> getProperties() { return properties; }
    @Override public Query createQuery(String qlString) { throw new UnsupportedOperationException("not implemented: createQuery"); }
    @Override public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) { throw new UnsupportedOperationException("not implemented: createQuery"); }
    @Override public <T> TypedQuery<T> createQuery(CriteriaSelect<T> criteriaSelect) { throw new UnsupportedOperationException("not implemented: createQuery"); }
    @Override public Query createQuery(CriteriaUpdate<?> criteriaUpdate) { throw new UnsupportedOperationException("not implemented: createQuery"); }
    @Override public Query createQuery(CriteriaDelete<?> criteriaDelete) { throw new UnsupportedOperationException("not implemented: createQuery"); }
    @Override public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) { throw new UnsupportedOperationException("not implemented: createQuery"); }
    @Override public Query createNamedQuery(String name) { throw new UnsupportedOperationException("not implemented: createNamedQuery"); }
    @Override public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) { throw new UnsupportedOperationException("not implemented: createNamedQuery"); }
    @Override public <T> TypedQuery<T> createQuery(TypedQueryReference<T> typedQueryReference) { throw new UnsupportedOperationException("not implemented: createQuery"); }
    @Override public Query createNativeQuery(String sqlString) { throw new UnsupportedOperationException("not implemented: createNativeQuery"); }
    @Override public <T> Query createNativeQuery(String sqlString, Class<T> resultClass) { throw new UnsupportedOperationException("not implemented: createNativeQuery"); }
    @Override public Query createNativeQuery(String sqlString, String resultSetMapping) { throw new UnsupportedOperationException("not implemented: createNativeQuery"); }
    @Override public StoredProcedureQuery createNamedStoredProcedureQuery(String name) { throw new UnsupportedOperationException("not implemented: createNamedStoredProcedureQuery"); }
    @Override public StoredProcedureQuery createStoredProcedureQuery(String procedureName) { throw new UnsupportedOperationException("not implemented: createStoredProcedureQuery"); }
    @Override public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class<?>... resultClasses) { throw new UnsupportedOperationException("not implemented: createStoredProcedureQuery"); }
    @Override public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) { throw new UnsupportedOperationException("not implemented: createStoredProcedureQuery"); }
    @Override public void joinTransaction() { throw new UnsupportedOperationException("not implemented: joinTransaction"); }
    @Override public boolean isJoinedToTransaction() { throw new UnsupportedOperationException("not implemented: isJoinedToTransaction"); }
    @Override public <T> T unwrap(Class<T> cls) { throw new UnsupportedOperationException("not implemented: unwrap"); }
    @Override public Object getDelegate() { throw new UnsupportedOperationException("not implemented: getDelegate"); }
    @Override public void close() { throw new UnsupportedOperationException("not implemented: close"); }
    @Override public boolean isOpen() { throw new UnsupportedOperationException("not implemented: isOpen"); }
    @Override public EntityTransaction getTransaction() { throw new UnsupportedOperationException("not implemented: getTransaction"); }
    @Override public EntityManagerFactory getEntityManagerFactory() { return entityManagerFactory; }
    @Override public CriteriaBuilder getCriteriaBuilder() { throw new UnsupportedOperationException("not implemented: getCriteriaBuilder"); }
    @Override public Metamodel getMetamodel() { throw new UnsupportedOperationException("not implemented: getMetamodel"); }
    @Override public <T> EntityGraph<T> createEntityGraph(Class<T> entityClass) { throw new UnsupportedOperationException("not implemented: createEntityGraph"); }
    @Override public EntityGraph<?> createEntityGraph(String graphName) { throw new UnsupportedOperationException("not implemented: createEntityGraph"); }
    @Override public EntityGraph<?> getEntityGraph(String name) { throw new UnsupportedOperationException("not implemented: getEntityGraph"); }
    @Override public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) { throw new UnsupportedOperationException("not implemented: getEntityGraphs"); }
    @Override public <C> void runWithConnection(ConnectionConsumer<C> consumer) { throw new UnsupportedOperationException("not implemented: runWithConnection"); }
    @Override public <C, T> T callWithConnection(ConnectionFunction<C, T> function) { throw new UnsupportedOperationException("not implemented: callWithConnection"); }
}
