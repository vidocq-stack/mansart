/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.ConnectionConsumer;
import jakarta.persistence.ConnectionFunction;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FindOption;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockOption;
import jakarta.persistence.Query;
import jakarta.persistence.RefreshOption;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaSelect;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.metamodel.ManagedType;

import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.data.dialect.SqlFragment;
import io.vidocq.mansart.data.dialect.Attribute;
import io.vidocq.mansart.data.dialect.Where;
import io.vidocq.mansart.data.dialect.OrderBy;
import io.vidocq.mansart.data.dialect.Pagination;
import io.vidocq.mansart.data.dialect.attribute.IdAttribute;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

/**
 * Minimal {@link EntityManager} backed by a {@link PersistenceContext}.
 *
 * <p>JP-22: lifecycle methods ({@code isOpen}, {@code close},
 * {@code isJoinedToTransaction}) are implemented.
 * JP-23: {@code persist(Object)} is implemented.
 * JP-25: {@code find(Class, Object)} is implemented.
 * JP-26: {@code remove(Object)} is implemented.
 * JP-28: {@code merge(Object)} is implemented.
 * JP-29: {@code flush()}, {@code getFlushMode()}, {@code setFlushMode()}
 * are implemented.
 * All other CRUD/query methods
 * throw {@code IllegalStateException("EntityManager is closed")} after
 * {@code close()}; before close they throw
 * {@code UnsupportedOperationException("not implemented: <method>")}</p>
 */
final class MansartEntityManager implements EntityManager {

    private final MansartEntityManagerFactory factory;
    private final PersistenceContext persistenceContext;
    private volatile boolean closed;
    private FlushModeType flushMode;

