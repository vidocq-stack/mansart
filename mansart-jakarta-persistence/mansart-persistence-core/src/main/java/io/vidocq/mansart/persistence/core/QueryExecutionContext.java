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
import io.vidocq.mansart.data.dialect.attribute.JoinedAttribute;
import io.vidocq.mansart.data.dialect.attribute.JoinPath;
import io.vidocq.mansart.data.dialect.attribute.ReferenceAttribute;
import io.vidocq.mansart.data.query.ast.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Maintains the execution context for JPQL query execution.
 * Handles alias-to-entity mapping and path expression resolution.
 * 
 * M5 — Query execution support.
 */
public final class QueryExecutionContext {

    private final MansartEntityManager entityManager;
    private final EntityNameResolver entityNameResolver;
    private final Map<String, EntityModel<?>> aliasToEntityModel = new HashMap<>();
    private final Map<String, Class<?>> aliasToEntityClass = new HashMap<>();

    public QueryExecutionContext(MansartEntityManager entityManager, JpqlFromClause fromClause) {
        this.entityManager = entityManager;
        this.entityNameResolver = new EntityNameResolver(entityManager);
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
            
            // Use EntityNameResolver to resolve the entity class
            Class<?> entityClass = entityNameResolver.resolveEntityClass(entityName);
            
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
     * Resolves a path expression to an Attribute.
     * Handles simple paths like "b.price" or "b.author.name".
     * 
     * <p>For relationship paths (e.g., "b.author.name"), creates a JoinedAttribute
     * that represents the leaf attribute through a chain of relationships.</p>
     * 
     * @param pathExpr the path expression
     * @return the resolved Attribute (may be a JoinedAttribute for relationship paths)
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
        
        // If only one part (e.g., "b"), return the ID attribute
        if (parts.length == 1) {
            return entityModel.id();
        }
        
        // Build the path step by step
        List<JoinPath.Step> joinSteps = new ArrayList<>();
        EntityModel<?> currentModel = entityModel;
        Attribute<?, ?> leafAttribute = null;
        Class<?> currentEntityType = entityModel.entityClass();
        
        for (int i = 1; i < parts.length; i++) {
            String attrName = parts[i];
            
            Optional<?> attrOptRaw = currentModel.attribute(attrName);
            
            if (attrOptRaw.isEmpty()) {
                throw new IllegalArgumentException(
                    "Cannot find attribute '" + attrName + "' in entity " + currentModel.entityClass().getSimpleName());
            }
            
            @SuppressWarnings("unchecked")
            Attribute<?, ?> attr = (Attribute<?, ?>) attrOptRaw.get();
            
            // Check if this is a relationship attribute (ReferenceAttribute)
            if (attr instanceof ReferenceAttribute<?, ?> refAttr) {
                // This is a relationship - add to join path
                String fkColumn = ""; // Will be set properly by dialect
                String refColumn = ""; // Will be set properly by dialect
                String targetTable = ""; // Will be set properly by dialect
                
                joinSteps.add(new JoinPath.Step(
                    attrName,
                    fkColumn,
                    refColumn,
                    targetTable,
                    "",
                    attr.javaType()
                ));
                
                // Move to the target entity model
                currentEntityType = attr.javaType();
                currentModel = entityManager.getEntityModel(currentEntityType);
                leafAttribute = null; // Reset, we need to find the leaf in the target entity
            } else {
                // This is a leaf attribute
                if (leafAttribute == null && !joinSteps.isEmpty()) {
                    // This is the leaf attribute after traversing relationships
                    leafAttribute = attr;
                } else if (joinSteps.isEmpty()) {
                    // Simple attribute path (no relationships)
                    leafAttribute = attr;
                }
            }
        }
        
        if (!joinSteps.isEmpty() && leafAttribute != null) {
            // Create a JoinedAttribute for relationship paths
            JoinPath joinPath = new JoinPath(joinSteps);
            return new JoinedAttribute<>(
                leafAttribute,
                joinPath,
                (Class<?>) entityModel.entityClass()
            );
        }
        
        // Simple attribute path (no relationships)
        return leafAttribute != null ? leafAttribute : entityModel.id();
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
