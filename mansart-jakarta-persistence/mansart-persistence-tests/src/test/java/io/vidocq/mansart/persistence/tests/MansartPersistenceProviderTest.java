/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.Properties;
import java.net.URL;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link io.vidocq.mansart.persistence.core.MansartPersistenceProvider}
 * covering the SPI contract and stub behaviour.
 *
 * <p>The TCK client {@code ee.jakarta.tck.persistence.spi.provider.Client}
 * exercises these methods (tests: {@code createContainerEntityManagerFactory},
 * {@code generateSchema}, {@code getProviderUtil},
 * {@code createEntityManagerFactory}).</p>
 */
class MansartPersistenceProviderTest {

    private final io.vidocq.mansart.persistence.core.MansartPersistenceProvider provider
            = new io.vidocq.mansart.persistence.core.MansartPersistenceProvider();

    /**
     * A minimal {@code PersistenceUnitInfo} implementation for testing.
     */
    private PersistenceUnitInfo makeInfo(List<String> managedClasses) {
        return new PersistenceUnitInfo() {
            @Override public String getPersistenceUnitName() { return "test"; }
            @Override public String getPersistenceProviderClassName() { return null; }
            @Override public String getScopeAnnotationName() { return null; }
            @Override public List<String> getQualifierAnnotationNames() { return List.of(); }
            @Override public jakarta.persistence.spi.PersistenceUnitTransactionType getTransactionType() {
                return jakarta.persistence.spi.PersistenceUnitTransactionType.RESOURCE_LOCAL;
            }
            @Override public DataSource getJtaDataSource() { return null; }
            @Override public DataSource getNonJtaDataSource() { return null; }
            @Override public List<String> getManagedClassNames() { return managedClasses; }
            @Override public List<String> getMappingFileNames() { return List.of(); }
            @Override public List<URL> getJarFileUrls() { return List.of(); }
            @Override public URL getPersistenceUnitRootUrl() { return null; }
            @Override public jakarta.persistence.SharedCacheMode getSharedCacheMode() { return null; }
            @Override public jakarta.persistence.ValidationMode getValidationMode() { return null; }
            @Override public Properties getProperties() { return new Properties(); }
            @Override public String getPersistenceXMLSchemaVersion() { return null; }
            @Override public ClassLoader getClassLoader() { return null; }
            @Override public boolean excludeUnlistedClasses() { return false; }
            @Override public ClassLoader getNewTempClassLoader() { return null; }
            @Override public void addTransformer(ClassTransformer t) { }
        };
    }

    /**
     * Verify that {@code createContainerEntityManagerFactory(PersistenceUnitInfo, Map)}
     * returns an {@code EntityManagerFactory}.
     */
    @Test
    void createContainerEntityManagerFactoryReturnsFactory() {
        var emf = provider.createContainerEntityManagerFactory(makeInfo(List.of()), Map.of());

        assertThat(emf).isNotNull();
        assertThat(emf.isOpen()).isTrue();
    }

    /**
     * Verify that {@code createContainerEntityManagerFactory(PersistenceUnitInfo, Map)}
     * throws when managed classes require runtime attribute building (not yet implemented).
     *
     * <p>Attribute-level metadata collection is delegated to the APT processor.
     * This test verifies that attempting to build a metamodel from loadable
     * entity classes fails with the expected exception.</p>
     */
    @Test
    void createContainerEntityManagerFactoryWithLoadableEntitiesThrows() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> provider.createContainerEntityManagerFactory(
                        makeInfo(List.of("io.vidocq.mansart.persistence.tests.TestEntity")), Map.of()))
                .withMessageContaining("buildAttributes");
    }

    /**
     * Verify that {@code generateSchema(PersistenceUnitInfo, Map)} throws
     * {@code UnsupportedOperationException}.
     */
    @Test
    void generateSchemaWithInfoThrows() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> provider.generateSchema(makeInfo(List.of()), Map.of()))
                .withMessageContaining("not implemented");
    }

    /**
     * Verify that {@code generateSchema(String, Map)} throws
     * {@code UnsupportedOperationException}.
     */
    @Test
    void generateSchemaWithNameThrows() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> provider.generateSchema("test", Map.of()))
                .withMessageContaining("not implemented");
    }

    /**
     * Verify that {@code getProviderUtil()} throws
     * {@code UnsupportedOperationException}.
     */
    @Test
    void getProviderUtilThrows() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(provider::getProviderUtil)
                .withMessageContaining("not implemented");
    }

    /**
     * Verify that {@code createEntityManagerFactory(String, Map)} throws
     * {@code UnsupportedOperationException}.
     */
    @Test
    void createEntityManagerFactoryWithNameAndHintsThrows() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> provider.createEntityManagerFactory("test", Map.of()))
                .withMessageContaining("not implemented");
    }

    /**
     * Verify that {@code createEntityManagerFactory(PersistenceConfiguration)} throws
     * {@code UnsupportedOperationException}.
     */
    @Test
    void createEntityManagerFactoryWithConfigThrows() {
        var config = new PersistenceConfiguration("test");
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> provider.createEntityManagerFactory(config))
                .withMessageContaining("not implemented");
    }
}
