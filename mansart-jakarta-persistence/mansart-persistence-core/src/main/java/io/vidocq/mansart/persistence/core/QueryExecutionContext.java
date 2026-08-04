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
            
            // M7 — Process explicit JOINs from this FROM item
            processJoins(fromItem, model);
        }
    }
    
    /**
     * M7 — Processes explicit JOINs from a FROM item.
     * Registers join aliases and their entity models.
     */
    @SuppressWarnings("unchecked")
    private void processJoins(JpqlFromItem fromItem, EntityModel<?> sourceModel) {
        String sourceAlias = fromItem.identifier();
        for (JpqlJoin join : fromItem.joins()) {
            String joinAlias = join.identifier();
            
            // Resolve the path from the source entity
            // The path is relative to the FROM item's alias
            // e.g., for "JOIN b.author a", path is "author" and source is "b"
            String path = join.path().path();
            
            // Build the full path expression: sourceAlias.path
            String fullPath = sourceAlias + "." + path;
            
            // Parse as a path expression and resolve
            JpqlPathExpr pathExpr = JpqlPathExpr.of(fullPath);
            Attribute<?, ?> attr = resolvePath(pathExpr);
            
            if (attr instanceof ReferenceAttribute<?, ?> refAttr) {
                // Get the target entity model from javaType (the referenced entity class)
                EntityModel<?> targetModel = entityManager.getEntityModel(refAttr.javaType());
                
                // Register the join alias
                aliasToEntityClass.put(joinAlias, refAttr.javaType());
                aliasToEntityModel.put(joinAlias, targetModel);
                
                // Store the join information for path resolution
                // This allows paths like "a.name" to resolve correctly
                joinedAliases.put(joinAlias, new JoinedPathInfo(sourceAlias, path));
            } else {
                throw new IllegalArgumentException(
                    "Join path " + path + " from " + sourceAlias + " does not resolve to a relationship");
            }
        }
    }
    
    /**
     * M7 — Stores information about a joined path for later resolution.
     */
    private static class JoinedPathInfo {
        final String sourceAlias;
        final String path;
        
        JoinedPathInfo(String sourceAlias, String path) {
            this.sourceAlias = sourceAlias;
            this.path = path;
        }
    }
    
    private final Map<String, JoinedPathInfo> joinedAliases = new HashMap<>();

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
        
        // M7 — Check if this alias is from a JOIN
        JoinedPathInfo joinedInfo = joinedAliases.get(alias);
        EntityModel<?> entityModel;
        
        if (joinedInfo != null) {
            // This alias is from a JOIN - resolve from the source alias
            // e.g., for "JOIN b.author a", alias "a" resolves from "b.author"
            entityModel = aliasToEntityModel.get(alias);
            if (entityModel == null) {
                throw new IllegalArgumentException("Unknown joined alias: " + alias);
            }
            
            // For joined aliases, if there are more parts, they are relative to the joined entity
            // e.g., "a.name" where "a" is a joined alias for Author
            if (parts.length > 1) {
                // Build path from the joined entity
                List<String> remainingParts = java.util.Arrays.asList(parts).subList(1, parts.length);
                return resolveAttributePath(entityModel, remainingParts);
            }
            // Single part is the joined alias itself - return its ID
            return entityModel.id();
        }
        
        entityModel = aliasToEntityModel.get(alias);
        
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
     * M7 — Resolves an attribute path from a given entity model.
     * Used for resolving paths relative to a joined entity.
     */
    @SuppressWarnings("unchecked")
    private Attribute<?, ?> resolveAttributePath(EntityModel<?> entityModel, List<String> parts) {
        if (parts.isEmpty()) {
            return entityModel.id();
        }
        
        // Build a path expression from the parts relative to this entity
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append(".");
            sb.append(parts.get(i));
        }
        
        // Create a temporary path expression - we need an alias prefix
        // Use a dummy alias that we'll ignore
        String tempPath = "_temp_." + sb.toString();
        JpqlPathExpr tempExpr = JpqlPathExpr.of(tempPath);
        
        // Create a temporary context with the entity mapped to _temp_
        // Actually, let's just resolve directly without going through resolvePath
        List<JoinPath.Step> joinSteps = new ArrayList<>();
        EntityModel<?> currentModel = entityModel;
        Attribute<?, ?> leafAttribute = null;
        Class<?> currentEntityType = entityModel.entityClass();
        
        for (String attrName : parts) {
            Optional<?> attrOptRaw = currentModel.attribute(attrName);
            
            if (attrOptRaw.isEmpty()) {
                throw new IllegalArgumentException(
                    "Cannot find attribute '" + attrName + "' in entity " + currentModel.entityClass().getSimpleName());
            }
            
            @SuppressWarnings("unchecked")
            Attribute<?, ?> attr = (Attribute<?, ?>) attrOptRaw.get();
            
            if (attr instanceof ReferenceAttribute<?, ?> refAttr) {
                String fkColumn = "";
                String refColumn = "id";
                String targetTable = "";
                String targetSchema = "";
                
                joinSteps.add(new JoinPath.Step(
                    attr.name(),
                    fkColumn,
                    refColumn,
                    targetTable,
                    targetSchema,
                    attr.javaType()
                ));
                
                currentEntityType = attr.javaType();
                currentModel = entityManager.getEntityModel(currentEntityType);
                leafAttribute = null;
            } else {
                if (leafAttribute == null && !joinSteps.isEmpty()) {
                    leafAttribute = attr;
                } else if (joinSteps.isEmpty()) {
                    leafAttribute = attr;
                }
            }
        }
        
        if (!joinSteps.isEmpty() && leafAttribute != null) {
            JoinPath joinPath = new JoinPath(joinSteps);
            return new JoinedAttribute<>(
                leafAttribute,
                joinPath,
                entityModel.entityClass()
            );
        }
        
        return leafAttribute != null ? leafAttribute : entityModel.id();
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
