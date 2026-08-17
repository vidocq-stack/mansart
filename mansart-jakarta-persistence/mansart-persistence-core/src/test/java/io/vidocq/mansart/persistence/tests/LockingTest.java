/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.core.testentities.common.SimpleEntity;
import io.vidocq.mansart.persistence.core.testentities.common.VersionedEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PessimisticLockScope;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Locking (M9-5) - Optimistic and Pessimistic.
 */
public class LockingTest extends BasePersistenceTest {

    @Test
    public void testLockWithOptimisticLockMode() {
        VersionedEntity entity = new VersionedEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        em.getTransaction().begin();
        VersionedEntity found = em.find(VersionedEntity.class, entity.getId(), LockModeType.OPTIMISTIC);
        em.getTransaction().commit();
        
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("test");
    }

    @Test
    public void testLockWithOptimisticForceIncrementLockMode() {
        VersionedEntity entity = new VersionedEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        em.getTransaction().begin();
        VersionedEntity found = em.find(VersionedEntity.class, entity.getId(), LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        em.getTransaction().commit();
        
        assertThat(found).isNotNull();
    }

    @Test
    public void testLockWithPessimisticReadLockMode() {
        SimpleEntity entity = new SimpleEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        em.getTransaction().begin();
        SimpleEntity found = em.find(SimpleEntity.class, entity.getId(), LockModeType.PESSIMISTIC_READ);
        em.getTransaction().commit();
        
        assertThat(found).isNotNull();
    }

    @Test
    public void testLockWithPessimisticWriteLockMode() {
        SimpleEntity entity = new SimpleEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        em.getTransaction().begin();
        SimpleEntity found = em.find(SimpleEntity.class, entity.getId(), LockModeType.PESSIMISTIC_WRITE);
        em.getTransaction().commit();
        
        assertThat(found).isNotNull();
    }

    @Test
    public void testLockWithPessimisticForceIncrementLockMode() {
        VersionedEntity entity = new VersionedEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        em.getTransaction().begin();
        VersionedEntity found = em.find(VersionedEntity.class, entity.getId(), LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        em.getTransaction().commit();
        
        assertThat(found).isNotNull();
    }

    @Test
    public void testLockMethodWithoutOptions() {
        SimpleEntity entity = new SimpleEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        em.getTransaction().begin();
        em.lock(entity, LockModeType.PESSIMISTIC_READ);
        em.getTransaction().commit();
        
        assertThat(entity.getName()).isEqualTo("test");
    }

    @Test
    public void testLockMethodWithTypeAndProperties() {
        SimpleEntity entity = new SimpleEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.lock.scope", PessimisticLockScope.NORMAL);
        
        em.getTransaction().begin();
        em.lock(entity, LockModeType.PESSIMISTIC_READ, properties);
        em.getTransaction().commit();
        
        assertThat(entity.getName()).isEqualTo("test");
    }

    @Test
    public void testLockWithRefresh() {
        VersionedEntity entity = new VersionedEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        em.getTransaction().begin();
        em.lock(entity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        em.refresh(entity);
        em.getTransaction().commit();
        
        assertThat(entity.getName()).isEqualTo("test");
    }
}
