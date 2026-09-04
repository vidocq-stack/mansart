/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.core.runtime.RuntimeEntityModelBuilder;
import io.vidocq.mansart.persistence.spi.Attribute;
import io.vidocq.mansart.persistence.spi.EntityModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for M3-JP-19: Runtime EntityModelBuilder (Tier 3 fallback).
 * Builds EntityModel from class file bytes at bootstrap for entities
 * that neither APT nor the Maven plugin reached.
 */
public class RuntimeEntityModelBuilderTest {

    @Test
    public void testBuildEntityModelFromTestClass() {
        RuntimeEntityModelBuilder builder = new RuntimeEntityModelBuilder();
        EntityModel<UnprocessedEntity> model = builder.build(UnprocessedEntity.class);

        assertThat(model).isNotNull();
        assertThat(model.getEntityClass()).isEqualTo(UnprocessedEntity.class);
        assertThat(model.getTableName()).isEqualTo("unprocessed_entities");
    }

    @Test
    public void testBuildEntityModelExtractsAttributes() {
        RuntimeEntityModelBuilder builder = new RuntimeEntityModelBuilder();
        EntityModel<UnprocessedEntity> model = builder.build(UnprocessedEntity.class);

        assertThat(model.getAttributes()).hasSize(4);
        assertThat(model.getAttributes())
                .extracting(Attribute::getName)
                .containsExactlyInAnyOrder("id", "name", "email", "version");
    }

    @Test
    public void testBuildEntityModelExtractsIdAttribute() {
        RuntimeEntityModelBuilder builder = new RuntimeEntityModelBuilder();
        EntityModel<UnprocessedEntity> model = builder.build(UnprocessedEntity.class);

        assertThat(model.getIdAttributes()).hasSize(1);
        Attribute<?, ?> idAttr = model.getIdAttributes().get(0);
        assertThat(idAttr.getName()).isEqualTo("id");
        assertThat(idAttr.isId()).isTrue();
    }

    @Test
    public void testBuildEntityModelExtractsVersionAttribute() {
        RuntimeEntityModelBuilder builder = new RuntimeEntityModelBuilder();
        EntityModel<UnprocessedEntity> model = builder.build(UnprocessedEntity.class);

        Attribute<?, ?> versionAttr = model.getVersionAttribute();
        assertThat(versionAttr).isNotNull();
        assertThat(versionAttr.getName()).isEqualTo("version");
        assertThat(versionAttr.isVersion()).isTrue();
    }

    @Test
    public void testBuildEntityModelExtractsColumnNames() {
        RuntimeEntityModelBuilder builder = new RuntimeEntityModelBuilder();
        EntityModel<UnprocessedEntity> model = builder.build(UnprocessedEntity.class);

        Attribute<?, ?> nameAttr = model.getAttribute("name");
        assertThat(nameAttr).isNotNull();
        assertThat(nameAttr.getColumnName()).isEqualTo("name");
        assertThat(nameAttr.isNullable()).isFalse();

        Attribute<?, ?> emailAttr = model.getAttribute("email");
        assertThat(emailAttr).isNotNull();
        assertThat(emailAttr.getColumnName()).isEqualTo("email");
    }

    @Test
    public void testBuildEntityModelHandlesCustomTableName() {
        RuntimeEntityModelBuilder builder = new RuntimeEntityModelBuilder();
        EntityModel<CustomTableEntity> model = builder.build(CustomTableEntity.class);

        assertThat(model.getTableName()).isEqualTo("custom_table");
    }

    // --- Test entities (NOT processed by APT or Maven plugin — simulating tier-3) ---

    @jakarta.persistence.Entity
    @jakarta.persistence.Table(name = "unprocessed_entities")
    public static class UnprocessedEntity {

        @jakarta.persistence.Id
        private Long id;

        @jakarta.persistence.Column(nullable = false)
        private String name;

        @jakarta.persistence.Column
        private String email;

        @jakarta.persistence.Version
        private int version;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public int getVersion() { return version; }
        public void setVersion(int version) { this.version = version; }
    }

    @jakarta.persistence.Entity
    @jakarta.persistence.Table(name = "custom_table")
    public static class CustomTableEntity {

        @jakarta.persistence.Id
        private Long id;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }
}
