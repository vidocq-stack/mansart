/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.criteria;

import jakarta.persistence.criteria.*;
import jakarta.persistence.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * Minimal CriteriaQuery stub for JPA 3.2 TCK.
 * Returns null for all methods, never throws exceptions.
 */
public class MansartCriteriaQuery<T> {

    @SuppressWarnings("unchecked")
    public static <T> CriteriaQuery<T> getProxy() {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Class<?> returnType = method.getReturnType();
                if (returnType == CriteriaQuery.class || returnType == AbstractQuery.class ||
                    returnType == CriteriaSelect.class || returnType == Query.class) {
                    return proxy;
                }
                if (returnType == Set.class) return Collections.emptySet();
                if (returnType == List.class) return Collections.emptyList();
                if (returnType == Map.class) return Collections.emptyMap();
                if (returnType == Class.class) return Object.class;
                if (returnType == String.class) return "";
                if (returnType == Integer.class || returnType == int.class) return 0;
                if (returnType == Boolean.class || returnType == boolean.class) return false;
                return null;
            }
        };
        return (CriteriaQuery<T>) Proxy.newProxyInstance(
            CriteriaQuery.class.getClassLoader(),
            new Class<?>[] { CriteriaQuery.class, Query.class },
            handler
        );
    }
}
