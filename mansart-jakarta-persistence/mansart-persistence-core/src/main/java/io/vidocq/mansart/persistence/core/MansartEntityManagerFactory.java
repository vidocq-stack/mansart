/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Mansart implementation of Jakarta Persistence EntityManagerFactory.
 * All methods throw UnsupportedOperationException - stub for JP-01.
 */
public class MansartEntityManagerFactory implements EntityManagerFactory {

    private final String persistenceUnitName;
    private final Map<String, Object> properties;

    public MansartEntityManagerFactory(String persistenceUnitName, Map<String, Object> properties) {
        this.persistenceUnitName = persistenceUnitName;
        this.properties = properties;
    }

    @Override public EntityManager createEntityManager() { 
        return new MansartEntityManager(this, properties); 
    }
    @Override public EntityManager createEntityManager(Map<?, ?> map) { 
        return new MansartEntityManager(this, (Map<String, Object>) map); 
    }
    @Override public EntityManager createEntityManager(SynchronizationType synchronizationType) { 
        return new MansartEntityManager(this, properties); 
    }
    @Override public EntityManager createEntityManager(SynchronizationType synchronizationType, Map<?, ?> map) { 
        return new MansartEntityManager(this, (Map<String, Object>) map); 
    }
    @Override public CriteriaBuilder getCriteriaBuilder() { 
        throw new UnsupportedOperationException("not implemented: getCriteriaBuilder"); 
    }
    @Override public Metamodel getMetamodel() { 
        throw new UnsupportedOperationException("not implemented: getMetamodel"); 
    }
    @Override public boolean isOpen() { 
        return false; 
    }
    @Override public void close() { 
        throw new UnsupportedOperationException("not implemented: close"); 
    }
    @Override public String getName() { 
        return persistenceUnitName; 
    }
    @Override public Map<String, Object> getProperties() { 
        return properties; 
    }
    @Override public Cache getCache() { 
        throw new UnsupportedOperationException("not implemented: getCache"); 
    }
    @Override public PersistenceUnitUtil getPersistenceUnitUtil() { 
        throw new UnsupportedOperationException("not implemented: getPersistenceUnitUtil"); 
    }
    @Override public PersistenceUnitTransactionType getTransactionType() { 
        throw new UnsupportedOperationException("not implemented: getTransactionType"); 
    }
    @Override public SchemaManager getSchemaManager() { 
        throw new UnsupportedOperationException("not implemented: getSchemaManager"); 
    }
    @Override public void addNamedQuery(String name, Query query) { 
        throw new UnsupportedOperationException("not implemented: addNamedQuery"); 
    }
    @Override public <T> T unwrap(Class<T> cls) { 
        throw new UnsupportedOperationException("not implemented: unwrap"); 
    }
    @Override public <T> void addNamedEntityGraph(String name, EntityGraph<T> graph) { 
        throw new UnsupportedOperationException("not implemented: addNamedEntityGraph"); 
    }
    @Override public <R> Map<String, TypedQueryReference<R>> getNamedQueries(Class<R> resultType) { 
        throw new UnsupportedOperationException("not implemented: getNamedQueries"); 
    }
    @Override public <E> Map<String, EntityGraph<? extends E>> getNamedEntityGraphs(Class<E> entityClass) { 
        throw new UnsupportedOperationException("not implemented: getNamedEntityGraphs"); 
    }
    @Override public void runInTransaction(Consumer<EntityManager> consumer) { 
        throw new UnsupportedOperationException("not implemented: runInTransaction"); 
    }
    @Override public <R> R callInTransaction(Function<EntityManager, R> function) { 
        throw new UnsupportedOperationException("not implemented: callInTransaction"); 
    }
    
    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }
}
