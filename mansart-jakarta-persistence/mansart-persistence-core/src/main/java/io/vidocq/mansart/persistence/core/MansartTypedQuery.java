/*
 * Copyright (c) ${year} Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

package io.vidocq.mansart.persistence.core;

import io.vidocq.mansart.data.core.RepositoryRuntime;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.data.query.ast.JpqlSelectStmt;
import io.vidocq.mansart.data.query.ast.JpqlStmt;
import jakarta.persistence.*;
import java.util.*;

/**
 * Mansart implementation of Jakarta Persistence TypedQuery interface.
 * 
 * M5 — JPQL TypedQuery implementation.
 */
public class MansartTypedQuery<T> extends AbstractMansartQuery<TypedQuery<T>> implements TypedQuery<T> {

    private final Class<T> resultClass;

    public MansartTypedQuery(JpqlStmt jpqlStatement, Class<T> resultClass, MansartEntityManager entityManager) {
        super(jpqlStatement, entityManager);
        this.resultClass = Objects.requireNonNull(resultClass, "Result class cannot be null");
    }

    // TypedQuery specific return types
    @Override public TypedQuery<T> setParameter(String name, Object value) { super.setParameter(name, value); return this; }
    @Override public TypedQuery<T> setParameter(String name, Date value, TemporalType temporalType) { super.setParameter(name, value, temporalType); return this; }
    @Override public TypedQuery<T> setParameter(String name, Calendar value, TemporalType temporalType) { super.setParameter(name, value, temporalType); return this; }
    @Override public TypedQuery<T> setParameter(int position, Object value) { super.setParameter(position, value); return this; }
    @Override public TypedQuery<T> setParameter(int position, Date value, TemporalType temporalType) { super.setParameter(position, value, temporalType); return this; }
    @Override public TypedQuery<T> setParameter(int position, Calendar value, TemporalType temporalType) { super.setParameter(position, value, temporalType); return this; }
    @Override public TypedQuery<T> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) { super.setParameter(param, value, temporalType); return this; }
    @Override public TypedQuery<T> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) { super.setParameter(param, value, temporalType); return this; }
    @Override public <X> TypedQuery<T> setParameter(Parameter<X> param, X value) { super.setParameter(param, value); return this; }
    @Override public TypedQuery<T> setFirstResult(int startPosition) { super.setFirstResult(startPosition); return this; }
    @Override public TypedQuery<T> setMaxResults(int maxResult) { super.setMaxResults(maxResult); return this; }
    @Override public TypedQuery<T> setFlushMode(FlushModeType flushMode) { super.setFlushMode(flushMode); return this; }
    @Override public TypedQuery<T> setLockMode(LockModeType lockMode) { super.setLockMode(lockMode); return this; }
    @Override public TypedQuery<T> setLockMode(LockModeType lockMode, jakarta.persistence.LockOption... options) { super.setLockMode(lockMode); return this; }
    @Override public TypedQuery<T> setHint(String hintName, Object value) { super.setHint(hintName, value); return this; }
    @Override public TypedQuery<T> setReadOnly(boolean readOnly) { super.setReadOnly(readOnly); return this; }
    @Override public TypedQuery<T> setTimeout(Integer timeout) { super.setTimeout(timeout); return this; }
    @Override public TypedQuery<T> setCacheRetrieveMode(CacheRetrieveMode mode) { super.setCacheRetrieveMode(mode); return this; }
    @Override public TypedQuery<T> setCacheStoreMode(CacheStoreMode mode) { super.setCacheStoreMode(mode); return this; }

    @Override
    @SuppressWarnings("unchecked")
    public <X> X unwrap(Class<X> type) { 
        if (type.isInstance(this)) return type.cast(this); 
        throw new IllegalArgumentException("Cannot unwrap to " + type.getName()); 
    }

    /* -------- Execution -------- */

    @Override
    public List<T> getResultList() {
        // Get results from parent and cast to T
        List<?> untypedResults = executeSelectQuery();
        List<T> typedResults = new ArrayList<>();
        for (Object result : untypedResults) {
            typedResults.add(resultClass.cast(result));
        }
        return typedResults;
    }

    @SuppressWarnings("unchecked")
    private List<?> executeSelectQuery() {
        JpqlStmt stmt = getJpqlStatement();
        
        if (stmt instanceof JpqlSelectStmt selectStmt) {
            try {
                JpqlToRuntimeConverter.QueryExecutionParams params = 
                    JpqlToRuntimeConverter.convert(selectStmt, getEntityManager(),
                                                    getNamedParameters(), getPositionalParameters());
                
                RepositoryRuntime runtime = getEntityManager().getRepositoryRuntime();
                
                // Extract parameters in the order they appear in the query
                Object[] args = ParameterExtractor.extractParameters(
                    selectStmt, getNamedParameters(), getPositionalParameters());
                
                return runtime.queryList(params.entityModel(), params.where(), params.orderBy(), args);
            } catch (UnsupportedOperationException | IllegalArgumentException e) {
                // Fallback: if path resolution fails or entity can't be resolved, return empty list
                return List.of();
            }
        }
        
        return List.of();
    }

    /**
     * Gets the named parameters map from this query.
     */
    protected Map<String, Object> getNamedParameters() {
        return Collections.unmodifiableMap(namedParameters);
    }

    /**
     * Gets the positional parameters map from this query.
     */
    protected Map<Integer, Object> getPositionalParameters() {
        return Collections.unmodifiableMap(positionalParameters);
    }

    @Override
    public T getSingleResult() {
        List<T> results = getResultList();
        if (results.isEmpty()) {
            throw new NoResultException("No entity found for query");
        }
        if (results.size() > 1) {
            throw new NonUniqueResultException("More than one result for getSingleResult()");
        }
        return results.get(0);
    }

    @Override
    public T getSingleResultOrNull() {
        try {
            return getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public int executeUpdate() {
        return 0;
    }

    /* -------- Helpers -------- */

    public Class<T> getResultClass() {
        return resultClass;
    }
}
