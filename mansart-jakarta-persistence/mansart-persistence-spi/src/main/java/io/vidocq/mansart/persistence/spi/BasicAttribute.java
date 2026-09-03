/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.spi;

/**
 * Attribute for basic types (String, Integer, Boolean, etc.).
 *
 * @param <T> the entity type
 * @param <V> the attribute value type
 */
public interface BasicAttribute<T, V> extends Attribute<T, V> {

    /**
     * Returns true if this is a lob (large object) attribute.
     *
     * @return true if lob
     */
    default boolean isLob() {
        return false;
    }

    /**
     * Returns the lob type if applicable.
     *
     * @return the lob type, or null
     */
    default LobType getLobType() {
        return null;
    }

    /**
     * LOB types.
     */
    enum LobType {
        /** Binary large object */
        BLOB,
        /** Character large object */
        CLOB
    }

    @Override
    default boolean isColumn() {
        return true;
    }
}
