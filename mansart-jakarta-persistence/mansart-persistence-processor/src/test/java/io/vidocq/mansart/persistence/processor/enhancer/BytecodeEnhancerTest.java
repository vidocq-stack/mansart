/*
 * Copyright (c) ${year} Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

package io.vidocq.mansart.persistence.processor.enhancer;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for the bytecode enhancer output.
 */
class BytecodeEnhancerTest {

    private static final String GENERATED_SOURCE_ROOT = "target/generated-test-sources/test-annotations";

    @Test
    void shouldGenerateEnhancedClassForEntity() throws Exception {
        Path enhancedPath = Paths.get(GENERATED_SOURCE_ROOT, 
                "io/vidocq/mansart/persistence/processor/TestEntity_Enhanced.java");
        
        assertThat(enhancedPath).exists();
        
        String content = Files.readString(enhancedPath);
        
        // Verify class structure
        assertThat(content).contains("public class TestEntity_Enhanced extends TestEntity");
        assertThat(content).contains("@Generated");
        assertThat(content).contains("BytecodeEnhancer");
    }

    @Test
    void shouldAddDirtyMaskField() throws Exception {
        Path enhancedPath = Paths.get(GENERATED_SOURCE_ROOT, 
                "io/vidocq/mansart/persistence/processor/TestEntity_Enhanced.java");
        String content = Files.readString(enhancedPath);
        
        assertThat(content).contains("private long __mansart_dirtyMask = 0L;");
    }

    @Test
    void shouldAddLazyHolderFieldsForAssociations() throws Exception {
        Path enhancedPath = Paths.get(GENERATED_SOURCE_ROOT, 
                "io/vidocq/mansart/persistence/processor/TestEntity_Enhanced.java");
        String content = Files.readString(enhancedPath);
        
        // Verify lazy holders for associations
        assertThat(content).contains("LazyLoadingUtils.LazyHolder<io.vidocq.mansart.persistence.processor.TestEntity> __mansart_scoped_parent");
        assertThat(content).contains("LazyLoadingUtils.LazyHolder<java.util.List<io.vidocq.mansart.persistence.processor.TestEntity>> __mansart_scoped_children");
        assertThat(content).contains("LazyLoadingUtils.LazyHolder<io.vidocq.mansart.persistence.processor.TestEmbeddable> __mansart_scoped_embeddedData");
    }

    @Test
    void shouldAddLoadedFlagsForAssociations() throws Exception {
        Path enhancedPath = Paths.get(GENERATED_SOURCE_ROOT, 
                "io/vidocq/mansart/persistence/processor/TestEntity_Enhanced.java");
        String content = Files.readString(enhancedPath);
        
        assertThat(content).contains("private boolean __mansart_loaded_parent = false;");
        assertThat(content).contains("private boolean __mansart_loaded_children = false;");
        assertThat(content).contains("private boolean __mansart_loaded_embeddedData = false;");
    }

    @Test
    void shouldOverrideGettersAndSetters() throws Exception {
        Path enhancedPath = Paths.get(GENERATED_SOURCE_ROOT, 
                "io/vidocq/mansart/persistence/processor/TestEntity_Enhanced.java");
        String content = Files.readString(enhancedPath);
        
        // Verify overridden methods
        assertThat(content).contains("@Override");
        assertThat(content).contains("public java.lang.Long getId()");
        assertThat(content).contains("public void setId(java.lang.Long value)");
        assertThat(content).contains("public io.vidocq.mansart.persistence.processor.TestEntity getParent()");
        assertThat(content).contains("public void setParent(io.vidocq.mansart.persistence.processor.TestEntity value)");
    }

    @Test
    void shouldAddDirtyTrackingInSetters() throws Exception {
        Path enhancedPath = Paths.get(GENERATED_SOURCE_ROOT, 
                "io/vidocq/mansart/persistence/processor/TestEntity_Enhanced.java");
        String content = Files.readString(enhancedPath);
        
        // Verify dirty mask updates in setters
        assertThat(content).contains("this.__mansart_dirtyMask |= (1L << 0);"); // id
        assertThat(content).contains("this.__mansart_dirtyMask |= (1L << 1);"); // name
        assertThat(content).contains("this.__mansart_dirtyMask |= (1L << 2);"); // age
        assertThat(content).contains("this.__mansart_dirtyMask |= (1L << 3);"); // parent
    }

