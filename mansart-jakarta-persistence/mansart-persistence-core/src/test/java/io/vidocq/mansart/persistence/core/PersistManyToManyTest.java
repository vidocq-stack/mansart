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
import io.vidocq.mansart.data.dialect.SqlFragment;
import io.vidocq.mansart.data.dialect.attribute.IdAttribute;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import jakarta.persistence.spi.ClassTransformer;

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
 * Unit test for {@code MansartEntityManager.persist(Object)} with many-to-many
 * relationships.
 *
 * <p>Verifies that persisting an entity with a {@code @ManyToMany} collection
 * inserts rows into the join table, and that the inverse collection is
 * updated accordingly.</p>
 */
class PersistManyToManyTest {

    private JdbcDataSource dataSource;
    private EntityManagerFactory emf;
    private EntityManager em;

    @BeforeEach
    void setUp() throws Exception {
        // Create H2 in-memory datasource with unique name.
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:manyXmanyTest" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1");

        // Create the main entity tables + join table.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE PERSIST_MANYXMANY_A ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "NAME VARCHAR(255), "
                            + "\"VALUE\" INT, "
                            + "PRIMARY KEY (ID))");
            stmt.execute(
                    "CREATE TABLE PERSIST_MANYXMANY_B ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "NAME VARCHAR(255), "
                            + "\"VALUE\" INT, "
                            + "PRIMARY KEY (ID))");
            stmt.execute(
                    "CREATE TABLE PERSIST_MANYXMANY_A_B ("
                            + "A_ID VARCHAR(255) NOT NULL, "
                            + "B_ID VARCHAR(255) NOT NULL, "
                            + "PRIMARY KEY (A_ID, B_ID))");
        }

        // Build EntityModel for entity A (owning side).
        var lookup = MethodHandles.lookup();
        var privateLookupA = MethodHandles.privateLookupIn(ManyXManyA.class, lookup);
        MethodHandle idGetterA = privateLookupA.findGetter(
                ManyXManyA.class, "id", String.class);
        MethodHandle idSetterA = privateLookupA.findSetter(
                ManyXManyA.class, "id", String.class);
        MethodHandle nameGetterA = privateLookupA.findGetter(
                ManyXManyA.class, "name", String.class);
        MethodHandle nameSetterA = privateLookupA.findSetter(
                ManyXManyA.class, "name", String.class);
        MethodHandle valueGetterA = privateLookupA.findGetter(
                ManyXManyA.class, "value", int.class);
        MethodHandle valueSetterA = privateLookupA.findSetter(
                ManyXManyA.class, "value", int.class);
        MethodHandle bColGetterA = privateLookupA.findGetter(
                ManyXManyA.class, "bCol", java.util.Collection.class);
        MethodHandle bColSetterA = privateLookupA.findSetter(
                ManyXManyA.class, "bCol", java.util.Collection.class);

        var idAttrA = new IdAttribute<ManyXManyA, String>(
                "id", "ID", String.class, ManyXManyA.class,
                false, idGetterA, idSetterA);

        // Plural attribute: many-to-many (owning side).
        var bColAttr = new io.vidocq.mansart.data.dialect.attribute.ManyToManyAttribute<ManyXManyA, ManyXManyB>(
                "bCol", "PERSIST_MANYXMANY_A_B",
                "A_ID", "B_ID",
                ManyXManyB.class,
                ManyXManyA.class,
                false, false, null,
                bColGetterA, bColSetterA);

        var modelA = new EntityModel<ManyXManyA>(
                ManyXManyA.class, "PERSIST_MANYXMANY_A", null,
                idAttrA, java.util.Optional.empty(),
                List.of(idAttrA,
                        new io.vidocq.mansart.data.dialect.attribute.TextAttribute<ManyXManyA>(
                                "name", "NAME", ManyXManyA.class, true, false, 255, nameGetterA, nameSetterA),
                        new io.vidocq.mansart.data.dialect.attribute.NumericAttribute<ManyXManyA, Integer>(
                                "value", "VALUE", Integer.class, ManyXManyA.class, true, false, 10, 0, valueGetterA, valueSetterA)),
                List.of(bColAttr),
                privateLookupA.findConstructor(ManyXManyA.class,
                        MethodType.methodType(void.class)));

        // Build EntityModel for entity B (inverse side).
        var privateLookupB = MethodHandles.privateLookupIn(ManyXManyB.class, lookup);
        MethodHandle idGetterB = privateLookupB.findGetter(
                ManyXManyB.class, "id", String.class);
        MethodHandle idSetterB = privateLookupB.findSetter(
                ManyXManyB.class, "id", String.class);
        MethodHandle nameGetterB = privateLookupB.findGetter(
                ManyXManyB.class, "name", String.class);
        MethodHandle nameSetterB = privateLookupB.findSetter(
                ManyXManyB.class, "name", String.class);
        MethodHandle valueGetterB = privateLookupB.findGetter(
                ManyXManyB.class, "value", int.class);
        MethodHandle valueSetterB = privateLookupB.findSetter(
                ManyXManyB.class, "value", int.class);
        MethodHandle aColGetterB = privateLookupB.findGetter(
                ManyXManyB.class, "aCol", java.util.Collection.class);
        MethodHandle aColSetterB = privateLookupB.findSetter(
                ManyXManyB.class, "aCol", java.util.Collection.class);

        var idAttrB = new IdAttribute<ManyXManyB, String>(
                "id", "ID", String.class, ManyXManyB.class,
                false, idGetterB, idSetterB);

        var aColAttr = new io.vidocq.mansart.data.dialect.attribute.ManyToManyInverseAttribute<ManyXManyB, ManyXManyA>(
                "aCol", "PERSIST_MANYXMANY_A_B",
                "A_ID", "B_ID",
                ManyXManyA.class,
                ManyXManyB.class,
                false, true, "bCol",
                aColGetterB, aColSetterB);

        var modelB = new EntityModel<ManyXManyB>(
                ManyXManyB.class, "PERSIST_MANYXMANY_B", null,
                idAttrB, java.util.Optional.empty(),
                List.of(idAttrB,
                        new io.vidocq.mansart.data.dialect.attribute.TextAttribute<ManyXManyB>(
                                "name", "NAME", ManyXManyB.class, true, false, 255, nameGetterB, nameSetterB),
                        new io.vidocq.mansart.data.dialect.attribute.NumericAttribute<ManyXManyB, Integer>(
                                "value", "VALUE", Integer.class, ManyXManyB.class, true, false, 10, 0, valueGetterB, valueSetterB)),
                List.of(aColAttr),
                privateLookupB.findConstructor(ManyXManyB.class,
                        MethodType.methodType(void.class)));

        // Build a dialect that generates INSERT for the join table.
        Dialect dialect = new Dialect() {
            @Override public String name() { return "h2"; }
            @Override public SqlFragment select(EntityModel<?> model,
                    io.vidocq.mansart.data.dialect.Where where,
                    io.vidocq.mansart.data.dialect.OrderBy orderBy,
                    io.vidocq.mansart.data.dialect.Pagination pagination) {
                return new SqlFragment("SELECT * FROM " + model.tableName(), List.of());
            }
            @Override public SqlFragment insert(EntityModel<?> model, boolean returningGeneratedKey) {
                return new SqlFragment("INSERT INTO " + model.tableName() + " VALUES ()", List.of());
            }
            @Override public SqlFragment update(EntityModel<?> model,
                    io.vidocq.mansart.data.dialect.Where where) {
                return new SqlFragment("UPDATE " + model.tableName(), List.of());
            }
            @Override public SqlFragment delete(EntityModel<?> model,
                    io.vidocq.mansart.data.dialect.Where where) {
                return new SqlFragment("DELETE FROM " + model.tableName(), List.of());
            }
            @Override public SqlFragment merge(EntityModel<?> model) {
                return new SqlFragment("MERGE INTO " + model.tableName(), List.of());
            }
            @Override public int sqlType(Class<?> javaType) { return 0; }
            @Override public void bind(java.sql.PreparedStatement ps, int idx,
                    Object value, Class<?> javaType) throws java.sql.SQLException { }
            @Override public <T> T extract(java.sql.ResultSet rs, int idx,
                    Class<T> javaType) throws java.sql.SQLException { return null; }
            @Override public RuntimeException translate(java.sql.SQLException e) { return null; }
        };

        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        PersistenceUnitInfo fakeInfo = makeFakeInfo(
                List.of(ManyXManyA.class.getName(), ManyXManyB.class.getName()),
                dataSource, dialect);

        var entityModelMap = Map.of(ManyXManyA.class, modelA, ManyXManyB.class, modelB);
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
    void persistManyToManyInsertsJoinTableRow() throws Exception {
        // Create entities.
        ManyXManyA entityA = new ManyXManyA();
        entityA.setId("a1");
        entityA.setName("Alpha");
        entityA.setValue(1);

        ManyXManyB entityB = new ManyXManyB();
        entityB.setId("b1");
        entityB.setName("Beta");
        entityB.setValue(2);

        entityA.getBCol().add(entityB);

        // Persist entity A (owning side).
        em.persist(entityA);

        // Verify the join table has a row.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) AS cnt FROM PERSIST_MANYXMANY_A_B "
                             + "WHERE A_ID = 'a1' AND B_ID = 'b1'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("cnt")).isEqualTo(1);
        }

        // Verify entity A is managed.
        assertThat(em.contains(entityA)).isTrue();
    }

    @Test
    void persistManyToManyUpdateInverseCollection() throws Exception {
        // Pre-insert entity B.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO PERSIST_MANYXMANY_B VALUES ('b2', 'Gamma', 3)");
        }

        // Create entity A with existing entity B.
        ManyXManyA entityA = new ManyXManyA();
        entityA.setId("a2");
        entityA.setName("Delta");
        entityA.setValue(4);

        ManyXManyB entityB = new ManyXManyB();
        entityB.setId("b2");
        entityB.setName("Gamma");
        entityB.setValue(3);

        entityA.getBCol().add(entityB);

        em.persist(entityA);

        // Verify the join table has the new row.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) AS cnt FROM PERSIST_MANYXMANY_A_B "
                             + "WHERE A_ID = 'a2' AND B_ID = 'b2'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("cnt")).isEqualTo(1);
        }
    }

    @Test
    void persistManyToManyMultipleTargets() throws Exception {
        ManyXManyA entityA = new ManyXManyA();
        entityA.setId("a3");
        entityA.setName("Epsilon");
        entityA.setValue(5);

        for (int i = 0; i < 3; i++) {
            ManyXManyB entityB = new ManyXManyB();
            entityB.setId("b" + (i + 10));
            entityB.setName("Zeta" + i);
            entityB.setValue(i + 10);
            entityA.getBCol().add(entityB);
        }

        em.persist(entityA);

        // Verify 3 rows in the join table.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) AS cnt FROM PERSIST_MANYXMANY_A_B "
                             + "WHERE A_ID = 'a3'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("cnt")).isEqualTo(3);
        }
    }

    @Test
    void persistManyToManyNullEntityThrows() {
        assertThatThrownBy(() -> em.persist(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void persistManyToManyAfterCloseThrows() {
        em.close();
        ManyXManyA entityA = new ManyXManyA();
        entityA.setId("a4");
        assertThatThrownBy(() -> em.persist(entityA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("EntityManager is closed");
    }

    private static PersistenceUnitInfo makeFakeInfo(
            List<String> managedClasses,
            DataSource ds,
            Dialect dialect) {
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
            @Override public void addTransformer(ClassTransformer t) { }
        };
    }

    // -- Test entities ------------------------------------------------------

    @jakarta.persistence.Entity(name = "ManyXManyA")
    @jakarta.persistence.Table(name = "PERSIST_MANYXMANY_A")
    static class ManyXManyA {
        private String id;
        private String name;
        private int value;
        private java.util.Collection<ManyXManyB> bCol = new java.util.ArrayList<>();

        public ManyXManyA() { }

        @jakarta.persistence.Id
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @jakarta.persistence.Column(name = "NAME")
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        @jakarta.persistence.Column(name = "\"VALUE\"")
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }

        @jakarta.persistence.ManyToMany(targetEntity = ManyXManyB.class)
        @jakarta.persistence.JoinTable(
                name = "PERSIST_MANYXMANY_A_B",
                joinColumns = @jakarta.persistence.JoinColumn(name = "A_ID"),
                inverseJoinColumns = @jakarta.persistence.JoinColumn(name = "B_ID"))
        public java.util.Collection<ManyXManyB> getBCol() { return bCol; }
        public void setBCol(java.util.Collection<ManyXManyB> bCol) { this.bCol = bCol; }
    }

    @jakarta.persistence.Entity(name = "ManyXManyB")
    @jakarta.persistence.Table(name = "PERSIST_MANYXMANY_B")
    static class ManyXManyB {
        private String id;
        private String name;
        private int value;
        private java.util.Collection<ManyXManyA> aCol = new java.util.ArrayList<>();

        public ManyXManyB() { }

        @jakarta.persistence.Id
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @jakarta.persistence.Column(name = "NAME")
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        @jakarta.persistence.Column(name = "\"VALUE\"")
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }

        @jakarta.persistence.ManyToMany(mappedBy = "bCol", targetEntity = ManyXManyA.class)
        public java.util.Collection<ManyXManyA> getACol() { return aCol; }
        public void setACol(java.util.Collection<ManyXManyA> aCol) { this.aCol = aCol; }
    }
}
