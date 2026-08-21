/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.core.testentities.common.PropertyAccessEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-access mapping (annotations on getters, TCK style): the APT must scan
 * getters when {@code @Id} sits on one, and the generated accessors must go through
 * getter/setter MethodHandles instead of field handles.
 */
class PropertyAccessTest extends BasePersistenceTest {

    @Test
    void persistAndFindPropertyAccessEntity() {
        PropertyAccessEntity entity = new PropertyAccessEntity();
        entity.setId(7L);
        entity.setLabel("by-property");
        entity.setActive(true);

        em.getTransaction().begin();
        em.persist(entity);
        em.getTransaction().commit();

        em.close();
        em = emf.createEntityManager();

        PropertyAccessEntity found = em.find(PropertyAccessEntity.class, 7L);
        assertThat(found).isNotNull();
        assertThat(found.getLabel()).isEqualTo("by-property");
        assertThat(found.isActive()).isTrue();
    }
}
