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
package io.vidocq.mansart.persistence.processor.enhancer;

/**
 * Utility class for lazy loading support.
 * <p>
 * This class provides a simple lazy loading mechanism that works with
 * virtual threads and is compatible with GraalVM native image.
 * <p>
 * The lazy loading uses a simple flag-based approach where associations
 * are loaded on first access and cached in a volatile field.
 */
public final class LazyLoadingUtils {

    private LazyLoadingUtils() {
        // Utility class
    }

    /**
     * Lazy holder for association values.
     * This is a simple implementation that can be replaced with
     * ScopedValue when JDK 22+ is available.
     */
    public static class LazyHolder<T> {
        private volatile T value;
        private volatile boolean initialized = false;
        
        public T get(java.util.function.Supplier<T> supplier) {
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        value = supplier.get();
                        initialized = true;
                    }
                }
            }
            return value;
        }
        
        public void set(T value) {
            this.value = value;
            this.initialized = true;
        }
        
        public void reset() {
            this.value = null;
            this.initialized = false;
        }
        
        public boolean isInitialized() {
            return initialized;
        }
    }
}
