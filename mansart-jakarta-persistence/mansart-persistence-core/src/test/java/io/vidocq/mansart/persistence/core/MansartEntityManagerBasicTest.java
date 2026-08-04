/*
 * Copyright (c) ${year} Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
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
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

package io.vidocq.mansart.persistence.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic tests for MansartEntityManager that don't require mocking.
 * Tests EntityCacheKey functionality.
 * M4 — Basic EntityManager tests.
 */
class MansartEntityManagerBasicTest {

    /* ==================== Entity Cache Key Tests ==================== */

    @Test
    void testEntityCacheKeyEqualsAndHashCode() {
        MansartEntityManager.EntityCacheKey key1 = 
            new MansartEntityManager.EntityCacheKey(TestEntity.class, 1L);
        MansartEntityManager.EntityCacheKey key2 = 
            new MansartEntityManager.EntityCacheKey(TestEntity.class, 1L);
        MansartEntityManager.EntityCacheKey key3 = 
            new MansartEntityManager.EntityCacheKey(TestEntity.class, 2L);
        MansartEntityManager.EntityCacheKey key4 = 
            new MansartEntityManager.EntityCacheKey(String.class, 1L);

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
        assertNotEquals(key1, key3);
        assertNotEquals(key1, key4);
    }

    @Test
    void testEntityCacheKeyWithNullId() {
        MansartEntityManager.EntityCacheKey key1 = 
            new MansartEntityManager.EntityCacheKey(TestEntity.class, null);
        MansartEntityManager.EntityCacheKey key2 = 
            new MansartEntityManager.EntityCacheKey(TestEntity.class, null);
        
        assertEquals(key1, key2);
    }

    @Test
    void testEntityCacheKeyDifferentTypes() {
        MansartEntityManager.EntityCacheKey key1 = 
            new MansartEntityManager.EntityCacheKey(String.class, "id");
        MansartEntityManager.EntityCacheKey key2 = 
            new MansartEntityManager.EntityCacheKey(Integer.class, "id");
        
        assertNotEquals(key1, key2);
    }

    @Test
    void testEntityCacheKeyToString() {
        MansartEntityManager.EntityCacheKey key = 
            new MansartEntityManager.EntityCacheKey(TestEntity.class, 1L);
        
        String toString = key.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("EntityCacheKey"));
    }
}
