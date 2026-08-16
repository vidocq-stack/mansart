/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Version;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Locking (M9-5) - Optimistic and Pessimistic.
 */
public class LockingTest extends BasePersistenceTest {

    /**
     * Entity with optimistic locking using @Version.
     */
    @Entity
    public static class VersionedEntity {
        @Id
        private Long id;
        
        private String name;
        
        @Version
        private Long version;
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public Long getVersion() {
            return version;
        }
        
        public void setVersion(Long version) {
            this.version = version;
        }
    }

    /**
     * Simple entity without version field for pessimistic locking tests.
     */
    @Entity
    public static class SimpleEntity {
        @Id
        private Long id;
        
        private String name;
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    public void testLockWithOptimisticLockMode() {
        VersionedEntity entity = new VersionedEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        // Lock with OPTIMISTIC should not throw
        em.getTransaction().begin();
        VersionedEntity found = em.find(VersionedEntity.class, entity.getId(), LockModeType.OPTIMISTIC);
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("test");
        em.getTransaction().commit();
    }

    @Test
    public void testLockWithOptimisticForceIncrementLockMode() {
        VersionedEntity entity = new VersionedEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        // Lock with OPTIMISTIC_FORCE_INCREMENT should not throw
        em.getTransaction().begin();
        VersionedEntity found = em.find(VersionedEntity.class, entity.getId(), LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        assertThat(found).isNotNull();
        em.getTransaction().commit();
    }

    @Test
    public void testLockWithPessimisticReadLockMode() {
        SimpleEntity entity = new SimpleEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        // Lock with PESSIMISTIC_READ should not throw (may be no-op in H2)
        em.getTransaction().begin();
        SimpleEntity found = em.find(SimpleEntity.class, entity.getId(), LockModeType.PESSIMISTIC_READ);
        assertThat(found).isNotNull();
        em.getTransaction().commit();
    }

    @Test
    public void testLockWithPessimisticWriteLockMode() {
        SimpleEntity entity = new SimpleEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        // Lock with PESSIMISTIC_WRITE should not throw (may be no-op in H2)
        em.getTransaction().begin();
        SimpleEntity found = em.find(SimpleEntity.class, entity.getId(), LockModeType.PESSIMISTIC_WRITE);
        assertThat(found).isNotNull();
        em.getTransaction().commit();
    }

    @Test
    public void testLockWithPessimisticForceIncrementLockMode() {
        VersionedEntity entity = new VersionedEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        // Lock with PESSIMISTIC_FORCE_INCREMENT should not throw
        em.getTransaction().begin();
        VersionedEntity found = em.find(VersionedEntity.class, entity.getId(), LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        assertThat(found).isNotNull();
        em.getTransaction().commit();
    }

    @Test
    public void testLockMethodWithTypeAndProperties() {
        SimpleEntity entity = new SimpleEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        // Lock with type and properties
        em.getTransaction().begin();
        em.lock(entity, LockModeType.OPTIMISTIC, java.util.Collections.emptyMap());
        em.getTransaction().commit();
    }

    @Test
    public void testLockMethodWithoutOptions() {
        SimpleEntity entity = new SimpleEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        // Lock with just entity and lock mode
        em.getTransaction().begin();
        em.lock(entity, LockModeType.OPTIMISTIC);
        em.getTransaction().commit();
    }

    @Test
    public void testLockWithRefresh() {
        VersionedEntity entity = new VersionedEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        // Lock and refresh
        em.getTransaction().begin();
        em.lock(entity, LockModeType.OPTIMISTIC);
        em.refresh(entity);
        assertThat(entity.getName()).isEqualTo("test");
        em.getTransaction().commit();
    }
}
