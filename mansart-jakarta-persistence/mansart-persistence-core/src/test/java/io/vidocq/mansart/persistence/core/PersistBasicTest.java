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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Properties;
import java.net.URL;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test for {@code MansartEntityManager.persist(Object)}.
 *
 * <p>Verifies that persisting a new entity inserts a row into the database
 * and registers the entity in the persistence context.</p>
 */
class PersistBasicTest {

    private JdbcDataSource dataSource;
    private EntityManagerFactory emf;
    private EntityManager em;

    @BeforeEach
    void setUp() throws Exception {
        // Create H2 in-memory datasource with unique name per test class.
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:persistTest" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");

        // Create the test table.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE PERSIST_TEST ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "NAME VARCHAR(255), "
                            + "PRIMARY KEY (ID))");
        }

        // Build an EntityModel with MethodHandles for getters/setters.
        var lookup = MethodHandles.lookup();
        var privateLookup = MethodHandles.privateLookupIn(PersistTestEntity.class, lookup);
        MethodHandle idGetter = privateLookup.findGetter(
                PersistTestEntity.class, "id", String.class);
        MethodHandle idSetter = privateLookup.findSetter(
                PersistTestEntity.class, "id", String.class);
        MethodHandle nameGetter = privateLookup.findGetter(
                PersistTestEntity.class, "name", String.class);
        MethodHandle nameSetter = privateLookup.findSetter(
                PersistTestEntity.class, "name", String.class);

        var idAttr = new IdAttribute<PersistTestEntity, String>(
                "id", "ID", String.class, PersistTestEntity.class,
                false, idGetter, idSetter);
        var nameAttr = new io.vidocq.mansart.data.dialect.attribute.TextAttribute<PersistTestEntity>(
                "name", "NAME", PersistTestEntity.class,
                true, false, 255, nameGetter, nameSetter);

        var model = new EntityModel<PersistTestEntity>(
                PersistTestEntity.class, "PERSIST_TEST", null,
                idAttr, java.util.Optional.empty(),
                List.of(idAttr, nameAttr),
                List.of(),
                privateLookup.findConstructor(PersistTestEntity.class,
                        MethodType.methodType(void.class)));

        // Build the factory.
        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        PersistenceUnitInfo fakeInfo =
                makeFakeInfo(List.of(PersistTestEntity.class.getName()),
                        dataSource);

        // Build a map containing our manually created EntityModel.
        var entityModelMap = Map.of(PersistTestEntity.class, model);
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
    void persistNewEntityCreatesDbRow() throws Exception {
        // Create and persist a new entity.
        PersistTestEntity entity = new PersistTestEntity();
        entity.setId("test-1");
        entity.setName("Test Name");

        em.persist(entity);

        // Verify the entity is managed.
        assertThat(em.contains(entity)).isTrue();

        // Verify the DB row exists.
        try (Connection conn = dataSource.getConnection();
              Statement stmt = conn.createStatement();
              ResultSet rs = stmt.executeQuery(
                      "SELECT COUNT(*) AS cnt FROM PERSIST_TEST WHERE ID = 'test-1'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("cnt")).isEqualTo(1);
        }
    }

    @Test
    void persistNullEntityThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> em.persist(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void persistAfterCloseThrowsIllegalStateException() {
        em.close();
        assertThatThrownBy(() -> em.persist(new PersistTestEntity()))
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
