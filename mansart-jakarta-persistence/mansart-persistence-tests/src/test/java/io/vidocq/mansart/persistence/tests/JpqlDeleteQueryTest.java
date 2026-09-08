package io.vidocq.mansart.persistence.tests;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;

import io.vidocq.mansart.persistence.tests.model.SimpleAssignedIdEntity;

import static org.assertj.core.api.Assertions.assertThat;

class JpqlDeleteQueryTest {

    private static final String DB_URL = "jdbc:h2:mem:m6jp39;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private EntityManagerFactory emf;
    private EntityManager em;

    @BeforeEach
    void setUp() throws SQLException {
        org.h2.jdbcx.JdbcDataSource dataSource = new org.h2.jdbcx.JdbcDataSource();
        dataSource.setURL(DB_URL);
        dataSource.setUser(USER);
        dataSource.setPassword(PASSWORD);

        emf = Persistence.createEntityManagerFactory(
                "test-pu",
                Map.of("jakarta.persistence.jtaDataSource", dataSource)
        );
        em = emf.createEntityManager();
        createSchemaTables();
    }

    @AfterEach
    void tearDown() {
        if (em != null && em.isOpen()) {
            em.close();
        }
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    private void createSchemaTables() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS \"simple_assigned_id_entities\"");
            stmt.execute("CREATE TABLE \"simple_assigned_id_entities\" (\"id\" BIGINT PRIMARY KEY, \"name\" VARCHAR(255), \"value\" INTEGER)");
        }
    }

    private void persist(SimpleAssignedIdEntity... entities) {
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        for (SimpleAssignedIdEntity e : entities) {
            em.persist(e);
        }
        em.flush();
        tx.commit();
    }

    private SimpleAssignedIdEntity entity(long id, String name, int value) {
        SimpleAssignedIdEntity e = new SimpleAssignedIdEntity();
        e.setId(id);
        e.setName(name);
        e.setValue(value);
        return e;
    }

    @Test
    void testDeleteAll() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200),
            entity(3, "Charlie", 300)
        );

        Query query = em.createQuery("DELETE FROM SimpleAssignedIdEntity");
        int deleted = query.executeUpdate();

        assertThat(deleted).isEqualTo(3);

        em.clear();

        // Verify table is empty
        int remaining = em.createQuery("SELECT e FROM SimpleAssignedIdEntity e", SimpleAssignedIdEntity.class)
            .getResultList().size();
        assertThat(remaining).isEqualTo(0);
    }

    @Test
    void testDeleteWithWhere() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200),
            entity(3, "Charlie", 300)
        );

        Query query = em.createQuery("DELETE FROM SimpleAssignedIdEntity e WHERE e.name = 'Alice'");
        int deleted = query.executeUpdate();

        assertThat(deleted).isEqualTo(1);

        em.clear();

        // Verify only Alice is deleted
        int remaining = em.createQuery("SELECT e FROM SimpleAssignedIdEntity e", SimpleAssignedIdEntity.class)
            .getResultList().size();
        assertThat(remaining).isEqualTo(2);

        SimpleAssignedIdEntity bob = em.find(SimpleAssignedIdEntity.class, 2L);
        assertThat(bob).isNotNull();
        assertThat(bob.getName()).isEqualTo("Bob");

        SimpleAssignedIdEntity charlie = em.find(SimpleAssignedIdEntity.class, 3L);
        assertThat(charlie).isNotNull();
        assertThat(charlie.getName()).isEqualTo("Charlie");
    }

    @Test
    void testDeleteWithNamedParameter() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200),
            entity(3, "Charlie", 300)
        );

        Query query = em.createQuery("DELETE FROM SimpleAssignedIdEntity e WHERE e.name = :name");
        query.setParameter("name", "Bob");
        int deleted = query.executeUpdate();

        assertThat(deleted).isEqualTo(1);

        em.clear();

        // Verify only Bob is deleted
        SimpleAssignedIdEntity bob = em.find(SimpleAssignedIdEntity.class, 2L);
        assertThat(bob).isNull();

        SimpleAssignedIdEntity alice = em.find(SimpleAssignedIdEntity.class, 1L);
        assertThat(alice).isNotNull();
        assertThat(alice.getName()).isEqualTo("Alice");
    }

    @Test
    void testDeleteWithPositionalParameter() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200),
            entity(3, "Charlie", 300)
        );

        Query query = em.createQuery("DELETE FROM SimpleAssignedIdEntity e WHERE e.name = ?1");
        query.setParameter(1, "Charlie");
        int deleted = query.executeUpdate();

        assertThat(deleted).isEqualTo(1);

        em.clear();

        // Verify only Charlie is deleted
        SimpleAssignedIdEntity charlie = em.find(SimpleAssignedIdEntity.class, 3L);
        assertThat(charlie).isNull();

        SimpleAssignedIdEntity alice = em.find(SimpleAssignedIdEntity.class, 1L);
        assertThat(alice).isNotNull();
        assertThat(alice.getName()).isEqualTo("Alice");
    }

    @Test
    void testDeleteReturnsZeroWhenNoMatch() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200)
        );

        Query query = em.createQuery("DELETE FROM SimpleAssignedIdEntity e WHERE e.name = 'NonExistent'");
        int deleted = query.executeUpdate();

        assertThat(deleted).isEqualTo(0);

        em.clear();

        // Verify no rows were deleted
        int remaining = em.createQuery("SELECT e FROM SimpleAssignedIdEntity e", SimpleAssignedIdEntity.class)
            .getResultList().size();
        assertThat(remaining).isEqualTo(2);
    }
}
