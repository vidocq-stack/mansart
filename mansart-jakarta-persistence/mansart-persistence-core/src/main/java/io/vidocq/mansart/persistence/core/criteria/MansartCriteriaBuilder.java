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
                throw new UnsupportedOperationException(
                    "Criteria API Phase 1: Not yet implemented - " + method.getName());
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
