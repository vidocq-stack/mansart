/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.spi;

/**
 * Attribute for embedded objects.
 *
 * @param <T> the entity type
 * @param <V> the embedded value type
 */
public interface EmbeddedAttribute<T, V> extends Attribute<T, V> {

    /**
     * Returns the embedded entity model for the embedded object.
     *
     * @return the embedded entity model
     */
    EntityModel<V> getEmbeddedEntityModel();

    /**
     * Returns the prefix for column names of this embedded object.
     *
     * @return the column prefix, or empty string
     */
    default String getPrefix() {
        return "";
    }

    @Override
    default boolean isColumn() {
        return false;
    }
}
