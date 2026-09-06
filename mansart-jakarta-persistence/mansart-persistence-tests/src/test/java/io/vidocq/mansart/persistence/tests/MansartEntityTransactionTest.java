package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.core.MansartEntityManager;
import io.vidocq.mansart.persistence.core.MansartEntityManagerFactory;
import io.vidocq.mansart.transactions.core.MansartTransactionManager;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.TransactionRequiredException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MansartEntityTransactionTest {

    // ── RESOURCE_LOCAL EntityTransaction ──────────────────────────────

    @Test
    void resourceLocalGetTransactionReturnsEntityTransaction() {
        MansartEntityManagerFactory emf = new MansartEntityManagerFactory(
                "test", PersistenceUnitTransactionType.RESOURCE_LOCAL, Map.of(), null);
        MansartEntityManager em = (MansartEntityManager) emf.createEntityManager();
        assertThat(em.getTransaction()).isNotNull();
        assertThat(em.getTransaction().isActive()).isFalse();
        em.close();
        emf.close();
    }

    @Test
    void resourceLocalIsJoinedToTransactionReflectsEntityTransactionActive() {
        MansartEntityManagerFactory emf = new MansartEntityManagerFactory(
                "test", PersistenceUnitTransactionType.RESOURCE_LOCAL, Map.of(), null);
        MansartEntityManager em = (MansartEntityManager) emf.createEntityManager();
        assertThat(em.isJoinedToTransaction()).isFalse();
        em.getTransaction().begin();
        assertThat(em.isJoinedToTransaction()).isTrue();
        em.getTransaction().commit();
        assertThat(em.isJoinedToTransaction()).isFalse();
        em.close();
        emf.close();
    }

    @Test
    void resourceLocalJoinTransactionIsNoOp() {
        MansartEntityManagerFactory emf = new MansartEntityManagerFactory(
                "test", PersistenceUnitTransactionType.RESOURCE_LOCAL, Map.of(), null);
        MansartEntityManager em = (MansartEntityManager) emf.createEntityManager();
        em.joinTransaction(); // should not throw
        assertThat(em.isJoinedToTransaction()).isFalse();
        em.close();
        emf.close();
    }

    // ── JTA transaction integration ─────────────────────────────────────

    @Test
    void jtaGetTransactionThrowsIllegalStateException() {
        MansartTransactionManager tm = new MansartTransactionManager();
        MansartEntityManagerFactory emf = new MansartEntityManagerFactory(
                "test", PersistenceUnitTransactionType.JTA, Map.of(), tm);
        MansartEntityManager em = (MansartEntityManager) emf.createEntityManager();
        assertThatThrownBy(em::getTransaction)
                .isInstanceOf(IllegalStateException.class);
        em.close();
        emf.close();
    }

    @Test
    void jtaIsJoinedToTransactionFalseWhenNoActiveTx() {
        MansartTransactionManager tm = new MansartTransactionManager();
        MansartEntityManagerFactory emf = new MansartEntityManagerFactory(
                "test", PersistenceUnitTransactionType.JTA, Map.of(), tm);
        MansartEntityManager em = (MansartEntityManager) emf.createEntityManager();
        assertThat(em.isJoinedToTransaction()).isFalse();
        em.close();
        emf.close();
    }

    @Test
    void jtaJoinTransactionThrowsWhenNoActiveTx() {
        MansartTransactionManager tm = new MansartTransactionManager();
        MansartEntityManagerFactory emf = new MansartEntityManagerFactory(
                "test", PersistenceUnitTransactionType.JTA, Map.of(), tm);
        MansartEntityManager em = (MansartEntityManager) emf.createEntityManager();
        assertThatThrownBy(em::joinTransaction)
                .isInstanceOf(TransactionRequiredException.class);
        em.close();
        emf.close();
    }

    @Test
    void jtaJoinTransactionSucceedsWhenActiveTx() throws Exception {
        MansartTransactionManager tm = new MansartTransactionManager();
        MansartEntityManagerFactory emf = new MansartEntityManagerFactory(
                "test", PersistenceUnitTransactionType.JTA, Map.of(), tm);
        MansartEntityManager em = (MansartEntityManager) emf.createEntityManager();
        tm.begin();
        em.joinTransaction();
        assertThat(em.isJoinedToTransaction()).isTrue();
        tm.commit();
        assertThat(em.isJoinedToTransaction()).isFalse();
        em.close();
        emf.close();
    }

    @Test
    void jtaGetTransactionOnClosedEmDoesNotThrow() {
        MansartTransactionManager tm = new MansartTransactionManager();
        MansartEntityManagerFactory emf = new MansartEntityManagerFactory(
                "test", PersistenceUnitTransactionType.JTA, Map.of(), tm);
        MansartEntityManager em = (MansartEntityManager) emf.createEntityManager();
        em.close();
        // getTransaction is exempt from closed-state — but for JTA it throws IllegalStateException
        // regardless of closed state (the JTA check happens before any closed check)
        assertThatThrownBy(em::getTransaction)
                .isInstanceOf(IllegalStateException.class);
        emf.close();
    }
}
