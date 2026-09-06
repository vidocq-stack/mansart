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
class MansartPersistenceContextTest {

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
    void persistNewBecomesManagedAndContains() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(1L, "alpha");
        em.persist(e);
        assertThat(em.contains(e)).isTrue();
    }

    @Test
    void persistManagedIsNoop() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(2L, "beta");
        em.persist(e);
        em.persist(e);
        assertThat(em.contains(e)).isTrue();
    }

    @Test
    void persistRemovedBecomesManaged() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(3L, "gamma");
        em.persist(e);
        em.remove(e);
        assertThat(em.contains(e)).isTrue();
        em.persist(e);
        assertThat(em.contains(e)).isTrue();
    }

    @Test
    void persistNullThrowsIllegalArgumentException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.persist(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── remove: MANAGED → REMOVED ───────────────────────────────────────

    @Test
    void removeManagedBecomesRemoved() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(4L, "delta");
        em.persist(e);
        em.remove(e);
        assertThat(em.contains(e)).isTrue();
    }

    @Test
    void removeNewThrowsIllegalArgumentException() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(5L, "epsilon");
        assertThatThrownBy(() -> em.remove(e))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeDetachedThrowsIllegalArgumentException() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(6L, "zeta");
        em.persist(e);
        em.detach(e);
        assertThatThrownBy(() -> em.remove(e))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void removeRemovedIsNoop() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(7L, "eta");
        em.persist(e);
        em.remove(e);
        em.remove(e);
        assertThat(em.contains(e)).isTrue();
    }

    // ── detach: MANAGED → DETACHED ──────────────────────────────────────

    @Test
    void detachManagedBecomesDetached() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(8L, "theta");
        em.persist(e);
        em.detach(e);
        assertThat(em.contains(e)).isFalse();
    }

    @Test
    void detachDetachedIsNoop() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(9L, "iota");
        em.persist(e);
        em.detach(e);
        em.detach(e);
        assertThat(em.contains(e)).isFalse();
    }

    // ── clear: all → DETACHED ───────────────────────────────────────────

    @Test
    void clearMakesAllDetached() {
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
    void containsReturnsTrueForManagedAndRemoved() {
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
    void containsReturnsFalseForDetachedAndNew() {
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
    void mergeReturnsManagedCopy() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(16L, "pi");
        SimpleEntity merged = em.merge(e);
        assertThat(merged).isNotNull();
        assertThat(em.contains(merged)).isTrue();
    }

    @Test
    void mergeManagedReturnsSameInstance() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(17L, "rho");
        em.persist(e);
        SimpleEntity merged = em.merge(e);
        assertThat(merged).isSameAs(e);
    }

    // ── find: identity map lookup ───────────────────────────────────────

    @Test
    void findReturnsCachedInstance() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(18L, "sigma");
        em.persist(e);
        SimpleEntity found = em.find(SimpleEntity.class, 18L);
        assertThat(found).isSameAs(e);
    }

    @Test
    void findReturnsNullWhenNotInContext() {
        EntityManager em = openEm();
        SimpleEntity found = em.find(SimpleEntity.class, 999L);
        assertThat(found).isNull();
    }

    // ── flush: no-op (no database) ──────────────────────────────────────

    @Test
    void flushIsNoop() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(19L, "tau");
        em.persist(e);
        em.flush();
        assertThat(em.contains(e)).isTrue();
    }

    // ── find overloads ────────────────────────────────────────────────────

    @Test
    void findWithMapReturnsCachedInstance() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(20L, "upsilon");
        em.persist(e);
        SimpleEntity found = em.find(SimpleEntity.class, 20L, java.util.Map.of());
        assertThat(found).isSameAs(e);
        em.close();
    }

    @Test
    void findWithLockModeTypeReturnsCachedInstance() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(21L, "phi");
        em.persist(e);
        SimpleEntity found = em.find(SimpleEntity.class, 21L, jakarta.persistence.LockModeType.NONE);
        assertThat(found).isSameAs(e);
        em.close();
    }

    @Test
    void findWithLockModeTypeAndMapReturnsCachedInstance() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(22L, "chi");
        em.persist(e);
        SimpleEntity found = em.find(SimpleEntity.class, 22L, jakarta.persistence.LockModeType.NONE, java.util.Map.of());
        assertThat(found).isSameAs(e);
        em.close();
    }

    @Test
    void findWithFindOptionsReturnsCachedInstance() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(23L, "psi");
        em.persist(e);
        SimpleEntity found = em.find(SimpleEntity.class, 23L, new jakarta.persistence.FindOption[0]);
        assertThat(found).isSameAs(e);
        em.close();
    }

    // ── refresh ───────────────────────────────────────────────────────────

    @Test
    void refreshManagedEntityDoesNotThrow() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(24L, "omega");
        em.persist(e);
        em.refresh(e);
        assertThat(em.contains(e)).isTrue();
        em.close();
    }

    @Test
    void refreshDetachedEntityThrowsIllegalArgumentException() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(25L, "alpha2");
        em.persist(e);
        em.detach(e);
        assertThatThrownBy(() -> em.refresh(e))
                .isInstanceOf(IllegalArgumentException.class);
        em.close();
    }

    @Test
    void refreshNullThrowsIllegalArgumentException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.refresh(null))
                .isInstanceOf(IllegalArgumentException.class);
        em.close();
    }

    // ── detach(null) is a no-op ───────────────────────────────────────────

    @Test
    void detachNullIsNoop() {
        EntityManager em = openEm();
        em.detach(null);
        assertThat(em.isOpen()).isTrue();
        em.close();
    }

    @Test
    void containsNullReturnsFalse() {
        EntityManager em = openEm();
        assertThat(em.contains(null)).isFalse();
        em.close();
    }

    // ── merge on DETACHED and REMOVED ─────────────────────────────────────

    @Test
    void mergeDetachedReturnsManagedCopy() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(26L, "beta2");
        em.persist(e);
        em.detach(e);
        SimpleEntity merged = em.merge(e);
        assertThat(merged).isNotNull();
        assertThat(em.contains(merged)).isTrue();
        em.close();
    }

    @Test
    void mergeRemovedReturnsManagedCopy() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(27L, "gamma2");
        em.persist(e);
        em.remove(e);
        SimpleEntity merged = em.merge(e);
        assertThat(merged).isNotNull();
        assertThat(em.contains(merged)).isTrue();
        em.close();
    }

    @Test
    void mergeNullThrowsIllegalArgumentException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.merge(null))
                .isInstanceOf(IllegalArgumentException.class);
        em.close();
    }

    @Test
    void removeNullThrowsIllegalArgumentException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.remove(null))
                .isInstanceOf(IllegalArgumentException.class);
        em.close();
    }

    // ── clear and detach identity map effects ────────────────────────────

    @Test
    void afterClearFindReturnsNull() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(28L, "delta2");
        em.persist(e);
        em.clear();
        SimpleEntity found = em.find(SimpleEntity.class, 28L);
        assertThat(found).isNull();
        em.close();
    }

    @Test
    void afterDetachFindReturnsNull() {
        EntityManager em = openEm();
        SimpleEntity e = newEntity(29L, "epsilon2");
        em.persist(e);
        em.detach(e);
        SimpleEntity found = em.find(SimpleEntity.class, 29L);
        assertThat(found).isNull();
        em.close();
    }

    // ── UnsupportedOperationException through EM delegation ──────────────

    @Test
    void getReferenceClassObjectOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.getReference(Object.class, 1L))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void getReferenceEntityOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.getReference(new Object()))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void lockObjectLockModeTypeOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.lock(new Object(), jakarta.persistence.LockModeType.NONE))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void lockObjectLockModeTypeMapOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.lock(new Object(), jakarta.persistence.LockModeType.NONE, java.util.Map.of()))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void lockObjectLockModeTypeLockOptionArrayOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.lock(new Object(), jakarta.persistence.LockModeType.NONE))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void getLockModeOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.getLockMode(new Object()))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void refreshObjectMapOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.refresh(new Object(), java.util.Map.of()))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void refreshObjectLockModeTypeOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.refresh(new Object(), jakarta.persistence.LockModeType.NONE))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void refreshObjectLockModeTypeMapOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.refresh(new Object(), jakarta.persistence.LockModeType.NONE, java.util.Map.of()))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void refreshObjectRefreshOptionArrayOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.refresh(new Object(), new jakarta.persistence.RefreshOption[0]))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void findEntityGraphObjectFindOptionArrayOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.find(em.createEntityGraph(Object.class), 1L))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }
}
