package io.vidocq.mansart.persistence.core;

import jakarta.persistence.EntityTransaction;
import jakarta.persistence.RollbackException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Resource-local EntityTransaction implementation for RESOURCE_LOCAL persistence units.
 * Tracks transaction state in-memory (flush is a no-op until M5).
 */
public final class MansartEntityTransaction implements EntityTransaction {

    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicBoolean rollbackOnly = new AtomicBoolean(false);

    @Override
    public void begin() {
        if (active.get()) {
            throw new IllegalStateException("transaction already active");
        }
        active.set(true);
        rollbackOnly.set(false);
    }

    @Override
    public void commit() {
        if (!active.get()) {
            throw new IllegalStateException("transaction not active");
        }
        if (rollbackOnly.get()) {
            active.set(false);
            throw new RollbackException();
        }
        active.set(false);
        // flush is a no-op until M5
    }

    @Override
    public void rollback() {
        if (!active.get()) {
            throw new IllegalStateException("transaction not active");
        }
        active.set(false);
        rollbackOnly.set(false);
    }

    @Override
    public void setRollbackOnly() {
        if (!active.get()) {
            throw new IllegalStateException("transaction not active");
        }
        rollbackOnly.set(true);
    }

    @Override
    public boolean getRollbackOnly() {
        if (!active.get()) {
            throw new IllegalStateException("transaction not active");
        }
        return rollbackOnly.get();
    }

    @Override
    public boolean isActive() {
        return active.get();
    }

    @Override
    public Integer getTimeout() {
        // Not implemented: returns 0 as per Jakarta Persistence spec default
        return 0;
    }

    @Override
    public void setTimeout(Integer timeout) {
        // Not implemented: no-op per Jakarta Persistence spec
        // This method is a no-op in the reference implementation
    }
}
