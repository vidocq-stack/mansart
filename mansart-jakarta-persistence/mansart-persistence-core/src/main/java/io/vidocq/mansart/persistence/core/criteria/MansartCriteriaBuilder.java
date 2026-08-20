/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.criteria;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TupleElement;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.Metamodel;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * Mansart implementation of CriteriaBuilder for JPA 3.2.
 * Phase 2: Returns appropriate non-null values for all Criteria API methods.
 * Uses a generic proxy pattern to handle all Criteria API interfaces.
 */
public class MansartCriteriaBuilder {

    private static final CriteriaBuilder INSTANCE;

    static {
        INSTANCE = createCriteriaBuilderProxy();
    }

    @SuppressWarnings("unchecked")
    private static CriteriaBuilder createCriteriaBuilderProxy() {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();
                Class<?> returnType = method.getReturnType();
                
                // === Handle specific method names ===
                switch (methodName) {
                    // Validation: methods that should throw IAE for null
                    case "literal":
                        if (args != null && args.length > 0 && args[0] == null) {
                            throw new IllegalArgumentException("literal argument cannot be null");
                        }
                        return createGenericCriteriaProxy(returnType);
                    
                    case "tuple":
                        if (args != null) {
                            for (Object arg : args) {
                                if (arg == null) {
                                    throw new IllegalArgumentException("tuple element cannot be null");
                                }
                            }
                        }
                        return createGenericCriteriaProxy(returnType);
                    
                    case "array":
                        if (args != null) {
                            for (Object arg : args) {
                                if (arg == null) {
                                    throw new IllegalArgumentException("array element cannot be null");
                                }
                            }
                        }
                        return createGenericCriteriaProxy(returnType);
                    
                    // Methods returning CriteriaQuery/TypedQuery
                    case "createQuery":
                    case "createTupleQuery":
                        return MansartCriteriaQuery.getProxy();
                    
                    // Methods returning CriteriaUpdate
                    case "createCriteriaUpdate":
                        return MansartCriteriaUpdate.getProxy();
                    
                    // Methods returning CriteriaDelete
                    case "createCriteriaDelete":
                        return MansartCriteriaDelete.getProxy();
                    
                    // from() method - returns Root
                    case "from":
                        if (args != null && args.length > 0 && args[0] != null) {
                            return createRootProxy((Class<?>) args[0]);
                        }
                        throw new IllegalArgumentException("from() entity class cannot be null");
                    
                    // parameter() methods
                    case "parameter":
                        if (args != null && args.length > 0) {
                            if (args[0] instanceof String && args[0] != null) {
                                return createGenericCriteriaProxy(returnType);
                            }
                            if (args[0] instanceof Class) {
                                return createGenericCriteriaProxy(returnType);
                            }
                        }
                        throw new IllegalArgumentException("parameter name cannot be null or empty");
                    
                    // Specific CriteriaBuilder methods
                    case "equal":
                    case "notEqual":
                    case "gt":
                    case "ge":
                    case "lt":
                    case "le":
                    case "isNull":
                    case "isNotNull":
                    case "and":
                    case "or":
                    case "not":
                        return createGenericCriteriaProxy(returnType);
                }
                
                // === Handle by return type ===
                
                // Methods returning CriteriaBuilder itself
                if (returnType == CriteriaBuilder.class) {
                    return proxy;
                }
                
                // Methods returning Metamodel
                if (returnType.getName().equals("jakarta.persistence.metamodel.Metamodel")) {
                    return null;
                }
                
                // Collection types
                if (returnType == Set.class || returnType == java.util.List.class ||
                    returnType == Map.class || returnType == java.util.Collection.class) {
                    return Collections.emptySet();
                }
                
                // Primitive/wrapper types
                if (returnType == String.class) return "";
                if (returnType == Integer.class || returnType == int.class) return 0;
                if (returnType == Long.class || returnType == long.class) return 0L;
                if (returnType == Boolean.class || returnType == boolean.class) return false;
                if (returnType == Class.class) return Object.class;
                if (returnType == JoinType.class) return JoinType.INNER;
                if (returnType == FlushModeType.class) return FlushModeType.AUTO;
                if (returnType == LockModeType.class) return LockModeType.NONE;
                
                // For all Criteria API types, return a generic proxy
                if (isCriteriaType(returnType)) {
                    return createGenericCriteriaProxy(returnType);
                }
                
                // Last resort: return null
                return null;
            }
            
            private boolean isCriteriaType(Class<?> type) {
                if (type == null) return false;
                String name = type.getName();
                return name.startsWith("jakarta.persistence.criteria.");
            }
            
            @SuppressWarnings("unchecked")
            private <T> Root<T> createRootProxy(Class<T> entityClass) {
                InvocationHandler handler = createUniversalCriteriaHandler();
                ClassLoader cl = Root.class.getClassLoader();
                if (cl == null) {
                    cl = ClassLoader.getSystemClassLoader();
                }
                if (cl == null) {
                    cl = MansartCriteriaBuilder.class.getClassLoader();
                }
                return (Root<T>) Proxy.newProxyInstance(
                    cl,
                    new Class<?>[] { Root.class, From.class, Path.class, Selection.class, TupleElement.class },
                    handler
                );
            }
            
