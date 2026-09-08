/*
 * Copyright (c) 2025 Vidocq contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.vidocq.mansart.persistence.core.jpql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Calendar;
import java.util.Date;

import javax.sql.DataSource;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import jakarta.persistence.TemporalType;

import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.persistence.core.jpql.JpqlQueryExecutor.BindParameter;
import io.vidocq.mansart.persistence.core.jpql.JpqlQueryExecutor.UpdatePlan;
import io.vidocq.mansart.persistence.core.runtime.MansartCallback;

/**
 * Query implementation that executes JPQL bulk UPDATE statements.
 */
public final class MansartBulkQuery implements Query {

    private final String sql;
    private final Dialect dialect;
    private final DataSource dataSource;
    private final Class<?> entityClass;
    private final MansartCallback callback;
    private final EntityModel dialectModel;
    private final List<BindParameter> bindParameters;
    private final Map<String, Object> namedParams = new HashMap<>();
    private final List<Object> positionalParams = new ArrayList<>();
    private Integer timeoutSeconds = null;
    private Integer maxResults = null;
    private Integer firstResult = null;

    public MansartBulkQuery(
        UpdatePlan plan,
        Dialect dialect,
        DataSource dataSource,
        MansartCallback callback
    ) {
        this.sql = Objects.requireNonNull(plan.sql(), "sql must not be null");
        this.dialect = Objects.requireNonNull(dialect, "dialect must not be null");
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        this.entityClass = Objects.requireNonNull(plan.entityClass(), "entityClass must not be null");
        this.callback = Objects.requireNonNull(callback, "callback must not be null");
        this.dialectModel = Objects.requireNonNull(plan.dialectModel(), "dialectModel must not be null");
        this.bindParameters = Collections.unmodifiableList(
            Objects.requireNonNull(plan.bindParameters(), "bindParameters must not be null")
        );
    }

