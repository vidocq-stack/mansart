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
import io.vidocq.mansart.data.dialect.attribute.IdAttribute;

import java.lang.invoke.MethodHandle;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

/**
 * Minimal {@link EntityManager} backed by a {@link PersistenceContext}.
 *
 * <p>JP-22: lifecycle methods ({@code isOpen}, {@code close},
 * {@code isJoinedToTransaction}) are implemented. All CRUD/query methods
 * throw {@code IllegalStateException("EntityManager is closed")} after
 * {@code close()}; before close they throw
 * {@code UnsupportedOperationException("not implemented: <method>")}</p>
 */
final class MansartEntityManager implements EntityManager {

    private final MansartEntityManagerFactory factory;
    private final PersistenceContext persistenceContext;
    private volatile boolean closed;

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
    }

    @Override
    public <T> T merge(T entity) {
        checkClosed();
        throw new UnsupportedOperationException("not implemented: merge");
    }

    @Override
    public void remove(Object entity) {
        checkClosed();
        throw new UnsupportedOperationException("not implemented: remove");
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
        throw new UnsupportedOperationException("not implemented: find");
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
        throw new UnsupportedOperationException("not implemented: flush");
    }

    @Override
    public FlushModeType getFlushMode() {
        checkClosed();
        throw new UnsupportedOperationException("not implemented: getFlushMode");
    }

    @Override
    public void setFlushMode(FlushModeType flushMode) {
        checkClosed();
        throw new UnsupportedOperationException("not implemented: setFlushMode");
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
