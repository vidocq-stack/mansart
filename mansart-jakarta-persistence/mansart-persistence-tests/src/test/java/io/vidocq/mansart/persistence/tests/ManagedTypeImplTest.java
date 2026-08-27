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

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link io.vidocq.mansart.persistence.core.metamodel.ManagedTypeImpl}
 * collection lookup methods (overloaded variants with element-type parameter).
 *
 * <p>The TCK client {@code ee.jakarta.tck.persistence.core.metamodelapi.managedtype.Client}
 * exercises these methods (test: {@code getCollectionStringTest},
 * {@code getDeclaredListStringTest}, {@code getPluralAttributes},
 * {@code getDeclaredPluralAttributes}).</p>
 */
class ManagedTypeImplTest {

    private static final String ENTITY_NAME = "Person";

    /**
     * A minimal Type subclass for use in tests.
     */
    private static final class BasicType<T> extends
            io.vidocq.mansart.persistence.core.metamodel.TypeImpl<T> {
        BasicType(Class<T> javaType) {
            super(javaType, Type.PersistenceType.BASIC);
        }
    }

    /**
     * Verify that {@code getCollection(String)} returns the collection attribute
     * matching the given name.
     */
    @Test
    @SuppressWarnings("rawtypes")
    void getCollectionStringReturnsMatchingCollection() {
        @SuppressWarnings("rawtypes")
        var collectionAttr = new io.vidocq.mansart.persistence.core.metamodel.CollectionAttributeImpl<TestEntity, TestEntity>(
                java.util.Collection.class, "projects", null,
                new BasicType<TestEntity>(TestEntity.class));

        @SuppressWarnings("rawtypes")
        List<Attribute<? super Object, ?>> attrs = (List) Arrays.asList(
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Long.class, "id", null,
                Attribute.PersistentAttributeType.BASIC, true, true, false, null),
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                String.class, "name", null,
                Attribute.PersistentAttributeType.BASIC, true, false, false, null),
            collectionAttr);

        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl<>(
            TestEntity.class, ENTITY_NAME, attrs);

        @SuppressWarnings("unchecked")
        var result = entityType.getCollection("projects");

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("projects");
        assertThat(result.getCollectionType()).isEqualTo(PluralAttribute.CollectionType.COLLECTION);
    }

    /**
     * Verify that {@code getCollection(String)} returns null when no matching
     * collection attribute exists.
     */
    @Test
    void getCollectionStringReturnsNullWhenNotFound() {
        @SuppressWarnings("rawtypes")
        List<Attribute<? super Object, ?>> attrs = (List) Arrays.asList(
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Long.class, "id", null,
                Attribute.PersistentAttributeType.BASIC, true, true, false, null)
        );

        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl<>(
            TestEntity.class, ENTITY_NAME, attrs);

        var result = entityType.getCollection("projects");

        assertThat(result).isNull();
    }

    /**
     * Verify that {@code getCollection(String, Class<E>) returns the collection
     * attribute matching the given name and element type.
     */
    @Test
    @SuppressWarnings("rawtypes")
    void getCollectionStringClassReturnsMatchingCollection() {
        var collectionAttr = new io.vidocq.mansart.persistence.core.metamodel.CollectionAttributeImpl<TestEntity, TestEntity>(
                java.util.Collection.class, "projects", null,
                new BasicType<TestEntity>(TestEntity.class));

        @SuppressWarnings("rawtypes")
        List<Attribute<? super Object, ?>> attrs = (List) Arrays.asList(
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Long.class, "id", null,
                Attribute.PersistentAttributeType.BASIC, true, true, false, null),
            collectionAttr);

        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl<>(
            TestEntity.class, ENTITY_NAME, attrs);

        @SuppressWarnings("unchecked")
        var result = entityType.getCollection("projects", TestEntity.class);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("projects");
        assertThat(result.getJavaType()).isEqualTo(java.util.Collection.class);
    }

    /**
     * Verify that {@code getCollection(String, Class<E>) returns null when the
     * element type does not match.
     */
    @Test
    @SuppressWarnings("rawtypes")
    void getCollectionStringClassReturnsNullForWrongElementType() {
        var collectionAttr = new io.vidocq.mansart.persistence.core.metamodel.CollectionAttributeImpl<TestEntity, TestEntity>(
                java.util.Collection.class, "projects", null,
                new BasicType<TestEntity>(TestEntity.class));

        @SuppressWarnings("rawtypes")
        List<Attribute<? super Object, ?>> attrs = (List) Arrays.asList(collectionAttr);

        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl<>(
            TestEntity.class, ENTITY_NAME, attrs);

        // String.class is not the element type of "projects"
        @SuppressWarnings("unchecked")
        var result = entityType.getCollection("projects", String.class);

        assertThat(result).isNull();
    }

    /**
     * Verify that {@code getList(String, Class<E>) returns the list attribute
     * matching the given name and element type.
     */
    @Test
    @SuppressWarnings("rawtypes")
    void getListStringClassReturnsMatchingList() {
        @SuppressWarnings("rawtypes")
        List<Attribute<? super Object, ?>> attrs = (List) Arrays.asList(
            new io.vidocq.mansart.persistence.core.metamodel.SingularAttributeImpl<>(
                Long.class, "id", null,
                Attribute.PersistentAttributeType.BASIC, true, true, false, null),
            new io.vidocq.mansart.persistence.core.metamodel.ListAttributeImpl<>(
                java.util.List.class, "biDirMX1Persons", null,
                new BasicType<TestEntity>(TestEntity.class))
        );

        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl<>(
            TestEntity.class, ENTITY_NAME, attrs);

        @SuppressWarnings("unchecked")
        var result = entityType.getList("biDirMX1Persons", TestEntity.class);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("biDirMX1Persons");
        assertThat(result.getCollectionType()).isEqualTo(PluralAttribute.CollectionType.LIST);
    }

    /**
     * Verify that {@code getSet(String, Class<E>) returns the set attribute
     * matching the given name and element type.
     */
    @Test
    @SuppressWarnings("rawtypes")
    void getSetStringClassReturnsMatchingSet() {
        @SuppressWarnings("rawtypes")
        List<Attribute<? super Object, ?>> attrs = (List) Arrays.asList(
            new io.vidocq.mansart.persistence.core.metamodel.SetAttributeImpl<>(
                java.util.Set.class, "projects", null,
                new BasicType<TestEntity>(TestEntity.class))
        );

        var entityType = new io.vidocq.mansart.persistence.core.metamodel.EntityTypeImpl<>(
            TestEntity.class, ENTITY_NAME, attrs);

        @SuppressWarnings("unchecked")
        var result = entityType.getSet("projects", TestEntity.class);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("projects");
        assertThat(result.getCollectionType()).isEqualTo(PluralAttribute.CollectionType.SET);
    }
}
