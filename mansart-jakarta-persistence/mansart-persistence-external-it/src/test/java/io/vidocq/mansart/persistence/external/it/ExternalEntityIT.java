/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.external.it;

import io.vidocq.mansart.persistence.external.ExternalDepartment;
import io.vidocq.mansart.persistence.external.ExternalPerson;
import io.vidocq.mansart.persistence.external.lib.Book;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests proving Tier 1 (APT) ≈ Tier 2 (Maven plugin) behavior.
 * Tests that external entities (from external-lib) work identically to compiled entities.
 */
public class ExternalEntityIT {

    private static final String GENERATED_SOURCES_DIR = findGeneratedSourcesDir();
    
    private static String findGeneratedSourcesDir() {
        // The external-lib module is at ../mansart-persistence-external-lib relative to external-it
        Path generatedDir = Path.of("../mansart-persistence-external-lib/target/generated-sources/mansart");
        if (Files.exists(generatedDir)) {
            return generatedDir.toString();
        }
        
        // Try absolute path as fallback
        return "/Users/yblazart/projects/perso/vidocq/mansart/mansart-jakarta-persistence/mansart-persistence-external-lib/target/generated-sources/mansart";
    }

    @Test
    public void testExternalPersonEntity() {
        // Test that ExternalPerson can be instantiated
        ExternalPerson person = new ExternalPerson("John Doe", "Test Developer");
        
        assertThat(person).isNotNull();
        assertThat(person.getName()).isEqualTo("John Doe");
        assertThat(person.getDescription()).isEqualTo("Test Developer");
    }

    @Test
    public void testExternalDepartmentEntity() {
        // Test that ExternalDepartment can be instantiated
        ExternalPerson manager = new ExternalPerson("Jane Doe", "Manager");
        ExternalDepartment dept = new ExternalDepartment("Engineering", manager);
        
        assertThat(dept).isNotNull();
        assertThat(dept.getName()).isEqualTo("Engineering");
        assertThat(dept.getManager()).isEqualTo(manager);
    }

    @Test
    public void testEntityRelationships() {
        ExternalPerson person1 = new ExternalPerson("Alice", "Developer");
        ExternalPerson person2 = new ExternalPerson("Bob", "Developer");
        
        ExternalDepartment dept1 = new ExternalDepartment("Dev Team 1", person1);
        ExternalDepartment dept2 = new ExternalDepartment("Dev Team 2", person2);
        
        assertThat(dept1.getManager()).isEqualTo(person1);
        assertThat(dept2.getManager()).isEqualTo(person2);
        
        // Test that relationships can be changed
        dept2.setManager(person1);
        assertThat(dept2.getManager()).isEqualTo(person1);
    }

    @Test
    public void testEntityLifecycleMethods() {
        ExternalPerson person = new ExternalPerson("Charlie", "Architect");
        
        // Test getters and setters
        person.setName("Charles");
        assertThat(person.getName()).isEqualTo("Charles");
        
        person.setDescription("Senior Architect");
        assertThat(person.getDescription()).isEqualTo("Senior Architect");
    }

    @Test
    public void testBookEntityFromExternalLib() {
        Book book = new Book(1L, "Test Title", "Test Author", "123-456");
        
        assertThat(book).isNotNull();
        assertThat(book.getId()).isEqualTo(1L);
        assertThat(book.getTitle()).isEqualTo("Test Title");
        assertThat(book.getAuthor()).isEqualTo("Test Author");
        assertThat(book.getIsbn()).isEqualTo("123-456");
    }

    @Test
    public void testMavenPluginGeneratedEntityModelForExternalPerson() {
        Path generatedFile = Path.of(GENERATED_SOURCES_DIR, "io/vidocq/mansart/persistence/external/_ExternalPerson.java");
        
        assertThat(Files.exists(generatedFile)).as("Generated _ExternalPerson.java should exist").isTrue();
        
        String content = readFile(generatedFile);
        assertThat(content).contains("public final class _ExternalPerson<T> implements EntityModel<T>");
        assertThat(content).contains("this.tableName = \"external_people\";");
    }

    @Test
    public void testMavenPluginGeneratedStandardMetamodelForExternalPerson() {
        Path generatedFile = Path.of(GENERATED_SOURCES_DIR, "io/vidocq/mansart/persistence/external/ExternalPerson_.java");
        
        assertThat(Files.exists(generatedFile)).as("Generated ExternalPerson_.java should exist").isTrue();
        
        String content = readFile(generatedFile);
        assertThat(content).contains("@StaticMetamodel(value = io.vidocq.mansart.persistence.external.ExternalPerson.class)");
        assertThat(content).contains("public static volatile SingularAttribute<ExternalPerson, java.lang.Long> id");
        assertThat(content).contains("public static volatile SingularAttribute<ExternalPerson, java.lang.String> name");
        assertThat(content).contains("public static volatile SingularAttribute<ExternalPerson, java.lang.String> description");
        assertThat(content).contains("public static volatile SingularAttribute<ExternalPerson, int> version");
    }

    @Test
    public void testMavenPluginGeneratedEntityModelForExternalDepartment() {
        Path generatedFile = Path.of(GENERATED_SOURCES_DIR, "io/vidocq/mansart/persistence/external/_ExternalDepartment.java");
        
        assertThat(Files.exists(generatedFile)).as("Generated _ExternalDepartment.java should exist").isTrue();
        
        String content = readFile(generatedFile);
        assertThat(content).contains("public final class _ExternalDepartment<T> implements EntityModel<T>");
        assertThat(content).contains("this.tableName = \"external_departments\";");
    }

