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
 * Unit tests for {@link TypeImpl} — a marker type.
 */
class TypeImplTest {

    /**
     * Verify that {@code TypeImpl} is-a {@code Type}.
     */
    @Test
    void isType() {
        var type = new TypeImpl<String>(String.class, Type.PersistenceType.BASIC) { };
        assertThat(type).isInstanceOf(Type.class);
    }

    /**
     * Verify that {@code getJavaType()} returns the injected class.
     */
    @Test
    void getJavaTypeReturnsInjectedClass() {
        var type = new TypeImpl<Integer>(Integer.class, Type.PersistenceType.BASIC) { };
        assertThat(type.getJavaType()).isEqualTo(Integer.class);
    }

    /**
     * Verify that {@code getPersistenceType()} returns the injected type.
     */
    @Test
    void getPersistenceTypeReturnsInjectedType() {
        var type = new TypeImpl<String>(String.class, Type.PersistenceType.BASIC) { };
        assertThat(type.getPersistenceType()).isEqualTo(Type.PersistenceType.BASIC);
    }
}
