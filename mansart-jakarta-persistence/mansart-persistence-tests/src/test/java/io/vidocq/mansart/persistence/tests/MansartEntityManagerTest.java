/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.core.MansartEntityManager;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for M4-JP-25: MansartEntityManager implements the EntityManager
 * interface and delegates entity-state operations to the persistence context.
 *
 * <p>Covers the closed-state contract (per the {@code EntityManager.close()}
 * Javadoc: after close every method throws IllegalStateException except
 * {@code isOpen()}, {@code getProperties()} and {@code getTransaction()}),
 * configuration storage (flush/cache modes, property overrides), unwrap,
 * getDelegate, and the delegation seam to the persistence context.
 */
class MansartEntityManagerTest {

    private static EntityManager openEm() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        return emf.createEntityManager();
    }

    // ── Closed-state contract: blanket IllegalStateException ─────────────

    @Test
    void persistOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        Object entity = new Object();
        assertThatThrownBy(() -> em.persist(entity))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void findOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.find(Object.class, 1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createQueryOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.createQuery("select e from E e"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getEntityManagerFactoryOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::getEntityManagerFactory)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void setFlushModeOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.setFlushMode(FlushModeType.COMMIT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getFlushModeOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::getFlushMode)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unwrapOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.unwrap(MansartEntityManager.class))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getDelegateOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::getDelegate)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void setCacheRetrieveModeOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.setCacheRetrieveMode(CacheRetrieveMode.BYPASS))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── Closed-state contract: exemptions ───────────────────────────────

    @Test
    void isOpenOnClosedEmReturnsFalse() {
        EntityManager em = openEm();
        em.close();
        assertThat(em.isOpen()).isFalse();
    }

    @Test
    void getPropertiesOnClosedEmDoesNotThrow() {
        EntityManager em = openEm();
        em.close();
        // Spec exemption: getProperties() is permitted on a closed EM
        Map<String, Object> props = em.getProperties();
        assertThat(props).isNotNull();
    }

    // ── Configuration storage: flush mode ───────────────────────────────

    @Test
    void getFlushModeDefaultsToAuto() {
        EntityManager em = openEm();
        assertThat(em.getFlushMode()).isEqualTo(FlushModeType.AUTO);
        em.close();
    }

    @Test
    void setFlushModeIsRetained() {
        EntityManager em = openEm();
        em.setFlushMode(FlushModeType.COMMIT);
        assertThat(em.getFlushMode()).isEqualTo(FlushModeType.COMMIT);
        em.close();
    }

    // ── Configuration storage: cache modes ──────────────────────────────

    @Test
    void getCacheRetrieveModeDefaultsToUse() {
        EntityManager em = openEm();
        assertThat(em.getCacheRetrieveMode()).isEqualTo(CacheRetrieveMode.USE);
        em.close();
    }

    @Test
    void setCacheRetrieveModeIsRetained() {
        EntityManager em = openEm();
        em.setCacheRetrieveMode(CacheRetrieveMode.BYPASS);
        assertThat(em.getCacheRetrieveMode()).isEqualTo(CacheRetrieveMode.BYPASS);
        em.close();
    }

    @Test
    void getCacheStoreModeDefaultsToUse() {
        EntityManager em = openEm();
        assertThat(em.getCacheStoreMode()).isEqualTo(CacheStoreMode.USE);
        em.close();
    }

    @Test
    void setCacheStoreModeIsRetained() {
        EntityManager em = openEm();
        em.setCacheStoreMode(CacheStoreMode.BYPASS);
        assertThat(em.getCacheStoreMode()).isEqualTo(CacheStoreMode.BYPASS);
        em.close();
    }

    // ── Configuration storage: properties ───────────────────────────────

    @Test
    void getPropertiesIncludesFactoryProperties() {
        EntityManager em = openEm();
        assertThat(em.getProperties().get("io.vidocq.mansart.test.marker"))
                .isEqualTo("from-xml");
        em.close();
    }

    @Test
    void setPropertyIsVisibleInGetProperties() {
        EntityManager em = openEm();
        em.setProperty("custom.key", "custom.value");
        assertThat(em.getProperties().get("custom.key")).isEqualTo("custom.value");
        em.close();
    }

    @Test
    void setPropertyOverridesFactoryProperty() {
        EntityManager em = openEm();
        em.setProperty("io.vidocq.mansart.test.marker", "overridden");
        assertThat(em.getProperties().get("io.vidocq.mansart.test.marker"))
                .isEqualTo("overridden");
        em.close();
    }

    // ── unwrap / getDelegate / getEntityManagerFactory ───────────────────

    @Test
    void unwrapToMansartEntityManagerReturnsSameInstance() {
        EntityManager em = openEm();
        MansartEntityManager unwrapped = em.unwrap(MansartEntityManager.class);
        assertThat(unwrapped).isSameAs(em);
        em.close();
    }

    @Test
    void unwrapToEntityManagerReturnsSameInstance() {
        EntityManager em = openEm();
        EntityManager unwrapped = em.unwrap(EntityManager.class);
        assertThat(unwrapped).isSameAs(em);
        em.close();
    }

    @Test
    void unwrapToUnsupportedTypeThrowsPersistenceException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.unwrap(String.class))
                .isInstanceOf(PersistenceException.class);
        em.close();
    }

    @Test
    void getDelegateReturnsSameInstance() {
        EntityManager em = openEm();
        assertThat(em.getDelegate()).isSameAs(em);
        em.close();
    }

    @Test
    void getEntityManagerFactoryReturnsCreatingFactory() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        EntityManager em = emf.createEntityManager();
        assertThat(em.getEntityManagerFactory()).isSameAs(emf);
        em.close();
        emf.close();
    }

    // ── Delegation seam to persistence context ───────────────────────────

    @Test
    void persistOnOpenEmDelegatesToPersistenceContext() {
        // The persistence context state machine is M4-JP-26; the EM now delegates
        // to the persistence context which validates entities and throws
        // IllegalArgumentException for non-@Entity instances.
        EntityManager em = openEm();
        Object entity = new Object();
        assertThatThrownBy(() -> em.persist(entity))
                .isInstanceOf(IllegalArgumentException.class);
        em.close();
    }

    @Test
    void containsOnOpenEmDelegatesToPersistenceContext() {
        // The persistence context state machine is M4-JP-26; the EM now delegates
        // to the persistence context which returns false for non-managed entities.
        EntityManager em = openEm();
        assertThat(em.contains(new Object())).isFalse();
        em.close();
    }

    @Test
    void closeIsIdempotentThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::close).isInstanceOf(IllegalStateException.class);
    }

    // ── UnsupportedOperationException on open EM ────────────────────────────

    @Test
    void createQueryStringOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createQuery("select e from E e"))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void createQueryCriteriaQueryOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createQuery((jakarta.persistence.criteria.CriteriaQuery<?>) null))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void createQueryCriteriaSelectOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createQuery((jakarta.persistence.criteria.CriteriaSelect<?>) null))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void createQueryCriteriaUpdateOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createQuery((jakarta.persistence.criteria.CriteriaUpdate<?>) null))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void createQueryCriteriaDeleteOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createQuery((jakarta.persistence.criteria.CriteriaDelete<?>) null))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void createQueryStringClassOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createQuery("select e from E e", Object.class))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void createNamedQueryStringOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createNamedQuery("someNamedQuery"))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void createNamedQueryStringClassOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createNamedQuery("someNamedQuery", Object.class))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void createQueryTypedQueryReferenceOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createQuery((jakarta.persistence.TypedQueryReference<?>) null))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void createNativeQueryStringOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createNativeQuery("select 1"))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void createNativeQueryStringClassOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createNativeQuery("select 1", Object.class))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void createNativeQueryStringStringOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createNativeQuery("select 1", "alias"))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void createNamedStoredProcedureQueryStringOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createNamedStoredProcedureQuery("proc"))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void createStoredProcedureQueryStringOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createStoredProcedureQuery("proc"))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void createStoredProcedureQueryStringClassArrayOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createStoredProcedureQuery("proc", Object.class))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void createStoredProcedureQueryStringStringArrayOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createStoredProcedureQuery("proc", "out1"))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void joinTransactionOnOpenEmIsNoOpForResourceLocal() {
        EntityManager em = openEm();
        // RESOURCE_LOCAL: joinTransaction is a no-op, does not throw
        em.joinTransaction();
        em.close();
    }

    @Test
    void isJoinedToTransactionReturnsFalseWhenNoActiveTransaction() {
        EntityManager em = openEm();
        assertThat(em.isJoinedToTransaction()).isFalse();
        em.close();
    }

    @Test
    void getCriteriaBuilderOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(em::getCriteriaBuilder)
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void getMetamodelOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(em::getMetamodel)
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void createEntityGraphClassOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createEntityGraph(Object.class))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void createEntityGraphStringOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createEntityGraph("graph"))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void getEntityGraphStringOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.getEntityGraph("graph"))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void getEntityGraphsClassOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.getEntityGraphs(Object.class))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void runWithConnectionOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.runWithConnection(con -> {}))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void callWithConnectionOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.callWithConnection(con -> null))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void getTransactionReturnsEntityTransactionForResourceLocal() {
        EntityManager em = openEm();
        jakarta.persistence.EntityTransaction tx = em.getTransaction();
        assertThat(tx).isNotNull();
        assertThat(tx.isActive()).isFalse();
        em.close();
    }

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
        Object entity = new Object();
        assertThatThrownBy(() -> em.getReference(entity))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void lockObjectLockModeTypeOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        Object entity = new Object();
        assertThatThrownBy(() -> em.lock(entity, jakarta.persistence.LockModeType.NONE))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void lockObjectLockModeTypeMapOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        Object entity = new Object();
        Map<String, Object> props = Map.of();
        assertThatThrownBy(() -> em.lock(entity, jakarta.persistence.LockModeType.NONE, props))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void lockObjectLockModeTypeLockOptionArrayOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        Object entity = new Object();
        assertThatThrownBy(() -> em.lock(entity, jakarta.persistence.LockModeType.NONE, (jakarta.persistence.LockOption[]) new jakarta.persistence.LockOption[0]))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void getLockModeOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        Object entity = new Object();
        assertThatThrownBy(() -> em.getLockMode(entity))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void entityTransactionBeginCommitCycle() {
        EntityManager em = openEm();
        jakarta.persistence.EntityTransaction tx = em.getTransaction();
        assertThat(tx.isActive()).isFalse();
        tx.begin();
        assertThat(tx.isActive()).isTrue();
        assertThat(em.isJoinedToTransaction()).isTrue();
        tx.commit();
        assertThat(tx.isActive()).isFalse();
        assertThat(em.isJoinedToTransaction()).isFalse();
        em.close();
    }

    @Test
    void entityTransactionBeginRollbackCycle() {
        EntityManager em = openEm();
        jakarta.persistence.EntityTransaction tx = em.getTransaction();
        tx.begin();
        assertThat(tx.isActive()).isTrue();
        tx.rollback();
        assertThat(tx.isActive()).isFalse();
        em.close();
    }

    @Test
    void entityTransactionBeginWhenActiveThrowsIllegalStateException() {
        EntityManager em = openEm();
        jakarta.persistence.EntityTransaction tx = em.getTransaction();
        tx.begin();
        assertThatThrownBy(tx::begin)
                .isInstanceOf(IllegalStateException.class);
        tx.rollback();
        em.close();
    }

    @Test
    void entityTransactionCommitWhenNotActiveThrowsIllegalStateException() {
        EntityManager em = openEm();
        jakarta.persistence.EntityTransaction tx = em.getTransaction();
        assertThatThrownBy(tx::commit)
                .isInstanceOf(IllegalStateException.class);
        em.close();
    }

    @Test
    void entityTransactionSetRollbackOnlyThenCommitThrowsRollbackException() {
        EntityManager em = openEm();
        jakarta.persistence.EntityTransaction tx = em.getTransaction();
        tx.begin();
        tx.setRollbackOnly();
        assertThat(tx.getRollbackOnly()).isTrue();
        assertThatThrownBy(tx::commit)
                .isInstanceOf(jakarta.persistence.RollbackException.class);
        assertThat(tx.isActive()).isFalse();
        em.close();
    }

    @Test
    void refreshObjectMapOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        Object entity = new Object();
        Map<String, Object> props = Map.of();
        assertThatThrownBy(() -> em.refresh(entity, props))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void refreshObjectLockModeTypeOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        Object entity = new Object();
        assertThatThrownBy(() -> em.refresh(entity, jakarta.persistence.LockModeType.NONE))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void refreshObjectLockModeTypeMapOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        Object entity = new Object();
        Map<String, Object> props = Map.of();
        assertThatThrownBy(() -> em.refresh(entity, jakarta.persistence.LockModeType.NONE, props))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void refreshObjectRefreshOptionArrayOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        Object entity = new Object();
        jakarta.persistence.RefreshOption[] options = new jakarta.persistence.RefreshOption[0];
        assertThatThrownBy(() -> em.refresh(entity, options))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void findClassObjectMapOnOpenEmReturnsEntity() {
        EntityManager em = openEm();
        SimpleEntity e = new SimpleEntity();
        e.setId(1L);
        e.setName("test");
        em.persist(e);
        SimpleEntity found = em.find(SimpleEntity.class, 1L, Map.of());
        assertThat(found).isSameAs(e);
        em.close();
    }

    @Test
    void findClassObjectLockModeTypeOnOpenEmReturnsEntity() {
        EntityManager em = openEm();
        SimpleEntity e = new SimpleEntity();
        e.setId(1L);
        e.setName("test");
        em.persist(e);
        SimpleEntity found = em.find(SimpleEntity.class, 1L, jakarta.persistence.LockModeType.NONE);
        assertThat(found).isSameAs(e);
        em.close();
    }

    @Test
    void findClassObjectLockModeTypeMapOnOpenEmReturnsEntity() {
        EntityManager em = openEm();
        SimpleEntity e = new SimpleEntity();
        e.setId(1L);
        e.setName("test");
        em.persist(e);
        SimpleEntity found = em.find(SimpleEntity.class, 1L, jakarta.persistence.LockModeType.NONE, Map.of());
        assertThat(found).isSameAs(e);
        em.close();
    }

    @Test
    void findClassObjectFindOptionArrayOnOpenEmReturnsEntity() {
        EntityManager em = openEm();
        SimpleEntity e = new SimpleEntity();
        e.setId(1L);
        e.setName("test");
        em.persist(e);
        SimpleEntity found = em.find(SimpleEntity.class, 1L, new jakarta.persistence.FindOption[0]);
        assertThat(found).isSameAs(e);
        em.close();
    }

    @Test
    void findEntityGraphObjectFindOptionArrayOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        @SuppressWarnings("unchecked")
        EntityGraph<Object> graph = (EntityGraph<Object>) null;
        assertThatThrownBy(() -> em.find(graph, 1L))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    void mergeOnOpenEmWithSimpleEntityReturnsManagedEntity() {
        EntityManager em = openEm();
        SimpleEntity e = new SimpleEntity();
        e.setId(1L);
        e.setName("test");
        em.persist(e);
        SimpleEntity merged = em.merge(e);
        assertThat(merged).isSameAs(e);
        assertThat(em.contains(merged)).isTrue();
        em.close();
    }

    @Test
    void removeOnOpenEmWithManagedSimpleEntityWorks() {
        EntityManager em = openEm();
        SimpleEntity e = new SimpleEntity();
        e.setId(1L);
        e.setName("test");
        em.persist(e);
        em.remove(e);
        assertThat(em.contains(e)).isTrue(); // REMOVED state still considered contained
        em.close();
    }

    @Test
    void detachOnOpenEmWithManagedSimpleEntityMakesItNotContained() {
        EntityManager em = openEm();
        SimpleEntity e = new SimpleEntity();
        e.setId(1L);
        e.setName("test");
        em.persist(e);
        em.detach(e);
        assertThat(em.contains(e)).isFalse();
        em.close();
    }

    @Test
    void clearOnOpenEmMakesAllEntitiesNotContained() {
        EntityManager em = openEm();
        SimpleEntity e = new SimpleEntity();
        e.setId(1L);
        e.setName("test");
        em.persist(e);
        em.clear();
        assertThat(em.contains(e)).isFalse();
        em.close();
    }

    @Test
    void isOpenOnFreshEmReturnsTrue() {
        EntityManager em = openEm();
        assertThat(em.isOpen()).isTrue();
        em.close();
    }

    // ── Closed-state contract: blanket IllegalStateException ─────────────

    @Test
    void mergeOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        Object entity = new Object();
        assertThatThrownBy(() -> em.merge(entity))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void removeOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        Object entity = new Object();
        assertThatThrownBy(() -> em.remove(entity))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refreshOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        Object entity = new Object();
        assertThatThrownBy(() -> em.refresh(entity))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void clearOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::clear)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void detachOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        Object entity = new Object();
        assertThatThrownBy(() -> em.detach(entity))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void containsOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        Object entity = new Object();
        assertThatThrownBy(() -> em.contains(entity))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void lockOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        Object entity = new Object();
        assertThatThrownBy(() -> em.lock(entity, jakarta.persistence.LockModeType.NONE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getLockModeOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        Object entity = new Object();
        assertThatThrownBy(() -> em.getLockMode(entity))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getReferenceOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.getReference(Object.class, 1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void setCacheStoreModeOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.setCacheStoreMode(CacheStoreMode.USE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getCacheStoreModeOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::getCacheStoreMode)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getCacheRetrieveModeOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::getCacheRetrieveMode)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void setPropertyOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.setProperty("key", "value"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createNamedQueryOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.createNamedQuery("q"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createNativeQueryOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.createNativeQuery("select 1"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createNamedStoredProcedureQueryOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.createNamedStoredProcedureQuery("proc"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createStoredProcedureQueryOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.createStoredProcedureQuery("proc"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void joinTransactionOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::joinTransaction)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void isJoinedToTransactionOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::isJoinedToTransaction)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getTransactionOnClosedEmDoesNotThrow() {
        EntityManager em = openEm();
        em.close();
        // getTransaction is exempt from the closed-state contract
        jakarta.persistence.EntityTransaction tx = em.getTransaction();
        assertThat(tx).isNotNull();
    }

    @Test
    void getCriteriaBuilderOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::getCriteriaBuilder)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getMetamodelOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::getMetamodel)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createEntityGraphOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.createEntityGraph(Object.class))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getEntityGraphOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.getEntityGraph("graph"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getEntityGraphsOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.getEntityGraphs(Object.class))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void runWithConnectionOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.runWithConnection(con -> {}))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void callWithConnectionOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.callWithConnection(con -> null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getReferenceEntityOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        Object entity = new Object();
        assertThatThrownBy(() -> em.getReference(entity))
                .isInstanceOf(IllegalStateException.class);
    }
}
