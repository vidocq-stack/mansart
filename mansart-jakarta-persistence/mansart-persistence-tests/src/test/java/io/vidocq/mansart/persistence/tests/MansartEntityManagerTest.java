/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.core.MansartEntityManager;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
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
public class MansartEntityManagerTest {

    private static EntityManager openEm() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        return emf.createEntityManager();
    }

    // ── Closed-state contract: blanket IllegalStateException ─────────────

    @Test
    public void persistOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.persist(new Object()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void findOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.find(Object.class, 1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void createQueryOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.createQuery("select e from E e"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void getEntityManagerFactoryOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::getEntityManagerFactory)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void setFlushModeOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.setFlushMode(FlushModeType.COMMIT))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void getFlushModeOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::getFlushMode)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void unwrapOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.unwrap(MansartEntityManager.class))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void getDelegateOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::getDelegate)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void setCacheRetrieveModeOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.setCacheRetrieveMode(CacheRetrieveMode.BYPASS))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── Closed-state contract: exemptions ───────────────────────────────

    @Test
    public void isOpenOnClosedEmReturnsFalse() {
        EntityManager em = openEm();
        em.close();
        assertThat(em.isOpen()).isFalse();
    }

    @Test
    public void getPropertiesOnClosedEmDoesNotThrow() {
        EntityManager em = openEm();
        em.close();
        // Spec exemption: getProperties() is permitted on a closed EM
        Map<String, Object> props = em.getProperties();
        assertThat(props).isNotNull();
    }

    // ── Configuration storage: flush mode ───────────────────────────────

    @Test
    public void getFlushModeDefaultsToAuto() {
        EntityManager em = openEm();
        assertThat(em.getFlushMode()).isEqualTo(FlushModeType.AUTO);
        em.close();
    }

    @Test
    public void setFlushModeIsRetained() {
        EntityManager em = openEm();
        em.setFlushMode(FlushModeType.COMMIT);
        assertThat(em.getFlushMode()).isEqualTo(FlushModeType.COMMIT);
        em.close();
    }

    // ── Configuration storage: cache modes ──────────────────────────────

    @Test
    public void getCacheRetrieveModeDefaultsToUse() {
        EntityManager em = openEm();
        assertThat(em.getCacheRetrieveMode()).isEqualTo(CacheRetrieveMode.USE);
        em.close();
    }

    @Test
    public void setCacheRetrieveModeIsRetained() {
        EntityManager em = openEm();
        em.setCacheRetrieveMode(CacheRetrieveMode.BYPASS);
        assertThat(em.getCacheRetrieveMode()).isEqualTo(CacheRetrieveMode.BYPASS);
        em.close();
    }

    @Test
    public void getCacheStoreModeDefaultsToUse() {
        EntityManager em = openEm();
        assertThat(em.getCacheStoreMode()).isEqualTo(CacheStoreMode.USE);
        em.close();
    }

    @Test
    public void setCacheStoreModeIsRetained() {
        EntityManager em = openEm();
        em.setCacheStoreMode(CacheStoreMode.BYPASS);
        assertThat(em.getCacheStoreMode()).isEqualTo(CacheStoreMode.BYPASS);
        em.close();
    }

    // ── Configuration storage: properties ───────────────────────────────

    @Test
    public void getPropertiesIncludesFactoryProperties() {
        EntityManager em = openEm();
        assertThat(em.getProperties().get("io.vidocq.mansart.test.marker"))
                .isEqualTo("from-xml");
        em.close();
    }

    @Test
    public void setPropertyIsVisibleInGetProperties() {
        EntityManager em = openEm();
        em.setProperty("custom.key", "custom.value");
        assertThat(em.getProperties().get("custom.key")).isEqualTo("custom.value");
        em.close();
    }

    @Test
    public void setPropertyOverridesFactoryProperty() {
        EntityManager em = openEm();
        em.setProperty("io.vidocq.mansart.test.marker", "overridden");
        assertThat(em.getProperties().get("io.vidocq.mansart.test.marker"))
                .isEqualTo("overridden");
        em.close();
    }

    // ── unwrap / getDelegate / getEntityManagerFactory ───────────────────

    @Test
    public void unwrapToMansartEntityManagerReturnsSameInstance() {
        EntityManager em = openEm();
        MansartEntityManager unwrapped = em.unwrap(MansartEntityManager.class);
        assertThat(unwrapped).isSameAs(em);
        em.close();
    }

    @Test
    public void unwrapToEntityManagerReturnsSameInstance() {
        EntityManager em = openEm();
        EntityManager unwrapped = em.unwrap(EntityManager.class);
        assertThat(unwrapped).isSameAs(em);
        em.close();
    }

    @Test
    public void unwrapToUnsupportedTypeThrowsPersistenceException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.unwrap(String.class))
                .isInstanceOf(PersistenceException.class);
        em.close();
    }

    @Test
    public void getDelegateReturnsSameInstance() {
        EntityManager em = openEm();
        assertThat(em.getDelegate()).isSameAs(em);
        em.close();
    }

    @Test
    public void getEntityManagerFactoryReturnsCreatingFactory() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        EntityManager em = emf.createEntityManager();
        assertThat(em.getEntityManagerFactory()).isSameAs(emf);
        em.close();
        emf.close();
    }

    // ── Delegation seam to persistence context ───────────────────────────

    @Test
    public void persistOnOpenEmDelegatesToPersistenceContext() {
        // The persistence context state machine is M4-JP-26; the EM now delegates
        // to the persistence context which validates entities and throws
        // IllegalArgumentException for non-@Entity instances.
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.persist(new Object()))
                .isInstanceOf(IllegalArgumentException.class);
        em.close();
    }

    @Test
    public void containsOnOpenEmDelegatesToPersistenceContext() {
        // The persistence context state machine is M4-JP-26; the EM now delegates
        // to the persistence context which returns false for non-managed entities.
        EntityManager em = openEm();
        assertThat(em.contains(new Object())).isFalse();
        em.close();
    }

    @Test
    public void closeIsIdempotentThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::close).isInstanceOf(IllegalStateException.class);
    }

    // ── UnsupportedOperationException on open EM ────────────────────────────

    @Test
    public void createQueryStringOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createQuery("select e from E e"))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void createQueryCriteriaQueryOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createQuery((jakarta.persistence.criteria.CriteriaQuery<?>) null))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void createQueryCriteriaSelectOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createQuery((jakarta.persistence.criteria.CriteriaSelect<?>) null))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void createQueryCriteriaUpdateOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createQuery((jakarta.persistence.criteria.CriteriaUpdate<?>) null))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void createQueryCriteriaDeleteOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createQuery((jakarta.persistence.criteria.CriteriaDelete<?>) null))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void createQueryStringClassOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createQuery("select e from E e", Object.class))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void createNamedQueryStringOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createNamedQuery("someNamedQuery"))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void createNamedQueryStringClassOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createNamedQuery("someNamedQuery", Object.class))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void createQueryTypedQueryReferenceOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createQuery((jakarta.persistence.TypedQueryReference<?>) null))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void createNativeQueryStringOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createNativeQuery("select 1"))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void createNativeQueryStringClassOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createNativeQuery("select 1", Object.class))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void createNativeQueryStringStringOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createNativeQuery("select 1", "alias"))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void createNamedStoredProcedureQueryStringOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createNamedStoredProcedureQuery("proc"))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void createStoredProcedureQueryStringOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createStoredProcedureQuery("proc"))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void createStoredProcedureQueryStringClassArrayOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createStoredProcedureQuery("proc", Object.class))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void createStoredProcedureQueryStringStringArrayOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createStoredProcedureQuery("proc", "out1"))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void joinTransactionOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(em::joinTransaction)
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void isJoinedToTransactionOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(em::isJoinedToTransaction)
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void getCriteriaBuilderOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(em::getCriteriaBuilder)
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void getMetamodelOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(em::getMetamodel)
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void createEntityGraphClassOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createEntityGraph(Object.class))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void createEntityGraphStringOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.createEntityGraph("graph"))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void getEntityGraphStringOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.getEntityGraph("graph"))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void getEntityGraphsClassOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.getEntityGraphs(Object.class))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void runWithConnectionOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.runWithConnection(con -> {}))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void callWithConnectionOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.callWithConnection(con -> null))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void getTransactionOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(em::getTransaction)
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void getReferenceClassObjectOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.getReference(Object.class, 1L))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void getReferenceEntityOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.getReference(new Object()))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void lockObjectLockModeTypeOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.lock(new Object(), jakarta.persistence.LockModeType.NONE))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void lockObjectLockModeTypeMapOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.lock(new Object(), jakarta.persistence.LockModeType.NONE, Map.of()))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void lockObjectLockModeTypeLockOptionArrayOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.lock(new Object(), jakarta.persistence.LockModeType.NONE))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void getLockModeOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.getLockMode(new Object()))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void refreshObjectMapOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.refresh(new Object(), Map.of()))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void refreshObjectLockModeTypeOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.refresh(new Object(), jakarta.persistence.LockModeType.NONE))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void refreshObjectLockModeTypeMapOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.refresh(new Object(), jakarta.persistence.LockModeType.NONE, Map.of()))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void refreshObjectRefreshOptionArrayOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        jakarta.persistence.RefreshOption[] options = new jakarta.persistence.RefreshOption[0];
        assertThatThrownBy(() -> em.refresh(new Object(), options))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void findClassObjectMapOnOpenEmReturnsEntity() {
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
    public void findClassObjectLockModeTypeOnOpenEmReturnsEntity() {
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
    public void findClassObjectLockModeTypeMapOnOpenEmReturnsEntity() {
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
    public void findClassObjectFindOptionArrayOnOpenEmReturnsEntity() {
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
    public void findEntityGraphObjectFindOptionArrayOnOpenEmThrowsUnsupportedOperationException() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.find(em.createEntityGraph(Object.class), 1L))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void mergeOnOpenEmWithSimpleEntityReturnsManagedEntity() {
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
    public void removeOnOpenEmWithManagedSimpleEntityWorks() {
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
    public void detachOnOpenEmWithManagedSimpleEntityMakesItNotContained() {
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
    public void clearOnOpenEmMakesAllEntitiesNotContained() {
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
    public void isOpenOnFreshEmReturnsTrue() {
        EntityManager em = openEm();
        assertThat(em.isOpen()).isTrue();
        em.close();
    }

    // ── Closed-state contract: blanket IllegalStateException ─────────────

    @Test
    public void mergeOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.merge(new Object()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void removeOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.remove(new Object()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void refreshOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.refresh(new Object()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void clearOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::clear)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void detachOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.detach(new Object()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void containsOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.contains(new Object()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void lockOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.lock(new Object(), jakarta.persistence.LockModeType.NONE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void getLockModeOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.getLockMode(new Object()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void getReferenceOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.getReference(Object.class, 1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void setCacheStoreModeOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.setCacheStoreMode(CacheStoreMode.USE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void getCacheStoreModeOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::getCacheStoreMode)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void getCacheRetrieveModeOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::getCacheRetrieveMode)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void setPropertyOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.setProperty("key", "value"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void createNamedQueryOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.createNamedQuery("q"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void createNativeQueryOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.createNativeQuery("select 1"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void createNamedStoredProcedureQueryOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.createNamedStoredProcedureQuery("proc"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void createStoredProcedureQueryOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.createStoredProcedureQuery("proc"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void joinTransactionOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::joinTransaction)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void isJoinedToTransactionOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::isJoinedToTransaction)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void getCriteriaBuilderOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::getCriteriaBuilder)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void getMetamodelOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::getMetamodel)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void createEntityGraphOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.createEntityGraph(Object.class))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void getEntityGraphOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.getEntityGraph("graph"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void getEntityGraphsOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.getEntityGraphs(Object.class))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void runWithConnectionOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.runWithConnection(con -> {}))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void callWithConnectionOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.callWithConnection(con -> null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void getReferenceEntityOnClosedEmThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(() -> em.getReference(new Object()))
                .isInstanceOf(IllegalStateException.class);
    }
}
