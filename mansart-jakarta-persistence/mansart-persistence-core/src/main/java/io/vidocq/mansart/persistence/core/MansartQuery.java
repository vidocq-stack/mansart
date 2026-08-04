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
 * Mansart implementation of Jakarta Persistence Query interface.
 * 
 * M5 — JPQL Query implementation.
 */
public class MansartQuery extends AbstractMansartQuery<Query> implements Query {

    public MansartQuery(JpqlStmt jpqlStatement, MansartEntityManager entityManager) {
        super(jpqlStatement, entityManager);
    }

    // Query specific return types
    @Override public Query setParameter(String name, Object value) { super.setParameter(name, value); return this; }
    @Override public Query setParameter(String name, Date value, TemporalType temporalType) { super.setParameter(name, value, temporalType); return this; }
    @Override public Query setParameter(String name, Calendar value, TemporalType temporalType) { super.setParameter(name, value, temporalType); return this; }
    @Override public Query setParameter(int position, Object value) { super.setParameter(position, value); return this; }
    @Override public Query setParameter(int position, Date value, TemporalType temporalType) { super.setParameter(position, value, temporalType); return this; }
    @Override public Query setParameter(int position, Calendar value, TemporalType temporalType) { super.setParameter(position, value, temporalType); return this; }
    @Override public Query setParameter(Parameter<Date> param, Date value, TemporalType temporalType) { super.setParameter(param, value, temporalType); return this; }
    @Override public Query setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) { super.setParameter(param, value, temporalType); return this; }
    @Override public <T> Query setParameter(Parameter<T> param, T value) { super.setParameter(param, value); return this; }
    @Override public Query setFirstResult(int startPosition) { super.setFirstResult(startPosition); return this; }
    @Override public Query setMaxResults(int maxResult) { super.setMaxResults(maxResult); return this; }
    @Override public Query setFlushMode(FlushModeType flushMode) { super.setFlushMode(flushMode); return this; }
    @Override public Query setLockMode(LockModeType lockMode) { super.setLockMode(lockMode); return this; }
    @Override public Query setHint(String hintName, Object value) { super.setHint(hintName, value); return this; }
    @Override public Query setTimeout(Integer timeout) { super.setTimeout(timeout); return this; }
    @Override public Query setCacheRetrieveMode(CacheRetrieveMode mode) { super.setCacheRetrieveMode(mode); return this; }
    @Override public Query setCacheStoreMode(CacheStoreMode mode) { super.setCacheStoreMode(mode); return this; }
    @Override public Query setReadOnly(boolean readOnly) { super.setReadOnly(readOnly); return this; }

    @Override
    @SuppressWarnings("unchecked")
    public <X> X unwrap(Class<X> type) { 
        if (type.isInstance(this)) return type.cast(this); 
        throw new IllegalArgumentException("Cannot unwrap to " + type.getName()); 
    }

    /* -------- Execution -------- */

    @Override
    public List<?> getResultList() {
        // TODO: Execute query and return results
        return List.of();
    }

    @Override
    public Object getSingleResult() {
        List<?> results = getResultList();
        if (results.isEmpty()) {
            throw new NoResultException("No entity found for query");
        }
        if (results.size() > 1) {
            throw new NonUniqueResultException("More than one result for getSingleResult()");
        }
        return results.get(0);
    }

    @Override
    public Object getSingleResultOrNull() {
        try {
            return getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public int executeUpdate() {
        // TODO: Execute UPDATE or DELETE
        return 0;
    }
}
