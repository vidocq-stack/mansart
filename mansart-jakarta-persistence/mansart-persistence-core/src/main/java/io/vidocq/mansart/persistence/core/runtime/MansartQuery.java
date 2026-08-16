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
package io.vidocq.mansart.persistence.core.runtime;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.persistence.core.jpql.JPQLQuery;
import io.vidocq.mansart.persistence.core.jpql.JpqlExecutor;
import io.vidocq.mansart.persistence.core.jpql.JpqlToSqlConverter;
import io.vidocq.mansart.persistence.core.jpql.QueryCache;

/**
 * Mutable query object wrapping a parsed JPQL AST for M7-13.
 * Implements {@link Query}; {@link Generic} adds {@code TypedQuery<V>}.
 *
 * <p>Deprecated date/calendar parameter methods exist in JPA 3.2 but would cause
 * erasure collisions. Throws UnsupportedOperationException at M7-13.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class MansartQuery implements Query {

    private final JPQLQuery parsedQuery;
    private final Map<String, Object> namedParameters = new ConcurrentHashMap<>();
    private final List<Object> positionParameters = java.util.Collections.synchronizedList(new java.util.LinkedList<>());
    private final Map<String, Object> hints = new ConcurrentHashMap<>();
    private int maxResults = Integer.MAX_VALUE;
    private int firstResult = 0;
    private FlushModeType flushMode;
    private int timeout = 0;

    // M7-13: Execution support
    private final Dialect dialect;
    private final JpqlExecutor.ConnectionProvider connectionProvider;
    private final Map<Class<?>, EntityModel<?>> entityModels;
    private final Map<String, Class<?>> entityClasses;
    
    // M9-8: Query cache for JPQL parsing optimization
    private final QueryCache queryCache;

    /**
     * Creates a query with execution capability.
     *
     * @param parsedQuery the parsed JPQL query
     * @param dialect the SQL dialect
     * @param connectionProvider provider for JDBC connections
     * @param entityModels map of entity classes to their EntityModel
     * @param entityClasses map of entity names to entity classes
     */
    MansartQuery(JPQLQuery<?> parsedQuery, Dialect dialect,
                JpqlExecutor.ConnectionProvider connectionProvider,
                Map<Class<?>, EntityModel<?>> entityModels, Map<String, Class<?>> entityClasses) {
        this(parsedQuery, dialect, connectionProvider, entityModels, entityClasses, null);
    }
    
    /**
     * Creates a query with execution capability and query caching.
     *
     * @param parsedQuery the parsed JPQL query
     * @param dialect the SQL dialect
     * @param connectionProvider provider for JDBC connections
     * @param entityModels map of entity classes to their EntityModel
     * @param entityClasses map of entity names to entity classes
     * @param queryCache the query cache for JPQL parsing optimization (may be null)
     */
    MansartQuery(JPQLQuery<?> parsedQuery, Dialect dialect,
                JpqlExecutor.ConnectionProvider connectionProvider,
                Map<Class<?>, EntityModel<?>> entityModels, Map<String, Class<?>> entityClasses,
                QueryCache queryCache) {
        this.parsedQuery = parsedQuery;
        this.dialect = dialect;
        this.connectionProvider = connectionProvider;
        this.entityModels = entityModels;
        this.entityClasses = entityClasses;
        this.queryCache = queryCache;
    }

    /**
     * Creates a query without execution capability (for backward compatibility).
     * This constructor creates a query that will throw when getResultList() is called.
     */
    MansartQuery(JPQLQuery<?> parsedQuery) {
        this(parsedQuery, null, null, Map.of(), Map.of(), null);
    }

    // ========================================================================
    // Query methods
    // ========================================================================

    @Override
    public int executeUpdate() {
        throw new UnsupportedOperationException(
                "UPDATE/DELETE JPQL not yet supported at M7-13");
    }

    @Override
    public Object getSingleResult() {
        var list = getResultList();
        if (list.isEmpty()) throw new jakarta.persistence.NoResultException();
        if (list.size() > 1) throw new jakarta.persistence.NonUniqueResultException();
        return list.get(0);
    }

    @Override
    public Object getSingleResultOrNull() {
        var list = getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List getResultList() {
        // M7-13: Execute the query via JpqlExecutor
        if (dialect == null || connectionProvider == null) {
            throw new jakarta.persistence.PersistenceException(
                    "Query execution not configured. Dialect and ConnectionProvider are required.");
        }
        
        try {
            // M9-8: Pass queryCache to JpqlExecutor for JPQL parsing optimization
            JpqlExecutor executor = new JpqlExecutor(dialect, connectionProvider, entityModels, entityClasses, queryCache);
            int actualMaxResults = maxResults == Integer.MAX_VALUE ? 0 : maxResults;
            return executor.execute(parsedQuery, namedParameters, positionParameters, actualMaxResults, firstResult);
        } catch (Exception e) {
            throw new jakarta.persistence.PersistenceException("Failed to execute query: " + e.getMessage(), e);
        }
    }

    @Override
    public Query setMaxResults(int maxResult) {
        this.maxResults = maxResult;
        return this;
    }

    @Override
    public int getMaxResults() {
        return maxResults == Integer.MAX_VALUE ? 0 : maxResults;
    }

    @Override
    public Query setFirstResult(int startPosition) {
        this.firstResult = startPosition;
        return this;
    }

    @Override
    public int getFirstResult() {
        return firstResult;
    }

    @Override
    public Query setParameter(String name, Object value) {
        namedParameters.put(name, value);
        return this;
    }

    @Override
    public Query setParameter(int position, Object value) {
        while (positionParameters.size() <= position) {
            positionParameters.add(null);
        }
        positionParameters.set(position, value);
        return this;
    }

    @Override
    public Query setParameter(Parameter<Calendar> param, Calendar value, jakarta.persistence.TemporalType temporalType) {
        return setParameter(param, value);
    }

    @Override
    public Query setParameter(Parameter<java.util.Date> param, java.util.Date value, jakarta.persistence.TemporalType temporalType) {
        return setParameter(param, value);
    }

    @Override
    public <T> Query setParameter(Parameter<T> param, T value) {
        if (param != null) {
            namedParameters.put(param.getName(), value);
        }
        return this;
    }

    @Override
    public boolean isBound(Parameter<?> param) {
        if (param == null) return false;
        return namedParameters.containsKey(param.getName());
    }

    @Override
    public Object getParameterValue(String name) {
        return namedParameters.get(name);
    }

    @Override
    public Object getParameterValue(int position) {
        if (position >= 0 && position < positionParameters.size()) {
            return positionParameters.get(position);
        }
        return null;
    }

    @Override
    public <T> T getParameterValue(Parameter<T> param) {
        if (param == null) return null;
        return (T) namedParameters.get(param.getName());
    }

    @Override
    public Parameter<?> getParameter(int position) {
        // Positional parameters start at 1
        if (position > 0 && position <= positionParameters.size()) {
            Object value = positionParameters.get(position - 1);
            if (value != null) {
                return new MansartParameter<>(position, value.getClass());
            }
        }
        return null;
    }

    @Override
    public <T> Parameter<T> getParameter(int position, Class<T> type) {
        // Return parameter at position with the specified type
        if (position > 0 && position <= positionParameters.size()) {
            Object value = positionParameters.get(position - 1);
            if (value != null) {
                return new MansartParameter<>(position, type);
            }
        }
        return null;
    }

    @Override
    public Parameter<?> getParameter(String name) {
        // Return named parameter with its actual type
        if (name != null && namedParameters.containsKey(name)) {
            Object value = namedParameters.get(name);
            if (value != null) {
                return new MansartParameter<>(name, value.getClass());
            }
        }
        return null;
    }

    @Override
    public <T> Parameter<T> getParameter(String name, Class<T> type) {
        // Return named parameter with the specified type
        if (name != null && namedParameters.containsKey(name)) {
            return new MansartParameter<>(name, type);
        }
        return null;
    }

    @Override
    public Set<Parameter<?>> getParameters() {
        // Return all named and positional parameters as Parameter instances
        Set<Parameter<?>> result = new java.util.HashSet<>();
        
        // Add named parameters
        for (Map.Entry<String, Object> entry : namedParameters.entrySet()) {
            Object value = entry.getValue();
            if (value != null) {
                result.add(new MansartParameter<>(entry.getKey(), value.getClass()));
            }
        }
        
        // Add positional parameters
        for (int i = 0; i < positionParameters.size(); i++) {
            Object value = positionParameters.get(i);
            if (value != null) {
                result.add(new MansartParameter<>(i + 1, value.getClass()));
            }
        }
        
        return result;
    }



    @Override
    public Map<String, Object> getHints() {
        return java.util.Collections.unmodifiableMap(hints);
    }

    @Override
    public Query setHint(String hintName, Object value) {
        if (hintName != null) {
            hints.put(hintName, value);
        }
        return this;
    }

    @Override
    public Query setFlushMode(FlushModeType flushMode) {
        this.flushMode = flushMode;
        return this;
    }

    @Override
    public FlushModeType getFlushMode() {
        return flushMode;
    }

    @Override
    public Query setLockMode(LockModeType lockMode) {
        return this;
    }

    @Override
    public LockModeType getLockMode() {
        return null;
    }

    @Override
    public Integer getTimeout() {
        return timeout;
    }

    @Override
    public Query setTimeout(Integer timeout) {
        this.timeout = timeout;
        return this;
    }

    @Override
    public Query setCacheStoreMode(jakarta.persistence.CacheStoreMode mode) {
        return this;
    }

    @Override
    public Query setCacheRetrieveMode(jakarta.persistence.CacheRetrieveMode mode) {
        return this;
    }

    @Override
    public jakarta.persistence.CacheRetrieveMode getCacheRetrieveMode() {
        return null;
    }

    @Override
    public jakarta.persistence.CacheStoreMode getCacheStoreMode() {
        return null;
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        if (cls == JPQLQuery.class) return (T) parsedQuery;
        throw new jakarta.persistence.PersistenceException("Not supported: " + cls.getName());
    }

    // --- Deprecated date/calendar methods. These cause erasure collisions
    //     with the generic <T> version, so we don't implement the
    //     generic Parameter overloads. ---
    @Deprecated(since = "3.2")
    public Query setParameter(int position, Calendar value, jakarta.persistence.TemporalType type) {
        return setParameter(position, value);
    }

    @Deprecated(since = "3.2")
    public Query setParameter(int position, java.util.Date value, jakarta.persistence.TemporalType type) {
        return setParameter(position, value);
    }

    @Deprecated(since = "3.2")
    public Query setParameter(String name, java.util.Date value, jakarta.persistence.TemporalType type) {
        return setParameter(name, value);
    }

    @Deprecated(since = "3.2")
    public Query setParameter(String name, Calendar value, jakarta.persistence.TemporalType type) {
        return setParameter(name, value);
    }

    // --- Model selector (added in 11) ---
    public JPQLQuery<?> model() {
        return parsedQuery;
    }

    // ========================================================================
    // Generic<V> subtype for TypedQuery<V> at M7-13
    // ========================================================================

    /**
     * Managed subset wraps a {@code TypedQuery} for use inside a
     * {@code Generic<V>} block. Must be static because {@code Generic} does
     * not need the enclosing instance.
     *
     * <p>The {@code TypedQuery} interface requires return-type redeclaration on
     * every method inherited from {@code Query}. {@code Generic} re-defines
     * those methods to turn every {@code Query}-returning call into a
     * {@code TypedQuery<V>}-returning call.
     */
    public static final class Generic<V> extends MansartQuery implements TypedQuery<V> {

        Generic(JPQLQuery<V> q) {
            super(q);
        }

        /**
         * Creates a typed query with execution capability.
         */
        Generic(JPQLQuery<V> q, Dialect dialect, JpqlExecutor.ConnectionProvider connectionProvider,
               Map<Class<?>, EntityModel<?>> entityModels, Map<String, Class<?>> entityClasses) {
            super(q, dialect, connectionProvider, entityModels, entityClasses);
        }
        
        /**
         * Creates a typed query with execution capability and query caching.
         */
        Generic(JPQLQuery<V> q, Dialect dialect, JpqlExecutor.ConnectionProvider connectionProvider,
               Map<Class<?>, EntityModel<?>> entityModels, Map<String, Class<?>> entityClasses,
               QueryCache queryCache) {
            super(q, dialect, connectionProvider, entityModels, entityClasses, queryCache);
        }

        @Override
        public List<V> getResultList() {
            return (List<V>) super.getResultList();
        }

        @Override
        public V getSingleResult() {
            return (V) super.getSingleResult();
        }

        @Override
        public V getSingleResultOrNull() {
            return (V) super.getSingleResultOrNull();
        }

        /** TypedQuery re-declaration of setMaxResults */
        @Override
        public TypedQuery<V> setMaxResults(int maxResult) {
            super.setMaxResults(maxResult);
            return this;
        }

        /** TypedQuery re-declaration of setFirstResult */
        @Override
        public TypedQuery<V> setFirstResult(int startPosition) {
            super.setFirstResult(startPosition);
            return this;
        }

        /** TypedQuery re-declaration of setParameter(String, Object) */
        @Override
        public TypedQuery<V> setParameter(String name, Object value) {
            super.setParameter(name, value);
            return this;
        }

        /** TypedQuery re-declaration of setParameter(int, Object) */
        @Override
        public TypedQuery<V> setParameter(int position, Object value) {
            super.setParameter(position, value);
            return this;
        }

        /** TypedQuery re-declaration of setParameter(Parameter, T) */
        @Override
        public <T> TypedQuery<V> setParameter(Parameter<T> param, T value) {
            super.setParameter(param, value);
            return this;
        }

        /** TypedQuery re-declaration of setHint */
        @Override
        public TypedQuery<V> setHint(String hintName, Object value) {
            super.setHint(hintName, value);
            return this;
        }

        /** TypedQuery re-declaration of setFlushMode */
        @Override
        public TypedQuery<V> setFlushMode(FlushModeType flushMode) {
            super.setFlushMode(flushMode);
            return this;
        }

        /** TypedQuery re-declaration of setLockMode */
        @Override
        public TypedQuery<V> setLockMode(LockModeType lockMode) {
            super.setLockMode(lockMode);
            return this;
        }

        /** TypedQuery re-declaration of setTimeout */
        @Override
        public TypedQuery<V> setTimeout(Integer timeout) {
            return this;
        }

        @Override
        public TypedQuery<V> setCacheStoreMode(jakarta.persistence.CacheStoreMode m) {
            super.setCacheStoreMode(m);
            return this;
        }

        @Override
        public TypedQuery<V> setCacheRetrieveMode(jakarta.persistence.CacheRetrieveMode m) {
            super.setCacheRetrieveMode(m);
            return this;
        }

        @Override
        public Map<String, Object> getHints() {
            return super.getHints();
        }

        @Override
        public <T> T unwrap(Class<T> cls) {
            return super.unwrap(cls);
        }

        @Override
        public <T> T getParameterValue(Parameter<T> param) {
            return super.getParameterValue(param);
        }

        @Override
        public TypedQuery<V> setParameter(Parameter<Calendar> param, Calendar value, jakarta.persistence.TemporalType temporalType) {
            return setParameter(param, value);
        }

        @Override
        public TypedQuery<V> setParameter(Parameter<java.util.Date> param, java.util.Date value, jakarta.persistence.TemporalType temporalType) {
            return setParameter(param, value);
        }

        @Override
        public TypedQuery<V> setParameter(int position, java.util.Date value, jakarta.persistence.TemporalType temporalType) {
            return setParameter(position, value);
        }

        @Override
        public TypedQuery<V> setParameter(int position, Calendar value, jakarta.persistence.TemporalType temporalType) {
            return setParameter(position, value);
        }

        @Override
        public TypedQuery<V> setParameter(String name, java.util.Date value, jakarta.persistence.TemporalType temporalType) {
            return setParameter(name, value);
        }

        @Override
        public TypedQuery<V> setParameter(String name, Calendar value, jakarta.persistence.TemporalType temporalType) {
            return setParameter(name, value);
        }

        // Deprecated date methods: same erasure with "non-deprecated" variant,
        // so we do NOT implement them here (compiler won't let them override
        // the deposit Motion).  They throw at runtime when invoked,  anyway.
    }
}
