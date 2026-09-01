/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under
 * the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

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

/**
 * Unit tests for {@code EntityManager.clear()} and {@code contains()}.
 *
 * <p>Verifies: clear empties the persistence context, contains reports
 * managed-state correctly, and both methods throw
 * {@link IllegalStateException} after close().</p>
 */
class ClearContainsTest {

    private JdbcDataSource dataSource;
    private EntityManagerFactory emf;
    private MansartEntityManager em;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:clearContainsTest" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE CC_ENTITY ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "LABEL VARCHAR(255), "
                            + "PRIMARY KEY (ID))");
        }

        var lookup = MethodHandles.lookup();
        var privateLookup =
                MethodHandles.privateLookupIn(TestCcEntity.class, lookup);
        MethodHandle idGetter = privateLookup.findGetter(
                TestCcEntity.class, "id", String.class);
        MethodHandle idSetter = privateLookup.findSetter(
                TestCcEntity.class, "id", String.class);
        MethodHandle labelGetter = privateLookup.findGetter(
                TestCcEntity.class, "label", String.class);
        MethodHandle labelSetter = privateLookup.findSetter(
                TestCcEntity.class, "label", String.class);

        var idAttr = new io.vidocq.mansart.data.dialect.attribute.IdAttribute<
                        TestCcEntity, String>(
                "id", "ID", String.class, TestCcEntity.class,
                false, idGetter, idSetter);
        var labelAttr = new io.vidocq.mansart.data.dialect.attribute.TextAttribute<
                        TestCcEntity>(
                "label", "LABEL", TestCcEntity.class,
                true, false, 255, labelGetter, labelSetter);

        var model = new io.vidocq.mansart.data.dialect.EntityModel<
                TestCcEntity>(
                TestCcEntity.class, "CC_ENTITY", null,
                idAttr, java.util.Optional.empty(),
                List.of(idAttr, labelAttr),
                List.of(),
                privateLookup.findConstructor(TestCcEntity.class,
                        MethodType.methodType(void.class)));

        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        PersistenceUnitInfo fakeInfo =
                makeFakeInfo(List.of(TestCcEntity.class.getName()),
                        dataSource);

        var entityModelMap = Map.of(TestCcEntity.class, model);
        var hints = Map.of("mansart.entityModels", entityModelMap);

        emf = provider.createContainerEntityManagerFactory(fakeInfo, hints);
        em = (MansartEntityManager) emf.createEntityManager();
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

    // -- contains() tests -----------------------------------------------

    /**
     * contains() returns true for a persisted (managed) entity.
     */
    @Test
    void containsManagedEntityTest() {
        TestCcEntity entity = new TestCcEntity("c1", "contained");
        em.persist(entity);
        assertTrue(em.contains(entity),
                "contains() must return true for a managed entity");
    }

    /**
     * contains() returns false for a new (unsaved, non-managed) entity.
     */
    @Test
    void containsNonManagedEntityTest() {
        TestCcEntity entity = new TestCcEntity("c2", "not-contained");
        assertFalse(em.contains(entity),
                "contains() must return false for a new (non-managed) entity");
    }

    // -- clear() tests ---------------------------------------------------

    /**
     * clear() empties the persistence context: entities are no longer
     * contained after clear().
     */
    @Test
    void clearEmptiesContextTest() {
        TestCcEntity entity = new TestCcEntity("c3", "before-clear");
        em.persist(entity);
        assertTrue(em.contains(entity),
                "Entity must be contained before clear()");

        em.clear();
        assertFalse(em.contains(entity),
                "Entity must not be contained after clear()");
    }

    /**
     * clear() on an empty context is a no-op (does not throw).
     */
    @Test
    void clearEmptyContextTest() {
        assertDoesNotThrow(() -> em.clear(),
                "clear() on an empty context must not throw");
    }

    // -- post-close behaviour --------------------------------------------

    /**
     * clear() after close() → IllegalStateException.
     */
    @Test
    void clearAfterCloseTest() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.clear(),
                "clear() after close() must throw IllegalStateException");
    }

    /**
     * contains() after close() → IllegalStateException.
     */
    @Test
    void containsAfterCloseTest() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.contains(
                new TestCcEntity("c4", "x")),
                "contains() after close() must throw IllegalStateException");
    }

    // -- helper: DB checks via JDBC ---------------------------------------

    private boolean rowExistsInDb(String id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM CC_ENTITY WHERE ID = ?")) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            throw new RuntimeException("DB check failed", e);
        }
    }

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

    @jakarta.persistence.Entity
    @jakarta.persistence.Table(name = "CC_ENTITY")
    static class TestCcEntity {

        @jakarta.persistence.Id
        private String id;
        private String label;

        TestCcEntity() {
        }

        TestCcEntity(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }
}
