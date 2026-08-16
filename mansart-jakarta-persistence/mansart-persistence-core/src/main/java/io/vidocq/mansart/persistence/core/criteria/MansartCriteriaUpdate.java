/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.criteria;

import io.vidocq.mansart.persistence.core.runtime.MansartParameter;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * Factory for CriteriaUpdate proxy stubs for JPA 3.2.
 * Phase 1: Returns proxy instances with smart defaults for common TCK methods.
 */
public class MansartCriteriaUpdate<T> {

    @SuppressWarnings("unchecked")
    public static <T> CriteriaUpdate<T> getProxy() {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();
                
                switch (methodName) {
                    case "executeUpdate": return 0;
                    case "getParameters": return Collections.emptySet();
                    case "subquery": return MansartCriteriaQuery.getProxy();
                    case "getRestriction": return null;
                    case "getParameter": 
                        if (args != null && args.length > 0) {
                            if (args[0] instanceof String name) {
                                return new MansartParameter<>(name, String.class);
                            } else if (args[0] instanceof Integer pos) {
                                return new MansartParameter<>((Integer) args[0], String.class);
                            }
                        }
                        return new MansartParameter<>("stub", String.class);
                    case "getParameterValue": return null;
                    case "isBound": return false;
                    case "getFirstResult": return 0;
                    case "getMaxResults": return Integer.MAX_VALUE;
                    case "getHints": return Collections.emptyMap();
                    case "getFlushMode": return FlushModeType.AUTO;
                    case "getLockMode": return LockModeType.NONE;
                }
                
                // Chainable methods: return self
                Class<?> returnType = method.getReturnType();
                if (returnType == CriteriaUpdate.class || returnType == AbstractQuery.class ||
                    returnType == CommonAbstractCriteria.class || returnType == Query.class) {
                    return proxy;
                }
                
                throw new UnsupportedOperationException("Criteria API Phase 1: " + methodName);
            }
        };
        return (CriteriaUpdate<T>) Proxy.newProxyInstance(
            CriteriaUpdate.class.getClassLoader(),
            new Class<?>[] { CriteriaUpdate.class, Query.class },
            handler
        );
    }
}
