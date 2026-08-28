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

import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.Query;
import jakarta.persistence.SchemaManager;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;

import io.vidocq.mansart.persistence.core.metamodel.MetamodelImpl;

import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.DialectFactory;
import io.vidocq.mansart.data.dialect.EntityModel;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.IdentityHashMap;
import javax.sql.DataSource;

/**
 * Minimal {@link EntityManagerFactory} implementation that exposes a
 * runtime {@code Metamodel} built from the persistence unit's managed
 * classes.
 *
 * <p>All methods except {@code getMetamodel()} throw
 * {@code UnsupportedOperationException}.</p>
 */
final class MansartEntityManagerFactory implements EntityManagerFactory {

    private final MetamodelImpl metamodel;
    private final String persistenceUnitName;
    private final PersistenceContext persistenceContext;
    private final DataSource dataSource;
    private final Dialect dialect;
    private final Map<Class<?>, EntityModel<?>> entityModels;
    private volatile boolean closed;

    MansartEntityManagerFactory(MetamodelImpl metamodel, String persistenceUnitName) {
        this(metamodel, persistenceUnitName, null, null, Map.of());
    }

    MansartEntityManagerFactory(MetamodelImpl metamodel) {
        this(metamodel, "default");
    }

    /**
     * Construct a factory with database connectivity.
     *
     * @param metamodel     the runtime metamodel
     * @param persistenceUnitName the persistence unit name
     * @param dataSource    the database source (may be {@code null} for no-DB mode)
     * @param dialect       the SQL dialect (may be {@code null} for no-DB mode)
     * @param entityModels  entity class → {@code EntityModel} mapping
     */
    MansartEntityManagerFactory(MetamodelImpl metamodel, String persistenceUnitName,
                                DataSource dataSource, Dialect dialect,
                                Map<Class<?>, EntityModel<?>> entityModels) {
        this.metamodel = metamodel;
        this.persistenceUnitName = persistenceUnitName;
        this.persistenceContext = new PersistenceContext();
        this.dataSource = dataSource;
        this.dialect = dialect;
        this.entityModels = new IdentityHashMap<>(entityModels);
    }

    @Override
    public EntityManager createEntityManager() {
        checkOpen();
        return new MansartEntityManager(this, persistenceContext);
    }

    /**
     * Return the database source, or {@code null} if this factory has no DB connectivity.
     *
     * @return the data source, or {@code null}
     */
    DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Return the SQL dialect, or {@code null} if this factory has no DB connectivity.
     *
     * @return the dialect, or {@code null}
     */
    Dialect getDialect() {
        return dialect;
    }

    /**
     * Look up the {@code EntityModel} for an entity class.
     *
     * @param entityClass the entity class
     * @return the entity model, or {@code null} if not registered
     */
    @SuppressWarnings("unchecked")
    <E> EntityModel<E> getEntityModel(Class<E> entityClass) {
        return (EntityModel<E>) entityModels.get(entityClass);
    }

    @Override
    public EntityManager createEntityManager(Map<?, ?> map) {
        checkOpen();
        return new MansartEntityManager(this, persistenceContext);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType) {
        checkOpen();
        return new MansartEntityManager(this, persistenceContext);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType, Map<?, ?> map) {
        checkOpen();
        return new MansartEntityManager(this, persistenceContext);
    }

    /**
     * Check that this factory is still open.
     *
     * @throws IllegalStateException if closed
     */
    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("EntityManagerFactory is closed");
        }
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        throw new UnsupportedOperationException("not implemented: getCriteriaBuilder");
    }

    @Override
    public Metamodel getMetamodel() {
        return metamodel;
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public String getName() {
        return persistenceUnitName;
    }

    @Override
    public Map<String, Object> getProperties() {
        throw new UnsupportedOperationException("not implemented: getProperties");
    }

    @Override
    public Cache getCache() {
        throw new UnsupportedOperationException("not implemented: getCache");
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return PersistenceUnitTransactionType.RESOURCE_LOCAL;
    }

    @Override
    public SchemaManager getSchemaManager() {
        throw new UnsupportedOperationException("not implemented: getSchemaManager");
    }

    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil() {
        throw new UnsupportedOperationException("not implemented: getPersistenceUnitUtil");
    }

    @Override
    public void addNamedQuery(String name, Query query) {
        throw new UnsupportedOperationException("not implemented: addNamedQuery");
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        throw new UnsupportedOperationException("not implemented: unwrap");
    }

    @Override
    public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
        throw new UnsupportedOperationException("not implemented: addNamedEntityGraph");
    }

    @Override
    public <R> Map<String, TypedQueryReference<R>> getNamedQueries(Class<R> resultType) {
        throw new UnsupportedOperationException("not implemented: getNamedQueries");
    }

    @Override
    public <E> Map<String, EntityGraph<? extends E>> getNamedEntityGraphs(Class<E> entityType) {
        throw new UnsupportedOperationException("not implemented: getNamedEntityGraphs");
    }

    @Override
    public void runInTransaction(Consumer<EntityManager> work) {
        throw new UnsupportedOperationException("not implemented: runInTransaction");
    }

    @Override
    public <R> R callInTransaction(Function<EntityManager, R> work) {
        throw new UnsupportedOperationException("not implemented: callInTransaction");
    }
}
