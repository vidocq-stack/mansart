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

import io.vidocq.mansart.persistence.core.MansartPersistenceProvider;

/**
 * Unit tests for {@code Query.setMaxResults} / {@code setFirstResult}
 * — JPQL pagination via LIMIT/OFFSET.
 */
class JpqlPaginationTest {

    private JdbcDataSource dataSource;
    private EntityManagerFactory emf;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:jpqlPag" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE PAG_ENTITY ("
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

    // -- setMaxResults --

    /**
     * setMaxResults(2): return at most 2 entities.
     */
    @Test
    void setMaxResultsTest() throws Exception {
        persistEntity("a1", "alpha");
        persistEntity("a2", "beta");
        persistEntity("a3", "charlie");

        List<?> results = executeQuery("SELECT e FROM PagEntity e")
                .setMaxResults(2)
                .getResultList();
        assertEquals(2, results.size(),
                "setMaxResults(2) returns at most 2 entities");
    }

    /**
     * setMaxResults(0): return no entities.
     */
    @Test
    void setMaxResultsZeroTest() throws Exception {
        persistEntity("z1", "alpha");
        persistEntity("z2", "beta");

        List<?> results = executeQuery("SELECT e FROM PagEntity e")
                .setMaxResults(0)
                .getResultList();
        assertTrue(results.isEmpty(),
                "setMaxResults(0) returns empty list");
    }

    /**
     * setMaxResults(-1): IllegalArgumentException.
     */
    @Test
    void setMaxResultsIllegalArgumentExceptionTest() throws Exception {
        try {
            executeQuery("SELECT e FROM PagEntity e").setMaxResults(-1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    // -- setFirstResult --

    /**
     * setFirstResult(1): skip first entity, return rest.
     */
    @Test
    void setFirstResultTest() throws Exception {
        persistEntity("f1", "first");
        persistEntity("f2", "second");
        persistEntity("f3", "third");

        List<?> results = executeQuery("SELECT e FROM PagEntity e ORDER BY e.id")
                .setFirstResult(1)
                .getResultList();
        assertEquals(2, results.size(),
                "setFirstResult(1) skips first entity");
        assertEquals("f2", getId(results.get(0)));
        assertEquals("f3", getId(results.get(1)));
    }

    /**
     * setFirstResult(-1): IllegalArgumentException.
     */
    @Test
    void setFirstResultIllegalArgumentExceptionTest() throws Exception {
        try {
            executeQuery("SELECT e FROM PagEntity e").setFirstResult(-1);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    // -- getFirstResult / getMaxResults --

    /**
     * getFirstResult() / getMaxResults() return current values.
     */
    @Test
    void getFirstResultMaxResultsTest() throws Exception {
        jakarta.persistence.Query query = executeQuery("SELECT e FROM PagEntity e");
        assertEquals(0, query.getFirstResult(), "Default firstResult is 0");
        assertEquals(Integer.MAX_VALUE, query.getMaxResults(), "Default maxResults is Integer.MAX_VALUE");

        query.setFirstResult(5).setMaxResults(10);
        assertEquals(5, query.getFirstResult(), "getFirstResult returns set value");
        assertEquals(10, query.getMaxResults(), "getMaxResults returns set value");
    }

    // -- helpers --

    private void persistEntity(String id, String name) throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO PAG_ENTITY (ID, NAME) VALUES (?, ?)")) {
            stmt.setString(1, id);
            stmt.setString(2, name);
            stmt.executeUpdate();
        }
    }

    private jakarta.persistence.Query executeQuery(String jpql) throws Exception {
        EntityManager em = createEntityManager();
        return em.createQuery(jpql);
    }

    private EntityManager createEntityManager() throws Exception {
        if (emf == null) {
            var lookup = MethodHandles.lookup();
            var privateLookup =
                    MethodHandles.privateLookupIn(PagEntity.class, lookup);
            var idGetter = privateLookup.findGetter(
                    PagEntity.class, "id", String.class);
            var idSetter = privateLookup.findSetter(
                    PagEntity.class, "id", String.class);
            var nameGetter = privateLookup.findGetter(
                    PagEntity.class, "name", String.class);
            var nameSetter = privateLookup.findSetter(
                    PagEntity.class, "name", String.class);

            var idAttr = new io.vidocq.mansart.data.dialect.attribute.IdAttribute<
                    PagEntity, String>(
                    "id", "ID", String.class, PagEntity.class,
                    false, idGetter, idSetter);
            var nameAttr = new io.vidocq.mansart.data.dialect.attribute.TextAttribute<
                    PagEntity>(
                    "name", "NAME", PagEntity.class,
                    true, false, 255, nameGetter, nameSetter);

            var model = new io.vidocq.mansart.data.dialect.EntityModel<
                    PagEntity>(
                    PagEntity.class, "PAG_ENTITY", null,
                    idAttr, java.util.Optional.empty(),
                    List.of(idAttr, nameAttr),
                    List.of(),
                    privateLookup.findConstructor(PagEntity.class,
                            MethodType.methodType(void.class)));

            var provider = new MansartPersistenceProvider();
            PersistenceUnitInfo fakeInfo = makeFakeInfo(
                    List.of(PagEntity.class.getName()), dataSource);
            var entityModelMap = Map.of(PagEntity.class, model);
            var hints = Map.of("mansart.entityModels", entityModelMap);
            emf = provider.createContainerEntityManagerFactory(fakeInfo, hints);
        }
        return emf.createEntityManager();
    }

    @SuppressWarnings("unchecked")
    private String getId(Object entity) throws Exception {
        var field = PagEntity.class.getDeclaredField("id");
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
    @jakarta.persistence.Table(name = "PAG_ENTITY")
    static class PagEntity {
        @jakarta.persistence.Id
        private String id;
        private String name;

        PagEntity() {}

        PagEntity(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
