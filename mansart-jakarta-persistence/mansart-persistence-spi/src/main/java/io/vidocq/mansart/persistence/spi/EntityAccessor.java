/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.spi;

/**
 * Provides field access for entities at runtime.
 * Used by Tier 3 (runtime Class-File API fallback) to access entity fields
 * without reflection — the implementation is a generated hidden class
 * that calls the entity's public getters and setters.
 *
 * @param <T> the entity type
 */
public interface EntityAccessor<T> {

    /**
     * Gets the value of a field from the given entity instance.
     *
     * @param entity    the entity instance
     * @param fieldName the field name
     * @return the field value
     * @throws IllegalArgumentException if the field name is unknown
     */
    Object get(T entity, String fieldName);

    /**
     * Sets the value of a field on the given entity instance.
     *
     * @param entity    the entity instance
     * @param fieldName the field name
     * @param value     the value to set
     * @throws IllegalArgumentException if the field name is unknown
     */
    void set(T entity, String fieldName, Object value);
}
