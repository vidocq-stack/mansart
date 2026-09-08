package io.vidocq.mansart.persistence.tests;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Tests for JPQL CASE expressions (simple CASE, searched CASE, COALESCE, NULLIF).
 * Follows the pattern from JpqlStringFunctionQueryTest.
 */
class JpqlCaseExpressionQueryTest {

    private static final String PU_NAME = "test-pu";

    private EntityManagerFactory emf;
    private EntityManager em;
    private DataSource dataSource;

    @BeforeEach
    void beforeEach() throws SQLException {
        // Create in-memory H2 database
        org.h2.Driver.load();
        org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
        ds.setURL("jdbc:h2:mem:m6jp44;DB_CLOSE_DELAY=-1");
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
    void searchedCaseReturnsCorrectBranch() {
        // Given: entities with different values
        persistEntity(1L, "Entity1", 100);
        persistEntity(2L, "Entity2", 200);

        // When: searched CASE expression
        TypedQuery<String> query = em.createQuery(
            "SELECT CASE WHEN e.value > 150 THEN 'high' ELSE 'low' END FROM SimpleAssignedIdEntity e WHERE e.id = :id",
            String.class
        );
        query.setParameter("id", 1L);
        String result1 = query.getSingleResult();
        
        query.setParameter("id", 2L);
        String result2 = query.getSingleResult();

        // Then
        assertThat(result1).isEqualTo("low");
        assertThat(result2).isEqualTo("high");
    }

    @Test
    void simpleCaseReturnsCorrectBranch() {
        // Given: entities with specific values
        persistEntity(1L, "Entity1", 100);
        persistEntity(2L, "Entity2", 200);

        // When: simple CASE expression
        TypedQuery<String> query = em.createQuery(
            "SELECT CASE e.value WHEN 100 THEN 'one hundred' WHEN 200 THEN 'two hundred' ELSE 'other' END FROM SimpleAssignedIdEntity e WHERE e.id = :id",
            String.class
        );
        query.setParameter("id", 1L);
        String result1 = query.getSingleResult();
        
        query.setParameter("id", 2L);
        String result2 = query.getSingleResult();

        // Then
        assertThat(result1).isEqualTo("one hundred");
        assertThat(result2).isEqualTo("two hundred");
    }

    @Test
    void coalesceReturnsFirstNonNull() {
        // Given: entity with null name
        persistEntity(1L, null, 100);
        persistEntity(2L, "NonNullName", 200);

        // When: COALESCE with null name
        TypedQuery<String> query1 = em.createQuery(
            "SELECT COALESCE(e.name, 'default') FROM SimpleAssignedIdEntity e WHERE e.id = 1",
            String.class
        );
        String result1 = query1.getSingleResult();
        
        // When: COALESCE with non-null name
        TypedQuery<String> query2 = em.createQuery(
            "SELECT COALESCE(e.name, 'default') FROM SimpleAssignedIdEntity e WHERE e.id = 2",
            String.class
        );
        String result2 = query2.getSingleResult();

        // Then
        assertThat(result1).isEqualTo("default");
        assertThat(result2).isEqualTo("NonNullName");
    }

    @Test
    void nullifReturnsNullWhenEqual() {
        // Given: entity with value 100
        persistEntity(1L, "Entity1", 100);
        persistEntity(2L, "Entity2", 200);

        // When: NULLIF with equal values
        TypedQuery<Integer> query1 = em.createQuery(
            "SELECT NULLIF(e.value, 100) FROM SimpleAssignedIdEntity e WHERE e.id = 1",
            Integer.class
        );
        Integer result1 = query1.getSingleResult();
        
        // When: NULLIF with different values
        TypedQuery<Integer> query2 = em.createQuery(
            "SELECT NULLIF(e.value, 200) FROM SimpleAssignedIdEntity e WHERE e.id = 2",
            Integer.class
        );
        Integer result2 = query2.getSingleResult();

        // Then
        assertThat(result1).isNull();
        assertThat(result2).isEqualTo(200);
    }

    @Test
    void caseInWhereClause() {
        // Given: entities with different values
        persistEntity(1L, "Low1", 100);
        persistEntity(2L, "High1", 200);
        persistEntity(3L, "High2", 300);

        // When: CASE expression in WHERE clause
        TypedQuery<String> query = em.createQuery(
            "SELECT e.name FROM SimpleAssignedIdEntity e WHERE CASE WHEN e.value > 150 THEN 'high' ELSE 'low' END = 'high'",
            String.class
        );
        List<String> results = query.getResultList();

        // Then
        assertThat(results).containsExactlyInAnyOrder("High1", "High2");
    }

    @Test
    void searchedCaseWithoutElse() {
        // Given: entity with value 100
        persistEntity(1L, "Entity1", 100);

        // When: searched CASE without ELSE (should default to NULL)
        TypedQuery<String> query = em.createQuery(
            "SELECT CASE WHEN e.value > 150 THEN 'high' END FROM SimpleAssignedIdEntity e WHERE e.id = :id",
            String.class
        );
        query.setParameter("id", 1L);
        String result = query.getSingleResult();

        // Then
        assertThat(result).isNull();
    }

    /**
     * Helper to persist a test entity
     */
    private void persistEntity(Long id, String name, Integer value) {
        em.getTransaction().begin();
        SimpleAssignedIdEntity entity = new SimpleAssignedIdEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setValue(value);
        em.persist(entity);
        em.flush();
        em.getTransaction().commit();
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
        seed.setName("seed");
        seed.setValue(0);
        em.persist(seed);
        em.flush();
        em.remove(seed);
        em.flush();
        tx.commit();
    }
}
