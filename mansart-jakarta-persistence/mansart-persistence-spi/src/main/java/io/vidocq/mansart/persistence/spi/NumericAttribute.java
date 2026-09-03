/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.spi;

/**
 * Attribute for numeric types.
 *
 * @param <T> the entity type
 * @param <V> the numeric value type
 */
public interface NumericAttribute<T, V extends Number> extends BasicAttribute<T, V> {

    @Override
    default boolean isColumn() {
        return true;
    }
}
