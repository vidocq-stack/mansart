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
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for M4-JP-24: MansartEntityManagerFactory manages EntityManager instances,
 * holds bootstrap metadata (transaction type), and implements the closed-state
 * contract and metadata accessor methods.
 */
class MansartEntityManagerFactoryTest {

    // ── EM instance management ──────────────────────────────────────────

    @Test
    void createEntityManagerReturnsOpenEm() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        EntityManager em = emf.createEntityManager();
        assertThat(em.isOpen()).isTrue();
        assertThat(em.getEntityManagerFactory()).isSameAs(emf);

        em.close();
        emf.close();
    }

    @Test
    void createdEntityManagerIsClosedWhenFactoryCloses() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        EntityManager em = emf.createEntityManager();
        assertThat(em.isOpen()).isTrue();

        emf.close();
        // Spec: once the factory is closed, all its entity managers are considered closed
        assertThat(em.isOpen()).isFalse();
    }

    @Test
    void multipleEntityManagersClosedWhenFactoryCloses() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        EntityManager em1 = emf.createEntityManager();
        EntityManager em2 = emf.createEntityManager();

        emf.close();
        assertThat(em1.isOpen()).isFalse();
        assertThat(em2.isOpen()).isFalse();
    }

    @Test
    void alreadyClosedEntityManagerRemainsClosedOnFactoryClose() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        EntityManager em = emf.createEntityManager();
        em.close();
        assertThat(em.isOpen()).isFalse();

        // Factory close must not throw even though EM is already closed
        emf.close();
        assertThat(em.isOpen()).isFalse();
    }

    @Test
    void createEntityManagerAfterFactoryCloseThrowsIllegalStateException() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        emf.close();

        assertThatThrownBy(emf::createEntityManager)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createEntityManagerWithMapAfterFactoryCloseThrowsIllegalStateException() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        emf.close();

        Map<String, Object> props = java.util.Map.of();
        assertThatThrownBy(() -> emf.createEntityManager(props))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── Bootstrap metadata: transaction type ────────────────────────────

    @Test
    void getTransactionTypeReturnsResourceLocalFromXml() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        assertThat(emf.getTransactionType())
                .isEqualTo(PersistenceUnitTransactionType.RESOURCE_LOCAL);

        emf.close();
    }

    @Test
    void getTransactionTypeReturnsJtaFromXml() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("second-pu");

        assertThat(emf.getTransactionType())
                .isEqualTo(PersistenceUnitTransactionType.JTA);

        emf.close();
    }

    @Test
    void getTransactionTypeReturnsResourceLocalByDefaultFromConfiguration() {
        MansartPersistenceProvider provider = new MansartPersistenceProvider();
        EntityManagerFactory emf = provider.createEntityManagerFactory(
                new jakarta.persistence.PersistenceConfiguration("cfg-pu"));

        assertThat(emf.getTransactionType())
                .isEqualTo(PersistenceUnitTransactionType.RESOURCE_LOCAL);

        emf.close();
    }

    @Test
    void getTransactionTypeReturnsJtaFromConfiguration() {
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
    void getCacheReturnsNullWhenNoL2Cache() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        Cache cache = emf.getCache();
        // Spec: returns null if no second-level cache is in use
        assertThat(cache).isNull();

        emf.close();
    }

    @Test
    void getCacheThrowsIllegalStateExceptionWhenClosed() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        emf.close();

        assertThatThrownBy(emf::getCache)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getPersistenceUnitUtilReturnsNonNull() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        PersistenceUnitUtil util = emf.getPersistenceUnitUtil();
        assertThat(util).isNotNull();

        emf.close();
    }

    @Test
    void getPersistenceUnitUtilThrowsIllegalStateExceptionWhenClosed() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        emf.close();

        assertThatThrownBy(emf::getPersistenceUnitUtil)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getPropertiesThrowsIllegalStateExceptionWhenClosed() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        emf.close();

        assertThatThrownBy(emf::getProperties)
                .isInstanceOf(IllegalStateException.class);
    }

    // ── unwrap ────────────────────────────────────────────────────────

    @Test
    void unwrapToMansartEntityManagerFactoryReturnsSameInstance() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        MansartEntityManagerFactory unwrapped = emf.unwrap(MansartEntityManagerFactory.class);
        assertThat(unwrapped).isSameAs(emf);

        emf.close();
    }

    @Test
    void unwrapToEntityManagerFactoryReturnsSameInstance() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        EntityManagerFactory unwrapped = emf.unwrap(EntityManagerFactory.class);
        assertThat(unwrapped).isSameAs(emf);

        emf.close();
    }

    @Test
    void unwrapToUnsupportedTypeThrowsPersistenceException() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");

        assertThatThrownBy(() -> emf.unwrap(String.class))
                .isInstanceOf(PersistenceException.class);

        emf.close();
    }

    @Test
    void unwrapThrowsIllegalStateExceptionWhenClosed() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        emf.close();

        assertThatThrownBy(() -> emf.unwrap(MansartEntityManagerFactory.class))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── UnsupportedOperationException on open EMF ────────────────────────

    @Test
    void getCriteriaBuilderOnOpenEmfThrowsUnsupportedOperationException() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        assertThatThrownBy(emf::getCriteriaBuilder)
                .isInstanceOf(UnsupportedOperationException.class);
        emf.close();
    }

    @Test
    void getMetamodelOnOpenEmfThrowsUnsupportedOperationException() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        assertThatThrownBy(emf::getMetamodel)
                .isInstanceOf(UnsupportedOperationException.class);
        emf.close();
    }

    @Test
    void getSchemaManagerOnOpenEmfThrowsUnsupportedOperationException() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        assertThatThrownBy(emf::getSchemaManager)
                .isInstanceOf(UnsupportedOperationException.class);
        emf.close();
    }

    @Test
    void addNamedQueryOnOpenEmfThrowsUnsupportedOperationException() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        assertThatThrownBy(() -> emf.addNamedQuery("q", null))
                .isInstanceOf(UnsupportedOperationException.class);
        emf.close();
    }

    @Test
    void addNamedEntityGraphOnOpenEmfThrowsUnsupportedOperationException() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        assertThatThrownBy(() -> emf.addNamedEntityGraph("g", null))
                .isInstanceOf(UnsupportedOperationException.class);
        emf.close();
    }

    @Test
    void getNamedQueriesOnOpenEmfThrowsUnsupportedOperationException() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        assertThatThrownBy(() -> emf.getNamedQueries(Object.class))
                .isInstanceOf(UnsupportedOperationException.class);
        emf.close();
    }

    @Test
    void getNamedEntityGraphsOnOpenEmfThrowsUnsupportedOperationException() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        assertThatThrownBy(() -> emf.getNamedEntityGraphs(Object.class))
                .isInstanceOf(UnsupportedOperationException.class);
        emf.close();
    }

    @Test
    void runInTransactionOnOpenEmfThrowsUnsupportedOperationException() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        assertThatThrownBy(() -> emf.runInTransaction(c -> {}))
                .isInstanceOf(UnsupportedOperationException.class);
        emf.close();
    }

    @Test
    void callInTransactionOnOpenEmfThrowsUnsupportedOperationException() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        assertThatThrownBy(() -> emf.callInTransaction(f -> null))
                .isInstanceOf(UnsupportedOperationException.class);
        emf.close();
    }

    // ── Functional behavior ────────────────────────────────────────────

    @Test
    void getNameReturnsPersistenceUnitName() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        assertThat(emf.getName()).isEqualTo("test-pu");
        emf.close();
    }

    @Test
    void closeCalledTwiceThrowsIllegalStateException() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        emf.close();
        assertThatThrownBy(emf::close)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createEntityManagerWithCustomPropertiesIncludesThemInGetProperties() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        EntityManager em = emf.createEntityManager(java.util.Map.of("custom.key", "custom.value"));
        assertThat(em.getProperties()).containsEntry("custom.key", "custom.value");
        em.close();
        emf.close();
    }

    @Test
    void createEntityManagerWithSynchronizationTypeJtaOnJtaPuReturnsOpenEm() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("second-pu");
        EntityManager em = emf.createEntityManager(jakarta.persistence.SynchronizationType.SYNCHRONIZED);
        assertThat(em.isOpen()).isTrue();
        em.close();
        emf.close();
    }

    @Test
    void createEntityManagerWithSynchronizationTypeResourceLocalOnResourceLocalPuThrowsIllegalStateException() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        assertThatThrownBy(() -> emf.createEntityManager(jakarta.persistence.SynchronizationType.SYNCHRONIZED))
                .isInstanceOf(IllegalStateException.class);
        emf.close();
    }

    @Test
    void createEntityManagerWithSynchronizationTypeAndMapJtaOnJtaPuReturnsOpenEm() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("second-pu");
        EntityManager em = emf.createEntityManager(jakarta.persistence.SynchronizationType.SYNCHRONIZED, java.util.Map.of());
        assertThat(em.isOpen()).isTrue();
        em.close();
        emf.close();
    }

    @Test
    void createEntityManagerWithSynchronizationTypeAndMapResourceLocalOnResourceLocalPuThrowsIllegalStateException() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        Map<String, Object> props = java.util.Map.of();
        assertThatThrownBy(() -> emf.createEntityManager(jakarta.persistence.SynchronizationType.SYNCHRONIZED, props))
                .isInstanceOf(IllegalStateException.class);
        emf.close();
    }

    @Test
    void createEntityManagerWithSynchronizationTypeOnClosedEmfThrowsIllegalStateException() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        emf.close();
        assertThatThrownBy(() -> emf.createEntityManager(jakarta.persistence.SynchronizationType.SYNCHRONIZED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getPropertiesOnOpenEmfIncludesFactoryProperties() {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test-pu");
        assertThat(emf.getProperties()).containsEntry("io.vidocq.mansart.test.marker", "from-xml");
        emf.close();
    }
}
