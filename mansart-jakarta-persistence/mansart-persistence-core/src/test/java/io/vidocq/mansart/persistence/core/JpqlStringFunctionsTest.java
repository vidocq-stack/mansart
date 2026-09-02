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
 * Unit tests for JPQL string functions: LOCATE, SUBSTRING, LEFT, RIGHT, CONCAT.
 *
 * <p>These functions take multiple arguments and are handled as raw SQL
 * fragments (not {@code Where.Func} wrappers). The parser must extract all
 * arguments from the function call and the translator must emit the correct
 * SQL.</p>
 */
class JpqlStringFunctionsTest {

    private JdbcDataSource dataSource;
    private EntityManagerFactory emf;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:stringFunc" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE STRING_FUNC_ENTITY ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "NAME VARCHAR(255), "
                            + "COUNTRY VARCHAR(255), "
                            + "PRIMARY KEY (ID))");
            stmt.execute(
                    "INSERT INTO STRING_FUNC_ENTITY VALUES ('1', 'Alan E. Frechette', 'United States')");
            stmt.execute(
                    "INSERT INTO STRING_FUNC_ENTITY VALUES ('2', 'Arthur D. Frechette', 'Canada')");
            stmt.execute(
                    "INSERT INTO STRING_FUNC_ENTITY VALUES ('3', 'Boris Unknown', 'Mexico')");
        }
    }

    @AfterEach
    void tearDown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    // -- LOCATE --------------------------------------------------------------

    /**
     * JPQL LOCATE(substring, field) in WHERE clause.
     */
    @Test
    void selectLocateTest() throws Exception {
        List<?> results = executeQuery(
                "SELECT e FROM StringFuncEntity e WHERE LOCATE('Frechette', e.name) > 0");
        assertEquals(2, results.size());
    }

    /**
     * JPQL LOCATE(substring, field) returns position.
     */
    @Test
    void selectLocatePositionTest() throws Exception {
        List<?> results = executeQuery(
                "SELECT e FROM StringFuncEntity e WHERE LOCATE('Frechette', e.name) > 10");
        assertEquals(1, results.size());
        assertEquals("2", getId(results.get(0)));
    }

    // -- SUBSTRING -----------------------------------------------------------

    /**
     * JPQL SUBSTRING(field, start, length) in WHERE clause.
     */
    @Test
    void selectSubstringTest() throws Exception {
        List<?> results = executeQuery(
                "SELECT e FROM StringFuncEntity e WHERE SUBSTRING(e.name, 1, 4) = 'Alan'");
        assertEquals(1, results.size());
        assertEquals("1", getId(results.get(0)));
    }

    // -- LEFT ----------------------------------------------------------------

    /**
     * JPQL LEFT(field, length) in WHERE clause.
     */
    @Test
    void selectLeftTest() throws Exception {
        List<?> results = executeQuery(
                "SELECT e FROM StringFuncEntity e WHERE LEFT(e.name, 4) = 'Alan'");
        assertEquals(1, results.size());
        assertEquals("1", getId(results.get(0)));
    }

    // -- RIGHT ---------------------------------------------------------------

    /**
     * JPQL RIGHT(field, length) in WHERE clause.
     */
    @Test
    void selectRightTest() throws Exception {
        List<?> results = executeQuery(
                "SELECT e FROM StringFuncEntity e WHERE RIGHT(e.name, 9) = 'Frechette'");
        assertEquals(2, results.size());
    }

    // -- CONCAT --------------------------------------------------------------

    /**
     * JPQL CONCAT(field1, field2) in WHERE clause.
     */
    @Test
    void selectConcatTest() throws Exception {
        List<?> results = executeQuery(
                "SELECT e FROM StringFuncEntity e WHERE CONCAT(e.name, ' ', e.country) = 'Alan E. Frechette United States'");
        assertEquals(1, results.size());
        assertEquals("1", getId(results.get(0)));
    }

    // -- helpers -------------------------------------------------------------

    private void persistEntity(String id, String name, String country) throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO STRING_FUNC_ENTITY (ID, NAME, COUNTRY) VALUES (?, ?, ?)")) {
            stmt.setString(1, id);
            stmt.setString(2, name);
            stmt.setString(3, country);
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
                    MethodHandles.privateLookupIn(StringFuncEntity.class, lookup);
            var idGetter = privateLookup.findGetter(
                    StringFuncEntity.class, "id", String.class);
            var idSetter = privateLookup.findSetter(
                    StringFuncEntity.class, "id", String.class);
            var nameGetter = privateLookup.findGetter(
                    StringFuncEntity.class, "name", String.class);
            var nameSetter = privateLookup.findSetter(
                    StringFuncEntity.class, "name", String.class);
            var countryGetter = privateLookup.findGetter(
                    StringFuncEntity.class, "country", String.class);
            var countrySetter = privateLookup.findSetter(
                    StringFuncEntity.class, "country", String.class);

            var idAttr = new io.vidocq.mansart.data.dialect.attribute.IdAttribute<
                    StringFuncEntity, String>(
                    "id", "ID", String.class, StringFuncEntity.class,
                    false, idGetter, idSetter);
            var nameAttr = new io.vidocq.mansart.data.dialect.attribute.TextAttribute<
                    StringFuncEntity>(
                    "name", "NAME", StringFuncEntity.class,
                    true, false, 255, nameGetter, nameSetter);
            var countryAttr = new io.vidocq.mansart.data.dialect.attribute.TextAttribute<
                    StringFuncEntity>(
                    "country", "COUNTRY", StringFuncEntity.class,
                    true, false, 255, countryGetter, countrySetter);

            var model = new io.vidocq.mansart.data.dialect.EntityModel<
                    StringFuncEntity>(
                    StringFuncEntity.class, "STRING_FUNC_ENTITY", null,
                    idAttr, java.util.Optional.empty(),
                    List.of(idAttr, nameAttr, countryAttr),
                    List.of(),
                    privateLookup.findConstructor(StringFuncEntity.class,
                            MethodType.methodType(void.class)));

            var provider = new MansartPersistenceProvider();
            PersistenceUnitInfo fakeInfo = makeFakeInfo(
                    List.of(StringFuncEntity.class.getName()), dataSource);
            var entityModelMap = Map.of(StringFuncEntity.class, model);
            var hints = Map.of("mansart.entityModels", entityModelMap);
            emf = provider.createContainerEntityManagerFactory(fakeInfo, hints);
        }
        return emf.createEntityManager();
    }

    @SuppressWarnings("unchecked")
    private String getId(Object entity) throws Exception {
        var field = StringFuncEntity.class.getDeclaredField("id");
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
    @jakarta.persistence.Table(name = "STRING_FUNC_ENTITY")
    static class StringFuncEntity {
        @jakarta.persistence.Id
        private String id;
        private String name;
        private String country;

        StringFuncEntity() {}

        StringFuncEntity(String id, String name, String country) {
            this.id = id;
            this.name = name;
            this.country = country;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
    }
}
