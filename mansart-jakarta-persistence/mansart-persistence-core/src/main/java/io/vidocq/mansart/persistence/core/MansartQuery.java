/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under
 * the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;

import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.data.dialect.Pagination;
import io.vidocq.mansart.data.dialect.SqlFragment;

/**
 * A minimal {@link jakarta.persistence.Query} backed by a parsed JPQL SELECT.
 *
 * <p>Executes the translated SQL, maps result rows to entity instances
 * using the entity model's constructor and attribute setters, and
 * binds extracted literal values and user-provided parameter values
 * to {@code ?} placeholders.</p>
 */
final class MansartQuery implements jakarta.persistence.Query,
        jakarta.persistence.TypedQuery<Object> {

    private final MansartEntityManager entityManager;
    private final EntityModel<?> entityModel;
    private final Dialect dialect;
    private final JpqlQuery jpqlQuery;
    private final List<String> literalValues;
    private final java.util.Map<Integer, Object> parameterValues;
    private int firstResult;
    private int maxResults = Integer.MAX_VALUE;

    /**
     * Creates a query from a parsed JPQL AST.
     *
     * @param entityManager the owning entity manager
     * @param query the parsed JPQL query
     * @param entityModel the entity model for the queried entity
     * @param dialect the SQL dialect
     */
    MansartQuery(MansartEntityManager entityManager,
                 JpqlQuery query,
                 EntityModel<?> entityModel,
                 Dialect dialect) {
        this.entityManager = entityManager;
        this.entityModel = entityModel;
        this.dialect = dialect;
        this.jpqlQuery = query;
        this.literalValues = new JpqlToSqlTranslator(query, entityModel, dialect)
                .getLiteralValues();
        this.parameterValues = new java.util.HashMap<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Object> getResultList() {
        entityManager.checkClosed();

        // Build the SQL with pagination.
        Pagination pagination;
        if (firstResult == 0 && maxResults == Integer.MAX_VALUE) {
            pagination = Pagination.NONE;
        } else {
            pagination = new Pagination.Offset(firstResult, maxResults);
        }
        SqlFragment fragment = new JpqlToSqlTranslator(
                jpqlQuery, entityModel, dialect).translate(pagination);
        String sql = fragment.sql();
        System.out.println("DEBUG SQL: " + sql);

        Connection conn = entityManager.getConnectionForSql();
        try {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                // Bind values to ? placeholders.
                int bindIndex = 0;
                for (JpqlPredicate p : jpqlQuery.predicates()) {
                    String v = p.value();
                    Object value;
                    if (v.startsWith("?")) {
                        // Positional parameter — look up from user-provided values
                        try {
                            int pos = Integer.parseInt(v.substring(1));
                            value = parameterValues.get(pos);
                        } catch (NumberFormatException e) {
                            value = v; // fallback: treat as literal
                        }
                    } else {
                        // Literal value
                        value = v;
                    }
                    if (value != null) {
                        stmt.setString(++bindIndex, value.toString());
                    }
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    List<Object> results = new ArrayList<>();
                    while (rs.next()) {
                        Object entity = instantiateEntity(rs);
                        results.add(entity);
                        // Register in persistence context.
                        Object id;
                        try {
                            id = entityModel.id().getter().invoke(entity);
                        } catch (Throwable t) {
                            throw new RuntimeException("Failed to get entity ID", t);
                        }
                        entityManager.persistenceContext().registerById(
                                entityModel.entityClass(), id, entity);
                    }
                    return results;
                }
            }
        } catch (Exception e) {
            entityManager.closeSqlConnection(conn);
            throw new RuntimeException("Failed to execute query: "
                    + jpqlQuery.entityName(), e);
        }
    }

    /**
     * Instantiates an entity from a {@link ResultSet} row.
     *
     * @param rs the result set
     * @return the instantiated entity
     * @throws RuntimeException if entity instantiation fails
     */
    @SuppressWarnings("unchecked")
    private Object instantiateEntity(ResultSet rs) throws Exception {
        Object entity = entityModel.entityClass().getDeclaredConstructor().newInstance();
        for (io.vidocq.mansart.data.dialect.Attribute<?, ?> attr : entityModel.attributes()) {
            String columnName = attr.columnName();
            Object value = rs.getObject(columnName);
            MethodHandle setter = attr.setter();
            if (setter != null && value != null) {
                try {
                    setter.invoke(entity, value);
                } catch (Throwable t) {
                    throw new RuntimeException("Failed to set attribute " + attr.name(), t);
                }
            }
        }
        return entity;
    }

    @Override
    public Object getSingleResult() {
        List<Object> results = getResultList();
        if (results.isEmpty()) {
            throw new jakarta.persistence.NoResultException(
                    "No entity found for query");
        }
        if (results.size() > 1) {
            throw new jakarta.persistence.NonUniqueResultException(
                    "Multiple entities found for query");
        }
        return results.get(0);
    }

    @Override
    public Object getSingleResultOrNull() {
        List<Object> results = getResultList();
        if (results.isEmpty()) {
            return null;
        }
        if (results.size() > 1) {
            throw new jakarta.persistence.NonUniqueResultException(
                    "Multiple entities found for query");
        }
        return results.get(0);
    }

    @Override
    public MansartQuery setMaxResults(int maxResult) {
        if (maxResult < 0) {
            throw new IllegalArgumentException("maxResults must be >= 0: " + maxResult);
        }
        this.maxResults = maxResult;
        return this;
    }

    @Override
    public MansartQuery setFirstResult(int startResult) {
        if (startResult < 0) {
            throw new IllegalArgumentException("firstResult must be >= 0: " + startResult);
        }
        this.firstResult = startResult;
        return this;
    }

    @Override
    public Set<jakarta.persistence.Parameter<?>> getParameters() {
        throw new UnsupportedOperationException("getParameters not implemented");
    }

    @Override
    public MansartQuery setHint(String key, Object value) {
        throw new UnsupportedOperationException("setHint not implemented");
    }

    @Override
    public Map<String, Object> getHints() {
        throw new UnsupportedOperationException("getHints not implemented");
    }

    @Override
    public Parameter<?> getParameter(String name) {
        throw new UnsupportedOperationException("getParameter(String) not implemented");
    }

    @Override
    public Parameter<?> getParameter(int position) {
        throw new UnsupportedOperationException("getParameter(int) not implemented");
    }

    @Override
    public <T> Parameter<T> getParameter(int position, Class<T> type) {
        throw new UnsupportedOperationException(
                "getParameter(int, Class<T>) not implemented");
    }

    @Override
    public <T> Parameter<T> getParameter(String name, Class<T> type) {
        throw new UnsupportedOperationException(
                "getParameter(String, Class<T>) not implemented");
    }

    @Override
    public MansartQuery setParameter(Parameter<Date> param, Date value,
                              TemporalType temporalType) {
        throw new UnsupportedOperationException(
                "setParameter(Parameter<Date>, Date, TemporalType) not implemented");
    }

    @Override
    public MansartQuery setParameter(Parameter<Calendar> param, Calendar value,
                              TemporalType temporalType) {
        throw new UnsupportedOperationException(
                "setParameter(Parameter<Calendar>, Calendar, TemporalType) not implemented");
    }

    @Override
    public <T> MansartQuery setParameter(Parameter<T> param, T value) {
        throw new UnsupportedOperationException(
                "setParameter(Parameter, T) not implemented");
    }

    @Override
    public MansartQuery setParameter(String name, Object value) {
        // For now, positional parameters only — name is ignored
        // Try to parse as numeric position
        try {
            int pos = Integer.parseInt(name.replace(":", ""));
            parameterValues.put(pos, value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Named parameters not supported: " + name);
        }
        return this;
    }

    @Override
    public MansartQuery setParameter(String name, Calendar value, TemporalType temporalType) {
        throw new UnsupportedOperationException(
                "setParameter(String, Calendar, TemporalType) not implemented");
    }

    @Override
    public MansartQuery setParameter(String name, Date value, TemporalType temporalType) {
        throw new UnsupportedOperationException(
                "setParameter(String, Date, TemporalType) not implemented");
    }

    @Override
    public MansartQuery setParameter(int position, Object value) {
        parameterValues.put(position, value);
        return this;
    }

    @Override
    public MansartQuery setParameter(int position, Calendar value, TemporalType temporalType) {
        throw new UnsupportedOperationException(
                "setParameter(int, Calendar, TemporalType) not implemented");
    }

    @Override
    public MansartQuery setParameter(int position, Date value, TemporalType temporalType) {
        throw new UnsupportedOperationException(
                "setParameter(int, Date, TemporalType) not implemented");
    }

    @Override
    public MansartQuery setLockMode(LockModeType lockMode) {
        throw new UnsupportedOperationException("setLockMode not implemented");
    }

    @Override
    public LockModeType getLockMode() {
        throw new UnsupportedOperationException("getLockMode not implemented");
    }

    @Override
    public Object getParameterValue(String name) {
        throw new UnsupportedOperationException(
                "getParameterValue(String) not implemented");
    }

    @Override
    public Object getParameterValue(int position) {
        throw new UnsupportedOperationException(
                "getParameterValue(int) not implemented");
    }

    @Override
    public <T> T getParameterValue(Parameter<T> param) {
        throw new UnsupportedOperationException(
                "getParameterValue(Parameter) not implemented");
    }

    @Override
    public boolean isBound(Parameter<?> param) {
        throw new UnsupportedOperationException("isBound(Parameter) not implemented");
    }

    @Override
    public int getMaxResults() {
        return maxResults;
    }

    @Override
    public int executeUpdate() {
        throw new UnsupportedOperationException(
                "executeUpdate not implemented for SELECT");
    }

    @Override
    public Integer getTimeout() {
        throw new UnsupportedOperationException("getTimeout not implemented");
    }

    @Override
    public FlushModeType getFlushMode() {
        throw new UnsupportedOperationException("getFlushMode not implemented on query");
    }

    @Override
    public <T> T unwrap(Class<T> cls) {
        throw new UnsupportedOperationException("unwrap not implemented");
    }

    @Override
    public MansartQuery setFlushMode(FlushModeType flushMode) {
        throw new UnsupportedOperationException("setFlushMode not implemented on query");
    }

    @Override
    public MansartQuery setCacheRetrieveMode(CacheRetrieveMode mode) {
        throw new UnsupportedOperationException("setCacheRetrieveMode not implemented");
    }

    @Override
    public CacheRetrieveMode getCacheRetrieveMode() {
        throw new UnsupportedOperationException("getCacheRetrieveMode not implemented");
    }

    @Override
    public MansartQuery setCacheStoreMode(CacheStoreMode mode) {
        throw new UnsupportedOperationException("setCacheStoreMode not implemented");
    }

    @Override
    public CacheStoreMode getCacheStoreMode() {
        throw new UnsupportedOperationException("getCacheStoreMode not implemented");
    }

    @Override
    public MansartQuery setTimeout(Integer timeout) {
        throw new UnsupportedOperationException("setTimeout not implemented");
    }

    @Override
    public int getFirstResult() {
        return firstResult;
    }
}
