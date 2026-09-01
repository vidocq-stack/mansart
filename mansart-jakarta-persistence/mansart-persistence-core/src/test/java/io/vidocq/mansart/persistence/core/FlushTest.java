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
import jakarta.persistence.FlushModeType;
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
 * Unit tests for {@code EntityManager.flush()}, {@code getFlushMode()},
 * and {@code setFlushMode()}.
 *
 * <p>Tests the flush ordering behaviour: persist → flush → DB row exists.
 * Also covers post-close IllegalStateException and flush mode lifecycle.</p>
 */
class FlushTest {

    private JdbcDataSource dataSource;
    private EntityManagerFactory emf;
    private MansartEntityManager em;

    @BeforeEach
    void setUp() throws Exception {
        // Create H2 in-memory datasource with unique name per test class.
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:flushTest" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1");

        // Create the test table.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE FLUSH_ENTITY ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "NAME VARCHAR(255), "
                            + "PRIMARY KEY (ID))");
        }

        // Build an EntityModel with MethodHandles for getters/setters.
        var lookup = MethodHandles.lookup();
        var privateLookup =
                MethodHandles.privateLookupIn(TestFlushEntity.class, lookup);
        MethodHandle idGetter = privateLookup.findGetter(
                TestFlushEntity.class, "id", String.class);
        MethodHandle idSetter = privateLookup.findSetter(
                TestFlushEntity.class, "id", String.class);
        MethodHandle nameGetter = privateLookup.findGetter(
                TestFlushEntity.class, "name", String.class);
        MethodHandle nameSetter = privateLookup.findSetter(
                TestFlushEntity.class, "name", String.class);

        var idAttr = new io.vidocq.mansart.data.dialect.attribute.IdAttribute<
                        TestFlushEntity, String>(
                "id", "ID", String.class, TestFlushEntity.class,
                false, idGetter, idSetter);
        var nameAttr = new io.vidocq.mansart.data.dialect.attribute.TextAttribute<
                        TestFlushEntity>(
                "name", "NAME", TestFlushEntity.class,
                true, false, 255, nameGetter, nameSetter);

        var model = new io.vidocq.mansart.data.dialect.EntityModel<
                TestFlushEntity>(
                TestFlushEntity.class, "FLUSH_ENTITY", null,
                idAttr, java.util.Optional.empty(),
                List.of(idAttr, nameAttr),
                List.of(),
                privateLookup.findConstructor(TestFlushEntity.class,
                        MethodType.methodType(void.class)));

        // Build the factory.
        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        PersistenceUnitInfo fakeInfo =
                makeFakeInfo(List.of(TestFlushEntity.class.getName()),
                        dataSource);

        var entityModelMap = Map.of(TestFlushEntity.class, model);
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

    // -- flush() basic behaviour ------------------------------------------

    /**
     * Flush after persist: flush() must not throw and must not remove
     * the row that persist() already created.
     */
    @Test
    void flushAfterPersistTest() throws Exception {
        TestFlushEntity entity = new TestFlushEntity("f1", "flush-one");
        em.persist(entity);
        // persist() already writes the INSERT — flush() must be a no-op
        // (or re-apply the same INSERT) without removing the row.
        em.flush();
        assertTrue(rowExistsInDb("f1"),
                "Row must still exist in DB after flush()");
    }

    /**
     * Flush on an entity already in the DB: re-applies current state via UPDATE.
     */
    @Test
    void flushUpdatesExistingEntityTest() throws Exception {
        TestFlushEntity entity = new TestFlushEntity("f2", "flush-two");
        em.persist(entity);
        // Verify the row exists in DB after persist+flush.
        assertTrue(rowExistsInDb("f2"));

        // Modify and flush again.
        entity.setName("flush-two-updated");
        em.flush();
        assertTrue(rowExistsInDb("f2"),
                "Row must exist after flush on existing entity");
        // Verify the updated name is in DB.
        String name = getDbName("f2");
        assertEquals("flush-two-updated", name);
    }

    /**
     * Flush on a new (unsaved) entity: no-op per spec — nothing to flush.
     */
    @Test
    void flushNewEntityTest() throws Exception {
        TestFlushEntity entity = new TestFlushEntity("f3", "flush-three");
        // Do NOT persist — just flush.
        // Per JPA spec, flush on a new entity is a no-op (nothing to sync).
        em.flush();
        assertFalse(rowExistsInDb("f3"),
                "New (unsaved) entity must not appear in DB after flush");
    }

    // -- post-close behaviour ---------------------------------------------

    /**
     * Flush after close() → IllegalStateException.
     */
    @Test
    void flushAfterCloseTest() {
        em.close();
        assertThrows(IllegalStateException.class, () -> em.flush(),
                "flush() after close() must throw IllegalStateException");
    }

    // -- flush mode -------------------------------------------------------

    /**
     * Default flush mode is AUTO.
     */
    @Test
    void defaultFlushModeIsAutoTest() {
        assertEquals(FlushModeType.AUTO, em.getFlushMode(),
                "Default flush mode must be AUTO");
    }

    /**
     * setFlushMode(COMMIT) then getFlushMode() returns COMMIT.
     */
    @Test
    void setFlushModeCommitTest() {
        em.setFlushMode(FlushModeType.COMMIT);
        assertEquals(FlushModeType.COMMIT, em.getFlushMode(),
                "getFlushMode() must return the value set by setFlushMode()");
    }

    /**
     * setFlushMode(AUTO) after COMMIT → returns AUTO.
     */
    @Test
    void setFlushModeAutoTest() {
        em.setFlushMode(FlushModeType.COMMIT);
        em.setFlushMode(FlushModeType.AUTO);
        assertEquals(FlushModeType.AUTO, em.getFlushMode(),
                "getFlushMode() must reflect the latest setFlushMode() call");
    }

    /**
     * Flush after close with setFlushMode → IllegalStateException.
     */
    @Test
    void setFlushModeAfterCloseTest() {
        em.close();
        assertThrows(IllegalStateException.class,
                () -> em.setFlushMode(FlushModeType.COMMIT),
                "setFlushMode() after close() must throw IllegalStateException");
    }

    // -- helper: DB checks via JDBC ---------------------------------------

    private boolean rowExistsInDb(String id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM FLUSH_ENTITY WHERE ID = ?")) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            throw new RuntimeException("DB check failed", e);
        }
    }

    private String getDbName(String id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT NAME FROM FLUSH_ENTITY WHERE ID = ?")) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("NAME") : null;
            }
        } catch (Exception e) {
            throw new RuntimeException("DB name lookup failed", e);
        }
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

    // -- test entity ------------------------------------------------------

    /**
     * Minimal entity for flush tests. Uses the FLUSH_ENTITY table.
     */
    @jakarta.persistence.Entity
    @jakarta.persistence.Table(name = "FLUSH_ENTITY")
    static class TestFlushEntity {

        @jakarta.persistence.Id
        private String id;
        private String name;

        TestFlushEntity() {
        }

        TestFlushEntity(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
