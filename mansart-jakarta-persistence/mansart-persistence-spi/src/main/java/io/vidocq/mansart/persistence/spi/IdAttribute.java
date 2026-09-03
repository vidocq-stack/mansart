/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.spi;

/**
 * Attribute that represents an ID field.
 *
 * @param <T> the entity type
 * @param <V> the ID type
 */
public interface IdAttribute<T, V> extends Attribute<T, V> {

    /**
     * Returns the ID generation strategy.
     *
     * @return the generation strategy
     */
    GenerationStrategy getGenerationStrategy();

    /**
     * Returns the generator name, if specified.
     *
     * @return the generator name, or empty string
     */
    default String getGenerator() {
        return "";
    }

    /**
     * Returns the sequence name, if using SEQUENCE strategy.
     *
     * @return the sequence name, or empty string
     */
    default String getSequenceName() {
        return "";
    }

    /**
     * Returns the table name for TABLE strategy, if applicable.
     *
     * @return the table generator table name, or empty string
     */
    default String getTable() {
        return "";
    }

    /**
     * ID generation strategies.
     */
    enum GenerationStrategy {
        /** Auto - provider selects appropriate strategy */
        AUTO,
        /** Identity column (database auto-increment) */
        IDENTITY,
        /** Database sequence */
        SEQUENCE,
        /** Table-based generator */
        TABLE
    }

    @Override
    default boolean isId() {
        return true;
    }
}
