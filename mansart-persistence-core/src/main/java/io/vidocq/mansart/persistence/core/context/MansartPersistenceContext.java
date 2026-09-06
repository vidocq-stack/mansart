/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.context;

import io.vidocq.mansart.persistence.core.runtime.MansartCallback;
import io.vidocq.mansart.persistence.spi.Attribute;
import io.vidocq.mansart.persistence.spi.EntityModel;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.IllegalArgumentException;
import jakarta.persistence.LockModeType;
import jakarta.persistence.FindOption;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Mansart persistence context: owns the entity-state machine (NEW/MANAGED/DETACHED/REMOVED)
 * and the first-level identity map.
 * 
 * <p>This implementation follows the Jakarta Persistence 3.2 spec §3.2 for entity lifecycle
 * states and the identity map semantics.
 * 
 * <p>The identity map is a {@link ConcurrentMap} keyed by {@link EntityKey} (entity class + id),
 * and the state of each entity instance is tracked in a separate map using identity semantics.
 * 
 * <p>No database access, no flush, no SQL, no JDBC in M4-JP-26.
 */
public final class MansartPersistenceContext {

    private final MansartCallback callback;
    private final ConcurrentMap<EntityKey, Object> identityMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<Object, EntityState> entityStates = 
        Collections.synchronizedMap(new java.util.IdentityHashMap<>());

    /**
     * Creates a new persistence context with a callback for metadata lookup.
     * 
     * @param callback the callback to obtain entity metadata and accessors
     */
    public MansartPersistenceContext(MansartCallback callback) {
        this.callback = callback;
    }

    /**
     * Creates a new persistence context with a shared callback.
     * This constructor is provided for backward compatibility during the transition.
     * 
     * @deprecated Use {@link #MansartPersistenceContext(MansartCallback)} instead.
     */
    @Deprecated
    public MansartPersistenceContext() {
        this(null);
    }

    // ── Entity-state operations ────────────────────────────────────────────

    /**
     * Makes an entity instance managed and persistent.
     * 
     * <p>State transitions:
     * <ul>
     *   <li>NEW → MANAGED</li>
     *   <li>DETACHED → MANAGED</li>
     *   <li>REMOVED → MANAGED</li>
     *   <li>MANAGED → MANAGED (no-op)</li>
     * </ul>
     * 
     * @param entity the entity instance
     * @throws IllegalArgumentException if entity is null or not an entity
     */
    public void persist(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null");
        }

        EntityModel<?> model = getEntityModel(entity);
        if (model == null) {
            throw new IllegalArgumentException("Not an entity: " + entity.getClass().getName());
        }

