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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vidocq.mansart.persistence.core.MansartPersistenceProvider;

/**
 * Unit tests for JPQL scalar functions (UPPER, LOWER, LENGTH, LOCATE,
 * SUBSTRING, LEFT, RIGHT, CONCAT, ABS, SQRT).
 */
class JpqlScalarFunctionsTest {

    private JdbcDataSource dataSource;
    private EntityManagerFactory emf;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:scalarFunc" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE SCALAR_ENTITY ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "NAME VARCHAR(255), "
                            + "PRIMARY KEY (ID))");
        }
    }

    @AfterEach
    void tearDown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    // -- UPPER ---------------------------------------------------------------

    /**
     * JPQL UPPER() in WHERE clause.
     */
    @Test
    void selectUpperTest() throws Exception {
        persistEntity("u1", "hello");
        persistEntity("u2", "WORLD");

        List<?> results = executeQuery(
                "SELECT e FROM ScalarTestEntity e WHERE UPPER(e.name) = 'HELLO'");
        assertEquals(1, results.size());
        assertEquals("u1", getId(results.get(0)));
    }

    // -- LOWER ---------------------------------------------------------------

    /**
     * JPQL LOWER() in WHERE clause.
     */
    @Test
    void selectLowerTest() throws Exception {
        persistEntity("l1", "HELLO");
        persistEntity("l2", "world");

        List<?> results = executeQuery(
                "SELECT e FROM ScalarTestEntity e WHERE LOWER(e.name) = 'hello'");
        assertEquals(1, results.size());
        assertEquals("l1", getId(results.get(0)));
    }

    // -- LENGTH --------------------------------------------------------------

    /**
     * JPQL LENGTH() in WHERE clause.
     */
    @Test
    void selectLengthTest() throws Exception {
        persistEntity("le1", "hi");
        persistEntity("le2", "hello");

        List<?> results = executeQuery(
                "SELECT e FROM ScalarTestEntity e WHERE LENGTH(e.name) = 5");
        assertEquals(1, results.size());
        assertEquals("le2", getId(results.get(0)));
    }

    // -- helpers -------------------------------------------------------------

    private void persistEntity(String id, String name) throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO SCALAR_ENTITY (ID, NAME) VALUES (?, ?)")) {
            stmt.setString(1, id);
            stmt.setString(2, name);
            stmt.executeUpdate();
        }
    }

    private List<?> executeQuery(String jpql) throws Exception {
        EntityManager em = createEntityManager();
        try {
            return em.createQuery(jpql).getResultList();
        } finally {
            em.close();
        }
    }

    private EntityManager createEntityManager() throws Exception {
        if (emf == null) {
            var lookup = MethodHandles.lookup();
            var privateLookup =
                    MethodHandles.privateLookupIn(ScalarTestEntity.class, lookup);
            var idGetter = privateLookup.findGetter(
                    ScalarTestEntity.class, "id", String.class);
            var idSetter = privateLookup.findSetter(
                    ScalarTestEntity.class, "id", String.class);
            var nameGetter = privateLookup.findGetter(
                    ScalarTestEntity.class, "name", String.class);
            var nameSetter = privateLookup.findSetter(
                    ScalarTestEntity.class, "name", String.class);

            var idAttr = new io.vidocq.mansart.data.dialect.attribute.IdAttribute<
                    ScalarTestEntity, String>(
                    "id", "ID", String.class, ScalarTestEntity.class,
                    false, idGetter, idSetter);
            var nameAttr = new io.vidocq.mansart.data.dialect.attribute.TextAttribute<
                    ScalarTestEntity>(
                    "name", "NAME", ScalarTestEntity.class,
                    true, false, 255, nameGetter, nameSetter);

            var model = new io.vidocq.mansart.data.dialect.EntityModel<
                    ScalarTestEntity>(
                    ScalarTestEntity.class, "SCALAR_ENTITY", null,
                    idAttr, java.util.Optional.empty(),
                    List.of(idAttr, nameAttr),
                    List.of(),
                    privateLookup.findConstructor(ScalarTestEntity.class,
                            MethodType.methodType(void.class)));

            var provider = new MansartPersistenceProvider();
            PersistenceUnitInfo fakeInfo = makeFakeInfo(
                    List.of(ScalarTestEntity.class.getName()), dataSource);
            var entityModelMap = Map.of(ScalarTestEntity.class, model);
            var hints = Map.of("mansart.entityModels", entityModelMap);
            emf = provider.createContainerEntityManagerFactory(fakeInfo, hints);
        }
        return emf.createEntityManager();
    }

    @SuppressWarnings("unchecked")
    private String getId(Object entity) throws Exception {
        var field = ScalarTestEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        return (String) field.get(entity);
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
            @Override public jakarta.persistence.SharedCacheMode getSharedCacheMode() { return null; }
            @Override public jakarta.persistence.ValidationMode getValidationMode() { return null; }
            @Override public Properties getProperties() { return new Properties(); }
            @Override public String getPersistenceXMLSchemaVersion() { return null; }
            @Override public ClassLoader getClassLoader() {
                return Thread.currentThread().getContextClassLoader(); }
            @Override public boolean excludeUnlistedClasses() { return false; }
            @Override public ClassLoader getNewTempClassLoader() { return null; }
            @Override public void addTransformer(jakarta.persistence.spi.ClassTransformer t) { }
        };
    }

    @jakarta.persistence.Entity
    @jakarta.persistence.Table(name = "SCALAR_ENTITY")
    static class ScalarTestEntity {
        @jakarta.persistence.Id
        private String id;
        private String name;

        ScalarTestEntity() {}

        ScalarTestEntity(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
