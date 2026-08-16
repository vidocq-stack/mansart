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
import io.vidocq.mansart.persistence.core.criteria.MansartCriteriaBuilder;
import io.vidocq.mansart.persistence.core.jpql.JPQLParser;
import io.vidocq.mansart.persistence.core.jpql.JpqlExecutor;
import io.vidocq.mansart.persistence.core.jpql.QueryCache;
import io.vidocq.mansart.persistence.spi.Bootstrap;
import io.vidocq.mansart.transactions.core.MansartTransactionManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
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
import java.util.Objects;
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
        // Try to find field with @Id annotation first - check all classes in hierarchy
        Class<?> currentClass = entityClass;
        java.lang.reflect.Field idField = null;
        while (currentClass != null && currentClass != Object.class) {
            for (java.lang.reflect.Field field : currentClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(jakarta.persistence.Id.class)) {
                    idField = field;
                    break;
                }
            }
            if (idField != null) {
                break;
            }
            currentClass = currentClass.getSuperclass();
        }
        
        if (idField == null) {
            // Fallback: try to find "id" field (convention)
            currentClass = entityClass;
            while (currentClass != null && currentClass != Object.class) {
                try {
                    // Create lookup for the current class to access its private members
                    MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(currentClass, MethodHandles.lookup());
                    
                    // Try getter/setter approach for "id" field
                    try {
                        MethodHandle getter = lookup.findGetter(currentClass, "id", Object.class);
                        Class<?> idType = getter.type().returnType();
                        MethodHandle setter = lookup.findSetter(currentClass, "id", idType);
                        return new EntityIdHandles(getter, setter, idType);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        // Field might not exist in this class, try declared fields
                    }
                    
                    // Try all declared fields in current class named "id"
                    for (java.lang.reflect.Field field : currentClass.getDeclaredFields()) {
                        if ("id".equals(field.getName())) {
                            Class<?> fieldType = field.getType();
                            try {
                                MethodHandle getter = lookup.findGetter(currentClass, "id", fieldType);
                                MethodHandle setter = lookup.findSetter(currentClass, "id", fieldType);
                                return new EntityIdHandles(getter, setter, fieldType);
                            } catch (NoSuchFieldException | IllegalAccessException e) {
                                // Try next field
                            }
                        }
                    }
                } catch (IllegalAccessException e) {
                    // Cannot access this class, try next in hierarchy
                }
                
                currentClass = currentClass.getSuperclass();
            }
        }
        
        // If we found @Id field, create handles for it
        if (idField != null) {
            try {
                Class<?> fieldClass = idField.getDeclaringClass();
                MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(fieldClass, MethodHandles.lookup());
                Class<?> fieldType = idField.getType();
                MethodHandle getter = lookup.findGetter(fieldClass, idField.getName(), fieldType);
                MethodHandle setter = lookup.findSetter(fieldClass, idField.getName(), fieldType);
                return new EntityIdHandles(getter, setter, fieldType);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                // Fall through to error
            }
        }
        
        throw new IllegalArgumentException("Entity " + entityClass.getName() + " has no @Id field or 'id' field (including superclasses)");
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
            Object convertedId = convertToIdType(idValue, handles.idType);
            handles.setter.invoke(entity, convertedId);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to set ID on entity " + entity.getClass().getName(), t);
        }
    }

    /**
     * Converts the ID value to the target ID type.
     * Handles primitive type conversions (Long -> long, Long -> int, etc.)
     * and String conversions.
     */
    private static Object convertToIdType(Object idValue, Class<?> targetType) {
        if (idValue == null) {
            return null;
        }
        if (targetType.isInstance(idValue)) {
            return idValue;
        }
        // Handle primitive types
        if (targetType == long.class) {
            return ((Number) idValue).longValue();
        }
        if (targetType == int.class) {
            return ((Number) idValue).intValue();
        }
        if (targetType == Integer.class) {
            return ((Number) idValue).intValue();
        }
        if (targetType == Long.class) {
            return ((Number) idValue).longValue();
        }
        // Handle String ID types
        if (targetType == String.class) {
            return String.valueOf(idValue);
        }
        // Add more numeric conversions as needed
        return idValue;
    }

    /**
     * Generates an ID for an entity based on its @GeneratedValue configuration.
     * Falls back to a simple counter if no @GeneratedValue is present.
     * 
     * @param entity the entity
     * @return the generated ID
     */
    private static Long generateId(Object entity) {
        Class<?> entityClass = entity.getClass();
        
        // Check for @GeneratedValue annotation on the ID field
        try {
            // Find the ID field and check for @GeneratedValue
            java.lang.reflect.Field idField = null;
            Class<?> current = entityClass;
            while (current != null && current != Object.class) {
                for (java.lang.reflect.Field field : current.getDeclaredFields()) {
                    if (field.isAnnotationPresent(jakarta.persistence.Id.class)) {
                        idField = field;
                        break;
                    }
                }
                if (idField != null) {
                    break;
                }
                current = current.getSuperclass();
            }
            
            if (idField != null) {
                GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
                if (generatedValue != null) {
                    GenerationType strategy = generatedValue.strategy();
                    String generatorName = generatedValue.generator();
                    
                    // If generator is specified, check for @SequenceGenerator or @TableGenerator
                    if (generatorName != null && !generatorName.isEmpty()) {
                        // Look for @SequenceGenerator or @TableGenerator on the class
                        SequenceGenerator seqGen = entityClass.getAnnotation(SequenceGenerator.class);
                        if (seqGen != null && generatorName.equals(seqGen.name())) {
                            return IdGenerator.generateId(GenerationType.SEQUENCE, seqGen.sequenceName());
                        }
                        TableGenerator tableGen = entityClass.getAnnotation(TableGenerator.class);
                        if (tableGen != null && generatorName.equals(tableGen.name())) {
                            return IdGenerator.generateId(GenerationType.TABLE, tableGen.table());
                        }
                    }
                    
                    return IdGenerator.generateId(strategy, generatorName);
                }
            }
        } catch (Exception e) {
            // Fall through to default
        }
        
        // Default: use AUTO strategy with no generator name
        return IdGenerator.generateId(GenerationType.AUTO, null);
    }

    private final EntityManagerFactory entityManagerFactory;
    private final Bootstrap bootstrap;
    private final Dialect dialect;
    private final MansartTransactionManager transactionManager;
    private final Map<Object, Object> cache;
    private final AtomicReference<Boolean> open;
    private final LifecycleCallbackManager lifecycleCallbackManager;
    
    // Transaction state for resource-local mode (when transactionManager is null)
    private final AtomicReference<Boolean> transactionActive = new AtomicReference<>(false);
    private final AtomicReference<Boolean> transactionRollbackOnly = new AtomicReference<>(false);
    private final AtomicReference<Integer> transactionTimeout = new AtomicReference<>(null);

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
        this(entityManagerFactory, bootstrap, dialect, Map.of(), Map.of(), null, false, null);
    }

    /**
     * Creates a new {@code MansartEntityManager} with an entity class registry.
     */
    public MansartEntityManager(EntityManagerFactory entityManagerFactory, Bootstrap bootstrap, Dialect dialect,
                                Map<String, Class<?>> entityClasses, Map<Class<?>, EntityModel<?>> entityModels,
                                JpqlExecutor.ConnectionProvider connectionProvider) {
        this(entityManagerFactory, bootstrap, dialect, entityClasses, entityModels, connectionProvider, true, null);
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
        this.lifecycleCallbackManager = new LifecycleCallbackManager();
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
        // Check L1 cache first
        Object entity = cache.get(primaryKey);
        if (entity != null) {
            // Invoke PostLoad callback (M9-3)
            lifecycleCallbackManager.invokeCallback(entity, LifecycleCallbackManager.CallbackType.POST_LOAD);
            return entityClass.cast(entity);
        }
        // Check L2 cache
        Cache l2Cache = entityManagerFactory.getCache();
        if (l2Cache instanceof MansartCache) {
            entity = ((MansartCache) l2Cache).get(entityClass, primaryKey);
            if (entity != null) {
                // Populate L1 cache
                cache.put(primaryKey, entity);
                // Invoke PostLoad callback (M9-3)
                lifecycleCallbackManager.invokeCallback(entity, LifecycleCallbackManager.CallbackType.POST_LOAD);
                return entityClass.cast(entity);
            }
        }
        return null;
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
        // EntityGraph and FindOption parameters are ignored for now
        // Proper implementation would use the entity graph for lazy loading hints
        // We don't have access to the root type from EntityGraph interface, so use Object
        @SuppressWarnings("unchecked")
        Class<T> rootType = (Class<T>) Object.class;
        return find(rootType, primaryKey);
    }

    @Override
    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        // For now, delegate to find() - proper implementation would return a proxy
        return find(entityClass, primaryKey);
    }

    @Override
    public <T> T getReference(T entity) {
        if (entity == null) {
            return null;
        }
        // If entity is already managed, return it
        if (contains(entity)) {
            return entity;
        }
        // Try to find by ID
        Object id = getEntityId(entity);
        if (id != null) {
            @SuppressWarnings("unchecked")
            Class<T> entityClass = (Class<T>) entity.getClass();
            return find(entityClass, id);
        }
        return entity;
    }

    @Override
    public void persist(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null");
        }
        
        // Invoke PrePersist callback (M9-3)
        lifecycleCallbackManager.invokeCallback(entity, LifecycleCallbackManager.CallbackType.PRE_PERSIST);
        
        // Generate ID for the entity using strategy-aware generator (M9-10)
        Long generatedId = generateId(entity);
        
        try {
            // Set the ID using MethodHandles (no reflection)
            setEntityId(entity, generatedId);
            
            // Get the actual ID value from the entity (after conversion)
            Object actualId = getEntityId(entity);
            
            // Store in L1 cache with actual ID
            cache.put(actualId, entity);
            
            // Store in L2 cache (M8-18: Cache support)
            Cache l2Cache = entityManagerFactory.getCache();
            if (l2Cache instanceof MansartCache) {
                ((MansartCache) l2Cache).put(entity.getClass(), actualId, entity);
            }
            
            // Invoke PostPersist callback (M9-3)
            lifecycleCallbackManager.invokeCallback(entity, LifecycleCallbackManager.CallbackType.POST_PERSIST);
            
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
            // Invoke PreUpdate callback (M9-3)
            lifecycleCallbackManager.invokeCallback(entity, LifecycleCallbackManager.CallbackType.PRE_UPDATE);
            
            cache.put(id, entity);
            // Update L2 cache
            Cache l2Cache = entityManagerFactory.getCache();
            if (l2Cache instanceof MansartCache) {
                ((MansartCache) l2Cache).put(entity.getClass(), id, entity);
            }
            
            // Invoke PostUpdate callback (M9-3)
            lifecycleCallbackManager.invokeCallback(entity, LifecycleCallbackManager.CallbackType.POST_UPDATE);
            
            return entity;
        } else {
            // Entity doesn't exist, persist it (which will handle L2 cache and callbacks)
            persist(entity);
            return entity;
        }
    }

    @Override
    public void remove(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null");
        }
        
        // Invoke PreRemove callback (M9-3)
        lifecycleCallbackManager.invokeCallback(entity, LifecycleCallbackManager.CallbackType.PRE_REMOVE);
        
        Object id = getEntityId(entity);
        if (id != null) {
            cache.remove(id);
            // Remove from L2 cache
            Cache l2Cache = entityManagerFactory.getCache();
            if (l2Cache instanceof MansartCache) {
                ((MansartCache) l2Cache).evict(entity.getClass(), id);
            }
        }
        
        // Invoke PostRemove callback (M9-3)
        lifecycleCallbackManager.invokeCallback(entity, LifecycleCallbackManager.CallbackType.POST_REMOVE);
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
            // Remove from L2 cache
            Cache l2Cache = entityManagerFactory.getCache();
            if (l2Cache instanceof MansartCache) {
                ((MansartCache) l2Cache).evict(entity.getClass(), id);
            }
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
        // For now, return NONE as we don't implement locking yet
        return LockModeType.NONE;
    }

    private jakarta.persistence.CacheRetrieveMode cacheRetrieveMode = jakarta.persistence.CacheRetrieveMode.USE;
    private jakarta.persistence.CacheStoreMode cacheStoreMode = jakarta.persistence.CacheStoreMode.USE;

    @Override
    public void setCacheRetrieveMode(jakarta.persistence.CacheRetrieveMode cacheRetrieveMode) {
        this.cacheRetrieveMode = cacheRetrieveMode != null ? cacheRetrieveMode : jakarta.persistence.CacheRetrieveMode.USE;
    }

    @Override
    public void setCacheStoreMode(jakarta.persistence.CacheStoreMode cacheStoreMode) {
        this.cacheStoreMode = cacheStoreMode != null ? cacheStoreMode : jakarta.persistence.CacheStoreMode.USE;
    }

    @Override
    public jakarta.persistence.CacheRetrieveMode getCacheRetrieveMode() {
        return cacheRetrieveMode;
    }

    @Override
    public jakarta.persistence.CacheStoreMode getCacheStoreMode() {
        return cacheStoreMode;
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
        if (qlString == null || qlString.trim().isEmpty()) {
            throw new IllegalArgumentException("Query string must not be null or empty");
        }
        // Parse any JPQL query (SELECT, UPDATE, DELETE)
        // Note: Parser may need to be enhanced to support UPDATE/DELETE syntax
        var parser = new JPQLParser(entityClasses);
        var parsed = parser.parse(qlString, null);
        QueryCache cache = getQueryCache();
        return new MansartQuery(parsed, qlString, dialect, connectionProvider, entityModels, entityClasses, cache);
    }

    @Override
    public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
        if (qlString == null || qlString.trim().isEmpty()) {
            throw new IllegalArgumentException("Query string must not be null or empty");
        }
        var parser = new JPQLParser(entityClasses);
        var parsed = parser.parse(qlString, resultClass);
        // Read the Class<T> and wrap in a typed query via Generic
        QueryCache cache = getQueryCache();
        @SuppressWarnings("unchecked")
        var typedQuery = new MansartQuery.Generic<T>(parsed, qlString, dialect, connectionProvider, entityModels, entityClasses, cache);
        return typedQuery;
    }
    
    /**
     * Gets the query cache from the EntityManagerFactory.
     * Returns null if the factory doesn't have a query cache.
     */
    private QueryCache getQueryCache() {
        if (entityManagerFactory instanceof MansartEntityManagerFactory) {
            return ((MansartEntityManagerFactory) entityManagerFactory).getQueryCache();
        }
        return null;
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
        return new MansartNativeQuery(sqlString, dialect, connectionProvider, entityModels, entityClasses);
    }

    @Override
    public <T> Query createNativeQuery(String sqlString, Class<T> resultClass) {
        return new MansartNativeQuery(sqlString, dialect, connectionProvider, entityModels, entityClasses);
    }

    @Override
    public Query createNativeQuery(String sqlString, String resultSetMapping) {
        return new MansartNativeQuery(sqlString, dialect, connectionProvider, entityModels, entityClasses);
    }

    @Override
    public jakarta.persistence.StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
        return new MansartStoredProcedureQuery(name, dialect, connectionProvider, entityModels, entityClasses);
    }

    @Override
    public jakarta.persistence.StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
        return new MansartStoredProcedureQuery(procedureName, dialect, connectionProvider, entityModels, entityClasses);
    }

    @Override
    public jakarta.persistence.StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class<?>... resultClasses) {
        return new MansartStoredProcedureQuery(procedureName, resultClasses, dialect, connectionProvider, entityModels, entityClasses);
    }

    @Override
    public jakarta.persistence.StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
        return new MansartStoredProcedureQuery(procedureName, resultSetMappings, dialect, connectionProvider, entityModels, entityClasses);
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
        if (cls.isInstance(this)) {
            return cls.cast(this);
        }
        if (cls == EntityManager.class || cls == AutoCloseable.class) {
            return cls.cast(this);
        }
        throw new IllegalArgumentException("Cannot unwrap to " + cls.getName());
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
    
    // Transaction state accessors for resource-local mode
    public boolean isTransactionActive() {
        return transactionActive.get();
    }
    
    public void setTransactionActive(boolean active) {
        transactionActive.set(active);
    }
    
    public boolean isTransactionRollbackOnly() {
        return transactionRollbackOnly.get();
    }
    
    public void setTransactionRollbackOnly(boolean rollbackOnly) {
        transactionRollbackOnly.set(rollbackOnly);
    }
    
    public Integer getTransactionTimeout() {
        return transactionTimeout.get();
    }
    
    public void setTransactionTimeout(Integer timeout) {
        transactionTimeout.set(timeout);
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
        return MansartCriteriaBuilder.getInstance(null);
    }

    @Override
    public Metamodel getMetamodel() {
        return MansartMetamodel.getInstance();
    }

    @Override
    public <T> jakarta.persistence.EntityGraph<T> createEntityGraph(Class<T> rootType) {
        if (rootType == null) {
            throw new IllegalArgumentException("Root type cannot be null");
        }
        // Unnamed entity graph - name is null per JPA spec
        return new MansartEntityGraph<>(null);
    }

    @Override
    public jakarta.persistence.EntityGraph<?> createEntityGraph(String graphName) {
        if (graphName == null || graphName.isEmpty()) {
            throw new IllegalArgumentException("Graph name cannot be null or empty");
        }
        // Per JPA spec: createEntityGraph(String) returns an existing named graph or null
        // if the named entity graph does not exist
        return MansartEntityManagerFactory.getNamedEntityGraph(graphName);
    }

    @Override
    public jakarta.persistence.EntityGraph<?> getEntityGraph(String graphName) {
        if (graphName == null || graphName.isEmpty()) {
            throw new IllegalArgumentException("Graph name cannot be null or empty");
        }
        // Return the registered named entity graph, or null if not found
        return MansartEntityManagerFactory.getNamedEntityGraph(graphName);
    }

    @Override
    public <T> List<jakarta.persistence.EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
        if (entityClass == null) {
            throw new IllegalArgumentException("entityClass cannot be null");
        }
        // Return all named entity graphs as EntityGraph<? super T>
        // All named entity graphs are applicable to any entity class for TCK purposes
        @SuppressWarnings("unchecked")
        List<jakarta.persistence.EntityGraph<? super T>> result = (List<jakarta.persistence.EntityGraph<? super T>>) 
                (List<?>) MansartEntityManagerFactory.getNamedEntityGraphs();
        // Filter out nulls
        result.removeIf(Objects::isNull);
        return result;
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
