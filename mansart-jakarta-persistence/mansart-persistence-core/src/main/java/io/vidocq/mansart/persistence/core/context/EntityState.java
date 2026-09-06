/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.context;

/**
 * Entity lifecycle states in the persistence context.
 * <p>
 * These states follow the Jakarta Persistence 3.2 spec §3.2.
 */
public enum EntityState {
    /**
     * New entity instance that has not been persisted yet.
     * Not associated with any persistence context.
     */
    NEW,

    /**
     * Entity instance managed by a persistence context.
     * Changes to the entity are tracked and synchronized to the database at flush time.
     */
    MANAGED,

    /**
     * Entity instance previously managed but now detached.
     * No longer tracked by the persistence context.
     */
    DETACHED,

    /**
     * Entity instance managed but marked for removal.
     * Will be deleted from the database at flush time.
     */
    REMOVED
}
