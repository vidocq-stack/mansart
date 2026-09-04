/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.ProviderUtil;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mansart implementation of Jakarta Persistence 3.2 PersistenceProvider.
 * Parses persistence.xml and PersistenceConfiguration to create EntityManagerFactory instances.
 */
public class MansartPersistenceProvider implements PersistenceProvider {

    @Override
    public EntityManagerFactory createEntityManagerFactory(String emName, Map<?, ?> emProperties) {
        if (emName == null) {
            throw new PersistenceException("Persistence unit name cannot be null");
        }

        // Parse persistence.xml to find the persistence unit
        Map<String, PersistenceXmlParser.PersistenceUnitDescriptor> descriptors =
                PersistenceXmlParser.parse(classLoader());

        PersistenceXmlParser.PersistenceUnitDescriptor descriptor = descriptors.get(emName);
        if (descriptor == null) {
            // Spec-compliant: provider doesn't recognize the PU
            return null;
        }

        // Build merged properties: start with XML properties, overlay runtime properties
        Map<String, Object> mergedProperties = new LinkedHashMap<>(
                descriptor.properties()
        );

        if (emProperties != null) {
            for (Map.Entry<?, ?> entry : emProperties.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (key != null) {
                    mergedProperties.put(String.valueOf(key), value);
                }
            }
        }

        return new MansartEntityManagerFactory(emName, descriptor.transactionType(), mergedProperties);
    }

    @Override
    public EntityManagerFactory createEntityManagerFactory(PersistenceConfiguration configuration) {
        if (configuration == null) {
            throw new PersistenceException("PersistenceConfiguration cannot be null");
        }

        return new MansartEntityManagerFactory(
                configuration.name(),
                configuration.transactionType(),
                new LinkedHashMap<>(configuration.properties())
        );
    }

    /**
     * Helper to get the appropriate class loader for resource lookup.
     * Uses context class loader, falling back to this class's class loader.
     */
    private static ClassLoader classLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = MansartPersistenceProvider.class.getClassLoader();
        }
        return cl;
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
