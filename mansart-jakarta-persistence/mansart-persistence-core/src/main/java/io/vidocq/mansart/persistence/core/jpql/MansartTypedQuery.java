/*
 * Copyright (c) 2025 Vidocq contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.vidocq.mansart.persistence.core.jpql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
import jakarta.persistence.*;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.QueryTimeoutException;

import jakarta.persistence.TypedQuery;

import io.vidocq.mansart.data.dialect.Attribute;
import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.data.dialect.SqlFragment;
import io.vidocq.mansart.data.dialect.attribute.ReferenceAttribute;
import io.vidocq.mansart.persistence.core.jpql.JpqlQueryExecutor.BindParameter;
import io.vidocq.mansart.persistence.core.jpql.JpqlQueryExecutor;
import io.vidocq.mansart.persistence.core.runtime.MansartCallback;
import jakarta.persistence.TemporalType;

/**
 * TypedQuery implementation that executes JPQL SELECT statements.
 */
public final class MansartTypedQuery<T> implements TypedQuery<T> {

    private final SqlFragment sqlFragment;
    private final Dialect dialect;
    private final DataSource dataSource;
    private final Class<T> entityClass;
    private final MansartCallback callback;
    private final EntityModel dialectModel;
    private final List<JpqlQueryExecutor.BindParameter> bindParameters;
    private final Map<String, Object> namedParams = new HashMap<>();
    private final List<Object> positionalParams = new ArrayList<>();
    private final boolean aggregate;
    private final List<JpqlQueryExecutor.ProjectionInfo> projections;
    private Integer timeoutSeconds = null;

    public MansartTypedQuery(
        SqlFragment sqlFragment,
        Dialect dialect,
        DataSource dataSource,
        Class<T> entityClass,
        MansartCallback callback,
        EntityModel dialectModel,
        List<BindParameter> bindParameters
    ) {
        this(sqlFragment, dialect, dataSource, entityClass, callback, dialectModel, bindParameters, false, List.of());
    }

    public MansartTypedQuery(
        SqlFragment sqlFragment,
        Dialect dialect,
        DataSource dataSource,
        Class<T> entityClass,
        MansartCallback callback,
        EntityModel dialectModel,
        List<BindParameter> bindParameters,
        boolean aggregate,
        List<JpqlQueryExecutor.ProjectionInfo> projections
    ) {
        this.sqlFragment = Objects.requireNonNull(sqlFragment, "sqlFragment must not be null");
        this.dialect = Objects.requireNonNull(dialect, "dialect must not be null");
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass must not be null");
        this.callback = Objects.requireNonNull(callback, "callback must not be null");
        this.dialectModel = Objects.requireNonNull(dialectModel, "dialectModel must not be null");
        this.bindParameters = Collections.unmodifiableList(
            Objects.requireNonNull(bindParameters, "bindParameters must not be null")
        );
        this.aggregate = aggregate;
        this.projections = projections == null ? List.of() : List.copyOf(projections);
    }

