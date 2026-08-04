/*
 * Copyright (c) ${year} Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * or any later version, which is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * It is also made available under the European Union Public Licence v. 1.2,
 * which is available at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

package io.vidocq.mansart.persistence.core;

import io.vidocq.mansart.data.core.RuntimeEntityModelBuilder;
import io.vidocq.mansart.data.dialect.EntityModel;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Utility class for resolving EntityModel instances for entity classes.
 * 
 * <p>M4 — Resolves EntityModel by looking up the generated _<EntityName> class
 * and accessing its $MODEL static field. Falls back to runtime model building
 * via {@link RuntimeEntityModelBuilder} for entities without generated metamodel
 * (e.g., TCK entities).
 */
public final class EntityModelResolver {

    private static final ConcurrentMap<Class<?>, EntityModel<?>> CACHE = new ConcurrentHashMap<>();

    private EntityModelResolver() {}

    /**
     * Resolves the EntityModel for the given entity class.
     * 
     * <p>First checks the cache, then tries to find the generated _<EntityName> class
     * and access its $MODEL field. If the generated metamodel is not found,
     * falls back to runtime model building using {@link RuntimeEntityModelBuilder}.
     *
     * @param entityClass the entity class
     * @param <T> the entity type
     * @return the EntityModel for the entity class
     * @throws IllegalArgumentException if no EntityModel can be found or built
     */
    @SuppressWarnings("unchecked")
    public static <T> EntityModel<T> resolve(Class<T> entityClass) {
        return (EntityModel<T>) CACHE.computeIfAbsent(entityClass, clazz -> {
            // Try to find the generated _<EntityName> class
            String pkg = clazz.getPackageName();
            String generatedName = pkg.isEmpty() ? "_" + clazz.getSimpleName() : pkg + "._" + clazz.getSimpleName();
            
            try {
                Class<?> metaClass = Class.forName(generatedName, true, clazz.getClassLoader());
                Field modelField = metaClass.getField("$MODEL");
                Object model = modelField.get(null);
                if (model instanceof EntityModel) {
                    return (EntityModel<?>) model;
                }
                throw new IllegalArgumentException(
                    "Generated metamodel class " + generatedName + " has $MODEL field with wrong type: " + model.getClass().getName());
            } catch (ClassNotFoundException e) {
                // Generated metamodel class not found - try runtime building
                try {
                    return RuntimeEntityModelBuilder.build(clazz);
                } catch (Exception ex) {
                    throw new IllegalArgumentException(
                        "No generated metamodel found for entity " + clazz.getName() + 
                        ". Expected class: " + generatedName + 
                        ". Also failed to build runtime model: " + ex.getMessage(), e);
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalArgumentException(
                    "Generated metamodel class " + generatedName + " has no public static $MODEL field", e);
            }
        });
    }

    /**
     * Clears the cache. Useful for testing.
     */
    public static void clearCache() {
        CACHE.clear();
    }
}