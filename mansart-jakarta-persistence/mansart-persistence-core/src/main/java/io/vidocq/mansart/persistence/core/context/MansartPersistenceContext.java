/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.context;

import jakarta.persistence.LockModeType;
import jakarta.persistence.FindOption;
import jakarta.persistence.LockOption;
import jakarta.persistence.RefreshOption;
import java.util.Map;

/**
 * Mansart persistence context: owns the entity-state machine (NEW, MANAGED,
 * DETACHED, REMOVED) and the first-level identity map.
 *
 * <p>{@link io.vidocq.mansart.persistence.core.MansartEntityManager} delegates
 * entity-state operations here. The state machine, identity map and database
 * flush are implemented in card M4-JP-26; until then every operation throws
 * {@link UnsupportedOperationException} so that an unimplemented path is never
 * mistaken for a quiet success (no null/empty/false stand-ins).
 */
public final class MansartPersistenceContext {

    public void persist(Object entity) {
        throw new UnsupportedOperationException("not implemented: persist");
    }

    public <T> T merge(T entity) {
        throw new UnsupportedOperationException("not implemented: merge");
    }

    public void remove(Object entity) {
        throw new UnsupportedOperationException("not implemented: remove");
    }

    public <T> T find(Class<T> entityClass, Object primaryKey) {
        throw new UnsupportedOperationException("not implemented: find");
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
        throw new UnsupportedOperationException("not implemented: find");
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
        throw new UnsupportedOperationException("not implemented: find");
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode,
                     Map<String, Object> properties) {
        throw new UnsupportedOperationException("not implemented: find");
    }

    public <T> T find(Class<T> entityClass, Object primaryKey, FindOption... options) {
        throw new UnsupportedOperationException("not implemented: find");
    }

    public <T> T find(jakarta.persistence.EntityGraph<T> entityGraph, Object primaryKey,
                     FindOption... options) {
        throw new UnsupportedOperationException("not implemented: find");
    }

    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        throw new UnsupportedOperationException("not implemented: getReference");
    }

    public <T> T getReference(T entity) {
        throw new UnsupportedOperationException("not implemented: getReference");
    }

    public void flush() {
        throw new UnsupportedOperationException("not implemented: flush");
    }

    public void refresh(Object entity) {
        throw new UnsupportedOperationException("not implemented: refresh");
    }

    public void refresh(Object entity, Map<String, Object> properties) {
        throw new UnsupportedOperationException("not implemented: refresh");
    }

    public void refresh(Object entity, LockModeType lockMode) {
        throw new UnsupportedOperationException("not implemented: refresh");
    }

    public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        throw new UnsupportedOperationException("not implemented: refresh");
    }

    public void refresh(Object entity, RefreshOption... options) {
        throw new UnsupportedOperationException("not implemented: refresh");
    }

    public void clear() {
        throw new UnsupportedOperationException("not implemented: clear");
    }

    public void detach(Object entity) {
        throw new UnsupportedOperationException("not implemented: detach");
    }

    public boolean contains(Object entity) {
        throw new UnsupportedOperationException("not implemented: contains");
    }

    public void lock(Object entity, LockModeType lockMode) {
        throw new UnsupportedOperationException("not implemented: lock");
    }

    public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        throw new UnsupportedOperationException("not implemented: lock");
    }

    public void lock(Object entity, LockModeType lockMode, LockOption... options) {
        throw new UnsupportedOperationException("not implemented: lock");
    }

    public LockModeType getLockMode(Object entity) {
        throw new UnsupportedOperationException("not implemented: getLockMode");
    }
}
