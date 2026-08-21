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
 * It is also made available under the European Union Public License v. 1.2,
 * which is available at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.runtime;

import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.persistence.core.jpql.JpqlExecutor;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.Query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
@SuppressWarnings({"unchecked", "rawtypes"})
public class MansartNativeQuery implements Query {

    // EntityManager this query was created by; null for internally-built queries
    private jakarta.persistence.EntityManager boundEntityManager;

    /**
     * Binds this query to its creating EntityManager so every method can enforce
     * the closed-EntityManager rule (Jakarta Persistence 3.2 section 3.1.1).
     */
    public void bindEntityManager(jakarta.persistence.EntityManager entityManager) {
        this.boundEntityManager = entityManager;
    }

    protected final void checkOpen() {
        if (boundEntityManager != null && !boundEntityManager.isOpen()) {
            throw new IllegalStateException("EntityManager is closed");
        }
    }

    private final String sql;
    private final Dialect dialect;
    private final JpqlExecutor.ConnectionProvider connectionProvider;
    private final Map<Class<?>, EntityModel<?>> entityModels;
    private final Map<String, Class<?>> entityClasses;
    private final Map<String, Object> namedParameters = new ConcurrentHashMap<>();
    private final List<Object> positionParameters = new LinkedList<>();
    private int maxResults = Integer.MAX_VALUE;
    private int firstResult = 0;
    private FlushModeType flushMode;
    private LockModeType lockMode;
    private Integer timeout = null;

    MansartNativeQuery(String sql, Dialect dialect, JpqlExecutor.ConnectionProvider connectionProvider) {
        this(sql, dialect, connectionProvider, Map.of(), Map.of());
    }

    MansartNativeQuery(String sql, Dialect dialect, JpqlExecutor.ConnectionProvider connectionProvider,
                       Map<Class<?>, EntityModel<?>> entityModels, Map<String, Class<?>> entityClasses) {
        this.sql = sql;
        this.dialect = dialect;
        this.connectionProvider = connectionProvider;
        this.entityModels = entityModels;
        this.entityClasses = entityClasses;
    }

    @Override
    public int executeUpdate() {
        checkOpen();
        if (connectionProvider == null || dialect == null) {
            throw new jakarta.persistence.PersistenceException(
                    "Native query execution not configured.");
        }
        try (Connection conn = connectionProvider.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                bindParameters(stmt);
                return stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new jakarta.persistence.PersistenceException("Failed to execute native query: " + e.getMessage(), e);
        }
    }

