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
package io.vidocq.mansart.persistence.core.runtime;

import jakarta.persistence.EntityTransaction;
import jakarta.persistence.RollbackException;

/**
 * Stub implementation of {@link EntityTransaction}.
 *
 * <p>Milestone: M7-6 - Minimal stub that compiles and can be extended later.
 */
public class MansartEntityTransaction implements EntityTransaction {

    private final MansartEntityManager entityManager;
    private boolean markForRollback;

    /**
     * Creates a new {@code MansartEntityTransaction} instance.
     *
     * @param entityManager the entity manager
     */
    public MansartEntityTransaction(MansartEntityManager entityManager) {
        this.entityManager = entityManager;
        this.markForRollback = false;
    }

    @Override
    public void begin() {
    }

    @Override
    public void commit() throws RollbackException {
    }

    @Override
    public void rollback() throws IllegalStateException {
        markForRollback = true;
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException {
        markForRollback = true;
    }

    @Override
    public boolean getRollbackOnly() {
        return markForRollback;
    }

    @Override
    public boolean isActive() {
        return !markForRollback;
    }

    @Override
    public Integer getTimeout() {
        return 0;
    }

    @Override
    public void setTimeout(Integer timeout) {
    }
}
