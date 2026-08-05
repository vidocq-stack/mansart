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

import io.vidocq.mansart.data.core.MansartData;
import io.vidocq.mansart.data.core.RepositoryRuntime;
import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.persistence.core.bootstrap.SchemaGenerator;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SchemaValidationException;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.ProviderUtil;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;

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
     * M6 - Sprint 1: Implement schema generation for PersistenceUnitInfo.
     */
    @Override
    public void generateSchema(PersistenceUnitInfo info, Map properties) {
        if (info == null) {
            throw new IllegalArgumentException("PersistenceUnitInfo cannot be null");
        }

        // Extract entity classes from PersistenceUnitInfo
        List<Class<?>> entityClasses = new ArrayList<>();
        for (String className : info.getManagedClassNames()) {
            try {
                Class<?> clazz = Class.forName(className, true, info.getClassLoader());
                entityClasses.add(clazz);
            } catch (ClassNotFoundException e) {
                // Log warning and continue
                System.Logger logger = System.getLogger(getClass().getName());
                logger.log(System.Logger.Level.WARNING, "Could not load entity class: " + className);
            }
        }

        // Create data source and repository runtime for schema generation
        DataSource dataSource = createDataSourceFromProperties(info.getProperties(), properties);
        MansartData mansartData = MansartData.builder()
                .dataSource(dataSource)
                .build();
        RepositoryRuntime repositoryRuntime = mansartData.runtime();

        // Create and execute schema generator
        SchemaGenerator schemaGenerator = new SchemaGenerator(
            repositoryRuntime, dataSource, mergePropertiesToMap(info.getProperties(), properties), entityClasses);
        
        try {
            schemaGenerator.generateSchema();
        } catch (SchemaValidationException e) {
            throw new RuntimeException("Schema generation failed", e);
        }
    }

    /**
     * Generates schema for the persistence unit by name.
     * M6 - Sprint 1: Implement schema generation for persistence unit name.
     */
    @Override
    public boolean generateSchema(String persistenceUnitName, Map properties) {
        if (persistenceUnitName == null || persistenceUnitName.isEmpty()) {
            throw new IllegalArgumentException("Persistence unit name cannot be null or empty");
        }

        try {
            // Look up persistence.xml for the given persistence unit name
            PersistenceUnitConfig puConfig = PersistenceXmlParser.findByName(persistenceUnitName);
            
            if (puConfig == null) {
                System.Logger logger = System.getLogger(getClass().getName());
                logger.log(System.Logger.Level.WARNING, "Persistence unit not found: " + persistenceUnitName);
                return false;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> propertiesMap = (Map<String, Object>) properties;
            
            // Merge properties
            Map<String, Object> mergedProperties = mergePropertiesToMap(toProperties(puConfig.getProperties()), propertiesMap);
            
            // Create data source and repository runtime
            DataSource dataSource = createDataSourceFromProperties(toProperties(puConfig.getProperties()), propertiesMap);
            MansartData mansartData = MansartData.builder()
                    .dataSource(dataSource)
                    .build();
            RepositoryRuntime repositoryRuntime = mansartData.runtime();

            // Get entity classes
            List<Class<?>> entityClasses = puConfig.getEntityClasses();

            // Create and execute schema generator
            SchemaGenerator schemaGenerator = new SchemaGenerator(
                repositoryRuntime, dataSource, mergedProperties, entityClasses);
            
            try {
                schemaGenerator.generateSchema();
                return true;
            } catch (SchemaValidationException e) {
                System.Logger logger = System.getLogger(getClass().getName());
                logger.log(System.Logger.Level.WARNING, "Schema generation failed: " + e.getMessage());
                return false;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse persistence.xml", e);
        }
    }

    /**
     * Creates a DataSource from properties.
     */
    private DataSource createDataSourceFromProperties(Properties puProperties, Map<String, Object> additionalProperties) {
        try {
            Class<?> jdbcDataSourceClass = Class.forName("org.h2.jdbcx.JdbcDataSource");
            Object dataSource = jdbcDataSourceClass.getDeclaredConstructor().newInstance();
            
            // Merge properties
            Map<String, Object> merged = mergePropertiesToMap(puProperties, additionalProperties);
            
            // Set JDBC URL
            String url = (String) merged.getOrDefault("jakarta.persistence.jdbc.url", 
                "jdbc:h2:mem:mansart-persistence;DB_CLOSE_DELAY=-1");
            jdbcDataSourceClass.getMethod("setURL", String.class).invoke(dataSource, url);
            
            // Set JDBC user
            String user = (String) merged.getOrDefault("jakarta.persistence.jdbc.user", "sa");
            jdbcDataSourceClass.getMethod("setUser", String.class).invoke(dataSource, user);
            
            // Set JDBC password
            String password = (String) merged.getOrDefault("jakarta.persistence.jdbc.password", "");
            jdbcDataSourceClass.getMethod("setPassword", String.class).invoke(dataSource, password);
            
            return (DataSource) dataSource;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create DataSource", e);
        }
    }

    /**
     * Merges properties from Properties and Map.
     */
    private Map<String, Object> mergePropertiesToMap(Properties properties, Map<String, Object> additionalProperties) {
        Map<String, Object> merged = new HashMap<>();
        
        if (properties != null) {
            for (Enumeration<?> e = properties.keys(); e.hasMoreElements(); ) {
                String key = (String) e.nextElement();
                merged.put(key, properties.getProperty(key));
            }
        }
        
        if (additionalProperties != null) {
            merged.putAll(additionalProperties);
        }
        
        return merged;
    }

    /**
     * Converts Map to Properties.
     */
    private Properties toProperties(Map<String, Object> map) {
        Properties properties = new Properties();
        if (map != null) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() != null) {
                    properties.setProperty(entry.getKey(), entry.getValue().toString());
                }
            }
        }
        return properties;
    }
}
