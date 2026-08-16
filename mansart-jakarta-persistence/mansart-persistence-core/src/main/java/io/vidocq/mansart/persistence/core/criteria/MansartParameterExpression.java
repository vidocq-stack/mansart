/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.criteria;

import jakarta.persistence.Parameter;
import jakarta.persistence.criteria.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * Factory for ParameterExpression proxy stubs for JPA 3.2.
 * Phase 1: Returns proxy instances with smart defaults for common TCK methods.
 */
public class MansartParameterExpression<T> {

    @SuppressWarnings("unchecked")
    public static <T> ParameterExpression<T> getProxy() {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();
                
                switch (methodName) {
                    case "getName": return "stub_param";
                    case "getPosition": return 1;
                    case "getParameterType": return Object.class;
                    case "getJavaType": return Object.class;
                    case "getAlias": return null;
                    case "isCompoundSelection": return false;
                    case "getCompoundSelectionItems": return Collections.emptyList();
                }
                
                // Chainable methods returning Expression: return expression proxy
                Class<?> returnType = method.getReturnType();
                if (returnType == Expression.class) {
                    return MansartExpression.getProxy();
                }
                if (returnType == Predicate.class) {
                    return MansartPredicate.getProxy();
                }
                if (returnType == ParameterExpression.class || returnType == Parameter.class ||
                    returnType == Selection.class) {
                    return proxy;
                }
                
                throw new UnsupportedOperationException("Criteria API Phase 1: " + methodName);
            }
        };
        return (ParameterExpression<T>) Proxy.newProxyInstance(
            ParameterExpression.class.getClassLoader(),
            new Class<?>[] { ParameterExpression.class, Parameter.class, Expression.class, 
                            Selection.class, jakarta.persistence.TupleElement.class },
            handler
        );
    }
}
