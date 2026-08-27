/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.metamodel;

import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.Type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PluralAttributeImpl} and {@link CollectionAttributeImpl}.
 *
 * <p>The TCK clients {@code singularattribute/Client} (isId, isVersion, isOptional,
 * getType, isCollection, isAssociation) and {@code collectionattribute/Client}
 * (getCollectionType, getElementType) exercise these implementations.</p>
 */
class PluralAttributeImplTest {

    /** Helper to create a {@link Type} in tests (wraps {@link TypeImpl}). */
    private static final class BasicType<T> extends TypeImpl<T> {
        BasicType(Class<T> javaType) {
            super(javaType, Type.PersistenceType.BASIC);
        }
    }

    /** Helper to create a {@link ManagedType} in tests (wraps {@link ManagedTypeImpl}). */
    private static final class TestManagedType<X> extends ManagedTypeImpl<X> {
        TestManagedType(Class<X> javaType) {
            super(javaType, Type.PersistenceType.ENTITY, null);
        }
    }

    @Test
    void getPersistentAttributeTypeReturnsOneToMany() {
        var attr = new PluralAttributeImpl<>(
                java.util.Collection.class, "projects",
                new TestManagedType<>(Object.class),
                PluralAttribute.CollectionType.COLLECTION,
                PluralAttribute.PersistentAttributeType.ONE_TO_MANY,
                new BasicType<>(String.class));
        assertEquals(PluralAttribute.PersistentAttributeType.ONE_TO_MANY,
                attr.getPersistentAttributeType());
    }

    @Test
    void getPersistentAttributeTypeReturnsManyToMany() {
        var attr = new PluralAttributeImpl<>(
                java.util.Set.class, "employees",
                new TestManagedType<>(Object.class),
                PluralAttribute.CollectionType.SET,
                PluralAttribute.PersistentAttributeType.MANY_TO_MANY,
                new BasicType<>(String.class));
        assertEquals(PluralAttribute.PersistentAttributeType.MANY_TO_MANY,
                attr.getPersistentAttributeType());
    }

    @Test
    void getPersistentAttributeTypeReturnsManyToOne() {
        var attr = new PluralAttributeImpl<>(
                java.util.Collection.class, "department",
                new TestManagedType<>(Object.class),
                PluralAttribute.CollectionType.COLLECTION,
                PluralAttribute.PersistentAttributeType.MANY_TO_ONE,
                new BasicType<>(String.class));
        assertEquals(PluralAttribute.PersistentAttributeType.MANY_TO_ONE,
                attr.getPersistentAttributeType());
    }

    @Test
    void getPersistentAttributeTypeReturnsOneToOne() {
        var attr = new PluralAttributeImpl<>(
                java.util.Collection.class, "profile",
                new TestManagedType<>(Object.class),
                PluralAttribute.CollectionType.COLLECTION,
                PluralAttribute.PersistentAttributeType.ONE_TO_ONE,
                new BasicType<>(String.class));
        assertEquals(PluralAttribute.PersistentAttributeType.ONE_TO_ONE,
                attr.getPersistentAttributeType());
    }

    @Test
    void getPersistentAttributeTypeReturnsElementCollection() {
        var attr = new PluralAttributeImpl<>(
                java.util.List.class, "tags",
                new TestManagedType<>(Object.class),
                PluralAttribute.CollectionType.LIST,
                PluralAttribute.PersistentAttributeType.ELEMENT_COLLECTION,
                new BasicType<>(String.class));
        assertEquals(PluralAttribute.PersistentAttributeType.ELEMENT_COLLECTION,
                attr.getPersistentAttributeType());
    }

    @Test
    void getPersistentAttributeTypeReturnsBasic() {
        var attr = new PluralAttributeImpl<>(
                java.util.Collection.class, "items",
                new TestManagedType<>(Object.class),
                PluralAttribute.CollectionType.COLLECTION,
                PluralAttribute.PersistentAttributeType.BASIC,
                new BasicType<>(String.class));
        assertEquals(PluralAttribute.PersistentAttributeType.BASIC,
                attr.getPersistentAttributeType());
    }

