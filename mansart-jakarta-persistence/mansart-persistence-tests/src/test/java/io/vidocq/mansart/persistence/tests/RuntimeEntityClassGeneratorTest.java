/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.core.runtime.RuntimeEntityClassGenerator;
import io.vidocq.mansart.persistence.spi.EntityAccessor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for M3-JP-20: RuntimeRepositoryClassGenerator (hidden class generation for tier-3 field access).
 * Generates hidden classes via MethodHandles.Lookup.defineHiddenClass using the Class-File API.
 */
public class RuntimeEntityClassGeneratorTest {

    @Test
    public void testGenerateAccessorCanReadFields() {
        RuntimeEntityClassGenerator generator = new RuntimeEntityClassGenerator();
        EntityAccessor<SimpleEntity> accessor = generator.generateAccessor(SimpleEntity.class);

        SimpleEntity entity = new SimpleEntity();
        entity.setId(42L);
        entity.setName("test");

        assertThat(accessor.get(entity, "id")).isEqualTo(42L);
        assertThat(accessor.get(entity, "name")).isEqualTo("test");
    }

    @Test
    public void testGenerateAccessorCanWriteFields() {
        RuntimeEntityClassGenerator generator = new RuntimeEntityClassGenerator();
        EntityAccessor<SimpleEntity> accessor = generator.generateAccessor(SimpleEntity.class);

        SimpleEntity entity = new SimpleEntity();
        accessor.set(entity, "id", 99L);
        accessor.set(entity, "name", "changed");

        assertThat(entity.getId()).isEqualTo(99L);
        assertThat(entity.getName()).isEqualTo("changed");
    }

    @Test
    public void testGenerateAccessorForEntityWithVersion() {
        RuntimeEntityClassGenerator generator = new RuntimeEntityClassGenerator();
        EntityAccessor<VersionedEntity> accessor = generator.generateAccessor(VersionedEntity.class);

        VersionedEntity entity = new VersionedEntity();
        entity.setId(1L);
        entity.setVersion(5);

        assertThat(accessor.get(entity, "id")).isEqualTo(1L);
        assertThat(accessor.get(entity, "version")).isEqualTo(5);

        accessor.set(entity, "version", 10);
        assertThat(entity.getVersion()).isEqualTo(10);
    }

    // --- Test entities ---

    @jakarta.persistence.Entity
    public static class SimpleEntity {
        @jakarta.persistence.Id
        private Long id;
        private String name;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @jakarta.persistence.Entity
    public static class VersionedEntity {
        @jakarta.persistence.Id
        private Long id;
        @jakarta.persistence.Version
        private int version;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public int getVersion() { return version; }
        public void setVersion(int version) { this.version = version; }
    }
}
