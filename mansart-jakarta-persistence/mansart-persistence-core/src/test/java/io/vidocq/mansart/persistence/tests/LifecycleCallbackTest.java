/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.core.testentities.callback.CallbackEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Lifecycle callbacks (M9-3).
 */
public class LifecycleCallbackTest extends BasePersistenceTest {

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
