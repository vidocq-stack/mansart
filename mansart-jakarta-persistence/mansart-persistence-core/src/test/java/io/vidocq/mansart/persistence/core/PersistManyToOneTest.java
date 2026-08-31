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
import io.vidocq.mansart.data.dialect.attribute.ReferenceAttribute;

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
 * Unit test for {@code MansartEntityManager.persist(Object)} with many-to-one
 * relationships.
 *
 * <p>Verifies that persisting an entity with a {@code @ManyToOne} reference
 * writes the foreign key value (target entity's ID) into the owning entity's
 * table.</p>
 */
class PersistManyToOneTest {

    private JdbcDataSource dataSource;
    private EntityManagerFactory emf;
    private EntityManager em;

    @BeforeEach
    void setUp() throws Exception {
        // Create H2 in-memory datasource with unique name.
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:manyXoneTest" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1");

        // Create the parent table.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE PERSIST_MXONE_PARENT ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "NAME VARCHAR(255), "
                            + "PRIMARY KEY (ID))");
        }

        // Create the child table with FK column.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE PERSIST_MXONE_CHILD ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "NAME VARCHAR(255), "
                            + "PARENT_ID VARCHAR(255), "
                            + "PRIMARY KEY (ID))");
        }

        // Build EntityModel for Parent.
        var lookup = MethodHandles.lookup();
        var privateLookupParent = MethodHandles.privateLookupIn(
                ManyXOneParent.class, lookup);
        MethodHandle parentIdGetter = privateLookupParent.findGetter(
                ManyXOneParent.class, "id", String.class);
        MethodHandle parentIdSetter = privateLookupParent.findSetter(
                ManyXOneParent.class, "id", String.class);
        MethodHandle parentNameGetter = privateLookupParent.findGetter(
                ManyXOneParent.class, "name", String.class);
        MethodHandle parentNameSetter = privateLookupParent.findSetter(
                ManyXOneParent.class, "name", String.class);

        var parentIdAttr = new IdAttribute<ManyXOneParent, String>(
                "id", "ID", String.class, ManyXOneParent.class,
                false, parentIdGetter, parentIdSetter);

        var parentModel = new EntityModel<ManyXOneParent>(
                ManyXOneParent.class, "PERSIST_MXONE_PARENT", null,
                parentIdAttr, java.util.Optional.empty(),
                List.of(parentIdAttr,
                        new io.vidocq.mansart.data.dialect.attribute.TextAttribute<ManyXOneParent>(
                                "name", "NAME", ManyXOneParent.class,
                                true, false, 255, parentNameGetter, parentNameSetter)),
                List.of(),
                privateLookupParent.findConstructor(
                        ManyXOneParent.class,
                        MethodType.methodType(void.class)));

        // Build EntityModel for Child (with @ManyToOne to Parent).
        var privateLookupChild = MethodHandles.privateLookupIn(
                ManyXOneChild.class, lookup);
        MethodHandle childIdGetter = privateLookupChild.findGetter(
                ManyXOneChild.class, "id", String.class);
        MethodHandle childIdSetter = privateLookupChild.findSetter(
                ManyXOneChild.class, "id", String.class);
        MethodHandle childNameGetter = privateLookupChild.findGetter(
                ManyXOneChild.class, "name", String.class);
        MethodHandle childNameSetter = privateLookupChild.findSetter(
                ManyXOneChild.class, "name", String.class);
        MethodHandle childParentGetter = privateLookupChild.findGetter(
                ManyXOneChild.class, "parent", ManyXOneParent.class);
        MethodHandle childParentSetter = privateLookupChild.findSetter(
                ManyXOneChild.class, "parent", ManyXOneParent.class);

        var childIdAttr = new IdAttribute<ManyXOneChild, String>(
                "id", "ID", String.class, ManyXOneChild.class,
                false, childIdGetter, childIdSetter);

        // ReferenceAttribute: @ManyToOne -> Parent.
        // E = child (owner), V = String (FK value type).
        var parentAttr = new ReferenceAttribute<ManyXOneChild, String>(
                "parent", "PARENT_ID",
                String.class, ManyXOneChild.class,
                false, false, false,
                childParentGetter, childParentSetter);

        var childModel = new EntityModel<ManyXOneChild>(
                ManyXOneChild.class, "PERSIST_MXONE_CHILD", null,
                childIdAttr, java.util.Optional.empty(),
                List.of(childIdAttr,
                        new io.vidocq.mansart.data.dialect.attribute.TextAttribute<ManyXOneChild>(
                                "name", "NAME", ManyXOneChild.class,
                                true, false, 255, childNameGetter, childNameSetter),
                        parentAttr),
                List.of(),
                privateLookupChild.findConstructor(
                        ManyXOneChild.class,
                        MethodType.methodType(void.class)));

        // Build a dialect that generates INSERT for the tables.
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
                List.of(ManyXOneParent.class.getName(),
                        ManyXOneChild.class.getName()),
                dataSource, dialect);

        var entityModelMap = Map.of(
                ManyXOneParent.class, parentModel,
                ManyXOneChild.class, childModel);
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
    void persistManyToOneWritesForeignKey() throws Exception {
        // Create parent.
        ManyXOneParent parent = new ManyXOneParent();
        parent.setId("p1");
        parent.setName("Parent One");

        // Create child with reference to parent.
        ManyXOneChild child = new ManyXOneChild();
        child.setId("c1");
        child.setName("Child One");
        child.setParent(parent);

        // Persist child (owning side of @ManyToOne).
        em.persist(child);

        // Verify the child row has the FK column set.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT PARENT_ID FROM PERSIST_MXONE_CHILD WHERE ID = 'c1'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("PARENT_ID")).isEqualTo("p1");
        }

        // Verify child is managed.
        assertThat(em.contains(child)).isTrue();
    }

    @Test
    void persistManyToOneWithNullReference() throws Exception {
        // Create parent.
        ManyXOneParent parent = new ManyXOneParent();
        parent.setId("p2");
        parent.setName("Parent Two");

        // Create child WITHOUT a parent reference (nullable FK).
        ManyXOneChild child = new ManyXOneChild();
        child.setId("c2");
        child.setName("Child Two");
        // parent is null by default.

        em.persist(child);

        // Verify the FK column is null.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT PARENT_ID FROM PERSIST_MXONE_CHILD WHERE ID = 'c2'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("PARENT_ID")).isNull();
        }
    }

    @Test
    void persistManyToOneNullEntityThrows() {
        assertThatThrownBy(() -> em.persist(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void persistManyToOneAfterCloseThrows() {
        em.close();
        ManyXOneChild child = new ManyXOneChild();
        child.setId("c3");
        assertThatThrownBy(() -> em.persist(child))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("EntityManager is closed");
    }

    // -- Test entities ------------------------------------------------------

    @jakarta.persistence.Entity(name = "ManyXOneParent")
    @jakarta.persistence.Table(name = "PERSIST_MXONE_PARENT")
    static class ManyXOneParent {
        private String id;
        private String name;

        public ManyXOneParent() { }

        @jakarta.persistence.Id
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @jakarta.persistence.Column(name = "NAME")
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @jakarta.persistence.Entity(name = "ManyXOneChild")
    @jakarta.persistence.Table(name = "PERSIST_MXONE_CHILD")
    static class ManyXOneChild {
        private String id;
        private String name;
        private ManyXOneParent parent;

        public ManyXOneChild() { }

        @jakarta.persistence.Id
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @jakarta.persistence.Column(name = "NAME")
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        @jakarta.persistence.ManyToOne
        @jakarta.persistence.JoinColumn(name = "PARENT_ID")
        public ManyXOneParent getParent() { return parent; }
        public void setParent(ManyXOneParent parent) { this.parent = parent; }
    }

    // -- Helper methods -----------------------------------------------------

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
}
