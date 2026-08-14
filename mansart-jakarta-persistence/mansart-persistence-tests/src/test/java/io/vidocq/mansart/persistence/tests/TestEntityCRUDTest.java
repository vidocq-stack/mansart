/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * or any later version, which is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * It is also made available under the European Union Public Licence v. 1.2,
 * which is available at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for basic CRUD operations on entities.
 */
public class TestEntityCRUDTest extends BasePersistenceTest {

    @Test
    public void testEntityEqualsAndHashCode() {
        TestEntity entity1 = new TestEntity("name", "value1");
        TestEntity entity2 = new TestEntity("name", "value2");

        // Before persist, both have null IDs
        assertThat(entity1).isNotEqualTo(entity2);

        // After setting same ID
        entity1.setId(1L);
        entity2.setId(1L);
        assertThat(entity1).isEqualTo(entity2);
        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());

        // Different IDs
        entity2.setId(2L);
        assertThat(entity1).isNotEqualTo(entity2);
    }
}
