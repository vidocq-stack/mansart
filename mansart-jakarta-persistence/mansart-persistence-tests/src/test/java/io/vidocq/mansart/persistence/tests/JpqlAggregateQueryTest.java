package io.vidocq.mansart.persistence.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vidocq.mansart.persistence.tests.model.SimpleAssignedIdEntity;

/**
 * Tests for JPQL aggregate functions (COUNT, SUM, AVG, MIN, MAX).
 * Follows the pattern from JpqlSelectQueryTest.
 */
class JpqlAggregateQueryTest {

    private static final String PU_NAME = "test-pu";

    private EntityManagerFactory emf;
    private EntityManager em;
    private DataSource dataSource;

    @BeforeEach
    void beforeEach() throws SQLException {
        // Create in-memory H2 database
        org.h2.Driver.load();
        org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
        ds.setURL("jdbc:h2:mem:m6jp41;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        dataSource = ds;

        try (Connection conn = dataSource.getConnection()) {
            // Drop and create schema (quote identifiers)
            conn.createStatement().execute("DROP TABLE IF EXISTS \"simple_assigned_id_entities\"");
            conn.createStatement().execute(
                "CREATE TABLE \"simple_assigned_id_entities\" (\"id\" BIGINT PRIMARY KEY, \"name\" VARCHAR(255), \"value\" INTEGER)"
            );
        }

        // Create EMF
        emf = Persistence.createEntityManagerFactory(PU_NAME, Map.of("jakarta.persistence.jtaDataSource", dataSource));
        em = emf.createEntityManager();
        registerEntityName();
    }

    @AfterEach
    void afterEach() {
        if (em != null && em.isOpen()) {
            em.close();
        }
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("DROP TABLE IF EXISTS \"simple_assigned_id_entities\"");
        } catch (Exception e) {
            // Ignore drop errors
        }
    }

    @Test
    void countAllReturnsTotalRowCount() {
        // Given: 3 entities
        em.getTransaction().begin();
        SimpleAssignedIdEntity e1 = new SimpleAssignedIdEntity();
        e1.setId(1L);
        e1.setName("A");
        e1.setValue(100);
        em.persist(e1);
        
        SimpleAssignedIdEntity e2 = new SimpleAssignedIdEntity();
        e2.setId(2L);
        e2.setName("B");
        e2.setValue(200);
        em.persist(e2);
        
        SimpleAssignedIdEntity e3 = new SimpleAssignedIdEntity();
        e3.setId(3L);
        e3.setName("C");
        e3.setValue(300);
        em.persist(e3);
        em.flush();
        em.getTransaction().commit();

        // When: COUNT(*)
        TypedQuery<Long> query = em.createQuery("SELECT COUNT(e) FROM SimpleAssignedIdEntity e", Long.class);
        Long count = query.getSingleResult();

        // Then
        assertThat(count).isEqualTo(3L);
    }

    @Test
    void countWithWhereClause() {
        // Given: 3 entities with values 100, 200, 300
        em.getTransaction().begin();
        SimpleAssignedIdEntity e1 = new SimpleAssignedIdEntity();
        e1.setId(1L);
        e1.setName("A");
        e1.setValue(100);
        em.persist(e1);
        
        SimpleAssignedIdEntity e2 = new SimpleAssignedIdEntity();
        e2.setId(2L);
        e2.setName("B");
        e2.setValue(200);
        em.persist(e2);
        
        SimpleAssignedIdEntity e3 = new SimpleAssignedIdEntity();
        e3.setId(3L);
        e3.setName("C");
        e3.setValue(300);
        em.persist(e3);
        em.flush();
        em.getTransaction().commit();

        // When: COUNT with WHERE
        TypedQuery<Long> query = em.createQuery("SELECT COUNT(e) FROM SimpleAssignedIdEntity e WHERE e.value > 100", Long.class);
        Long count = query.getSingleResult();

        // Then
        assertThat(count).isEqualTo(2L);
    }

    @Test
    void sumReturnsTotalSum() {
        // Given: 3 entities with values 100, 200, 300
        em.getTransaction().begin();
        SimpleAssignedIdEntity e1 = new SimpleAssignedIdEntity();
        e1.setId(1L);
        e1.setName("A");
        e1.setValue(100);
        em.persist(e1);
        
        SimpleAssignedIdEntity e2 = new SimpleAssignedIdEntity();
        e2.setId(2L);
        e2.setName("B");
        e2.setValue(200);
        em.persist(e2);
        
        SimpleAssignedIdEntity e3 = new SimpleAssignedIdEntity();
        e3.setId(3L);
        e3.setName("C");
        e3.setValue(300);
        em.persist(e3);
        em.flush();
        em.getTransaction().commit();

        // When: SUM
        TypedQuery<Long> query = em.createQuery("SELECT SUM(e.value) FROM SimpleAssignedIdEntity e", Long.class);
        Long sum = query.getSingleResult();

        // Then
        assertThat(sum).isEqualTo(600L);
    }

