/**
 * Represents the lifecycle state of an entity instance within a persistence context.
 * 
 * <p>Entity states follow the JPA 3.2 specification lifecycle:
 * <ul>
 *   <li>{@link #NEW} - New entity, not yet persisted</li>
 *   <li>{@link #MANAGED} - Entity is managed by the persistence context</li>
 *   <li>{@link #DETACHED} - Entity was managed but has been detached</li>
 *   <li>{@link #REMOVED} - Entity has been removed from the persistence context</li>
 * </ul>
 */
package io.vidocq.mansart.persistence.core;

/**
 * Entity lifecycle states as defined by Jakarta Persistence 3.2.
 */
public enum EntityState {
    
    /**
     * The entity instance is new (not yet persisted).
     * It has no persistent identity and is not associated with a persistence context.
     */
    NEW,
    
    /**
     * The entity instance is managed by a persistence context.
     * It has a persistent identity and changes are tracked for synchronization with the database.
     */
    MANAGED,
    
    /**
     * The entity instance was previously managed but has been detached from the persistence context.
     * It has a persistent identity but changes are not synchronized with the database.
     */
    DETACHED,
    
    /**
     * The entity instance has been removed from the persistence context.
     * It will be deleted from the database when the transaction commits.
     */
    REMOVED
}
