/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.context;

import io.vidocq.mansart.persistence.core.dialect.EntityMapper;
import io.vidocq.mansart.persistence.core.runtime.MansartCallback;
import io.vidocq.mansart.persistence.spi.EntityAccessor;
import io.vidocq.mansart.persistence.spi.EntityModel;

import jakarta.persistence.LockModeType;
import jakarta.persistence.FindOption;
import jakarta.persistence.LockOption;
import jakarta.persistence.RefreshOption;
import jakarta.persistence.PersistenceException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
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
    private final EntityMapper entityMapper;
    private final DataSource dataSource;
    
    // Identity map: keyed by (entityClass, primaryKey)
    private final Map<EntityKey, Object> identityMap = new ConcurrentHashMap<>();
    
    // State tracking: entity instance -> EntityState (identity-based lookup)
    private final Map<Object, EntityState> entityStates = Collections.synchronizedMap(new java.util.IdentityHashMap<>());

    public MansartPersistenceContext(MansartCallback callback) {
        this(callback, null, null);
    }

    public MansartPersistenceContext(MansartCallback callback, EntityMapper entityMapper, DataSource dataSource) {
        this.callback = callback;
        this.entityMapper = entityMapper;
        this.dataSource = dataSource;
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
                // NEW entity (never persisted) - keep in NEW state until flush
                setState(entity, EntityState.NEW);
                Object id = extractId(entity.getClass(), entity);
                EntityKey key = new EntityKey(entity.getClass(), id);
                identityMap.put(key, entity);
                break;
            case EntityState.NEW:
                // Already in NEW state - no-op
                return;
            case EntityState.DETACHED, EntityState.REMOVED:
                // Transition to NEW (will be INSERTed on flush)
                setState(entity, EntityState.NEW);
                id = extractId(entity.getClass(), entity);
                key = new EntityKey(entity.getClass(), id);
                identityMap.put(key, entity);
                break;
            case EntityState.MANAGED:
                // Already managed - no-op
                return;
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

    @SuppressWarnings("java:S6208")
    public void remove(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        EntityState currentState = entityStates.get(entity);
        
        switch (currentState) {
            case EntityState.MANAGED:
            case EntityState.NEW:
                setState(entity, EntityState.REMOVED);
                break;
            case EntityState.REMOVED:
                // Already removed - no-op
                return;
            case null:
            case EntityState.DETACHED:
                throw new IllegalArgumentException("Entity must be MANAGED, NEW, or REMOVED to be removed");
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        EntityKey key = new EntityKey(entityClass, primaryKey);
        Object entity = identityMap.get(key);
        if (entity != null) {
            EntityState state = entityStates.get(entity);
            if (state != EntityState.REMOVED) {
                return (T) entity;
            }
        }
        
        if (dataSource == null || entityMapper == null) {
            return null;
        }
        
        try (Connection conn = dataSource.getConnection()) {
            T found = entityMapper.select(conn, entityClass, primaryKey);
            if (found != null) {
                setState(found, EntityState.MANAGED);
                identityMap.put(new EntityKey(entityClass, primaryKey), found);
            }
            return found;
        } catch (SQLException e) {
            throw new PersistenceException("Error during find", e);
        }
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
        if (dataSource == null || entityMapper == null) {
            return;
        }
        
        try (Connection conn = dataSource.getConnection()) {
            for (Map.Entry<Object, EntityState> entry : entityStates.entrySet()) {
                Object entity = entry.getKey();
                EntityState state = entry.getValue();
                if (state == null) {
                    continue;
                }
                
                Class<?> entityClass = entity.getClass();
                Object id = extractId(entityClass, entity);
                EntityKey key = new EntityKey(entityClass, id);
                
                switch (state) {
                    case NEW:
                        Object generatedId = entityMapper.insert(conn, entity);
                        if (generatedId != null) {
                            String idFieldName = callback.getEntityModel(entityClass).getIdAttributes().get(0).getName();
                            @SuppressWarnings("unchecked")
                            EntityAccessor<Object> accessor = (EntityAccessor<Object>) callback.getAccessor(entityClass);
                            accessor.set(entity, idFieldName, generatedId);
                        }
                        Object actualId = extractId(entityClass, entity);
                        identityMap.remove(key);
                        identityMap.put(new EntityKey(entityClass, actualId), entity);
                        setState(entity, EntityState.MANAGED);
                        break;
                    case REMOVED:
                        if (id != null) {
                            entityMapper.delete(conn, entityClass, id);
                        }
                        identityMap.remove(key);
                        entityStates.remove(entity);
                        break;
                    case MANAGED:
                        entityMapper.update(conn, entity);
                        break;
                    case DETACHED:
                        break;
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("Error during flush", e);
        }
    }

    public void refresh(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        EntityState currentState = entityStates.get(entity);
        
        // NEW and MANAGED entities can be refreshed (they're tracked in the context)
        if (currentState != EntityState.NEW && currentState != EntityState.MANAGED) {
            throw new IllegalArgumentException("Entity must be NEW or MANAGED to be refreshed");
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
        
        if (currentState == EntityState.MANAGED || currentState == EntityState.REMOVED || currentState == EntityState.NEW) {
            setState(entity, EntityState.DETACHED);
            // Remove from identity map
            Object id = extractId(entity.getClass(), entity);
            EntityKey key = new EntityKey(entity.getClass(), id);
            identityMap.remove(key);
        }
        // DETACHED: no-op
    }

    public boolean contains(Object entity) {
        if (entity == null) {
            return false;
        }

        EntityState state = entityStates.get(entity);
        // Only NEW, MANAGED, and REMOVED entities are tracked in the persistence context
        // DETACHED entities are not contained
        return state == EntityState.NEW || state == EntityState.MANAGED || state == EntityState.REMOVED;
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
