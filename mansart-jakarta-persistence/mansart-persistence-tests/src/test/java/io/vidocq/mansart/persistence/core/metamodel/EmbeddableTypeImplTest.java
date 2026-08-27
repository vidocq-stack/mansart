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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EmbeddableTypeImpl} — a marker type.
 */
class EmbeddableTypeImplTest {

    /**
     * Verify that {@code EmbeddableTypeImpl} is-a {@code ManagedType}
     * and {@code EmbeddableType}.
     */
    @Test
    void isManagedTypeAndEmbeddableType() {
        var attrs = java.util.List.<jakarta.persistence.metamodel.Attribute<? super Object, ?>>of(
            new SingularAttributeImpl<>(
                String.class, "street", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, false, false, null)
        );
        var type = new EmbeddableTypeImpl<>(Address.class, attrs);
        assertThat(type).isInstanceOf(jakarta.persistence.metamodel.ManagedType.class);
        assertThat(type).isInstanceOf(jakarta.persistence.metamodel.EmbeddableType.class);
    }

    /**
     * Verify that {@code getJavaType()} returns the embeddable class.
     */
    @Test
    void getJavaTypeReturnsEmbeddableClass() {
        var attrs = java.util.List.<jakarta.persistence.metamodel.Attribute<? super Object, ?>>of(
            new SingularAttributeImpl<>(
                String.class, "street", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, false, false, null)
        );
        var type = new EmbeddableTypeImpl<>(Address.class, attrs);
        assertThat(type.getJavaType()).isEqualTo(Address.class);
    }

    /** Minimal embeddable fixture. */
    @jakarta.persistence.Embeddable
    static class Address {
        private String street;
        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
    }
}
