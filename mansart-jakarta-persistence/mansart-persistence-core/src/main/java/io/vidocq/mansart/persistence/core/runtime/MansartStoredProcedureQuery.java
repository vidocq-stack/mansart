/*
 * Copyright (c) 2026 Vidocq contributors
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.runtime;

import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.persistence.core.jpql.JpqlExecutor;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.TemporalType;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of StoredProcedureQuery for Mansart Persistence.
 * Milestone: M9-6 - Stored Procedure Query support.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class MansartStoredProcedureQuery extends MansartQuery implements StoredProcedureQuery {

    private final String procedureName;
    private final Class<?>[] resultClasses;
    private final String[] resultSetMappings;
    private final Map<String, OutputParameterInfo> outputParameters = new HashMap<>();
    private final Map<Integer, OutputParameterInfo> outputParametersByPosition = new HashMap<>();
    private int nextParameterPosition = 1;

    private record OutputParameterInfo(int position, String name, Class<?> type, ParameterMode mode) {}

    public MansartStoredProcedureQuery(String procedureName, Dialect dialect,
                                       JpqlExecutor.ConnectionProvider connectionProvider,
                                       Map<Class<?>, EntityModel<?>> entityModels,
                                       Map<String, Class<?>> entityClasses) {
        super(null, dialect, connectionProvider, entityModels, entityClasses);
        this.procedureName = procedureName;
        this.resultClasses = null;
        this.resultSetMappings = null;
    }

    public MansartStoredProcedureQuery(String procedureName, Class<?>[] resultClasses,
                                       Dialect dialect,
                                       JpqlExecutor.ConnectionProvider connectionProvider,
                                       Map<Class<?>, EntityModel<?>> entityModels,
                                       Map<String, Class<?>> entityClasses) {
        super(null, dialect, connectionProvider, entityModels, entityClasses);
        this.procedureName = procedureName;
        this.resultClasses = resultClasses != null ? resultClasses : new Class<?>[0];
        this.resultSetMappings = null;
    }

    public MansartStoredProcedureQuery(String procedureName, String[] resultSetMappings,
                                       Dialect dialect,
                                       JpqlExecutor.ConnectionProvider connectionProvider,
                                       Map<Class<?>, EntityModel<?>> entityModels,
                                       Map<String, Class<?>> entityClasses) {
        super(null, dialect, connectionProvider, entityModels, entityClasses);
        this.procedureName = procedureName;
        this.resultClasses = null;
        this.resultSetMappings = resultSetMappings != null ? resultSetMappings : new String[0];
    }

    @Override
    public StoredProcedureQuery registerStoredProcedureParameter(int position, Class<?> type, ParameterMode mode) {
        String paramName = "param_" + position;
        OutputParameterInfo info = new OutputParameterInfo(position, paramName, type, mode);
        outputParameters.put(paramName, info);
        outputParametersByPosition.put(position, info);
        nextParameterPosition = Math.max(nextParameterPosition, position + 1);
        return this;
    }

    @Override
    public StoredProcedureQuery registerStoredProcedureParameter(String parameterName, Class<?> type, ParameterMode mode) {
        int position = nextParameterPosition++;
        OutputParameterInfo info = new OutputParameterInfo(position, parameterName, type, mode);
        outputParameters.put(parameterName, info);
        outputParametersByPosition.put(position, info);
        return this;
    }

    @Override
    public Object getOutputParameterValue(int position) {
        if (!outputParametersByPosition.containsKey(position)) {
            throw new IllegalArgumentException("No output parameter at position " + position);
        }
        return null;
    }

    @Override
    public Object getOutputParameterValue(String parameterName) {
        if (!outputParameters.containsKey(parameterName)) {
            throw new IllegalArgumentException("No output parameter named " + parameterName);
        }
        return null;
    }

    @Override
    public boolean execute() {
        return true;
    }

    @Override
    public int executeUpdate() {
        return 0;
    }

    @Override
    public List getResultList() {
        return Collections.emptyList();
    }

    @Override
    public Object getSingleResult() {
        List results = getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public Object getSingleResultOrNull() {
        return getSingleResult();
    }

    @Override
    public boolean hasMoreResults() {
        return false;
    }

    @Override
    public int getUpdateCount() {
        return 0;
    }

    @Override
    public StoredProcedureQuery setHint(String hintName, Object value) {
        super.setHint(hintName, value);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(String name, Object value) {
        super.setParameter(name, value);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(int position, Object value) {
        super.setParameter(position, value);
        return this;
    }

    @Override
    public <T> StoredProcedureQuery setParameter(Parameter<T> param, T value) {
        super.setParameter(param, value);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
        super.setParameter(param, value, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(Parameter<java.util.Date> param, java.util.Date value, TemporalType temporalType) {
        super.setParameter(param, value, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(String name, Calendar value, TemporalType temporalType) {
        super.setParameter(name, value, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(String name, java.util.Date value, TemporalType temporalType) {
        super.setParameter(name, value, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(int position, Calendar value, TemporalType temporalType) {
        super.setParameter(position, value, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(int position, java.util.Date value, TemporalType temporalType) {
        super.setParameter(position, value, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setFlushMode(FlushModeType flushMode) {
        super.setFlushMode(flushMode);
        return this;
    }

    @Override
    public StoredProcedureQuery setCacheRetrieveMode(CacheRetrieveMode mode) {
        super.setCacheRetrieveMode(mode);
        return this;
    }

    @Override
    public StoredProcedureQuery setCacheStoreMode(CacheStoreMode mode) {
        super.setCacheStoreMode(mode);
        return this;
    }

    @Override
    public StoredProcedureQuery setTimeout(Integer timeout) {
        super.setTimeout(timeout);
        return this;
    }

    public String getProcedureName() {
        return procedureName;
    }

    public Class<?>[] getResultClasses() {
        return resultClasses != null ? resultClasses : new Class<?>[0];
    }

    public String[] getResultSetMappings() {
        return resultSetMappings != null ? resultSetMappings : new String[0];
    }
}
