/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.criteria;

import jakarta.persistence.criteria.CriteriaUpdate;
import java.lang.reflect.Proxy;

/**
 * Factory for CriteriaUpdate proxy stubs for JPA 3.2.
 * Phase 1: Returns proxy instances that throw UnsupportedOperationException for all methods.
 */
public class MansartCriteriaUpdate<T> {

    @SuppressWarnings("unchecked")
    public static <T> CriteriaUpdate<T> getProxy() {
        return (CriteriaUpdate<T>) Proxy.newProxyInstance(
            CriteriaUpdate.class.getClassLoader(),
            new Class<?>[] { CriteriaUpdate.class },
            (p, m, a) -> {
                throw new UnsupportedOperationException("Criteria API Phase 1: Not yet implemented - " + m.getName());
            }
        );
    }
}
