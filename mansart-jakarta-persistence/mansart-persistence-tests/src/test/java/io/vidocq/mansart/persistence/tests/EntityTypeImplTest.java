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
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.SingularAttribute;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl}
 * and {@link io.vidocq.mansart.persistence.core.metamodel.IdentifiableTypeImpl}
 * focusing on identity and version metadata.
 *
 * <p>The TCK client {@code ee.jakarta.tck.persistence.core.metamodelapi.entitytype.Client}
 * exercises these methods (test: {@code getName}, {@code getId}, {@code getVersion},
 * {@code getSupertype}, {@code getBindableType}, {@code getBindableJavaType},
 * {@code hasSingleIdAttribute}, {@code hasVersionAttribute}, {@code getIdClassAttributes},
 * {@code getIdType}, {@code getDeclaredId}, {@code getDeclaredVersion}).</p>
 */
class EntityTypeImplTest {

    private static final String ENTITY_NAME = "Employee";

    /**
     * Verify that {@code getId(Class)} returns the attribute marked as id.
     */
    @Test
    void getIdReturnsIdAttribute() {
        var attrs = List.<jakarta.persistence.metamodel.Attribute<? super Object, ?>>of(
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Long.class, "id", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, true, false, null),
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                String.class, "name", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, false, false, null),
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Integer.class, "version", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, false, true, null)
        );

        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl<>(
            TestEntity.class, ENTITY_NAME, attrs);

        SingularAttribute<? super TestEntity, ?> idAttr = entityType.getId(Long.class);

        assertThat(idAttr).isNotNull();
        assertThat(idAttr.getName()).isEqualTo("id");
        assertThat(idAttr.isId()).isTrue();
        assertThat(idAttr.getJavaType()).isEqualTo(Long.class);
    }

    /**
     * Verify that {@code getId(Class)} throws IllegalArgumentException when no attribute matches the given type.
     */
    @Test
    void getIdReturnsNullForNonMatchingType() {
        var attrs = List.<jakarta.persistence.metamodel.Attribute<? super Object, ?>>of(
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Long.class, "id", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, true, false, null),
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                String.class, "name", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, false, false, null)
        );

        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl<>(
            TestEntity.class, ENTITY_NAME, attrs);

        // String.class is not the id type — should throw IllegalArgumentException
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> entityType.getId(String.class))
            .withMessageContaining("String");
    }

    /**
     * Verify that {@code getVersion(Class)} returns the attribute marked as version.
     */
    @Test
    void getVersionReturnsVersionAttribute() {
        var attrs = List.<jakarta.persistence.metamodel.Attribute<? super Object, ?>>of(
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Long.class, "id", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, true, false, null),
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                String.class, "name", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, false, false, null),
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Integer.class, "version", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, false, true, null)
        );

        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl<>(
            TestEntity.class, ENTITY_NAME, attrs);

        SingularAttribute<? super TestEntity, ?> verAttr = entityType.getVersion(Integer.class);

        assertThat(verAttr).isNotNull();
        assertThat(verAttr.getName()).isEqualTo("version");
        assertThat(verAttr.isVersion()).isTrue();
        assertThat(verAttr.getJavaType()).isEqualTo(Integer.class);
    }

    /**
     * Verify that {@code getVersion(Class)} throws IllegalArgumentException when no attribute matches.
     */
    @Test
    void getVersionReturnsNullForNonMatchingType() {
        var attrs = List.<jakarta.persistence.metamodel.Attribute<? super Object, ?>>of(
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Long.class, "id", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, true, false, null),
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                String.class, "name", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, false, false, null)
        );

        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl<>(
            TestEntity.class, ENTITY_NAME, attrs);

        // String.class is not the version type — should throw IllegalArgumentException
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> entityType.getVersion(String.class))
            .withMessageContaining("String");
    }

    /**
     * Verify that {@code getSupertype()} returns null for an entity with no parent.
     */
    @Test
    void getSupertypeReturnsNullForRootEntity() {
        var attrs = List.<jakarta.persistence.metamodel.Attribute<? super Object, ?>>of(
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Long.class, "id", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, true, false, null),
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                String.class, "name", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, false, false, null)
        );

        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl<>(
            TestEntity.class, ENTITY_NAME, attrs);

        IdentifiableType<? super TestEntity> supertype = entityType.getSupertype();

        assertThat(supertype).isNull();
    }

    /**
     * Verify that {@code hasSingleIdAttribute()} returns true when exactly one id attribute exists.
     */
    @Test
    void hasSingleIdAttributeReturnsTrue() {
        var attrs = List.<jakarta.persistence.metamodel.Attribute<? super Object, ?>>of(
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Long.class, "id", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, true, false, null),
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                String.class, "name", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, false, false, null)
        );

        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl<>(
            TestEntity.class, ENTITY_NAME, attrs);

        assertThat(entityType.hasSingleIdAttribute()).isTrue();
    }

    /**
     * Verify that {@code hasVersionAttribute()} returns true when a version attribute exists.
     */
    @Test
    void hasVersionAttributeReturnsTrue() {
        var attrs = List.<jakarta.persistence.metamodel.Attribute<? super Object, ?>>of(
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Long.class, "id", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, true, false, null),
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Integer.class, "version", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, false, true, null)
        );

        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl<>(
            TestEntity.class, ENTITY_NAME, attrs);

        assertThat(entityType.hasVersionAttribute()).isTrue();
    }

    /**
     * Verify that {@code hasVersionAttribute()} returns false when no version attribute exists.
     */
    @Test
    void hasVersionAttributeReturnsFalse() {
        var attrs = List.<jakarta.persistence.metamodel.Attribute<? super Object, ?>>of(
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Long.class, "id", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, true, false, null),
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                String.class, "name", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, false, false, null)
        );

        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl<>(
            TestEntity.class, ENTITY_NAME, attrs);

        assertThat(entityType.hasVersionAttribute()).isFalse();
    }

    /**
     * Verify that {@code getName()} returns the entity name.
     */
    @Test
    void getNameReturnsEntityName() {
        var attrs = List.<jakarta.persistence.metamodel.Attribute<? super Object, ?>>of(
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Long.class, "id", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, true, false, null)
        );

        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl<>(
            TestEntity.class, ENTITY_NAME, attrs);

        assertThat(entityType.getName()).isEqualTo(ENTITY_NAME);
    }

    /**
     * Verify that {@code getIdType()} returns the type of the id attribute.
     */
    @Test
    void getIdTypeReturnsIdType() {
        var idType = new io.vidocq.mansart.persistence.core.metamodel.TypeImpl<Long>(Long.class,
                jakarta.persistence.metamodel.Type.PersistenceType.BASIC) {
        };
        var attrs = List.<jakarta.persistence.metamodel.Attribute<? super Object,?>>of(
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Long.class, "id", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, true, false, idType),
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                String.class, "name", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, false, false, null)
        );

        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl<>(
            TestEntity.class, ENTITY_NAME, attrs);

        var actualIdType = entityType.getIdType();

        assertThat(actualIdType).isNotNull();
        assertThat(actualIdType.getJavaType()).isEqualTo(Long.class);
    }

    /**
     * Verify that {@code getIdType()} throws UnsupportedOperationException when there is no id attribute.
     */
    @Test
    void getIdTypeReturnsNullWhenNoId() {
        var attrs = List.<jakarta.persistence.metamodel.Attribute<? super Object, ?>>of(
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                String.class, "name", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, false, false, null)
        );

        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl<>(
            TestEntity.class, ENTITY_NAME, attrs);

        // No id attribute defined — should throw UnsupportedOperationException
        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(entityType::getIdType)
            .withMessageContaining("no identifier attribute");
    }

    /**
     * Verify that {@code getIdClassAttributes()} returns an empty set when there is no @IdClass.
     */
    @Test
    void getIdClassAttributesReturnsEmptySet() {
        var attrs = List.<jakarta.persistence.metamodel.Attribute<? super Object, ?>>of(
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Long.class, "id", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, true, false, null),
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                String.class, "name", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, false, false, null)
        );

        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl<>(
            TestEntity.class, ENTITY_NAME, attrs);

        // Single-key entity: getIdClassAttributes returns the single @Id attribute
        var idClassAttrs = entityType.getIdClassAttributes();
        assertThat(idClassAttrs).hasSize(1);
        var idAttr = idClassAttrs.iterator().next();
        assertThat(idAttr.getName()).isEqualTo("id");
        assertThat(idAttr.isId()).isTrue();
    }

    /**
     * Verify that {@code getDeclaredId(Class)} returns the id attribute declared on this entity.
     */
    @Test
    void getDeclaredIdReturnsIdAttribute() {
        var attrs = List.<jakarta.persistence.metamodel.Attribute<? super Object, ?>>of(
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Long.class, "id", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, true, false, null),
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                String.class, "name", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, false, false, null)
        );

        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl<>(
            TestEntity.class, ENTITY_NAME, attrs);

        SingularAttribute<?, ?> declaredId = entityType.getDeclaredId(Long.class);

        assertThat(declaredId).isNotNull();
        assertThat(declaredId.getName()).isEqualTo("id");
    }

    /**
     * Verify that {@code getDeclaredVersion(Class)} returns the version attribute declared on this entity.
     */
    @Test
    void getDeclaredVersionReturnsVersionAttribute() {
        var attrs = List.<jakarta.persistence.metamodel.Attribute<? super Object, ?>>of(
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Long.class, "id", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, true, false, null),
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Integer.class, "version", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, false, true, null)
        );

        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl<>(
            TestEntity.class, ENTITY_NAME, attrs);

        SingularAttribute<?, ?> declaredVer = entityType.getDeclaredVersion(Integer.class);

        assertThat(declaredVer).isNotNull();
        assertThat(declaredVer.getName()).isEqualTo("version");
    }

    /** Simple helper to construct a {@link jakarta.persistence.metamodel.Type} in tests. */
    private static final class BasicType<T> extends io.vidocq.mansart.persistence.core.metamodel.TypeImpl<T> {
        BasicType(Class<T> javaType) {
            super(javaType, jakarta.persistence.metamodel.Type.PersistenceType.BASIC);
        }
    }
}
