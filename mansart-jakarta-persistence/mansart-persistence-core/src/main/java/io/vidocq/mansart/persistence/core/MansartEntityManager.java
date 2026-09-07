/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.DialectFactory;
import io.vidocq.mansart.persistence.core.context.MansartPersistenceContext;
import io.vidocq.mansart.persistence.core.dialect.DialectEntityModelAdapter;
import io.vidocq.mansart.persistence.core.dialect.EntityMapper;
import io.vidocq.mansart.persistence.core.jpql.JpqlAst;
import io.vidocq.mansart.persistence.core.jpql.JpqlParser;
import io.vidocq.mansart.persistence.core.jpql.JpqlQueryExecutor;
import io.vidocq.mansart.persistence.core.jpql.MansartTypedQuery;
import io.vidocq.mansart.persistence.core.runtime.MansartCallback;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mansart implementation of Jakarta Persistence EntityManager.
 *
 * <p>Lifecycle is managed jointly with the owning {@link MansartEntityManagerFactory}:
 * when the factory closes, all tracked EMs are marked closed via {@link #markClosed()}.
 *
 * <p>Closed-state contract (per the {@code EntityManager.close()} Javadoc): after
 * {@link #close()} every method throws {@link IllegalStateException} except
 * {@link #isOpen()}, {@link #getProperties()} and {@link #getTransaction()}.
 *
 * <p>Entity-state operations (persist, find, merge, remove, refresh, flush, contains,
 * detach, clear, lock) are delegated to the {@link MansartPersistenceContext}; the
 * state machine itself is implemented in card M4-JP-26.
 */
public class MansartEntityManager implements EntityManager {

    private final MansartEntityManagerFactory entityManagerFactory;
    private final Map<String, Object> properties;
    private final AtomicBoolean open = new AtomicBoolean(true);
    private final ConcurrentHashMap<String, Object> propertyOverrides = new ConcurrentHashMap<>();
    private final MansartPersistenceContext persistenceContext;
    private final PersistenceUnitTransactionType transactionType;
    private final MansartEntityTransaction entityTransaction;
    private final TransactionManager transactionManager;
    private final DataSource dataSource;
    private volatile boolean joinedToJtaTransaction = false;

    private final Dialect dialect;
    private final MansartCallback callback;
    private final DialectEntityModelAdapter adapter;

    private volatile FlushModeType flushMode = FlushModeType.AUTO;
    private volatile CacheRetrieveMode cacheRetrieveMode = CacheRetrieveMode.USE;
    private volatile CacheStoreMode cacheStoreMode = CacheStoreMode.USE;

    public MansartEntityManager(MansartEntityManagerFactory entityManagerFactory, Map<String, Object> properties) {
        this(entityManagerFactory, properties, null, PersistenceUnitTransactionType.RESOURCE_LOCAL, null);
    }

    public MansartEntityManager(MansartEntityManagerFactory entityManagerFactory, Map<String, Object> properties,
                               MansartCallback callback) {
        this(entityManagerFactory, properties, callback, PersistenceUnitTransactionType.RESOURCE_LOCAL, null);
    }

    public MansartEntityManager(MansartEntityManagerFactory entityManagerFactory, Map<String, Object> properties,
                               MansartCallback callback, PersistenceUnitTransactionType transactionType,
                               TransactionManager transactionManager) {
        this(entityManagerFactory, properties, callback, transactionType, transactionManager, null);
    }

    public MansartEntityManager(MansartEntityManagerFactory entityManagerFactory, Map<String, Object> properties,
                               MansartCallback callback, PersistenceUnitTransactionType transactionType,
                               TransactionManager transactionManager, DataSource dataSource) {
        this.entityManagerFactory = entityManagerFactory;
        this.properties = properties;
        this.callback = callback;
        this.dataSource = dataSource;
        
        // Resolve dialect and create EntityMapper if dataSource is available
        Dialect dialect = null;
        EntityMapper entityMapper = null;
        if (dataSource != null) {
            dialect = resolveDialect(dataSource);
            this.adapter = new DialectEntityModelAdapter();
            entityMapper = new EntityMapper(dialect, callback, this.adapter);
        } else {
            this.adapter = null;
        }
        this.dialect = dialect;
        
        this.persistenceContext = new MansartPersistenceContext(callback, entityMapper, dataSource);
        this.transactionType = transactionType;
        this.transactionManager = transactionManager;
        this.entityTransaction = new MansartEntityTransaction();
    }

    private void ensureOpen() {
        if (!open.get()) {
            throw new IllegalStateException("EntityManager is closed");
        }
    }

    // ── Entity-state operations: delegated to the persistence context ────

    @Override public void persist(Object entity) { ensureOpen(); persistenceContext.persist(entity); }
    @Override public <T> T merge(T entity) { ensureOpen(); return persistenceContext.merge(entity); }
    @Override public void remove(Object entity) { ensureOpen(); persistenceContext.remove(entity); }
    @Override public <T> T find(Class<T> entityClass, Object primaryKey) { ensureOpen(); return persistenceContext.find(entityClass, primaryKey); }
    @Override public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> props) { ensureOpen(); return persistenceContext.find(entityClass, primaryKey, props); }
    @Override public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) { ensureOpen(); return persistenceContext.find(entityClass, primaryKey, lockMode); }
    @Override public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> props) { ensureOpen(); return persistenceContext.find(entityClass, primaryKey, lockMode, props); }
    @Override public <T> T find(Class<T> entityClass, Object primaryKey, FindOption... options) { ensureOpen(); return persistenceContext.find(entityClass, primaryKey, options); }
    @Override public <T> T find(EntityGraph<T> entityGraph, Object primaryKey, FindOption... options) { ensureOpen(); return persistenceContext.find(entityGraph, primaryKey, options); }
    @Override public <T> T getReference(Class<T> entityClass, Object primaryKey) { ensureOpen(); return persistenceContext.getReference(entityClass, primaryKey); }
    @Override public <T> T getReference(T entity) { ensureOpen(); return persistenceContext.getReference(entity); }
    @Override public void flush() { ensureOpen(); persistenceContext.flush(); }
    @Override public void refresh(Object entity) { ensureOpen(); persistenceContext.refresh(entity); }
    @Override public void refresh(Object entity, Map<String, Object> props) { ensureOpen(); persistenceContext.refresh(entity, props); }
    @Override public void refresh(Object entity, LockModeType lockMode) { ensureOpen(); persistenceContext.refresh(entity, lockMode); }
    @Override public void refresh(Object entity, LockModeType lockMode, Map<String, Object> props) { ensureOpen(); persistenceContext.refresh(entity, lockMode, props); }
    @Override public void refresh(Object entity, RefreshOption... options) { ensureOpen(); persistenceContext.refresh(entity, options); }
    @Override public void clear() { ensureOpen(); persistenceContext.clear(); }
    @Override public void detach(Object entity) { ensureOpen(); persistenceContext.detach(entity); }
    @Override public boolean contains(Object entity) { ensureOpen(); return persistenceContext.contains(entity); }
    @Override public void lock(Object entity, LockModeType lockMode) { ensureOpen(); persistenceContext.lock(entity, lockMode); }
    @Override public void lock(Object entity, LockModeType lockMode, Map<String, Object> props) { ensureOpen(); persistenceContext.lock(entity, lockMode, props); }
    @Override public void lock(Object entity, LockModeType lockMode, LockOption... options) { ensureOpen(); persistenceContext.lock(entity, lockMode, options); }
    @Override public LockModeType getLockMode(Object entity) { ensureOpen(); return persistenceContext.getLockMode(entity); }

    // ── Configuration: flush and cache modes ─────────────────────────────

    @Override public void setFlushMode(FlushModeType flushModeType) {
        ensureOpen();
        this.flushMode = flushModeType;
    }
    @Override public FlushModeType getFlushMode() {
        ensureOpen();
        return this.flushMode;
    }
    @Override public void setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        ensureOpen();
        this.cacheRetrieveMode = cacheRetrieveMode;
    }
    @Override public CacheRetrieveMode getCacheRetrieveMode() {
        ensureOpen();
        return this.cacheRetrieveMode;
    }
    @Override public void setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        ensureOpen();
        this.cacheStoreMode = cacheStoreMode;
    }
    @Override public CacheStoreMode getCacheStoreMode() {
        ensureOpen();
        return this.cacheStoreMode;
    }

    // ── Properties ──────────────────────────────────────────────────────

    @Override public void setProperty(String name, Object value) {
        ensureOpen();
        propertyOverrides.put(name, value);
    }

    /**
     * Returns the properties in effect for this EntityManager: the factory
     * properties overlaid with any overrides set via {@link #setProperty}.
     * Exempt from the closed-state contract, so this does not throw if the
     * EntityManager has been closed.
     */
    @Override public Map<String, Object> getProperties() {
        Map<String, Object> merged = new LinkedHashMap<>(properties == null ? Map.of() : properties);
        merged.putAll(propertyOverrides);
        return Collections.unmodifiableMap(merged);
    }

    // ── Query operations (JPQL/Criteria — M6, native — M12) ──────────────

    @Override public Query createQuery(String qlString) {
        ensureOpen();
        if (dialect == null || adapter == null) {
            throw new PersistenceException("EntityManager has no DataSource; cannot execute queries");
        }
        JpqlAst.JpqlStatement stmt = new JpqlParser().parse(qlString);
        if (!(stmt instanceof JpqlAst.SelectStatement select)) {
            throw new UnsupportedOperationException("Only SELECT queries are supported");
        }
        JpqlQueryExecutor executor = new JpqlQueryExecutor(dialect, callback, adapter);
        JpqlQueryExecutor.QueryPlan plan = executor.plan(select);
        return new MansartTypedQuery<>(plan.sqlFragment(), dialect, dataSource, plan.entityClass(), callback, plan.dialectModel(), plan.bindParameters());
    }
    @Override public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createQuery"); }
    @Override public <T> TypedQuery<T> createQuery(CriteriaSelect<T> criteriaSelect) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createQuery"); }
    @Override public Query createQuery(CriteriaUpdate<?> criteriaUpdate) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createQuery"); }
    @Override public Query createQuery(CriteriaDelete<?> criteriaDelete) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createQuery"); }
    @Override public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
        ensureOpen();
        if (dialect == null || adapter == null) {
            throw new PersistenceException("EntityManager has no DataSource; cannot execute queries");
        }
        JpqlAst.JpqlStatement stmt = new JpqlParser().parse(qlString);
        if (!(stmt instanceof JpqlAst.SelectStatement select)) {
            throw new UnsupportedOperationException("Only SELECT queries are supported");
        }
        JpqlQueryExecutor executor = new JpqlQueryExecutor(dialect, callback, adapter);
        JpqlQueryExecutor.QueryPlan plan = executor.plan(select);
        // Ensure the result class matches the entity class from the query
        if (!plan.entityClass().isAssignableFrom(resultClass)) {
            throw new IllegalArgumentException("Result class " + resultClass.getName() + " is not assignable from entity class " + plan.entityClass().getName());
        }
        return new MansartTypedQuery<>(plan.sqlFragment(), dialect, dataSource, resultClass, callback, plan.dialectModel(), plan.bindParameters());
    }
    @Override public Query createNamedQuery(String name) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createNamedQuery"); }
    @Override public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createNamedQuery"); }
    @Override public <T> TypedQuery<T> createQuery(TypedQueryReference<T> typedQueryReference) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createQuery"); }
    @Override public Query createNativeQuery(String sqlString) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createNativeQuery"); }
    @Override public <T> Query createNativeQuery(String sqlString, Class<T> resultClass) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createNativeQuery"); }
    @Override public Query createNativeQuery(String sqlString, String resultSetMapping) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createNativeQuery"); }
    @Override public StoredProcedureQuery createNamedStoredProcedureQuery(String name) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createNamedStoredProcedureQuery"); }
    @Override public StoredProcedureQuery createStoredProcedureQuery(String procedureName) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createStoredProcedureQuery"); }
    @Override public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class<?>... resultClasses) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createStoredProcedureQuery"); }
    @Override public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createStoredProcedureQuery"); }

    // ── Transaction integration (M4-JP-27) / unwrap / delegate ────────────

    @Override
    public void joinTransaction() {
        ensureOpen();
        if (transactionType == PersistenceUnitTransactionType.RESOURCE_LOCAL) {
            // RESOURCE_LOCAL: joinTransaction is a no-op; the EM is always associated with its own resource transaction
            return;
        }
        // JTA: must join the active JTA transaction
        if (transactionManager == null) {
            throw new TransactionRequiredException("no JTA transaction manager available");
        }
        try {
            int status = transactionManager.getStatus();
            if (status == Status.STATUS_ACTIVE || status == Status.STATUS_MARKED_ROLLBACK) {
                joinedToJtaTransaction = true;
            } else {
                throw new TransactionRequiredException("no JTA transaction active");
            }
        } catch (Exception ex) {
            throw new TransactionRequiredException("failed to join JTA transaction: " + ex.getMessage());
        }
    }

    @Override
    public boolean isJoinedToTransaction() {
        ensureOpen();
        if (transactionType == PersistenceUnitTransactionType.RESOURCE_LOCAL) {
            return entityTransaction.isActive();
        }
        // JTA: return whether we are joined to an active JTA transaction
        if (transactionManager == null) {
            return false;
        }
        try {
            int status = transactionManager.getStatus();
            return status == Status.STATUS_ACTIVE && joinedToJtaTransaction;
        } catch (Exception _) {
            return false;
        }
    }

    private static Dialect resolveDialect(DataSource ds) {
        try (Connection c = ds.getConnection()) {
            var md = c.getMetaData();
            for (DialectFactory factory : ServiceLoader.load(DialectFactory.class)) {
                if (factory.supports(md)) return factory.create();
            }
            throw new IllegalStateException("No DialectFactory accepts database product '"
                    + md.getDatabaseProductName() + "'. Add a mansart-data-dialect-* JAR to the module path.");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to detect dialect from DataSource", e);
        }
    }

    @Override public Object getDelegate() { ensureOpen(); return this; }
    @Override public <T> T unwrap(Class<T> cls) {
        ensureOpen();
        if (cls.isInstance(this)) {
            return cls.cast(this);
        }
        throw new PersistenceException("Cannot unwrap to " + cls.getName());
    }

    // ── Lifecycle ───────────────────────────────────────────────────────

    @Override public void close() {
        if (!open.compareAndSet(true, false)) {
            throw new IllegalStateException("EntityManager is closed");
        }
        entityManagerFactory.unregisterEntityManager(this);
    }
    @Override public boolean isOpen() {
        return open.get() && entityManagerFactory.isOpen();
    }

    /**
     * Marks this EntityManager as closed without deregistering from the factory.
     * Called by {@link MansartEntityManagerFactory#close()} when cascading close
     * to all tracked entity managers.
     */
    void markClosed() {
        open.set(false);
    }

    // ── Metadata accessors ──────────────────────────────────────────────

    @Override
    public EntityTransaction getTransaction() {
        // NO ensureOpen() call — this method is exempt from the closed-state contract per the EntityManager.close() Javadoc
        if (transactionType == PersistenceUnitTransactionType.JTA) {
            throw new IllegalStateException("getTransaction() not allowed for JTA persistence unit");
        }
        return this.entityTransaction;
    }
    @Override public EntityManagerFactory getEntityManagerFactory() { ensureOpen(); return entityManagerFactory; }
    @Override public CriteriaBuilder getCriteriaBuilder() { ensureOpen(); throw new UnsupportedOperationException("not implemented: getCriteriaBuilder"); }
    @Override public Metamodel getMetamodel() { ensureOpen(); throw new UnsupportedOperationException("not implemented: getMetamodel"); }
    @Override public <T> EntityGraph<T> createEntityGraph(Class<T> entityClass) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createEntityGraph"); }
    @Override public EntityGraph<?> createEntityGraph(String graphName) { ensureOpen(); throw new UnsupportedOperationException("not implemented: createEntityGraph"); }
    @Override public EntityGraph<?> getEntityGraph(String name) { ensureOpen(); throw new UnsupportedOperationException("not implemented: getEntityGraph"); }
    @Override public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) { ensureOpen(); throw new UnsupportedOperationException("not implemented: getEntityGraphs"); }
    private <C> void executeWithConnection(ConnectionConsumer<C> consumer, Connection connection) {
        try {
            // Cast to C - the generic type is expected to be the connection type
            @SuppressWarnings("unchecked")
            C conn = (C) connection;
            consumer.accept(conn);
        } catch (Exception e) {
            if (entityTransaction.isActive()) {
                entityTransaction.setRollbackOnly();
            }
            throw new PersistenceException("Error in ConnectionConsumer", e);
        }
    }

    public <C> void runWithConnection(ConnectionConsumer<C> consumer) {
        ensureOpen();
        if (dataSource == null) {
            throw new PersistenceException("no DataSource available");
        }
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            executeWithConnection(consumer, connection);
        } catch (SQLException e) {
            throw new PersistenceException("Error obtaining connection", e);
        } finally {
            // Do NOT close the connection - the spec says the action should not close it
            // The connection will be managed by the persistence context/transaction
        }
    }

    private <C, T> T executeWithConnection(ConnectionFunction<C, T> function, Connection connection) {
        try {
            // Cast to C - the generic type is expected to be the connection type
            @SuppressWarnings("unchecked")
            C conn = (C) connection;
            return function.apply(conn);
        } catch (Exception e) {
            if (entityTransaction.isActive()) {
                entityTransaction.setRollbackOnly();
            }
            throw new PersistenceException("Error in ConnectionFunction", e);
        }
    }

    @Override
    public <C, T> T callWithConnection(ConnectionFunction<C, T> function) {
        ensureOpen();
        if (dataSource == null) {
            throw new PersistenceException("no DataSource available");
        }
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            return executeWithConnection(function, connection);
        } catch (SQLException e) {
            throw new PersistenceException("Error obtaining connection", e);
        }
    }
}
