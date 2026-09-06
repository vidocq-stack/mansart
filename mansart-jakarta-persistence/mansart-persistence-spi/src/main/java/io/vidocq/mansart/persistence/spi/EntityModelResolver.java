/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.spi;

/**
 * Resolves entity models for a given entity class.
 * <p>
 * This SPI is used by the persistence runtime to obtain metadata about entities
 * at runtime without reflection.
 */
public interface EntityModelResolver {

    /**
     * Resolves the entity model for the given entity class.
     *
     * @param entityClass the entity class
     * @param <T> the entity type
     * @return the entity model
     * @throws IllegalArgumentException if entityClass is null or no model is available
     */
    <T> EntityModel<T> resolve(Class<T> entityClass);
}
