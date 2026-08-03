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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
/**
 * Implementation of Jakarta Persistence EntityManagerFactory.
 * Factory for creating EntityManager instances.
 */
package io.vidocq.mansart.persistence.core.bootstrap;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.Cache;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.SchemaManager;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQueryReference;
import java.util.Map;

/**
 * Mansart implementation of the Jakarta Persistence EntityManagerFactory interface.
 * This class is responsible for creating EntityManager instances and managing
 * the persistence unit.
 * Note: For M1, this is an abstract class. Full implementation will be done in later milestones.
 */
public abstract class MansartEntityManagerFactory implements EntityManagerFactory {

    private final String persistenceUnitName;
    private final Map properties;

    /**
     * Creates a new MansartEntityManagerFactory.
     *
     * @param persistenceUnitName the name of the persistence unit
     * @param properties the properties for this factory
     */
    public MansartEntityManagerFactory(String persistenceUnitName, Map properties) {
        this.persistenceUnitName = persistenceUnitName;
        this.properties = properties;
    }

    /**
     * Creates a new application-managed EntityManager.
     *
     * @return a new EntityManager instance
     */
    @Override
    public EntityManager createEntityManager() {
        // TODO: Implement EntityManager creation
        throw new UnsupportedOperationException("MansartEntityManagerFactory.createEntityManager not yet implemented");
    }

    /**
     * Creates a new application-managed EntityManager with the specified properties.
     *
     * @param map properties for the EntityManager
     * @return a new EntityManager instance
     */
    @Override
    public EntityManager createEntityManager(Map map) {
        // TODO: Implement EntityManager creation with properties
        throw new UnsupportedOperationException("MansartEntityManagerFactory.createEntityManager(Map) not yet implemented");
    }

    /**
     * Creates a new application-managed CriteriaBuilder.
     *
     * @return a new CriteriaBuilder instance
     */
    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        // TODO: Implement CriteriaBuilder creation
        throw new UnsupportedOperationException("MansartEntityManagerFactory.getCriteriaBuilder not yet implemented");
    }

    /**
     * Returns the metamodel for this persistence unit.
     *
     * @return the Metamodel for this persistence unit
     */
    @Override
    public Metamodel getMetamodel() {
        // TODO: Implement Metamodel retrieval
        throw new UnsupportedOperationException("MansartEntityManagerFactory.getMetamodel not yet implemented");
    }

    /**
     * Returns the persistence unit util.
     *
     * @return the PersistenceUnitUtil for this factory
     */
    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil() {
        // TODO: Implement PersistenceUnitUtil retrieval
        throw new UnsupportedOperationException("MansartEntityManagerFactory.getPersistenceUnitUtil not yet implemented");
    }

    /**
     * Calls the specified function with a new EntityManager within a transaction.
     *
     * @param function the function to call
     * @param <R> the return type
     * @return the result of the function
     */
    @Override
    public <R> R callInTransaction(java.util.function.Function<EntityManager, R> function) {
        // TODO: Implement callInTransaction
        throw new UnsupportedOperationException("MansartEntityManagerFactory.callInTransaction not yet implemented");
    }

    /**
     * Returns the properties for this factory.
     *
     * @return the properties map
     */
    @Override
    public Map getProperties() {
        return properties;
    }

    /**
     * Returns the name of the persistence unit.
     *
     * @return the persistence unit name
     */
    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    /**
     * Closes the factory and releases all resources.
     */
    @Override
    public void close() {
        // TODO: Implement factory cleanup
    }

    /**
     * Checks if the factory is open.
     *
     * @return true if the factory is open, false otherwise
     */
    @Override
    public boolean isOpen() {
        // TODO: Implement open state check
        return true;
    }

    /**
     * Adds a named query to the persistence unit.
     *
     * @param name the name of the query
     * @param query the query string
     */
    @Override
    public void addNamedQuery(String name, jakarta.persistence.Query query) {
        // TODO: Implement named query addition
        throw new UnsupportedOperationException("MansartEntityManagerFactory.addNamedQuery not yet implemented");
    }
}
