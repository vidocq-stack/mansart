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

import io.vidocq.mansart.data.query.ast.JpqlStmt;
import jakarta.persistence.*;
import java.util.*;

/**
 * Abstract base class for Mansart query implementations.
 * 
 * M5 — JPQL Query implementation.
 */
public abstract class AbstractMansartQuery<X> {

    protected final JpqlStmt jpqlStatement;
    protected final MansartEntityManager entityManager;
    
    protected final Map<String, Object> namedParameters = new HashMap<>();
    protected final Map<Integer, Object> positionalParameters = new HashMap<>();
    protected int firstResult = 0;
    protected int maxResults = Integer.MAX_VALUE;
    protected FlushModeType flushMode = FlushModeType.AUTO;
    protected LockModeType lockMode = LockModeType.NONE;
    protected Map<String, Object> hints = new HashMap<>();
    protected boolean readOnly = false;
    protected Integer timeout = null;
    protected CacheRetrieveMode cacheRetrieveMode = CacheRetrieveMode.USE;
    protected CacheStoreMode cacheStoreMode = CacheStoreMode.USE;

    public AbstractMansartQuery(JpqlStmt jpqlStatement, MansartEntityManager entityManager) {
        this.jpqlStatement = Objects.requireNonNull(jpqlStatement, "JPQL statement cannot be null");
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager cannot be null");
    }

    // Parameter binding
    public X setParameter(String name, Object value) { namedParameters.put(name, value); return self(); }
    public X setParameter(String name, Date value, TemporalType temporalType) { return setParameter(name, value); }
    public X setParameter(String name, Calendar value, TemporalType temporalType) { return setParameter(name, value); }
    public X setParameter(int position, Object value) { positionalParameters.put(position, value); return self(); }
    public X setParameter(int position, Date value, TemporalType temporalType) { return setParameter(position, value); }
    public X setParameter(int position, Calendar value, TemporalType temporalType) { return setParameter(position, value); }
    public X setParameter(Parameter<Date> param, Date value, TemporalType temporalType) { return setParameter(param.getName(), value, temporalType); }
    public X setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) { return setParameter(param.getName(), value, temporalType); }
    public <T> X setParameter(Parameter<T> param, T value) { return setParameter(param.getName(), value); }
    public Set<Parameter<?>> getParameters() { return Set.of(); }
    public Object getParameterValue(String name) { return namedParameters.get(name); }
    public Object getParameterValue(int position) { return positionalParameters.get(position); }
    public Parameter<?> getParameter(String name) { return null; }
    public <T> Parameter<T> getParameter(String name, Class<T> type) { return null; }
    public Parameter<?> getParameter(int position) { return null; }
    public <T> Parameter<T> getParameter(int position, Class<T> type) { return null; }
    public <T> T getParameterValue(Parameter<T> param) { 
        @SuppressWarnings("unchecked")
        T value = (T) namedParameters.get(param.getName());
        return value;
    }
    public boolean isBound(Parameter<?> param) { 
        return namedParameters.containsKey(param.getName()) || 
               (param.getPosition() >= 1 && positionalParameters.containsKey(param.getPosition()));
    }

    // Result control
    public X setFirstResult(int startPosition) { this.firstResult = startPosition; return self(); }
    public int getFirstResult() { return firstResult; }
    public X setMaxResults(int maxResult) { this.maxResults = maxResult; return self(); }
    public int getMaxResults() { return maxResults; }

    // Flush mode
    public X setFlushMode(FlushModeType flushMode) { this.flushMode = flushMode; return self(); }
    public FlushModeType getFlushMode() { return flushMode; }

    // Lock mode
    public X setLockMode(LockModeType lockMode) { this.lockMode = lockMode; return self(); }
    public X setLockMode(LockModeType lockMode, jakarta.persistence.LockOption... options) { this.lockMode = lockMode; return self(); }
    public LockModeType getLockMode() { return lockMode; }

    // Hints
    public X setHint(String hintName, Object value) { 
        hints.put(hintName, value); 
        if ("jakarta.persistence.readonly".equals(hintName)) readOnly = Boolean.TRUE.equals(value);
        return self(); 
    }
    public Map<String, Object> getHints() { return Collections.unmodifiableMap(hints); }

    // Timeout
    public X setTimeout(Integer timeout) { this.timeout = timeout; return self(); }
    public Integer getTimeout() { return timeout; }

    // Cache modes
    public X setCacheRetrieveMode(CacheRetrieveMode mode) { this.cacheRetrieveMode = mode; return self(); }
    public CacheRetrieveMode getCacheRetrieveMode() { return cacheRetrieveMode; }
    public X setCacheStoreMode(CacheStoreMode mode) { this.cacheStoreMode = mode; return self(); }
    public CacheStoreMode getCacheStoreMode() { return cacheStoreMode; }

    // Read only
    public X setReadOnly(boolean readOnly) { this.readOnly = readOnly; return self(); }
    public boolean isReadOnly() { return readOnly; }

    // Unwrap
    @SuppressWarnings("unchecked")
    public <Y> Y unwrap(Class<Y> type) { 
        if (type.isInstance(this)) return type.cast(this); 
        throw new IllegalArgumentException("Cannot unwrap to " + type.getName()); 
    }

    // Helpers
    protected JpqlStmt getJpqlStatement() { return jpqlStatement; }
    protected MansartEntityManager getEntityManager() { return entityManager; }
    
    @SuppressWarnings("unchecked")
    protected X self() { return (X) this; }
}
