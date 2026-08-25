/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions of such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * or any later version, which is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * It is also made available under the European Union Public Licence v. 1.2,
 * which is available at https://joinup.ec.europa.eu/collection/eupl/eupl-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

import javax.sql.DataSource;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Mutable implementation of {@link PersistenceUnitInfo} populated by
 * {@link PersistenceUnitReader} from a {@code persistence.xml} document.
 *
 * <p>All data-source related methods return {@code null} because this
 * implementation does not bind real {@code DataSource} instances —
 * that is the responsibility of the container at deployment time.</p>
 *
 * @see PersistenceUnitReader
 */
public class PersistenceUnitInfoImpl implements PersistenceUnitInfo {

    private String persistenceUnitName;
    private String persistenceProviderClassName;
    private String scopeAnnotationName;
    private List<String> qualifierAnnotationNames;
    private PersistenceUnitTransactionType transactionType;
    private String jtaDataSourceName;
    private String nonJtaDataSourceName;
    private List<String> mappingFileNames;
    private List<URL> jarFileUrls;
    private URL persistenceUnitRootUrl;
    private List<String> managedClassNames;
    private boolean excludeUnlistedClasses;
    private SharedCacheMode sharedCacheMode;
    private ValidationMode validationMode;
    private String persistenceXMLSchemaVersion;
    private Properties properties;
    private ClassLoader classLoader;
    private List<ClassTransformer> transformers;

    /**
     * Default constructor — all fields are {@code null} or empty.
     */
    public PersistenceUnitInfoImpl() {
        this.qualifierAnnotationNames = new ArrayList<>();
        this.mappingFileNames = new ArrayList<>();
        this.jarFileUrls = new ArrayList<>();
        this.managedClassNames = new ArrayList<>();
        this.transformers = new ArrayList<>();
        this.properties = new Properties();
    }

    @Override
    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    public void setPersistenceUnitName(String persistenceUnitName) {
        this.persistenceUnitName = persistenceUnitName;
    }

    @Override
    public String getPersistenceProviderClassName() {
        return persistenceProviderClassName;
    }

    public void setPersistenceProviderClassName(String persistenceProviderClassName) {
        this.persistenceProviderClassName = persistenceProviderClassName;
    }

    @Override
    public String getScopeAnnotationName() {
        return scopeAnnotationName;
    }

    public void setScopeAnnotationName(String scopeAnnotationName) {
        this.scopeAnnotationName = scopeAnnotationName;
    }

    @Override
    public List<String> getQualifierAnnotationNames() {
        return Collections.unmodifiableList(qualifierAnnotationNames);
    }

    public void setQualifierAnnotationNames(List<String> qualifierAnnotationNames) {
        this.qualifierAnnotationNames = new ArrayList<>(qualifierAnnotationNames);
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(PersistenceUnitTransactionType transactionType) {
        this.transactionType = transactionType;
    }

    @Override
    public DataSource getJtaDataSource() {
        // Not bound yet — the reader stores the resource name only.
        // A real DataSource is provided by the container at deployment.
        return null;
    }

    public String getJtaDataSourceName() {
        return jtaDataSourceName;
    }

    public void setJtaDataSourceName(String jtaDataSourceName) {
        this.jtaDataSourceName = jtaDataSourceName;
    }

    @Override
    public DataSource getNonJtaDataSource() {
        // Not bound yet — the reader stores the resource name only.
        return null;
    }

    public String getNonJtaDataSourceName() {
        return nonJtaDataSourceName;
    }

    public void setNonJtaDataSourceName(String nonJtaDataSourceName) {
        this.nonJtaDataSourceName = nonJtaDataSourceName;
    }

    @Override
    public List<String> getMappingFileNames() {
        return Collections.unmodifiableList(mappingFileNames);
    }

    public void addMappingFileName(String mappingFileName) {
        this.mappingFileNames.add(mappingFileName);
    }

    @Override
    public List<URL> getJarFileUrls() {
        return Collections.unmodifiableList(jarFileUrls);
    }

    public void addJarFileUrl(URL jarFileUrl) {
        this.jarFileUrls.add(jarFileUrl);
    }

    @Override
    public URL getPersistenceUnitRootUrl() {
        return persistenceUnitRootUrl;
    }

    public void setPersistenceUnitRootUrl(URL persistenceUnitRootUrl) {
        this.persistenceUnitRootUrl = persistenceUnitRootUrl;
    }

    @Override
    public List<String> getManagedClassNames() {
        return Collections.unmodifiableList(managedClassNames);
    }

    public void addManagedClassName(String managedClassName) {
        this.managedClassNames.add(managedClassName);
    }

    @Override
    public boolean excludeUnlistedClasses() {
        return excludeUnlistedClasses;
    }

    public void setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
        this.excludeUnlistedClasses = excludeUnlistedClasses;
    }

    @Override
    public SharedCacheMode getSharedCacheMode() {
        return sharedCacheMode;
    }

    public void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
        this.sharedCacheMode = sharedCacheMode;
    }

    @Override
    public ValidationMode getValidationMode() {
        return validationMode;
    }

    public void setValidationMode(ValidationMode validationMode) {
        this.validationMode = validationMode;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    public void addProperty(String name, String value) {
        this.properties.setProperty(name, value);
    }

    @Override
    public String getPersistenceXMLSchemaVersion() {
        return persistenceXMLSchemaVersion;
    }

    public void setPersistenceXMLSchemaVersion(String persistenceXMLSchemaVersion) {
        this.persistenceXMLSchemaVersion = persistenceXMLSchemaVersion;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void addTransformer(ClassTransformer transformer) {
        this.transformers.add(transformer);
    }

    @Override
    public ClassLoader getNewTempClassLoader() {
        // Extreme fallback — just return the same class loader.
        return getClassLoader();
    }
}
