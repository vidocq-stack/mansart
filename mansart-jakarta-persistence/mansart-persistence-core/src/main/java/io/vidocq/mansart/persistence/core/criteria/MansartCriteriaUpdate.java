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
 * Minimal CriteriaUpdate stub for JPA 3.2 TCK.
 * Returns null for all methods, never throws exceptions.
 */
public class MansartCriteriaUpdate<T> {

    @SuppressWarnings("unchecked")
    public static <T> CriteriaUpdate<T> getProxy() {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Class<?> returnType = method.getReturnType();
                if (returnType == CriteriaUpdate.class || returnType == AbstractQuery.class ||
                    returnType == CommonAbstractCriteria.class || returnType == Query.class) {
                    return proxy;
                }
                if (returnType == Set.class) return Collections.emptySet();
                if (returnType == Integer.class || returnType == int.class) return 0;
                if (returnType == Boolean.class || returnType == boolean.class) return false;
                return null;
            }
        };
        return (CriteriaUpdate<T>) Proxy.newProxyInstance(
            CriteriaUpdate.class.getClassLoader(),
            new Class<?>[] { CriteriaUpdate.class, Query.class },
            handler
        );
    }
}
