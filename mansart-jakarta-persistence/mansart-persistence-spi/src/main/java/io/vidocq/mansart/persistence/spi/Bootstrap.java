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
package io.vidocq.mansart.persistence.spi;

import java.util.Map;
import java.util.Optional;

/**
 * Bootstrap configuration for the persistence provider.
 *
 * <p>An instance of this interface is created during persistence provider initialization
 * to configure and bootstrap the persistence layer. It provides access to:
 * <ul>
 *   <li>Persistence unit name</li>
 *   <li>Configuration properties</li>
 *   <li>Data source information</li>
 * </ul>
 */
public interface Bootstrap {

    /**
     * Returns the persistence unit name.
     *
     * @return the persistence unit name, never {@code null}
     */
    String persistenceUnitName();

    /**
     * Returns the configuration properties for the persistence unit.
     *
     * @return unmodifiable map of configuration properties, never {@code null}
     */
    Map<String, Object> properties();

    /**
     * Retrieves a configuration property with the specified key.
     *
     * @param <T> the type of the property value
     * @param key the property key
     * @param type the type of the property value
     * @return optional containing the property value if present, empty otherwise
     */
    <T> Optional<T> getProperty(String key, Class<T> type);

    /**
     * Returns the name of the database dialect to use.
     *
     * @return the dialect name, or {@code null} if not specified
     */
    String dialect();

    /**
     * Returns the schema name to use for the persistence unit.
     *
     * @return the schema name, or {@code null} if not specified
     */
    String schema();

    /**
     * Indicates whether schema generation is enabled.
     *
     * @return {@code true} if schema generation is enabled, {@code false} otherwise
     */
    boolean generateSchema();

    /**
     * Returns the class loader to use for loading entities and services.
     *
     * @return the class loader, never {@code null}
     */
    ClassLoader classLoader();
}
