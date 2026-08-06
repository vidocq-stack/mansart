/*
 * Copyright (c) ${year} Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
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
import jakarta.persistence.SchemaValidationException;
import jakarta.persistence.TypedQueryReference;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
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
    private final List<Class<?>> entityClasses;

    /**
     * Creates a new DefaultMansartEntityManagerFactory.
     *
     * @param persistenceUnitName the name of the persistence unit
     * @param properties the properties for this factory (from persistence.xml or provided)
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
        
        // Extract entity classes from properties or persistence configuration
        this.entityClasses = extractEntityClasses();
        
        // Perform schema generation if configured
        SchemaGenerator schemaGenerator = new SchemaGenerator(
            repositoryRuntime, dataSource, properties, entityClasses);
        try {
            schemaGenerator.generateSchema();
        } catch (SchemaValidationException e) {
            // Log the validation error but don't fail factory creation
            System.Logger logger = System.getLogger(getClass().getName());
            logger.log(System.Logger.Level.WARNING, "Schema validation failed: " + e.getMessage());
        }
    }

    /**
     * Creates a DataSource from the persistence unit properties.
     * Now uses properties from persistence.xml (via PersistenceUnitInfo) or provided properties.
     * Creates an in-memory H2 DataSource by default for testing.
     * 
     * @return the configured DataSource
     */
    private DataSource createDataSource() {
        try {
            Class<?> jdbcDataSourceClass = Class.forName("org.h2.jdbcx.JdbcDataSource");
            Object dataSource = jdbcDataSourceClass.getDeclaredConstructor().newInstance();
            
            // Get properties from either PersistenceUnitInfo or the factory properties
            Map<String, Object> props = getProperties();
            
            // Set JDBC URL - can be overridden via properties
            String url = (String) props.getOrDefault("jakarta.persistence.jdbc.url", 
                "jdbc:h2:mem:mansart-persistence;DB_CLOSE_DELAY=-1");
            jdbcDataSourceClass.getMethod("setURL", String.class).invoke(dataSource, url);
            
            // Set JDBC user
            String user = (String) props.getOrDefault("jakarta.persistence.jdbc.user", "sa");
            jdbcDataSourceClass.getMethod("setUser", String.class).invoke(dataSource, user);
            
            // Set JDBC password
            String password = (String) props.getOrDefault("jakarta.persistence.jdbc.password", "");
            jdbcDataSourceClass.getMethod("setPassword", String.class).invoke(dataSource, password);
            
            return (DataSource) dataSource;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create DataSource", e);
        }
    }

    /**
     * Extracts entity classes from persistence unit configuration.
     * Tries to find entity classes from the persistence.xml configuration.
     *
     * @return list of entity classes
     */
    private List<Class<?>> extractEntityClasses() {
        List<Class<?>> classes = new ArrayList<>();
        
        // Try to find the persistence unit configuration and extract class names
        try {
            PersistenceUnitConfig puConfig = PersistenceXmlParser.findByName(getPersistenceUnitName());
            if (puConfig != null) {
                classes.addAll(puConfig.getEntityClasses());
            }
        } catch (IOException e) {
            System.Logger logger = System.getLogger(getClass().getName());
            logger.log(System.Logger.Level.WARNING, "Failed to parse persistence.xml: " + e.getMessage());
        }
        
        // Also check if there are entity classes in the properties
        // (some implementations might pass them as a property)
        Object entityClassesObj = getProperties().get("jakarta.persistence.entity.classes");
        if (entityClassesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<?> list = (List<?>) entityClassesObj;
            for (Object item : list) {
                if (item instanceof Class) {
                    classes.add((Class<?>) item);
                } else if (item instanceof String) {
                    try {
                        classes.add(Class.forName((String) item));
                    } catch (ClassNotFoundException e) {
                        // Ignore - will be logged during schema generation
                    }
                }
            }
        }
        
        return classes;
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
     * M6 — Returns the list of entity classes managed by this persistence unit.
     */
    @Override
    public List<Class<?>> getEntityClasses() {
        return Collections.unmodifiableList(entityClasses);
    }

    /**
     * Creates a new application-managed CriteriaBuilder.
     * Not yet fully implemented - returns null for now.
     *
     * @return a new CriteriaBuilder instance
     */
    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        // Return stub implementation via dynamic proxy
        return io.vidocq.mansart.persistence.core.criteria.MansartCriteriaBuilder.create();
    }

    /**
     * Returns the metamodel for this persistence unit.
     * Not yet fully implemented - returns null for now.
     *
     * @return the Metamodel for this persistence unit
     */
    @Override
    public Metamodel getMetamodel() {
        // TODO: M5 - Implement Metamodel
        throw new UnsupportedOperationException("Metamodel not yet implemented");
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
     * Not yet fully implemented - returns null for now.
     *
     * @return the Cache for this factory
     */
    @Override
    public Cache getCache() {
        // TODO: Implement Cache
        return null;
    }

    /**
     * Returns the persistence unit util.
     * Not yet fully implemented - returns null for now.
     *
     * @return the PersistenceUnitUtil for this factory
     */
    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil() {
        // TODO: Implement PersistenceUnitUtil
        return null;
    }

    /**
     * Returns the transaction type for this factory.
     * Defaults to RESOURCE_LOCAL.
     *
     * @return the PersistenceUnitTransactionType
     */
    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return PersistenceUnitTransactionType.RESOURCE_LOCAL;
    }

    /**
     * Returns the schema manager for this factory.
     *
     * @return the SchemaManager for this factory
     */
    @Override
    public SchemaManager getSchemaManager() {
        return new MansartSchemaManager(this);
    }

    /**
     * Adds a named query to the persistence unit.
     * Not yet implemented.
     *
     * @param name the name of the query
     * @param query the query
     */
    @Override
    public void addNamedQuery(String name, Query query) {
        // TODO: Implement named query addition
    }

    /**
     * Unwraps this factory to the specified type.
     * Not yet fully implemented.
     *
     * @param <T> the type to unwrap to
     * @param clazz the class to unwrap to
     * @return the unwrapped object
     */
    @Override
    public <T> T unwrap(Class<T> clazz) {
        if (clazz.isInstance(this)) {
            return clazz.cast(this);
        }
        throw new IllegalArgumentException("Cannot unwrap to " + clazz.getName());
    }

    /**
     * Adds a named entity graph to the persistence unit.
     * Not yet implemented.
     *
     * @param <T> the entity type
     * @param name the name of the entity graph
     * @param entityGraph the entity graph
     */
    @Override
    public <T> void addNamedEntityGraph(String name, EntityGraph<T> entityGraph) {
        // TODO: Implement named entity graph addition
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
     * Not yet fully implemented - executes without transaction.
     *
     * @param <R> the return type
     * @param function the function to call
     * @return the result of the function
     */
    @Override
    public <R> R callInTransaction(Function<EntityManager, R> function) {
        EntityManager em = createEntityManager();
        try {
            return function.apply(em);
        } finally {
            em.close();
        }
    }

    /**
     * Calls the specified consumer with a new EntityManager within a transaction.
     * Not yet fully implemented - executes without transaction.
     *
     * @param consumer the consumer to call
     */
    @Override
    public void runInTransaction(Consumer<EntityManager> consumer) {
        EntityManager em = createEntityManager();
        try {
            consumer.accept(em);
        } finally {
            em.close();
        }
    }

    /**
     * Returns the named entity graphs for the given entity class.
     * Not yet implemented - returns empty map.
     *
     * @param <E> the entity type
     * @param entityClass the entity class
     * @return the map of named entity graphs
     */
    @Override
    public <E> Map<String, EntityGraph<? extends E>> getNamedEntityGraphs(Class<E> entityClass) {
        return Collections.emptyMap();
    }
}
