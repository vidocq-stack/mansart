/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * or any later version, which is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * It is also made available under the European Union Public Licence v. 1.2,
 * which is available at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.spi;

/**
 * Compile-time-generated dispatcher for JPA lifecycle callbacks.
 *
 * <p>Each entity class gets a generated {@code _EntityCallbacks} implementation that
 * directly invokes the {@code @PrePersist}, {@code @PostLoad}, … methods — no
 * reflection, no {@code MethodHandles} scanning at runtime.
 *
 * <p>Implemented by the Mansart APT processor (DEBT-06).
 */
public interface LifecycleCallbackDispatcher {

    /**
     * Lifecycle callback phase.
     */
    enum Phase {
        PRE_PERSIST,
        POST_PERSIST,
        PRE_UPDATE,
        POST_UPDATE,
        PRE_REMOVE,
        POST_REMOVE,
        POST_LOAD
    }

    /**
     * Invokes all lifecycle callbacks for the given phase and entity instance.
     *
     * @param entity the entity instance
     * @param phase the callback phase
     * @throws RuntimeException if a callback throws
     */
    void invoke(Object entity, Phase phase);
}
