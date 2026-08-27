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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

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
    private volatile boolean closed;

    MansartEntityManagerFactory(MetamodelImpl metamodel, String persistenceUnitName) {
        this.metamodel = metamodel;
        this.persistenceUnitName = persistenceUnitName;
    }

    MansartEntityManagerFactory(MetamodelImpl metamodel) {
        this(metamodel, "default");
    }

    @Override
    public EntityManager createEntityManager() {
        throw new UnsupportedOperationException("not implemented: createEntityManager");
    }

    @Override
    public EntityManager createEntityManager(Map<?, ?> map) {
        throw new UnsupportedOperationException("not implemented: createEntityManager(Map)");
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType) {
        throw new UnsupportedOperationException("not implemented: createEntityManager(SynchronizationType)");
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType, Map<?, ?> map) {
        throw new UnsupportedOperationException("not implemented: createEntityManager(SynchronizationType, Map)");
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
        return Map.of();
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
        return Map.of();
    }

    @Override
    public <E> Map<String, EntityGraph<? extends E>> getNamedEntityGraphs(Class<E> entityType) {
        return Map.of();
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
