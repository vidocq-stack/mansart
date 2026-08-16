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
                // Check return type for Expression-based types
                String returnTypeName = method.getReturnType().getName();
                if (returnTypeName.contains("Expression") ||
                    returnTypeName.contains("Predicate") ||
                    returnTypeName.contains("Order") ||
                    returnTypeName.contains("Selection") ||
                    returnTypeName.contains("From") ||
                    returnTypeName.contains("Join") ||
                    returnTypeName.contains("Root") ||
                    returnTypeName.contains("Path") ||
                    returnTypeName.contains("Parameter") ||
                    returnTypeName.contains("Coalesce") ||
                    returnTypeName.contains("SimpleCase") ||
                    returnTypeName.contains("Case") ||
                    returnTypeName.contains("Subquery")) {
                    return MansartExpression.getProxy();
                }
                
                // Specific methods that return CriteriaBuilder (for chaining)
                // Methods that return CriteriaQuery or TypedQuery
                if (methodName.startsWith("createQuery") || methodName.startsWith("createTupleQuery")) {
                    return MansartCriteriaQuery.getProxy();
                }
                if (methodName.startsWith("createCriteriaUpdate")) {
                    return MansartCriteriaUpdate.getProxy();
                }
                if (methodName.startsWith("createCriteriaDelete")) {
                    return MansartCriteriaDelete.getProxy();
                }
                
                // Methods that return Expression, Predicate, Order, Selection, etc.
                switch (methodName) {
                    case "literal":
                    case "coalesce":
                    case "nullif":
                    case "abs":
                    case "mod":
                    case "sqrt":
                    case "length":
                    case "locate":
                    case "substring":
                    case "trim":
                    case "lower":
                    case "upper":
                    case "concat":
                    case "parameter":
                    case "asc":
                    case "desc":
                    case "equal":
                    case "notEqual":
                    case "gt":
                    case "ge":
                    case "lt":
                    case "le":
                    case "isNull":
                    case "isNotNull":
                    case "and":
                    case "or":
                    case "not":
                    case "exists":
                    case "in":
                    case "between":
                    case "like":
                    case "isTrue":
                    case "isFalse":
                    case "isMember":
                    case "size":
                    case "isEmpty":
                    case "currentDate":
                    case "currentTime":
                    case "currentTimestamp":
                    case "diff":
                    case "sum":
                    case "avg":
                    case "max":
                    case "min":
                    case "count":
                    case "countDistinct":
                    case "function":
                    case "selectCase":
                    case "simpleCase":
                        return MansartExpression.getProxy();
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
