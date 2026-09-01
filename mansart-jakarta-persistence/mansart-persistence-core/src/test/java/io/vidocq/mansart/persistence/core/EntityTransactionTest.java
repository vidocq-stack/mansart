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
 * Unit tests for {@code EntityManagerTransaction} lifecycle:
 * begin, commit, rollback, isActive, getRollbackOnly, setRollbackOnly.
 *
 * <p>Tests the full transaction state machine for resource-local
 * transactions: begin → commit, begin → rollback,
 * isActive during/after transaction, rollback-only flag.</p>
 */
class EntityTransactionTest {

    private JdbcDataSource dataSource;
    private EntityManagerFactory emf;
    private MansartEntityManager em;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:txTest" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE TX_ENTITY ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "NAME VARCHAR(255), "
                            + "PRIMARY KEY (ID))");
        }

        var lookup = MethodHandles.lookup();
        var privateLookup =
                MethodHandles.privateLookupIn(TestTxEntity.class, lookup);
        MethodHandle idGetter = privateLookup.findGetter(
                TestTxEntity.class, "id", String.class);
        MethodHandle idSetter = privateLookup.findSetter(
                TestTxEntity.class, "id", String.class);
        MethodHandle nameGetter = privateLookup.findGetter(
                TestTxEntity.class, "name", String.class);
        MethodHandle nameSetter = privateLookup.findSetter(
                TestTxEntity.class, "name", String.class);

        var idAttr = new io.vidocq.mansart.data.dialect.attribute.IdAttribute<
                        TestTxEntity, String>(
                "id", "ID", String.class, TestTxEntity.class,
                false, idGetter, idSetter);
        var nameAttr = new io.vidocq.mansart.data.dialect.attribute.TextAttribute<
                        TestTxEntity>(
                "name", "NAME", TestTxEntity.class,
                true, false, 255, nameGetter, nameSetter);

        var model = new io.vidocq.mansart.data.dialect.EntityModel<
                TestTxEntity>(
                TestTxEntity.class, "TX_ENTITY", null,
                idAttr, java.util.Optional.empty(),
                List.of(idAttr, nameAttr),
                List.of(),
                privateLookup.findConstructor(TestTxEntity.class,
                        MethodType.methodType(void.class)));

        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        PersistenceUnitInfo fakeInfo =
                makeFakeInfo(List.of(TestTxEntity.class.getName()),
                        dataSource);

        var entityModelMap = Map.of(TestTxEntity.class, model);
        var hints = Map.of("mansart.entityModels", entityModelMap);

        emf = provider.createContainerEntityManagerFactory(fakeInfo, hints);
        em = (MansartEntityManager) emf.createEntityManager();
    }

    @AfterEach
    void tearDown() {
        try { if (em != null && em.isOpen()) em.close(); } catch (Exception ignored) {}
        try { if (emf != null && emf.isOpen()) emf.close(); } catch (Exception ignored) {}
    }

    // -- isActive() -------------------------------------------------------

    /**
     * isActive() returns false before begin().
     */
    @Test
    void isActiveReturnsFalseBeforeBegin() {
        assertFalse(em.getTransaction().isActive(),
                "isActive() must be false before begin()");
    }

    /**
     * isActive() returns true after begin().
     */
    @Test
    void isActiveReturnsTrueAfterBegin() {
        em.getTransaction().begin();
        try {
            assertTrue(em.getTransaction().isActive(),
                    "isActive() must be true during transaction");
        } finally {
            em.getTransaction().rollback();
        }
    }

    /**
     * isActive() returns false after commit().
     */
    @Test
    void isActiveReturnsFalseAfterCommit() {
        em.getTransaction().begin();
        em.getTransaction().commit();
        assertFalse(em.getTransaction().isActive(),
                "isActive() must be false after commit()");
    }

    /**
     * isActive() returns false after rollback().
     */
    @Test
    void isActiveReturnsFalseAfterRollback() {
        em.getTransaction().begin();
        em.getTransaction().rollback();
        assertFalse(em.getTransaction().isActive(),
                "isActive() must be false after rollback()");
    }

    // -- begin() ----------------------------------------------------------

    /**
     * begin() starts a new transaction.
     */
    @Test
    void beginStartsTransaction() {
        em.getTransaction().begin();
        assertTrue(em.getTransaction().isActive());
        em.getTransaction().rollback();
    }

    /**
     * begin() on an already-active transaction throws IllegalStateException.
     */
    @Test
    void beginOnActiveTransactionThrowsIllegalStateException() {
        em.getTransaction().begin();
        try {
            assertThrows(IllegalStateException.class,
                    () -> em.getTransaction().begin(),
                    "begin() on active transaction must throw IllegalStateException");
        } finally {
            em.getTransaction().rollback();
        }
    }

    /**
     * begin() on a closed EntityManager throws IllegalStateException.
     */
    @Test
    void beginOnClosedEntityManagerThrowsIllegalStateException() {
        em.close();
        assertThrows(IllegalStateException.class,
                () -> em.getTransaction().begin(),
                "begin() on closed EM must throw IllegalStateException");
    }

    // -- commit() ---------------------------------------------------------

    /**
     * commit() finalises a transaction.
     */
    @Test
    void commitFinalisesTransaction() {
        em.getTransaction().begin();
        em.persist(new TestTxEntity("tx1", "commit-test"));
        em.getTransaction().commit();
        assertFalse(em.getTransaction().isActive());
    }

    /**
     * commit() on a non-active transaction throws IllegalStateException.
     */
    @Test
    void commitOnNonActiveTransactionThrowsIllegalStateException() {
        assertThrows(IllegalStateException.class,
                () -> em.getTransaction().commit(),
                "commit() on non-active transaction must throw IllegalStateException");
    }

    /**
     * commit() on a closed EntityManager throws IllegalStateException.
     */
    @Test
    void commitOnClosedEntityManagerThrowsIllegalStateException() {
        em.close();
        assertThrows(IllegalStateException.class,
                () -> em.getTransaction().commit(),
                "commit() on closed EM must throw IllegalStateException");
    }

    // -- rollback() -------------------------------------------------------

    /**
     * rollback() aborts a transaction and rolls back DB changes.
     */
    @Test
    void rollbackAbortsTransaction() throws java.sql.SQLException {
        em.getTransaction().begin();
        em.persist(new TestTxEntity("tx2", "rollback-test"));
        em.getTransaction().rollback();
        assertFalse(em.getTransaction().isActive());
        // Verify the row was NOT written to DB.
        assertFalse(rowExistsInDb("tx2"),
                "Rollback must not persist entity to DB");
    }

    /**
     * rollback() on a non-active transaction throws IllegalStateException.
     */
    @Test
    void rollbackOnNonActiveTransactionThrowsIllegalStateException() {
        assertThrows(IllegalStateException.class,
                () -> em.getTransaction().rollback(),
                "rollback() on non-active transaction must throw IllegalStateException");
    }

    /**
     * rollback() on a closed EntityManager throws IllegalStateException.
     */
    @Test
    void rollbackOnClosedEntityManagerThrowsIllegalStateException() {
        em.close();
        assertThrows(IllegalStateException.class,
                () -> em.getTransaction().rollback(),
                "rollback() on closed EM must throw IllegalStateException");
    }

    // -- getRollbackOnly() / setRollbackOnly() ----------------------------

    /**
     * getRollbackOnly() returns false before setRollbackOnly().
     */
    @Test
    void getRollbackOnlyReturnsFalseBeforeSet() {
        em.getTransaction().begin();
        try {
            assertFalse(em.getTransaction().getRollbackOnly(),
                    "getRollbackOnly() must be false initially");
        } finally {
            em.getTransaction().rollback();
        }
    }

    /**
     * setRollbackOnly() marks the transaction for rollback.
     */
    @Test
    void setRollbackOnlyMarksTransaction() {
        em.getTransaction().begin();
        try {
            em.getTransaction().setRollbackOnly();
            assertTrue(em.getTransaction().getRollbackOnly(),
                    "getRollbackOnly() must return true after setRollbackOnly()");
        } finally {
            em.getTransaction().rollback();
        }
    }

    /**
     * commit() on a rollback-only transaction throws RollbackException.
     */
    @Test
    void commitOnRollbackOnlyThrowsRollbackException() {
        em.getTransaction().begin();
        try {
            em.getTransaction().setRollbackOnly();
            assertThrows(jakarta.persistence.RollbackException.class,
                    () -> em.getTransaction().commit(),
                    "commit() on rollback-only must throw RollbackException");
        } finally {
            em.getTransaction().rollback();
        }
    }

    /**
     * setRollbackOnly() on a non-active transaction throws IllegalStateException.
     */
    @Test
    void setRollbackOnlyOnNonActiveThrowsIllegalStateException() {
        assertThrows(IllegalStateException.class,
                () -> em.getTransaction().setRollbackOnly(),
                "setRollbackOnly() on non-active transaction must throw IllegalStateException");
    }

    /**
     * setRollbackOnly() on a closed EntityManager throws IllegalStateException.
     */
    @Test
    void setRollbackOnlyOnClosedEntityManagerThrowsIllegalStateException() {
        em.close();
        assertThrows(IllegalStateException.class,
                () -> em.getTransaction().setRollbackOnly(),
                "setRollbackOnly() on closed EM must throw IllegalStateException");
    }

    // -- persistence verification -----------------------------------------

    /**
     * A persist inside a committed transaction is visible in DB.
     */
    @Test
    void commitPersistsEntityToDb() throws java.sql.SQLException {
        em.getTransaction().begin();
        em.persist(new TestTxEntity("tx3", "commit-persist"));
        em.getTransaction().commit();
        assertTrue(rowExistsInDb("tx3"),
                "Committed transaction must persist entity to DB");
    }

    // -- helpers ----------------------------------------------------------

    private PersistenceUnitInfo makeFakeInfo(
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

    private boolean rowExistsInDb(String id) throws java.sql.SQLException {
        try (Connection conn = dataSource.getConnection();
             java.sql.ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT ID FROM TX_ENTITY WHERE ID='" + id + "'")) {
            return rs.next();
        }
    }

    // -- test entity ------------------------------------------------------

    @jakarta.persistence.Entity
    @jakarta.persistence.Table(name = "TX_ENTITY")
    static class TestTxEntity {

        @jakarta.persistence.Id
        private String id;
        private String name;

        TestTxEntity() {
        }

        TestTxEntity(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
