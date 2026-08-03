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
 * Implementation of Jakarta Persistence PersistenceProvider.
 * This is the main entry point for the Mansart JPA implementation.
 */
package io.vidocq.mansart.persistence.core.bootstrap;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.ProviderUtil;
import java.util.Map;

/**
 * Mansart implementation of the Jakarta Persistence PersistenceProvider interface.
 * This class is responsible for creating EntityManagerFactory instances.
 * Note: For M1, some methods throw UnsupportedOperationException.
 * Full implementation will be done in later milestones.
 */
public class MansartPersistenceProvider implements PersistenceProvider {

    /**
     * Creates and returns a new EntityManagerFactory for the named persistence unit.
     *
     * @param persistenceUnitName the name of the persistence unit
     * @param properties additional properties for the EntityManagerFactory
     * @return the EntityManagerFactory for the named persistence unit
     */
    @Override
    public EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map properties) {
        // TODO: Implement persistence unit resolution
        // 1. Look up persistence.xml for the given persistence unit name
        // 2. Merge properties from persistence.xml and the provided map
        // 3. Create and configure MansartEntityManagerFactory
        return new DefaultMansartEntityManagerFactory(persistenceUnitName, properties);
    }

    /**
     * Creates and returns a new EntityManagerFactory for the given PersistenceUnitInfo.
     *
     * @param info the PersistenceUnitInfo for the persistence unit
     * @param properties additional properties for the EntityManagerFactory
     * @return the EntityManagerFactory for the given PersistenceUnitInfo
     */
    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
        // TODO: Implement container-managed EntityManagerFactory creation
        // 1. Validate the PersistenceUnitInfo
        // 2. Merge properties from PersistenceUnitInfo and the provided map
        // 3. Create and configure MansartEntityManagerFactory
        return new DefaultMansartEntityManagerFactory(info.getPersistenceUnitName(), properties);
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