    @Override
    public int executeUpdate() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParameters(ps);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to execute update: " + sql, e);
        }
    }

    @Override
    public List<Object> getResultList() {
        throw new IllegalStateException("UPDATE/DELETE queries cannot return a result list");
    }

    @Override
    public Object getSingleResult() {
        throw new IllegalStateException("UPDATE/DELETE queries cannot return a single result");
    }

    @Override
    public Object getSingleResultOrNull() {
        throw new IllegalStateException("UPDATE/DELETE queries cannot return a single result");
    }

    @Override
    public Query setMaxResults(int maxResult) {
        this.maxResults = maxResult;
        return this;
    }

    @Override
    public int getMaxResults() {
        return maxResults == null ? Integer.MAX_VALUE : maxResults;
    }

    @Override
    public Query setFirstResult(int startPosition) {
        this.firstResult = startPosition;
        return this;
    }

    @Override
    public int getFirstResult() {
        return firstResult == null ? 0 : firstResult;
    }

    @Override
    public Query setHint(String hintName, Object value) {
        throw new UnsupportedOperationException("not implemented: setHint");
    }

    @Override
    public Map<String, Object> getHints() {
        return Collections.emptyMap();
    }

    @Override
    public Query setTimeout(Integer timeout) {
        this.timeoutSeconds = timeout;
        return this;
    }

    @Override
    public Integer getTimeout() {
        return timeoutSeconds;
    }

    @Override
    public <U> Query setParameter(Parameter<U> param, U value) {
        throw new UnsupportedOperationException("not implemented: setParameter(Parameter)");
    }

    @Override
    public Query setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
        throw new UnsupportedOperationException("not implemented: setParameter(Parameter, Date, TemporalType)");
    }

    @Override
    public Query setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
        throw new UnsupportedOperationException("not implemented: setParameter(Parameter, Calendar, TemporalType)");
    }

    @Override
    public Query setParameter(String name, Object value) {
        namedParams.put(name, value);
        return this;
    }

    @Override
    public Query setParameter(String name, Date value, TemporalType temporalType) {
        throw new UnsupportedOperationException("not implemented: setParameter(String, Date, TemporalType)");
    }

    @Override
    public Query setParameter(String name, Calendar value, TemporalType temporalType) {
        throw new UnsupportedOperationException("not implemented: setParameter(String, Calendar, TemporalType)");
    }

    @Override
    public Query setParameter(int position, Object value) {
        while (positionalParams.size() < position - 1) {
            positionalParams.add(null);
        }
        if (positionalParams.size() < position) {
            positionalParams.add(value);
        } else {
            positionalParams.set(position - 1, value);
        }
        return this;
    }

    @Override
    public Query setParameter(int position, Date value, TemporalType temporalType) {
        throw new UnsupportedOperationException("not implemented: setParameter(int, Date, TemporalType)");
    }

    @Override
    public Query setParameter(int position, Calendar value, TemporalType temporalType) {
        throw new UnsupportedOperationException("not implemented: setParameter(int, Calendar, TemporalType)");
    }

    @Override
    public Set<Parameter<?>> getParameters() {
        throw new UnsupportedOperationException("not implemented: getParameters");
    }

    @Override
    public Parameter<?> getParameter(String name) {
        throw new UnsupportedOperationException("not implemented: getParameter(String)");
    }

    @Override
    public <U> Parameter<U> getParameter(String name, Class<U> type) {
        throw new UnsupportedOperationException("not implemented: getParameter(String, Class)");
    }

    @Override
    public Parameter<?> getParameter(int position) {
        throw new UnsupportedOperationException("not implemented: getParameter(int)");
    }

    @Override
    public <U> Parameter<U> getParameter(int position, Class<U> type) {
        throw new UnsupportedOperationException("not implemented: getParameter(int, Class)");
    }

    @Override
    public boolean isBound(Parameter<?> param) {
        throw new UnsupportedOperationException("not implemented: isBound");
    }

    @Override
    public <U> U getParameterValue(Parameter<U> param) {
        throw new UnsupportedOperationException("not implemented: getParameterValue(Parameter)");
    }

    @Override
    public Object getParameterValue(String name) {
        throw new UnsupportedOperationException("not implemented: getParameterValue(String)");
    }

    @Override
    public Object getParameterValue(int position) {
        throw new UnsupportedOperationException("not implemented: getParameterValue(int)");
    }

    @Override
    public Query setFlushMode(FlushModeType flushMode) {
        throw new UnsupportedOperationException("not implemented: setFlushMode");
    }

    @Override
    public FlushModeType getFlushMode() {
        return FlushModeType.AUTO;
    }

    @Override
    public Query setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        throw new UnsupportedOperationException("not implemented: setCacheStoreMode");
    }

    @Override
    public CacheStoreMode getCacheStoreMode() {
        throw new UnsupportedOperationException("not implemented: getCacheStoreMode");
    }

    @Override
    public Query setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        throw new UnsupportedOperationException("not implemented: setCacheRetrieveMode");
    }

    @Override
    public CacheRetrieveMode getCacheRetrieveMode() {
        throw new UnsupportedOperationException("not implemented: getCacheRetrieveMode");
    }

    @Override
    public Query setLockMode(LockModeType lockMode) {
        throw new UnsupportedOperationException("not implemented: setLockMode");
    }

    @Override
    public LockModeType getLockMode() {
        return null;
    }

    @Override
    public <U> U unwrap(Class<U> cls) {
        throw new UnsupportedOperationException("not implemented: unwrap");
    }

    private void bindParameters(PreparedStatement ps) throws SQLException {
        int idx = 0;
        for (BindParameter bp : bindParameters) {
            Object value;
            if (bp.isNamed()) {
                value = namedParams.get(bp.paramName());
                if (value == null) {
                    throw new IllegalStateException("Named parameter '" + bp.paramName() + "' not set");
                }
            } else if (bp.isPositional()) {
                int pos = bp.paramPosition();
                if (pos < 1 || pos > positionalParams.size()) {
                    throw new IllegalStateException("Positional parameter at position " + pos + " not set");
                }
                value = positionalParams.get(pos - 1);
            } else if (bp.isLiteral()) {
                value = bp.value();
            } else {
                throw new IllegalStateException("BindParameter has no value source");
            }

            Class<?> javaType = wrap(bp.target().javaType());
            dialect.bind(ps, idx + 1, value, javaType);
            idx++;
        }
    }

    private Class<?> wrap(Class<?> type) {
        if (type == null) return null;
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        if (type == short.class) return Short.class;
        if (type == void.class) return Void.class;
        return type;
    }
}
