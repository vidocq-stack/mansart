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
 * Unit test for {@code MansartEntityManager.persist(Object)} with one-to-many
 * relationships on the inverse side.
 *
 * <p>Verifies that persisting an entity with a {@code @OneToMany(mappedBy=...)}
 * collection maintains the inverse collection on the target entity — i.e.,
 * adds the source entity to the target's collection attribute — without writing
 * a join table row.</p>
 */
class PersistOneToManyTest {

    private JdbcDataSource dataSource;
    private EntityManagerFactory emf;
    private EntityManager em;

    @BeforeEach
    void setUp() throws Exception {
        // Create H2 in-memory datasource with unique name.
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:oneXmanyTest" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1");

        // Create the main entity tables (no join table for one-to-many).
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE PERSIST_ONEXMANY_A ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "NAME VARCHAR(255), "
                            + "PRIMARY KEY (ID))");
            stmt.execute(
                    "CREATE TABLE PERSIST_ONEXMANY_B ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "NAME VARCHAR(255), "
                            + "A_ID VARCHAR(255), "
                            + "PRIMARY KEY (ID))");
        }

        // Build EntityModel for entity A (inverse side).
        var lookup = MethodHandles.lookup();
        var privateLookupA = MethodHandles.privateLookupIn(OneXManyA.class, lookup);
        MethodHandle idGetterA = privateLookupA.findGetter(
                OneXManyA.class, "id", String.class);
        MethodHandle idSetterA = privateLookupA.findSetter(
                OneXManyA.class, "id", String.class);
        MethodHandle nameGetterA = privateLookupA.findGetter(
                OneXManyA.class, "name", String.class);
        MethodHandle nameSetterA = privateLookupA.findSetter(
                OneXManyA.class, "name", String.class);
        MethodHandle bColGetterA = privateLookupA.findGetter(
                OneXManyA.class, "bCol", java.util.Collection.class);
        MethodHandle bColSetterA = privateLookupA.findSetter(
                OneXManyA.class, "bCol", java.util.Collection.class);

        var idAttrA = new IdAttribute<OneXManyA, String>(
                "id", "ID", String.class, OneXManyA.class,
                false, idGetterA, idSetterA);

        // Plural attribute: one-to-many (inverse side).
        // NOTE: inverseSide=true, joinTableName=null (no join table for one-to-many).
        var bColAttr = new io.vidocq.mansart.data.dialect.attribute.ManyToManyAttribute<OneXManyA, OneXManyB>(
                "bCol", null,  // no join table for one-to-many
                null, null,    // no join columns
                OneXManyB.class,
                OneXManyA.class,
                false, true, "aRef",  // inverseSide=true, mappedBy="aRef"
                bColGetterA, bColSetterA);

        var modelA = new EntityModel<OneXManyA>(
                OneXManyA.class, "PERSIST_ONEXMANY_A", null,
                idAttrA, java.util.Optional.empty(),
                List.of(idAttrA,
                        new io.vidocq.mansart.data.dialect.attribute.TextAttribute<OneXManyA>(
                                "name", "NAME", OneXManyA.class, true, false, 255, nameGetterA, nameSetterA)),
                List.of(bColAttr),
                privateLookupA.findConstructor(OneXManyA.class,
                        MethodType.methodType(void.class)));

        // Build EntityModel for entity B (owning side, has @ManyToOne).
        var privateLookupB = MethodHandles.privateLookupIn(OneXManyB.class, lookup);
        MethodHandle idGetterB = privateLookupB.findGetter(
                OneXManyB.class, "id", String.class);
        MethodHandle idSetterB = privateLookupB.findSetter(
                OneXManyB.class, "id", String.class);
        MethodHandle nameGetterB = privateLookupB.findGetter(
                OneXManyB.class, "name", String.class);
        MethodHandle nameSetterB = privateLookupB.findSetter(
                OneXManyB.class, "name", String.class);
        MethodHandle aRefGetterB = privateLookupB.findGetter(
                OneXManyB.class, "aRef", OneXManyA.class);
        MethodHandle aRefSetterB = privateLookupB.findSetter(
                OneXManyB.class, "aRef", OneXManyA.class);

        var idAttrB = new IdAttribute<OneXManyB, String>(
                "id", "ID", String.class, OneXManyB.class,
                false, idGetterB, idSetterB);

        // Reference attribute: many-to-one (owning side).
        var aRefAttr = new io.vidocq.mansart.data.dialect.attribute.ReferenceAttribute<OneXManyB, OneXManyA>(
                "aRef", "A_ID",
                OneXManyA.class,
                OneXManyB.class,
                true, false, false,
                aRefGetterB, aRefSetterB);

        var modelB = new EntityModel<OneXManyB>(
                OneXManyB.class, "PERSIST_ONEXMANY_B", null,
                idAttrB, java.util.Optional.empty(),
                List.of(idAttrB,
                        new io.vidocq.mansart.data.dialect.attribute.TextAttribute<OneXManyB>(
                                "name", "NAME", OneXManyB.class, true, false, 255, nameGetterB, nameSetterB),
                        aRefAttr),
                List.of(),  // no plural attributes on B
                privateLookupB.findConstructor(OneXManyB.class,
                        MethodType.methodType(void.class)));

        // Build a dialect that generates INSERT for the main tables.
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
                List.of(OneXManyA.class.getName(), OneXManyB.class.getName()),
                dataSource, dialect);

        var entityModelMap = Map.of(OneXManyA.class, modelA, OneXManyB.class, modelB);
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
    void persistOneToManyMaintainsInverseCollection() throws Exception {
        // Pre-insert entity B into the DB (B is a pre-existing entity).
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO PERSIST_ONEXMANY_B VALUES ('b1', 'Beta', NULL)");
        }

        // Create entity A referencing the pre-existing entity B.
        OneXManyA entityA = new OneXManyA();
        entityA.setId("a1");
        entityA.setName("Alpha");

        OneXManyB entityB = new OneXManyB();
        entityB.setId("b1");
        entityB.setName("Beta");

        entityA.getBCol().add(entityB);

        // Persist entity A (inverse side).
        em.persist(entityA);

        // Verify entity A is managed.
        assertThat(em.contains(entityA)).isTrue();

        // Verify entity B's A_ID column was updated by inverse collection maintenance.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) AS cnt FROM PERSIST_ONEXMANY_B "
                             + "WHERE A_ID = 'a1'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("cnt")).isEqualTo(1);
        }
    }

    @Test
    void persistOneToManyMultipleTargets() throws Exception {
        // Pre-insert 3 target entities into the DB.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO PERSIST_ONEXMANY_B VALUES ('b10', 'Member0', NULL)");
            stmt.execute("INSERT INTO PERSIST_ONEXMANY_B VALUES ('b11', 'Member1', NULL)");
            stmt.execute("INSERT INTO PERSIST_ONEXMANY_B VALUES ('b12', 'Member2', NULL)");
        }

        OneXManyA entityA = new OneXManyA();
        entityA.setId("a2");
        entityA.setName("BetaGroup");

        for (int i = 0; i < 3; i++) {
            OneXManyB entityB = new OneXManyB();
            entityB.setId("b" + (i + 10));
            entityB.setName("Member" + i);
            entityA.getBCol().add(entityB);
        }

        em.persist(entityA);

        // Verify all 3 target entities have the FK pointing to A.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) AS cnt FROM PERSIST_ONEXMANY_B "
                             + "WHERE A_ID = 'a2'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("cnt")).isEqualTo(3);
        }
    }

    @Test
    void persistOneToManyNullEntityThrows() {
        assertThatThrownBy(() -> em.persist(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void persistOneToManyAfterCloseThrows() {
        em.close();
        OneXManyA entityA = new OneXManyA();
        entityA.setId("a3");
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

    @jakarta.persistence.Entity(name = "OneXManyA")
    @jakarta.persistence.Table(name = "PERSIST_ONEXMANY_A")
    static class OneXManyA {
        private String id;
        private String name;
        private java.util.Collection<OneXManyB> bCol = new java.util.ArrayList<>();

        public OneXManyA() { }

        @jakarta.persistence.Id
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @jakarta.persistence.Column(name = "NAME")
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        @jakarta.persistence.OneToMany(mappedBy = "aRef", targetEntity = OneXManyB.class)
        public java.util.Collection<OneXManyB> getBCol() { return bCol; }
        public void setBCol(java.util.Collection<OneXManyB> bCol) { this.bCol = bCol; }
    }

    @jakarta.persistence.Entity(name = "OneXManyB")
    @jakarta.persistence.Table(name = "PERSIST_ONEXMANY_B")
    static class OneXManyB {
        private String id;
        private String name;
        private OneXManyA aRef;

        public OneXManyB() { }

        @jakarta.persistence.Id
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @jakarta.persistence.Column(name = "NAME")
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        @jakarta.persistence.ManyToOne(targetEntity = OneXManyA.class)
        public OneXManyA getARef() { return aRef; }
        public void setARef(OneXManyA aRef) { this.aRef = aRef; }
    }
}
