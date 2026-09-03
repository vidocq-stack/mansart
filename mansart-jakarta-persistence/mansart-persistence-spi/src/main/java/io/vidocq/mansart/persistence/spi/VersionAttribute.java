/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.spi;

/**
 * Attribute that represents a version field for optimistic locking.
 *
 * @param <T> the entity type
 * @param <V> the version type (typically Integer, Long, Short, java.time.Instant)
 */
public interface VersionAttribute<T, V> extends Attribute<T, V> {

    /**
     * Returns the versioning strategy.
     *
     * @return the version strategy
     */
    VersionStrategy getVersionStrategy();

    /**
     * Version strategies.
     */
    enum VersionStrategy {
        /** Simple numeric version increment */
        NUMERIC,
        /** Timestamp-based versioning */
        TIMESTAMP
    }

    @Override
    default boolean isVersion() {
        return true;
    }

    @Override
    default boolean isColumn() {
        return true;
    }
}
