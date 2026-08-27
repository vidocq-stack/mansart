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

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;
import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SingularAttributeImpl} covering id, version,
 * optional, type, and bindable properties.
 *
 * <p>The TCK client {@code ee.jakarta.tck.persistence.core.metamodelapi.singularattribute.Client}
 * exercises these methods (tests: {@code isId}, {@code isVersion},
 * {@code isOptional}, {@code getType}, {@code getBindableType},
 * {@code getBindableJavaType}).</p>
 */
class SingularAttributeImplTest {

    /**
     * Verify that {@code isId()} returns true when constructed with id=true.
     */
    @Test
    void isIdReturnsTrue() {
        var attr = new SingularAttributeImpl<>(
                Long.class, "id", null,
                PersistentAttributeType.BASIC, true, true, false, null);
        assertThat(attr.isId()).isTrue();
    }

    /**
     * Verify that {@code isId()} returns false when constructed with id=false.
     */
    @Test
    void isIdReturnsFalse() {
        var attr = new SingularAttributeImpl<>(
                String.class, "name", null,
                PersistentAttributeType.BASIC, true, false, false, null);
        assertThat(attr.isId()).isFalse();
    }

    /**
     * Verify that {@code isVersion()} returns true when constructed with version=true.
     */
    @Test
    void isVersionReturnsTrue() {
        var attr = new SingularAttributeImpl<>(
                Integer.class, "version", null,
                PersistentAttributeType.BASIC, true, false, true, null);
        assertThat(attr.isVersion()).isTrue();
    }

    /**
     * Verify that {@code isVersion()} returns false when constructed with version=false.
     */
    @Test
    void isVersionReturnsFalse() {
        var attr = new SingularAttributeImpl<>(
                String.class, "name", null,
                PersistentAttributeType.BASIC, true, false, false, null);
        assertThat(attr.isVersion()).isFalse();
    }

    /**
     * Verify that {@code isOptional()} returns the injected value.
     */
    @Test
    void isOptionalReturnsInjectedValue() {
        var attr = new SingularAttributeImpl<>(
                String.class, "name", null,
                PersistentAttributeType.BASIC, true, false, false, null);
        assertThat(attr.isOptional()).isTrue();
    }

    /**
     * Verify that {@code isOptional()} returns false when optional=false.
     */
    @Test
    void isOptionalReturnsFalseWhenNotOptional() {
        var attr = new SingularAttributeImpl<>(
                String.class, "name", null,
                PersistentAttributeType.BASIC, false, false, false, null);
        assertThat(attr.isOptional()).isFalse();
    }

    /**
     * Verify that {@code getType()} returns the injected Type.
     */
    @Test
    void getTypeReturnsInjectedType() {
        var baseType = new TypeImpl<String>(String.class,
                Type.PersistenceType.BASIC) { };
        var attr = new SingularAttributeImpl<>(
                String.class, "name", null,
                PersistentAttributeType.BASIC, true, false, false, baseType);
        assertThat(attr.getType()).isSameAs(baseType);
        assertThat(attr.getType().getJavaType()).isEqualTo(String.class);
    }

    /**
     * Verify that {@code getType()} returns null when no Type is injected.
     */
    @Test
    void getTypeReturnsNullWhenNotInjected() {
        var attr = new SingularAttributeImpl<>(
                String.class, "name", null,
                PersistentAttributeType.BASIC, true, false, false, null);
        assertThat(attr.getType()).isNull();
    }

    /**
     * Verify that {@code getBindableType()} returns SINGULAR_ATTRIBUTE.
     */
    @Test
    void getBindableTypeReturnsSingularAttribute() {
        var attr = new SingularAttributeImpl<>(
                String.class, "name", null,
                PersistentAttributeType.BASIC, true, false, false, null);
        assertThat(attr.getBindableType()).isEqualTo(Bindable.BindableType.SINGULAR_ATTRIBUTE);
    }

    /**
     * Verify that {@code getBindableJavaType()} returns the Java type.
     */
    @Test
    void getBindableJavaTypeReturnsJavaType() {
        var attr = new SingularAttributeImpl<>(
                String.class, "name", null,
                PersistentAttributeType.BASIC, true, false, false, null);
        assertThat(attr.getBindableJavaType()).isEqualTo(String.class);
    }

    /**
     * Verify that the attribute is-a {@code SingularAttribute} and
     * {@code Attribute}.
     */
    @Test
    void isSingularAttribute() {
        var attr = new SingularAttributeImpl<>(
                String.class, "name", null,
                PersistentAttributeType.BASIC, true, false, false, null);
        assertThat(attr).isInstanceOf(SingularAttribute.class);
        assertThat(attr).isInstanceOf(jakarta.persistence.metamodel.Attribute.class);
    }
}
