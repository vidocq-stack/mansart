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
import java.util.Properties;
import java.net.URL;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test for {@code MansartEntityManager.remove(Object)}.
 *
 * <p>Verifies: new entity (no-op), managed entity (DELETE + unregister),
 * detached entity (find + DELETE + unregister), already-removed (no-op),
 * and post-close behaviour.</p>
 */
class RemoveBasicTest {

    private JdbcDataSource dataSource;
    private EntityManagerFactory emf;
    private EntityManager em;

    @BeforeEach
    void setUp() throws Exception {
        // Create H2 in-memory datasource with unique name per test class.
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:removeTest" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");

        // Create the test table.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE REMOVE_TEST ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "NAME VARCHAR(255), "
                            + "PRIMARY KEY (ID))");
        }

        // Build an EntityModel with MethodHandles for getters/setters.
        var lookup = MethodHandles.lookup();
        var privateLookup = MethodHandles.privateLookupIn(RemoveTestEntity.class, lookup);
        MethodHandle idGetter = privateLookup.findGetter(
                RemoveTestEntity.class, "id", String.class);
        MethodHandle idSetter = privateLookup.findSetter(
                RemoveTestEntity.class, "id", String.class);
        MethodHandle nameGetter = privateLookup.findGetter(
                RemoveTestEntity.class, "name", String.class);
        MethodHandle nameSetter = privateLookup.findSetter(
                RemoveTestEntity.class, "name", String.class);

        var idAttr = new IdAttribute<RemoveTestEntity, String>(
                "id", "ID", String.class, RemoveTestEntity.class,
                false, idGetter, idSetter);
        var nameAttr = new io.vidocq.mansart.data.dialect.attribute.TextAttribute<RemoveTestEntity>(
                "name", "NAME", RemoveTestEntity.class,
                true, false, 255, nameGetter, nameSetter);

        var model = new EntityModel<RemoveTestEntity>(
                RemoveTestEntity.class, "REMOVE_TEST", null,
                idAttr, java.util.Optional.empty(),
                List.of(idAttr, nameAttr),
                List.of(),
                privateLookup.findConstructor(RemoveTestEntity.class,
                        MethodType.methodType(void.class)));

        // Build the factory.
        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        PersistenceUnitInfo fakeInfo =
                makeFakeInfo(List.of(RemoveTestEntity.class.getName()),
                        dataSource);

        // Build a map containing our manually created EntityModel.
        var entityModelMap = Map.of(RemoveTestEntity.class, model);
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

    /**
     * JP-26: remove on a new (not yet persisted) entity is a no-op.
     * Per JPA spec: "If X is a new entity, it is ignored by the remove operation."
     */
    @Test
    void removeNewEntityIsNoOp() throws Exception {
        RemoveTestEntity entity = new RemoveTestEntity();
        entity.setId("new-1");
        entity.setName("New Entity");

        // Persist first so we can verify the DB state.
        em.persist(entity);

        // Get the DB count before remove (should be 1).
        int countBefore = countRows("new-1");

        // Remove the entity that was just persisted (now managed).
        // But then we test: create a brand-new entity that was never persisted.
        RemoveTestEntity neverPersisted = new RemoveTestEntity();
        neverPersisted.setId("new-2");
        neverPersisted.setName("Never Persisted");

        em.remove(neverPersisted);

        // The never-persisted entity should not have been removed from DB
        // (it was never there). The DB count for "new-1" is unchanged.
        int countAfter = countRows("new-1");
        assertThat(countAfter).isEqualTo(countBefore);

        // The never-persisted entity should NOT be in the persistence context.
        assertThat(em.contains(neverPersisted)).isFalse();
    }

    /**
     * JP-26: remove on a managed entity executes DELETE and unregisters it.
     */
    @Test
    void removeManagedEntityDeletesAndUnregisters() throws Exception {
        RemoveTestEntity entity = new RemoveTestEntity();
        entity.setId("managed-1");
        entity.setName("Managed Entity");

        em.persist(entity);

        // Verify it's managed.
        assertThat(em.contains(entity)).isTrue();

        // Verify the DB row exists.
        assertThat(countRows("managed-1")).isEqualTo(1);

        // Remove the managed entity.
        em.remove(entity);

        // Verify the DB row is gone.
        assertThat(countRows("managed-1")).isEqualTo(0);

        // Verify the entity is no longer managed.
        assertThat(em.contains(entity)).isFalse();
    }

    /**
     * JP-26: remove on a detached entity finds it in DB, deletes, and unregisters.
     */
    @Test
    void removeDetachedEntityFindsDeletesAndUnregisters() throws Exception {
        // Insert a row directly into the DB (bypassing persist/flush).
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "INSERT INTO REMOVE_TEST (ID, NAME) VALUES ('detached-1', 'Detached Entity')");
        }

        // Close the EM to simulate a detached entity.
        em.close();
        em = emf.createEntityManager();

        // Create a new instance with the same ID (simulates a detached entity
        // loaded outside this EM's context).
        RemoveTestEntity detached = new RemoveTestEntity();
        detached.setId("detached-1");
        detached.setName("Detached Entity");

        // Remove the detached entity — should find by ID from DB, DELETE, and unregister.
        em.remove(detached);

        // Verify the DB row is gone.
        assertThat(countRows("detached-1")).isEqualTo(0);
    }

    /**
     * JP-26: remove on an already-removed entity is a no-op.
     */
    @Test
    void removeAlreadyRemovedIsNoOp() throws Exception {
        RemoveTestEntity entity = new RemoveTestEntity();
        entity.setId("removed-1");
        entity.setName("Already Removed");

        em.persist(entity);

        // Remove once.
        em.remove(entity);

        // Verify it's gone from DB.
        assertThat(countRows("removed-1")).isEqualTo(0);

        // Remove again — should be a no-op (no exception).
        em.remove(entity);

        // Still gone from DB.
        assertThat(countRows("removed-1")).isEqualTo(0);
    }

    /**
     * JP-26: remove on a closed EntityManager throws IllegalStateException.
     */
    @Test
    void removeAfterCloseThrowsIllegalStateException() {
        em.close();
        assertThatThrownBy(() -> em.remove(new RemoveTestEntity()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("EntityManager is closed");
    }

    /**
     * JP-26: remove(null) throws IllegalArgumentException.
     */
    @Test
    void removeNullEntityThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> em.remove(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Count rows in the REMOVE_TEST table by ID.
     */
    private int countRows(String id) throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) AS cnt FROM REMOVE_TEST WHERE ID = '" + id + "'")) {
            if (rs.next()) {
                return rs.getInt("cnt");
            }
            return 0;
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
}
