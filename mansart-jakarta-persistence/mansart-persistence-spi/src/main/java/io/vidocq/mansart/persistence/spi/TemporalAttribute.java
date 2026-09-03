/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.spi;

/**
 * Attribute for temporal (date/time) types.
 *
 * @param <T> the entity type
 * @param <V> the temporal value type
 */
public interface TemporalAttribute<T, V> extends BasicAttribute<T, V> {

    /**
     * Returns the temporal type.
     *
     * @return the temporal type
     */
    TemporalType getTemporalType();

    /**
     * Temporal types as defined by JPA.
     */
    enum TemporalType {
        /** Date only (no time) */
        DATE,
        /** Time only (no date) */
        TIME,
        /** Date and time */
        TIMESTAMP
    }

    @Override
    default boolean isColumn() {
        return true;
    }
}
