/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse License 2.0 which is available at
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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.runtime;

import io.vidocq.mansart.transactions.core.MansartTransactionManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.RollbackException;

/**
 * JPA EntityTransaction implementation that delegates to Mansart TransactionManager.
 *
 * <p>This implementation bridges JPA's EntityTransaction interface with Jakarta Transactions.
 * When a JTA transaction manager is available, this transaction participates in the
 * global JTA transaction. Otherwise, it manages its own resource-local transaction.
 *
 * <p>Milestone: M7-15 - Transaction Management integration with mansart-transactions.
 */
public class MansartEntityTransaction implements EntityTransaction {

    private final MansartTransactionManager transactionManager;
    private final MansartEntityManager entityManager;

    // Track whether this transaction has been started locally (for resource-local mode)
    private boolean locallyStarted;
    private boolean rollbackOnly;
    private Integer timeout;

    /**
     * Creates a new {@code MansartEntityTransaction} instance.
     *
     * @param entityManager the entity manager
     * @param transactionManager the transaction manager to delegate to
     */
    public MansartEntityTransaction(MansartEntityManager entityManager, 
                                     MansartTransactionManager transactionManager) {
        this.entityManager = entityManager;
        this.transactionManager = transactionManager;
        this.locallyStarted = false;
        this.rollbackOnly = false;
        this.timeout = null;
    }

    /**
     * Creates a new {@code MansartEntityTransaction} instance with no transaction manager.
     * This is for backwards compatibility and will be removed once full integration is complete.
     *
     * @param entityManager the entity manager
     */
    public MansartEntityTransaction(MansartEntityManager entityManager) {
        this(entityManager, null);
    }

    @Override
    public void begin() {
        if (transactionManager != null) {
            try {
                // Delegate to JTA transaction manager
                transactionManager.begin();
                locallyStarted = false; // Transaction managed by JTA
            } catch (jakarta.transaction.NotSupportedException e) {
                throw new IllegalStateException("Nested transactions are not supported", e);
            }
        } else {
            // Resource-local mode - mark as started
            locallyStarted = true;
            rollbackOnly = false;
        }
    }

    @Override
    public void commit() throws RollbackException {
        if (transactionManager != null) {
            if (transactionManager.getStatus() == jakarta.transaction.Status.STATUS_ROLLEDBACK) {
                throw new RollbackException("Transaction has been rolled back");
            }
            if (transactionManager.getStatus() == jakarta.transaction.Status.STATUS_MARKED_ROLLBACK) {
                throw new RollbackException("Transaction is marked for rollback");
            }
            try {
                transactionManager.commit();
            } catch (jakarta.transaction.HeuristicMixedException | 
                     jakarta.transaction.HeuristicRollbackException e) {
                throw new RollbackException("Failed to commit transaction: " + e.getMessage());
            } catch (SecurityException | jakarta.transaction.SystemException e) {
                throw new IllegalStateException("Failed to commit transaction", e);
            } catch (jakarta.transaction.RollbackException e) {
                throw new RollbackException("Transaction was rolled back: " + e.getMessage());
            }
        } else {
            // Resource-local mode - commit
            if (rollbackOnly) {
                throw new RollbackException("Transaction is marked for rollback");
            }
            locallyStarted = false;
        }
    }

    @Override
    public void rollback() throws IllegalStateException {
        if (transactionManager != null) {
            try {
                transactionManager.rollback();
            } catch (SecurityException | jakarta.transaction.SystemException e) {
                throw new IllegalStateException("Failed to rollback transaction", e);
            }
        } else {
            // Resource-local mode - rollback
            rollbackOnly = true;
            locallyStarted = false;
        }
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException {
        if (transactionManager != null) {
            try {
                transactionManager.setRollbackOnly();
            } catch (jakarta.transaction.SystemException e) {
                throw new IllegalStateException("Failed to set rollback only", e);
            }
        } else {
            // Resource-local mode
            rollbackOnly = true;
        }
    }

    @Override
    public boolean getRollbackOnly() {
        if (transactionManager != null) {
            return transactionManager.getStatus() == jakarta.transaction.Status.STATUS_MARKED_ROLLBACK;
        } else {
            // Resource-local mode
            return rollbackOnly;
        }
    }

    @Override
    public boolean isActive() {
        if (transactionManager != null) {
            int status = transactionManager.getStatus();
            return status == jakarta.transaction.Status.STATUS_ACTIVE ||
                   status == jakarta.transaction.Status.STATUS_MARKED_ROLLBACK;
        } else {
            // Resource-local mode
            return locallyStarted && !rollbackOnly;
        }
    }

    @Override
    public Integer getTimeout() {
        // Note: jakarta.transaction.Transaction does not have a getTimeout() method
        // The timeout is set at the TransactionManager level, not per-transaction
        if (transactionManager != null) {
            // For JTA, the timeout is managed by the TransactionManager
            // We could return the default timeout, but it's not part of the Transaction interface
            return null; // Not supported via Transaction interface
        } else {
            // Resource-local mode
            return timeout;
        }
    }

    @Override
    public void setTimeout(Integer timeout) {
        if (transactionManager != null) {
            try {
                transactionManager.setTransactionTimeout(timeout != null ? timeout : 0);
            } catch (jakarta.transaction.SystemException e) {
                throw new IllegalStateException("Failed to set transaction timeout", e);
            }
        } else {
            // Resource-local mode
            this.timeout = timeout;
        }
    }

    /**
     * Returns the underlying transaction manager.
     *
     * @return the transaction manager, or null if in resource-local mode
     */
    MansartTransactionManager getTransactionManager() {
        return transactionManager;
    }
}
