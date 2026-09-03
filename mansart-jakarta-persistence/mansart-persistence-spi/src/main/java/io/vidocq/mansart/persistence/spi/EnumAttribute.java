/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.spi;

/**
 * Attribute for enum types.
 *
 * @param <T> the entity type
 * @param <V> the enum type
 */
public interface EnumAttribute<T, V extends Enum<V>> extends BasicAttribute<T, V> {

    /**
     * Returns the enum storage strategy.
     *
     * @return the enum strategy
     */
    EnumStrategy getEnumStrategy();

    /**
     * Enum storage strategies.
     */
    enum EnumStrategy {
        /** Store enum name as string */
        STRING,
        /** Store enum ordinal as number */
        ORDINAL
    }

    @Override
    default boolean isColumn() {
        return true;
    }
}
