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
        // The persistence context state machine is M4-JP-26; until then the
        // delegated operation throws UnsupportedOperationException, proving
        // the EM delegates rather than handling it inline.
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.persist(new Object()))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void containsOnOpenEmDelegatesToPersistenceContext() {
        EntityManager em = openEm();
        assertThatThrownBy(() -> em.contains(new Object()))
                .isInstanceOf(UnsupportedOperationException.class);
        em.close();
    }

    @Test
    public void closeIsIdempotentThrowsIllegalStateException() {
        EntityManager em = openEm();
        em.close();
        assertThatThrownBy(em::close).isInstanceOf(IllegalStateException.class);
    }
}
