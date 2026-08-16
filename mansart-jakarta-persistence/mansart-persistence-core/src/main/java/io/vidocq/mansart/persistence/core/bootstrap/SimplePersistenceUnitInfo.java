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

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;

/**
 * Simple implementation of PersistenceUnitInfo for use when parsing persistence.xml
 * at runtime. Used for TCK compatibility when PersistenceUnitInfo is not provided
 * by the container.
 */
public final class SimplePersistenceUnitInfo implements PersistenceUnitInfo {

    private final String persistenceUnitName;
    private final List<String> managedClassNames;
    private final Properties properties;
    private final ClassLoader classLoader;

    /**
     * Creates a SimplePersistenceUnitInfo from a persistence-unit XML element.
     */
    public SimplePersistenceUnitInfo(String persistenceUnitName, 
                                    org.w3c.dom.Element puElement, 
                                    ClassLoader classLoader) {
        this.persistenceUnitName = persistenceUnitName;
        this.classLoader = classLoader;
        
        // Extract managed class names
        List<String> classNames = Collections.emptyList();
        org.w3c.dom.NodeList classNodes = puElement.getElementsByTagName("class");
        if (classNodes.getLength() > 0) {
            classNames = new java.util.ArrayList<>();
            for (int i = 0; i < classNodes.getLength(); i++) {
                String className = classNodes.item(i).getTextContent().trim();
                if (!className.isEmpty()) {
                    classNames.add(className);
                }
            }
        }
        this.managedClassNames = Collections.unmodifiableList(classNames);
        
        // Extract properties
        Properties props = new Properties();
        org.w3c.dom.NodeList propNodes = puElement.getElementsByTagName("property");
        for (int i = 0; i < propNodes.getLength(); i++) {
            org.w3c.dom.Element propElement = (org.w3c.dom.Element) propNodes.item(i);
            String name = propElement.getAttribute("name");
            String value = propElement.getAttribute("value");
            if (name != null && !name.isEmpty() && value != null) {
                props.setProperty(name, value);
            }
        }
        this.properties = props;
    }

    @Override
    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    @Override
    public String getPersistenceProviderClassName() {
        // Extract from persistence-unit element
        return null; // Will be handled by provider
    }

    @Override
    public String getScopeAnnotationName() {
        return null; // Not implemented for TCK
    }

    @Override
    public List<String> getQualifierAnnotationNames() {
        return Collections.emptyList(); // Not implemented for TCK
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        String txType = properties.getProperty("jakarta.persistence.transactionType", 
                properties.getProperty("javax.persistence.transactionType", "RESOURCE_LOCAL"));
        return "JTA".equalsIgnoreCase(txType) 
                ? PersistenceUnitTransactionType.JTA 
                : PersistenceUnitTransactionType.RESOURCE_LOCAL;
    }

    @Override
    public DataSource getJtaDataSource() {
        return null; // Not implemented for TCK
    }

    @Override
    public DataSource getNonJtaDataSource() {
        return null; // Not implemented for TCK
    }

    @Override
    public List<String> getMappingFileNames() {
        return Collections.emptyList();
    }

    @Override
    public List<URL> getJarFileUrls() {
        return Collections.emptyList();
    }

    @Override
    public URL getPersistenceUnitRootUrl() {
        return null;
    }

    @Override
    public List<String> getManagedClassNames() {
        return managedClassNames;
    }

    @Override
    public boolean excludeUnlistedClasses() {
        String exclude = properties.getProperty("jakarta.persistence.excludeUnlistedClasses",
                properties.getProperty("javax.persistence.excludeUnlistedClasses", "false"));
        return Boolean.parseBoolean(exclude);
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        return SharedCacheMode.UNSPECIFIED;
    }

    @Override
    public ValidationMode getValidationMode() {
        return ValidationMode.AUTO;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public String getPersistenceXMLSchemaVersion() {
        return "3.2";
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public void addTransformer(ClassTransformer transformer) {
        // Not implemented
    }

    @Override
    public ClassLoader getNewTempClassLoader() {
        return classLoader;
    }
}
