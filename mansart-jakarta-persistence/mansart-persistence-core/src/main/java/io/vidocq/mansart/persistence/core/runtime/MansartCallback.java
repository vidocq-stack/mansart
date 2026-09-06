/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.runtime;

import io.vidocq.mansart.persistence.spi.EntityAccessor;
import io.vidocq.mansart.persistence.spi.EntityModel;

import jakarta.persistence.PersistenceException;

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
     * Creates a new MansartCallback with no pre-registered entities.
     */
    public MansartCallback() {
    }

    /**
     * Creates a new MansartCallback with pre-registered entity classes.
     * This pre-builds their EntityModel and EntityAccessor instances.
     *
     * @param entityClasses list of entity classes to pre-register
     */
    public MansartCallback(List<Class<?>> entityClasses) {
        for (Class<?> entityClass : entityClasses) {
            accessorCache.computeIfAbsent(entityClass, classGenerator::generateAccessor);
            modelCache.computeIfAbsent(entityClass, modelBuilder::build);
        }
    }

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
     * Instantiates a new entity instance of the given class.
     * This is the runtime tier fallback for entity instantiation.
     *
     * @param entityClass the entity class to instantiate
     * @param <T>         the entity type
     * @return a new entity instance
     * @throws PersistenceException if instantiation fails
     */
    public <T> T instantiate(Class<T> entityClass) {
        try {
            return entityClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new PersistenceException("Failed to instantiate entity of type " + entityClass.getName(), e);
        }
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