    @Test
    void shouldAddLazyLoadingInGetters() throws Exception {
        Path enhancedPath = Paths.get(GENERATED_SOURCE_ROOT, 
                "io/vidocq/mansart/persistence/processor/TestEntity_Enhanced.java");
        String content = Files.readString(enhancedPath);
        
        // Verify lazy loading in association getters
        assertThat(content).contains("return __mansart_scoped_parent.get(() -> super.getParent());");
        assertThat(content).contains("return __mansart_scoped_children.get(() -> super.getChildren());");
        assertThat(content).contains("return __mansart_scoped_embeddedData.get(() -> super.getEmbeddedData());");
    }

    @Test
    void shouldAddDirtyTrackingMethods() throws Exception {
        Path enhancedPath = Paths.get(GENERATED_SOURCE_ROOT, 
                "io/vidocq/mansart/persistence/processor/TestEntity_Enhanced.java");
        String content = Files.readString(enhancedPath);
        
        // Verify dirty tracking methods
        assertThat(content).contains("public boolean __mansart_isDirty()");
        assertThat(content).contains("return this.__mansart_dirtyMask != 0L;");
        assertThat(content).contains("public boolean __mansart_isDirty(String fieldName)");
        assertThat(content).contains("public void __mansart_clearDirty()");
        assertThat(content).contains("this.__mansart_dirtyMask = 0L;");
    }

    @Test
    void shouldAddLazyLoadingMethods() throws Exception {
        Path enhancedPath = Paths.get(GENERATED_SOURCE_ROOT, 
                "io/vidocq/mansart/persistence/processor/TestEntity_Enhanced.java");
        String content = Files.readString(enhancedPath);
        
        // Verify lazy loading methods
        assertThat(content).contains("public void __mansart_initLazyFields()");
        assertThat(content).contains("public boolean __mansart_isLazyLoaded(String fieldName)");
    }

    @Test
    void shouldAddLicenseHeaderToEnhancedClass() throws Exception {
        Path enhancedPath = Paths.get(GENERATED_SOURCE_ROOT, 
                "io/vidocq/mansart/persistence/processor/TestEntity_Enhanced.java");
        String content = Files.readString(enhancedPath);
        
        assertThat(content).contains("Copyright (c) ${year} Yann Blazart");
        assertThat(content).contains("SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later");
    }

    @Test
    void shouldNotEnhanceNonAssociationFieldsWithLazyLoading() throws Exception {
        Path enhancedPath = Paths.get(GENERATED_SOURCE_ROOT, 
                "io/vidocq/mansart/persistence/processor/TestEntity_Enhanced.java");
        String content = Files.readString(enhancedPath);
        
        // Basic fields (id, name, age) should not have lazy loading
        assertThat(content).doesNotContain("__mansart_scoped_id");
        assertThat(content).doesNotContain("__mansart_scoped_name");
        assertThat(content).doesNotContain("__mansart_scoped_age");
        
        // Their getters should just call super
        assertThat(content).contains("return super.getId();");
        assertThat(content).contains("return super.getName();");
        assertThat(content).contains("return super.getAge();");
    }

    @Test
    void shouldGenerateEnhancedClassForEmbeddable() throws Exception {
        Path enhancedPath = Paths.get(GENERATED_SOURCE_ROOT, 
                "io/vidocq/mansart/persistence/processor/TestEmbeddable_Enhanced.java");
        
        assertThat(enhancedPath).exists();
        
        String content = Files.readString(enhancedPath);
        
        // Verify class structure
        assertThat(content).contains("public class TestEmbeddable_Enhanced extends TestEmbeddable");
        assertThat(content).contains("@Generated");
    }

    @Test
    void shouldAddImportForLazyLoadingUtils() throws Exception {
        Path enhancedPath = Paths.get(GENERATED_SOURCE_ROOT, 
                "io/vidocq/mansart/persistence/processor/TestEntity_Enhanced.java");
        String content = Files.readString(enhancedPath);
        
        assertThat(content).contains("import io.vidocq.mansart.persistence.processor.enhancer.LazyLoadingUtils;");
        assertThat(content).contains("import java.util.function.Supplier;");
    }
}
