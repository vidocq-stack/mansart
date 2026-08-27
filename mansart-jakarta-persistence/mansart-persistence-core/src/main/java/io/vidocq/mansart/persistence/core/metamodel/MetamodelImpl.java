/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.metamodel;

import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.ManagedType;

import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Implementation of {@link Metamodel} for the Mansart persistence provider.
 *
 * <p>This class maintains a registry of all managed types (entities, embeddables,
 * mapped superclasses) discovered in the persistence unit. The constructor
 * accepts a list of managed types and builds an internal lookup by Java class.</p>
 */
public final class MetamodelImpl implements Metamodel {

    private final Map<Class<?>, ManagedType<?>> byClass;
    private final Map<String, EntityType<?>> byName;

    @SuppressWarnings("rawtypes")
    public MetamodelImpl(List<ManagedType<?>> managedTypes) {
        this.byClass = new HashMap<>();
        this.byName = new HashMap<>();
        for (ManagedType<?> mt : managedTypes) {
            byClass.put(mt.getJavaType(), mt);
            if (mt instanceof EntityType<?>) {
                byName.put(((EntityType<?>) mt).getName(), (EntityType<?>) mt);
            }
        }
    }

    @Override
    public EntityType<?> entity(String entityName) {
        EntityType<?> et = byName.get(entityName);
        if (et == null) {
            throw new IllegalArgumentException(
                "No entity found with name: " + entityName);
        }
        return et;
    }

    @Override
    public <X> EntityType<X> entity(Class<X> cls) {
        ManagedType<?> mt = byClass.get(cls);
        if (mt == null) {
            throw new IllegalArgumentException("Not a managed class: " + cls.getName());
        }
        if (!(mt instanceof EntityType)) {
            throw new IllegalArgumentException("Not an entity: " + cls.getName());
        }
        return (EntityType<X>) mt;
    }

    @Override
    public <X> ManagedType<X> managedType(Class<X> cls) {
        ManagedType<?> mt = byClass.get(cls);
        if (mt == null) {
            throw new IllegalArgumentException("Not a managed class: " + cls.getName());
        }
        return (ManagedType<X>) mt;
    }

    @Override
    public <X> EmbeddableType<X> embeddable(Class<X> cls) {
        ManagedType<?> mt = byClass.get(cls);
        if (mt == null) {
            throw new IllegalArgumentException("Not a managed class: " + cls.getName());
        }
        if (!(mt instanceof EmbeddableType)) {
            throw new IllegalArgumentException("Not an embeddable: " + cls.getName());
        }
        return (EmbeddableType<X>) mt;
    }

    @Override
    public Set<ManagedType<?>> getManagedTypes() {
        return (Set) byClass.values();
    }

    @Override
    public Set<EntityType<?>> getEntities() {
        Set<EntityType<?>> result = new java.util.HashSet<>();
        for (ManagedType<?> mt : byClass.values()) {
            if (mt instanceof EntityType) {
                result.add((EntityType<?>) mt);
            }
        }
        return result;
    }

    @Override
    public Set<EmbeddableType<?>> getEmbeddables() {
        Set<EmbeddableType<?>> result = new java.util.HashSet<>();
        for (ManagedType<?> mt : byClass.values()) {
            if (mt instanceof EmbeddableType) {
                result.add((EmbeddableType<?>) mt);
            }
        }
        return result;
    }
}
