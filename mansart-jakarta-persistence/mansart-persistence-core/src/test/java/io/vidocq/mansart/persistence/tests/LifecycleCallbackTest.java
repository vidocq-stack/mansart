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
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Lifecycle callbacks (M9-3).
 */
public class LifecycleCallbackTest extends BasePersistenceTest {

    /**
     * Entity with lifecycle callback annotations for testing.
     */
    @Entity
    public static class CallbackEntity {
        @Id
        private Long id;
        
        private String name;
        
        private static final List<String> callbackLog = new ArrayList<>();
        
        public static void clearCallbackLog() {
            callbackLog.clear();
        }
        
        public static List<String> getCallbackLog() {
            return new ArrayList<>(callbackLog);
        }
        
        @PrePersist
        public void prePersist() {
            callbackLog.add("prePersist");
        }
        
        @PostPersist
        public void postPersist() {
            callbackLog.add("postPersist");
        }
        
        @PostLoad
        public void postLoad() {
            callbackLog.add("postLoad");
        }
        
        @PreRemove
        public void preRemove() {
            callbackLog.add("preRemove");
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
    public void testPrePersistCallback() {
        CallbackEntity.clearCallbackLog();
        
        CallbackEntity entity = new CallbackEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        List<String> log = CallbackEntity.getCallbackLog();
        assertThat(log).contains("prePersist");
    }

    @Test
    public void testPostPersistCallback() {
        CallbackEntity.clearCallbackLog();
        
        CallbackEntity entity = new CallbackEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        List<String> log = CallbackEntity.getCallbackLog();
        assertThat(log).contains("postPersist");
    }

    @Test
    public void testCallbackOrderOnPersist() {
        CallbackEntity.clearCallbackLog();
        
        CallbackEntity entity = new CallbackEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        List<String> log = CallbackEntity.getCallbackLog();
        // prePersist should be called before postPersist
        int prePersistIndex = log.indexOf("prePersist");
        int postPersistIndex = log.indexOf("postPersist");
        
        assertThat(prePersistIndex).isGreaterThanOrEqualTo(0);
        assertThat(postPersistIndex).isGreaterThanOrEqualTo(0);
        assertThat(prePersistIndex).isLessThan(postPersistIndex);
    }

    @Test
    public void testPreRemoveCallback() {
        CallbackEntity.clearCallbackLog();
        
        CallbackEntity entity = new CallbackEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        CallbackEntity.clearCallbackLog();
        
        em.getTransaction().begin();
        em.remove(entity);
        em.getTransaction().commit();
        
        List<String> log = CallbackEntity.getCallbackLog();
        assertThat(log).contains("preRemove");
    }

    @Test
    public void testPostLoadCallback() {
        CallbackEntity.clearCallbackLog();
        
        CallbackEntity entity = new CallbackEntity();
        entity.setName("test");
        
        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();
        
        CallbackEntity.clearCallbackLog();
        
        // Find should trigger PostLoad
        em.getTransaction().begin();
        CallbackEntity found = em.find(CallbackEntity.class, entity.getId());
        em.getTransaction().commit();
        
        List<String> log = CallbackEntity.getCallbackLog();
        assertThat(log).contains("postLoad");
    }
}
