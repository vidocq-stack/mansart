/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.criteria;

import jakarta.persistence.criteria.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * Factory for Predicate proxy stubs for JPA 3.2.
 * Phase 1: Returns proxy instances with smart defaults for common TCK methods.
 */
public class MansartPredicate {

    @SuppressWarnings("unchecked")
    public static Predicate getProxy() {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();
                
                switch (methodName) {
                    case "getJavaType": return Boolean.class;
                    case "getAlias": return null;
                    case "isCompoundSelection": return false;
                    case "getCompoundSelectionItems": return Collections.emptyList();
                    case "not": return proxy;
                    case "asExpression": return MansartExpression.getProxy();
                    case "getOperator": return null;
                    case "getExpression": return null;
                    case "getExpressions": return Collections.emptyList();
                }
                
                // Chainable methods: return self
                Class<?> returnType = method.getReturnType();
                if (returnType == Predicate.class || returnType == Expression.class ||
                    returnType == Selection.class) {
                    if (returnType == Expression.class) {
                        return MansartExpression.getProxy();
                    }
                    return proxy;
                }
                
                throw new UnsupportedOperationException("Criteria API Phase 1: " + methodName);
            }
        };
        return (Predicate) Proxy.newProxyInstance(
            Predicate.class.getClassLoader(),
            new Class<?>[] { Predicate.class, Expression.class, Selection.class, jakarta.persistence.TupleElement.class },
            handler
        );
    }
}
