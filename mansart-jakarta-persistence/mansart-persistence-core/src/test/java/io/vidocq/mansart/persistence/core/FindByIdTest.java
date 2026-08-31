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
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

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
 * Unit tests for {@code MansartEntityManager.find(Class, Object)}.
 *
 * <p>Covers: basic findById, not-found (null), persistence context registration,
 * and post-close behaviour.</p>
 */
class FindByIdTest {

    private JdbcDataSource dataSource;
    private EntityManagerFactory emf;
    private EntityManager em;

    @BeforeEach
    void setUp() throws Exception {
        // Create H2 in-memory datasource with unique name per test class.
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:findTest" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");

        // Create the test table.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE FIND_TEST ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "NAME VARCHAR(255), "
                            + "PRIMARY KEY (ID))");
        }

        // Build an EntityModel with MethodHandles for getters/setters.
        var lookup = MethodHandles.lookup();
        var privateLookup = MethodHandles.privateLookupIn(FindTestEntity.class, lookup);
        MethodHandle idGetter = privateLookup.findGetter(
                FindTestEntity.class, "id", String.class);
        MethodHandle idSetter = privateLookup.findSetter(
                FindTestEntity.class, "id", String.class);
        MethodHandle nameGetter = privateLookup.findGetter(
                FindTestEntity.class, "name", String.class);
        MethodHandle nameSetter = privateLookup.findSetter(
                FindTestEntity.class, "name", String.class);

        var idAttr = new IdAttribute<FindTestEntity, String>(
                "id", "ID", String.class, FindTestEntity.class,
                false, idGetter, idSetter);
        var nameAttr = new TextAttribute<FindTestEntity>(
                "name", "NAME", FindTestEntity.class,
                true, false, 255, nameGetter, nameSetter);

        var model = new EntityModel<FindTestEntity>(
                FindTestEntity.class, "FIND_TEST", null,
                idAttr, java.util.Optional.empty(),
                List.of(idAttr, nameAttr),
                List.of(),
                privateLookup.findConstructor(FindTestEntity.class,
                        MethodType.methodType(void.class)));

        // Build the factory.
        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        PersistenceUnitInfo fakeInfo =
                makeFakeInfo(List.of(FindTestEntity.class.getName()),
                        dataSource);

        // Build a map containing our manually created EntityModel.
        var entityModelMap = Map.of(FindTestEntity.class, model);
        var hints = Map.of("mansart.entityModels", entityModelMap);

        emf = provider.createContainerEntityManagerFactory(fakeInfo, hints);
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
    void findByIdReturnsEntityWithAllAttributes() throws Exception {
        // Insert data directly via JDBC (skip persist/flush).
        try (Connection conn = dataSource.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO FIND_TEST (ID, NAME) VALUES (?, ?)")) {
            stmt.setString(1, "abc123");
            stmt.setString(2, "Test Name");
            stmt.executeUpdate();
        }

        // Find by ID.
        var found = em.find(FindTestEntity.class, "abc123");
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo("abc123");
        assertThat(found.getName()).isEqualTo("Test Name");
    }

    @Test
    void findByIdNotFoundReturnsNull() {
        var found = em.find(FindTestEntity.class, "nonexistent");
        assertThat(found).isNull();
    }

    @Test
    void findByIdRegistersInPersistenceContext() throws Exception {
        // Insert data directly via JDBC (skip persist/flush).
        try (Connection conn = dataSource.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO FIND_TEST (ID, NAME) VALUES (?, ?)")) {
            stmt.setString(1, "ctx001");
            stmt.setString(2, "Context Test");
            stmt.executeUpdate();
        }

        var found = em.find(FindTestEntity.class, "ctx001");
        assertThat(found).isNotNull();

        // The found entity should be the same instance as the persisted one
        // (from the persistence context's identity map).
        assertThat(em.contains(found)).isTrue();
    }

    @Test
    void findByIdAfterCloseThrowsIllegalStateException() {
        em.close();
        assertThatThrownBy(() -> em.find(FindTestEntity.class, "any-id"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("EntityManager is closed");
    }

    /**
     * Create a fake PersistenceUnitInfo for testing.
     */
    private static PersistenceUnitInfo makeFakeInfo(
            List<String> managedClasses,
            DataSource ds) {
        return new PersistenceUnitInfo() {
            @Override public String getPersistenceUnitName() { return "testPU"; }
            @Override public String getPersistenceProviderClassName() { return null; }
            @Override public String getScopeAnnotationName() { return null; }
            @Override public List<String> getQualifierAnnotationNames() { return List.of(); }
            @Override public PersistenceUnitTransactionType getTransactionType() {
                return PersistenceUnitTransactionType.RESOURCE_LOCAL; }
            @Override public DataSource getJtaDataSource() { return null; }
            @Override public DataSource getNonJtaDataSource() { return ds; }
            @Override public List<String> getManagedClassNames() { return managedClasses; }
            @Override public List<String> getMappingFileNames() { return List.of(); }
            @Override public List<URL> getJarFileUrls() { return List.of(); }
            @Override public URL getPersistenceUnitRootUrl() { return null; }
            @Override public SharedCacheMode getSharedCacheMode() { return null; }
            @Override public ValidationMode getValidationMode() { return null; }
            @Override public Properties getProperties() { return new Properties(); }
            @Override public String getPersistenceXMLSchemaVersion() { return null; }
            @Override public ClassLoader getClassLoader() {
                return Thread.currentThread().getContextClassLoader(); }
            @Override public boolean excludeUnlistedClasses() { return false; }
            @Override public ClassLoader getNewTempClassLoader() { return null; }
            @Override public void addTransformer(ClassTransformer t) { }
        };
    }
}