    @Override
    public List getResultList() {
        checkOpen();
        if (connectionProvider == null || dialect == null) {
            throw new jakarta.persistence.PersistenceException(
                    "Native query execution not configured.");
        }
        try (Connection conn = connectionProvider.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                bindParameters(stmt);
                return executeSelect(stmt);
            }
        } catch (SQLException e) {
            throw new jakarta.persistence.PersistenceException("Failed to execute native query: " + e.getMessage(), e);
        }
    }

    @Override
    public Object getSingleResult() {
        checkOpen();
        List results = getResultList();
        if (results.isEmpty()) {
            throw new jakarta.persistence.NoResultException("No result found for native query");
        }
        if (results.size() > 1) {
            throw new jakarta.persistence.NonUniqueResultException("Multiple results found for native query");
        }
        return results.get(0);
    }

    @Override
    public Object getSingleResultOrNull() {
        checkOpen();
        List results = getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    private void bindParameters(PreparedStatement stmt) throws SQLException {
        int paramIndex = 1;
        // For M8-16: Only positional parameters are supported for native queries
        // Named parameters in the SQL will cause syntax errors with most databases
        for (Object value : positionParameters) {
            stmt.setObject(paramIndex, value);
            paramIndex++;
        }
    }

    private List<Object[]> executeSelect(PreparedStatement stmt) throws SQLException {
        List<Object[]> results = new ArrayList<>();
        int actualMaxResults = maxResults == Integer.MAX_VALUE ? 0 : maxResults;
        
        if (actualMaxResults > 0) {
            stmt.setMaxRows(actualMaxResults);
        }
        
        if (firstResult > 0) {
            stmt.setFetchSize(firstResult);
            // For dialects that support it, we would use LIMIT/OFFSET here
            // For now, we use a simple approach with setFetchSize
        }
        
        try (ResultSet rs = stmt.executeQuery()) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    row[i] = rs.getObject(i + 1);
                }
                results.add(row);
            }
        }
        
        return results;
    }

    @Override
    public Query setMaxResults(int maxResult) {
        checkOpen();
        this.maxResults = maxResult;
        return this;
    }

    @Override
    public int getMaxResults() {
        checkOpen();
        return maxResults == Integer.MAX_VALUE ? 0 : maxResults;
    }

    @Override
    public Query setFirstResult(int startPosition) {
        checkOpen();
        this.firstResult = startPosition;
        return this;
    }

    @Override
    public int getFirstResult() {
        checkOpen();
        return firstResult;
    }

    @Override
    public Query setFlushMode(FlushModeType flushMode) {
        checkOpen();
        this.flushMode = flushMode;
        return this;
    }

    @Override
    public FlushModeType getFlushMode() {
        checkOpen();
        return flushMode;
    }

    @Override
    public Query setLockMode(LockModeType lockMode) {
        checkOpen();
        this.lockMode = lockMode;
        return this;
    }

    @Override
    public LockModeType getLockMode() {
        checkOpen();
        return lockMode;
    }

    @Override
    public Query setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        checkOpen();
        return this;
    }

    @Override
    public CacheRetrieveMode getCacheRetrieveMode() {
        checkOpen();
        return CacheRetrieveMode.BYPASS;
    }

    @Override
    public Query setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        checkOpen();
        return this;
    }

    @Override
    public CacheStoreMode getCacheStoreMode() {
        checkOpen();
        return CacheStoreMode.BYPASS;
    }

    @Override
    public Query setParameter(int position, Object value) {
        checkOpen();
        while (positionParameters.size() <= position - 1) {
            positionParameters.add(null);
        }
        positionParameters.set(position - 1, value);
        return this;
    }

    @Override
    public Query setParameter(int position, Calendar value, jakarta.persistence.TemporalType temporalType) {
        checkOpen();
        return setParameter(position, value);
    }

    @Override
    public Query setParameter(int position, java.util.Date value, jakarta.persistence.TemporalType temporalType) {
        checkOpen();
        return setParameter(position, value);
    }

    @Override
    public Query setParameter(String name, Object value) {
        checkOpen();
        namedParameters.put(name, value);
        return this;
    }

    @Override
    public Query setParameter(String name, Calendar value, jakarta.persistence.TemporalType temporalType) {
        checkOpen();
        return setParameter(name, value);
    }

    @Override
    public Query setParameter(String name, java.util.Date value, jakarta.persistence.TemporalType temporalType) {
        checkOpen();
        return setParameter(name, value);
    }

    @Override
    public Query setParameter(Parameter<Calendar> param, Calendar value, jakarta.persistence.TemporalType temporalType) {
        checkOpen();
        return setParameter(param.getName(), value);
    }

    @Override
    public Query setParameter(Parameter<java.util.Date> param, java.util.Date value, jakarta.persistence.TemporalType temporalType) {
        checkOpen();
        return setParameter(param.getName(), value);
    }

    @Override
    public <T> Query setParameter(Parameter<T> param, T value) {
        checkOpen();
        if (param != null) {
            namedParameters.put(param.getName(), value);
        }
        return this;
    }

    @Override
    public Parameter<?> getParameter(String name) {
        checkOpen();
        return null;
    }

    @Override
    public <T> Parameter<T> getParameter(String name, Class<T> resultType) {
        checkOpen();
        return null;
    }

    @Override
    public Parameter<?> getParameter(int position) {
        checkOpen();
        return null;
    }

    @Override
    public <T> Parameter<T> getParameter(int position, Class<T> resultType) {
        checkOpen();
        return null;
    }

    @Override
    public Set<Parameter<?>> getParameters() {
        checkOpen();
        return new HashSet<>();
    }

    @Override
    public boolean isBound(Parameter<?> param) {
        checkOpen();
        return param != null && namedParameters.containsKey(param.getName());
    }

    @Override
    public <T> T getParameterValue(Parameter<T> param) {
        checkOpen();
        return param == null ? null : (T) namedParameters.get(param.getName());
    }

    @Override
    public Object getParameterValue(String name) {
        checkOpen();
        return namedParameters.get(name);
    }

    @Override
    public Object getParameterValue(int position) {
        checkOpen();
        return position > 0 && position <= positionParameters.size() ? positionParameters.get(position - 1) : null;
    }

    @Override
    public Query setHint(String hintName, Object value) {
        checkOpen();
        return this;
    }

    @Override
    public Map<String, Object> getHints() {
        checkOpen();
        return Map.of();
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        checkOpen();
        return type.cast(this);
    }

    @Override
    public Query setTimeout(Integer timeout) {
        checkOpen();
        this.timeout = timeout;
        return this;
    }

    @Override
    public Integer getTimeout() {
        checkOpen();
        return timeout;
    }
}
