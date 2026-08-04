/*
 * Copyright (c) ${year} Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

package io.vidocq.mansart.persistence.core;

import jakarta.persistence.EntityTransaction;
import jakarta.persistence.RollbackException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Mansart implementation of Jakarta Persistence EntityTransaction.
 * 
 * <p>Basic transaction management for MansartEntityManager.
 */
public class MansartEntityTransaction implements EntityTransaction {

    private final MansartEntityManager entityManager;
    private final DataSource dataSource;
    
    private enum TransactionState { INACTIVE, ACTIVE, COMMITTED, ROLLED_BACK }
    private TransactionState state = TransactionState.INACTIVE;
    private Connection transactionConnection;
    private boolean rollbackOnly = false;
    private Integer timeout = null;

    /**
     * Creates a new MansartEntityTransaction.
     *
     * @param entityManager the owning EntityManager
     */
    public MansartEntityTransaction(MansartEntityManager entityManager) {
        this.entityManager = entityManager;
        this.dataSource = entityManager.getDataSource();
    }

    @Override
    public void begin() {
        if (state != TransactionState.INACTIVE) {
            throw new IllegalStateException("Transaction is already " + state);
        }
        
        try {
            transactionConnection = dataSource.getConnection();
            transactionConnection.setAutoCommit(false);
            state = TransactionState.ACTIVE;
            rollbackOnly = false;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to begin transaction", e);
        }
    }

    @Override
    public void commit() {
        if (state != TransactionState.ACTIVE) {
            throw new IllegalStateException("Transaction is not active, current state: " + state);
        }
        
        if (rollbackOnly) {
            rollback();
            throw new RollbackException("Transaction marked for rollback only");
        }
        
        try {
            transactionConnection.commit();
            state = TransactionState.COMMITTED;
        } catch (SQLException e) {
            state = TransactionState.ROLLED_BACK;
            throw new RollbackException("Transaction commit failed", e);
        } finally {
            cleanupConnection();
            state = TransactionState.INACTIVE;
        }
    }

    @Override
    public void rollback() {
        if (state != TransactionState.ACTIVE && state != TransactionState.COMMITTED) {
            throw new IllegalStateException("Transaction is not active, current state: " + state);
        }
        
        try {
            if (state == TransactionState.ACTIVE) {
                transactionConnection.rollback();
                state = TransactionState.ROLLED_BACK;
            }
        } catch (SQLException e) {
            state = TransactionState.ROLLED_BACK;
            throw new IllegalStateException("Transaction rollback failed", e);
        } finally {
            cleanupConnection();
            state = TransactionState.INACTIVE;
        }
    }

    @Override
    public boolean isActive() {
        return state == TransactionState.ACTIVE;
    }

    @Override
    public Integer getTimeout() {
        return timeout;
    }

    @Override
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    @Override
    public boolean getRollbackOnly() {
        return rollbackOnly;
    }

    @Override
    public void setRollbackOnly() {
        this.rollbackOnly = true;
    }



    /**
     * Cleans up the connection after transaction completion.
     */
    private void cleanupConnection() {
        if (transactionConnection != null) {
            try {
                transactionConnection.setAutoCommit(true);
            } catch (SQLException ignored) {
                // Ignore
            }
            try {
                transactionConnection.close();
            } catch (SQLException ignored) {
                // Ignore
            }
            transactionConnection = null;
        }
    }
}