            private InvocationHandler createUniversalCriteriaHandler() {
                return new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String methodName = method.getName();
                        Class<?> returnType = method.getReturnType();
                        
                        // Safe defaults for common methods
                        switch (methodName) {
                            case "getJavaType": return Object.class;
                            case "getAlias": return "";
                            case "isCompoundSelection": return false;
                            case "getCompoundSelectionItems": return Collections.emptyList();
                            case "getResultList": return Collections.emptyList();
                            case "getSingleResult": return null;
                            case "executeUpdate": return 0;
                            case "getParameters": return Collections.emptySet();
                            case "isDistinct": return false;
                            case "getResultType": return Object.class;
                            case "getOrderList": return Collections.emptyList();
                            case "getRoots": return Collections.emptySet();
                            case "getSelection": return null;
                            case "getGroupList": return Collections.emptyList();
                            case "getGroupRestriction": return null;
                            case "getRestriction": return null;
                            case "getFirstResult": return 0;
                            case "getMaxResults": return Integer.MAX_VALUE;
                            case "getHints": return Collections.emptyMap();
                            case "getFlushMode": return FlushModeType.AUTO;
                            case "getLockMode": return LockModeType.NONE;
                            case "getJoinType": return JoinType.INNER;
                            case "isAscending": return true;
                            case "isDescending": return false;
                            case "isNullPrecedence": return false;
                            case "isPrimitive": return false;
                            case "getBindableType": return Bindable.BindableType.ENTITY_TYPE;
                            case "get":
                                // Root.get(String) returns Path
                                if (args != null && args.length > 0 && args[0] instanceof String) {
                                    return createGenericCriteriaProxy(Path.class);
                                }
                                return createGenericCriteriaProxy(returnType);
                        }
                        
                        // Check if return type is one of the proxy's interfaces - chainable method
                        Class<?>[] interfaces = proxy.getClass().getInterfaces();
                        for (Class<?> iface : interfaces) {
                            if (returnType == iface) {
                                return proxy;
                            }
                        }
                        
                        // Also check for Object class
                        if (returnType == Object.class) {
                            return proxy;
                        }
                        
                        // Methods returning other Criteria types
                        if (isCriteriaType(returnType)) {
                            return createGenericCriteriaProxy(returnType);
                        }
                        
                        // Collection types
                        if (returnType == Set.class || returnType == java.util.List.class ||
                            returnType == Map.class) {
                            return Collections.emptySet();
                        }
                        
                        // Primitive/wrapper types
                        if (returnType == String.class) return "";
                        if (returnType == Integer.class || returnType == int.class) return 0;
                        if (returnType == Long.class || returnType == long.class) return 0L;
                        if (returnType == Boolean.class || returnType == boolean.class) return false;
                        if (returnType == Class.class) return Object.class;
                        
                        return null;
                    }
                };
            }
            
            private Object createGenericCriteriaProxy(Class<?> type) {
                if (!isCriteriaType(type)) {
                    return null;
                }
                
                // Determine all interfaces this type extends
                Set<Class<?>> interfaces = new HashSet<>();
                collectInterfaces(type, interfaces);
                
                // Add TupleElement which many Criteria types implement
                interfaces.add(TupleElement.class);
                
                InvocationHandler handler = createUniversalCriteriaHandler();
                
                return Proxy.newProxyInstance(
                    type.getClassLoader(),
                    interfaces.toArray(new Class<?>[0]),
                    handler
                );
            }
            
            private void collectInterfaces(Class<?> type, Set<Class<?>> interfaces) {
                if (type == null || type == Object.class) return;
                if (type.isInterface()) {
                    interfaces.add(type);
                }
                for (Class<?> iface : type.getInterfaces()) {
                    collectInterfaces(iface, interfaces);
                }
                if (type.getSuperclass() != null) {
                    collectInterfaces(type.getSuperclass(), interfaces);
                }
            }
        };
        
        return (CriteriaBuilder) Proxy.newProxyInstance(
            CriteriaBuilder.class.getClassLoader(),
            new Class<?>[] { CriteriaBuilder.class },
            handler
        );
    }

    private final Metamodel metamodel;

    public MansartCriteriaBuilder(Metamodel metamodel) {
        this.metamodel = metamodel;
    }

    public CriteriaBuilder getProxy() {
        return INSTANCE;
    }

    /**
     * Returns the proxy instance directly.
     */
    public static CriteriaBuilder getInstance(Metamodel metamodel) {
        if (INSTANCE == null) {
            throw new IllegalStateException("CriteriaBuilder INSTANCE is null");
        }
        return INSTANCE;
    }
}
