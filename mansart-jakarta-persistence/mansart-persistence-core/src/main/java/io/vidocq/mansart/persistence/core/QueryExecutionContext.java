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

import io.vidocq.mansart.data.dialect.Attribute;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.data.query.ast.*;

import java.util.*;

/**
 * Maintains the execution context for JPQL query execution.
 * Handles alias-to-entity mapping and path expression resolution.
 * 
 * M5 — Query execution support.
 */
public final class QueryExecutionContext {

    private final MansartEntityManager entityManager;
    private final Map<String, EntityModel<?>> aliasToEntityModel = new HashMap<>();
    private final Map<String, Class<?>> aliasToEntityClass = new HashMap<>();

    public QueryExecutionContext(MansartEntityManager entityManager, JpqlFromClause fromClause) {
        this.entityManager = entityManager;
        initializeFromClause(fromClause);
    }

    /**
     * Initializes alias-to-entity mappings from the FROM clause.
     */
    @SuppressWarnings("unchecked")
    private void initializeFromClause(JpqlFromClause fromClause) {
        for (JpqlFromItem fromItem : fromClause.items()) {
            String alias = fromItem.identifier();
            String entityName = fromItem.entityName();
            
            // First, try to resolve the entity name as a fully qualified class name
            Class<?> entityClass = tryResolveEntityClass(entityName);
            
            if (entityClass == null) {
                throw new IllegalArgumentException(
                    "Cannot find entity class: " + entityName + " (alias: " + alias + ")");
            }
            
            aliasToEntityClass.put(alias, entityClass);
            EntityModel<?> model = entityManager.getEntityModel(entityClass);
            aliasToEntityModel.put(alias, model);
        }
    }

    /**
     * Tries to resolve an entity name to a Class.
     * First tries the fully qualified name, then tries with common base packages.
     */
    private Class<?> tryResolveEntityClass(String entityName) {
        // If it already looks like a FQN, try that first
        if (entityName.contains(".")) {
            try {
                return Class.forName(entityName);
            } catch (ClassNotFoundException e) {
                // Fall through to try other approaches
            }
        }
        
        // Try to find the class by scanning the test entity packages
        String[] basePackages = {
            "io.vidocq.mansart.persistence.core",
            "io.vidocq.mansart.test",
            "example"
        };
        
        for (String basePackage : basePackages) {
            String fqn = basePackage + "." + entityName;
            try {
                return Class.forName(fqn);
            } catch (ClassNotFoundException e) {
                // Try next package
            }
        }
        
        // Try without package (default package)
        try {
            return Class.forName(entityName);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Resolves a path expression to an Attribute.
     * Handles simple paths like "b.price" or "b.author.name".
     * 
     * @param pathExpr the path expression
     * @return the resolved Attribute
     */
    @SuppressWarnings("unchecked")
    public Attribute<?, ?> resolvePath(JpqlPathExpr pathExpr) {
        String path = pathExpr.path();
        String[] parts = path.split("\\.");
        
        if (parts.length == 0) {
            throw new IllegalArgumentException("Empty path expression");
        }
        
        // First part is the alias
        String alias = parts[0];
        EntityModel<?> entityModel = aliasToEntityModel.get(alias);
        
        if (entityModel == null) {
            throw new IllegalArgumentException("Unknown alias: " + alias);
        }
        
        // If only one part (e.g., "b"), return the ID attribute or first attribute
        if (parts.length == 1) {
            // This is just the alias itself - return the entity's ID or first attribute
            return entityModel.id();
        }
        
        // Resolve the path within the entity
        Attribute<?, ?> currentAttr = entityModel.id(); // Default to ID if path has only one part
        EntityModel<?> currentModel = entityModel;
        
        for (int i = 1; i < parts.length; i++) {
            String attrName = parts[i];
            
            // Try to find the attribute in the current model
            @SuppressWarnings("unchecked")
            Optional<Attribute<?, ?>> attrOpt = (Optional<Attribute<?, ?>>) (Optional<?>) currentModel.attribute(attrName);
            
            if (attrOpt.isEmpty()) {
                // If attribute not found, try to find an entity with that name
                // This handles cases like "b.author" where "author" is a relationship
                throw new UnsupportedOperationException(
                    "Path resolution for relationship '" + attrName + "' not yet implemented");
            }
            
            currentAttr = attrOpt.get();
        }
        
        return currentAttr;
    }

    /**
     * Resolves a path expression string to an Attribute.
     */
    public Attribute<?, ?> resolvePath(String path) {
        return resolvePath(new JpqlPathExpr(path, List.of(path.split("\\."))));
    }

    /**
     * Gets the EntityModel for an alias.
     */
    public EntityModel<?> getEntityModel(String alias) {
        EntityModel<?> model = aliasToEntityModel.get(alias);
        if (model == null) {
            throw new IllegalArgumentException("Unknown alias: " + alias);
        }
        return model;
    }

    /**
     * Gets the entity class for an alias.
     */
    public Class<?> getEntityClass(String alias) {
        Class<?> clazz = aliasToEntityClass.get(alias);
        if (clazz == null) {
            throw new IllegalArgumentException("Unknown alias: " + alias);
        }
        return clazz;
    }

    /**
     * Gets the root entity model (first FROM item).
     */
    public EntityModel<?> getRootEntityModel() {
        if (aliasToEntityModel.isEmpty()) {
            throw new IllegalStateException("No FROM clause initialized");
        }
        return aliasToEntityModel.values().iterator().next();
    }

    public MansartEntityManager getEntityManager() {
        return entityManager;
    }
}
