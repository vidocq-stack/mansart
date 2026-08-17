/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.core.testentities.listener.AuditListener;
import io.vidocq.mansart.persistence.core.testentities.listener.AuditedEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Entity Listeners (M9-4).
 */
public class EntityListenerTest extends BasePersistenceTest {

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