    @Override
    public List<T> getResultList() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlFragment.sql())) {

            bindParameters(ps);

            try (ResultSet rs = ps.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(materializeRow(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to execute query: " + sqlFragment.sql(), e);
        }
    }

    @Override
    public T getSingleResult() {
        List<T> results = getResultList();
        if (results.isEmpty()) {
            throw new jakarta.persistence.NoResultException("No result found for query: " + sqlFragment.sql());
        }
        if (results.size() > 1) {
            throw new jakarta.persistence.NonUniqueResultException("Expected single result but found " + results.size() + " for query: " + sqlFragment.sql());
        }
        return results.getFirst();
    }

    @Override
    public int executeUpdate() {
        throw new UnsupportedOperationException("not implemented: executeUpdate");
    }

    @Override
    public TypedQuery<T> setMaxResults(int maxResult) {
        throw new UnsupportedOperationException("not implemented: setMaxResults");
    }

    @Override
    public int getMaxResults() {
        return Integer.MAX_VALUE;
    }

    @Override
    public TypedQuery<T> setFirstResult(int startPosition) {
        throw new UnsupportedOperationException("not implemented: setFirstResult");
    }

    @Override
    public int getFirstResult() {
        return 0;
    }

    @Override
    public TypedQuery<T> setHint(String hintName, Object value) {
        throw new UnsupportedOperationException("not implemented: setHint");
    }

    @Override
    public Map<String, Object> getHints() {
        return Collections.emptyMap();
    }

    @Override
    public TypedQuery<T> setTimeout(Integer timeout) {
        this.timeoutSeconds = timeout;
        return this;
    }

    @Override
    public <U> TypedQuery<T> setParameter(Parameter<U> param, U value) {
        if (param == null) {
            throw new IllegalArgumentException("Parameter must not be null");
        }
        if (param.getName() != null) {
            return setParameter(param.getName(), value);
        } else if (param.getPosition() != null) {
            return setParameter(param.getPosition(), value);
        } else {
            throw new IllegalArgumentException("Parameter must have a name or position");
        }
    }

    @Override
    public TypedQuery<T> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
        if (param == null) {
            throw new IllegalArgumentException("Parameter must not be null");
        }
        if (param.getName() != null) {
            return setParameter(param.getName(), value, temporalType);
        } else if (param.getPosition() != null) {
            return setParameter(param.getPosition(), value, temporalType);
        } else {
            throw new IllegalArgumentException("Parameter must have a name or position");
        }
    }

    @Override
    public TypedQuery<T> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
        if (param == null) {
            throw new IllegalArgumentException("Parameter must not be null");
        }
        if (param.getName() != null) {
            return setParameter(param.getName(), value, temporalType);
        } else if (param.getPosition() != null) {
            return setParameter(param.getPosition(), value, temporalType);
        } else {
            throw new IllegalArgumentException("Parameter must have a name or position");
        }
    }

    @Override
    public TypedQuery<T> setParameter(String name, Object value) {
        namedParams.put(name, value);
        return this;
    }

    @Override
    public TypedQuery<T> setParameter(String name, Date value, TemporalType temporalType) {
        if (value == null) {
            return setParameter(name, (Object) null);
        }
        Object converted = convertTemporal(value, temporalType);
        return setParameter(name, converted);
    }

    @Override
    public TypedQuery<T> setParameter(String name, Calendar value, TemporalType temporalType) {
        if (value == null) {
            return setParameter(name, (Object) null);
        }
        Object converted = convertTemporal(value, temporalType);
        return setParameter(name, converted);
    }

    @Override
    public TypedQuery<T> setParameter(int position, Object value) {
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
    public TypedQuery<T> setParameter(int position, Date value, TemporalType temporalType) {
        if (value == null) {
            return setParameter(position, (Object) null);
        }
        Object converted = convertTemporal(value, temporalType);
        return setParameter(position, converted);
    }

    @Override
    public TypedQuery<T> setParameter(int position, Calendar value, TemporalType temporalType) {
        if (value == null) {
            return setParameter(position, (Object) null);
        }
        Object converted = convertTemporal(value, temporalType);
        return setParameter(position, converted);
    }

    @Override
    public Set<Parameter<?>> getParameters() {
        return declaredParameters();
    }

    @Override
    public Parameter<?> getParameter(String name) {
        for (Parameter<?> p : declaredParameters()) {
            if (Objects.equals(name, p.getName())) {
                return p;
            }
        }
        throw new IllegalArgumentException("Parameter with name '" + name + "' not found");
    }

    @Override
    public <U> Parameter<U> getParameter(String name, Class<U> type) {
        getParameter(name);
        return new MansartParameter<>(name, null, type);
    }

    @Override
    public Parameter<?> getParameter(int position) {
        for (Parameter<?> p : declaredParameters()) {
            if (Objects.equals(position, p.getPosition())) {
                return p;
            }
        }
        throw new IllegalArgumentException("Parameter at position " + position + " not found");
    }

    @Override
    public <U> Parameter<U> getParameter(int position, Class<U> type) {
        getParameter(position);
        return new MansartParameter<>(null, position, type);
    }

    @Override
    public boolean isBound(Parameter<?> param) {
        if (param == null) {
            throw new IllegalArgumentException("Parameter must not be null");
        }
        if (param.getName() != null) {
            if (!declaredParameters().stream().anyMatch(p -> Objects.equals(p.getName(), param.getName()))) {
                throw new IllegalArgumentException("Parameter '" + param.getName() + "' is not declared in this query");
            }
            return namedParams.containsKey(param.getName());
        } else if (param.getPosition() != null) {
            if (!declaredParameters().stream().anyMatch(p -> Objects.equals(p.getPosition(), param.getPosition()))) {
                throw new IllegalArgumentException("Parameter at position " + param.getPosition() + " is not declared in this query");
            }
            int pos = param.getPosition();
            return pos >= 1 && pos <= positionalParams.size() && positionalParams.get(pos - 1) != null;
        } else {
            throw new IllegalArgumentException("Parameter must have a name or position");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> U getParameterValue(Parameter<U> param) {
        if (param == null) {
            throw new IllegalArgumentException("Parameter must not be null");
        }
        if (param.getName() != null) {
            return (U) getParameterValue(param.getName());
        } else if (param.getPosition() != null) {
            return (U) getParameterValue(param.getPosition());
        } else {
            throw new IllegalArgumentException("Parameter must have a name or position");
        }
    }

    @Override
    public Object getParameterValue(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter name must not be null");
        }
        if (!declaredParameters().stream().anyMatch(p -> Objects.equals(p.getName(), name))) {
            throw new IllegalArgumentException("Parameter '" + name + "' is not declared in this query");
        }
        Object val = namedParams.get(name);
        if (val == null) {
            throw new IllegalStateException("Parameter '" + name + "' is not bound");
        }
        return val;
    }

    @Override
    public Object getParameterValue(int position) {
        if (position < 1) {
            throw new IllegalArgumentException("Position must be >= 1");
        }
        if (!declaredParameters().stream().anyMatch(p -> Objects.equals(p.getPosition(), position))) {
            throw new IllegalArgumentException("Parameter at position " + position + " is not declared in this query");
        }
        Object val = positionalParams.get(position - 1);
        if (val == null) {
            throw new IllegalStateException("Parameter at position " + position + " is not bound");
        }
        return val;
    }

    private Set<Parameter<?>> declaredParameters() {
        Set<Parameter<?>> params = new LinkedHashSet<>();
        for (BindParameter bp : bindParameters) {
            if (bp.isNamed()) {
                params.add(new MansartParameter<>(bp.paramName(), null, Object.class));
            } else if (bp.isPositional()) {
                params.add(new MansartParameter<>(null, bp.paramPosition(), Object.class));
            }
        }
        return Collections.unmodifiableSet(params);
    }

    private Object convertTemporal(Date value, TemporalType temporalType) {
        Objects.requireNonNull(temporalType, "temporalType must not be null");
        switch (temporalType) {
            case DATE:
                return new java.sql.Date(value.getTime());
            case TIME:
                return new java.sql.Time(value.getTime());
            case TIMESTAMP:
                return new java.sql.Timestamp(value.getTime());
            default:
                throw new IllegalArgumentException("Unknown TemporalType: " + temporalType);
        }
    }

    private Object convertTemporal(Calendar value, TemporalType temporalType) {
        Objects.requireNonNull(temporalType, "temporalType must not be null");
        switch (temporalType) {
            case DATE:
                return new java.sql.Date(value.getTimeInMillis());
            case TIME:
                return new java.sql.Time(value.getTimeInMillis());
            case TIMESTAMP:
                return new java.sql.Timestamp(value.getTimeInMillis());
            default:
                throw new IllegalArgumentException("Unknown TemporalType: " + temporalType);
        }
    }

    @Override
    public TypedQuery<T> setFlushMode(FlushModeType flushMode) {
        throw new UnsupportedOperationException("not implemented: setFlushMode");
    }

    @Override
    public FlushModeType getFlushMode() {
        return FlushModeType.AUTO;
    }

    @Override
    public Integer getTimeout() {
        return timeoutSeconds;
    }

    @Override
    public TypedQuery<T> setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        throw new UnsupportedOperationException("not implemented: setCacheStoreMode");
    }

    @Override
    public CacheStoreMode getCacheStoreMode() {
        throw new UnsupportedOperationException("not implemented: getCacheStoreMode");
    }

    @Override
    public TypedQuery<T> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        throw new UnsupportedOperationException("not implemented: setCacheRetrieveMode");
    }

    @Override
    public CacheRetrieveMode getCacheRetrieveMode() {
        throw new UnsupportedOperationException("not implemented: getCacheRetrieveMode");
    }

    @Override
    public TypedQuery<T> setLockMode(LockModeType lockMode) {
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

    @Override
    public T getSingleResultOrNull() {
        List<T> results = getResultList();
        return results.isEmpty() ? null : results.getFirst();
    }

    private void bindParameters(PreparedStatement ps) throws SQLException {
        int idx = 0;
        for (JpqlQueryExecutor.BindParameter bp : bindParameters) {
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

    private T materializeRow(ResultSet rs) throws SQLException {
        if (aggregate) {
            return materializeAggregateRow(rs);
        }
        T entity = callback.instantiate(entityClass);
        List<Attribute<?, ?>> attrs = dialectModel.attributes();
        for (Attribute<?, ?> attr : attrs) {
            if (attr instanceof ReferenceAttribute<?, ?>) {
                continue;
            }
            Object value = dialect.extract(rs, rs.findColumn(attr.columnName()), wrap(attr.javaType()));
            callback.getAccessor(entityClass).set(entity, attr.name(), value);
        }
        return entity;
    }

    @SuppressWarnings("unchecked")
    private T materializeAggregateRow(ResultSet rs) throws SQLException {
        if (projections.isEmpty()) {
            throw new PersistenceException("Aggregate query has no projections");
        }
        if (projections.size() > 1) {
            throw new UnsupportedOperationException("Multiple projections in aggregate queries are not supported");
        }
        
        JpqlQueryExecutor.ProjectionInfo proj = projections.getFirst();
        Object result;
        
        if (proj.function().equalsIgnoreCase("COUNT")) {
            result = rs.getLong(1);
        } else if (proj.function().equalsIgnoreCase("SUM")) {
            Class<?> resultType = proj.resultType();
            if (resultType == Long.class || resultType == long.class) {
                result = rs.getLong(1);
            } else if (resultType == Integer.class || resultType == int.class) {
                result = rs.getInt(1);
            } else if (resultType == Double.class || resultType == double.class) {
                result = rs.getDouble(1);
            } else if (resultType == Float.class || resultType == float.class) {
                result = rs.getFloat(1);
            } else {
                result = rs.getLong(1);
            }
        } else if (proj.function().equalsIgnoreCase("AVG")) {
            result = rs.getDouble(1);
        } else if (proj.function().equalsIgnoreCase("MIN") || proj.function().equalsIgnoreCase("MAX")) {
            Class<?> resultType = proj.resultType();
            if (resultType == Integer.class || resultType == int.class) {
                result = rs.getInt(1);
            } else if (resultType == Long.class || resultType == long.class) {
                result = rs.getLong(1);
            } else if (resultType == Double.class || resultType == double.class) {
                result = rs.getDouble(1);
            } else if (resultType == Float.class || resultType == float.class) {
                result = rs.getFloat(1);
            } else {
                result = rs.getObject(1);
            }
        } else if (proj.function().equalsIgnoreCase("LENGTH") || proj.function().equalsIgnoreCase("LOCATE")) {
            // H2 returns LENGTH/LOCATE as BIGINT/Long, but we declared resultType as Integer
            result = rs.getInt(1);
        } else {
            result = rs.getObject(1);
        }
        
        return (T) result;
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
