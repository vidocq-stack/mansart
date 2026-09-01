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
package io.vidocq.mansart.persistence.core;

import jakarta.persistence.EntityTransaction;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Resource-local {@link EntityTransaction} implementation that manages a
 * {@link java.sql.Connection} for the lifetime of the transaction.
 *
 * <p>State machine (mirrors JPA §1.3.1 and JTA §3.3.3) :
 * <pre>
 *   NO_TRANSACTION ──begin()──▶ ACTIVE
 *      │                              │
 *      │ commit() / rollback()        │ commit() / rollback()
 *      ▼                              ▼
 *   NO_TRANSACTION              NO_TRANSACTION
 *
 *   ACTIVE ──setRollbackOnly()──▶ ACTIVE (rollback-only)
 *      │                            │
 *      │ commit()                   │ commit()
 *      ▼                            ▼
 *   NO_TRANSACTION            RollbackException
 * </pre>
 */
final class MansartEntityTransaction implements EntityTransaction {

    private final MansartEntityManager entityManager;
    private final TransactionManager transactionManager;
    private final DataSource dataSource;
    private Connection connection;
    private volatile boolean began;
    private volatile boolean everStarted;

    MansartEntityTransaction(MansartEntityManager entityManager,
                             TransactionManager transactionManager,
                             DataSource dataSource) {
        this.entityManager = entityManager;
        this.transactionManager = transactionManager;
        this.dataSource = dataSource;
    }

    /**
     * Returns the transaction-bound connection, or {@code null} if no
     * transaction is active.  Callers that execute SQL should use this
     * when a transaction is in progress, falling back to a fresh
     * connection from the data source when outside a transaction.
     *
     * @return the connection, or {@code null}
     */
    Connection getConnection() {
        return connection;
    }

    @Override
    public void begin() {
        entityManager.checkClosed();
        int status;
        try {
            status = transactionManager.getStatus();
        } catch (SystemException e) {
            throw new jakarta.persistence.RollbackException(e);
        }
        if (status != Status.STATUS_NO_TRANSACTION) {
            throw new IllegalStateException(
                    "begin() called when a transaction is already active");
        }
        try {
            transactionManager.begin();
        } catch (NotSupportedException | SystemException e) {
            throw new IllegalStateException(e);
        }
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new jakarta.persistence.RollbackException(
                    "Failed to obtain connection for transaction", e);
        }
        began = true;
        everStarted = true;
    }

    @Override
    public void commit() {
        entityManager.checkClosed();
        if (!began) {
            throw new IllegalStateException(
                    "commit() called without an active transaction");
        }
        try {
            transactionManager.commit();
            if (connection != null) {
                connection.commit();
            }
        } catch (RollbackException | SystemException e) {
            closeConnection();
            throw new jakarta.persistence.RollbackException(e);
        } catch (HeuristicMixedException | HeuristicRollbackException e) {
            closeConnection();
            throw new IllegalStateException(e);
        } catch (SQLException e) {
            closeConnection();
            throw new jakarta.persistence.RollbackException(e);
        } finally {
            began = false;
        }
    }

    @Override
    public void rollback() {
        entityManager.checkClosed();
        if (!began) {
            // Transaction was never started, or already ended (commit/rollback).
            // Per JPA spec: if no transaction is active, do nothing.
            // We only throw IllegalStateException if the transaction was
            // never started (everStarted is false).  Once it was started
            // and ended, a subsequent rollback is a no-op.
            if (!everStarted) {
                throw new IllegalStateException(
                        "rollback() called without an active transaction");
            }
            return;
        }
        try {
            transactionManager.rollback();
            if (connection != null) {
                connection.rollback();
            }
        } catch (SystemException e) {
            throw new jakarta.persistence.RollbackException(e);
        } catch (SQLException e) {
            throw new jakarta.persistence.RollbackException(e);
        } finally {
            began = false;
            closeConnection();
        }
    }

    private void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
            connection = null;
        }
    }

    @Override
    public boolean isActive() {
        try {
            return transactionManager.getStatus() != Status.STATUS_NO_TRANSACTION;
        } catch (SystemException e) {
            throw new jakarta.persistence.RollbackException(e);
        }
    }

    @Override
    public void setRollbackOnly() {
        entityManager.checkClosed();
        if (!began) {
            throw new IllegalStateException(
                    "setRollbackOnly() called without an active transaction");
        }
        try {
            transactionManager.setRollbackOnly();
        } catch (SystemException e) {
            throw new jakarta.persistence.RollbackException(e);
        }
    }

    @Override
    public boolean getRollbackOnly() {
        try {
            int status = transactionManager.getStatus();
            return status == Status.STATUS_MARKED_ROLLBACK;
        } catch (SystemException e) {
            throw new jakarta.persistence.RollbackException(e);
        }
    }

    @Override
    public void setTimeout(Integer seconds) {
        throw new UnsupportedOperationException(
                "not implemented: EntityTransaction.setTimeout (resource-local)");
    }

    @Override
    public Integer getTimeout() {
        throw new UnsupportedOperationException(
                "not implemented: EntityTransaction.getTimeout (resource-local)");
    }
}
