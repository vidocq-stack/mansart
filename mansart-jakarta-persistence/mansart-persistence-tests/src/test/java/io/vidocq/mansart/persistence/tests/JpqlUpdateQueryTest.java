package io.vidocq.mansart.persistence.tests;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
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

class JpqlUpdateQueryTest {

    private static final String DB_URL = "jdbc:h2:mem:m6jp38;DB_CLOSE_DELAY=-1";
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
    void executeUpdateReturnsRowCount() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200),
            entity(3, "Charlie", 300)
        );

        Query query = em.createQuery("UPDATE SimpleAssignedIdEntity e SET e.value = 999");
        int updated = query.executeUpdate();

        assertThat(updated).isEqualTo(3);
    }

    @Test
    void executeUpdateWithWhereClause() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200),
            entity(3, "Charlie", 300)
        );

        Query query = em.createQuery("UPDATE SimpleAssignedIdEntity e SET e.value = 0 WHERE e.name = 'Alice'");
        int updated = query.executeUpdate();

        assertThat(updated).isEqualTo(1);

        em.clear();

        SimpleAssignedIdEntity alice = em.find(SimpleAssignedIdEntity.class, 1L);
        assertThat(alice).isNotNull();
        assertThat(alice.getValue()).isEqualTo(0);

        SimpleAssignedIdEntity bob = em.find(SimpleAssignedIdEntity.class, 2L);
        assertThat(bob).isNotNull();
        assertThat(bob.getValue()).isEqualTo(200);
    }

    @Test
    void executeUpdateWithNamedParameter() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200)
        );

        Query query = em.createQuery("UPDATE SimpleAssignedIdEntity e SET e.value = :val WHERE e.name = :name");
        query.setParameter("val", 42);
        query.setParameter("name", "Alice");
        int updated = query.executeUpdate();

        assertThat(updated).isEqualTo(1);

        em.clear();

        SimpleAssignedIdEntity alice = em.find(SimpleAssignedIdEntity.class, 1L);
        assertThat(alice).isNotNull();
        assertThat(alice.getValue()).isEqualTo(42);
    }

    @Test
    void executeUpdateWithPositionalParameter() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200)
        );

        Query query = em.createQuery("UPDATE SimpleAssignedIdEntity e SET e.value = ?1 WHERE e.name = ?2");
        query.setParameter(1, 777);
        query.setParameter(2, "Bob");
        int updated = query.executeUpdate();

        assertThat(updated).isEqualTo(1);
    }

    @Test
    void executeUpdateWithLiteralValue() {
        persist(entity(1, "Alice", 100));

        Query query = em.createQuery("UPDATE SimpleAssignedIdEntity e SET e.value = 42 WHERE e.name = 'Alice'");
        int updated = query.executeUpdate();

        assertThat(updated).isEqualTo(1);

        em.clear();

        SimpleAssignedIdEntity alice = em.find(SimpleAssignedIdEntity.class, 1L);
        assertThat(alice).isNotNull();
        assertThat(alice.getValue()).isEqualTo(42);
    }
}
