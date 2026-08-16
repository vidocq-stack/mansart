/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.criteria;

import jakarta.persistence.criteria.CriteriaDelete;
import java.lang.reflect.Proxy;

/**
 * Factory for CriteriaDelete proxy stubs for JPA 3.2.
 * Phase 1: Returns proxy instances that throw UnsupportedOperationException for all methods.
 */
public class MansartCriteriaDelete<T> {

    @SuppressWarnings("unchecked")
    public static <T> CriteriaDelete<T> getProxy() {
        return (CriteriaDelete<T>) Proxy.newProxyInstance(
            CriteriaDelete.class.getClassLoader(),
            new Class<?>[] { CriteriaDelete.class },
            (p, m, a) -> {
                throw new UnsupportedOperationException("Criteria API Phase 1: Not yet implemented - " + m.getName());
            }
        );
    }
}
