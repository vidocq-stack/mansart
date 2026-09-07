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
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.Persistence;
import jakarta.persistence.TypedQuery;

import io.vidocq.mansart.persistence.tests.model.SimpleAssignedIdEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JpqlSelectQueryTest {

    private static final String DB_URL = "jdbc:h2:mem:m6jp35;DB_CLOSE_DELAY=-1";
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
    void selectAllReturnsAllPersistedEntities() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200),
            entity(3, "Charlie", 300)
        );

        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e",
            SimpleAssignedIdEntity.class
        );
        List<SimpleAssignedIdEntity> result = query.getResultList();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(SimpleAssignedIdEntity::getName)
            .containsExactlyInAnyOrder("Alice", "Bob", "Charlie");
    }

    @Test
    void selectWithWhereClauseFiltersResults() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200),
            entity(3, "Charlie", 300)
        );

        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.name = 'Bob'",
            SimpleAssignedIdEntity.class
        );
        List<SimpleAssignedIdEntity> result = query.getResultList();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Bob");
        assertThat(result.get(0).getValue()).isEqualTo(200);
    }

    @Test
    void selectWithWhereAndNamedParameter() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200),
            entity(3, "Charlie", 300)
        );

        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.name = :name",
            SimpleAssignedIdEntity.class
        );
        query.setParameter("name", "Charlie");
        List<SimpleAssignedIdEntity> result = query.getResultList();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Charlie");
    }

    @Test
    void selectWithWhereAndPositionalParameter() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200),
            entity(3, "Charlie", 300)
        );

        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.value = ?1",
            SimpleAssignedIdEntity.class
        );
        query.setParameter(1, 200);
        List<SimpleAssignedIdEntity> result = query.getResultList();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Bob");
    }

    @Test
    void selectWithOrderByReturnsSortedResults() {
        persist(
            entity(1, "Charlie", 300),
            entity(2, "Alice", 100),
            entity(3, "Bob", 200)
        );

        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e ORDER BY e.name ASC",
            SimpleAssignedIdEntity.class
        );
        List<SimpleAssignedIdEntity> result = query.getResultList();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(SimpleAssignedIdEntity::getName)
            .containsExactly("Alice", "Bob", "Charlie");
    }

    @Test
    void getSingleResultReturnsOneEntity() {
        persist(entity(1, "Alice", 100));

        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.id = 1",
            SimpleAssignedIdEntity.class
        );
        SimpleAssignedIdEntity result = query.getSingleResult();

        assertThat(result.getName()).isEqualTo("Alice");
    }

    @Test
    void getSingleResultThrowsNoResultException() {
        persist(entity(1, "Alice", 100));

        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.id = 999",
            SimpleAssignedIdEntity.class
        );

        assertThatThrownBy(() -> query.getSingleResult())
            .isInstanceOf(NoResultException.class);
    }

    @Test
    void getSingleResultThrowsNonUniqueResultException() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200)
        );

        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e",
            SimpleAssignedIdEntity.class
        );

        assertThatThrownBy(() -> query.getSingleResult())
            .isInstanceOf(NonUniqueResultException.class);
    }

    @Test
    void getSingleResultOrNullReturnsNullWhenEmpty() {
        persist(entity(1, "Alice", 100));

        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.id = 999",
            SimpleAssignedIdEntity.class
        );
        SimpleAssignedIdEntity result = query.getSingleResultOrNull();

        assertThat(result).isNull();
    }

    @Test
    void getResultListReturnsEmptyWhenNoMatches() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200)
        );

        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.name = 'Nonexistent'",
            SimpleAssignedIdEntity.class
        );
        List<SimpleAssignedIdEntity> result = query.getResultList();

        assertThat(result).isEmpty();
    }

    @Test
    void createQueryWithoutResultClassReturnsQuery() {
        persist(entity(1, "Alice", 100));

        var query = em.createQuery("SELECT e FROM SimpleAssignedIdEntity e");
        @SuppressWarnings("unchecked")
        List<SimpleAssignedIdEntity> result = query.getResultList();

        assertThat(result).hasSize(1);
    }

    @Test
    void selectWithMultipleWhereConditions() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Alice", 200),
            entity(3, "Bob", 100),
            entity(4, "Bob", 200)
        );

        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.name = :name AND e.value = :val",
            SimpleAssignedIdEntity.class
        );
        query.setParameter("name", "Alice");
        query.setParameter("val", 200);
        List<SimpleAssignedIdEntity> result = query.getResultList();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(2);
    }
}
