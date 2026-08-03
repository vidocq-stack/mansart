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
package io.vidocq.mansart.persistence.core;

import io.vidocq.mansart.data.core.RepositoryRuntime;
import io.vidocq.mansart.data.dialect.Attribute;
import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.data.dialect.attribute.IdAttribute;
import io.vidocq.mansart.persistence.core.bootstrap.MansartEntityManagerFactory;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.ConnectionConsumer;
import jakarta.persistence.ConnectionFunction;
import jakarta.persistence.StoredProcedureQuery;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mansart implementation of Jakarta Persistence EntityManager.
 * 
 * <p>M4 — EntityManager implementation using Mansart Data RepositoryRuntime.
 */
public class MansartEntityManager implements EntityManager {

    private final RepositoryRuntime repositoryRuntime;
    private final Dialect dialect;
    private final DataSource dataSource;
    private final Map<EntityCacheKey, Object> persistenceContext = new IdentityHashMap<>();
    private final Map<Class<?>, EntityModel<?>> entityModelCache = new ConcurrentHashMap<>();
    private final MansartEntityManagerFactory entityManagerFactory;
    
    private MansartEntityTransaction currentTransaction;
    private boolean open = true;

    public MansartEntityManager(RepositoryRuntime repositoryRuntime, Dialect dialect, 
                               DataSource dataSource, MansartEntityManagerFactory entityManagerFactory) {
        this.repositoryRuntime = repositoryRuntime;
        this.dialect = dialect;
        this.dataSource = dataSource;
        this.entityManagerFactory = entityManagerFactory;
    }

    /* -------- Entity Lifecycle Operations -------- */

    @Override
    public void persist(Object entity) {
        if (entity == null) throw new IllegalArgumentException("Entity cannot be null");
        
        EntityModel<?> model = getEntityModel(entity.getClass());
        Object id = getIdValue(entity, model);
        
        if (id != null && contains(entity)) {
            throw new IllegalArgumentException("Entity already exists in persistence context");
        }
        
        saveEntity(model, entity);
        
        if (id == null) id = getIdValue(entity, model);
        EntityCacheKey cacheKey = new EntityCacheKey(entity.getClass(), id);
        persistenceContext.put(cacheKey, entity);
    }

    @Override
    public <T> T merge(T entity) {
        if (entity == null) throw new IllegalArgumentException("Entity cannot be null");
        
        EntityModel<?> model = getEntityModel(entity.getClass());
        Object id = getIdValue(entity, model);
        
        if (id == null) {
            persist(entity);
            return entity;
        }
        
        EntityCacheKey cacheKey = new EntityCacheKey(entity.getClass(), id);
        @SuppressWarnings("unchecked")
        T managed = (T) persistenceContext.get(cacheKey);
        
        if (managed != null) {
            copyState(entity, managed, model);
            return managed;
        }
        
        @SuppressWarnings("unchecked")
        T existing = (T) find(model.entityClass(), id);
        if (existing != null) {
            copyState(entity, existing, model);
            return existing;
        }
        
        persist(entity);
        return entity;
    }

    @Override
    public void remove(Object entity) {
        if (entity == null) throw new IllegalArgumentException("Entity cannot be null");
        if (!contains(entity)) throw new IllegalArgumentException("Entity is not managed");
        
        EntityModel<?> model = getEntityModel(entity.getClass());
        Object id = getIdValue(entity, model);
        
        persistenceContext.remove(new EntityCacheKey(entity.getClass(), id));
        deleteEntity(model, entity);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        return find(entityClass, primaryKey, null, null);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
        return find(entityClass, primaryKey, null, properties);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
        return find(entityClass, primaryKey, lockMode, null);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
        if (entityClass == null || primaryKey == null) throw new IllegalArgumentException("Arguments cannot be null");
        
        EntityCacheKey cacheKey = new EntityCacheKey(entityClass, primaryKey);
        @SuppressWarnings("unchecked")
        T cached = (T) persistenceContext.get(cacheKey);
        if (cached != null) return cached;
        
        EntityModel<T> model = getEntityModel(entityClass);
        Optional<T> result = repositoryRuntime.findById(model, primaryKey);
        
        return result.map(entity -> {
            persistenceContext.put(cacheKey, entity);
            return entity;
        }).orElse(null);
    }

