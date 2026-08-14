/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.runtime;

import io.vidocq.mansart.persistence.core.cache.EntityCache;
import io.vidocq.mansart.persistence.spi.EntityState;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Manages entity lifecycle state transitions.
 *
 * <p>This class tracks and transitions entity states through the persistence
 * context using an {@link EntityCache} for state storage.
 *
 * <p>Entity states:
 * <ul>
 *   <li>{@link EntityState#NEW} - Entity not yet persisted</li>
 *   <li>{@link EntityState#MANAGED} - Entity associated with persistence context</li>
 *   <li>{@link EntityState#DETACHED} - Entity was persistent but detached</li>
 *   <li>{@link EntityState#REMOVED} - Entity scheduled for removal</li>
 * </ul>
 */
public class EntityStateManager {

    private final EntityCache cache;
    private final Map<Object, EntityState> states;

    /**
     * Creates a new {@code EntityStateManager} instance.
     *
     * @param cache the entity cache for storing entities
     */
    public EntityStateManager(EntityCache cache) {
        this.cache = cache;
        this.states = new IdentityHashMap<>();
    }

    /**
     * Gets the current state of an entity.
     *
     * @param entity the entity to check
     * @return the current state of the entity, or {@link EntityState#NEW} if not tracked
     */
    public EntityState getState(Object entity) {
        return states.getOrDefault(entity, EntityState.NEW);
    }

    /**
     * Sets the state of an entity.
     *
     * @param entity the entity to update
     * @param newState the new state
     */
    public void setState(Object entity, EntityState newState) {
        states.put(entity, newState);
    }

    /**
     * Checks if an entity is persisted (MANAGED or REMOVED).
     *
     * @param entity the entity to check
     * @return true if the entity is MANAGED or REMOVED
     */
    public boolean isPersisted(Object entity) {
        EntityState state = states.get(entity);
        return state == EntityState.MANAGED || state == EntityState.REMOVED;
    }

    /**
     * Marks an entity as MANAGED.
     *
     * @param entity the entity to mark
     */
    public void markManaged(Object entity) {
        states.put(entity, EntityState.MANAGED);
    }

    /**
     * Marks an entity as DETACHED.
     *
     * @param entity the entity to mark
     */
    public void markDetached(Object entity) {
        states.put(entity, EntityState.DETACHED);
    }

    /**
     * Marks an entity as REMOVED.
     *
     * @param entity the entity to mark
     */
    public void markRemoved(Object entity) {
        states.put(entity, EntityState.REMOVED);
    }
}
