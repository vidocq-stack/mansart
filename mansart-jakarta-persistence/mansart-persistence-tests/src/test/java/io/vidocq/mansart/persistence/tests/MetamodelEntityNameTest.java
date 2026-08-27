/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.SingularAttribute;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link io.vidocq.mansart.persistence.core.metamodel.MetamodelImpl}
 * focusing on {@code entity(String)} — the lookup by entity name.
 *
 * <p>The TCK client {@code ee.jakarta.tck.persistence.core.metamodelapi.metamodel.Client}
 * exercises this method (test {@code entity()}, {@code entityIllegalArgumentException()}).
 * The full TCK client is blocked by the stub provider (JP-03); this unit test
 * verifies the behaviour directly.</p>
 */
class MetamodelEntityNameTest {

    private static final String ENTITY_NAME = "Employee";
    private static final String ANOTHER_NAME = "Department";

    /**
     * Verify that {@code entity(String)} resolves an entity by its declared name.
     */
    @Test
    void entityByNameReturnsEntityType() {
        var metamodel = buildMetamodelWithEntities();

        EntityType<?> result = metamodel.entity(ENTITY_NAME);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(ENTITY_NAME);
        assertThat(result.getJavaType()).isEqualTo(TestEntity.class);
    }

    /**
     * Verify that {@code entity(String)} throws for an unknown name.
     */
    @Test
    void entityByNameThrowsForUnknownName() {
        var metamodel = buildMetamodelWithEntities();

        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> metamodel.entity("NonExistent"))
            .withMessageContaining("NonExistent");
    }

    /**
     * Verify that {@code entity(String)} throws for embeddables (not entities).
     * Per the spec, entity(String) only returns EntityTypes and throws for any
     * name that does not match an entity.
     */
    @Test
    void entityByNameThrowsForNonEntityTypes() {
        var metamodel = buildMetamodelWithEmbeddable();

        // Embeddable named "Department" should throw — entity(String) only returns EntityTypes
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> metamodel.entity(ANOTHER_NAME))
            .withMessageContaining("Department");
    }

    private io.vidocq.mansart.persistence.core.metamodel.MetamodelImpl buildMetamodelWithEntities() {
        var attrs = List.<jakarta.persistence.metamodel.Attribute<? super Object, ?>>of(
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Long.class, "id", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, true, false, null),
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                String.class, "name", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, false, false, null),
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Integer.class, "age", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, false, false, null),
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Integer.class, "version", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, false, true, null)
        );

        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl(
            TestEntity.class, ENTITY_NAME, attrs);

        return new io.vidocq.mansart.persistence.core.metamodel.MetamodelImpl(
            List.of(entityType));
    }

    private io.vidocq.mansart.persistence.core.metamodel.MetamodelImpl buildMetamodelWithEmbeddable() {
        var attrs = List.<jakarta.persistence.metamodel.Attribute<? super Object, ?>>of(
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                String.class, "code", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, false, false, null)
        );

        var embeddableType = new io.vidocq.mansart.persistence.core.metamodel.EmbeddableTypeImpl(
            Department.class, attrs);

        return new io.vidocq.mansart.persistence.core.metamodel.MetamodelImpl(
            List.of(embeddableType));
    }

    // Minimal embeddable fixture
    @jakarta.persistence.Embeddable
    static class Department {
        private String code;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}
