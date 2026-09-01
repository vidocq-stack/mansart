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
 * Unit test for {@code MansartEntityManager.merge(Object)}.
 *
 * <p>Verifies that merging a detached entity re-attaches it to the
 * persistence context and updates the database.</p>
 */
class MergeTest {

    private JdbcDataSource dataSource;
    private EntityManagerFactory emf;
    private EntityManager em;

    @BeforeEach
    void setUp() throws Exception {
        // Create H2 in-memory datasource with unique name per test class.
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:mergeTest" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");

        // Create the test table.
        try (Connection conn = dataSource.getConnection();
              Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE MERGE_TEST ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "NAME VARCHAR(255), "
                            + "PRIMARY KEY (ID))");
        }

        // Build an EntityModel with MethodHandles for getters/setters.
        var lookup = MethodHandles.lookup();
        var privateLookup = MethodHandles.privateLookupIn(MergeTestEntity.class, lookup);
        MethodHandle idGetter = privateLookup.findGetter(
                MergeTestEntity.class, "id", String.class);
        MethodHandle idSetter = privateLookup.findSetter(
                MergeTestEntity.class, "id", String.class);
        MethodHandle nameGetter = privateLookup.findGetter(
                MergeTestEntity.class, "name", String.class);
        MethodHandle nameSetter = privateLookup.findSetter(
                MergeTestEntity.class, "name", String.class);

        var idAttr = new IdAttribute<MergeTestEntity, String>(
                "id", "ID", String.class, MergeTestEntity.class,
                false, idGetter, idSetter);
        var nameAttr = new io.vidocq.mansart.data.dialect.attribute.TextAttribute<MergeTestEntity>(
                "name", "NAME", MergeTestEntity.class,
                true, false, 255, nameGetter, nameSetter);

        var model = new EntityModel<MergeTestEntity>(
                MergeTestEntity.class, "MERGE_TEST", null,
                idAttr, java.util.Optional.empty(),
                List.of(idAttr, nameAttr),
                List.of(),
                privateLookup.findConstructor(MergeTestEntity.class,
                        MethodType.methodType(void.class)));

        // Build the factory.
        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        PersistenceUnitInfo fakeInfo =
                makeFakeInfo(List.of(MergeTestEntity.class.getName()),
                        dataSource);

        // Build a map containing our manually created EntityModel.
        var entityModelMap = Map.of(MergeTestEntity.class, model);
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
    void mergeNewEntityThrowsIllegalStateException() {
        // Merge on a closed EM should throw IllegalStateException.
        em.close();
        assertThatThrownBy(() -> em.merge(new MergeTestEntity()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("EntityManager is closed");
    }

    @Test
    void mergeNullEntityThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> em.merge(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mergeDetachedEntityReAttachesAndUpdatesDb() throws Exception {
        // Step 1: persist a new entity.
        MergeTestEntity entity = new MergeTestEntity();
        entity.setId("merge-1");
        entity.setName("Original Name");
        em.persist(entity);

        // Verify the entity is managed and in the DB.
        assertThat(em.contains(entity)).isTrue();
        verifyDbName("merge-1", "Original Name");

        // Step 2: close the EM — entity becomes detached.
        em.close();

        // Step 3: modify the detached entity.
        entity.setName("Updated Name");

        // Step 4: open a new EM and merge.
        EntityManager newEm = emf.createEntityManager();
        try {
            MergeTestEntity merged = newEm.merge(entity);

            // The returned entity should be managed.
            assertThat(newEm.contains(merged)).isTrue();

            // The returned entity should NOT be the same instance as the detached one.
            assertThat(merged).isNotSameAs(entity);

            // The DB should reflect the updated name.
            verifyDbName("merge-1", "Updated Name");
        } finally {
            newEm.close();
        }
    }

    @Test
    void mergeManagedEntityReturnsItSelf() throws Exception {
        // Persist and merge a managed entity — merge should return the same instance.
        MergeTestEntity entity = new MergeTestEntity();
        entity.setId("merge-2");
        entity.setName("Managed Name");
        em.persist(entity);

        MergeTestEntity merged = em.merge(entity);
        assertThat(merged).isSameAs(entity);
        assertThat(em.contains(merged)).isTrue();
    }

    /**
     * Verify the name stored in the database for a given ID.
     */
    private void verifyDbName(String id, String expectedName) throws Exception {
        try (Connection conn = dataSource.getConnection();
              Statement stmt = conn.createStatement();
              ResultSet rs = stmt.executeQuery(
                      "SELECT NAME FROM MERGE_TEST WHERE ID = '" + id + "'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("NAME")).isEqualTo(expectedName);
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
