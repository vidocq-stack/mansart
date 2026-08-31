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
 * Unit test for {@code MansartEntityManager.persist(Object)} with one-to-one
 * relationships.
 *
 * <p>Verifies that persisting an entity with a {@code @OneToOne} reference
 * writes the foreign key value (target entity's ID) into the owning entity's
 * table, and that persisting the inverse side updates the owning side's FK.</p>
 */
class PersistOneToOneTest {

    private JdbcDataSource dataSource;
    private EntityManagerFactory emf;
    private EntityManager em;

    @BeforeEach
    void setUp() throws Exception {
        // Create H2 in-memory datasource with unique name.
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:oneXoneTest" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1");

        // Create the parent (inverse side) table.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE PERSIST_1X1_PARENT ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "NAME VARCHAR(255), "
                            + "PRIMARY KEY (ID))");
        }

        // Create the child (owning side) table with FK column.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE PERSIST_1X1_CHILD ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "NAME VARCHAR(255), "
                            + "PARENT_ID VARCHAR(255), "
                            + "PRIMARY KEY (ID))");
        }

        // Build EntityModel for Parent (inverse side).
        var lookup = MethodHandles.lookup();
        var privateLookupParent = MethodHandles.privateLookupIn(
                OneXOneParent.class, lookup);
        MethodHandle parentIdGetter = privateLookupParent.findGetter(
                OneXOneParent.class, "id", String.class);
        MethodHandle parentIdSetter = privateLookupParent.findSetter(
                OneXOneParent.class, "id", String.class);
        MethodHandle parentNameGetter = privateLookupParent.findGetter(
                OneXOneParent.class, "name", String.class);
        MethodHandle parentNameSetter = privateLookupParent.findSetter(
                OneXOneParent.class, "name", String.class);

        var parentIdAttr = new IdAttribute<OneXOneParent, String>(
                "id", "ID", String.class, OneXOneParent.class,
                false, parentIdGetter, parentIdSetter);

        var parentModel = new EntityModel<OneXOneParent>(
                OneXOneParent.class, "PERSIST_1X1_PARENT", null,
                parentIdAttr, java.util.Optional.empty(),
                List.of(parentIdAttr,
                        new io.vidocq.mansart.data.dialect.attribute.TextAttribute<OneXOneParent>(
                                "name", "NAME", OneXOneParent.class,
                                true, false, 255, parentNameGetter, parentNameSetter)),
                List.of(),
                privateLookupParent.findConstructor(
                        OneXOneParent.class,
                        MethodType.methodType(void.class)));

        // Build EntityModel for Child (owning side with @OneToOne to Parent).
        var privateLookupChild = MethodHandles.privateLookupIn(
                OneXOneChild.class, lookup);
        MethodHandle childIdGetter = privateLookupChild.findGetter(
                OneXOneChild.class, "id", String.class);
        MethodHandle childIdSetter = privateLookupChild.findSetter(
                OneXOneChild.class, "id", String.class);
        MethodHandle childNameGetter = privateLookupChild.findGetter(
                OneXOneChild.class, "name", String.class);
        MethodHandle childNameSetter = privateLookupChild.findSetter(
                OneXOneChild.class, "name", String.class);
        MethodHandle childParentGetter = privateLookupChild.findGetter(
                OneXOneChild.class, "parent", OneXOneParent.class);
        MethodHandle childParentSetter = privateLookupChild.findSetter(
                OneXOneChild.class, "parent", OneXOneParent.class);

        var childIdAttr = new IdAttribute<OneXOneChild, String>(
                "id", "ID", String.class, OneXOneChild.class,
                false, childIdGetter, childIdSetter);

        // ReferenceAttribute: @OneToOne -> Parent (owning side, unique FK).
        var parentAttr = new ReferenceAttribute<OneXOneChild, String>(
                "parent", "PARENT_ID",
                String.class, OneXOneChild.class,
                false, true, false,  // nullable=false, unique=true, lazy=false
                childParentGetter, childParentSetter);

        var childModel = new EntityModel<OneXOneChild>(
                OneXOneChild.class, "PERSIST_1X1_CHILD", null,
                childIdAttr, java.util.Optional.empty(),
                List.of(childIdAttr,
                        new io.vidocq.mansart.data.dialect.attribute.TextAttribute<OneXOneChild>(
                                "name", "NAME", OneXOneChild.class,
                                true, false, 255, childNameGetter, childNameSetter),
                        parentAttr),
                List.of(),
                privateLookupChild.findConstructor(
                        OneXOneChild.class,
                        MethodType.methodType(void.class)));

        // Build a dialect that generates INSERT/UPDATE for the tables.
        Dialect dialect = new Dialect() {
            @Override public String name() { return "h2"; }
            @Override public SqlFragment select(EntityModel<?> model,
                    io.vidocq.mansart.data.dialect.Where where,
                    io.vidocq.mansart.data.dialect.OrderBy orderBy,
                    io.vidocq.mansart.data.dialect.Pagination pagination) {
                return new SqlFragment("SELECT * FROM " + model.tableName(), List.of());
            }
            @Override public SqlFragment insert(EntityModel<?> model, boolean returningGeneratedKey) {
                // Build INSERT with columns from the model's attributes.
                // Skip ReferenceAttribute columns (they belong to the target table).
                List<String> columns = new java.util.ArrayList<>();
                for (io.vidocq.mansart.data.dialect.Attribute<?, ?> attr : model.attributes()) {
                    if (attr instanceof io.vidocq.mansart.data.dialect.attribute.IdAttribute<?, ?>) {
                        columns.add(attr.columnName());
                    } else if (!(attr instanceof io.vidocq.mansart.data.dialect.attribute.ReferenceAttribute<?, ?>)) {
                        columns.add(attr.columnName());
                    }
                }
                String cols = String.join(", ", columns);
                String pcts = columns.stream().map(c -> "?").collect(java.util.stream.Collectors.joining(", "));
                return new SqlFragment("INSERT INTO " + model.tableName() + " (" + cols + ") VALUES (" + pcts + ")", List.of());
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
                List.of(OneXOneParent.class.getName(),
                        OneXOneChild.class.getName()),
                dataSource, dialect);

        var entityModelMap = Map.of(
                OneXOneParent.class, parentModel,
                OneXOneChild.class, childModel);
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
    void persistOneToOneWritesForeignKey() throws Exception {
        // Create parent (inverse side).
        OneXOneParent parent = new OneXOneParent();
        parent.setId("p1");
        parent.setName("Parent One");

        // Create child (owning side) with reference to parent.
        OneXOneChild child = new OneXOneChild();
        child.setId("c1");
        child.setName("Child One");
        child.setParent(parent);

        // Persist child (owning side of @OneToOne).
        em.persist(child);

        // Verify the child row has the FK column set.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT PARENT_ID FROM PERSIST_1X1_CHILD WHERE ID = 'c1'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("PARENT_ID")).isEqualTo("p1");
        }

        // Verify child is managed.
        assertThat(em.contains(child)).isTrue();
    }

    @Test
    void persistOneToOneWithNullReference() throws Exception {
        // Create parent.
        OneXOneParent parent = new OneXOneParent();
        parent.setId("p2");
        parent.setName("Parent Two");

        // Create child WITHOUT a parent reference (nullable FK).
        OneXOneChild child = new OneXOneChild();
        child.setId("c2");
        child.setName("Child Two");
        // parent is null by default.

        em.persist(child);

        // Verify the FK column is null.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT PARENT_ID FROM PERSIST_1X1_CHILD WHERE ID = 'c2'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("PARENT_ID")).isNull();
        }
    }

    @Test
    void persistOneToOneNullEntityThrows() {
        assertThatThrownBy(() -> em.persist(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void persistOneToOneAfterCloseThrows() {
        em.close();
        OneXOneChild child = new OneXOneChild();
        child.setId("c3");
        assertThatThrownBy(() -> em.persist(child))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("EntityManager is closed");
    }

    @Test
    void persistInverseSideUpdatesOwningSideFk() throws Exception {
        // Create parent (inverse side) and child (owning side).
        OneXOneParent parent = new OneXOneParent();
        parent.setId("p1");
        parent.setName("Parent One");

        OneXOneChild child = new OneXOneChild();
        child.setId("c1");
        child.setName("Child One");
        child.setParent(parent);

        // Persist the child (owning side) first — creates child row with FK.
        em.persist(child);

        // Verify the child row has FK pointing to parent.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT PARENT_ID FROM PERSIST_1X1_CHILD WHERE ID = 'c1'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("PARENT_ID")).isEqualTo("p1");
        }

        // Now persist the parent (inverse side).
        // This should UPDATE the child's FK column to point to the parent.
        em.persist(parent);

        // Verify the child's FK column is still correctly set.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT PARENT_ID FROM PERSIST_1X1_CHILD WHERE ID = 'c1'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("PARENT_ID")).isEqualTo("p1");
        }

        // Verify parent is now managed.
        assertThat(em.contains(parent)).isTrue();
    }

    // -- Test entities ------------------------------------------------------

    @jakarta.persistence.Entity(name = "OneXOneParent")
    @jakarta.persistence.Table(name = "PERSIST_1X1_PARENT")
    static class OneXOneParent {
        private String id;
        private String name;

        public OneXOneParent() { }

        @jakarta.persistence.Id
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @jakarta.persistence.Column(name = "NAME")
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @jakarta.persistence.Entity(name = "OneXOneChild")
    @jakarta.persistence.Table(name = "PERSIST_1X1_CHILD")
    static class OneXOneChild {
        private String id;
        private String name;
        private OneXOneParent parent;

        public OneXOneChild() { }

        @jakarta.persistence.Id
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        @jakarta.persistence.Column(name = "NAME")
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        @jakarta.persistence.OneToOne
        @jakarta.persistence.JoinColumn(name = "PARENT_ID")
        public OneXOneParent getParent() { return parent; }
        public void setParent(OneXOneParent parent) { this.parent = parent; }
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
