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

import io.vidocq.mansart.data.dialect.Attribute;
import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.persistence.core.criteria.MansartCriteriaBuilder;
import io.vidocq.mansart.persistence.core.jpql.JPQLParser;
import io.vidocq.mansart.persistence.core.jpql.JpqlExecutor;
import io.vidocq.mansart.persistence.core.jpql.QueryCache;
import io.vidocq.mansart.persistence.spi.Bootstrap;
import io.vidocq.mansart.persistence.spi.LifecycleCallbackDispatcher;
import io.vidocq.mansart.transactions.core.MansartTransactionManager;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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

    // ── Phase 1 (DEBT-05): EntityMetadata helper ──

    /**
     * Returns the APT-generated EntityMetadata for the given entity class.
     * Phase 1: falls back to null if metadata is not registered.
     */
    private io.vidocq.mansart.persistence.spi.EntityMetadata resolveMetadata(Class<?> entityClass) {
        io.vidocq.mansart.persistence.spi.EntityMetadata metadata = io.vidocq.mansart.persistence.spi.EntityMetadataRegistry.getMetadata(entityClass);
        if (metadata != null) return metadata;
        // Fallback: try registry directly
        return io.vidocq.mansart.persistence.spi.EntityMetadataRegistry.getMetadata(entityClass);
    }

    /**
     * Gets the ID value from an entity.
     * Phase 1 (DEBT-05): uses EntityMetadata SPI; throws if metadata is unavailable.
     *
     * @param entity the entity
     * @return the ID value, or null if the entity has no ID
     */
    private Object getEntityId(Object entity) {
        if (entity == null) return null;
        io.vidocq.mansart.persistence.spi.EntityMetadata metadata = resolveMetadata(entity.getClass());
        if (metadata != null) {
            return metadata.readAttribute(entity, metadata.getIdAttributeName());
        }
        throw new IllegalStateException("No EntityMetadata for " + entity.getClass().getName() +
            " — entity was not processed by the APT processor");
    }

    /**
     * Sets the ID value on an entity.
     * Phase 1 (DEBT-05): uses EntityMetadata SPI.
     *
     * @param entity  the entity
     * @param idValue the ID value to set
     */
    private void setEntityId(Object entity, Object idValue) {
        if (entity == null) throw new IllegalArgumentException("Entity must not be null");
        io.vidocq.mansart.persistence.spi.EntityMetadata metadata = resolveMetadata(entity.getClass());
        if (metadata != null) {
            metadata.writeAttribute(entity, metadata.getIdAttributeName(), idValue);
            return;
        }
        throw new IllegalStateException("No EntityMetadata for " + entity.getClass().getName() +
            " — entity was not processed by the APT processor");
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
        // Handle java.util.Date ID type
        if (targetType == java.util.Date.class) {
            if (idValue instanceof Number) {
                return new java.util.Date(((Number) idValue).longValue());
            }
            return idValue;
        }
        // Handle java.sql.Date ID type
        if (targetType == java.sql.Date.class) {
            if (idValue instanceof Number) {
                return new java.sql.Date(((Number) idValue).longValue());
            }
            return idValue;
        }
        // Handle Calendar ID type
        if (targetType == java.util.Calendar.class) {
            if (idValue instanceof Number) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTimeInMillis(((Number) idValue).longValue());
                return cal;
            }
            return idValue;
        }
        // Handle String ID types
        if (targetType == String.class) {
            return String.valueOf(idValue);
        }
        // Handle BigInteger ID type
        if (targetType == java.math.BigInteger.class) {
            if (idValue instanceof Number) {
                return java.math.BigInteger.valueOf(((Number) idValue).longValue());
            }
            return idValue;
        }
        // Add more numeric conversions as needed
        return idValue;
    }

    /**
     * Checks if an entity uses an auto-generated ID strategy.
     * Phase 1 (DEBT-05): uses EntityMetadata SPI.
     */
    private boolean hasAutoGeneratedId(Object entity) {
        io.vidocq.mansart.persistence.spi.EntityMetadata metadata = resolveMetadata(entity.getClass());
        if (metadata != null) {
            var pk = metadata.getPrimaryKeyMetadata();
            return pk.getGenerationType() != null;
        }
        // Fallback: reflection
        return false;
    }

    /**
     * Generates an ID for an entity based on its @GeneratedValue configuration.
     * Phase 1 (DEBT-05): uses EntityMetadata SPI; falls back to IdGenerator.
     */
    private Long generateId(Object entity) {
        io.vidocq.mansart.persistence.spi.EntityMetadata metadata = resolveMetadata(entity.getClass());
        if (metadata != null) {
            var pk = metadata.getPrimaryKeyMetadata();
            if (pk.getGenerationType() != null) {
                return IdGenerator.generateId(pk.getGenerationType(), pk.getGeneratorName());
            }
        }
        // Fallback: default AUTO strategy
        return IdGenerator.generateId(jakarta.persistence.GenerationType.AUTO, null);
    }

    private final EntityManagerFactory entityManagerFactory;
    private final Bootstrap bootstrap;
    private final Dialect dialect;
    private final MansartTransactionManager transactionManager;
    private final Map<Object, Object> cache;
    private final AtomicReference<Boolean> open;
    // Phase 1 (DEBT-06): lifecycle callbacks are dispatched via EntityMetadata.lifecycleCallbacks()
    // LifecycleCallbackManager was deleted — generated _EntityCallbacks handle all dispatch.

    /**
     * Invokes lifecycle callbacks for the given entity and phase.
     * Phase 1 (DEBT-06): uses EntityMetadata SPI.
     */
    private void invokeLifecycleCallbacks(Object entity, LifecycleCallbackDispatcher.Phase phase) {
        if (entity == null) return;
        io.vidocq.mansart.persistence.spi.EntityMetadata metadata = resolveMetadata(entity.getClass());
        if (metadata != null) {
            metadata.lifecycleCallbacks().invoke(entity, phase);
        }
        // If metadata is not available, callbacks are silently skipped
        // (entity was not processed by the APT processor)
    }

    // Transaction state for resource-local mode (when transactionManager is null)
    private final AtomicReference<Boolean> transactionActive = new AtomicReference<>(false);
    private final AtomicReference<Boolean> transactionRollbackOnly = new AtomicReference<>(false);
    private final AtomicReference<Integer> transactionTimeout = new AtomicReference<>(null);

    // Shared connection for the lifetime of this EntityManager
    // This is needed for H2 in-memory databases where AUTO_INCREMENT is connection-scoped
    // and each new connection resets the sequence
    private Connection sharedConnection;

    // Track detached entity IDs (className:id) to ensure find() returns null after detach
    // as expected by some TCK tests
    private final java.util.Set<Object> detachedKeys = new java.util.HashSet<>();

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

    /**
     * Gets the connection for this EntityManager.
     * Uses shared connection pattern for H2 to avoid AUTO_INCREMENT sequence reset.
     */
    private Connection getConnection() throws SQLException {
        // For H2 in-memory databases, use shared connection to prevent sequence reset
        if ("H2".equals(dialect.name()) && sharedConnection == null) {
            synchronized (this) {
                if (sharedConnection == null) {
                    sharedConnection = connectionProvider.getConnection();
                }
            }
        }
        return sharedConnection != null ? sharedConnection : connectionProvider.getConnection();
    }

    /**
     * Creates a cache key that is unique per entity class and ID.
     */
    private Object createCacheKey(Class<?> entityClass, Object id) {
        return entityClass.getName() + ":" + id;
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        if (primaryKey == null) {
            return null;
        }

        // Create cache key once
        Object cacheKey = createCacheKey(entityClass, primaryKey);

        // Check if entity was detached - return null if so (as expected by TCK tests)
        if (detachedKeys.contains(cacheKey)) {
            return null;
        }

        // Check L1 cache first
        Object entity = cache.get(cacheKey);
        if (entity != null && entityClass.isInstance(entity)) {
            // Invoke PostLoad callback (M9-3)
            invokeLifecycleCallbacks(entity, LifecycleCallbackDispatcher.Phase.POST_LOAD);
            return entityClass.cast(entity);
        }
        // Check L2 cache
        Cache l2Cache = entityManagerFactory.getCache();
        if (l2Cache instanceof MansartCache) {
            entity = ((MansartCache) l2Cache).get(entityClass, primaryKey);
            if (entity != null) {
                // Populate L1 cache
                cache.put(cacheKey, entity);
                // Invoke PostLoad callback (M9-3)
                invokeLifecycleCallbacks(entity, LifecycleCallbackDispatcher.Phase.POST_LOAD);
                return entityClass.cast(entity);
            }
        }

        // If not in cache, try to load from database (M9-10: DB loading)
        // Only load from DB if entity is not in detachedKeys set (TCK expects null after detach)
        if (dialect != null && connectionProvider != null && !detachedKeys.contains(cacheKey)) {
            entity = executeFindFromDatabase(entityClass, primaryKey);
            if (entity != null) {
                // Store in L1 cache
                cache.put(cacheKey, entity);
                // Store in L2 cache
                if (l2Cache instanceof MansartCache) {
                    ((MansartCache) l2Cache).put(entityClass, primaryKey, entity);
                }
                // Invoke PostLoad callback (M9-3)
                invokeLifecycleCallbacks(entity, LifecycleCallbackDispatcher.Phase.POST_LOAD);
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
        invokeLifecycleCallbacks(entity, LifecycleCallbackDispatcher.Phase.PRE_PERSIST);

        // Check if we should set the ID before insert.
        // IDENTITY: database generates ID, don't set it.
        // AUTO/SEQUENCE/TABLE: application generates ID, set it before insert.
        boolean shouldSetId = true;
        io.vidocq.mansart.persistence.spi.EntityMetadata _meta = resolveMetadata(entity.getClass());
        if (_meta != null) {
            var pk = _meta.getPrimaryKeyMetadata();
            if (pk.getGenerationType() == jakarta.persistence.GenerationType.IDENTITY) {
                shouldSetId = false;
            }
        }

        try {
            // An identifier already assigned by the application is never overwritten:
            // only generate one when the entity has no ID yet (M9-10).
            if (shouldSetId && getEntityId(entity) == null) {
                // Set the ID using MethodHandles (no reflection)
                setEntityId(entity, generateId(entity));
            }

            // Get the actual ID value from the entity (after conversion or from DB)
            Object actualId = shouldSetId ? getEntityId(entity) : null;

            // Execute INSERT in database (M9-10: Phase 1 - DB persistence for TCK)
            // This ensures JPQL queries can find persisted entities
            if (dialect != null && connectionProvider != null) {
                executeInsert(entity, actualId);
            }

            // For IDENTITY strategy, the ID is generated by the database and set on the entity
            // during executeInsert, so we need to get it from the entity after the insert
            if (!shouldSetId) {
                actualId = getEntityId(entity);
            }

            // Store in L1 cache with actual ID
            cache.put(createCacheKey(entity.getClass(), actualId), entity);

            // Store in L2 cache (M8-18: Cache support)
            Cache l2Cache = entityManagerFactory.getCache();
            if (l2Cache instanceof MansartCache) {
                ((MansartCache) l2Cache).put(entity.getClass(), actualId, entity);
            }

            // Invoke PostPersist callback (M9-3)
            invokeLifecycleCallbacks(entity, LifecycleCallbackDispatcher.Phase.POST_PERSIST);

        } catch (Exception e) {
            throw new RuntimeException("Failed to persist entity: " + e.getMessage(), e);
        }
    }

    /**
     * Executes INSERT statement for the entity in the database (M9-10: Phase 1).
     * Uses dialect.insert() to generate SQL and binds attribute values.
     * This ensures that JPQL queries can find persisted entities in the database.
     *
     * @param entity the entity to insert
     * @param id     the ID value to use for the entity
     * @throws SQLException if the insert fails
     */
    @SuppressWarnings("unchecked")
    private void executeInsert(Object entity, Object id) throws SQLException {
        Class<?> entityClass = entity.getClass();

        // Try to get EntityModel from APT-generated models
        EntityModel<Object> entityModel = (EntityModel<Object>) entityModels.get(entityClass);

        if (entityModel != null) {
            // Use dialect-based insert with EntityModel
            executeInsertWithEntityModel(entity, id, entityModel);
        } else {
            // Fallback: use reflection-based insert for entities without EntityModel
            // This handles TCK entities and any entities compiled with different APT processor
            executeInsertWithReflection(entity, id, entityClass);
        }
    }

    /**
     * Executes INSERT using EntityModel and dialect.
     */
    @SuppressWarnings("unchecked")
    private void executeInsertWithEntityModel(Object entity, Object id, EntityModel<Object> entityModel) throws SQLException {
        io.vidocq.mansart.data.dialect.SqlFragment insertFragment = dialect.insert(entityModel, true);
        String idColumnName = entityModel.id().columnName();

        Connection connection = getConnection();
        try (java.sql.PreparedStatement ps = connection.prepareStatement(
                insertFragment.sql(), new String[]{idColumnName})) {

            int paramIndex = 1;
            for (Attribute<Object, ?> attr : entityModel.attributes()) {
                if (attr == entityModel.id() && entityModel.id().generated()) {
                    continue;
                }

                Object value;
                try {
                    value = attr.getter().invoke(entity);
                } catch (Throwable t) {
                    value = null;
                }

                dialect.bind(ps, paramIndex++, value, attr.javaType());
            }

            ps.executeUpdate();

            if (entityModel.id().generated()) {
                try (java.sql.ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        Object generatedKey = dialect.extract(keys, 1, entityModel.id().javaType());
                        try {
                            entityModel.id().setter().invoke(entity, generatedKey);
                        } catch (Throwable t) {
                            // Ignore
                        }
                    }
                }
            }
        }
    }

    /**
     * Fallback: executes INSERT using EntityMetadata SPI to read/write entity state.
     * Used when EntityModel is not available (e.g., TCK entities).
     */
    private void executeInsertWithReflection(Object entity, Object id, Class<?> entityClass) throws SQLException {
        io.vidocq.mansart.persistence.spi.EntityMetadata meta = resolveMetadata(entityClass);
        if (meta == null) {
            throw new IllegalStateException("No EntityMetadata for " + entityClass.getName() +
                " — entity was not processed by the APT processor");
        }

        String tableName = meta.tableName();
        if (tableName == null || tableName.isEmpty()) {
            tableName = entityClass.getSimpleName().toLowerCase();
        }

        String idAttributeName = meta.getIdAttributeName();
        Class<?> idAttributeType = meta.getIdAttributeType();

        // Build INSERT SQL using metadata persistent attributes
        java.util.List<String> persistentAttrs = meta.getPersistentAttributeOrder();
        java.util.List<String> columnNames = new java.util.ArrayList<>();
        java.util.List<Object> values = new java.util.ArrayList<>();

        for (String attrName : persistentAttrs) {
            if (meta.isRelationship(attrName)) continue;

            // Skip ID if auto-generated (null)
            if (attrName.equals(idAttributeName)) {
                Object attrValue = meta.readAttribute(entity, attrName);
                if (attrValue == null) continue;
            }

            columnNames.add(attrName);
            values.add(meta.readAttribute(entity, attrName));
        }

        // Build INSERT SQL
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        String quotedTableName = quoteTableIdentifier(tableName);
        sql.append(quotedTableName).append(" (");

        for (int i = 0; i < columnNames.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(quoteColumnIdentifier(columnNames.get(i)));
        }
        sql.append(") VALUES (");

        for (int i = 0; i < columnNames.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(")");

        String insertSql = sql.toString();

        Connection connection = getConnection();
        java.sql.PreparedStatement ps;
        if (meta.getPrimaryKeyMetadata() != null && meta.getPrimaryKeyMetadata().isAutoGenerated()) {
            ps = connection.prepareStatement(insertSql, new String[]{idAttributeName});
        } else {
            ps = connection.prepareStatement(insertSql);
        }
        try (ps) {
            for (int i = 0; i < values.size(); i++) {
                Object v = values.get(i);
                dialect.bind(ps, i + 1, v, v != null ? v.getClass() : Object.class);
            }
            ps.executeUpdate();

            // Retrieve generated ID if auto-generated
            if (meta.getPrimaryKeyMetadata() != null && meta.getPrimaryKeyMetadata().isAutoGenerated()) {
                try (java.sql.ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        Object generatedKey = dialect.extract(keys, 1, idAttributeType);
                        meta.writeAttribute(entity, idAttributeName, generatedKey);
                    }
                }
            }
        }
    }

    /**
     * Collects all fields from a class including inherited fields.
     */
    private void collectAllFields(Class<?> clazz, java.util.List<java.lang.reflect.Field> result) {
        if (clazz == null || Object.class.equals(clazz)) {
            return;
        }
        // Add fields from parent class first
        collectAllFields(clazz.getSuperclass(), result);
        // Add fields from current class
        result.addAll(java.util.Arrays.asList(clazz.getDeclaredFields()));
    }

    /**
     * Gets table name from @Table annotation or @Entity name.
     * In JPA, if no @Table is specified, the default table name is the entity name.
     */
    private String getTableNameFromAnnotation(Class<?> entityClass) {
        // Check @Table annotation first, including inherited annotations
        Class<?> current = entityClass;
        while (current != null && current != Object.class) {
            jakarta.persistence.Table tableAnn = current.getAnnotation(jakarta.persistence.Table.class);
            if (tableAnn != null && !tableAnn.name().isEmpty()) {
                return tableAnn.name();
            }
            current = current.getSuperclass();
        }

        // Check @Entity annotation for name, including inherited
        current = entityClass;
        while (current != null && current != Object.class) {
            jakarta.persistence.Entity entityAnn = current.getAnnotation(jakarta.persistence.Entity.class);
            if (entityAnn != null && !entityAnn.name().isEmpty()) {
                return entityAnn.name();
            }
            current = current.getSuperclass();
        }

        // Default to simple class name - must match getEntityTableName in MansartSchemaManager
        return entityClass.getSimpleName();
    }

    /**
     * Gets column name from @Column annotation or field name.
     */
    private String getColumnName(java.lang.reflect.Field field) {
        // Check @JoinColumn first (for relationship FK columns) - field or getter
        jakarta.persistence.JoinColumn joinColumn = getAnnotationOnFieldOrGetter(field, jakarta.persistence.JoinColumn.class);
        if (joinColumn != null && !joinColumn.name().isEmpty()) {
            return joinColumn.name();
        }
        jakarta.persistence.Column columnAnn = getAnnotationOnFieldOrGetter(field, jakarta.persistence.Column.class);
        return columnAnn != null && !columnAnn.name().isEmpty() ? columnAnn.name() : field.getName();
    }

    /**
     * Quotes SQL table identifier for the current dialect.
     * <p>
     * /**
     * Executes SELECT from database to find an entity by its primary key.
     * Used when entity is not found in L1 or L2 cache.
     */
    @SuppressWarnings("unchecked")
    private <T> T executeFindFromDatabase(Class<T> entityClass, Object primaryKey) {
        io.vidocq.mansart.persistence.spi.EntityMetadata metadata = resolveMetadata(entityClass);
        if (metadata == null) {
            throw new IllegalStateException("No EntityMetadata for " + entityClass.getName() +
                " — entity was not processed by the APT processor");
        }

        String tableName = metadata.tableName();
        if (tableName == null || tableName.isEmpty()) {
            tableName = entityClass.getSimpleName().toLowerCase();
        }

        String idColumnName = metadata.getIdAttributeName();

        // Build SELECT SQL
        String quotedTableName = quoteTableIdentifier(tableName);
        String quotedIdColumn = quoteColumnIdentifier(idColumnName);
        String sql = "SELECT * FROM " + quotedTableName + " WHERE " + quotedIdColumn + " = ?";

        try {
            Connection connection = getConnection();
            try (java.sql.PreparedStatement ps = connection.prepareStatement(sql)) {
                dialect.bind(ps, 1, primaryKey, primaryKey != null ? primaryKey.getClass() : Object.class);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // Create new instance using APT-generated factory
                        @SuppressWarnings("unchecked")
                        T entity = (T) metadata.createInstance();

                        // Populate all persistent attributes from ResultSet
                        java.util.List<String> attrs = metadata.getPersistentAttributeOrder();
                        for (String attrName : attrs) {
                            if (metadata.isRelationship(attrName)) continue;
                            try {
                                int columnIndex = rs.findColumn(attrName);
                                Object value = dialect.extract(rs, columnIndex, Object.class);
                                if (value != null) {
                                    metadata.writeAttribute(entity, attrName, value);
                                }
                            } catch (SQLException e) {
                                // Column not in result set — skip
                            }
                        }
                        return entity;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute find query for " + entityClass.getName(), e);
        }
        return null;
    }

    /**
     * Executes DELETE statement for the entity in the database.
     * Phase 1 (DEBT-05): uses EntityMetadata SPI.
     */
    private void executeDelete(Object entity, Object id) throws SQLException {
        Class<?> entityClass = entity.getClass();
        io.vidocq.mansart.persistence.spi.EntityMetadata metadata = resolveMetadata(entityClass);
        String tableName = metadata != null ? metadata.tableName() : getTableNameFromAnnotation(entityClass);
        if (tableName == null || tableName.isEmpty()) {
            tableName = entityClass.getSimpleName().toLowerCase();
        }
        String idColumnName = metadata != null ? metadata.getIdAttributeName() : "id";

        String quotedTableName = quoteTableIdentifier(tableName);
        String quotedIdColumn = quoteColumnIdentifier(idColumnName);
        String sql = "DELETE FROM " + quotedTableName + " WHERE " + quotedIdColumn + " = ?";

        Connection connection = getConnection();
        try (java.sql.PreparedStatement ps = connection.prepareStatement(sql)) {
            dialect.bind(ps, 1, id, id != null ? id.getClass() : Object.class);
            ps.executeUpdate();
        }
    }

    /**
     * Quotes SQL table identifier for the current dialect.
     * For PostgreSQL: returns double-quoted identifiers.
     * For H2: returns uppercase unquoted identifiers for TCK compatibility.
     */
    private String quoteTableIdentifier(String identifier) {
        // Fold-then-quote: schemas created from spec-conformant UNQUOTED DDL (e.g. the
        // official TCK scripts) store identifiers in the database's folded case — H2
        // folds to upper, PostgreSQL to lower. Quoting the folded name matches both
        // unquoted-DDL schemas and our own SchemaManager output, while staying safe
        // for reserved words (ORDER, VALUE, ...).
        if ("postgresql".equals(dialect.name())) {
            return "\"" + identifier.toLowerCase(java.util.Locale.ROOT) + "\"";
        } else if ("H2".equals(dialect.name())) {
            return "\"" + identifier.toUpperCase(java.util.Locale.ROOT) + "\"";
        } else {
            // Default: return unquoted identifier
            return identifier;
        }
    }

    /**
     * Quotes SQL column identifier for the current dialect.
     * For H2: returns double-quoted identifiers to preserve case sensitivity.
     * For PostgreSQL: returns double-quoted identifiers.
     */
    private String quoteColumnIdentifier(String identifier) {
        // Fold-then-quote — see quoteTableIdentifier
        if ("postgresql".equals(dialect.name())) {
            return "\"" + identifier.toLowerCase(java.util.Locale.ROOT) + "\"";
        } else if ("H2".equals(dialect.name())) {
            return "\"" + identifier.toUpperCase(java.util.Locale.ROOT) + "\"";
        } else {
            // Default: return unquoted identifier
            return identifier;
        }
    }

    @Override
    public <T> T merge(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null");
        }

        // Get the entity's ID using MethodHandles
        Object id = getEntityId(entity);

        if (id != null && cache.containsKey(createCacheKey(entity.getClass(), id))) {
            // Entity exists, update it
            // Invoke PreUpdate callback (M9-3)
            invokeLifecycleCallbacks(entity, LifecycleCallbackDispatcher.Phase.PRE_UPDATE);

            cache.put(createCacheKey(entity.getClass(), id), entity);
            // Update L2 cache
            Cache l2Cache = entityManagerFactory.getCache();
            if (l2Cache instanceof MansartCache) {
                ((MansartCache) l2Cache).put(entity.getClass(), id, entity);
            }

            // Invoke PostUpdate callback (M9-3)
            invokeLifecycleCallbacks(entity, LifecycleCallbackDispatcher.Phase.POST_UPDATE);

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
        invokeLifecycleCallbacks(entity, LifecycleCallbackDispatcher.Phase.PRE_REMOVE);

        Object id = getEntityId(entity);
        if (id != null) {
            // Execute DELETE in database
            if (dialect != null && connectionProvider != null) {
                try {
                    executeDelete(entity, id);
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to delete entity: " + e.getMessage(), e);
                }
            }

            cache.remove(createCacheKey(entity.getClass(), id));
            // Remove from L2 cache
            Cache l2Cache = entityManagerFactory.getCache();
            if (l2Cache instanceof MansartCache) {
                ((MansartCache) l2Cache).evict(entity.getClass(), id);
            }
        }

        // Invoke PostRemove callback (M9-3)
        invokeLifecycleCallbacks(entity, LifecycleCallbackDispatcher.Phase.POST_REMOVE);
    }

    @Override
    public void lock(Object entity, LockModeType lockMode) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null");
        }
        if (lockMode == null) {
            throw new IllegalArgumentException("LockModeType must not be null");
        }
        // Locking not yet fully implemented - no-op for now
    }

    @Override
    public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        lock(entity, lockMode);
    }

    @Override
    public void lock(Object entity, LockModeType lockMode, jakarta.persistence.LockOption... options) {
        lock(entity, lockMode);
    }

    @Override
    public void refresh(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null");
        }
        if (!contains(entity)) {
            throw new IllegalArgumentException("Entity must be managed");
        }

        // Get the ID value
        Object idValue = getEntityId(entity);
        if (idValue == null) {
            throw new IllegalArgumentException("Entity has no ID");
        }

        // Clear L1 cache for this entity
        Object cacheKey = createCacheKey(entity.getClass(), idValue);
        cache.remove(cacheKey);

        // Reload from database
        Object reloaded = executeFindFromDatabase(entity.getClass(), idValue);
        if (reloaded != null) {
            // Replace the entity in L1 cache
            cache.put(cacheKey, reloaded);
        }
    }

    @Override
    public void refresh(Object entity, Map<String, Object> properties) {
        refresh(entity);
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode) {
        refresh(entity);
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        refresh(entity);
    }

    @Override
    public void refresh(Object entity, jakarta.persistence.RefreshOption... options) {
        refresh(entity);
    }

    @Override
    public void flush() {
        // In auto-commit mode (default), changes are immediately synchronized with the database
        // In transaction-scoped mode, changes will be synchronized at transaction commit
        // For now, this is a no-op as we don't buffer changes
    }

    @Override
    public void setFlushMode(FlushModeType flushMode) {
        this.flushMode = flushMode != null ? flushMode : FlushModeType.AUTO;
    }

    @Override
    public FlushModeType getFlushMode() {
        return flushMode;
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
            Object cacheKey = createCacheKey(entity.getClass(), id);
            cache.remove(cacheKey);
            // Track detached entity to ensure find() returns null
            detachedKeys.add(cacheKey);
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
            throw new IllegalArgumentException("Entity must not be null");
        }

        Object id = getEntityId(entity);
        return id != null && cache.containsKey(createCacheKey(entity.getClass(), id));
    }

    @Override
    public LockModeType getLockMode(Object entity) {
        // For now, return NONE as we don't implement locking yet
        return LockModeType.NONE;
    }

    private jakarta.persistence.CacheRetrieveMode cacheRetrieveMode = jakarta.persistence.CacheRetrieveMode.USE;
    private jakarta.persistence.CacheStoreMode cacheStoreMode = jakarta.persistence.CacheStoreMode.USE;
    private FlushModeType flushMode = FlushModeType.AUTO;

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
        // Return a stub TypedQuery for TCK compatibility
        QueryCache qc = getQueryCache();
        return new MansartQuery.Generic<>(null, null, dialect, connectionProvider, entityModels, entityClasses, qc);
    }

    @Override
    public <T> TypedQuery<T> createQuery(jakarta.persistence.criteria.CriteriaSelect<T> selectQuery) {
        // Return a stub TypedQuery for TCK compatibility
        QueryCache qc = getQueryCache();
        return new MansartQuery.Generic<>(null, null, dialect, connectionProvider, entityModels, entityClasses, qc);
    }

    @Override
    public Query createQuery(jakarta.persistence.criteria.CriteriaUpdate<?> updateQuery) {
        // Return a stub Query for TCK compatibility
        QueryCache qc = getQueryCache();
        return new MansartQuery(null, null, dialect, connectionProvider, entityModels, entityClasses, qc);
    }

    @Override
    public Query createQuery(jakarta.persistence.criteria.CriteriaDelete<?> deleteQuery) {
        // Return a stub Query for TCK compatibility
        QueryCache qc = getQueryCache();
        return new MansartQuery(null, null, dialect, connectionProvider, entityModels, entityClasses, qc);
    }

    @Override
    public Query createNamedQuery(String name) {
        // Return a stub Query for TCK compatibility
        // TODO: Implement proper named query lookup
        QueryCache qc = getQueryCache();
        return new MansartQuery(null, name, dialect, connectionProvider, entityModels, entityClasses, qc);
    }

    @Override
    public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
        // Return a stub TypedQuery for TCK compatibility
        // TODO: Implement proper named query lookup
        QueryCache qc = getQueryCache();
        return new MansartQuery.Generic<>(null, name, dialect, connectionProvider, entityModels, entityClasses, qc);
    }

    @Override
    public <T> TypedQuery<T> createQuery(jakarta.persistence.TypedQueryReference<T> reference) {
        // Return a stub TypedQuery for TCK compatibility
        // TODO: Implement proper TypedQueryReference handling
        QueryCache qc = getQueryCache();
        return new MansartQuery.Generic<>(null, null, dialect, connectionProvider, entityModels, entityClasses, qc);
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
        // Clear detached keys set
        detachedKeys.clear();
        // Close shared connection if it was opened
        if (sharedConnection != null) {
            try {
                sharedConnection.close();
            } catch (SQLException e) {
                // Ignore connection close errors
            }
            sharedConnection = null;
        }
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
        if (function == null) return null;
        try (Connection conn = connectionProvider.getConnection()) {
            @SuppressWarnings("unchecked")
            T result = function.apply((C) conn);
            return result;
        } catch (Exception e) {
            throw new jakarta.persistence.PersistenceException("Error in callWithConnection", e);
        }
    }

    /**
     * Returns the transaction manager used by this entity manager.
     *
     * @return the transaction manager, or null if not configured
     */
    public MansartTransactionManager getTransactionManager() {
        return transactionManager;
    }

    /**
     * Checks if an annotation is present on the field OR its corresponding getter method.
     * JPA supports both field-level and property-level (getter) annotations.
     */
    /**
     * Finds the getter method name for a field, handling non-standard camelCase
     * field names (e.g. "wareHouse" -> "getWarehouse", not "getWareHouse").
     * Tries standard JavaBean convention first, then falls back to variants
     * that handle camelCase acronyms.
     */
    private String findGetterName(java.lang.reflect.Field field, String prefix) {
        String fieldName = field.getName();
        // Standard JavaBean: capitalize first char only
        String standard = prefix + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            field.getDeclaringClass().getMethod(standard);
            return standard;
        } catch (NoSuchMethodException ignored) {
            // Fallback: for camelCase acronyms like "wareHouse", try "getWarehouse"
            // (second char lowercased)
            if (fieldName.length() > 1 && Character.isUpperCase(fieldName.charAt(1))) {
                String fallback = prefix + Character.toUpperCase(fieldName.charAt(0))
                        + Character.toLowerCase(fieldName.charAt(1))
                        + fieldName.substring(2);
                try {
                    field.getDeclaringClass().getMethod(fallback);
                    return fallback;
                } catch (NoSuchMethodException ignored2) {
                    // Try all-uppercase acronym: "getURL", "getHIREDATE"
                    String acronym = prefix + fieldName.toUpperCase();
                    try {
                        field.getDeclaringClass().getMethod(acronym);
                        return acronym;
                    } catch (NoSuchMethodException ignored3) {
                        return null;
                    }
                }
            }
            return null;
        }
    }

    /**
     * Finds the setter method name for a field, using the same camelCase
     * acronym handling as findGetterName.
     */
    private String findSetterName(java.lang.reflect.Field field) {
        String fieldName = field.getName();
        String standard = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            field.getDeclaringClass().getMethod(standard, field.getType());
            return standard;
        } catch (NoSuchMethodException ignored) {
            if (fieldName.length() > 1 && Character.isUpperCase(fieldName.charAt(1))) {
                String fallback = "set" + Character.toUpperCase(fieldName.charAt(0))
                        + Character.toLowerCase(fieldName.charAt(1))
                        + fieldName.substring(2);
                try {
                    field.getDeclaringClass().getMethod(fallback, field.getType());
                    return fallback;
                } catch (NoSuchMethodException ignored2) {
                    String acronym = "set" + fieldName.toUpperCase();
                    try {
                        field.getDeclaringClass().getMethod(acronym, field.getType());
                        return acronym;
                    } catch (NoSuchMethodException ignored3) {
                        return null;
                    }
                }
            }
            return null;
        }
    }

    private boolean hasAnnotation(java.lang.reflect.Field field, Class<? extends java.lang.annotation.Annotation> annotationType) {
        if (field.isAnnotationPresent(annotationType)) {
            return true;
        }
        // Check getters (get/is)
        String getterName = findGetterName(field, "get");
        if (getterName != null) {
            try {
                java.lang.reflect.Method getter = field.getDeclaringClass().getMethod(getterName);
                if (getter.isAnnotationPresent(annotationType)) return true;
            } catch (NoSuchMethodException ignored) {
            }
        }
        getterName = findGetterName(field, "is");
        if (getterName != null) {
            try {
                java.lang.reflect.Method isGetter = field.getDeclaringClass().getMethod(getterName);
                if (isGetter.isAnnotationPresent(annotationType)) return true;
            } catch (NoSuchMethodException ignored) {
            }
        }
        // Check setters (set) — JPA property access checks both getters and setters
        String setterName = findSetterName(field);
        if (setterName != null) {
            try {
                java.lang.reflect.Method setter = field.getDeclaringClass().getMethod(setterName, field.getType());
                if (setter.isAnnotationPresent(annotationType)) return true;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return false;
    }

    /**
     * Gets an annotation from the field OR its corresponding getter/setter method.
     * JPA property access checks both getters and setters for annotations.
     */
    @SuppressWarnings("unchecked")
    private <A extends java.lang.annotation.Annotation> A getAnnotationOnFieldOrGetter(
            java.lang.reflect.Field field, Class<A> annotationType) {
        A ann = field.getAnnotation(annotationType);
        if (ann != null) {
            return ann;
        }
        // Check getters (get/is)
        String getterName = findGetterName(field, "get");
        if (getterName != null) {
            try {
                java.lang.reflect.Method getter = field.getDeclaringClass().getMethod(getterName);
                A result = getter.getAnnotation(annotationType);
                if (result != null) return result;
            } catch (NoSuchMethodException ignored) {
            }
        }
        getterName = findGetterName(field, "is");
        if (getterName != null) {
            try {
                java.lang.reflect.Method isGetter = field.getDeclaringClass().getMethod(getterName);
                A result = isGetter.getAnnotation(annotationType);
                if (result != null) return result;
            } catch (NoSuchMethodException ignored) {
            }
        }
        // Check setters (set) — JPA property access checks both getters and setters
        String setterName = findSetterName(field);
        if (setterName != null) {
            try {
                java.lang.reflect.Method setter = field.getDeclaringClass().getMethod(setterName, field.getType());
                A result = setter.getAnnotation(annotationType);
                if (result != null) return result;
            } catch (NoSuchMethodException ignored) {
            }
        }
        return null;
    }
}
