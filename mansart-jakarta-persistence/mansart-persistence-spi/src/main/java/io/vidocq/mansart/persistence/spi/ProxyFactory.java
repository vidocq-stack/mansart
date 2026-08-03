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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
/**
 * SPI interface for creating dynamic proxies for JPA entities.
 * Used for lazy loading of entity relationships.
 */
package io.vidocq.mansart.persistence.spi;

import java.lang.reflect.InvocationHandler;

/**
 * Factory interface for creating dynamic proxies for JPA entities.
 * Proxies are used primarily for lazy loading of entity relationships.
 */
public interface ProxyFactory {

    /**
     * Creates a new proxy instance for the given entity class.
     *
     * @param <T> the entity type
     * @param entityClass the entity class to proxy
     * @param handler the invocation handler for the proxy
     * @return a new proxy instance
     */
    <T> T createProxy(Class<T> entityClass, InvocationHandler handler);

    /**
     * Creates a new proxy instance with a specific class loader.
     *
     * @param <T> the entity type
     * @param entityClass the entity class to proxy
     * @param classLoader the class loader to use for the proxy
     * @param handler the invocation handler for the proxy
     * @return a new proxy instance
     */
    <T> T createProxy(Class<T> entityClass, ClassLoader classLoader, InvocationHandler handler);

    /**
     * Checks if the given object is a proxy created by this factory.
     *
     * @param obj the object to check
     * @return true if the object is a proxy, false otherwise
     */
    boolean isProxy(Object obj);

    /**
     * Gets the invocation handler for the given proxy.
     *
     * @param proxy the proxy object
     * @return the invocation handler, or null if the object is not a proxy
     */
    InvocationHandler getInvocationHandler(Object proxy);
}
