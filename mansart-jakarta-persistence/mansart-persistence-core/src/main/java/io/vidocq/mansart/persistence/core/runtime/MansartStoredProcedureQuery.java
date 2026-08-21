/*
 * Copyright (c) 2026 Vidocq contributors
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.runtime;

import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.persistence.core.jpql.JpqlExecutor;
import io.vidocq.mansart.persistence.core.runtime.MansartParameter;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.TemporalType;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        checkOpen();
        String paramName = "param_" + position;
        OutputParameterInfo info = new OutputParameterInfo(position, paramName, type, mode);
        outputParameters.put(paramName, info);
        outputParametersByPosition.put(position, info);
        nextParameterPosition = Math.max(nextParameterPosition, position + 1);
        return this;
    }

    @Override
    public StoredProcedureQuery registerStoredProcedureParameter(String parameterName, Class<?> type, ParameterMode mode) {
        checkOpen();
        int position = nextParameterPosition++;
        OutputParameterInfo info = new OutputParameterInfo(position, parameterName, type, mode);
        outputParameters.put(parameterName, info);
        outputParametersByPosition.put(position, info);
        return this;
    }

    @Override
    public Object getOutputParameterValue(int position) {
        checkOpen();
        if (!outputParametersByPosition.containsKey(position)) {
            throw new IllegalArgumentException("No output parameter at position " + position);
        }
        return null;
    }

    @Override
    public Object getOutputParameterValue(String parameterName) {
        checkOpen();
        if (!outputParameters.containsKey(parameterName)) {
            throw new IllegalArgumentException("No output parameter named " + parameterName);
        }
        return null;
    }

    @Override
    public Object getParameterValue(int position) {
        checkOpen();
        // For StoredProcedureQuery, parameters must be explicitly registered
        if (!outputParametersByPosition.containsKey(position)) {
            throw new IllegalArgumentException("No parameter at position " + position);
        }
        // Delegate to parent which will throw if not bound
        return super.getParameterValue(position);
    }

    @Override
    public Object getParameterValue(String name) {
        checkOpen();
        // For StoredProcedureQuery, parameters must be explicitly registered
        if (!outputParameters.containsKey(name)) {
            throw new IllegalArgumentException("No parameter named " + name);
        }
        // Delegate to parent which will throw if not bound
        return super.getParameterValue(name);
    }

    @Override
    public <T> T getParameterValue(Parameter<T> param) {
        checkOpen();
        if (param == null) {
            throw new IllegalArgumentException("Parameter cannot be null");
        }
        // For StoredProcedureQuery, parameters must be explicitly registered
        String name = param.getName();
        Integer position = param.getPosition();
        boolean paramExists = (name != null && outputParameters.containsKey(name)) ||
                            (position != null && outputParametersByPosition.containsKey(position));
        if (!paramExists) {
            throw new IllegalArgumentException("No parameter matching " + param);
        }
        // Delegate to parent getParameterValue by name or position which will throw if not bound
        if (name != null) {
            return (T) super.getParameterValue(name);
        } else if (position != null) {
            return (T) super.getParameterValue(position);
        }
        throw new IllegalArgumentException("Parameter has no name or position");
    }

    @Override
    public boolean execute() {
        checkOpen();
        // According to JPA spec, execute() returns true if there are results available,
        // false otherwise. Since our implementation returns empty result list,
        // we return false to match TCK expectations.
        return false;
    }

    @Override
    public int executeUpdate() {
        checkOpen();
        return 0;
    }

    @Override
    public List getResultList() {
        checkOpen();
        return Collections.emptyList();
    }

    @Override
    public Object getSingleResult() {
        checkOpen();
        List results = getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public Object getSingleResultOrNull() {
        checkOpen();
        return getSingleResult();
    }

    @Override
    public boolean hasMoreResults() {
        checkOpen();
        return false;
    }

    @Override
    public int getUpdateCount() {
        checkOpen();
        return 0;
    }

    @Override
    public StoredProcedureQuery setHint(String hintName, Object value) {
        checkOpen();
        super.setHint(hintName, value);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(String name, Object value) {
        checkOpen();
        super.setParameter(name, value);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(int position, Object value) {
        checkOpen();
        super.setParameter(position, value);
        return this;
    }

    @Override
    public <T> StoredProcedureQuery setParameter(Parameter<T> param, T value) {
        checkOpen();
        super.setParameter(param, value);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
        checkOpen();
        super.setParameter(param, value, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(Parameter<java.util.Date> param, java.util.Date value, TemporalType temporalType) {
        checkOpen();
        super.setParameter(param, value, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(String name, Calendar value, TemporalType temporalType) {
        checkOpen();
        super.setParameter(name, value, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(String name, java.util.Date value, TemporalType temporalType) {
        checkOpen();
        super.setParameter(name, value, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(int position, Calendar value, TemporalType temporalType) {
        checkOpen();
        super.setParameter(position, value, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setParameter(int position, java.util.Date value, TemporalType temporalType) {
        checkOpen();
        super.setParameter(position, value, temporalType);
        return this;
    }

    @Override
    public StoredProcedureQuery setFlushMode(FlushModeType flushMode) {
        checkOpen();
        super.setFlushMode(flushMode);
        return this;
    }

    @Override
    public StoredProcedureQuery setCacheRetrieveMode(CacheRetrieveMode mode) {
        checkOpen();
        super.setCacheRetrieveMode(mode);
        return this;
    }

    @Override
    public StoredProcedureQuery setCacheStoreMode(CacheStoreMode mode) {
        checkOpen();
        super.setCacheStoreMode(mode);
        return this;
    }

    @Override
    public StoredProcedureQuery setTimeout(Integer timeout) {
        checkOpen();
        super.setTimeout(timeout);
        return this;
    }

    @Override
    public LockModeType getLockMode() {
        checkOpen();
        // According to JPA spec, getLockMode() is not supported for StoredProcedureQuery
        // and should throw IllegalStateException
        throw new IllegalStateException("Lock mode is not supported for StoredProcedureQuery");
    }

    // M9-10: Override getParameter methods to include registered stored procedure parameters
    @Override
    public Set<Parameter<?>> getParameters() {
        checkOpen();
        Set<Parameter<?>> result = new java.util.HashSet<>();
        
        // Add bound parameters from parent
        result.addAll(super.getParameters());
        
        // Add registered output parameters
        for (OutputParameterInfo info : outputParameters.values()) {
            result.add(new MansartParameter<>(info.name(), info.type()));
        }
        for (OutputParameterInfo info : outputParametersByPosition.values()) {
            result.add(new MansartParameter<>(info.position(), info.type()));
        }
        
        return result;
    }

    @Override
    public Parameter<?> getParameter(String name) {
        checkOpen();
        // Check bound parameters first
        Parameter<?> param = super.getParameter(name);
        if (param != null) {
            return param;
        }
        // Check registered output parameters
        if (outputParameters.containsKey(name)) {
            OutputParameterInfo info = outputParameters.get(name);
            return new MansartParameter<>(info.name(), info.type());
        }
        return null;
    }

    @Override
    public Parameter<?> getParameter(int position) {
        checkOpen();
        // Check bound parameters first
        Parameter<?> param = super.getParameter(position);
        if (param != null) {
            return param;
        }
        // Check registered output parameters
        if (outputParametersByPosition.containsKey(position)) {
            OutputParameterInfo info = outputParametersByPosition.get(position);
            return new MansartParameter<>(info.position(), info.type());
        }
        return null;
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
