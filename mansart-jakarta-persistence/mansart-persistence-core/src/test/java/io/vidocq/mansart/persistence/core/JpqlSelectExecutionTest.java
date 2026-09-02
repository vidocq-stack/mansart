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

import io.vidocq.mansart.persistence.core.MansartPersistenceProvider;

/**
 * Unit tests for {@code EntityManager.createQuery(String)} — basic
 * JPQL SELECT execution: FROM, WHERE clause with literals, ORDER BY.
 */
class JpqlSelectExecutionTest {

    private JdbcDataSource dataSource;
    private EntityManagerFactory emf;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:jpqlTest" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE JPQL_ENTITY ("
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

    // -- SELECT FROM (no WHERE) -------------------------------------------

    /**
     * Simple SELECT: fetch all entities by class, no WHERE clause.
     */
    @Test
    void selectFromTest() throws Exception {
        persistEntity("a1", "alpha");
        persistEntity("a2", "beta");

        List<?> results = executeQuery("SELECT e FROM JpqlTestEntity e");
        assertEquals(2, results.size(),
                "SELECT without WHERE must return all entities");
    }

    // -- SELECT WHERE (literal comparison) ---------------------------------

    /**
     * SELECT with WHERE: name = literal string.
     */
    @Test
    void selectWhereLiteralTest() throws Exception {
        persistEntity("b1", "searched");
        persistEntity("b2", "other");

        List<?> results = executeQuery(
                "SELECT e FROM JpqlTestEntity e WHERE e.name = 'searched'");
        assertEquals(1, results.size(),
                "WHERE with exact string match returns one entity");
        String id = getId(results.get(0));
        assertEquals("b1", id);
    }

    /**
     * SELECT with WHERE: name = positional parameter.
     */
    @Test
    void selectWherePositionalParamTest() throws Exception {
        persistEntity("c1", "param-test");
        persistEntity("c2", "other");

        List<?> results = executeQuery(
                "SELECT e FROM JpqlTestEntity e WHERE e.name = ?1", "param-test");
        assertEquals(1, results.size());
        String id = getId(results.get(0));
        assertEquals("c1", id);
    }

    // -- SELECT ORDER BY ---------------------------------------------------

    /**
     * SELECT with ORDER BY ASC: results sorted by name ascending.
     */
    @Test
    void selectOrderByAscTest() throws Exception {
        persistEntity("o1", "charlie");
        persistEntity("o2", "alpha");
        persistEntity("o3", "bravo");

        List<?> results = executeQuery(
                "SELECT e FROM JpqlTestEntity e ORDER BY e.name ASC");
        assertEquals(3, results.size());
        assertEquals("o2", getId(results.get(0)), "First by ASC name");
        assertEquals("o3", getId(results.get(1)), "Second by ASC name");
        assertEquals("o1", getId(results.get(2)), "Third by ASC name");
    }

    /**
     * SELECT with ORDER BY: ordering by ID.
     */
    @Test
    void selectOrderByNumericTest() throws Exception {
        persistEntity("n1", "first");
        persistEntity("n2", "second");
        persistEntity("n3", "third");

        List<?> results = executeQuery(
                "SELECT e FROM JpqlTestEntity e ORDER BY e.id ASC");
        assertEquals(3, results.size());
        assertEquals("n1", getId(results.get(0)));
        assertEquals("n2", getId(results.get(1)));
        assertEquals("n3", getId(results.get(2)));
    }

    /**
     * SELECT with ORDER BY DESC: results sorted by name descending.
     */
    @Test
    void selectOrderByDescTest() throws Exception {
        persistEntity("d1", "zulu");
        persistEntity("d2", "alpha");

        List<?> results = executeQuery(
                "SELECT e FROM JpqlTestEntity e ORDER BY e.name DESC");
        assertEquals(2, results.size());
        assertEquals("d1", getId(results.get(0)), "First by DESC name");
        assertEquals("d2", getId(results.get(1)), "Second by DESC name");
    }

    // -- SELECT WHERE + ORDER BY -------------------------------------------

