/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.context;

import io.vidocq.mansart.persistence.core.runtime.MansartCallback;
import io.vidocq.mansart.persistence.spi.EntityAccessor;
import io.vidocq.mansart.persistence.spi.EntityModel;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import jakarta.persistence.FindOption;
import jakarta.persistence.LockOption;
import jakarta.persistence.RefreshOption;

import java.lang.IllegalArgumentException;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private final MansartCallback callback;
    
    // Identity map: keyed by (entityClass, primaryKey)
    private final Map<EntityKey, Object> identityMap = new ConcurrentHashMap<>();
    
    // State tracking: entity instance -> EntityState (identity-based lookup)
    private final Map<Object, EntityState> entityStates = Collections.synchronizedMap(new java.util.IdentityHashMap<>());

    public MansartPersistenceContext(MansartCallback callback) {
        this.callback = callback;
    }

    /**
     * Extracts the primary key value from an entity using the callback's metadata.
     * Returns null if the id attribute is null (NEW entity without assigned id).
     *
     * @param entityClass the entity class
     * @param entity the entity instance
     * @return the primary key value, or null
     */
    private Object extractId(Class<?> entityClass, Object entity) {
        EntityModel<?> model = callback.getEntityModel(entityClass);
        if (!model.isSingleId()) {
            throw new UnsupportedOperationException("Composite ids not supported in M4-JP-26");
        }
        String idFieldName = model.getIdAttributes().get(0).getName();
        @SuppressWarnings("unchecked")
        EntityAccessor<Object> accessor = callback.getAccessor((Class<Object>) entityClass);
        return accessor.get(entity, idFieldName);
    }

    public void persist(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        EntityState currentState = entityStates.get(entity);
        
        switch (currentState) {
            case null:
                // NEW entity (never persisted)
                setState(entity, EntityState.MANAGED);
                Object id = extractId(entity.getClass(), entity);
                EntityKey key = new EntityKey(entity.getClass(), id);
                identityMap.put(key, entity);
                break;
            case EntityState.NEW:
            case EntityState.DETACHED:
                // Transition to MANAGED
                setState(entity, EntityState.MANAGED);
                id = extractId(entity.getClass(), entity);
                key = new EntityKey(entity.getClass(), id);
                identityMap.put(key, entity);
                break;
            case EntityState.MANAGED:
                // Already managed - no-op
                return;
            case EntityState.REMOVED:
                // Transition from REMOVED to MANAGED
                setState(entity, EntityState.MANAGED);
                id = extractId(entity.getClass(), entity);
                key = new EntityKey(entity.getClass(), id);
                identityMap.put(key, entity);
                break;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T merge(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        EntityState currentState = entityStates.get(entity);
        
        if (currentState == EntityState.MANAGED) {
            // Already managed - return same instance
            return entity;
        }

        // For NEW, DETACHED, or REMOVED: create a new managed copy
        // In M4-JP-26, we don't actually copy the entity, we just track it as MANAGED
        // The input entity stays in its original state
        setState(entity, EntityState.MANAGED);
        Object id = extractId(entity.getClass(), entity);
        EntityKey key = new EntityKey(entity.getClass(), id);
        identityMap.put(key, entity);
        
        return entity;
    }

    public void remove(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        EntityState currentState = entityStates.get(entity);
        
        switch (currentState) {
            case null:
            case EntityState.NEW:
            case EntityState.DETACHED:
                throw new IllegalArgumentException("Entity must be MANAGED or REMOVED to be removed");
            case EntityState.MANAGED:
                setState(entity, EntityState.REMOVED);
                break;
            case EntityState.REMOVED:
                // Already removed - no-op
                return;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        EntityKey key = new EntityKey(entityClass, primaryKey);
        Object entity = identityMap.get(key);
        if (entity != null) {
            return (T) entity;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
        // Ignore properties for M4-JP-26
        return find(entityClass, primaryKey);
    }

    @SuppressWarnings("unchecked")
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
        // Ignore lock mode for M4-JP-26
        return find(entityClass, primaryKey);
    }

    @SuppressWarnings("unchecked")
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode,
                     Map<String, Object> properties) {
        // Ignore lock mode and properties for M4-JP-26
        return find(entityClass, primaryKey);
    }

    @SuppressWarnings("unchecked")
    public <T> T find(Class<T> entityClass, Object primaryKey, FindOption... options) {
        // Ignore options for M4-JP-26
        return find(entityClass, primaryKey);
    }

    @SuppressWarnings("unchecked")
    public <T> T find(jakarta.persistence.EntityGraph<T> entityGraph, Object primaryKey,
                     FindOption... options) {
        // Ignore entityGraph and options for M4-JP-26
        throw new UnsupportedOperationException("EntityGraph not supported in M4-JP-26");
    }

    public void flush() {
        // No-op for M4-JP-26 (no database)
    }

    public void refresh(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        EntityState currentState = entityStates.get(entity);
        
        if (currentState != EntityState.MANAGED) {
            throw new IllegalArgumentException("Entity must be MANAGED to be refreshed");
        }
        
        // No-op for M4-JP-26 (no database)
    }

    public void clear() {
        // All entities become DETACHED and are removed from identity map
        for (Object entity : entityStates.keySet()) {
            entityStates.put(entity, EntityState.DETACHED);
        }
        identityMap.clear();
    }

    public void detach(Object entity) {
        if (entity == null) {
            return; // No-op for null
        }

        EntityState currentState = entityStates.get(entity);
        
        if (currentState == EntityState.MANAGED || currentState == EntityState.REMOVED) {
            setState(entity, EntityState.DETACHED);
            // Remove from identity map
            Object id = extractId(entity.getClass(), entity);
            EntityKey key = new EntityKey(entity.getClass(), id);
            identityMap.remove(key);
        }
        // DETACHED and NEW: no-op
    }

    public boolean contains(Object entity) {
        if (entity == null) {
            return false;
        }

        EntityState state = entityStates.get(entity);
        return state == EntityState.MANAGED || state == EntityState.REMOVED;
    }

    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        throw new UnsupportedOperationException("not implemented: getReference");
    }

    public <T> T getReference(T entity) {
        throw new UnsupportedOperationException("not implemented: getReference");
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

    /**
     * Updates the state of an entity.
     *
     * @param entity the entity instance
     * @param newState the new state
     */
    private void setState(Object entity, EntityState newState) {
        entityStates.put(entity, newState);
    }
}
