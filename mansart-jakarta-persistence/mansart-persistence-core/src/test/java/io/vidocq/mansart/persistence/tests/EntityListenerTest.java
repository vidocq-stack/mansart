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
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Entity Listeners (M9-4).
 */
public class EntityListenerTest extends BasePersistenceTest {

    /**
     * Entity Listener class for testing.
     */
    public static class AuditListener {
        private static final List<String> listenerLog = new ArrayList<>();
        
        public static void clearListenerLog() {
            listenerLog.clear();
        }
        
        public static List<String> getListenerLog() {
            return new ArrayList<>(listenerLog);
        }
        
        @PrePersist
        public void onPrePersist(Object entity) {
            listenerLog.add("listener:prePersist");
        }
        
        @PostPersist
        public void onPostPersist(Object entity) {
            listenerLog.add("listener:postPersist");
        }
        
        @PreRemove
        public void onPreRemove(Object entity) {
            listenerLog.add("listener:preRemove");
        }
        
        @PostLoad
        public void onPostLoad(Object entity) {
            listenerLog.add("listener:postLoad");
        }
    }

    /**
     * Entity with EntityListeners annotation.
     */
    @Entity
    @EntityListeners(AuditListener.class)
    public static class AuditedEntity {
        @Id
        private Long id;
        
        private String name;
        
        private static final List<String> entityLog = new ArrayList<>();
        
        public static void clearEntityLog() {
            entityLog.clear();
        }
        
        public static List<String> getEntityLog() {
            return new ArrayList<>(entityLog);
        }
        
        @PrePersist
        public void onEntityPrePersist() {
            entityLog.add("entity:prePersist");
        }
        
        @PostPersist
        public void onEntityPostPersist() {
            entityLog.add("entity:postPersist");
        }
        
        // Getters and setters
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
    public void testEntityListenerPrePersistCallback() {
        AuditListener.clearListenerLog();
        
        AuditedEntity entity = new AuditedEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        List<String> log = AuditListener.getListenerLog();
        assertThat(log).contains("listener:prePersist");
    }

    @Test
    public void testEntityListenerPostPersistCallback() {
        AuditListener.clearListenerLog();
        
        AuditedEntity entity = new AuditedEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        List<String> log = AuditListener.getListenerLog();
        assertThat(log).contains("listener:postPersist");
    }

    @Test
    public void testEntityAndListenerCallbacksBothCalled() {
        AuditListener.clearListenerLog();
        AuditedEntity.clearEntityLog();
        
        AuditedEntity entity = new AuditedEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        List<String> entityLog = AuditedEntity.getEntityLog();
        List<String> listenerLog = AuditListener.getListenerLog();
        
        // Both entity callbacks and listener callbacks should be invoked
        assertThat(entityLog).contains("entity:prePersist", "entity:postPersist");
        assertThat(listenerLog).contains("listener:prePersist", "listener:postPersist");
    }

    @Test
    public void testEntityListenerPostLoadCallback() {
        AuditListener.clearListenerLog();
        
        AuditedEntity entity = new AuditedEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        AuditListener.clearListenerLog();
        
        em.getTransaction().begin();
        AuditedEntity found = em.find(AuditedEntity.class, entity.getId());
        em.getTransaction().commit();
        
        List<String> log = AuditListener.getListenerLog();
        assertThat(log).contains("listener:postLoad");
    }

    @Test
    public void testEntityListenerPreRemoveCallback() {
        AuditListener.clearListenerLog();
        
        AuditedEntity entity = new AuditedEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        AuditListener.clearListenerLog();
        
        em.getTransaction().begin();
        em.remove(entity);
        em.getTransaction().commit();
        
        List<String> log = AuditListener.getListenerLog();
        assertThat(log).contains("listener:preRemove");
    }
}