    /**
     * SELECT with WHERE and ORDER BY combined.
     */
    @Test
    void selectWhereAndOrderByTest() throws Exception {
        persistEntity("w1", "visible");
        persistEntity("w2", "visible");
        persistEntity("w3", "hidden");

        List<?> results = executeQuery(
                "SELECT e FROM JpqlTestEntity e WHERE e.name = 'visible' ORDER BY e.id ASC");
        assertEquals(2, results.size());
        assertEquals("w1", getId(results.get(0)), "First visible by ID");
        assertEquals("w2", getId(results.get(1)), "Second visible by ID");
    }

    // -- TypedQuery variant ------------------------------------------------

    /**
     * createQuery(String, Class<T>) returns a TypedQuery.
     */
    @Test
    void typedQuerySelectTest() throws Exception {
        persistEntity("t1", "typed");

        // Ensure EMF is initialized
        createEntityManager().close();

        @SuppressWarnings("unchecked")
        List<Object> results = (List<Object>) ((jakarta.persistence.TypedQuery<Object>)
                emf.createEntityManager().createQuery(
                        "SELECT e FROM JpqlTestEntity e", Object.class))
                .getResultList();
        assertEquals(1, results.size());
        assertEquals("t1", getId(results.get(0)));
    }

    // -- helpers ------------------------------------------------------------

    private void persistEntity(String id, String name) throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO JPQL_ENTITY (ID, NAME) VALUES (?, ?)")) {
            stmt.setString(1, id);
            stmt.setString(2, name);
            stmt.executeUpdate();
        }
    }

    /**
     * Build EntityManagerFactory, create an EM, and execute a JPQL query.
     */
    private List<?> executeQuery(String jpql, Object... params) throws Exception {
        EntityManager em = createEntityManager();
        try {
            jakarta.persistence.Query query = em.createQuery(jpql);
            for (int i = 0; i < params.length; i++) {
                query.setParameter(i + 1, params[i]);
            }
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    private EntityManager createEntityManager() throws Exception {
        if (emf == null) {
            var lookup = MethodHandles.lookup();
            var privateLookup =
                    MethodHandles.privateLookupIn(JpqlTestEntity.class, lookup);
            MethodHandle idGetter = privateLookup.findGetter(
                    JpqlTestEntity.class, "id", String.class);
            MethodHandle idSetter = privateLookup.findSetter(
                    JpqlTestEntity.class, "id", String.class);
            MethodHandle nameGetter = privateLookup.findGetter(
                    JpqlTestEntity.class, "name", String.class);
            MethodHandle nameSetter = privateLookup.findSetter(
                    JpqlTestEntity.class, "name", String.class);
            var idAttr = new io.vidocq.mansart.data.dialect.attribute.IdAttribute<
                    JpqlTestEntity, String>(
                    "id", "ID", String.class, JpqlTestEntity.class,
                    false, idGetter, idSetter);
            var nameAttr = new io.vidocq.mansart.data.dialect.attribute.TextAttribute<
                    JpqlTestEntity>(
                    "name", "NAME", JpqlTestEntity.class,
                    true, false, 255, nameGetter, nameSetter);

            var model = new io.vidocq.mansart.data.dialect.EntityModel<
                    JpqlTestEntity>(
                    JpqlTestEntity.class, "JPQL_ENTITY", null,
                    idAttr, java.util.Optional.empty(),
                    List.of(idAttr, nameAttr),
                    List.of(),
                    privateLookup.findConstructor(JpqlTestEntity.class,
                            MethodType.methodType(void.class)));

            var provider = new MansartPersistenceProvider();
            PersistenceUnitInfo fakeInfo = makeFakeInfo(
                    List.of(JpqlTestEntity.class.getName()), dataSource);
            var entityModelMap = Map.of(JpqlTestEntity.class, model);
            var hints = Map.of("mansart.entityModels", entityModelMap);
            emf = provider.createContainerEntityManagerFactory(fakeInfo, hints);
        }
        return emf.createEntityManager();
    }

    @SuppressWarnings("unchecked")
    private String getId(Object entity) throws Exception {
        var field = JpqlTestEntity.class.getDeclaredField("id");
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
    @jakarta.persistence.Table(name = "JPQL_ENTITY")
    static class JpqlTestEntity {
        @jakarta.persistence.Id
        private String id;
        private String name;

        JpqlTestEntity() {}

        JpqlTestEntity(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
