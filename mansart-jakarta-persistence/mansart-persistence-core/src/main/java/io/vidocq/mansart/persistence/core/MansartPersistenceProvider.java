/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.ProviderUtil;
import java.util.Map;

/**
 * Mansart implementation of Jakarta Persistence 3.2 PersistenceProvider.
 * All methods throw UnsupportedOperationException - stub for JP-01.
 */
public class MansartPersistenceProvider implements PersistenceProvider {

    @Override
    public EntityManagerFactory createEntityManagerFactory(String emName, Map emProperties) {
        throw new UnsupportedOperationException("not implemented: createEntityManagerFactory(String, Map)");
    }

    @Override
    public EntityManagerFactory createEntityManagerFactory(PersistenceConfiguration configuration) {
        throw new UnsupportedOperationException("not implemented: createEntityManagerFactory(PersistenceConfiguration)");
    }

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map properties) {
        throw new UnsupportedOperationException("not implemented: createContainerEntityManagerFactory(PersistenceUnitInfo, Map)");
    }

    @Override
    public void generateSchema(PersistenceUnitInfo info, Map<?, ?> map) {
        throw new UnsupportedOperationException("not implemented: generateSchema(PersistenceUnitInfo, Map)");
    }

    @Override
    public boolean generateSchema(String persistenceUnitName, Map<?, ?> map) {
        throw new UnsupportedOperationException("not implemented: generateSchema(String, Map)");
    }

    @Override
    public ProviderUtil getProviderUtil() {
        throw new UnsupportedOperationException("not implemented: getProviderUtil()");
    }
}
