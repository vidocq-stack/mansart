/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.spi;

import java.util.List;

/**
 * Metadata model for a JPA entity.
 * Describes the entity's table name, attributes, and their mapping to database columns.
 *
 * <p>This is the root interface for the Mansart Persistence metadata hierarchy.
 * It is populated at compile time (APT), build time (Maven plugin), or bootstrap time (runtime fallback).
 *
 * @param <T> the entity type
 */
public interface EntityModel<T> {

    /**
     * Returns the entity class.
     *
     * @return the entity class
     */
    Class<T> getEntityClass();

    /**
     * Returns the name of the table this entity maps to.
     *
     * @return the table name
     */
    String getTableName();

    /**
     * Returns the schema name for this entity's table, or empty if not specified.
     *
     * @return the schema name
     */
    String getSchema();

    /**
     * Returns the catalog name for this entity's table, or empty if not specified.
     *
     * @return the catalog name
     */
    String getCatalog();

    /**
     * Returns all attributes of this entity, including inherited ones.
     *
     * @return unmodifiable list of all attributes
     */
    List<Attribute<?, ?>> getAttributes();

    /**
     * Returns the attribute with the given name.
     *
     * @param name the attribute name
     * @return the attribute, or null if not found
     */
    Attribute<?, ?> getAttribute(String name);

    /**
     * Returns the id attribute(s) of this entity.
     *
     * @return list of id attributes (single or composite)
     */
    List<Attribute<?, ?>> getIdAttributes();

    /**
     * Returns the version attribute, if present.
     *
     * @return the version attribute, or null if not present
     */
    Attribute<?, ?> getVersionAttribute();

    /**
     * Returns true if this entity uses optimistic locking (has a version attribute).
     *
     * @return true if optimistic locking is enabled
     */
    default boolean isOptimisticLockingEnabled() {
        return getVersionAttribute() != null;
    }

    /**
     * Returns true if this entity has a single id attribute.
     *
     * @return true if single id
     */
    default boolean isSingleId() {
        return getIdAttributes().size() == 1;
    }

    /**
     * Returns true if this entity has composite id.
     *
     * @return true if composite id
     */
    default boolean isCompositeId() {
        return getIdAttributes().size() > 1;
    }
}
