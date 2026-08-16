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
package io.vidocq.mansart.persistence.core.bootstrap;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.LoadState;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.ProviderUtil;
import jakarta.persistence.spi.PersistenceUnitInfo;

import java.util.Map;

import io.vidocq.mansart.persistence.spi.Bootstrap;

/**
 * Mansart Persistence Provider implementation.
 *
 * <p>This class implements the {@link jakarta.persistence.spi.PersistenceProvider} interface
 * and serves as the entry point for creating {@link EntityManagerFactory} instances.
 *
 * <p>The provider uses the {@link Bootstrap} interface for configuration and
 * {@link EntityState} for entity state management. Dialect support is obtained
 * via {@link ServiceLoader} from {@code mansart-data-dialect-spi}.
 */
public final class MansartPersistenceProvider implements PersistenceProvider {

    private final ProviderUtil providerUtil = new MansartProviderUtil();

    /**
     * Creates an {@link EntityManagerFactory} for the given persistence unit name and properties.
     *
     * @param emName the name of the persistence unit, or {@code null} if there is only
     *               one persistence unit in the application
     * @param properties        a Map of configuration properties, or {@code null}
     * @return a new {@link EntityManagerFactory} instance
     */
    @Override
    public EntityManagerFactory createEntityManagerFactory(String emName, Map<?, ?> properties) {
        // Try to find PersistenceUnitInfo from persistence.xml on classpath
        jakarta.persistence.spi.PersistenceUnitInfo persistenceUnitInfo = findPersistenceUnitInfo(emName, properties);
        if (persistenceUnitInfo != null) {
            return createContainerEntityManagerFactory(persistenceUnitInfo, properties);
        }
        // Fallback to simple factory creation
        return new io.vidocq.mansart.persistence.core.runtime.MansartEntityManagerFactory(this, emName, properties);
    }
    
    /**
     * Finds PersistenceUnitInfo by parsing persistence.xml from classpath.
     * Returns null if not found.
     */
    private jakarta.persistence.spi.PersistenceUnitInfo findPersistenceUnitInfo(String emName, Map<?, ?> properties) {
        try {
            java.net.URL resource = getClass().getClassLoader().getResource("META-INF/persistence.xml");
            if (resource == null) {
                return null;
            }
            
            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(resource.openStream());
            
            org.w3c.dom.NodeList puNodes = doc.getElementsByTagName("persistence-unit");
            for (int i = 0; i < puNodes.getLength(); i++) {
                org.w3c.dom.Node puNode = puNodes.item(i);
                if (puNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    org.w3c.dom.Element puElement = (org.w3c.dom.Element) puNode;
                    String name = puElement.getAttribute("name");
                    if (emName == null || emName.equals(name)) {
                        return new SimplePersistenceUnitInfo(name, puElement, getClass().getClassLoader());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[M8-18] Error finding PersistenceUnitInfo: " + e.getMessage());
        }
        return null;
    }

    /**
     * Creates an {@link EntityManagerFactory} for the given persistence configuration.
     *
     * @param config persistence configuration
     * @return a new {@link EntityManagerFactory} instance
     */
    @Override
    public EntityManagerFactory createEntityManagerFactory(PersistenceConfiguration config) {
        return new io.vidocq.mansart.persistence.core.runtime.MansartEntityManagerFactory(
                this, config.name(), config.properties());
    }

    /**
     * Creates an {@link EntityManagerFactory} for the given persistence unit info and properties.
     *
     * <p>This method allows the provider to use the {@link PersistenceUnitInfo} to
     * configure the persistence unit. The PersistenceUnitInfo is passed to the factory
     * so it can extract entity classes for schema generation.
     *
     * @param persistenceUnitInfo persistence unit information
     * @param properties          a Map of configuration properties, or {@code null}
     * @return a new {@link EntityManagerFactory} instance
     */
    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo persistenceUnitInfo, Map<?, ?> properties) {
        return new io.vidocq.mansart.persistence.core.runtime.MansartEntityManagerFactory(
                this, persistenceUnitInfo.getPersistenceUnitName(), properties, persistenceUnitInfo);
    }

    /**
     * Generates the database schema for the given persistence unit info and properties.
     *
     * <p>This method is used to generate SQL DDL scripts for creating the database schema.
     * The actual implementation will be provided in subsequent milestones.
     *
     * @param info        persistence unit information
     * @param properties  a Map of configuration properties, or {@code null}
     */
    @Override
    public void generateSchema(PersistenceUnitInfo info, Map<?, ?> properties) {
        // Schema generation is handled by dialect
    }

    /**
     * Generates the database schema for the given persistence unit name and properties.
     *
     * @param puName      the persistence unit name
     * @param properties  a Map of configuration properties, or {@code null}
     * @return {@code true} if schema generation succeeded, {@code false} otherwise
     */
    @Override
    public boolean generateSchema(String puName, Map<?, ?> properties) {
        return false; // Not implemented yet
    }

    /**
     * Returns the provider utility for loading entity classes.
     *
     * @return the provider utility instance
     */
    @Override
    public ProviderUtil getProviderUtil() {
        return providerUtil;
    }
}
