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
 * Unit tests for {@link MappedSuperclassTypeImpl} — a marker type.
 */
class MappedSuperclassTypeImplTest {

    /**
     * Verify that {@code MappedSuperclassTypeImpl} is-a {@code ManagedType}
     * and {@code MappedSuperclassType}.
     */
    @Test
    void isManagedTypeAndMappedSuperclassType() {
        var attrs = java.util.List.<jakarta.persistence.metamodel.Attribute<? super Object, ?>>of(
            new SingularAttributeImpl<>(
                Long.class, "id", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, true, false, null)
        );
        var type = new MappedSuperclassTypeImpl<>(BaseEntity.class, "BaseEntity",
                attrs, null, null, null, true);
        assertThat(type).isInstanceOf(jakarta.persistence.metamodel.ManagedType.class);
        assertThat(type).isInstanceOf(jakarta.persistence.metamodel.MappedSuperclassType.class);
    }

    /** Minimal entity fixture. */
    @jakarta.persistence.MappedSuperclass
    static class BaseEntity {
        private Long id;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }
}
