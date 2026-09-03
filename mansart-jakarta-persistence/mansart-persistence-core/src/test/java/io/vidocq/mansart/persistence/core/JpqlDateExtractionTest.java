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
import java.sql.Timestamp;
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
 * Unit tests for JPQL EXTRACT date/time extraction functions.
 *
 * <p>Tests: {@code EXTRACT(YEAR FROM ...)}, {@code EXTRACT(MONTH FROM ...)},
 * {@code EXTRACT(DAY FROM ...)}, {@code EXTRACT(HOUR FROM ...)},
 * {@code EXTRACT(MINUTE FROM ...)}, {@code EXTRACT(SECOND FROM ...)}.
 *
 * <p>TCK client: {@code ee.jakarta.tck.persistence.core.annotations.access.field.Client4}.
 */
class JpqlDateExtractionTest {

    private JdbcDataSource dataSource;
    private EntityManagerFactory emf;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:dateExtr" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE DATE_TIME_ENTITY ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "TIMESTAMP_COL TIMESTAMP, "
                            + "PRIMARY KEY (ID))");
        }
    }

    @AfterEach
    void tearDown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    /**
     * JPQL EXTRACT(YEAR FROM ...) in WHERE clause.
     */
    @Test
    void extractYearTest() throws Exception {
        persistEntity("y1", java.sql.Timestamp.valueOf("1989-11-17 11:23:36"));
        persistEntity("y2", java.sql.Timestamp.valueOf("2020-01-01 00:00:00"));

        List<?> results = executeQuery(
                "SELECT e FROM DateTimeEntity e WHERE EXTRACT(YEAR FROM e.timestamp) = 1989");
        assertEquals(1, results.size());
        assertEquals("y1", getId(results.get(0)));
    }

    /**
     * JPQL EXTRACT(MONTH FROM ...) in WHERE clause.
     */
    @Test
    void extractMonthTest() throws Exception {
        persistEntity("m1", java.sql.Timestamp.valueOf("1989-11-17 11:23:36"));
        persistEntity("m2", java.sql.Timestamp.valueOf("2020-01-01 00:00:00"));

        List<?> results = executeQuery(
                "SELECT e FROM DateTimeEntity e WHERE EXTRACT(MONTH FROM e.timestamp) = 11");
        assertEquals(1, results.size());
        assertEquals("m1", getId(results.get(0)));
    }

    /**
     * JPQL EXTRACT(DAY FROM ...) in WHERE clause.
     */
    @Test
    void extractDayTest() throws Exception {
        persistEntity("d1", java.sql.Timestamp.valueOf("1989-11-17 11:23:36"));
        persistEntity("d2", java.sql.Timestamp.valueOf("2020-01-01 00:00:00"));

        List<?> results = executeQuery(
                "SELECT e FROM DateTimeEntity e WHERE EXTRACT(DAY FROM e.timestamp) = 17");
        assertEquals(1, results.size());
        assertEquals("d1", getId(results.get(0)));
    }

    /**
     * JPQL EXTRACT(HOUR FROM ...) in WHERE clause.
     */
    @Test
    void extractHourTest() throws Exception {
        persistEntity("h1", java.sql.Timestamp.valueOf("1989-11-17 11:23:36"));
        persistEntity("h2", java.sql.Timestamp.valueOf("2020-01-01 15:00:00"));

        List<?> results = executeQuery(
                "SELECT e FROM DateTimeEntity e WHERE EXTRACT(HOUR FROM e.timestamp) = 11");
        assertEquals(1, results.size());
        assertEquals("h1", getId(results.get(0)));
    }

    /**
     * JPQL EXTRACT(MINUTE FROM ...) in WHERE clause.
     */
    @Test
    void extractMinuteTest() throws Exception {
        persistEntity("mi1", java.sql.Timestamp.valueOf("1989-11-17 11:23:36"));
        persistEntity("mi2", java.sql.Timestamp.valueOf("2020-01-01 15:45:00"));

        List<?> results = executeQuery(
                "SELECT e FROM DateTimeEntity e WHERE EXTRACT(MINUTE FROM e.timestamp) = 23");
        assertEquals(1, results.size());
        assertEquals("mi1", getId(results.get(0)));
    }

    /**
     * JPQL EXTRACT(SECOND FROM ...) in WHERE clause.
     */
    @Test
    void extractSecondTest() throws Exception {
        persistEntity("s1", java.sql.Timestamp.valueOf("1989-11-17 11:23:36"));
        persistEntity("s2", java.sql.Timestamp.valueOf("2020-01-01 15:00:10"));

        List<?> results = executeQuery(
                "SELECT e FROM DateTimeEntity e WHERE EXTRACT(SECOND FROM e.timestamp) = 36");
        assertEquals(1, results.size());
        assertEquals("s1", getId(results.get(0)));
    }

    // -- helpers -------------------------------------------------------------

    private void persistEntity(String id, Timestamp ts) throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO DATE_TIME_ENTITY (ID, TIMESTAMP_COL) VALUES (?, ?)")) {
            stmt.setString(1, id);
            stmt.setTimestamp(2, ts);
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
                    MethodHandles.privateLookupIn(DateTimeEntity.class, lookup);
            var idGetter = privateLookup.findGetter(
                    DateTimeEntity.class, "id", String.class);
            var idSetter = privateLookup.findSetter(
                    DateTimeEntity.class, "id", String.class);
            var tsGetter = privateLookup.findGetter(
                    DateTimeEntity.class, "timestamp", Timestamp.class);
            var tsSetter = privateLookup.findSetter(
                    DateTimeEntity.class, "timestamp", Timestamp.class);

            var idAttr = new io.vidocq.mansart.data.dialect.attribute.IdAttribute<
                    DateTimeEntity, String>(
                    "id", "ID", String.class, DateTimeEntity.class,
                    false, idGetter, idSetter);
            var tsAttr = new io.vidocq.mansart.data.dialect.attribute.TemporalAttribute<
                    DateTimeEntity, java.sql.Timestamp>(
                    "timestamp", "TIMESTAMP_COL", java.sql.Timestamp.class,
                    DateTimeEntity.class,
                    true, false, tsGetter, tsSetter);

            var model = new io.vidocq.mansart.data.dialect.EntityModel<
                    DateTimeEntity>(
                    DateTimeEntity.class, "DATE_TIME_ENTITY", null,
                    idAttr, java.util.Optional.empty(),
                    List.of(idAttr, tsAttr),
                    java.util.List.of(),
                    privateLookup.findConstructor(DateTimeEntity.class,
                            MethodType.methodType(void.class)));

            var provider = new MansartPersistenceProvider();
            PersistenceUnitInfo fakeInfo = makeFakeInfo(
                    List.of(DateTimeEntity.class.getName()), dataSource);
            var entityModelMap = Map.of(DateTimeEntity.class, model);
            var hints = Map.of("mansart.entityModels", entityModelMap);
            emf = provider.createContainerEntityManagerFactory(fakeInfo, hints);
        }
        return emf.createEntityManager();
    }

    @SuppressWarnings("unchecked")
    private String getId(Object entity) throws Exception {
        var field = DateTimeEntity.class.getDeclaredField("id");
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
    @jakarta.persistence.Table(name = "DATE_TIME_ENTITY")
    static class DateTimeEntity {
        @jakarta.persistence.Id
        private String id;
        private java.sql.Timestamp timestamp;

        DateTimeEntity() {}

        DateTimeEntity(String id, java.sql.Timestamp timestamp) {
            this.id = id;
            this.timestamp = timestamp;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public java.sql.Timestamp getTimestamp() { return timestamp; }
        public void setTimestamp(java.sql.Timestamp timestamp) { this.timestamp = timestamp; }
    }
}
