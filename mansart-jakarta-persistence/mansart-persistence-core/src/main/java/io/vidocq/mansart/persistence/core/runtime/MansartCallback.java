/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.runtime;

import io.vidocq.mansart.persistence.spi.EntityAccessor;
import io.vidocq.mansart.persistence.spi.EntityModel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dispatch mechanism for Tier 3 entities. Caches and provides
 * {@link EntityAccessor} and {@link EntityModel} instances for entities
 * that fell back to runtime Class-File API parsing.
 * <p>
 * Acts as the single entry point for the runtime to obtain metadata and
 * field access for entities that neither APT (Tier 1) nor the Maven plugin
 * (Tier 2) processed.
 */
public final class MansartCallback {

    private final Tier3WarningCollector warningCollector = new Tier3WarningCollector();
    private final RuntimeEntityModelBuilder modelBuilder = new RuntimeEntityModelBuilder(warningCollector);
    private final RuntimeEntityClassGenerator classGenerator = new RuntimeEntityClassGenerator();

    private final Map<Class<?>, EntityAccessor<?>> accessorCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, EntityModel<?>> modelCache = new ConcurrentHashMap<>();

    /**
     * Returns a cached or newly generated {@link EntityAccessor} for the given entity class.
     *
     * @param entityClass the entity class
     * @param <T>         the entity type
     * @return the entity accessor
     */
    @SuppressWarnings("unchecked")
    public <T> EntityAccessor<T> getAccessor(Class<T> entityClass) {
        return (EntityAccessor<T>) accessorCache.computeIfAbsent(entityClass, classGenerator::generateAccessor);
    }

    /**
     * Returns a cached or newly built {@link EntityModel} for the given entity class.
     *
     * @param entityClass the entity class
     * @param <T>         the entity type
     * @return the entity model
     */
    @SuppressWarnings("unchecked")
    public <T> EntityModel<T> getEntityModel(Class<T> entityClass) {
        return (EntityModel<T>) modelCache.computeIfAbsent(entityClass, modelBuilder::build);
    }

    /**
     * Returns the list of tier-3 warnings collected from entity model building.
     *
     * @return unmodifiable list of warning messages
     */
    public List<String> getWarnings() {
        return warningCollector.getWarnings();
    }
}
