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
 * Tests for JPQL string functions (CONCAT, SUBSTRING, TRIM, LOWER, UPPER, LENGTH, LOCATE).
 * Follows the pattern from JpqlAggregateQueryTest.
 */
class JpqlStringFunctionQueryTest {

    private static final String PU_NAME = "test-pu";

    private EntityManagerFactory emf;
    private EntityManager em;
    private DataSource dataSource;

    @BeforeEach
    void beforeEach() throws SQLException {
        // Create in-memory H2 database
        org.h2.Driver.load();
        org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
        ds.setURL("jdbc:h2:mem:m6jp42;DB_CLOSE_DELAY=-1");
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
    void upperReturnsUppercaseName() {
        // Given: entity with name "Alice"
        persistEntity(1L, "Alice", 100);

        // When: UPPER function
        TypedQuery<String> query = em.createQuery("SELECT UPPER(e.name) FROM SimpleAssignedIdEntity e WHERE e.id = 1", String.class);
        String result = query.getSingleResult();

        // Then
        assertThat(result).isEqualTo("ALICE");
    }

    @Test
    void lowerReturnsLowercaseName() {
        // Given: entity with name "Bob"
        persistEntity(2L, "Bob", 200);

        // When: LOWER function
        TypedQuery<String> query = em.createQuery("SELECT LOWER(e.name) FROM SimpleAssignedIdEntity e WHERE e.id = 2", String.class);
        String result = query.getSingleResult();

        // Then
        assertThat(result).isEqualTo("bob");
    }

    @Test
    void lengthReturnsNameLength() {
        // Given: entity with name "Charlie" (7 chars)
        persistEntity(3L, "Charlie", 300);

        // When: LENGTH function
        TypedQuery<Integer> query = em.createQuery("SELECT LENGTH(e.name) FROM SimpleAssignedIdEntity e WHERE e.id = 3", Integer.class);
        Integer result = query.getSingleResult();

        // Then
        assertThat(result).isEqualTo(7);
    }

    @Test
    void concatReturnsConcatenatedString() {
        // Given: entity with name "Alice"
        persistEntity(1L, "Alice", 100);

        // When: CONCAT function
        TypedQuery<String> query = em.createQuery("SELECT CONCAT(e.name, e.name) FROM SimpleAssignedIdEntity e WHERE e.id = 1", String.class);
        String result = query.getSingleResult();

        // Then
        assertThat(result).isEqualTo("AliceAlice");
    }

    @Test
    void substringReturnsSubstring() {
        // Given: entity with name "Alice"
        persistEntity(1L, "Alice", 100);

        // When: SUBSTRING function (1-indexed, first 3 chars)
        TypedQuery<String> query = em.createQuery("SELECT SUBSTRING(e.name, 1, 3) FROM SimpleAssignedIdEntity e WHERE e.id = 1", String.class);
        String result = query.getSingleResult();

        // Then
        assertThat(result).isEqualTo("Ali");
    }

    @Test
    void trimReturnsTrimmedString() {
        // Given: entity with name "Alice" (no leading/trailing spaces)
        persistEntity(1L, "Alice", 100);

        // When: TRIM function
        TypedQuery<String> query = em.createQuery("SELECT TRIM(e.name) FROM SimpleAssignedIdEntity e WHERE e.id = 1", String.class);
        String result = query.getSingleResult();

        // Then
        assertThat(result).isEqualTo("Alice");
    }

    @Test
    void locateReturnsPosition() {
        // Given: entity with name "Alice" (contains 'i' at position 3)
        persistEntity(1L, "Alice", 100);

        // When: LOCATE function
        TypedQuery<Integer> query = em.createQuery("SELECT LOCATE('i', e.name) FROM SimpleAssignedIdEntity e WHERE e.id = 1", Integer.class);
        Integer result = query.getSingleResult();

        // Then
        assertThat(result).isEqualTo(3);
    }

    @Test
    void indexThrowsUnsupportedOperationException() {
        // Given: entity exists
        persistEntity(1L, "Alice", 100);

        // When/Then: INDEX function should throw at query creation time
        assertThatThrownBy(() -> em.createQuery("SELECT INDEX(e.name) FROM SimpleAssignedIdEntity e", Integer.class))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("not implemented: INDEX");
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
        seed.setName("__seed__");
        seed.setValue(0);
        em.persist(seed);
        em.flush();
        em.remove(seed);
        em.flush();
        tx.rollback();
    }
}