    @Test
    void getCollectionTypeReturnsCollection() {
        var attr = new PluralAttributeImpl<>(
                java.util.Collection.class, "projects",
                new TestManagedType<>(Object.class),
                PluralAttribute.CollectionType.COLLECTION,
                PluralAttribute.PersistentAttributeType.ONE_TO_MANY,
                new BasicType<>(String.class));
        assertEquals(PluralAttribute.CollectionType.COLLECTION, attr.getCollectionType());
    }

    @Test
    void getCollectionTypeReturnsSet() {
        var attr = new PluralAttributeImpl<>(
                java.util.Set.class, "tags",
                new TestManagedType<>(Object.class),
                PluralAttribute.CollectionType.SET,
                PluralAttribute.PersistentAttributeType.ELEMENT_COLLECTION,
                new BasicType<>(String.class));
        assertEquals(PluralAttribute.CollectionType.SET, attr.getCollectionType());
    }

    @Test
    void getCollectionTypeReturnsList() {
        var attr = new PluralAttributeImpl<>(
                java.util.List.class, "items",
                new TestManagedType<>(Object.class),
                PluralAttribute.CollectionType.LIST,
                PluralAttribute.PersistentAttributeType.BASIC,
                new BasicType<>(String.class));
        assertEquals(PluralAttribute.CollectionType.LIST, attr.getCollectionType());
    }

    @Test
    void getCollectionTypeReturnsMap() {
        var attr = new PluralAttributeImpl<>(
                java.util.Map.class, "properties",
                new TestManagedType<>(Object.class),
                PluralAttribute.CollectionType.MAP,
                PluralAttribute.PersistentAttributeType.BASIC,
                new BasicType<>(Integer.class));
        assertEquals(PluralAttribute.CollectionType.MAP, attr.getCollectionType());
    }

    @Test
    void getElementTypeReturnsCorrectType() {
        var attr = new PluralAttributeImpl<>(
                java.util.Collection.class, "projects",
                new TestManagedType<>(Object.class),
                PluralAttribute.CollectionType.COLLECTION,
                PluralAttribute.PersistentAttributeType.ONE_TO_MANY,
                new BasicType<>(String.class));
        assertEquals(String.class, attr.getElementType().getJavaType());
    }

    @Test
    void collectionAttributeImplementsCollectionInterface() {
        @SuppressWarnings("rawtypes")
        var attr = new CollectionAttributeImpl<>(
                java.util.Collection.class,
                "projects",
                new TestManagedType<>(Object.class),
                new BasicType<>(String.class));
        assertInstanceOf(jakarta.persistence.metamodel.CollectionAttribute.class, attr);
        assertEquals(PluralAttribute.CollectionType.COLLECTION, attr.getCollectionType());
    }

    @Test
    void getNameReturnsAttributeName() {
        var attr = new PluralAttributeImpl<>(
                java.util.Collection.class, "projects",
                new TestManagedType<>(Object.class),
                PluralAttribute.CollectionType.COLLECTION,
                PluralAttribute.PersistentAttributeType.ONE_TO_MANY,
                new BasicType<>(String.class));
        assertEquals("projects", attr.getName());
    }

    @Test
    void isCollectionReturnsTrue() {
        var attr = new PluralAttributeImpl<>(
                java.util.Collection.class, "projects",
                new TestManagedType<>(Object.class),
                PluralAttribute.CollectionType.COLLECTION,
                PluralAttribute.PersistentAttributeType.ONE_TO_MANY,
                new BasicType<>(String.class));
        assertTrue(attr.isCollection());
    }

    @Test
    void getBindableTypeReturnsPluralAttribute() {
        var attr = new PluralAttributeImpl<>(
                java.util.Collection.class, "projects",
                new TestManagedType<>(Object.class),
                PluralAttribute.CollectionType.COLLECTION,
                PluralAttribute.PersistentAttributeType.ONE_TO_MANY,
                new BasicType<>(String.class));
        assertEquals(jakarta.persistence.metamodel.Bindable.BindableType.PLURAL_ATTRIBUTE,
                attr.getBindableType());
    }

    @Test
    void getBindableJavaTypeReturnsElementType() {
        var attr = new PluralAttributeImpl<>(
                java.util.Collection.class, "projects",
                new TestManagedType<>(Object.class),
                PluralAttribute.CollectionType.COLLECTION,
                PluralAttribute.PersistentAttributeType.ONE_TO_MANY,
                new BasicType<>(String.class));
        assertEquals(String.class, attr.getBindableJavaType());
    }
}
