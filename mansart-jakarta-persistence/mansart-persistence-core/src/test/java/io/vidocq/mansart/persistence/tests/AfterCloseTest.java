/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.core.testentities.common.SimpleEntity;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Jakarta Persistence 3.2 §3.1.1: after {@code EntityManager.close()}, every method
 * of the EntityManager — and of any Query obtained from it — must throw
 * {@code IllegalStateException} (except {@code isOpen} and {@code getTransaction}).
 */
class AfterCloseTest extends BasePersistenceTest {

    @Test
    void entityManagerMethodsThrowAfterClose() {
        em.close();

        assertThatIllegalStateException().isThrownBy(() -> em.persist(new SimpleEntity()));
        assertThatIllegalStateException().isThrownBy(() -> em.find(SimpleEntity.class, 1L));
        assertThatIllegalStateException().isThrownBy(() -> em.merge(new SimpleEntity()));
        assertThatIllegalStateException().isThrownBy(() -> em.remove(new SimpleEntity()));
        assertThatIllegalStateException().isThrownBy(() -> em.flush());
        assertThatIllegalStateException().isThrownBy(() -> em.clear());
        assertThatIllegalStateException().isThrownBy(() -> em.contains(new SimpleEntity()));
        assertThatIllegalStateException().isThrownBy(() -> em.createQuery("SELECT s FROM SimpleEntity s"));
        assertThatIllegalStateException().isThrownBy(() -> em.createNativeQuery("SELECT 1"));
        assertThatIllegalStateException().isThrownBy(() -> em.getCriteriaBuilder());
        assertThatIllegalStateException().isThrownBy(() -> em.getMetamodel());
        assertThatIllegalStateException().isThrownBy(() -> em.getDelegate());
        assertThatIllegalStateException().isThrownBy(() -> em.close());
    }

    @Test
    void queryMethodsThrowAfterEntityManagerClose() {
        Query query = em.createQuery("SELECT s FROM SimpleEntity s");
        TypedQuery<SimpleEntity> typedQuery =
                em.createQuery("SELECT s FROM SimpleEntity s", SimpleEntity.class);
        Query nativeQuery = em.createNativeQuery("SELECT 1");

        em.close();

        assertThatIllegalStateException().isThrownBy(query::getResultList);
        assertThatIllegalStateException().isThrownBy(() -> query.setMaxResults(1));
        assertThatIllegalStateException().isThrownBy(typedQuery::getResultList);
        assertThatIllegalStateException().isThrownBy(() -> typedQuery.setFirstResult(1));
        assertThatIllegalStateException().isThrownBy(nativeQuery::getResultList);
        assertThatIllegalStateException().isThrownBy(nativeQuery::executeUpdate);
    }
}