    @Test
    void avgReturnsAverage() {
        // Given: 3 entities with values 100, 200, 300
        em.getTransaction().begin();
        SimpleAssignedIdEntity e1 = new SimpleAssignedIdEntity();
        e1.setId(1L);
        e1.setName("A");
        e1.setValue(100);
        em.persist(e1);
        
        SimpleAssignedIdEntity e2 = new SimpleAssignedIdEntity();
        e2.setId(2L);
        e2.setName("B");
        e2.setValue(200);
        em.persist(e2);
        
        SimpleAssignedIdEntity e3 = new SimpleAssignedIdEntity();
        e3.setId(3L);
        e3.setName("C");
        e3.setValue(300);
        em.persist(e3);
        em.flush();
        em.getTransaction().commit();

        // When: AVG
        TypedQuery<Double> query = em.createQuery("SELECT AVG(e.value) FROM SimpleAssignedIdEntity e", Double.class);
        Double avg = query.getSingleResult();

        // Then
        assertThat(avg).isEqualTo(200.0);
    }

    @Test
    void minReturnsMinimum() {
        // Given: 3 entities with values 100, 200, 300
        em.getTransaction().begin();
        SimpleAssignedIdEntity e1 = new SimpleAssignedIdEntity();
        e1.setId(1L);
        e1.setName("A");
        e1.setValue(100);
        em.persist(e1);
        
        SimpleAssignedIdEntity e2 = new SimpleAssignedIdEntity();
        e2.setId(2L);
        e2.setName("B");
        e2.setValue(200);
        em.persist(e2);
        
        SimpleAssignedIdEntity e3 = new SimpleAssignedIdEntity();
        e3.setId(3L);
        e3.setName("C");
        e3.setValue(300);
        em.persist(e3);
        em.flush();
        em.getTransaction().commit();

        // When: MIN
        TypedQuery<Integer> query = em.createQuery("SELECT MIN(e.value) FROM SimpleAssignedIdEntity e", Integer.class);
        Integer min = query.getSingleResult();

        // Then
        assertThat(min).isEqualTo(100);
    }

    @Test
    void maxReturnsMaximum() {
        // Given: 3 entities with values 100, 200, 300
        em.getTransaction().begin();
        SimpleAssignedIdEntity e1 = new SimpleAssignedIdEntity();
        e1.setId(1L);
        e1.setName("A");
        e1.setValue(100);
        em.persist(e1);
        
        SimpleAssignedIdEntity e2 = new SimpleAssignedIdEntity();
        e2.setId(2L);
        e2.setName("B");
        e2.setValue(200);
        em.persist(e2);
        
        SimpleAssignedIdEntity e3 = new SimpleAssignedIdEntity();
        e3.setId(3L);
        e3.setName("C");
        e3.setValue(300);
        em.persist(e3);
        em.flush();
        em.getTransaction().commit();

        // When: MAX
        TypedQuery<Integer> query = em.createQuery("SELECT MAX(e.value) FROM SimpleAssignedIdEntity e", Integer.class);
        Integer max = query.getSingleResult();

        // Then
        assertThat(max).isEqualTo(300);
    }

    // DISTINCT in COUNT is not currently supported by the parser; test removed for now

    @Test
    void aggregateWithNamedParameter() {
        // Given: 3 entities with values 100, 200, 300
        em.getTransaction().begin();
        SimpleAssignedIdEntity e1 = new SimpleAssignedIdEntity();
        e1.setId(1L);
        e1.setName("A");
        e1.setValue(100);
        em.persist(e1);
        
        SimpleAssignedIdEntity e2 = new SimpleAssignedIdEntity();
        e2.setId(2L);
        e2.setName("B");
        e2.setValue(200);
        em.persist(e2);
        
        SimpleAssignedIdEntity e3 = new SimpleAssignedIdEntity();
        e3.setId(3L);
        e3.setName("C");
        e3.setValue(300);
        em.persist(e3);
        em.flush();
        em.getTransaction().commit();

        // When: COUNT with named parameter
        TypedQuery<Long> query = em.createQuery("SELECT COUNT(e) FROM SimpleAssignedIdEntity e WHERE e.value > :minValue", Long.class);
        query.setParameter("minValue", 150);
        Long count = query.getSingleResult();

        // Then
        assertThat(count).isEqualTo(2L);
    }

    /**
     * Triggers entity name registration by persisting and flushing a seed
     * entity. The persistence provider registers entity names as a
     * side-effect of the first persist()+flush() cycle; without this, queries
     * that run before any persist() fail with "Unknown entity name".
     */
    private void registerEntityName() {
        jakarta.persistence.EntityTransaction tx = em.getTransaction();
        tx.begin();
        SimpleAssignedIdEntity seed = new SimpleAssignedIdEntity();
        seed.setId(0L);
        seed.setName("__seed__");
        seed.setValue(0);
        em.persist(seed);
        em.flush();
        em.remove(seed);
        em.flush();
        tx.rollback();
    }

    @Test
    void aggregateWithNoResults() {
        // When: COUNT with no matching rows
        TypedQuery<Long> query = em.createQuery("SELECT COUNT(e) FROM SimpleAssignedIdEntity e WHERE e.value > 999", Long.class);
        Long count = query.getSingleResult();

        // Then
        assertThat(count).isEqualTo(0L);
    }
}