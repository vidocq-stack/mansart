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

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link AttributeImpl} covering name, declaring type,
 * persistent attribute type, and stub behaviour.
 *
 * <p>The TCK client {@code ee.jakarta.tck.persistence.core.metamodelapi.singularattribute.Client}
 * exercises these methods (tests: {@code getName}, {@code getPersistentAttributeType},
 * {@code getDeclaringType}, {@code getJavaType}, {@code isAssociation},
 * {@code isCollection}, {@code getJavaMember}).</p>
 */
class AttributeImplTest {

    /**
     * A minimal concrete AttributeImpl for testing.
     */
    private static final class TestAttribute extends AttributeImpl<Object, String> {
        TestAttribute(String name, ManagedType<?> declaringType) {
            super(String.class, name, declaringType,
                  PersistentAttributeType.BASIC, true);
        }
    }

    /**
     * Verify that {@code getName()} returns the attribute name.
     */
    @Test
    void getNameReturnsAttributeName() {
        var attr = new TestAttribute("fieldName", null);
        assertThat(attr.getName()).isEqualTo("fieldName");
    }

    /**
     * Verify that {@code getPersistentAttributeType()} returns the injected type.
     */
    @Test
    void getPersistentAttributeTypeReturnsInjectedType() {
        var attr = new TestAttribute("fieldName", null);
        assertThat(attr.getPersistentAttributeType())
                .isEqualTo(PersistentAttributeType.BASIC);
    }

    /**
     * Verify that {@code getDeclaringType()} returns the declaring type.
     */
    @Test
    void getDeclaringTypeReturnsDeclaringType() {
        var attrs = java.util.List.<jakarta.persistence.metamodel.Attribute<? super Object, ?>>of(
            new SingularAttributeImpl<>(
                Long.class, "id", null,
                PersistentAttributeType.BASIC, true, true, false, null)
        );
        var entityType = new EntityTypeImpl(Object.class, "Test", attrs);

        var attr = new TestAttribute("fieldName", entityType);
        assertThat(attr.getDeclaringType()).isSameAs(entityType);
    }

    /**
     * Verify that {@code getJavaType()} returns the Java class.
     */
    @Test
    void getJavaTypeReturnsJavaClass() {
        var attr = new TestAttribute("fieldName", null);
        assertThat(attr.getJavaType()).isEqualTo(String.class);
    }

    /**
     * Verify that {@code isAssociation()} returns false.
     */
    @Test
    void isAssociationReturnsFalse() {
        var attr = new TestAttribute("fieldName", null);
        assertThat(attr.isAssociation()).isFalse();
    }

    /**
     * Verify that {@code isCollection()} returns false.
     */
    @Test
    void isCollectionReturnsFalse() {
        var attr = new TestAttribute("fieldName", null);
        assertThat(attr.isCollection()).isFalse();
    }

    /**
     * Verify that {@code getJavaMember()} throws
     * {@code UnsupportedOperationException}.
     */
    @Test
    void getJavaMemberThrowsUnsupportedOperationException() {
        var attr = new TestAttribute("fieldName", null);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(attr::getJavaMember)
                .withMessageContaining("not implemented");
    }
}
