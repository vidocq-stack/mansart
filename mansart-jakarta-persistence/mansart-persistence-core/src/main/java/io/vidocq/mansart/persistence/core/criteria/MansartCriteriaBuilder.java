/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.criteria;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.Metamodel;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Mansart implementation of CriteriaBuilder for JPA 3.2.
 * Phase 2: Returns proxy instances that never throw exceptions.
 */
public class MansartCriteriaBuilder {

    private static final CriteriaBuilder INSTANCE;

    static {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();
                Class<?> returnType = method.getReturnType();
                
                // Methods returning CriteriaQuery/TypedQuery
                if (methodName.startsWith("createQuery") || methodName.startsWith("createTupleQuery")) {
                    return MansartCriteriaQuery.getProxy();
                }
                
                // Methods returning CriteriaUpdate
                if (methodName.startsWith("createCriteriaUpdate")) {
                    return MansartCriteriaUpdate.getProxy();
                }
                
                // Methods returning CriteriaDelete
                if (methodName.startsWith("createCriteriaDelete")) {
                    return MansartCriteriaDelete.getProxy();
                }
                
                // Methods that should return the builder itself
                if (returnType == CriteriaBuilder.class) {
                    return proxy;
                }
                
                // Methods returning Collection types
                if (returnType == Set.class || returnType == java.util.List.class ||
                    returnType == Map.class || returnType == java.util.Collection.class) {
                    return Collections.emptySet();
                }
                
                // Methods returning primitive/wrapper types
                if (returnType == String.class) return "";
                if (returnType == Integer.class || returnType == int.class) return 0;
                if (returnType == Long.class || returnType == long.class) return 0L;
                if (returnType == Boolean.class || returnType == boolean.class) return false;
                if (returnType == Class.class) return Object.class;
                
                // Methods returning Metamodel
                if (returnType.getName().equals("jakarta.persistence.metamodel.Metamodel")) {
                    return null;
                }
                
                // For all other Criteria API types, return a generic proxy
                if (isCriteriaType(returnType)) {
                    return createGenericCriteriaProxy(returnType);
                }
                
                // Last resort: return null
                return null;
            }
            
            private boolean isCriteriaType(Class<?> type) {
                String name = type.getName();
                return name.startsWith("jakarta.persistence.criteria.");
            }
            
            private Object createGenericCriteriaProxy(Class<?> type) {
                InvocationHandler genericHandler = new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String methodName = method.getName();
                        Class<?> returnType = method.getReturnType();
                        
                        // Return safe defaults for getter methods
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
                        }
                        
                        // For chainable methods, return self if same type
                        if (returnType == type || returnType == Object.class) {
                            return proxy;
                        }
                        
                        // For methods returning other Criteria types, create a new proxy
                        if (isCriteriaType(returnType)) {
                            return createGenericCriteriaProxy(returnType);
                        }
                        
                        // For collection types
                        if (returnType == Set.class || returnType == java.util.List.class ||
                            returnType == Map.class) {
                            return Collections.emptySet();
                        }
                        
                        return null;
                    }
                };
                
                return Proxy.newProxyInstance(
                    type.getClassLoader(),
                    new Class<?>[] { type },
                    genericHandler
                );
            }
        };
        INSTANCE = (CriteriaBuilder) Proxy.newProxyInstance(
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
        return INSTANCE;
    }
}
