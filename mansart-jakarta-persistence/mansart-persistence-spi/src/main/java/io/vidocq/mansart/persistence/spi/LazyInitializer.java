/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.spi;

/**
 * Callback interface used by lazy entity proxies to trigger database loading.
 * The runtime (EntityManager) provides the implementation that performs the actual SQL fetch.
 */
@FunctionalInterface
public interface LazyInitializer {
    
    /**
     * Triggers the loading of the underlying entity from the database.
     * After this call returns, the proxy's fields should be populated.
     */
    void initialize();
}
