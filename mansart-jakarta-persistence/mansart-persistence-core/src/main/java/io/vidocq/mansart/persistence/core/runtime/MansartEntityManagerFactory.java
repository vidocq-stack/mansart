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
package io.vidocq.mansart.persistence.core.runtime;

import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.DialectFactory;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.persistence.core.jpql.JpqlExecutor;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Mansart EntityManagerFactory implementation.
 *
 * <p>Creates EntityManager instances and manages the persistence unit lifecycle.
 */
public class MansartEntityManagerFactory implements EntityManagerFactory {

    private final io.vidocq.mansart.persistence.core.bootstrap.MansartPersistenceProvider provider;
    private final String persistenceUnitName;
    private final Map<String, Object> properties;
    private volatile boolean open = true;
    
    // M8-16: Runtime components for EntityManager creation
    private final Dialect dialect;
    private final JpqlExecutor.ConnectionProvider connectionProvider;
    private final Map<String, Class<?>> entityClasses;
    private final Map<Class<?>, EntityModel<?>> entityModels;

    /**
     * Creates a new MansartEntityManagerFactory.
     *
     * @param provider the persistence provider
     * @param persistenceUnitName the persistence unit name
     * @param properties the persistence unit properties
     */
    @SuppressWarnings("unchecked")
    public MansartEntityManagerFactory(io.vidocq.mansart.persistence.core.bootstrap.MansartPersistenceProvider provider, 
                                       String persistenceUnitName, Map<?, ?> properties) {
        this.provider = provider;
        this.persistenceUnitName = persistenceUnitName;
        if (properties != null && !properties.isEmpty()) {
            Map<String, Object> copy = new HashMap<>();
            for (Map.Entry<?, ?> entry : properties.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            this.properties = Collections.unmodifiableMap(copy);
        } else {
            this.properties = Collections.emptyMap();
        }
        
        // M8-16: Initialize runtime components
        this.dialect = createDialect();
        this.connectionProvider = createConnectionProvider();
        this.entityClasses = Map.of();
        this.entityModels = Map.of();
    }
    
    /**
     * Creates a Dialect instance using ServiceLoader.
     * Falls back to H2 dialect if no factory is found.
     */
    private Dialect createDialect() {
        ServiceLoader<DialectFactory> loader = ServiceLoader.load(DialectFactory.class);
        for (DialectFactory factory : loader) {
            try {
                return factory.create();
            } catch (Exception e) {
                // Try next factory
            }
        }
        // Fallback: try to instantiate H2 dialect directly
        try {
            Class<?> h2DialectClass = Class.forName("io.vidocq.mansart.data.dialect.h2.H2Dialect");
            return (Dialect) h2DialectClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("No DialectFactory found and H2 fallback failed", e);
        }
    }
    
    /**
     * Creates a ConnectionProvider from JDBC properties.
     */
    private JpqlExecutor.ConnectionProvider createConnectionProvider() {
        final String url = (String) properties.get("jakarta.persistence.jdbc.url");
        final String user = (String) properties.get("jakarta.persistence.jdbc.user");
        final String password = (String) properties.get("jakarta.persistence.jdbc.password");
        
        final String effectiveUrl;
        final String effectiveUser;
        final String effectivePassword;
        
        if (url == null) {
            // For tests without explicit config, use H2 in-memory with DB_CLOSE_DELAY
            effectiveUrl = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
            effectiveUser = "sa";
            effectivePassword = "";
        } else {
            effectiveUrl = url;
            effectiveUser = user != null ? user : "sa";
            effectivePassword = password != null ? password : "";
        }
        
        return () -> DriverManager.getConnection(effectiveUrl, effectiveUser, effectivePassword);
    }

    @Override
    public EntityManager createEntityManager() {
        return createMansartEntityManager(Collections.emptyMap(), SynchronizationType.SYNCHRONIZED);
    }

    @Override
    public EntityManager createEntityManager(Map<?, ?> map) {
        return createMansartEntityManager(map, SynchronizationType.SYNCHRONIZED);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType) {
        return createMansartEntityManager(Collections.emptyMap(), synchronizationType);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType, Map<?, ?> map) {
        return createMansartEntityManager(map, synchronizationType);
    }

    private EntityManager createMansartEntityManager(Map<?, ?> map, SynchronizationType syncType) {
        return new MansartEntityManager(this, null, dialect, entityClasses, entityModels, connectionProvider);
    }

    @Override
    public void close() {
        open = false;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public String getName() {
        return persistenceUnitName;
    }

    @Override
    public Metamodel getMetamodel() {
        return null; // TODO: Implement in M8
    }

    @Override
    public jakarta.persistence.Cache getCache() {
        return null; // TODO: Implement in M9
    }

    @Override
    public jakarta.persistence.PersistenceUnitUtil getPersistenceUnitUtil() {
        return null; // TODO: Implement
    }

    @Override
    public jakarta.persistence.PersistenceUnitTransactionType getTransactionType() {
        return jakarta.persistence.PersistenceUnitTransactionType.RESOURCE_LOCAL; // Default
    }

    @Override
    public jakarta.persistence.SchemaManager getSchemaManager() {
        return null; // TODO: Implement schema management
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return null; // TODO: Implement Criteria API in M8
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        throw new IllegalArgumentException("Cannot unwrap to " + type.getName());
    }

    @Override
    public <T> void addNamedEntityGraph(String name, EntityGraph<T> entityGraph) {
        // TODO: Implement named entity graphs
    }

    @Override
    public <E> Map<String, EntityGraph<? extends E>> getNamedEntityGraphs(Class<E> entityClass) {
        return Collections.emptyMap();
    }

    @Override
    public void addNamedQuery(String name, Query query) {
        // TODO: Implement named queries
    }

    @Override
    public <R> Map<String, TypedQueryReference<R>> getNamedQueries(Class<R> resultClass) {
        return Collections.emptyMap();
    }

    @Override
    public <R> R callInTransaction(Function<EntityManager, R> function) {
        EntityManager em = createEntityManager();
        try {
            jakarta.persistence.EntityTransaction tx = em.getTransaction();
            tx.begin();
            R result = function.apply(em);
            tx.commit();
            return result;
        } catch (Exception e) {
            jakarta.persistence.EntityTransaction tx = em.getTransaction();
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    @Override
    public void runInTransaction(Consumer<EntityManager> consumer) {
        EntityManager em = createEntityManager();
        try {
            jakarta.persistence.EntityTransaction tx = em.getTransaction();
            tx.begin();
            consumer.accept(em);
            tx.commit();
        } catch (Exception e) {
            jakarta.persistence.EntityTransaction tx = em.getTransaction();
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    // Helper to access provider
    public io.vidocq.mansart.persistence.core.bootstrap.MansartPersistenceProvider getProvider() {
        return provider;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }
}
