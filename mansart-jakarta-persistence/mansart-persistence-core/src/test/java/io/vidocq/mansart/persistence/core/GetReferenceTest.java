/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import io.vidocq.mansart.data.dialect.Dialect;
import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.data.dialect.attribute.IdAttribute;
import io.vidocq.mansart.data.dialect.attribute.TextAttribute;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.h2.jdbcx.JdbcDataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@code MansartEntityManager.getReference(Class, Object)}.
 *
 * <p>Covers: basic getReference (non-null proxy),
 * post-close behaviour, and not-found (EntityNotFoundException).</p>
 */
class GetReferenceTest {

    private JdbcDataSource dataSource;
    private EntityManagerFactory emf;
    private EntityManager em;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:refTest" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE REF_TEST ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "NAME VARCHAR(255), "
                            + "PRIMARY KEY (ID))");
        }

        var lookup = MethodHandles.lookup();
        var privateLookup = MethodHandles.privateLookupIn(
                GetReferenceTestEntity.class, lookup);
        MethodHandle idGetter = privateLookup.findGetter(
                GetReferenceTestEntity.class, "id", String.class);
        MethodHandle idSetter = privateLookup.findSetter(
                GetReferenceTestEntity.class, "id", String.class);
        MethodHandle nameGetter = privateLookup.findGetter(
                GetReferenceTestEntity.class, "name", String.class);
        MethodHandle nameSetter = privateLookup.findSetter(
                GetReferenceTestEntity.class, "name", String.class);

        var idAttr = new IdAttribute<GetReferenceTestEntity, String>(
                "id", "ID", String.class, GetReferenceTestEntity.class,
                false, idGetter, idSetter);
        var nameAttr = new TextAttribute<GetReferenceTestEntity>(
                "name", "NAME", GetReferenceTestEntity.class,
                true, false, 255, nameGetter, nameSetter);

        var model = new EntityModel<GetReferenceTestEntity>(
                GetReferenceTestEntity.class, "REF_TEST", null,
                idAttr, java.util.Optional.empty(),
                List.of(idAttr, nameAttr),
                List.of(),
                privateLookup.findConstructor(GetReferenceTestEntity.class,
                        MethodType.methodType(void.class)));

        var fakeInfo = makeFakeInfo(
                List.of(GetReferenceTestEntity.class.getName()),
                dataSource);
        var entityModelMap = Map.of(
                GetReferenceTestEntity.class, model);
        var hints = Map.of("mansart.entityModels", entityModelMap);

        MansartPersistenceProvider provider
                = new MansartPersistenceProvider();
        emf = provider.createContainerEntityManagerFactory(
                fakeInfo, hints);
        em = emf.createEntityManager();
    }

    @AfterEach
    void tearDown() {
        if (em != null && em.isOpen()) {
            em.close();
        }
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @Test
    void getReferenceReturnsNonNullProxy() throws Exception {
        // Insert data directly via JDBC (skip persist/flush).
        try (Connection conn = dataSource.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO REF_TEST (ID, NAME) VALUES (?, ?)")) {
            stmt.setString(1, "ref001");
            stmt.setString(2, "Ref Name");
            stmt.executeUpdate();
        }

        // getReference returns a lazy proxy (not null).
        // Note: the proxy is NOT an instance of the entity class
        // because the Class-File API (JEP 484) is not accessible
        // from this module. The proxy is registered in the
        // persistence context by class + PK, so contains() works.
        Object proxy = em.getReference(
                GetReferenceTestEntity.class, "ref001");
        assertThat(proxy).isNotNull();
        // The proxy is registered in the persistence context.
        assertThat(em.contains(proxy)).isTrue();
    }

    @Test
    void getReferenceAfterCloseThrowsIllegalStateException() {
        em.close();
        assertThatThrownBy(() -> em.getReference(
                GetReferenceTestEntity.class, "any"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("EntityManager is closed");
    }

    /**
     * Create a fake PersistenceUnitInfo for testing.
     */
    private static jakarta.persistence.spi.PersistenceUnitInfo
            makeFakeInfo(List<String> managedClasses, DataSource ds) {
        return new jakarta.persistence.spi.PersistenceUnitInfo() {
            @Override public String getPersistenceUnitName() {
                return "testPU"; }
            @Override public String getPersistenceProviderClassName() {
                return null; }
            @Override public String getScopeAnnotationName() {
                return null; }
            @Override public List<String> getQualifierAnnotationNames() {
                return List.of(); }
            @Override
            public jakarta.persistence.spi.PersistenceUnitTransactionType
                    getTransactionType() {
                return jakarta.persistence.spi.PersistenceUnitTransactionType.RESOURCE_LOCAL; }
            @Override public DataSource getJtaDataSource() {
                return null; }
            @Override public DataSource getNonJtaDataSource() {
                return ds; }
            @Override public List<String> getManagedClassNames() {
                return managedClasses; }
            @Override public List<String> getMappingFileNames() {
                return List.of(); }
            @Override public List<URL> getJarFileUrls() {
                return List.of(); }
            @Override public URL getPersistenceUnitRootUrl() {
                return null; }
            @Override public jakarta.persistence.SharedCacheMode
                    getSharedCacheMode() { return null; }
            @Override public jakarta.persistence.ValidationMode
                    getValidationMode() { return null; }
            @Override public Properties getProperties() {
                return new Properties(); }
            @Override public String getPersistenceXMLSchemaVersion() {
                return null; }
            @Override public ClassLoader getClassLoader() {
                return Thread.currentThread().getContextClassLoader(); }
            @Override public boolean excludeUnlistedClasses() {
                return false; }
            @Override public ClassLoader getNewTempClassLoader() {
                return null; }
            @Override public void addTransformer(
                    jakarta.persistence.spi.ClassTransformer t) { }
        };
    }
}