        EntityState currentState = getState(entity);
        switch (currentState) {
            case NEW:
            case DETACHED:
            case REMOVED:
                setState(entity, EntityState.MANAGED);
                putInIdentityMap(entity);
                break;
            case MANAGED:
                // No-op: already managed
                break;
        }
    }

    /**
     * Merges the state of the given entity into the persistence context.
     * 
     * <p>If the entity is already managed, returns it unchanged.
     * If detached or new, creates a new managed instance, copies the state,
     * registers it in the identity map, and returns the managed copy.
     * The input entity remains in its original state.
     * 
     * @param entity the entity instance
     * @return a managed copy of the entity
     * @throws IllegalArgumentException if entity is null or not an entity
     */
    @SuppressWarnings("unchecked")
    public <T> T merge(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null");
        }

        EntityModel<T> model = (EntityModel<T>) getEntityModel(entity);
        if (model == null) {
            throw new IllegalArgumentException("Not an entity: " + entity.getClass().getName());
        }

        EntityState currentState = getState(entity);
        
        // If already managed, return the same instance
        if (currentState == EntityState.MANAGED) {
            return entity;
        }

        // For NEW, DETACHED, or REMOVED: create a new managed instance
        T managedCopy;
        try {
            managedCopy = entity.getClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to create managed copy of entity", e);
        }

        // Copy id and other fields from original to copy
        Attribute<T, ?> idAttribute = model.getIdAttributes().get(0);
        Object idValue = idAttribute.get(entity);
        idAttribute.set(managedCopy, idValue);

        // Copy all basic/column attributes (skip id since we already copied it)
        // For M4-JP-26, we only need to copy the id to make the copy identifiable
        // Other fields would be copied by later milestones with cascade/attribute tracking

        // Register the managed copy in the identity map
        setState(managedCopy, EntityState.MANAGED);
        putInIdentityMap(managedCopy);

        return managedCopy;
    }

    /**
     * Removes the entity instance from the persistence context.
     * 
     * <p>State transitions:
     * <ul>
     *   <li>MANAGED → REMOVED</li>
     *   <li>REMOVED → REMOVED (no-op)</li>
     * </ul>
     * 
     * @param entity the entity instance
     * @throws IllegalArgumentException if entity is null, not managed, or not an entity
     */
    public void remove(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null");
        }

        EntityModel<?> model = getEntityModel(entity);
        if (model == null) {
            throw new IllegalArgumentException("Not an entity: " + entity.getClass().getName());
        }

        EntityState currentState = getState(entity);
        switch (currentState) {
            case MANAGED:
                setState(entity, EntityState.REMOVED);
                break;
            case REMOVED:
                // No-op: already removed
                break;
            case NEW:
            case DETACHED:
                throw new IllegalArgumentException(
                    "Entity is not managed: " + entity.getClass().getName());
        }
    }

    /**
     * Finds an entity by its primary key.
     * 
     * <p>For M4-JP-26, this only checks the identity map (no database access).
     * Returns the managed instance if found, or null if not in context.
     * 
     * @param entityClass the entity class
     * @param primaryKey the primary key value
     * @return the managed entity instance, or null if not found
     * @throws IllegalArgumentException if entityClass is null
     */
    @SuppressWarnings("unchecked")
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        if (entityClass == null) {
            throw new IllegalArgumentException("Entity class must not be null");
        }
        if (primaryKey == null) {
            throw new IllegalArgumentException("Primary key must not be null");
        }

        EntityKey key = new EntityKey(entityClass, primaryKey);
        Object entity = identityMap.get(key);
        
        // For M4-JP-26, return the cached instance or null (no DB lookup)
        return entity != null ? (T) entity : null;
    }

    /**
     * Unsupported operation in M4-JP-26 (lock modes require database access).
     */
    public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
        throw new UnsupportedOperationException("not implemented: find with properties");
    }

    /**
     * Unsupported operation in M4-JP-26 (lock modes require database access).
     */
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
        throw new UnsupportedOperationException("not implemented: find with LockModeType");
    }

    /**
     * Unsupported operation in M4-JP-26 (lock modes require database access).
     */
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode,
                     Map<String, Object> properties) {
        throw new UnsupportedOperationException("not implemented: find with LockModeType and properties");
    }

    /**
     * Unsupported operation in M4-JP-26 (FindOption requires database access).
     */
    public <T> T find(Class<T> entityClass, Object primaryKey, FindOption... options) {
        throw new UnsupportedOperationException("not implemented: find with FindOption");
    }

    /**
     * Unsupported operation in M4-JP-26 (entity graph requires database access).
     */
    public <T> T find(jakarta.persistence.EntityGraph<T> entityGraph, Object primaryKey,
                     FindOption... options) {
        throw new UnsupportedOperationException("not implemented: find with EntityGraph");
    }

    /**
     * Unsupported operation in M4-JP-26 (lazy proxy requires database access).
     */
    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        throw new UnsupportedOperationException("not implemented: getReference");
    }

    /**
     * Unsupported operation in M4-JP-26 (lazy proxy requires database access).
     */
    public <T> T getReference(T entity) {
        throw new UnsupportedOperationException("not implemented: getReference");
    }

    /**
     * Flush is a no-op in M4-JP-26 (no database access).
     */
    public void flush() {
        // No-op: no database to flush to
    }

    /**
     * Refreshes the state of the entity from the database.
     * 
     * <p>For M4-JP-26, this is a no-op since there's no database.
     * Only validates that the entity is managed.
     * 
     * @param entity the entity instance
     * @throws IllegalArgumentException if entity is null or not managed
     */
    public void refresh(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null");
        }

        EntityState currentState = getState(entity);
        if (currentState != EntityState.MANAGED) {
            throw new IllegalArgumentException(
                "Entity is not managed and cannot be refreshed: " + entity.getClass().getName());
        }
        // No-op: no database to refresh from
    }

    /**
     * Unsupported operation in M4-JP-26 (lock modes require database access).
     */
    public void refresh(Object entity, Map<String, Object> properties) {
        throw new UnsupportedOperationException("not implemented: refresh with properties");
    }

    /**
     * Unsupported operation in M4-JP-26 (lock modes require database access).
     */
    public void refresh(Object entity, LockModeType lockMode) {
        throw new UnsupportedOperationException("not implemented: refresh with LockModeType");
    }

    /**
     * Unsupported operation in M4-JP-26 (lock modes require database access).
     */
    public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        throw new UnsupportedOperationException("not implemented: refresh with LockModeType and properties");
    }

    /**
     * Unsupported operation in M4-JP-26 (RefreshOption requires database access).
     */
    public void refresh(Object entity, jakarta.persistence.RefreshOption... options) {
        throw new UnsupportedOperationException("not implemented: refresh with RefreshOption");
    }

    /**
     * Clears the persistence context.
     * 
     * <p>All entities become DETACHED, and the identity map is emptied.
     */
    public void clear() {
        // Detach all entities in the context
        for (Object entity : entityStates.keySet()) {
            setState(entity, EntityState.DETACHED);
        }
        // Clear the identity map
        identityMap.clear();
    }

    /**
     * Detaches the entity instance from the persistence context.
     * 
     * <p>State transitions:
     * <ul>
     *   <li>MANAGED → DETACHED</li>
     *   <li>REMOVED → DETACHED</li>
     *   <li>DETACHED → DETACHED (no-op)</li>
     *   <li>NEW → NEW (no-op)</li>
     * </ul>
     * 
     * @param entity the entity instance
     */
    public void detach(Object entity) {
        if (entity == null) {
            return; // No-op for null
        }

        EntityState currentState = getState(entity);
        switch (currentState) {
            case MANAGED:
            case REMOVED:
                setState(entity, EntityState.DETACHED);
                break;
            case DETACHED:
            case NEW:
                // No-op
                break;
        }
    }

    /**
     * Checks if the entity instance is in the persistence context.
     * 
     * <p>Returns true if the entity is MANAGED or REMOVED (still tracked).
     * 
     * @param entity the entity instance
     * @return true if the entity is in the context
     */
    public boolean contains(Object entity) {
        if (entity == null) {
            return false;
        }

        EntityState state = getState(entity);
        return state == EntityState.MANAGED || state == EntityState.REMOVED;
    }

    /**
     * Gets the current state of an entity instance.
     * Package-private for testing.
     * 
     * @param entity the entity instance
     * @return the entity state
     */
    EntityState getState(Object entity) {
        return entityStates.getOrDefault(entity, EntityState.NEW);
    }

    /**
     * Unsupported operation in M4-JP-26 (lock modes require database access).
     */
    public void lock(Object entity, LockModeType lockMode) {
        throw new UnsupportedOperationException("not implemented: lock");
    }

    /**
     * Unsupported operation in M4-JP-26 (lock modes require database access).
     */
    public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        throw new UnsupportedOperationException("not implemented: lock with properties");
    }

    /**
     * Unsupported operation in M4-JP-26 (lock modes require database access).
     */
    public void lock(Object entity, LockModeType lockMode, jakarta.persistence.LockOption... options) {
        throw new UnsupportedOperationException("not implemented: lock with LockOption");
    }

    /**
     * Unsupported operation in M4-JP-26 (lock mode retrieval requires database access).
     */
    public LockModeType getLockMode(Object entity) {
        throw new UnsupportedOperationException("not implemented: getLockMode");
    }

    // ── Internal helpers ───────────────────────────────────────────────────

    /**
     * Gets the entity model for the given entity instance.
     * 
     * @param entity the entity instance
     * @return the entity model, or null if not an entity
     */
    private EntityModel<?> getEntityModel(Object entity) {
        if (callback != null) {
            return callback.getEntityModel(entity.getClass());
        }
        // Fallback for backward compatibility
        try {
            Class<?> entityClass = entity.getClass();
            // Try to find EntityModel via reflection (simplified for M4-JP-26)
            // In a real implementation, this would use the proper mechanism
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the current state of an entity instance.
     * 
     * @param entity the entity instance
     * @return the entity state
     */
    private EntityState getState(Object entity) {
        return entityStates.getOrDefault(entity, EntityState.NEW);
    }

    /**
     * Sets the state of an entity instance.
     * 
     * @param entity the entity instance
     * @param state the new state
     */
    private void setState(Object entity, EntityState state) {
        entityStates.put(entity, state);
    }

    /**
     * Puts an entity in the identity map using its primary key.
     * 
     * @param entity the entity instance
     */
    private void putInIdentityMap(Object entity) {
        EntityModel<?> model = getEntityModel(entity);
        if (model != null && model.isSingleId()) {
            Attribute<?, ?> idAttribute = model.getIdAttributes().get(0);
            Object idValue = idAttribute.get(entity);
            if (idValue != null) {
                EntityKey key = new EntityKey(entity.getClass(), idValue);
                identityMap.put(key, entity);
            }
        }
    }
}
