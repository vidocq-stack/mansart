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
/**
 * Default implementation of MansartEntityManagerFactory.
 * This is a concrete implementation for M1 skeleton.
 */
package io.vidocq.mansart.persistence.core.bootstrap;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.Cache;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.SchemaManager;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQueryReference;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Consumer;

/**
 * Default concrete implementation of MansartEntityManagerFactory.
 * This class provides skeleton implementations for all EntityManagerFactory methods.
 * Full implementations will be provided in later milestones.
 */
public class DefaultMansartEntityManagerFactory extends MansartEntityManagerFactory {

    /**
     * Creates a new DefaultMansartEntityManagerFactory.
     *
     * @param persistenceUnitName the name of the persistence unit
     * @param properties the properties for this factory
     */
    public DefaultMansartEntityManagerFactory(String persistenceUnitName, Map properties) {
        super(persistenceUnitName, properties);
    }

    /**
     * Creates a new application-managed EntityManager.
     *
     * @return a new EntityManager instance
     */
    @Override
    public EntityManager createEntityManager() {
        throw new UnsupportedOperationException("createEntityManager not yet implemented");
    }

    /**
     * Creates a new application-managed EntityManager with the specified properties.
     *
     * @param map properties for the EntityManager
     * @return a new EntityManager instance
     */
    @Override
    public EntityManager createEntityManager(Map map) {
        throw new UnsupportedOperationException("createEntityManager(Map) not yet implemented");
    }

    /**
     * Creates a new application-managed EntityManager with the specified synchronization type.
     *
     * @param synchronizationType the synchronization type
     * @return a new EntityManager instance
     */
    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType) {
        throw new UnsupportedOperationException("createEntityManager(SynchronizationType) not yet implemented");
    }

    /**
     * Creates a new application-managed EntityManager with the specified synchronization type and properties.
     *
     * @param synchronizationType the synchronization type
     * @param map properties for the EntityManager
     * @return a new EntityManager instance
     */
    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map) {
        throw new UnsupportedOperationException("createEntityManager(SynchronizationType, Map) not yet implemented");
    }

    /**
     * Creates a new application-managed CriteriaBuilder.
     *
     * @return a new CriteriaBuilder instance
     */
    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        throw new UnsupportedOperationException("getCriteriaBuilder not yet implemented");
    }

    /**
     * Returns the metamodel for this persistence unit.
     *
     * @return the Metamodel for this persistence unit
     */
    @Override
    public Metamodel getMetamodel() {
        throw new UnsupportedOperationException("getMetamodel not yet implemented");
    }

    /**
     * Checks if the factory is open.
     *
     * @return true if the factory is open, false otherwise
     */
    @Override
    public boolean isOpen() {
        return true;
    }

    /**
     * Closes the factory and releases all resources.
     */
    @Override
    public void close() {
        // TODO: Implement factory cleanup
    }

    /**
     * Returns the name of the persistence unit.
     *
     * @return the persistence unit name
     */
    @Override
    public String getName() {
        return getPersistenceUnitName();
    }

    /**
     * Returns the properties for this factory.
     *
     * @return the properties map
     */
    @Override
    public Map<String, Object> getProperties() {
        return getProperties();
    }

    /**
     * Returns the cache for this factory.
     *
     * @return the Cache for this factory
     */
    @Override
    public Cache getCache() {
        throw new UnsupportedOperationException("getCache not yet implemented");
    }

    /**
     * Returns the persistence unit util.
     *
     * @return the PersistenceUnitUtil for this factory
     */
    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil() {
        throw new UnsupportedOperationException("getPersistenceUnitUtil not yet implemented");
    }

    /**
     * Returns the transaction type for this factory.
     *
     * @return the PersistenceUnitTransactionType
     */
    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        throw new UnsupportedOperationException("getTransactionType not yet implemented");
    }

    /**
     * Returns the schema manager for this factory.
     *
     * @return the SchemaManager for this factory
     */
    @Override
    public SchemaManager getSchemaManager() {
        throw new UnsupportedOperationException("getSchemaManager not yet implemented");
    }

    /**
     * Adds a named query to the persistence unit.
     *
     * @param name the name of the query
     * @param query the query
     */
    @Override
    public void addNamedQuery(String name, Query query) {
        throw new UnsupportedOperationException("addNamedQuery not yet implemented");
    }

    /**
     * Unwraps this factory to the specified type.
     *
     * @param <T> the type to unwrap to
     * @param clazz the class to unwrap to
     * @return the unwrapped object
     */
    @Override
    public <T> T unwrap(Class<T> clazz) {
        throw new UnsupportedOperationException("unwrap not yet implemented");
    }

    /**
     * Adds a named entity graph to the persistence unit.
     *
     * @param <T> the entity type
     * @param name the name of the entity graph
     * @param entityGraph the entity graph
     */
    @Override
    public <T> void addNamedEntityGraph(String name, EntityGraph<T> entityGraph) {
        throw new UnsupportedOperationException("addNamedEntityGraph not yet implemented");
    }

    /**
     * Returns the named queries for the given result type.
     *
     * @param <R> the result type
     * @param resultType the result type
     * @return the map of named queries
     */
    @Override
    public <R> Map<String, TypedQueryReference<R>> getNamedQueries(Class<R> resultType) {
        throw new UnsupportedOperationException("getNamedQueries not yet implemented");
    }

    /**
     * Calls the specified function with a new EntityManager within a transaction.
     *
     * @param <R> the return type
     * @param function the function to call
     * @return the result of the function
     */
    @Override
    public <R> R callInTransaction(Function<EntityManager, R> function) {
        throw new UnsupportedOperationException("callInTransaction not yet implemented");
    }

    /**
     * Calls the specified consumer with a new EntityManager within a transaction.
     *
     * @param consumer the consumer to call
     */
    @Override
    public void runInTransaction(Consumer<EntityManager> consumer) {
        throw new UnsupportedOperationException("runInTransaction not yet implemented");
    }

    /**
     * Returns the named entity graphs for the given entity class.
     *
     * @param <E> the entity type
     * @param entityClass the entity class
     * @return the map of named entity graphs
     */
    @Override
    public <E> Map<String, EntityGraph<? extends E>> getNamedEntityGraphs(Class<E> entityClass) {
        throw new UnsupportedOperationException("getNamedEntityGraphs not yet implemented");
    }
}
