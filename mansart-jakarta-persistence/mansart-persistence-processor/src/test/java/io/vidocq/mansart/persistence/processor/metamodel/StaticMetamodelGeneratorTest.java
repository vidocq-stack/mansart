/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * or any later version, which is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * It is also made available under the European Union Public Licence v. 1.2,
 * which is available at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.processor.metamodel;

import org.junit.jupiter.api.*;
import javax.annotation.processing.Processor;
import javax.tools.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for static metamodel generation.
 * <p>
 * These tests verify that the annotation processor correctly generates
 * static metamodel classes for JPA entities, embeddables, and mapped superclasses.
 */
@Tag("integration")
class StaticMetamodelGeneratorTest {

    private static final String TEST_SOURCE_ROOT = "src/test/resources/metamodel";
    private static final String GENERATED_SOURCE_ROOT = "target/generated-test-sources/test-annotations";

    @BeforeAll
    static void setup() throws IOException {
        // Create test directories if they don't exist
        Files.createDirectories(Paths.get(TEST_SOURCE_ROOT));
        Files.createDirectories(Paths.get(GENERATED_SOURCE_ROOT));
    }

    @AfterAll
    static void cleanup() throws IOException {
        // Clean up generated files
        deleteDirectory(Paths.get(GENERATED_SOURCE_ROOT));
    }

    private static void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                 .sorted(Comparator.reverseOrder())
                 .forEach(p -> {
                     try {
                         Files.delete(p);
                     } catch (IOException e) {
                         // Ignore
                     }
                 });
        }
    }

    /**
     * Tests that the annotation processor generates a metamodel for a simple entity.
     */
    @Test
    void shouldGenerateMetamodelForSimpleEntity() {
        // This test is run via Maven's annotation processing during test-compile
        // The generated files are verified in the next test
    }

    /**
     * Tests that the generated metamodel class exists and has the expected structure.
     */
    @Test
    void shouldGenerateValidMetamodelClass() {
        // Verify TestEntity_ was generated
        Path metamodelPath = Paths.get(GENERATED_SOURCE_ROOT, "io/vidocq/mansart/persistence/processor/TestEntity_.java");
        
        assertThat(metamodelPath).exists();
        
        String content = readFile(metamodelPath);
        
        // Verify class structure
        assertThat(content).contains("package io.vidocq.mansart.persistence.processor;");
        assertThat(content).contains("public abstract class TestEntity_");
        assertThat(content).contains("@Generated");
        assertThat(content).contains("SingularAttribute");
        assertThat(content).contains("PluralAttribute");
        
        // Verify attributes (with fully qualified names)
        assertThat(content).contains("public static volatile SingularAttribute<io.vidocq.mansart.persistence.processor.TestEntity, java.lang.Long> id");
        assertThat(content).contains("public static volatile SingularAttribute<io.vidocq.mansart.persistence.processor.TestEntity, java.lang.String> name");
        assertThat(content).contains("public static volatile SingularAttribute<io.vidocq.mansart.persistence.processor.TestEntity, java.lang.Integer> age");
        assertThat(content).contains("public static volatile SingularAttribute<io.vidocq.mansart.persistence.processor.TestEntity, io.vidocq.mansart.persistence.processor.TestEntity> parent");
        assertThat(content).contains("public static volatile PluralAttribute<io.vidocq.mansart.persistence.processor.TestEntity, java.util.List, io.vidocq.mansart.persistence.processor.TestEntity> children");
    }

    /**
     * Tests that the generated metamodel for embeddable classes is valid.
     */
    @Test
    void shouldGenerateValidMetamodelForEmbeddable() {
        Path metamodelPath = Paths.get(GENERATED_SOURCE_ROOT, "io/vidocq/mansart/persistence/processor/TestEmbeddable_.java");
        
        assertThat(metamodelPath).exists();
        
        String content = readFile(metamodelPath);
        
        // Verify class structure
        assertThat(content).contains("public abstract class TestEmbeddable_");
        assertThat(content).contains("SingularAttribute<io.vidocq.mansart.persistence.processor.TestEmbeddable, java.lang.String> field1");
        assertThat(content).contains("SingularAttribute<io.vidocq.mansart.persistence.processor.TestEmbeddable, java.lang.Integer> field2");
    }

    /**
     * Tests that transient fields are excluded from the metamodel.
     */
    @Test
    void shouldExcludeTransientFields() {
        Path metamodelPath = Paths.get(GENERATED_SOURCE_ROOT, "io/vidocq/mansart/persistence/processor/TestEntity_.java");
        String content = readFile(metamodelPath);
        
        // Transient fields should not appear in the metamodel
        assertThat(content).doesNotContain("transientField");
    }

    /**
     * Tests that the license header is included in generated files.
     */
    @Test
    void shouldIncludeLicenseHeader() {
        Path metamodelPath = Paths.get(GENERATED_SOURCE_ROOT, "io/vidocq/mansart/persistence/processor/TestEntity_.java");
        String content = readFile(metamodelPath);
        
        assertThat(content).contains("Copyright (c) ${year} Yann Blazart");
        assertThat(content).contains("SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later");
    }

    private String readFile(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + path, e);
        }
    }
}