    @Test
    public void testMavenPluginGeneratedStandardMetamodelForExternalDepartment() {
        Path generatedFile = Path.of(GENERATED_SOURCES_DIR, "io/vidocq/mansart/persistence/external/ExternalDepartment_.java");
        
        assertThat(Files.exists(generatedFile)).as("Generated ExternalDepartment_.java should exist").isTrue();
        
        String content = readFile(generatedFile);
        assertThat(content).contains("@StaticMetamodel(value = io.vidocq.mansart.persistence.external.ExternalDepartment.class)");
        assertThat(content).contains("public static volatile SingularAttribute<ExternalDepartment, java.lang.Long> id");
        assertThat(content).contains("public static volatile SingularAttribute<ExternalDepartment, java.lang.String> name");
        assertThat(content).contains("public static volatile SingularAttribute<ExternalDepartment, io.vidocq.mansart.persistence.external.ExternalPerson> manager");
        assertThat(content).contains("public static volatile SingularAttribute<ExternalDepartment, int> version");
    }

    @Test
    public void testMavenPluginGeneratedEntityModelForBook() {
        Path generatedFile = Path.of(GENERATED_SOURCES_DIR, "io/vidocq/mansart/persistence/external/lib/_Book.java");
        
        assertThat(Files.exists(generatedFile)).as("Generated _Book.java should exist").isTrue();
        
        String content = readFile(generatedFile);
        assertThat(content).contains("public final class _Book<T> implements EntityModel<T>");
        assertThat(content).contains("this.tableName = \"external_books\";");
    }

    @Test
    public void testMavenPluginGeneratedStandardMetamodelForBook() {
        Path generatedFile = Path.of(GENERATED_SOURCES_DIR, "io/vidocq/mansart/persistence/external/lib/Book_.java");
        
        assertThat(Files.exists(generatedFile)).as("Generated Book_.java should exist").isTrue();
        
        String content = readFile(generatedFile);
        assertThat(content).contains("@StaticMetamodel(value = io.vidocq.mansart.persistence.external.lib.Book.class)");
        assertThat(content).contains("public static volatile SingularAttribute<Book, java.lang.Long> id");
        assertThat(content).contains("public static volatile SingularAttribute<Book, java.lang.String> title");
        assertThat(content).contains("public static volatile SingularAttribute<Book, java.lang.String> author");
        assertThat(content).contains("public static volatile SingularAttribute<Book, java.lang.String> isbn");
        assertThat(content).contains("public static volatile SingularAttribute<Book, int> version");
    }

    private String readFile(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + path, e);
        }
    }

    // --- M2-JP-16: Lazy association proxies ---

    @Test
    public void testMavenPluginGeneratedLazyProxyForExternalPerson() {
        // ExternalPerson is the target of ExternalDepartment's @ManyToOne, so a proxy should be generated
        Path generatedFile = Path.of(GENERATED_SOURCES_DIR, "io/vidocq/mansart/persistence/external/ExternalPerson_Lazy.java");
        
        assertThat(Files.exists(generatedFile)).as("Generated ExternalPerson_Lazy.java should exist").isTrue();
        
        String content = readFile(generatedFile);
        assertThat(content).contains("public final class ExternalPerson_Lazy extends ExternalPerson");
        assertThat(content).contains("implements LazyEntityProxy");
        assertThat(content).contains("import io.vidocq.mansart.persistence.spi.LazyEntityProxy");
        assertThat(content).contains("import io.vidocq.mansart.persistence.spi.LazyInitializer");
        assertThat(content).contains("private LazyInitializer lazyInitializer");
        assertThat(content).contains("private boolean loaded");
    }

    @Test
    public void testMavenPluginGeneratedLazyProxyOverridesGetters() {
        Path generatedFile = Path.of(GENERATED_SOURCES_DIR, "io/vidocq/mansart/persistence/external/ExternalPerson_Lazy.java");
        
        assertThat(Files.exists(generatedFile)).as("Generated ExternalPerson_Lazy.java should exist").isTrue();
        
        String content = readFile(generatedFile);
        // The proxy should override entity getters to trigger lazy loading
        assertThat(content).contains("public String getName()");
        assertThat(content).contains("public String getDescription()");
        assertThat(content).contains("public int getVersion()");
        assertThat(content).contains("ensureLoaded()");
    }

    @Test
    public void testMavenPluginGeneratedLazyProxyHasIsLoadedMethod() {
        Path generatedFile = Path.of(GENERATED_SOURCES_DIR, "io/vidocq/mansart/persistence/external/ExternalPerson_Lazy.java");
        
        assertThat(Files.exists(generatedFile)).as("Generated ExternalPerson_Lazy.java should exist").isTrue();
        
        String content = readFile(generatedFile);
        assertThat(content).contains("public boolean isLoaded()");
        assertThat(content).contains("public void setLazyInitializer(LazyInitializer");
    }

    @Test
    public void testNoLazyProxyGeneratedForNonReferencedEntities() {
        // Book is not the target of any @ManyToOne/@OneToOne, so no proxy should be generated
        Path generatedFile = Path.of(GENERATED_SOURCES_DIR, "io/vidocq/mansart/persistence/external/lib/Book_Lazy.java");
        
        assertThat(Files.exists(generatedFile)).as("Book_Lazy.java should NOT exist — Book is not referenced by any entity").isFalse();
    }
}