    @Override
    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        return find(entityClass, primaryKey);
    }
    
    @Override
    public <T> T getReference(T entity) {
        return entity; // For now, return the entity itself
    }
    
    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, jakarta.persistence.FindOption... options) {
        return find(entityClass, primaryKey);
    }
    
    @Override
    public <T> T find(jakarta.persistence.EntityGraph<T> graph, Object primaryKey, jakarta.persistence.FindOption... options) {
        throw new UnsupportedOperationException("find with EntityGraph not yet implemented");
    }

    /* -------- Query Operations -------- */

    @Override public CriteriaBuilder getCriteriaBuilder() { throw new UnsupportedOperationException("getCriteriaBuilder not yet implemented"); }
    
    @Override public void flush() {}
    @Override public void setFlushMode(FlushModeType flushMode) {}
    @Override public FlushModeType getFlushMode() { return FlushModeType.AUTO; }
    @Override public Query createQuery(String qlString) { throw new UnsupportedOperationException("JPQL not yet implemented"); }
    @Override public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) { throw new UnsupportedOperationException("CriteriaQuery not yet implemented"); }
    @Override public <T> TypedQuery<T> createQuery(jakarta.persistence.criteria.CriteriaSelect<T> criteriaSelect) { throw new UnsupportedOperationException("CriteriaSelect not yet implemented"); }
    @Override public Query createQuery(CriteriaUpdate updateQuery) { throw new UnsupportedOperationException("CriteriaUpdate not yet implemented"); }
    @Override public Query createQuery(CriteriaDelete deleteQuery) { throw new UnsupportedOperationException("CriteriaDelete not yet implemented"); }
    @Override public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) { throw new UnsupportedOperationException("TypedQuery not yet implemented"); }
    @Override public <T> TypedQuery<T> createQuery(jakarta.persistence.TypedQueryReference<T> typedQueryReference) { throw new UnsupportedOperationException("TypedQueryReference not yet implemented"); }
    @Override public Query createNamedQuery(String name) { throw new UnsupportedOperationException("Named queries not yet implemented"); }
    @Override public <T> TypedQuery<T> createNamedQuery(String name, Class<T> result) { throw new UnsupportedOperationException("Named queries not yet implemented"); }
    @Override public Query createNativeQuery(String sqlString) { throw new UnsupportedOperationException("Native queries not yet implemented"); }
    @Override public Query createNativeQuery(String sqlString, Class resultClass) { throw new UnsupportedOperationException("Native queries not yet implemented"); }
    @Override public Query createNativeQuery(String sqlString, String resultSetMapping) { throw new UnsupportedOperationException("Native queries not yet implemented"); }
    @Override public jakarta.persistence.StoredProcedureQuery createNamedStoredProcedureQuery(String name) { throw new UnsupportedOperationException("Stored procedures not yet implemented"); }
    @Override public jakarta.persistence.StoredProcedureQuery createStoredProcedureQuery(String procedureName) { throw new UnsupportedOperationException("Stored procedures not yet implemented"); }
    @Override public jakarta.persistence.StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class<?>... resultClasses) { throw new UnsupportedOperationException("Stored procedures not yet implemented"); }
    @Override public jakarta.persistence.StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) { throw new UnsupportedOperationException("Stored procedures not yet implemented"); }

    /* -------- Transaction Management -------- */

    @Override
    public EntityTransaction getTransaction() {
        if (currentTransaction == null) {
            currentTransaction = new MansartEntityTransaction(this);
        }
        return currentTransaction;
    }

    @Override public boolean isJoinedToTransaction() { return getTransaction().isActive(); }
    @Override public void joinTransaction() { throw new UnsupportedOperationException("joinTransaction not yet implemented"); }

    /* -------- Persistence Context Management -------- */

    @Override
    public boolean contains(Object entity) {
        if (entity == null) return false;
        EntityModel<?> model = getEntityModel(entity.getClass());
        Object id = getIdValue(entity, model);
        if (id == null) return persistenceContext.containsValue(entity);
        return persistenceContext.containsKey(new EntityCacheKey(entity.getClass(), id));
    }

    @Override public void clear() { persistenceContext.clear(); }
    
    @Override
    public void detach(Object entity) {
        if (entity == null) return;
        EntityModel<?> model = getEntityModel(entity.getClass());
        Object id = getIdValue(entity, model);
        if (id != null) {
            persistenceContext.remove(new EntityCacheKey(entity.getClass(), id));
        } else {
            persistenceContext.values().remove(entity);
        }
    }

    @Override
    public void refresh(Object entity) {
        if (entity == null) throw new IllegalArgumentException("Entity cannot be null");
        EntityModel<?> model = getEntityModel(entity.getClass());
        Object id = getIdValue(entity, model);
        if (id == null) throw new IllegalArgumentException("Entity has no ID");
        
        EntityCacheKey cacheKey = new EntityCacheKey(entity.getClass(), id);
        Object existing = persistenceContext.get(cacheKey);
        if (existing == null) throw new IllegalArgumentException("Entity not managed");
        
        @SuppressWarnings("unchecked")
        Optional<Object> reloaded = repositoryRuntime.findById((EntityModel<Object>) model, id);
        if (reloaded.isPresent()) {
            copyState(reloaded.get(), existing, model);
        } else {
            persistenceContext.remove(cacheKey);
            throw new EntityNotFoundException("Entity no longer exists in database");
        }
    }

    @Override public void lock(Object entity, LockModeType lockMode) { throw new UnsupportedOperationException("Locking not yet implemented"); }
    @Override public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) { throw new UnsupportedOperationException("Locking not yet implemented"); }
    @Override public LockModeType getLockMode(Object entity) { throw new UnsupportedOperationException("getLockMode not yet implemented"); }
    @Override public void setProperty(String name, Object value) { throw new UnsupportedOperationException("setProperty not yet implemented"); }
    @Override public void lock(Object entity, LockModeType lockMode, jakarta.persistence.LockOption... options) { throw new UnsupportedOperationException("lock with options not yet implemented"); }
    @Override public void refresh(Object entity, jakarta.persistence.RefreshOption... options) { refresh(entity); }
    @Override public void refresh(Object entity, Map<String, Object> properties) { refresh(entity); }
    @Override public void refresh(Object entity, LockModeType lockMode) { refresh(entity); }
    @Override public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) { refresh(entity); }

    /* -------- Metadata and Properties -------- */

    @Override public Metamodel getMetamodel() { throw new UnsupportedOperationException("Metamodel not yet implemented"); }
    @Override public <T> EntityGraph<T> createEntityGraph(Class<T> entityClass) { throw new UnsupportedOperationException("Entity graphs not yet implemented"); }
    @Override public EntityGraph<?> createEntityGraph(String name) { throw new UnsupportedOperationException("Entity graphs not yet implemented"); }
    @Override public EntityGraph<?> getEntityGraph(String name) { throw new UnsupportedOperationException("Entity graphs not yet implemented"); }
    @Override public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) { throw new UnsupportedOperationException("Entity graphs not yet implemented"); }
    @Override public Map<String, Object> getProperties() { return entityManagerFactory.getProperties(); }
    @Override public EntityManagerFactory getEntityManagerFactory() { return entityManagerFactory; }
    @Override public Object getDelegate() { return this; }
    @Override public <T> T unwrap(Class<T> clazz) { 
        if (clazz.isInstance(this)) return clazz.cast(this);
        throw new IllegalArgumentException("Cannot unwrap to " + clazz.getName()); 
    }
    
    // Cache modes
    @Override public void setCacheRetrieveMode(jakarta.persistence.CacheRetrieveMode mode) {}
    @Override public void setCacheStoreMode(jakarta.persistence.CacheStoreMode mode) {}
    @Override public jakarta.persistence.CacheRetrieveMode getCacheRetrieveMode() { return jakarta.persistence.CacheRetrieveMode.BYPASS; }
    @Override public jakarta.persistence.CacheStoreMode getCacheStoreMode() { return jakarta.persistence.CacheStoreMode.BYPASS; }

    /* -------- Connection Management -------- */

    @Override
    public <C> void runWithConnection(ConnectionConsumer<C> connectionConsumer) {
        throw new UnsupportedOperationException("runWithConnection not yet implemented");
    }

    @Override
    public <C, T> T callWithConnection(ConnectionFunction<C, T> connectionFunction) {
        throw new UnsupportedOperationException("callWithConnection not yet implemented");
    }

    /* -------- Cleanup -------- */

    @Override
    public void close() {
        if (!open) return;
        open = false;
        persistenceContext.clear();
        if (currentTransaction != null && currentTransaction.isActive()) {
            currentTransaction.rollback();
        }
    }

    @Override public boolean isOpen() { return open; }


    /* -------- Helper Methods -------- */

    @SuppressWarnings("unchecked")
    private <T> EntityModel<T> getEntityModel(Class<T> entityClass) {
        return (EntityModel<T>) entityModelCache.computeIfAbsent(entityClass, clazz -> {
            try {
                return EntityModelResolver.resolve(clazz);
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot find EntityModel for " + clazz.getName(), e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private Object getIdValue(Object entity, EntityModel<?> model) {
        try {
            return ((IdAttribute<?, Object>) model.id()).getter().invoke(entity);
        } catch (Throwable t) {
            throw new IllegalArgumentException("Failed to read ID from entity " + entity.getClass().getName(), t);
        }
    }

    @SuppressWarnings("unchecked")
    private void saveEntity(EntityModel<?> model, Object entity) {
        repositoryRuntime.save((EntityModel<Object>) model, (Object) entity);
    }

    @SuppressWarnings("unchecked")
    private void deleteEntity(EntityModel<?> model, Object entity) {
        repositoryRuntime.delete((EntityModel<Object>) model, (Object) entity);
    }

    @SuppressWarnings("unchecked")
    private void copyState(Object source, Object target, EntityModel<?> model) {
        for (Attribute<?, ?> attr : model.attributes()) {
            if (attr == model.id()) continue;
            try {
                Object value = attr.getter().invoke(source);
                attr.setter().invoke(target, value);
            } catch (Throwable t) {
                throw new IllegalArgumentException("Failed to copy state for attribute " + attr.name(), t);
            }
        }
    }

    private static class EntityCacheKey {
        private final Class<?> entityClass;
        private final Object id;
        private final int hashCode;

        EntityCacheKey(Class<?> entityClass, Object id) {
            this.entityClass = entityClass;
            this.id = id;
            this.hashCode = Objects.hash(entityClass, id);
        }

        @Override public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            EntityCacheKey that = (EntityCacheKey) obj;
            return Objects.equals(entityClass, that.entityClass) && Objects.equals(id, that.id);
        }

        @Override public int hashCode() { return hashCode; }
    }

    // Package-private getters for MansartEntityTransaction
    DataSource getDataSource() { return dataSource; }
    RepositoryRuntime getRepositoryRuntime() { return repositoryRuntime; }
    Dialect getDialect() { return dialect; }
}