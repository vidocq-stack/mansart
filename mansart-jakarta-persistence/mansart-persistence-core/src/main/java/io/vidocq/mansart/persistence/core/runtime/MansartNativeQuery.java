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
import io.vidocq.mansart.persistence.core.jpql.JpqlExecutor;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.Query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic native SQL query implementation for M7-24.
 * Supports executeUpdate() for DELETE/UPDATE/INSERT statements.
 * getResultList() for SELECT statements is not yet implemented.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class MansartNativeQuery implements Query {

    private final String sql;
    private final Dialect dialect;
    private final JpqlExecutor.ConnectionProvider connectionProvider;
    private final Map<String, Object> namedParameters = new ConcurrentHashMap<>();
    private final List<Object> positionParameters = new LinkedList<>();
    private int maxResults = Integer.MAX_VALUE;
    private int firstResult = 0;
    private FlushModeType flushMode;
    private LockModeType lockMode;
    private Integer timeout = null;

    MansartNativeQuery(String sql, Dialect dialect, JpqlExecutor.ConnectionProvider connectionProvider) {
        this.sql = sql;
        this.dialect = dialect;
        this.connectionProvider = connectionProvider;
    }

    @Override
    public int executeUpdate() {
        if (connectionProvider == null || dialect == null) {
            throw new jakarta.persistence.PersistenceException(
                    "Native query execution not configured.");
        }
        String processedSql = applyParameters(sql);
        try (Connection conn = connectionProvider.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(processedSql)) {
                bindPositionParameters(stmt);
                return stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new jakarta.persistence.PersistenceException("Failed to execute native query: " + e.getMessage(), e);
        }
    }

    @Override
    public List getResultList() {
        throw new UnsupportedOperationException("Native SQL SELECT queries not yet supported at M7-24");
    }

    @Override
    public Object getSingleResult() {
        throw new UnsupportedOperationException("Native SQL SELECT queries not yet supported at M7-24");
    }

    @Override
    public Object getSingleResultOrNull() {
        throw new UnsupportedOperationException("Native SQL SELECT queries not yet supported at M7-24");
    }

    private String applyParameters(String sql) {
        if (namedParameters.isEmpty()) {
            return sql;
        }
        String result = sql;
        for (Map.Entry<String, Object> entry : namedParameters.entrySet()) {
            String placeholder = ":" + entry.getKey();
            String value = String.valueOf(entry.getValue());
            result = result.replace(placeholder, value);
        }
        return result;
    }

    private void bindPositionParameters(PreparedStatement stmt) throws SQLException {
        for (int i = 0; i < positionParameters.size(); i++) {
            stmt.setObject(i + 1, positionParameters.get(i));
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
        this.lockMode = lockMode;
        return this;
    }

    @Override
    public LockModeType getLockMode() {
        return lockMode;
    }

    @Override
    public Query setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        return this;
    }

    @Override
    public CacheRetrieveMode getCacheRetrieveMode() {
        return CacheRetrieveMode.BYPASS;
    }

    @Override
    public Query setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        return this;
    }

    @Override
    public CacheStoreMode getCacheStoreMode() {
        return CacheStoreMode.BYPASS;
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
    public Query setParameter(int position, Calendar value, jakarta.persistence.TemporalType temporalType) {
        return setParameter(position, value);
    }

    @Override
    public Query setParameter(int position, java.util.Date value, jakarta.persistence.TemporalType temporalType) {
        return setParameter(position, value);
    }

    @Override
    public Query setParameter(String name, Object value) {
        namedParameters.put(name, value);
        return this;
    }

    @Override
    public Query setParameter(String name, Calendar value, jakarta.persistence.TemporalType temporalType) {
        return setParameter(name, value);
    }

    @Override
    public Query setParameter(String name, java.util.Date value, jakarta.persistence.TemporalType temporalType) {
        return setParameter(name, value);
    }

    @Override
    public Query setParameter(Parameter<Calendar> param, Calendar value, jakarta.persistence.TemporalType temporalType) {
        return setParameter(param.getName(), value);
    }

    @Override
    public Query setParameter(Parameter<java.util.Date> param, java.util.Date value, jakarta.persistence.TemporalType temporalType) {
        return setParameter(param.getName(), value);
    }

    @Override
    public <T> Query setParameter(Parameter<T> param, T value) {
        if (param != null) {
            namedParameters.put(param.getName(), value);
        }
        return this;
    }

    @Override
    public Parameter<?> getParameter(String name) {
        return null;
    }

    @Override
    public <T> Parameter<T> getParameter(String name, Class<T> resultType) {
        return null;
    }

    @Override
    public Parameter<?> getParameter(int position) {
        return null;
    }

    @Override
    public <T> Parameter<T> getParameter(int position, Class<T> resultType) {
        return null;
    }

    @Override
    public Set<Parameter<?>> getParameters() {
        return new HashSet<>();
    }

    @Override
    public boolean isBound(Parameter<?> param) {
        return param != null && namedParameters.containsKey(param.getName());
    }

    @Override
    public <T> T getParameterValue(Parameter<T> param) {
        return param == null ? null : (T) namedParameters.get(param.getName());
    }

    @Override
    public Object getParameterValue(String name) {
        return namedParameters.get(name);
    }

    @Override
    public Object getParameterValue(int position) {
        return position >= 0 && position < positionParameters.size() ? positionParameters.get(position) : null;
    }

    @Override
    public Query setHint(String hintName, Object value) {
        return this;
    }

    @Override
    public Map<String, Object> getHints() {
        return Map.of();
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return type.cast(this);
    }

    @Override
    public Query setTimeout(Integer timeout) {
        this.timeout = timeout;
        return this;
    }

    @Override
    public Integer getTimeout() {
        return timeout;
    }
}
