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

import io.vidocq.mansart.data.core.MansartData;
import io.vidocq.mansart.data.core.RepositoryRuntime;
import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.persistence.core.MansartEntityManager;
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
import javax.sql.DataSource;

/**
 * Default concrete implementation of MansartEntityManagerFactory.
 * This class provides skeleton implementations for all EntityManagerFactory methods.
 * Full implementations will be provided in later milestones.
 */
public class DefaultMansartEntityManagerFactory extends MansartEntityManagerFactory {

    /**
     * Mansart Data components for entity operations.
     */
    private final RepositoryRuntime repositoryRuntime;
    private final Dialect dialect;
    private final DataSource dataSource;

    /**
     * Creates a new DefaultMansartEntityManagerFactory.
     *
     * @param persistenceUnitName the name of the persistence unit
     * @param properties the properties for this factory
     */
    public DefaultMansartEntityManagerFactory(String persistenceUnitName, Map properties) {
        super(persistenceUnitName, properties);
        // Initialize Mansart Data components
        this.dataSource = createDataSource();
        MansartData mansartData = MansartData.builder()
                .dataSource(dataSource)
                .build();
        this.repositoryRuntime = mansartData.runtime();
        this.dialect = mansartData.dialect();
    }

    /**
     * Creates a DataSource from the persistence unit properties.
     * For M4, this creates an in-memory H2 DataSource for testing.
     * In a real implementation, this would come from the persistence.xml or properties.
     */
    private DataSource createDataSource() {
        // For M4, create a default in-memory H2 DataSource
        // In production, this would be configured via persistence.xml properties
        try {
            Class<?> jdbcDataSourceClass = Class.forName("org.h2.jdbcx.JdbcDataSource");
            Object dataSource = jdbcDataSourceClass.getDeclaredConstructor().newInstance();
            
            // Set default H2 URL - can be overridden via properties
            String url = (String) getProperties().getOrDefault("jakarta.persistence.jdbc.url", 
                "jdbc:h2:mem:mansart-persistence;DB_CLOSE_DELAY=-1");
            jdbcDataSourceClass.getMethod("setURL", String.class).invoke(dataSource, url);
            jdbcDataSourceClass.getMethod("setUser", String.class).invoke(dataSource, 
                getProperties().getOrDefault("jakarta.persistence.jdbc.user", "sa"));
            jdbcDataSourceClass.getMethod("setPassword", String.class).invoke(dataSource, 
                getProperties().getOrDefault("jakarta.persistence.jdbc.password", ""));
            
            return (DataSource) dataSource;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create DataSource", e);
        }
    }

    @Override
    public EntityManager createEntityManager() {
        return createEntityManager((Map) null);
    }

    @Override
    public EntityManager createEntityManager(Map map) {
        return createEntityManager(SynchronizationType.SYNCHRONIZED, map);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType) {
        return createEntityManager(synchronizationType, (Map) null);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map) {
        return new MansartEntityManager(repositoryRuntime, dialect, dataSource, this);
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
        return super.getProperties();
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
