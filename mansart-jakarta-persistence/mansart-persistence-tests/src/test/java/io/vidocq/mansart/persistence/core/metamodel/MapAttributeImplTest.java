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

import jakarta.persistence.metamodel.Type;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MapAttributeImpl} — a marker type.
 */
class MapAttributeImplTest {

    /**
     * Verify that {@code MapAttributeImpl} is-a {@code PluralAttribute}
     * and {@code MapAttribute}.
     */
    @Test
    void isPluralAttributeAndMapAttribute() {
        @SuppressWarnings("rawtypes")
        var keyType = new TypeImpl<String>(String.class, Type.PersistenceType.BASIC) { };
        @SuppressWarnings("rawtypes")
        var valueType = new TypeImpl<Integer>(Integer.class, Type.PersistenceType.BASIC) { };
        @SuppressWarnings("rawtypes")
        var attr = new MapAttributeImpl<>(
                java.util.Map.class, "items", null, keyType, String.class, valueType);
        assertThat(attr).isInstanceOf(jakarta.persistence.metamodel.PluralAttribute.class);
        assertThat(attr).isInstanceOf(jakarta.persistence.metamodel.MapAttribute.class);
    }

    /**
     * Verify that {@code getCollectionType()} returns MAP.
     */
    @Test
    void getCollectionTypeReturnsMap() {
        @SuppressWarnings("rawtypes")
        var keyType = new TypeImpl<String>(String.class, Type.PersistenceType.BASIC) { };
        @SuppressWarnings("rawtypes")
        var valueType = new TypeImpl<Integer>(Integer.class, Type.PersistenceType.BASIC) { };
        @SuppressWarnings("rawtypes")
        var attr = new MapAttributeImpl<>(
                java.util.Map.class, "items", null, keyType, String.class, valueType);
        assertThat(attr.getCollectionType())
                .isEqualTo(jakarta.persistence.metamodel.PluralAttribute.CollectionType.MAP);
    }

    /**
     * Verify that {@code isCollection()} returns true.
     */
    @Test
    void isCollectionReturnsTrue() {
        @SuppressWarnings("rawtypes")
        var keyType = new TypeImpl<String>(String.class, Type.PersistenceType.BASIC) { };
        @SuppressWarnings("rawtypes")
        var valueType = new TypeImpl<Integer>(Integer.class, Type.PersistenceType.BASIC) { };
        @SuppressWarnings("rawtypes")
        var attr = new MapAttributeImpl<>(
                java.util.Map.class, "items", null, keyType, String.class, valueType);
        assertThat(attr.isCollection()).isTrue();
    }
}
