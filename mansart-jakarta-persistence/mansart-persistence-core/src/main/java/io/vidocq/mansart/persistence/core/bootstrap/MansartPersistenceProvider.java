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

package io.vidocq.mansart.persistence.core.bootstrap;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.ProviderUtil;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Mansart implementation of the Jakarta Persistence PersistenceProvider interface.
 * This class is responsible for creating EntityManagerFactory instances.
 * Implements persistence.xml parsing for M7.
 */
public class MansartPersistenceProvider implements PersistenceProvider {

    /**
     * Creates and returns a new EntityManagerFactory for the named persistence unit.
     * This implementation:
     * 1. Looks up persistence.xml for the given persistence unit name
     * 2. Merges properties from persistence.xml and the provided map
     * 3. Creates and configures MansartEntityManagerFactory
     *
     * @param persistenceUnitName the name of the persistence unit
     * @param properties additional properties for the EntityManagerFactory
     * @return the EntityManagerFactory for the named persistence unit
     * @throws IllegalArgumentException if the persistence unit is not found
     */
    @Override
    public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map properties) {
        try {
            // 1. Look up persistence.xml for the given persistence unit name
            PersistenceUnitConfig puConfig = PersistenceXmlParser.findByName(persistenceUnitName);
            
            // 2. Merge properties from persistence.xml and the provided map
            Map<String, Object> mergedProperties = new HashMap<>();
            
            // Add properties from persistence.xml if found
            if (puConfig != null) {
                mergedProperties.putAll(puConfig.getProperties());
            }
            
            // Override with provided properties (user properties take precedence)
            if (properties != null) {
                mergedProperties.putAll(properties);
            }
            
            // 3. Create and configure MansartEntityManagerFactory
            return new DefaultMansartEntityManagerFactory(persistenceUnitName, mergedProperties);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse persistence.xml", e);
        }
    }

    /**
     * Creates and returns a new EntityManagerFactory for the given PersistenceUnitInfo.
     * This implementation:
     * 1. Validates the PersistenceUnitInfo
     * 2. Merges properties from PersistenceUnitInfo and the provided map
     * 3. Creates and configures MansartEntityManagerFactory
     *
     * @param info the PersistenceUnitInfo for the persistence unit
     * @param properties additional properties for the EntityManagerFactory
     * @return the EntityManagerFactory for the given PersistenceUnitInfo
     */
    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
        if (info == null) {
            throw new IllegalArgumentException("PersistenceUnitInfo cannot be null");
        }
        
        String persistenceUnitName = info.getPersistenceUnitName();
        if (persistenceUnitName == null || persistenceUnitName.isEmpty()) {
            throw new IllegalArgumentException("Persistence unit name cannot be null or empty");
        }
        
        // Merge properties from PersistenceUnitInfo and the provided map
        Map<String, Object> mergedProperties = new HashMap<>();
        
        Properties puProperties = info.getProperties();
        if (puProperties != null) {
            for (Enumeration<?> e = puProperties.keys(); e.hasMoreElements(); ) {
                String key = (String) e.nextElement();
                mergedProperties.put(key, puProperties.getProperty(key));
            }
        }
        
        if (properties != null) {
            mergedProperties.putAll(properties);
        }
        
        return new DefaultMansartEntityManagerFactory(persistenceUnitName, mergedProperties);
    }

    /**
     * Returns the ProviderUtil implementation for this persistence provider.
     *
     * @return the ProviderUtil implementation
     */
    @Override
    public ProviderUtil getProviderUtil() {
        return new MansartProviderUtil();
    }

    /**
     * Creates EntityManagerFactory from PersistenceConfiguration.
     * Not yet implemented - will be implemented in a later milestone.
     */
    @Override
    public EntityManagerFactory createEntityManagerFactory(jakarta.persistence.PersistenceConfiguration configuration) {
        // TODO: Implement createEntityManagerFactory from PersistenceConfiguration
        throw new UnsupportedOperationException("createEntityManagerFactory(PersistenceConfiguration) not yet implemented");
    }

    /**
     * Generates schema for the persistence unit.
     * Not yet implemented - will be implemented in a later milestone.
     */
    @Override
    public void generateSchema(jakarta.persistence.spi.PersistenceUnitInfo info, Map properties) {
        // TODO: Implement schema generation
        throw new UnsupportedOperationException("Schema generation not yet implemented");
    }

    /**
     * Generates schema for the persistence unit.
     * Not yet implemented - will be implemented in a later milestone.
     */
    @Override
    public boolean generateSchema(String persistenceUnitName, Map properties) {
        // TODO: Implement schema generation
        throw new UnsupportedOperationException("Schema generation not yet implemented");
    }
}