    /**
     * Check that this EntityManager has not been closed.
     *
     * @throws IllegalStateException if {@code close()} has been called
     */
    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("EntityManager is closed");
        }
    }

    MansartEntityManager(MansartEntityManagerFactory factory,
                         PersistenceContext persistenceContext) {
        this.factory = factory;
        this.persistenceContext = persistenceContext;
        this.flushMode = FlushModeType.AUTO;
    }

    // -- Entity lifecycle ---------------------------------------------------

    @Override
    public void persist(Object entity) {
        checkClosed();
        if (entity == null) {
            throw new IllegalArgumentException("entity must not be null");
        }
        var entityClass = entity.getClass();
        EntityModel<?> model = factory.getEntityModel(entityClass);
        IdAttribute<?, ?> idAttr = model.id();
        Object idValue;
        try {
            idValue = idAttr.getter().invoke(entity);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to get ID from entity", t);
        }
        persistenceContext.registerById(entityClass, idValue, entity);
        // Generate and execute INSERT SQL.
        SqlFragment sql = factory.getDialect().insert(model, false);
        try (Connection conn = factory.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.sql())) {
            int bindIndex = 1;
            for (Attribute<?, ?> attr : model.attributes()) {
                MethodHandle setter = attr.setter();
                try {
                    // Get the current value from the entity.
                    MethodHandle getter = attr.getter();
                    Object value = getter.invoke(entity);

                    // Handle @ManyToOne / @OneToOne: resolve FK value.
                    if (attr instanceof io.vidocq.mansart.data.dialect.attribute.ReferenceAttribute<?, ?> refAttr) {
                        // Only bind FK during INSERT if this ReferenceAttribute's
                        // entityType matches the current entity class (owning side).
                        // For inverse side, entityType is the target entity — skip binding.
                        if (refAttr.entityType() == entityClass) {
                            if (value != null) {
                                // Resolve the referenced entity's ID from its actual runtime class.
                                Class<?> refEntityClass = value.getClass();
                                EntityModel<?> refModel = factory.getEntityModel(refEntityClass);
                                Object refId;
                                try {
                                    refId = refModel.id().getter().invoke(value);
                                } catch (Throwable t) {
                                    throw new RuntimeException("Failed to get ID from referenced entity " + refEntityClass.getSimpleName(), t);
                                }
                                stmt.setObject(bindIndex++, refId);
                            } else {
                                // Null reference: explicitly bind null for nullable FK.
                                stmt.setNull(bindIndex++, java.sql.Types.VARCHAR);
                            }
                        }
                        continue;
                    }

                    // Set the value on the prepared statement.
                    stmt.setObject(bindIndex++, value);
                    // Also set the value on the entity via the setter (for identity maps).
                    setter.invoke(entity, value);
                } catch (Throwable t) {
                    throw new RuntimeException("Failed to invoke setter for attribute " + attr.name(), t);
                }
            }
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist entity", e);
        }

        // Handle plural attributes (many-to-many / one-to-many).
        for (Attribute<?, ?> pa : model.pluralAttributes()) {
            tryInsertJoinTable(entity, pa, model, entityClass);
        }

        // Handle inverse side @OneToOne: update the owning side's FK column.
        updateInverseSideOneToOne(entity, model, entityClass);
    }

    /**
     * Update the owning side's FK column when persisting the inverse side of a
     * bidirectional {@code @OneToOne} relationship.
     *
     * <p>When persisting the inverse side (entity A with {@code @OneToOne(mappedBy="...")}),
     * this method scans all managed entities for {@code ReferenceAttribute} instances
     * that point to entity A, and updates those entities' FK columns to point to A's ID.</p>
     */
    private void updateInverseSideOneToOne(Object entity, EntityModel<?> model,
                                           Class<?> entityClass) {
        try (Connection conn = factory.getDataSource().getConnection()) {
            // Get the current entity's ID.
            Object sourceId;
            try {
                sourceId = model.id().getter().invoke(entity);
            } catch (Throwable t) {
                throw new RuntimeException("Failed to get source entity ID", t);
            }

            // Scan all managed entities for ReferenceAttribute pointing to this entity.
            for (java.util.Map.Entry<Class<?>, java.util.Map<Object, Object>> entry
                    : persistenceContext.registeredById().entrySet()) {
                Class<?> targetClass = entry.getKey();
                if (targetClass == entityClass) continue;

                EntityModel<?> targetModel = factory.getEntityModel(targetClass);

                // Find the target's ReferenceAttribute with the FK column.
                String fkColumn = null;
                for (Attribute<?, ?> targetAttr : targetModel.attributes()) {
                    if (targetAttr instanceof io.vidocq.mansart.data.dialect.attribute.ReferenceAttribute<?, ?> targetRef) {
                        // Check if this ReferenceAttribute points to the current entity class.
                        if (targetRef.entityType() == entityClass) {
                            fkColumn = targetRef.columnName();
                            break;
                        }
                    }
                }
                if (fkColumn == null) continue;

                // Build and execute UPDATE on the target table.
                String updateSql = "UPDATE " + targetModel.tableName()
                        + " SET " + fkColumn + " = ? WHERE " + targetModel.id().name() + " = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setObject(1, sourceId);
                    // Update all managed instances of the target class.
                    for (java.util.Map.Entry<Object, Object> regEntry : entry.getValue().entrySet()) {
                        Object targetId = regEntry.getKey();
                        stmt.setObject(2, targetId);
                        stmt.executeUpdate();
                    }
                }
            }
        } catch (Exception e) {
            // Inverse side update failure is non-fatal — the owning side INSERT
            // already bound the FK. Log and continue.
            System.err.println("[WARN] Inverse side @OneToOne update failed: " + e.getMessage());
        }
    }

    private void tryInsertJoinTable(Object entity, Attribute<?, ?> pa,
                                    EntityModel<?> model, Class<?> entityClass) {
        String joinTableName = pa.joinTableName();
        String joinColumnName = pa.joinColumnName();
        String inverseJoinColumnName = pa.inverseJoinColumnName();
        String targetEntityFqn = pa.javaTypeFqn();
        boolean inverseSide = pa.inverseSide();

        if (joinTableName == null || joinColumnName == null) {
            // One-to-many inverse side: maintain the inverse collection.
            if (inverseSide) {
                maintainInverseCollection(entity, pa, model, entityClass);
            }
            return; // skip incomplete metadata
        }

        try (Connection conn = factory.getDataSource().getConnection()) {
            // Get the target entity IDs from the collection.
            Object collection = null;
            try {
                collection = pa.getter().invoke(entity);
            } catch (Throwable t) {
                throw new RuntimeException("Failed to get collection for " + pa.name(), t);
            }

            if (collection == null) return;

            java.util.Collection<?> items;
            if (collection instanceof java.util.Collection) {
                items = (java.util.Collection<?>) collection;
            } else if (collection instanceof java.util.Set) {
                items = (java.util.Set<?>) collection;
            } else {
                return;
            }

            if (items.isEmpty()) return;

            // Build INSERT into join table.
            String insertSql = "INSERT INTO " + joinTableName + " ("
                    + joinColumnName + ", " + inverseJoinColumnName + ") VALUES (?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                Object sourceId;
                try {
                    sourceId = model.id().getter().invoke(entity);
                } catch (Throwable t) {
                    throw new RuntimeException("Failed to get source ID", t);
                }
                for (Object item : items) {
                    if (item == null) continue;
                    stmt.setObject(1, sourceId);
                    // Get the target entity's ID.
                    Class<?> targetClass;
                    try {
                        targetClass = Class.forName(targetEntityFqn);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Target class not found: " + targetEntityFqn, e);
                    }
                    EntityModel<?> targetModel = factory.getEntityModel(targetClass);
                    Object targetId;
                    try {
                        targetId = targetModel.id().getter().invoke(item);
                    } catch (Throwable t) {
                        throw new RuntimeException("Failed to get target ID", t);
                    }
                    stmt.setObject(2, targetId);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist join table for " + pa.name(), e);
        }
    }

    /**
     * Maintain the inverse collection for a one-to-many relationship.
     *
     * <p>When persisting the inverse side (entity A with {@code @OneToMany(mappedBy="...")}),
     * this method updates the owning side entity's (B's) foreign key column to point
     * back to A. This ensures the bidirectional relationship is correctly persisted
     * in the database.</p>
     *
     * <p>For each target entity B in the collection, if B is already in the persistence
     * context (pre-existing in the DB), an UPDATE statement sets B's FK column to A's ID.</p>
     */
    private void maintainInverseCollection(Object entity, Attribute<?, ?> pa,
                                           EntityModel<?> model, Class<?> entityClass) {
        String mappedBy = null;
        if (pa instanceof io.vidocq.mansart.data.dialect.attribute.ManyToManyAttribute<?, ?> m2mAttr) {
            mappedBy = m2mAttr.mappedBy();
        }

        try (Connection conn = factory.getDataSource().getConnection()) {
            // Get the target entities from the collection.
            Object collection;
            try {
                collection = pa.getter().invoke(entity);
            } catch (Throwable t) {
                throw new RuntimeException("Failed to get collection for " + pa.name(), t);
            }
            if (collection == null) return;

            java.util.Collection<?> items;
            if (collection instanceof java.util.Collection) {
                items = (java.util.Collection<?>) collection;
            } else if (collection instanceof java.util.Set) {
                items = (java.util.Set<?>) collection;
            } else {
                return;
            }

            if (items.isEmpty()) return;

            // Get source entity ID.
            Object sourceId;
            try {
                sourceId = model.id().getter().invoke(entity);
            } catch (Throwable t) {
                throw new RuntimeException("Failed to get source ID", t);
            }

            // For each target entity, update its FK column.
            for (Object item : items) {
                if (item == null) continue;

                Class<?> targetClass;
                try {
                    targetClass = Class.forName(pa.javaTypeFqn());
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Target class not found: " + pa.javaTypeFqn(), e);
                }

                EntityModel<?> targetModel = factory.getEntityModel(targetClass);

                // Find the ReferenceAttribute (the @ManyToOne) on the target entity.
                // If mappedBy is specified, look for the attribute by name.
                io.vidocq.mansart.data.dialect.attribute.ReferenceAttribute<?, ?> refAttr = null;
                if (mappedBy != null) {
                    var opt = targetModel.attribute(mappedBy);
                    if (opt.isPresent() && opt.get() instanceof io.vidocq.mansart.data.dialect.attribute.ReferenceAttribute<?, ?> ref) {
                        refAttr = ref;
                    }
                }
                if (refAttr == null) {
                    // Fallback: find any ReferenceAttribute on the target entity.
                    for (var attr : targetModel.attributes()) {
                        if (attr instanceof io.vidocq.mansart.data.dialect.attribute.ReferenceAttribute<?, ?> ref) {
                            refAttr = ref;
                            break;
                        }
                    }
                }
                if (refAttr == null) continue; // no reference attribute on target

                // Get target entity ID.
                Object targetId;
                try {
                    targetId = targetModel.id().getter().invoke(item);
                } catch (Throwable t) {
                    throw new RuntimeException("Failed to get target ID", t);
                }

                // UPDATE the FK column on the target entity.
                // We always attempt the UPDATE — if the target entity doesn't exist
                // in the DB, 0 rows are affected (no error).
                String updateSql = "UPDATE " + targetModel.tableName()
                        + " SET " + refAttr.columnName() + " = ? WHERE "
                        + targetModel.id().columnName() + " = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setObject(1, sourceId);
                    stmt.setObject(2, targetId);
                    stmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to maintain inverse collection for " + pa.name(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T merge(T entity) {
        checkClosed();

        if (entity == null) {
            throw new IllegalArgumentException("entity must not be null");
        }

        var entityClass = entity.getClass();
        EntityModel<?> model = factory.getEntityModel(entityClass);
        if (model == null) {
            throw new IllegalArgumentException("not a managed type: " + entityClass.getName());
        }

        // If already managed, return it (spec: "it is itself ignored").
        if (persistenceContext.contains(entity)) {
            return (T) entity;
        }

        // Get the ID of the detached entity.
        Object idValue;
        try {
            idValue = model.id().getter().invoke(entity);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to get ID from entity", t);
        }

        // Check if there's already a managed entity with the same ID in the persistence context.
        Object managed = persistenceContext.lookupById(entityClass, idValue);
        if (managed != null) {
            // Copy state from detached entity to the managed entity.
            copyState(entity, managed, model);
            return (T) managed;
        }

        // Detached entity: find it from DB, copy state, register in persistence context.
        @SuppressWarnings("unchecked")
        T managedEntity = (T) find(entityClass, idValue);
        if (managedEntity != null) {
            // Copy state from the detached entity onto the managed copy.
            copyState(entity, managedEntity, model);
            // Flush the updated state to the database.
            flushManagedEntity(managedEntity, model);
            return managedEntity;
        }

        // Entity not in DB — treat as new: persist it.
        persist(entity);
        return (T) entity;
    }

    /**
     * Copy attribute state from a source entity to a target entity.
     *
     * @param source the detached (source) entity
     * @param target the managed (target) entity
     * @param model  the entity model
     */
    private void copyState(Object source, Object target, EntityModel<?> model) {
        for (Attribute<?, ?> attr : model.attributes()) {
            try {
                MethodHandle getter = attr.getter();
                MethodHandle setter = attr.setter();
                Object value = getter.invoke(source);
                setter.invoke(target, value);
            } catch (Throwable t) {
                throw new RuntimeException("Failed to copy attribute " + attr.name(), t);
            }
        }
    }

    /**
     * Flush a managed entity's state to the database via an UPDATE statement.
     *
     * @param entity the managed entity
     * @param model  the entity model
     */
    private void flushManagedEntity(Object entity, EntityModel<?> model) {
        SqlFragment sql;
        try {
            sql = factory.getDialect().update(model, Where.eq(model.id()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to build UPDATE for " + model.entityClass().getSimpleName(), e);
        }

        try (Connection conn = factory.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.sql())) {
            int bindIndex = 1;
            // Bind non-ID attributes for the SET clause.
            for (Attribute<?, ?> attr : model.attributes()) {
                if (attr == model.id()) continue;
                try {
                    MethodHandle getter = attr.getter();
                    Object value = getter.invoke(entity);
                    stmt.setObject(bindIndex++, value);
                } catch (Throwable t) {
                    throw new RuntimeException("Failed to bind attribute " + attr.name(), t);
                }
            }
            // Bind WHERE parameters.
            try {
                factory.getDialect().bind(stmt, bindIndex,
                        model.id().getter().invoke(entity), model.id().javaType());
            } catch (Throwable t) {
                throw new RuntimeException("Failed to bind WHERE clause", t);
            }
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to flush entity " + model.entityClass().getSimpleName(), e);
        }
    }

    @Override
    public void remove(Object entity) {
        checkClosed();

        if (entity == null) {
            throw new IllegalArgumentException("null entity");
        }

        // Look up the EntityModel for this entity class.
        EntityModel<?> model = factory.getEntityModel(entity.getClass());
        if (model == null) {
            throw new IllegalArgumentException("not a managed type: " + entity.getClass().getName());
        }

        Object id;
        try {
            id = model.id().getter().invoke(entity);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to get entity ID", t);
        }

        Object removedEntity = null;

        // Case 1: managed entity — DELETE and unregister.
        if (persistenceContext.contains(entity)) {
            executeDelete(model, entity);
            persistenceContext.unregister(entity);
            removedEntity = entity;
        } else {
            // Case 2: detached entity — look up by ID in the persistence context,
            // then in the database, DELETE and unregister if found.
            // Check the persistence context's identity map first (by ID).
            Object cached = persistenceContext.lookupById(entity.getClass(), id);
            if (cached != null) {
                executeDelete(model, cached);
                persistenceContext.unregister(cached);
                removedEntity = cached;
            } else {
                // Not in persistence context: try to find from DB (detached entity).
                @SuppressWarnings("unchecked")
                Object found = find((Class<Object>) entity.getClass(), id);
                if (found != null) {
                    executeDelete(model, found);
                    persistenceContext.unregister(found);
                    removedEntity = found;
                }
                // If found == null: new or already-removed entity — no-op (per spec).
            }
        }

        // Relationship-specific cleanup: clear FK columns on inverse-side relationships.
        if (removedEntity != null) {
            clearInverseSideFk(removedEntity, model, entity.getClass());
        }
        // If removedEntity == null: new or already-removed entity — no-op (per spec).
    }

    /**
     * Clear FK columns on inverse-side relationships pointing to the removed entity.
     *
     * <p>After deleting an entity, this method scans all managed entities for
     * {@code ReferenceAttribute} instances that point to the removed entity's class,
     * and sets their FK columns to NULL. It also scans the database for entities
     * that are not in the persistence context but have FKs pointing to the removed entity.</p>
     *
     * <p>Matching is done by FK column name pattern: the FK column name is expected
     * to follow the convention {@code <TARGET_TABLE_UPPER>_&lt;TARGET_ID_COLUMN_UPPER&gt;}.</p>
     */
    private void clearInverseSideFk(Object removedEntity, EntityModel<?> removedModel,
                                     Class<?> removedClass) {
        Object removedId;
        try {
            removedId = removedModel.id().getter().invoke(removedEntity);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to get removed entity ID", t);
        }

        String removedTable = removedModel.tableName().toUpperCase();
        String removedIdCol = removedModel.id().name().toUpperCase();
        String fkPattern = removedTable + "_" + removedIdCol;

        try (Connection conn = factory.getDataSource().getConnection()) {
            // Scan all managed entities for ReferenceAttribute pointing to the removed entity.
            for (java.util.Map.Entry<Class<?>, java.util.Map<Object, Object>> entry
                    : persistenceContext.registeredById().entrySet()) {
                Class<?> targetClass = entry.getKey();
                if (targetClass == removedClass) continue;

                EntityModel<?> targetModel = factory.getEntityModel(targetClass);

                for (Attribute<?, ?> targetAttr : targetModel.attributes()) {
                    if (targetAttr instanceof io.vidocq.mansart.data.dialect.attribute.ReferenceAttribute<?, ?> targetRef) {
                        String fkCol = targetRef.columnName().toUpperCase();
                        if (fkCol.equals(fkPattern)) {
                            String updateSql = "UPDATE " + targetModel.tableName()
                                    + " SET " + targetRef.columnName()
                                    + " = NULL WHERE " + targetModel.id().name() + " = ?";
                            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                                stmt.setObject(1, removedId);
                                stmt.executeUpdate();
                            }
                        }
                    }
                }
            }

            // Also scan the database for entities that are not in the persistence context.
            for (Class<?> targetClass : factory.getEntityModelClassSet()) {
                if (targetClass == removedClass) continue;

                EntityModel<?> targetModel = factory.getEntityModel(targetClass);
                for (Attribute<?, ?> targetAttr : targetModel.attributes()) {
                    if (targetAttr instanceof io.vidocq.mansart.data.dialect.attribute.ReferenceAttribute<?, ?> targetRef) {
                        String fkCol = targetRef.columnName().toUpperCase();
                        if (fkCol.equals(fkPattern)) {
                            String updateSql = "UPDATE " + targetModel.tableName()
                                    + " SET " + targetRef.columnName()
                                    + " = NULL WHERE " + targetRef.columnName() + " = ?";
                            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                                stmt.setObject(1, removedId);
                                stmt.executeUpdate();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[WARN] Inverse side FK cleanup failed: " + e.getMessage());
        }
    }

    /**
     * Execute a DELETE statement for the given entity.
     */
    private void executeDelete(EntityModel<?> model, Object entity) {
        Object id;
        try {
            id = model.id().getter().invoke(entity);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to get entity ID", t);
        }

        String sql;
        try {
            SqlFragment fragment = factory.getDialect().delete(
                    model, Where.eq(model.id()));
            sql = fragment.sql();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build DELETE for "
                    + model.entityClass().getSimpleName(), e);
        }

        int paramCount = (sql.length() - sql.replace("?", "").length());

        try (Connection conn = factory.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < paramCount; i++) {
                stmt.setObject(i + 1, id);
            }
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove entity "
                    + model.entityClass().getSimpleName()
                    + " with ID " + id, e);
        }
    }

    @Override
    public void detach(Object entity) {
        checkClosed();
        throw new UnsupportedOperationException("not implemented: detach");
    }

    @Override
    public void refresh(Object entity) {
        checkClosed();
        throw new UnsupportedOperationException("not implemented: refresh");
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: refresh(LockModeType)");
    }

    @Override
    public void refresh(Object entity, Map<String, Object> properties) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: refresh(Map)");
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode,
                        Map<String, Object> properties) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: refresh(LockModeType, Map)");
    }

    @Override
    public void refresh(Object entity, RefreshOption... options) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: refresh(RefreshOption...)");
    }

    @Override
    public void lock(Object entity, LockModeType lockMode) {
        checkClosed();
        throw new UnsupportedOperationException("not implemented: lock");
    }

    @Override
    public void lock(Object entity, LockModeType lockMode,
                     Map<String, Object> properties) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: lock(LockModeType, Map)");
    }

    @Override
    public void lock(Object entity, LockModeType lockMode,
                     LockOption... options) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: lock(LockModeType, LockOption...)");
    }

    // -- Find / Reference ---------------------------------------------------

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        checkClosed();

        // Look up the EntityModel for this entity class.
        EntityModel<?> model = factory.getEntityModel(entityClass);
        if (model == null) {
            throw new IllegalArgumentException("not a managed type: " + entityClass.getName());
        }

        // Check the persistence context's identity map first.
        Object cached = persistenceContext.lookupById(entityClass, primaryKey);
        if (cached != null) {
            @SuppressWarnings("unchecked")
            T result = (T) cached;
            return result;
        }

        // Query the database.
        String sql;
        try {
            SqlFragment fragment = factory.getDialect().select(
                    model, Where.eq(model.id()), OrderBy.NONE, Pagination.NONE);
            sql = fragment.sql();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build SELECT for " + entityClass.getSimpleName(), e);
        }

        // Count ? placeholders in the SQL to determine how many parameters to bind.
        int paramCount = (sql.length() - sql.replace("?", "").length());

        try (Connection conn = factory.getDataSource().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Bind the primary key for each ? placeholder.
            for (int i = 0; i < paramCount; i++) {
                stmt.setObject(i + 1, primaryKey);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null; // Not found.
                }
                // Map the single result row to an entity instance.
                T entity = instantiateEntity(entityClass, model, rs);
                // Register in the persistence context.
                Object id;
                try {
                    id = model.id().getter().invoke(entity);
                } catch (Throwable t) {
                    throw new RuntimeException("Failed to get entity ID", t);
                }
                persistenceContext.registerById(entityClass, id, entity);
                return entity;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to find entity " + entityClass.getSimpleName()
                    + " with ID " + primaryKey, e);
        }
    }

    /**
     * Instantiate an entity from a {@code ResultSet} row.
     *
     * @param entityClass the entity class
     * @param model       the entity model
     * @param rs          the result set
     * @return the instantiated entity
     * @throws Exception if instantiation or attribute binding fails
     */
    @SuppressWarnings("unchecked")
    private <T> T instantiateEntity(Class<T> entityClass, EntityModel<?> model,
                                     ResultSet rs) throws Exception {
        // Use the no-arg constructor from the entity model.
        T entity;
        try {
            entity = (T) model.constructor().invoke();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to instantiate entity " + entityClass.getSimpleName(), t);
        }

        // Bind each attribute from the result set.
        for (io.vidocq.mansart.data.dialect.Attribute<?, ?> attr : model.attributes()) {
            Object value = rs.getObject(attr.columnName());
            MethodHandle setter = findSetter(attr);
            if (setter != null && value != null) {
                try {
                    setter.invoke(entity, value);
                } catch (Throwable t) {
                    throw new RuntimeException("Failed to set attribute " + attr.name(), t);
                }
            }
        }
        return entity;
    }

    /**
     * Find the setter method handle for an attribute.
     *
     * @param attr the attribute
     * @return the setter method handle, or {@code null} if not found
     */
    private MethodHandle findSetter(io.vidocq.mansart.data.dialect.Attribute<?, ?> attr) {
        try {
            String setterName = "set" + Character.toUpperCase(attr.name().charAt(0))
                    + attr.name().substring(1);
            Class<?> attrType = attr.javaType();
            return MethodHandles.lookup().findVirtual(attr.entityType(), setterName,
                    MethodType.methodType(void.class, attrType));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey,
                      Map<String, Object> properties) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: find(Class, Map)");
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey,
                      LockModeType lockMode) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: find(Class, LockModeType)");
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey,
                      LockModeType lockMode, Map<String, Object> properties) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: find(Class, LockModeType, Map)");
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey,
                      FindOption... options) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: find(Class, Object, FindOption...)");
    }

    @Override
    public <T> T find(EntityGraph<T> entityGraph, Object primaryKey,
                      FindOption... options) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: find(EntityGraph, Object, FindOption...)");
    }

    @Override
    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        checkClosed();
        throw new UnsupportedOperationException("not implemented: getReference");
    }

    @Override
    public <T> T getReference(T entity) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: getReference(T)");
    }

    // -- Query creation -----------------------------------------------------

    @Override
    public Query createQuery(String qlString) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: createQuery(String)");
    }

    @Override
    public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: createQuery(String, Class)");
    }

    @Override
    public Query createQuery(CriteriaDelete<?> deleteQuery) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: createQuery(CriteriaDelete)");
    }

    @Override
    public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: createQuery(CriteriaQuery)");
    }

    @Override
    public <T> TypedQuery<T> createQuery(CriteriaSelect<T> selectQuery) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: createQuery(CriteriaSelect)");
    }

    @Override
    public Query createQuery(CriteriaUpdate<?> updateQuery) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: createQuery(CriteriaUpdate)");
    }

    @Override
    public <T> TypedQuery<T> createQuery(TypedQueryReference<T> reference) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: createQuery(TypedQueryReference)");
    }

    @Override
    public Query createNamedQuery(String name) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: createNamedQuery(String)");
    }

    @Override
    public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: createNamedQuery(String, Class)");
    }

    @Override
    public Query createNativeQuery(String sqlString) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: createNativeQuery(String)");
    }

    @Override
    public <T> Query createNativeQuery(String sqlString, Class<T> resultClass) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: createNativeQuery(String, Class)");
    }

    @Override
    public Query createNativeQuery(String sqlString,
                                   String resultSetMapping) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: createNativeQuery(String, String)");
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(
            String procedureName) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: createStoredProcedureQuery(String)");
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(
            String procedureName, Class<?>... resultClasses) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: createStoredProcedureQuery(String, Class...)");
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(
            String procedureName, String... resultSetMappings) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: createStoredProcedureQuery(String, String...)");
    }

    @Override
    public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: createNamedStoredProcedureQuery");
    }

    // -- Entity graph -------------------------------------------------------

    @Override
    public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: createEntityGraph(Class)");
    }

    @Override
    public EntityGraph<?> createEntityGraph(String graphName) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: createEntityGraph(String)");
    }

    @Override
    public EntityGraph<?> getEntityGraph(String graphName) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: getEntityGraph(String)");
    }

    @Override
    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: getEntityGraphs(Class)");
    }

    // -- Flush mode / Cache mode --------------------------------------------

    @Override
    public void flush() {
        checkClosed();
        // Iterate over all managed entities and flush each one.
        for (Object entity : persistenceContext.entities()) {
            Class<?> entityClass = persistenceContext.managedEntities()
                    .get(entity);
            if (entityClass != null) {
                EntityModel<?> model = factory.getEntityModel(entityClass);
                if (model != null) {
                    flushManagedEntity(entity, model);
                }
            }
        }
    }

    @Override
    public FlushModeType getFlushMode() {
        checkClosed();
        return flushMode;
    }

    @Override
    public void setFlushMode(FlushModeType flushMode) {
        checkClosed();
        this.flushMode = flushMode;
    }

    @Override
    public CacheRetrieveMode getCacheRetrieveMode() {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: getCacheRetrieveMode");
    }

    @Override
    public void setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: setCacheRetrieveMode");
    }

    @Override
    public CacheStoreMode getCacheStoreMode() {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: getCacheStoreMode");
    }

    @Override
    public void setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: setCacheStoreMode");
    }

    // -- Transaction / Connection -------------------------------------------

    @Override
    public EntityTransaction getTransaction() {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: getTransaction");
    }

    @Override
    public void joinTransaction() {
        checkClosed();
        throw new UnsupportedOperationException("not implemented: joinTransaction");
    }

    @Override
    public boolean isJoinedToTransaction() {
        checkClosed();
        return false;
    }

    @Override
    public <C, T> T callWithConnection(ConnectionFunction<C, T> function) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: callWithConnection");
    }

    @Override
    public <C> void runWithConnection(ConnectionConsumer<C> action) {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: runWithConnection");
    }

    // -- Meta / Properties / Utility ----------------------------------------

    @Override
    public jakarta.persistence.criteria.CriteriaBuilder getCriteriaBuilder() {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: getCriteriaBuilder");
    }

    @Override
    public jakarta.persistence.metamodel.Metamodel getMetamodel() {
        checkClosed();
        throw new UnsupportedOperationException("not implemented: getMetamodel");
    }

    @Override
    public Map<String, Object> getProperties() {
        checkClosed();
        throw new UnsupportedOperationException("not implemented: getProperties");
    }

    @Override
    public void setProperty(String propertyName, Object value) {
        checkClosed();
        throw new UnsupportedOperationException("not implemented: setProperty");
    }

    @Override
    public Object getDelegate() {
        checkClosed();
        throw new UnsupportedOperationException("not implemented: getDelegate");
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        checkClosed();
        throw new UnsupportedOperationException(
                "not implemented: getEntityManagerFactory");
    }

    @Override
    public LockModeType getLockMode(Object entity) {
        checkClosed();
        throw new UnsupportedOperationException("not implemented: getLockMode");
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        checkClosed();
        throw new UnsupportedOperationException("not implemented: unwrap");
    }

    @Override
    public void clear() {
        checkClosed();
        throw new UnsupportedOperationException("not implemented: clear");
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public void close() {
        closed = true;
        persistenceContext.close();
    }

    @Override
    public boolean contains(Object entity) {
        if (closed) {
            throw new IllegalStateException("EntityManager is closed");
        }
        return persistenceContext.contains(entity);
    }
}
