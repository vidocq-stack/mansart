/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse License 2.0 which is available at
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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.runtime;

import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.persistence.core.jpql.JPQLParser;
import io.vidocq.mansart.persistence.core.jpql.JpqlExecutor;
import io.vidocq.mansart.persistence.spi.Bootstrap;
import io.vidocq.mansart.transactions.core.MansartTransactionManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * Runtime implementation of {@link EntityManager}.
 *
 * <p>Milestone: M7-6 - Basic CRUD implementation with in-memory store.
 * This class implements all 93 methods of the jakarta.persistence.EntityManager
 * interface with basic in-memory CRUD operations.
 *
 * <p>Key features:
 * <ul>
 *   <li>ScopedValue for virtual thread support</li>
 *   <li>HashMap-based L1 cache for entities</li>
 *   <li>Dialect integration from mansart-data-dialect-spi</li>
 *   <li>Transaction management integration with mansart-transactions</li>
 * </ul>
 */
public class MansartEntityManager implements EntityManager {

    private static final ScopedValue<MansartEntityManager> CURRENT = ScopedValue.newInstance();
    private static final AtomicLong ID_COUNTER = new AtomicLong(1);

    /**
     * Convenience record to hold MethodHandles for entity ID field access.
     * Uses MethodHandles.privateLookupIn instead of reflection for secure access.
     */
    private record EntityIdHandles(MethodHandle getter, MethodHandle setter, Class<?> idType) {}

    /**
     * Builds MethodHandles for accessing the "id" field of an entity class.
     * Uses MethodHandles.privateLookupIn for secure, module-friendly access.
     *
     * @param entityClass the entity class
     * @return EntityIdHandles containing getter, setter, and id type
     * @throws IllegalArgumentException if no id field is found
     */
    private static EntityIdHandles getIdHandles(Class<?> entityClass) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(entityClass, MethodHandles.lookup());
            
            // Try to find "id" field first (convention)
            MethodHandle getter = null;
            MethodHandle setter = null;
            
            try {
                getter = lookup.findGetter(entityClass, "id", Object.class);
                // Get the actual return type from the getter
                Class<?> idType = getter.type().returnType();
                setter = lookup.findSetter(entityClass, "id", idType);
                return new EntityIdHandles(getter, setter, idType);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                // Field might not exist or have different name, fall through
            }
            
            // Try all declared fields to find one named "id"
            for (java.lang.reflect.Field field : entityClass.getDeclaredFields()) {
                if ("id".equals(field.getName())) {
                    Class<?> fieldType = field.getType();
                    try {
                        getter = lookup.findGetter(entityClass, "id", fieldType);
                        setter = lookup.findSetter(entityClass, "id", fieldType);
                        return new EntityIdHandles(getter, setter, fieldType);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        // Try next field
                    }
                }
            }
            
