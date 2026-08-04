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

package io.vidocq.mansart.data.core;

import java.util.function.Supplier;

/**
 * A thread-safe lazy initialization holder using double-checked locking.
 * Used for lazy loading of entity associations ({@code @ManyToOne}, {@code @OneToOne}).
 *
 * <p>M4 — Lazy loading for Jakarta Persistence associations.
 * The holder stores a foreign key value and a supplier function to load the full entity.
 * When {@link #get()} is called, the supplier is invoked (at most once) and the result
 * is cached for future access.
 */
public final class LazyHolder<T> {

    private final Supplier<T> loader;
    private volatile T cachedValue;
    private volatile boolean loaded = false;
    private final Object lock = new Object();

    /**
     * Creates a new LazyHolder with the given loader.
     *
     * @param loader the supplier function that loads the value when needed
     */
    public LazyHolder(Supplier<T> loader) {
        this.loader = loader;
    }

    /**
     * Returns the value, initializing it lazily if necessary.
     * Uses double-checked locking for thread-safe lazy initialization.
     *
     * @return the value (may be null if the loader returns null)
     */
    public T get() {
        // First check without synchronization for performance
        if (loaded) {
            return cachedValue;
        }
        
        // Synchronize and double-check
        synchronized (lock) {
            if (!loaded) {
                cachedValue = loader.get();
                loaded = true;
            }
        }
        return cachedValue;
    }

    /**
     * Returns whether the value has been loaded.
     *
     * @return true if the value has been loaded, false otherwise
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Resets the lazy holder, clearing any cached value.
     * The value will be reloaded on the next call to {@link #get()}.
     */
    public void reset() {
        synchronized (lock) {
            loaded = false;
            cachedValue = null;
        }
    }

    /**
     * Sets the value directly, bypassing the loader.
     * Useful for testing or for setting a pre-loaded value.
     *
     * @param value the value to set
     */
    public void set(T value) {
        synchronized (lock) {
            this.cachedValue = value;
            this.loaded = true;
        }
    }
}