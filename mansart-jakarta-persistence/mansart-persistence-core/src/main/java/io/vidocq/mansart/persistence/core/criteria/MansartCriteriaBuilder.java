/*
 * Copyright (c) ${year} Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

package io.vidocq.mansart.persistence.core.criteria;

import jakarta.persistence.criteria.CriteriaBuilder;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Factory for creating a stub CriteriaBuilder implementation.
 * Uses dynamic proxy to implement all CriteriaBuilder methods.
 */
public final class MansartCriteriaBuilder {
    
    private MansartCriteriaBuilder() {}
    
    public static CriteriaBuilder create() {
        InvocationHandler handler = (proxy, method, args) -> {
            throw new UnsupportedOperationException(
                "CriteriaBuilder." + method.getName() + " not yet implemented (M5)");
        };
        return (CriteriaBuilder) Proxy.newProxyInstance(
            CriteriaBuilder.class.getClassLoader(),
            new Class<?>[] { CriteriaBuilder.class },
            handler
        );
    }
}
