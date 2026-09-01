/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-lateral
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
 * Unit tests for {@code EntityManager.refresh(Object)}.
 *
 * <p>Covers: basic refresh (state re-read from DB), refresh-after-close,
 * and null-entity edge case.</p>
 */
class RefreshTest {

    private JdbcDataSource dataSource;
    private EntityManagerFactory emf;
    private EntityManager em;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:refreshTest" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1");

        // Create the test table.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE REFRESH_TEST ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "NAME VARCHAR(255), "
                            + "PRIMARY KEY (ID))");
        }

        // Build an EntityModel with MethodHandles for getters/setters.
        var lookup = MethodHandles.lookup();
        var privateLookup = MethodHandles.privateLookupIn(RefreshTestEntity.class,
                lookup);
        MethodHandle idGetter = privateLookup.findGetter(
                RefreshTestEntity.class, "id", String.class);
        MethodHandle idSetter = privateLookup.findSetter(
                RefreshTestEntity.class, "id", String.class);
        MethodHandle nameGetter = privateLookup.findGetter(
                RefreshTestEntity.class, "name", String.class);
        MethodHandle nameSetter = privateLookup.findSetter(
                RefreshTestEntity.class, "name", String.class);

        var idAttr = new IdAttribute<RefreshTestEntity, String>(
                "id", "ID", String.class, RefreshTestEntity.class,
                false, idGetter, idSetter);
        var nameAttr = new TextAttribute<RefreshTestEntity>(
                "name", "NAME", RefreshTestEntity.class,
                true, false, 255, nameGetter, nameSetter);

        var model = new EntityModel<RefreshTestEntity>(
                RefreshTestEntity.class, "REFRESH_TEST", null,
                idAttr, java.util.Optional.empty(),
                List.of(idAttr, nameAttr),
                List.of(),
                privateLookup.findConstructor(RefreshTestEntity.class,
                        MethodType.methodType(void.class)));

        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        var fakeInfo = makeFakeInfo(List.of(RefreshTestEntity.class.getName()),
                dataSource);
        var entityModelMap = Map.of(RefreshTestEntity.class, model);
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
    void refreshOverwritesManagedEntityStateFromDB() throws Exception {
        // Insert initial row: id=ref001, name="Original".
        try (Connection conn = dataSource.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO REFRESH_TEST (ID, NAME) VALUES (?, ?)")) {
            stmt.setString(1, "ref001");
            stmt.setString(2, "Original");
            stmt.executeUpdate();
        }

        // Find the entity — it is now managed with name="Original".
        var entity = em.find(RefreshTestEntity.class, "ref001");
        assertThat(entity).isNotNull();
        assertThat(entity.getName()).isEqualTo("Original");

        // Update the DB row directly (bypassing EntityManager).
        try (Connection conn = dataSource.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE REFRESH_TEST SET NAME = ? WHERE ID = ?")) {
            stmt.setString(1, "Refreshed");
            stmt.setString(2, "ref001");
            stmt.executeUpdate();
        }

        // Refresh should re-read the DB and overwrite the managed state.
        em.refresh(entity);
        assertThat(entity.getName()).isEqualTo("Refreshed");
    }

    @Test
    void refreshAfterCloseThrowsIllegalStateException() {
        em.close();
        assertThatThrownBy(() -> em.refresh(new RefreshTestEntity()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("EntityManager is closed");
    }

    @Test
    void refreshWithNullThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> em.refresh(null))
                .isInstanceOf(IllegalArgumentException.class);
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
            @Override public List<String> getQualifierAnnotationNames() {
                return List.of(); }
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
            @Override public void addTransformer(jakarta.persistence.spi.ClassTransformer t) { }
        };
    }
}
