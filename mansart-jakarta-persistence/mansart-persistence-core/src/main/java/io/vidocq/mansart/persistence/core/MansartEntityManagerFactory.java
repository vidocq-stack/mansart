/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import io.vidocq.mansart.persistence.core.runtime.MansartCallback;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.transaction.TransactionManager;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Mansart implementation of Jakarta Persistence EntityManagerFactory.
 *
 * <p>Manages EntityManager instances: every created EM is tracked and
 * closed when the factory closes. Holds bootstrap metadata (transaction
 * type, properties, persistence unit name).
 */
public class MansartEntityManagerFactory implements EntityManagerFactory {

    private final String persistenceUnitName;
    private final PersistenceUnitTransactionType transactionType;
    private final Map<String, Object> properties;
    private final AtomicBoolean open = new AtomicBoolean(true);
    private final ConcurrentLinkedQueue<MansartEntityManager> entityManagers = new ConcurrentLinkedQueue<>();
    private final MansartCallback callback = new MansartCallback();
    private final TransactionManager transactionManager;

    public MansartEntityManagerFactory(String persistenceUnitName,
                                       PersistenceUnitTransactionType transactionType,
                                       Map<String, Object> properties) {
        this(persistenceUnitName, transactionType, properties, null);
    }

    public MansartEntityManagerFactory(String persistenceUnitName,
                                       PersistenceUnitTransactionType transactionType,
                                       Map<String, Object> properties,
                                       TransactionManager transactionManager) {
        this.persistenceUnitName = persistenceUnitName;
        this.transactionType = transactionType;
        this.properties = Collections.unmodifiableMap(
                new LinkedHashMap<>(properties == null ? Map.of() : properties));
        this.transactionManager = transactionManager;
    }

    private void ensureOpen() {
        if (!open.get()) {
            throw new IllegalStateException("EntityManagerFactory is closed");
        }
    }

    void unregisterEntityManager(MansartEntityManager em) {
        entityManagers.remove(em);
    }

    @Override
    public EntityManager createEntityManager() {
        return createTrackedEm(properties);
    }

    @Override
    public EntityManager createEntityManager(Map<?, ?> map) {
        Map<String, Object> emProps = mergeProperties(properties, map);
        return createTrackedEm(emProps);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType) {
        ensureOpen();
        requireJta(synchronizationType);
        return createTrackedEm(properties);
    }

    @Override
    public EntityManager createEntityManager(SynchronizationType synchronizationType, Map<?, ?> map) {
        ensureOpen();
        requireJta(synchronizationType);
        Map<String, Object> emProps = mergeProperties(properties, map);
        return createTrackedEm(emProps);
    }

    private MansartEntityManager createTrackedEm(Map<String, Object> emProps) {
        ensureOpen();
        MansartEntityManager em = new MansartEntityManager(this, emProps, callback, transactionType, transactionManager);
        entityManagers.add(em);
        // Guard against a race where the factory closes between ensureOpen and add
        if (!open.get()) {
            em.markClosed();
            entityManagers.remove(em);
            throw new IllegalStateException("EntityManagerFactory is closed");
        }
        return em;
    }

    private void requireJta(SynchronizationType synchronizationType) {
        if (transactionType != PersistenceUnitTransactionType.JTA) {
            throw new IllegalStateException(
                    "SynchronizationType " + synchronizationType + " is only valid for JTA persistence units");
        }
    }

    private static Map<String, Object> mergeProperties(Map<String, Object> base, Map<?, ?> overlay) {
        Map<String, Object> merged = new LinkedHashMap<>(base);
        if (overlay != null) {
            for (Map.Entry<?, ?> entry : overlay.entrySet()) {
                Object key = entry.getKey();
                if (key != null) {
                    merged.put(String.valueOf(key), entry.getValue());
                }
            }
        }
        return Collections.unmodifiableMap(merged);
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        throw new UnsupportedOperationException("not implemented: getCriteriaBuilder");
    }

    @Override
    public Metamodel getMetamodel() {
        throw new UnsupportedOperationException("not implemented: getMetamodel");
    }

    @Override
    public boolean isOpen() {
        return open.get();
    }

    @Override
    public void close() {
        if (!open.compareAndSet(true, false)) {
            throw new IllegalStateException("EntityManagerFactory is closed");
        }
        for (MansartEntityManager em : entityManagers) {
            em.markClosed();
        }
        entityManagers.clear();
    }

    @Override
    public String getName() {
        return persistenceUnitName;
    }

    @Override
    public Map<String, Object> getProperties() {
        ensureOpen();
        return properties;
    }

    @Override
    public Cache getCache() {
        ensureOpen();
        // No second-level cache is in use; returning null is explicitly sanctioned
        return null;
    }

    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil() {
        ensureOpen();
        return new MansartPersistenceUnitUtil();
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return transactionType;
    }

    @Override
    public SchemaManager getSchemaManager() {
        throw new UnsupportedOperationException("not implemented: getSchemaManager");
    }

    @Override
    public void addNamedQuery(String name, Query query) {
        throw new UnsupportedOperationException("not implemented: addNamedQuery");
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        ensureOpen();
        if (cls.isInstance(this)) {
            return cls.cast(this);
        }
        throw new PersistenceException("Cannot unwrap to " + cls.getName());
    }

    @Override
    public <T> void addNamedEntityGraph(String name, EntityGraph<T> graph) {
        throw new UnsupportedOperationException("not implemented: addNamedEntityGraph");
    }

    @Override
    public <R> Map<String, TypedQueryReference<R>> getNamedQueries(Class<R> resultType) {
        throw new UnsupportedOperationException("not implemented: getNamedQueries");
    }

    @Override
    public <E> Map<String, EntityGraph<? extends E>> getNamedEntityGraphs(Class<E> entityClass) {
        throw new UnsupportedOperationException("not implemented: getNamedEntityGraphs");
    }

    @Override
    public void runInTransaction(Consumer<EntityManager> consumer) {
        throw new UnsupportedOperationException("not implemented: runInTransaction");
    }

    @Override
    public <R> R callInTransaction(Function<EntityManager, R> function) {
        throw new UnsupportedOperationException("not implemented: callInTransaction");
    }
}
