/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import jakarta.persistence.spi.LoadState;
import jakarta.persistence.spi.ProviderUtil;

/**
 * {@link ProviderUtil} implementation for Mansart Jakarta Persistence.
 *
 * <p>Determines load state by checking whether an entity is registered in the
 * current persistence context. Since Mansart fully fetches all attributes on
 * load (no partial attribute-level lazy loading), any managed entity is
 * considered fully loaded.</p>
 *
 * <p>Load-state rules:</p>
 * <ul>
 *   <li>Entity is in the persistence context → {@link LoadState#LOADED}</li>
 *   <li>Entity is not in the persistence context → {@link LoadState#NOT_LOADED}</li>
 *   <li>{@code null} entity → {@link IllegalArgumentException}</li>
 * </ul>
 *
 * <p>The current persistence context is tracked via a {@code ThreadLocal} that
 * is bound when an {@code EntityManager} is created and unbound when it is
 * closed. This allows the singleton provider util to determine whether an
 * entity is managed without requiring the entity to carry a reference to its
 * factory.</p>
 */
enum MansartProviderUtil implements ProviderUtil {

    INSTANCE;

    /**
     * Thread-local holding the current persistence context.
     * Bound by {@code MansartEntityManagerFactory.createEntityManager()}.
     */
    static final ThreadLocal<PersistenceContext> CURRENT_CONTEXT = ThreadLocal.withInitial(PersistenceContext::new);

    /**
     * Resolve the current persistence context.
     *
     * @return the current persistence context (never null)
     */
    private PersistenceContext currentContext() {
        return CURRENT_CONTEXT.get();
    }

    /**
     * Clear the thread-local persistence context.
     */
    void clear() {
        CURRENT_CONTEXT.remove();
    }

    @Override
    public LoadState isLoaded(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity must not be null");
        }
        return currentContext().contains(entity)
                ? LoadState.LOADED
                : LoadState.NOT_LOADED;
    }

    @Override
    public LoadState isLoadedWithReference(Object entity, String attributeName) {
        if (entity == null) {
            throw new IllegalArgumentException("entity must not be null");
        }
        return currentContext().contains(entity)
                ? LoadState.LOADED
                : LoadState.NOT_LOADED;
    }

    @Override
    public LoadState isLoadedWithoutReference(Object entity, String attributeName) {
        if (entity == null) {
            throw new IllegalArgumentException("entity must not be null");
        }
        return currentContext().contains(entity)
                ? LoadState.LOADED
                : LoadState.NOT_LOADED;
    }
}
