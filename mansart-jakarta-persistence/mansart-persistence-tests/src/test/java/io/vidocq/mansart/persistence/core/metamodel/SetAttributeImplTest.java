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
 * Unit tests for {@link SetAttributeImpl} — a marker type.
 */
class SetAttributeImplTest {

    /**
     * Verify that {@code SetAttributeImpl} is-a {@code PluralAttribute}
     * and {@code SetAttribute}.
     */
    @Test
    void isPluralAttributeAndSetAttribute() {
        @SuppressWarnings("rawtypes")
        var type = new TypeImpl<String>(String.class, Type.PersistenceType.BASIC) { };
        @SuppressWarnings("rawtypes")
        var attr = new SetAttributeImpl<>(
                java.util.Set.class, "items", null, type);
        assertThat(attr).isInstanceOf(jakarta.persistence.metamodel.PluralAttribute.class);
        assertThat(attr).isInstanceOf(jakarta.persistence.metamodel.SetAttribute.class);
    }

    /**
     * Verify that {@code getCollectionType()} returns SET.
     */
    @Test
    void getCollectionTypeReturnsSet() {
        @SuppressWarnings("rawtypes")
        var type = new TypeImpl<String>(String.class, Type.PersistenceType.BASIC) { };
        @SuppressWarnings("rawtypes")
        var attr = new SetAttributeImpl<>(
                java.util.Set.class, "items", null, type);
        assertThat(attr.getCollectionType())
                .isEqualTo(jakarta.persistence.metamodel.PluralAttribute.CollectionType.SET);
    }

    /**
     * Verify that {@code isCollection()} returns true.
     */
    @Test
    void isCollectionReturnsTrue() {
        @SuppressWarnings("rawtypes")
        var type = new TypeImpl<String>(String.class, Type.PersistenceType.BASIC) { };
        @SuppressWarnings("rawtypes")
        var attr = new SetAttributeImpl<>(
                java.util.Set.class, "items", null, type);
        assertThat(attr.isCollection()).isTrue();
    }
}
