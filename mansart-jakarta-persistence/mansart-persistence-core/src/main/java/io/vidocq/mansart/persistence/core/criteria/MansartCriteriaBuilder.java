/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.criteria;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Mansart implementation of CriteriaBuilder for JPA 3.2.
 * Phase 1: Minimal stub implementation using a dynamic proxy that throws
 * UnsupportedOperationException for all methods.
 * This allows getCriteriaBuilder() to return a non-null value and pass compilation.
 */
public class MansartCriteriaBuilder {

    private static final CriteriaBuilder INSTANCE;

    static {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();
                
                // Phase 1: Return non-null proxy stubs for essential methods
                switch (methodName) {
                    case "createQuery":
                        if (args == null || args.length == 0) {
                            return MansartCriteriaQuery.getProxy();
                        } else {
                            return MansartCriteriaQuery.getProxy();
                        }
                    case "createTupleQuery":
                        return MansartCriteriaQuery.getProxy();
                    case "createCriteriaUpdate":
                        return MansartCriteriaUpdate.getProxy();
                    case "createCriteriaDelete":
                        return MansartCriteriaDelete.getProxy();
                    case "literal":
                        return MansartExpression.getProxy();
                    case "parameter":
                        if (args.length == 1) {
                            return MansartParameterExpression.getProxy();
                        } else {
                            return MansartParameterExpression.getProxy();
                        }
                }
                
                // All other methods throw UnsupportedOperationException
                throw new UnsupportedOperationException(
                    "Criteria API Phase 1: Not yet implemented - " + methodName);
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
