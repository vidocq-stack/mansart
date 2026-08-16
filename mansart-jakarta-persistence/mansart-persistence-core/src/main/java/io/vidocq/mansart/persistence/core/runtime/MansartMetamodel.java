/*
 * Copyright (c) 2026 Vidocq contributors
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.runtime;

import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.Type;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Set;

/**
 * Minimal stub implementation of Metamodel for JPA 3.2.
 * Phase 1: Dynamic proxy that returns stub types to allow EntityGraph tests to proceed.
 */
public class MansartMetamodel implements Metamodel {

    private static final Metamodel INSTANCE;

    static {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();
                
                // Collection methods return empty sets
                if (methodName.startsWith("get") && 
                    (methodName.contains("Types") || methodName.contains("Entities") || 
                     methodName.contains("Embeddables"))) {
                    return Collections.emptySet();
                }
                
                // entity(), managedType(), embeddable() return a stub ManagedType proxy
                if (methodName.equals("entity") || methodName.equals("managedType") || methodName.equals("embeddable")) {
                    return createStubManagedTypeProxy();
                }
                
                return null;
            }
        };
        
        INSTANCE = (Metamodel) Proxy.newProxyInstance(
            Metamodel.class.getClassLoader(),
            new Class<?>[] { Metamodel.class },
            handler
        );
    }

    private static Object createStubManagedTypeProxy() {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();
                
                // Return empty sets for all collection-returning methods
                if (method.getReturnType() == Set.class) {
                    return Collections.emptySet();
                }
                
                // Return null for all other methods
                return null;
            }
        };
        
        // Create a proxy that implements ManagedType, EntityType, EmbeddableType, and Type
        return Proxy.newProxyInstance(
            Metamodel.class.getClassLoader(),
            new Class<?>[] { ManagedType.class, EntityType.class, EmbeddableType.class, Type.class },
            handler
        );
    }

    private MansartMetamodel() {
        // Singleton - use getInstance()
    }

    public static Metamodel getInstance() {
        return INSTANCE;
    }

    @Override
    public EntityType<?> entity(String entityName) {
        return (EntityType<?>) INSTANCE.entity(entityName);
    }

    @Override
    public <X> EntityType<X> entity(Class<X> entityClass) {
        return (EntityType<X>) INSTANCE.entity(entityClass);
    }

    @Override
    public <X> ManagedType<X> managedType(Class<X> type) {
        return (ManagedType<X>) INSTANCE.managedType(type);
    }

    @Override
    public <X> EmbeddableType<X> embeddable(Class<X> type) {
        return (EmbeddableType<X>) INSTANCE.embeddable(type);
    }

    @Override
    public Set<ManagedType<?>> getManagedTypes() {
        return INSTANCE.getManagedTypes();
    }

    @Override
    public Set<EntityType<?>> getEntities() {
        return INSTANCE.getEntities();
    }

    @Override
    public Set<EmbeddableType<?>> getEmbeddables() {
        return INSTANCE.getEmbeddables();
    }
}