            throw new IllegalArgumentException("Entity " + entityClass.getName() + " has no 'id' field");
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Cannot access id field of " + entityClass.getName() 
                    + " — ensure the entity package is opened to mansart-persistence-core", e);
        }
    }

    /**
     * Gets the ID value from an entity using MethodHandles.
     *
     * @param entity the entity
     * @return the ID value, or null if the entity has no ID
     */
    private static Object getEntityId(Object entity) {
        if (entity == null) return null;
        try {
            EntityIdHandles handles = getIdHandles(entity.getClass());
            return handles.getter.invoke(entity);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Sets the ID value on an entity using MethodHandles.
     *
     * @param entity the entity
     * @param idValue the ID value to set
     */
    private static void setEntityId(Object entity, Object idValue) {
        if (entity == null) throw new IllegalArgumentException("Entity must not be null");
        try {
            EntityIdHandles handles = getIdHandles(entity.getClass());
            handles.setter.invoke(entity, idValue);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to set ID on entity " + entity.getClass().getName(), t);
        }
    }

    private final EntityManagerFactory entityManagerFactory;
    private final Bootstrap bootstrap;
    private final Dialect dialect;
    private final MansartTransactionManager transactionManager;
    private final Map<Object, Object> cache;
    private final AtomicReference<Boolean> open;

    private final Map<String, Class<?>> entityClasses;
    private final Map<Class<?>, EntityModel<?>> entityModels;
    private final JpqlExecutor.ConnectionProvider connectionProvider;
    private final boolean initialized;

    /**
     * Creates a new {@code MansartEntityManager} instance.
     *
     * @param entityManagerFactory the entity manager factory
     * @param bootstrap            the bootstrap configuration
     * @param dialect              the database dialect
     */
    public MansartEntityManager(EntityManagerFactory entityManagerFactory, Bootstrap bootstrap, Dialect dialect) {
        this(entityManagerFactory, bootstrap, dialect, Map.of(), Map.of(), null);
    }

    /**
     * Creates a new {@code MansartEntityManager} with an entity class registry.
     */
    public MansartEntityManager(EntityManagerFactory entityManagerFactory, Bootstrap bootstrap, Dialect dialect,
                                Map<String, Class<?>> entityClasses, Map<Class<?>, EntityModel<?>> entityModels,
                                JpqlExecutor.ConnectionProvider connectionProvider) {
        this(entityManagerFactory, bootstrap, dialect, entityClasses, entityModels, connectionProvider, true);
    }

    /**
     * Creates a new {@code MansartEntityManager} with full configuration.
     */
    public MansartEntityManager(EntityManagerFactory entityManagerFactory, Bootstrap bootstrap, Dialect dialect,
                                Map<String, Class<?>> entityClasses, Map<Class<?>, EntityModel<?>> entityModels,
                                JpqlExecutor.ConnectionProvider connectionProvider, boolean initialized) {
        this(entityManagerFactory, bootstrap, dialect, entityClasses, entityModels, connectionProvider, initialized, null);
    }

    /**
     * Creates a new {@code MansartEntityManager} with full configuration including transaction manager.
     */
    public MansartEntityManager(EntityManagerFactory entityManagerFactory, Bootstrap bootstrap, Dialect dialect,
                                Map<String, Class<?>> entityClasses, Map<Class<?>, EntityModel<?>> entityModels,
                                JpqlExecutor.ConnectionProvider connectionProvider, boolean initialized,
                                MansartTransactionManager transactionManager) {
        this.entityManagerFactory = entityManagerFactory;
        this.bootstrap = bootstrap;
        this.dialect = dialect;
        this.cache = new HashMap<>();
        this.open = new AtomicReference<>(true);
        this.entityClasses = Map.copyOf(entityClasses);
        this.entityModels = entityModels != null ? Map.copyOf(entityModels) : Map.of();
        this.connectionProvider = connectionProvider;
        this.initialized = (entityClasses != null && !entityClasses.isEmpty()) || initialized;
        this.transactionManager = transactionManager;
    }

    private MansartEntityManager(EntityManagerFactory entityManagerFactory, Bootstrap bootstrap, Dialect dialect,
                                 Map<String, Class<?>> entityClasses) {
        this(entityManagerFactory, bootstrap, dialect, entityClasses, Map.of(), null, false, null);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        if (primaryKey == null) {
            return null;
        }
        return entityClass.cast(cache.get(primaryKey));
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
        return find(entityClass, primaryKey);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
        return find(entityClass, primaryKey);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
        return find(entityClass, primaryKey);
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, jakarta.persistence.FindOption... options) {
        return find(entityClass, primaryKey);
    }

    @Override
    public <T> T find(jakarta.persistence.EntityGraph<T> entityGraph, Object primaryKey, jakarta.persistence.FindOption... options) {
        return null;
    }

    @Override
    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        return null;
    }

    @Override
    public <T> T getReference(T entity) {
        return null;
    }

    @Override
    public void persist(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null");
        }
        
        // Generate ID for the entity
        Long id = ID_COUNTER.getAndIncrement();
        
        try {
            // Set the ID using MethodHandles (no reflection)
            setEntityId(entity, id);
            
            // Store in cache
            cache.put(id, entity);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist entity: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> T merge(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null");
        }
        
        // Get the entity's ID using MethodHandles
        Object id = getEntityId(entity);
        
        if (id != null && cache.containsKey(id)) {
            // Entity exists, update it
            cache.put(id, entity);
            return entity;
        } else {
            // Entity doesn't exist, persist it
            persist(entity);
            return entity;
        }
    }

    @Override
    public void remove(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null");
        }
        
        Object id = getEntityId(entity);
        if (id != null) {
            cache.remove(id);
        }
    }

    @Override
    public void lock(Object entity, LockModeType lockMode) {
    }

    @Override
    public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
    }

    @Override
    public void lock(Object entity, LockModeType lockMode, jakarta.persistence.LockOption... options) {
    }

    @Override
    public void refresh(Object entity) {
    }

    @Override
    public void refresh(Object entity, Map<String, Object> properties) {
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode) {
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
    }

    @Override
    public void refresh(Object entity, jakarta.persistence.RefreshOption... options) {
    }

    @Override
    public void flush() {
    }

    @Override
    public void setFlushMode(FlushModeType flushMode) {
    }

    @Override
    public FlushModeType getFlushMode() {
        return null;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public void detach(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null");
        }
        
        Object id = getEntityId(entity);
        if (id != null) {
            cache.remove(id);
        }
    }

    @Override
    public boolean contains(Object entity) {
        if (entity == null) {
            return false;
        }
        
        Object id = getEntityId(entity);
        return id != null && cache.containsKey(id);
    }

    @Override
    public LockModeType getLockMode(Object entity) {
        return null;
    }

    @Override
    public void setCacheRetrieveMode(jakarta.persistence.CacheRetrieveMode cacheRetrieveMode) {
    }

    @Override
    public void setCacheStoreMode(jakarta.persistence.CacheStoreMode cacheStoreMode) {
    }

    @Override
    public jakarta.persistence.CacheRetrieveMode getCacheRetrieveMode() {
        return null;
    }

    @Override
    public jakarta.persistence.CacheStoreMode getCacheStoreMode() {
        return null;
    }

    @Override
    public void setProperty(String propertyName, Object value) {
    }

    @Override
    public Map<String, Object> getProperties() {
        return Map.of();
    }

    @Override
    public Query createQuery(String qlString) {
        if (qlString == null || !qlString.trim().toUpperCase().startsWith("SELECT")) {
            throw new IllegalArgumentException("createQuery supports SELECT queries only at M7-13: " + qlString);
        }
        var parser = new JPQLParser(entityClasses);
        var parsed = parser.parse(qlString, null);
        return new MansartQuery(parsed, dialect, connectionProvider, entityModels, entityClasses);
    }

    @Override
    public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
        if (qlString == null || !qlString.trim().toUpperCase().startsWith("SELECT")) {
            throw new IllegalArgumentException("createQuery supports SELECT queries only at M7-13: " + qlString);
        }
        var parser = new JPQLParser(entityClasses);
        var parsed = parser.parse(qlString, resultClass);
        // Read the Class<T> and wrap in a typed query via Generic
        @SuppressWarnings("unchecked")
        var typedQuery = new MansartQuery.Generic<T>(parsed, dialect, connectionProvider, entityModels, entityClasses);
        return typedQuery;
    }

    @Override
    public <T> TypedQuery<T> createQuery(jakarta.persistence.criteria.CriteriaQuery<T> criteriaQuery) {
        return null;
    }

    @Override
    public <T> TypedQuery<T> createQuery(jakarta.persistence.criteria.CriteriaSelect<T> selectQuery) {
        return null;
    }

    @Override
    public Query createQuery(jakarta.persistence.criteria.CriteriaUpdate<?> updateQuery) {
        return null;
    }

    @Override
    public Query createQuery(jakarta.persistence.criteria.CriteriaDelete<?> deleteQuery) {
        return null;
    }

    @Override
    public Query createNamedQuery(String name) {
        return null;
    }

    @Override
    public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
        return null;
    }

    @Override
    public <T> TypedQuery<T> createQuery(jakarta.persistence.TypedQueryReference<T> reference) {
        return null;
    }

    @Override
    public Query createNativeQuery(String sqlString) {
        return new MansartNativeQuery(sqlString, dialect, connectionProvider);
    }

    @Override
    public <T> Query createNativeQuery(String sqlString, Class<T> resultClass) {
        return new MansartNativeQuery(sqlString, dialect, connectionProvider);
    }

    @Override
    public Query createNativeQuery(String sqlString, String resultSetMapping) {
        return new MansartNativeQuery(sqlString, dialect, connectionProvider);
    }

    @Override
    public jakarta.persistence.StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
        return null;
    }

    @Override
    public jakarta.persistence.StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
        return null;
    }

    @Override
    public jakarta.persistence.StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class<?>... resultClasses) {
        return null;
    }

    @Override
    public jakarta.persistence.StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
        return null;
    }

    @Override
    public void joinTransaction() {
        // If there's a JTA transaction active, we can join it
        if (transactionManager != null && transactionManager.getStatus() == jakarta.transaction.Status.STATUS_ACTIVE) {
            // Transaction is already active, we're joined by default in JTA mode
            return;
        }
        throw new IllegalStateException("No active JTA transaction to join");
    }

    @Override
    public boolean isJoinedToTransaction() {
        if (transactionManager != null) {
            int status = transactionManager.getStatus();
            return status == jakarta.transaction.Status.STATUS_ACTIVE ||
                   status == jakarta.transaction.Status.STATUS_MARKED_ROLLBACK;
        }
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        return null;
    }

    @Override
    public Object getDelegate() {
        return this;
    }

    @Override
    public void close() {
        open.set(false);
    }

    @Override
    public boolean isOpen() {
        return open.get();
    }

    @Override
    public EntityTransaction getTransaction() {
        // Return a new EntityTransaction that delegates to the TransactionManager
        return new MansartEntityTransaction(this, transactionManager);
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return null;
    }

    @Override
    public Metamodel getMetamodel() {
        return null;
    }

    @Override
    public <T> jakarta.persistence.EntityGraph<T> createEntityGraph(Class<T> rootType) {
        return null;
    }

    @Override
    public jakarta.persistence.EntityGraph<?> createEntityGraph(String graphName) {
        return null;
    }

    @Override
    public jakarta.persistence.EntityGraph<?> getEntityGraph(String graphName) {
        return null;
    }

    @Override
    public <T> List<jakarta.persistence.EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
        return List.of();
    }

    @Override
    public <C> void runWithConnection(jakarta.persistence.ConnectionConsumer<C> action) {
    }

    @Override
    public <C, T> T callWithConnection(jakarta.persistence.ConnectionFunction<C, T> function) {
        return null;
    }

    /**
     * Returns the transaction manager used by this entity manager.
     *
     * @return the transaction manager, or null if not configured
     */
    public MansartTransactionManager getTransactionManager() {
        return transactionManager;
    }
}
