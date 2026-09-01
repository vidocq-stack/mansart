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

import io.vidocq.mansart.data.dialect.EntityModel;
import io.vidocq.mansart.data.dialect.attribute.IdAttribute;
import io.vidocq.mansart.data.dialect.attribute.ReferenceAttribute;
import io.vidocq.mansart.data.dialect.attribute.TextAttribute;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

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

/**
 * Unit test for {@code MansartEntityManager.remove(Object)} with relationship
 * cleanup (one-to-one inverse FK clearing).
 *
 * <p>Verifies: removing the owning side of a bidirectional @OneToOne clears
 * the FK column on the inverse side.</p>
 */
class RemoveRelationshipTest {

    private JdbcDataSource dataSource;
    private EntityManagerFactory emf;
    private EntityManager em;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:relRemoveTest" + System.nanoTime()
                + ";DB_CLOSE_DELAY=-1");

        // Create tables: AUTHOR (owning) has FK BOOK_ID on AUTHOR table.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "CREATE TABLE AUTHOR ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "NAME VARCHAR(255), "
                            + "BOOK_ID VARCHAR(255), "
                            + "PRIMARY KEY (ID))");
            stmt.execute(
                    "CREATE TABLE BOOK ("
                            + "ID VARCHAR(255) NOT NULL, "
                            + "TITLE VARCHAR(255), "
                            + "AUTHOR_ID VARCHAR(255), "
                            + "PRIMARY KEY (ID))");
        }

        // Build EntityModels with MethodHandles.
        var lookup = MethodHandles.lookup();

        // --- Author (owning side of @OneToOne) ---
        var aLookup = MethodHandles.privateLookupIn(Author.class, lookup);
        MethodHandle aIdGetter = aLookup.findGetter(Author.class, "id", String.class);
        MethodHandle aIdSetter = aLookup.findSetter(Author.class, "id", String.class);
        MethodHandle aNameGetter = aLookup.findGetter(Author.class, "name", String.class);
        MethodHandle aNameSetter = aLookup.findSetter(Author.class, "name", String.class);
        MethodHandle aBookGetter = aLookup.findGetter(Author.class, "book", String.class);
        MethodHandle aBookSetter = aLookup.findSetter(Author.class, "book", String.class);

        var aIdAttr = new IdAttribute<Author, String>(
                "id", "ID", String.class, Author.class,
                false, aIdGetter, aIdSetter);
        var aNameAttr = new TextAttribute<Author>(
                "name", "NAME", Author.class,
                true, false, 255, aNameGetter, aNameSetter);
        // FK to Book (owning side of @OneToOne).
        var aBookAttr = new ReferenceAttribute<>(
                "book", "BOOK_ID", String.class, Author.class,
                false, false, false, aBookGetter, aBookSetter);

        var authorModel = new EntityModel<Author>(
                Author.class, "AUTHOR", null,
                aIdAttr, java.util.Optional.empty(),
                List.of(aIdAttr, aNameAttr, aBookAttr),
                List.of(),
                aLookup.findConstructor(Author.class,
                        MethodType.methodType(void.class)));

        // --- Book (inverse side of @OneToOne) ---
        var bLookup = MethodHandles.privateLookupIn(Book.class, lookup);
        MethodHandle bIdGetter = bLookup.findGetter(Book.class, "id", String.class);
        MethodHandle bIdSetter = bLookup.findSetter(Book.class, "id", String.class);
        MethodHandle bTitleGetter = bLookup.findGetter(Book.class, "title", String.class);
        MethodHandle bTitleSetter = bLookup.findSetter(Book.class, "title", String.class);
        MethodHandle bAuthorGetter = bLookup.findGetter(Book.class, "author", String.class);
        MethodHandle bAuthorSetter = bLookup.findSetter(Book.class, "author", String.class);

        var bIdAttr = new IdAttribute<Book, String>(
                "id", "ID", String.class, Book.class,
                false, bIdGetter, bIdSetter);
        var bTitleAttr = new TextAttribute<Book>(
                "title", "TITLE", Book.class,
                true, false, 255, bTitleGetter, bTitleSetter);
        // FK back to Author (inverse side of @OneToOne).
        var bAuthorAttr = new ReferenceAttribute<>(
                "author", "AUTHOR_ID", String.class, Book.class,
                false, false, false, bAuthorGetter, bAuthorSetter);

        var bookModel = new EntityModel<Book>(
                Book.class, "BOOK", null,
                bIdAttr, java.util.Optional.empty(),
                List.of(bIdAttr, bTitleAttr, bAuthorAttr),
                List.of(),
                bLookup.findConstructor(Book.class,
                        MethodType.methodType(void.class)));

        var entityModelMap = Map.of(
                Author.class, authorModel,
                Book.class, bookModel);

        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        PersistenceUnitInfo fakeInfo = makeFakeInfo(
                List.of(Author.class.getName(), Book.class.getName()),
                dataSource);
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

    /**
     * JP-27: removing the owning side of a bidirectional @OneToOne.
     *
     * <p>When removing an Author (owning side), the FK (BOOK_ID) on the
     * Book (inverse side) should be set to NULL.</p>
     */
    @Test
    void removeOwningSideClearsInverseFk() throws Exception {
        // Insert an Author and a Book that references it.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "INSERT INTO AUTHOR (ID, NAME, BOOK_ID) "
                            + "VALUES ('a-1', 'Author One', 'b-1')");
            stmt.execute(
                    "INSERT INTO BOOK (ID, TITLE, AUTHOR_ID) "
                            + "VALUES ('b-1', 'Book One', 'a-1')");
        }

        // Load and remove the Author (owning side of the @OneToOne).
        Author author = em.find(Author.class, "a-1");
        assertThat(author).isNotNull();

        em.remove(author);

        // Verify the Book's FK (AUTHOR_ID) is now NULL.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT AUTHOR_ID FROM BOOK WHERE ID = 'b-1'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("AUTHOR_ID")).isNull();
        }
    }

    /**
     * JP-27: removing the inverse side of a bidirectional @OneToOne.
     *
     * <p>When removing a Book (inverse side), the FK (AUTHOR_ID) on the
     * Author (owning side) should be set to NULL.</p>
     */
    @Test
    void removeInverseSideClearsOwningFk() throws Exception {
        // Insert an Author and a Book that references it.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "INSERT INTO AUTHOR (ID, NAME, BOOK_ID) "
                            + "VALUES ('a-1', 'Author One', 'b-1')");
            stmt.execute(
                    "INSERT INTO BOOK (ID, TITLE, AUTHOR_ID) "
                            + "VALUES ('b-1', 'Book One', 'a-1')");
        }

        // Load and remove the Book (inverse side).
        Book book = em.find(Book.class, "b-1");
        assertThat(book).isNotNull();

        em.remove(book);

        // Verify the Author's FK (BOOK_ID) is now NULL.
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT BOOK_ID FROM AUTHOR WHERE ID = 'a-1'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("BOOK_ID")).isNull();
        }
    }

    // --- Test entities ---

    @jakarta.persistence.Entity
    static class Author {
        private String id;
        private String name;
        private String book;

        public Author() {}
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getBook() { return book; }
        public void setBook(String book) { this.book = book; }
    }

    @jakarta.persistence.Entity
    static class Book {
        private String id;
        private String title;
        private String author;

        public Book() {}
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
    }

    // --- Helpers ---

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
}
