/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

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
        // TODO: Execute query and return typed results
        return List.of();
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
