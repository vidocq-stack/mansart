/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for M4-JP-26: persistence context entity state machine
 * (NEW, MANAGED, DETACHED, REMOVED) and first-level identity map.
 *
 * <p>Tests go through the EntityManager (the exported API) since the
 * persistence context package is internal. State transitions are
 * verified by observable behaviour: {@code contains}, {@code find},
 * and the exceptions thrown by illegal transitions.
 */
public class MansartPersistenceContextTest {

    private static EntityManager openEm() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        return emf.createEntityManager();
    }

    private static SimpleEntity newEntity(Long id, String name) {
        SimpleEntity e = new SimpleEntity();
        e.setId(id);
        e.setName(name);
        return e;
    }

    // ── persist: NEW → MANAGED ───────────────────────────────────────────

    @Test
    public void persistNewBecomesManagedAndContains() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(1L, "alpha");
        em.persist(e);
        assertThat(em.contains(e)).isTrue();
    }

    @Test
    public void persistManagedIsNoop() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(2L, "beta");
        em.persist(e);
        em.persist(e);
        assertThat(em.contains(e)).isTrue();
    }

    @Test
    public void persistRemovedBecomesManaged() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(3L, "gamma");
        em.persist(e);
        em.remove(e);
        assertThat(em.contains(e)).isTrue();
        em.persist(e);
        assertThat(em.contains(e)).isTrue();
    }

    @Test
    public void persistNullThrowsIllegalArgumentException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.persist(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── remove: MANAGED → REMOVED ───────────────────────────────────────

    @Test
    public void removeManagedBecomesRemoved() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(4L, "delta");
        em.persist(e);
        em.remove(e);
        assertThat(em.contains(e)).isTrue();
    }

    @Test
    public void removeNewThrowsIllegalArgumentException() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(5L, "epsilon");
        assertThatThrownBy(() -> em.remove(e))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void removeDetachedThrowsIllegalArgumentException() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(6L, "zeta");
        em.persist(e);
        em.detach(e);
        assertThatThrownBy(() -> em.remove(e))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void removeRemovedIsNoop() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(7L, "eta");
        em.persist(e);
        em.remove(e);
        em.remove(e);
        assertThat(em.contains(e)).isTrue();
    }

    // ── detach: MANAGED → DETACHED ──────────────────────────────────────

    @Test
    public void detachManagedBecomesDetached() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(8L, "theta");
        em.persist(e);
        em.detach(e);
        assertThat(em.contains(e)).isFalse();
    }

    @Test
    public void detachDetachedIsNoop() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(9L, "iota");
        em.persist(e);
        em.detach(e);
        em.detach(e);
        assertThat(em.contains(e)).isFalse();
    }

    // ── clear: all → DETACHED ───────────────────────────────────────────

    @Test
    public void clearMakesAllDetached() {
        EntityManager em = openEm();
        SimpleEntity e1 = newEntity(10L, "kappa");
        SimpleEntity e2 = newEntity(11L, "lambda");
        em.persist(e1);
        em.persist(e2);
        em.clear();
        assertThat(em.contains(e1)).isFalse();
        assertThat(em.contains(e2)).isFalse();
    }

    // ── contains ────────────────────────────────────────────────────────

    @Test
    public void containsReturnsTrueForManagedAndRemoved() {
        EntityManager em = openEm();
        SimpleEntity managed = newEntity(12L, "mu");
        em.persist(managed);
        assertThat(em.contains(managed)).isTrue();

        SimpleEntity removed = newEntity(13L, "nu");
        em.persist(removed);
        em.remove(removed);
        assertThat(em.contains(removed)).isTrue();
    }

    @Test
    public void containsReturnsFalseForDetachedAndNew() {
        EntityManager em = openEm();
        SimpleEntity detached = newEntity(14L, "xi");
        em.persist(detached);
        em.detach(detached);
        assertThat(em.contains(detached)).isFalse();

        SimpleEntity newEntity = newEntity(15L, "omicron");
        assertThat(em.contains(newEntity)).isFalse();
    }

    // ── merge ───────────────────────────────────────────────────────────

    @Test
    public void mergeReturnsManagedCopy() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(16L, "pi");
        SimpleEntity merged = em.merge(e);
        assertThat(merged).isNotNull();
        assertThat(em.contains(merged)).isTrue();
    }

    @Test
    public void mergeManagedReturnsSameInstance() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(17L, "rho");
        em.persist(e);
        SimpleEntity merged = em.merge(e);
        assertThat(merged).isSameAs(e);
    }

    // ── find: identity map lookup ───────────────────────────────────────

    @Test
    public void findReturnsCachedInstance() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(18L, "sigma");
        em.persist(e);
        SimpleEntity found = em.find(SimpleEntity.class, 18L);
        assertThat(found).isSameAs(e);
    }

    @Test
    public void findReturnsNullWhenNotInContext() {
        EntityManager em = openEm();
        SimpleEntity found = em.find(SimpleEntity.class, 999L);
        assertThat(found).isNull();
    }

    // ── flush: no-op (no database) ──────────────────────────────────────

    @Test
    public void flushIsNoop() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(19L, "tau");
        em.persist(e);
        em.flush();
        assertThat(em.contains(e)).isTrue();
    }
}
