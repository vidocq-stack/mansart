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

    @Test
    public void testPersistAndFind() {
        TestEntity entity = new TestEntity("test", "value");
        
        em.persist(entity);
        assertThat(entity.getId()).isNotNull();
        
        // Find the persisted entity
        TestEntity found = em.find(TestEntity.class, entity.getId());
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(entity.getId());
        assertThat(found.getName()).isEqualTo(entity.getName());
        assertThat(found.getValue()).isEqualTo(entity.getValue());
        
        // Entity should be in the persistence context
        assertThat(em.contains(entity)).isTrue();
    }

    @Test
    public void testMerge() {
        TestEntity entity = new TestEntity("original", "value");
        em.persist(entity);
        Long originalId = entity.getId();
        
        // Create a new entity with same ID but different values
        TestEntity detachedEntity = new TestEntity("merged", "newValue");
        detachedEntity.setId(originalId);
        
        // Merge should update the persistent entity
        TestEntity merged = em.merge(detachedEntity);
        assertThat(merged.getId()).isEqualTo(originalId);
        
        // Find the entity to verify it was updated
        TestEntity found = em.find(TestEntity.class, originalId);
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("merged");
        assertThat(found.getValue()).isEqualTo("newValue");
    }

    @Test
    public void testRemove() {
        TestEntity entity = new TestEntity("toRemove", "value");
        em.persist(entity);
        Long id = entity.getId();
        
        // Verify entity exists
        TestEntity found = em.find(TestEntity.class, id);
        assertThat(found).isNotNull();
        assertThat(em.contains(entity)).isTrue();
        
        // Remove the entity
        em.remove(entity);
        
        // Verify entity no longer exists in persistence context
        TestEntity removed = em.find(TestEntity.class, id);
        assertThat(removed).isNull();
        assertThat(em.contains(entity)).isFalse();
    }

    @Test
    public void testDetach() {
        TestEntity entity = new TestEntity("detachTest", "value");
        em.persist(entity);
        Long id = entity.getId();
        
        // Verify entity is managed
        assertThat(em.contains(entity)).isTrue();
        
        // Detach the entity
        em.detach(entity);
        
        // Verify entity is no longer managed
        assertThat(em.contains(entity)).isFalse();
        
        // After detach, the entity should NOT be findable by ID in the same persistence context
        TestEntity found = em.find(TestEntity.class, id);
        assertThat(found).isNull();
    }
}
