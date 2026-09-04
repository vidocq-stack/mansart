/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.core.MansartEntityManagerFactory;
import io.vidocq.mansart.persistence.core.MansartPersistenceProvider;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for M4-JP-24: MansartEntityManagerFactory manages EntityManager instances,
 * holds bootstrap metadata (transaction type), and implements the closed-state
 * contract and metadata accessor methods.
 */
public class MansartEntityManagerFactoryTest {

    // ── EM instance management ──────────────────────────────────────────

    @Test
    public void createEntityManagerReturnsOpenEm() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        EntityManager em = emf.createEntityManager();
        assertThat(em.isOpen()).isTrue();
        assertThat(em.getEntityManagerFactory()).isSameAs(emf);

        em.close();
        emf.close();
    }

    @Test
    public void createdEntityManagerIsClosedWhenFactoryCloses() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        EntityManager em = emf.createEntityManager();
        assertThat(em.isOpen()).isTrue();

        emf.close();
        // Spec: once the factory is closed, all its entity managers are considered closed
        assertThat(em.isOpen()).isFalse();
    }

    @Test
    public void multipleEntityManagersClosedWhenFactoryCloses() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        EntityManager em1 = emf.createEntityManager();
        EntityManager em2 = emf.createEntityManager();

        emf.close();
        assertThat(em1.isOpen()).isFalse();
        assertThat(em2.isOpen()).isFalse();
    }

    @Test
    public void alreadyClosedEntityManagerRemainsClosedOnFactoryClose() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        EntityManager em = emf.createEntityManager();
        em.close();
        assertThat(em.isOpen()).isFalse();

        // Factory close must not throw even though EM is already closed
        emf.close();
        assertThat(em.isOpen()).isFalse();
    }

    @Test
    public void createEntityManagerAfterFactoryCloseThrowsIllegalStateException() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        emf.close();

        assertThatThrownBy(emf::createEntityManager)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void createEntityManagerWithMapAfterFactoryCloseThrowsIllegalStateException() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        emf.close();

        assertThatThrownBy(() -> emf.createEntityManager(java.util.Map.of()))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── Bootstrap metadata: transaction type ────────────────────────────

    @Test
    public void getTransactionTypeReturnsResourceLocalFromXml() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        assertThat(emf.getTransactionType())
                .isEqualTo(PersistenceUnitTransactionType.RESOURCE_LOCAL);

        emf.close();
    }

    @Test
    public void getTransactionTypeReturnsJtaFromXml() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("second-pu");

        assertThat(emf.getTransactionType())
                .isEqualTo(PersistenceUnitTransactionType.JTA);

        emf.close();
    }

    @Test
    public void getTransactionTypeReturnsResourceLocalByDefaultFromConfiguration() {
        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        EntityManagerFactory emf = provider.createEntityManagerFactory(
                new jakarta.persistence.PersistenceConfiguration("cfg-pu"));

        assertThat(emf.getTransactionType())
                .isEqualTo(PersistenceUnitTransactionType.RESOURCE_LOCAL);

        emf.close();
    }

    @Test
    public void getTransactionTypeReturnsJtaFromConfiguration() {
        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        EntityManagerFactory emf = provider.createEntityManagerFactory(
                new jakarta.persistence.PersistenceConfiguration("cfg-jta-pu")
                        .transactionType(PersistenceUnitTransactionType.JTA));

        assertThat(emf.getTransactionType())
                .isEqualTo(PersistenceUnitTransactionType.JTA);

        emf.close();
    }

    // ── Closed-state contract on metadata methods ─────────────────────

    @Test
    public void getCacheReturnsNullWhenNoL2Cache() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        Cache cache = emf.getCache();
        // Spec: returns null if no second-level cache is in use
        assertThat(cache).isNull();

        emf.close();
    }

    @Test
    public void getCacheThrowsIllegalStateExceptionWhenClosed() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        emf.close();

        assertThatThrownBy(emf::getCache)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void getPersistenceUnitUtilReturnsNonNull() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        PersistenceUnitUtil util = emf.getPersistenceUnitUtil();
        assertThat(util).isNotNull();

        emf.close();
    }

    @Test
    public void getPersistenceUnitUtilThrowsIllegalStateExceptionWhenClosed() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        emf.close();

        assertThatThrownBy(emf::getPersistenceUnitUtil)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void getPropertiesThrowsIllegalStateExceptionWhenClosed() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        emf.close();

        assertThatThrownBy(emf::getProperties)
                .isInstanceOf(IllegalStateException.class);
    }

    // ── unwrap ────────────────────────────────────────────────────────

    @Test
    public void unwrapToMansartEntityManagerFactoryReturnsSameInstance() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        MansartEntityManagerFactory unwrapped = emf.unwrap(MansartEntityManagerFactory.class);
        assertThat(unwrapped).isSameAs(emf);

        emf.close();
    }

    @Test
    public void unwrapToEntityManagerFactoryReturnsSameInstance() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        EntityManagerFactory unwrapped = emf.unwrap(EntityManagerFactory.class);
        assertThat(unwrapped).isSameAs(emf);

        emf.close();
    }

    @Test
    public void unwrapToUnsupportedTypeThrowsPersistenceException() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        assertThatThrownBy(() -> emf.unwrap(String.class))
                .isInstanceOf(PersistenceException.class);

        emf.close();
    }

    @Test
    public void unwrapThrowsIllegalStateExceptionWhenClosed() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        emf.close();

        assertThatThrownBy(() -> emf.unwrap(MansartEntityManagerFactory.class))
                .isInstanceOf(IllegalStateException.class);
    }
}
