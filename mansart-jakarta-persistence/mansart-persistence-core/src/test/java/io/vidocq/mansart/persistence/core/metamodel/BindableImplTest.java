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
 * Unit tests for {@link BindableImpl} — a marker type.
 */
class BindableImplTest {

    /**
     * Verify that {@code SingularAttributeImpl} is-a {@code Bindable}.
     */
    @Test
    void isBindable() {
        var attr = new SingularAttributeImpl<>(
                Long.class, "id", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, true, false, null);
        assertThat(attr).isInstanceOf(jakarta.persistence.metamodel.Bindable.class);
    }

    /**
     * Verify that {@code getBindableJavaType()} returns the attribute type.
     */
    @Test
    void getBindableJavaTypeReturnsAttributeType() {
        var attr = new SingularAttributeImpl<>(
                Long.class, "id", null,
                jakarta.persistence.metamodel.Attribute.PersistentAttributeType.BASIC, true, true, false, null);
        assertThat(attr.getBindableJavaType()).isEqualTo(Long.class);
    }
}
