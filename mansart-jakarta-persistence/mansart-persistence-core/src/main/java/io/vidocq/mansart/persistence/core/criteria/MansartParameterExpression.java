/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.criteria;

import jakarta.persistence.Parameter;
import jakarta.persistence.criteria.ParameterExpression;
import java.lang.reflect.Proxy;

/**
 * Factory for ParameterExpression proxy stubs for JPA 3.2.
 * Phase 1: Returns proxy instances that throw UnsupportedOperationException for all methods.
 */
public class MansartParameterExpression<T> {

    @SuppressWarnings("unchecked")
    public static <T> ParameterExpression<T> getProxy() {
        return (ParameterExpression<T>) Proxy.newProxyInstance(
            ParameterExpression.class.getClassLoader(),
            new Class<?>[] { ParameterExpression.class, Parameter.class },
            (p, m, a) -> {
                throw new UnsupportedOperationException("Criteria API Phase 1: Not yet implemented - " + m.getName());
            }
        );
    }
}
