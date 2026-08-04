/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import io.vidocq.mansart.data.dialect.EntityModel;
import jakarta.persistence.Entity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves JPA entity names to their corresponding entity classes and EntityModel instances.
 * 
 * <p>This resolver maintains mappings from:</p>
 * <ul>
 *   <li>Entity name (from @Entity.name attribute) to Class</li>
 *   <li>Simple class name to Class</li>
 *   <li>Fully qualified class name to Class</li>
 * </ul>
 * 
 * <p>M6 — Proper entity name resolution for JPQL queries.</p>
 */
public final class EntityNameResolver {

    private final Map<String, Class<?>> nameToClass = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> simpleNameToClass = new ConcurrentHashMap<>();
    private final Map<Class<?>, EntityModel<?>> classToModel = new ConcurrentHashMap<>();
    private final Map<String, EntityModel<?>> nameToModel = new ConcurrentHashMap<>();
    
    private final MansartEntityManager entityManager;

    /**
     * Creates a new EntityNameResolver for the given EntityManager.
     *
     * @param entityManager the EntityManager to use for model resolution
     */
    public EntityNameResolver(MansartEntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager cannot be null");
    }

    /**
     * Registers an entity class with its JPA entity name.
     * The entity name is determined from the @Entity annotation's name attribute,
     * or defaults to the simple class name if not specified.
     *
     * @param entityClass the entity class to register
     * @param <T> the entity type
     */
    public <T> void registerEntity(Class<T> entityClass) {
        Objects.requireNonNull(entityClass, "Entity class cannot be null");
        
        // Verify the class is an entity
        if (!entityClass.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException(
                "Class " + entityClass.getName() + " is not annotated with @Entity");
        }
        
        // Get the entity name from @Entity annotation
        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
        String entityName = entityAnnotation.name();
        
        // If @Entity.name is empty (default), use the simple class name
        if (entityName == null || entityName.isEmpty()) {
            entityName = entityClass.getSimpleName();
        }
        
        // Store mappings
        nameToClass.put(entityName, entityClass);
        simpleNameToClass.put(entityClass.getSimpleName(), entityClass);
        nameToClass.put(entityClass.getName(), entityClass);
        
        // Resolve and cache the EntityModel
        EntityModel<T> model = entityManager.getEntityModel(entityClass);
        classToModel.put(entityClass, model);
        nameToModel.put(entityName, model);
        nameToModel.put(entityClass.getSimpleName(), model);
        nameToModel.put(entityClass.getName(), model);
    }

    /**
     * Resolves an entity name to its corresponding Class.
     * Tries the following in order:
     * 1. Exact match in registered entity names
     * 2. Exact match in simple names
     * 3. Exact match in fully qualified names
     * 4. Class.forName with the name as-is (for FQN)
     * 5. Class.forName with common base packages
     *
     * @param entityName the entity name to resolve
     * @return the resolved entity class, or null if not found
     */
    public Class<?> resolveEntityClass(String entityName) {
        Objects.requireNonNull(entityName, "Entity name cannot be null");
        
        // Try registered mappings first
        Class<?> resolved = nameToClass.get(entityName);
        if (resolved != null) return resolved;
        
        resolved = simpleNameToClass.get(entityName);
        if (resolved != null) return resolved;
        
        resolved = nameToClass.get(entityName);
        if (resolved != null) return resolved;
        
        // Try direct Class.forName (for FQN)
        try {
            Class<?> clazz = Class.forName(entityName);
            if (clazz.isAnnotationPresent(Entity.class)) {
                // Auto-register and return
                registerEntity(clazz);
                return clazz;
            }
        } catch (ClassNotFoundException e) {
            // Continue with fallback
        }
        
        // Try with common base packages
        String[] basePackages = {
            "io.vidocq.mansart.persistence.core",
            "io.vidocq.mansart.test",
            "example",
            "", // default package
            getClass().getPackageName()
        };
        
        for (String basePackage : basePackages) {
            String fqn = basePackage.isEmpty() ? entityName : basePackage + "." + entityName;
            try {
                Class<?> clazz = Class.forName(fqn);
                if (clazz.isAnnotationPresent(Entity.class)) {
                    registerEntity(clazz);
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
                // Try next package
            }
        }
        
        return null;
    }

    /**
     * Resolves an entity name to its EntityModel.
     *
     * @param entityName the entity name to resolve
     * @return the EntityModel for the entity, or null if not found
     */
    public EntityModel<?> resolveEntityModel(String entityName) {
        Class<?> entityClass = resolveEntityClass(entityName);
        if (entityClass == null) return null;
        return classToModel.get(entityClass);
    }

    /**
     * Gets the EntityModel for a registered entity class.
     *
     * @param entityClass the entity class
     * @return the EntityModel, or null if not registered
     */
    public EntityModel<?> getEntityModel(Class<?> entityClass) {
        return classToModel.get(entityClass);
    }

    /**
     * Checks if an entity name is registered.
     *
     * @param entityName the entity name to check
     * @return true if the entity name is registered
     */
    public boolean isEntityRegistered(String entityName) {
        return nameToClass.containsKey(entityName) ||
               simpleNameToClass.containsKey(entityName);
    }

    /**
     * Gets all registered entity classes.
     *
     * @return unmodifiable set of all registered entity classes
     */
    public Set<Class<?>> getRegisteredEntities() {
        return Collections.unmodifiableSet(new HashSet<>(nameToClass.values()));
    }

    /**
     * Clears all registered entities. Useful for testing.
     */
    public void clear() {
        nameToClass.clear();
        simpleNameToClass.clear();
        classToModel.clear();
        nameToModel.clear();
    }
}
