/*
 * /*
 *  * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License 2.0 which is available at
 *  * https://www.eclipse.org/legal/epl-2.0/
 *  *
 *  * This Source Code may also be made available under the following Secondary
 *  * Licenses when the conditions for such availability set forth in the Eclipse
 *  * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 *  * or any later version, which is available at
 *  * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *  *
 *  * It is also made available under the European Union Public Licence v. 1.2,
 *  * which is available at
 *  * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-1.2
 *  *
 *  * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 *  */
 */
package io.vidocq.mansart.data.core;

import java.util.function.Supplier;

/**
 * A virtual thread-safe lazy initialization holder using ScopedValue.
 * Used for lazy loading of entity associations ({@code @ManyToOne}, {@code @OneToOne}).
 *
 * <p>M4 — Lazy loading for Jakarta Persistence associations.
 * The holder stores a foreign key value and a supplier function to load the full entity.
 * When {@link #get()} is called, the supplier is invoked (at most once) and the result
 * is cached using ScopedValue for virtual thread safety.
 */
public final class LazyHolder<T> {

    private final ScopedValue<T> value = ScopedValue.newInstance();
    private final Supplier<T> loader;
    private volatile boolean loaded = false;
    private T cachedValue;

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
     * Uses ScopedValue for virtual thread-safe lazy initialization.
     *
     * @return the value (may be null if the loader returns null)
     */
    public T get() {
        T v = value.get();
        if (v != null) {
            return v;
        }
        
        // Double-check the cached value to avoid unnecessary ScopedValue operations
        if (loaded) {
            return cachedValue;
        }
        
        // Initialize the value using ScopedValue for virtual thread safety
        ScopedValue.where(value, loader.get()).call(() -> null);
        cachedValue = value.get();
        loaded = true;
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
        loaded = false;
        cachedValue = null;
    }

    /**
     * Sets the value directly, bypassing the loader.
     * Useful for testing or for setting a pre-loaded value.
     *
     * @param value the value to set
     */
    public void set(T value) {
        this.cachedValue = value;
        this.loaded = true;
        ScopedValue.where(this.value, value).call(() -> null);
    }
}