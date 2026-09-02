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
 * Unit tests for JPQL numeric functions: ABS, SQRT.
 *
 * <p>These functions are unary scalar functions mapped to {@code Where.Func}
 * nodes (via {@link JpqlFunctionRegistry}). The parser must extract the
 * single argument, the translator must wrap it in the correct SQL function.</p>
 */
class JpqlNumericFunctionsTest {

    private JdbcDataSource dataSource;
    private EntityManagerFactory emf;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:numFunc" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE NUM_FUNC_ENTITY ("
                            + "\"ID\" VARCHAR(255) NOT NULL, "
                            + "\"VALUE\" INTEGER, "
                            + "\"AMOUNT\" DECIMAL(10,2), "
                            + "PRIMARY KEY (\"ID\"))");
            stmt.execute(
                    "INSERT INTO NUM_FUNC_ENTITY VALUES ('1', -42, CAST(16.00 AS DECIMAL(10,2)))");
            stmt.execute(
                    "INSERT INTO NUM_FUNC_ENTITY VALUES ('2', 9, CAST(25.00 AS DECIMAL(10,2)))");
            stmt.execute(
                    "INSERT INTO NUM_FUNC_ENTITY VALUES ('3', -7, CAST(1.00 AS DECIMAL(10,2)))");
            stmt.execute(
                    "INSERT INTO NUM_FUNC_ENTITY VALUES ('4', 100, CAST(100.00 AS DECIMAL(10,2)))");
        }
    }

    @AfterEach
    void tearDown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    // -- ABS -----------------------------------------------------------------

    /**
     * JPQL ABS(field) in WHERE clause.
     */
    @Test
    void absTest() throws Exception {
        List<?> results = executeQuery(
                "SELECT e FROM NumFuncEntity e WHERE ABS(e.value) = 42");
        assertEquals(1, results.size());
        assertEquals("1", getId(results.get(0)));
    }

    // -- SQRT ----------------------------------------------------------------

    /**
     * JPQL SQRT(field) in WHERE clause.
     */
    @Test
    void sqrtTest() throws Exception {
        List<?> results = executeQuery(
                "SELECT e FROM NumFuncEntity e WHERE SQRT(e.amount) = 5.0");
        assertEquals(1, results.size());
        assertEquals("2", getId(results.get(0)));
    }

    // -- helpers -------------------------------------------------------------

    private void persistEntity(String id, Integer value, java.math.BigDecimal amount)
            throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO NUM_FUNC_ENTITY (\"ID\", \"VALUE\", \"AMOUNT\") VALUES (?, ?, ?)")) {
            stmt.setString(1, id);
            stmt.setObject(2, value);
            stmt.setObject(3, amount);
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
                    MethodHandles.privateLookupIn(NumFuncEntity.class, lookup);
            var idGetter = privateLookup.findGetter(
                    NumFuncEntity.class, "id", String.class);
            var idSetter = privateLookup.findSetter(
                    NumFuncEntity.class, "id", String.class);
            var valueGetter = privateLookup.findGetter(
                    NumFuncEntity.class, "value", Integer.class);
            var valueSetter = privateLookup.findSetter(
                    NumFuncEntity.class, "value", Integer.class);
             var amountGetter = privateLookup.findGetter(
                     NumFuncEntity.class, "amount", java.math.BigDecimal.class);
            var amountSetter = privateLookup.findSetter(
                    NumFuncEntity.class, "amount", java.math.BigDecimal.class);

            var idAttr = new io.vidocq.mansart.data.dialect.attribute.IdAttribute<
                    NumFuncEntity, String>(
                    "id", "ID", String.class, NumFuncEntity.class,
                    false, idGetter, idSetter);
            var valueAttr = new io.vidocq.mansart.data.dialect.attribute.NumericAttribute<
                    NumFuncEntity, Integer>(
                    "value", "VALUE", Integer.class, NumFuncEntity.class,
                    false, false, 0, 0, valueGetter, valueSetter);
            var amountAttr = new io.vidocq.mansart.data.dialect.attribute.NumericAttribute<
                    NumFuncEntity, java.math.BigDecimal>(
                    "amount", "AMOUNT", java.math.BigDecimal.class, NumFuncEntity.class,
                    false, false, 10, 2, amountGetter, amountSetter);

            var model = new io.vidocq.mansart.data.dialect.EntityModel<
                    NumFuncEntity>(
                    NumFuncEntity.class, "NUM_FUNC_ENTITY", null,
                    idAttr, java.util.Optional.empty(),
                    List.of(idAttr, valueAttr, amountAttr),
                    List.of(),
                    privateLookup.findConstructor(NumFuncEntity.class,
                            MethodType.methodType(void.class)));

            var provider = new MansartPersistenceProvider();
            PersistenceUnitInfo fakeInfo = makeFakeInfo(
                    List.of(NumFuncEntity.class.getName()), dataSource);
            var entityModelMap = Map.of(NumFuncEntity.class, model);
            var hints = Map.of("mansart.entityModels", entityModelMap);
            emf = provider.createContainerEntityManagerFactory(fakeInfo, hints);
        }
        return emf.createEntityManager();
    }

    @SuppressWarnings("unchecked")
    private String getId(Object entity) throws Exception {
        var field = NumFuncEntity.class.getDeclaredField("id");
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
    @jakarta.persistence.Table(name = "NUM_FUNC_ENTITY")
    static class NumFuncEntity {
        @jakarta.persistence.Id
        private String id;
        private Integer value;
        private java.math.BigDecimal amount;

        NumFuncEntity() {}

        NumFuncEntity(String id, Integer value, java.math.BigDecimal amount) {
            this.id = id;
            this.value = value;
            this.amount = amount;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public Integer getValue() { return value; }
        public void setValue(Integer value) { this.value = value; }
        public java.math.BigDecimal getAmount() { return amount; }
        public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
    }
}
