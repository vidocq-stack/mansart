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
import jakarta.persistence.TypedQuery;

import io.vidocq.mansart.persistence.tests.model.SimpleAssignedIdEntity;

import static org.assertj.core.api.Assertions.assertThat;

class JpqlWhereExpressionTest {

    private static final String DB_URL = "jdbc:h2:mem:m6jp37;DB_CLOSE_DELAY=-1";
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
    void whereBetweenFiltersByRange() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200),
            entity(3, "Charlie", 300),
            entity(4, "Diana", 400)
        );
        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.value BETWEEN 150 AND 350",
            SimpleAssignedIdEntity.class
        );
        List<SimpleAssignedIdEntity> result = query.getResultList();
        assertThat(result).hasSize(2);
        assertThat(result).extracting(SimpleAssignedIdEntity::getName)
            .containsExactlyInAnyOrder("Bob", "Charlie");
    }

    @Test
    void whereNotBetweenExcludesRange() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200),
            entity(3, "Charlie", 300),
            entity(4, "Diana", 400)
        );
        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.value NOT BETWEEN 150 AND 350",
            SimpleAssignedIdEntity.class
        );
        List<SimpleAssignedIdEntity> result = query.getResultList();
        assertThat(result).hasSize(2);
        assertThat(result).extracting(SimpleAssignedIdEntity::getName)
            .containsExactlyInAnyOrder("Alice", "Diana");
    }

    @Test
    void whereBetweenWithParameters() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200),
            entity(3, "Charlie", 300)
        );
        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.value BETWEEN :low AND :high",
            SimpleAssignedIdEntity.class
        );
        query.setParameter("low", 150);
        query.setParameter("high", 250);
        List<SimpleAssignedIdEntity> result = query.getResultList();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Bob");
    }

    @Test
    void whereInWithLiteralsFiltersByList() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200),
            entity(3, "Charlie", 300),
            entity(4, "Diana", 400)
        );
        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.name IN ('Alice', 'Charlie', 'Diana')",
            SimpleAssignedIdEntity.class
        );
        List<SimpleAssignedIdEntity> result = query.getResultList();
        assertThat(result).hasSize(3);
        assertThat(result).extracting(SimpleAssignedIdEntity::getName)
            .containsExactlyInAnyOrder("Alice", "Charlie", "Diana");
    }

    @Test
    void whereNotInExcludesList() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200),
            entity(3, "Charlie", 300)
        );
        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.name NOT IN ('Alice', 'Charlie')",
            SimpleAssignedIdEntity.class
        );
        List<SimpleAssignedIdEntity> result = query.getResultList();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Bob");
    }

    @Test
    void whereInWithParameters() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200),
            entity(3, "Charlie", 300)
        );
        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.value IN (?1, ?2)",
            SimpleAssignedIdEntity.class
        );
        query.setParameter(1, 100);
        query.setParameter(2, 300);
        List<SimpleAssignedIdEntity> result = query.getResultList();
        assertThat(result).hasSize(2);
        assertThat(result).extracting(SimpleAssignedIdEntity::getName)
            .containsExactlyInAnyOrder("Alice", "Charlie");
    }

    @Test
    void whereLikeWithWildcardMatchesPattern() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Alicia", 200),
            entity(3, "Bob", 300),
            entity(4, "Charlie", 400)
        );
        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.name LIKE 'Ali%'",
            SimpleAssignedIdEntity.class
        );
        List<SimpleAssignedIdEntity> result = query.getResultList();
        assertThat(result).hasSize(2);
        assertThat(result).extracting(SimpleAssignedIdEntity::getName)
            .containsExactlyInAnyOrder("Alice", "Alicia");
    }

    @Test
    void whereLikeWithUnderscoreWildcard() {
        persist(
            entity(1, "Al", 100),
            entity(2, "Ali", 200),
            entity(3, "Alice", 300)
        );
        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.name LIKE 'Al_'",
            SimpleAssignedIdEntity.class
        );
        List<SimpleAssignedIdEntity> result = query.getResultList();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Ali");
    }

    @Test
    void whereLikeWithParameter() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200),
            entity(3, "Charlie", 300)
        );
        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.name LIKE :pattern",
            SimpleAssignedIdEntity.class
        );
        query.setParameter("pattern", "B%");
        List<SimpleAssignedIdEntity> result = query.getResultList();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Bob");
    }

    @Test
    void whereNotLikeExcludesPattern() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Alicia", 200),
            entity(3, "Bob", 300)
        );
        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.name NOT LIKE 'Ali%'",
            SimpleAssignedIdEntity.class
        );
        List<SimpleAssignedIdEntity> result = query.getResultList();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Bob");
    }

    @Test
    void whereOrMatchesEitherCondition() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200),
            entity(3, "Charlie", 300)
        );
        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.name = 'Alice' OR e.value = 300",
            SimpleAssignedIdEntity.class
        );
        List<SimpleAssignedIdEntity> result = query.getResultList();
        assertThat(result).hasSize(2);
        assertThat(result).extracting(SimpleAssignedIdEntity::getName)
            .containsExactlyInAnyOrder("Alice", "Charlie");
    }

    @Test
    void whereNotNegatesCondition() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200),
            entity(3, "Charlie", 300)
        );
        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE NOT e.name = 'Bob'",
            SimpleAssignedIdEntity.class
        );
        List<SimpleAssignedIdEntity> result = query.getResultList();
        assertThat(result).hasSize(2);
        assertThat(result).extracting(SimpleAssignedIdEntity::getName)
            .containsExactlyInAnyOrder("Alice", "Charlie");
    }

    @Test
    void whereAndOrWithParentheses() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Alice", 300),
            entity(3, "Bob", 200),
            entity(4, "Charlie", 300)
        );
        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE (e.name = 'Alice' AND e.value = 100) OR e.value = 300",
            SimpleAssignedIdEntity.class
        );
        List<SimpleAssignedIdEntity> result = query.getResultList();
        assertThat(result).hasSize(3);
        assertThat(result).extracting(SimpleAssignedIdEntity::getId)
            .containsExactlyInAnyOrder(1L, 2L, 4L);
    }

    @Test
    void whereIsNullMatchesNullValues() {
        persist(
            entity(1, "Alice", 100),
            entity(2, null, 200),
            entity(3, "Charlie", 300)
        );
        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.name IS NULL",
            SimpleAssignedIdEntity.class
        );
        List<SimpleAssignedIdEntity> result = query.getResultList();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(2L);
    }

    @Test
    void whereIsNotNullExcludesNullValues() {
        persist(
            entity(1, "Alice", 100),
            entity(2, null, 200),
            entity(3, "Charlie", 300)
        );
        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.name IS NOT NULL",
            SimpleAssignedIdEntity.class
        );
        List<SimpleAssignedIdEntity> result = query.getResultList();
        assertThat(result).hasSize(2);
        assertThat(result).extracting(SimpleAssignedIdEntity::getName)
            .containsExactlyInAnyOrder("Alice", "Charlie");
    }

    @Test
    void whereGreaterThanAndLessThanCombined() {
        persist(
            entity(1, "Alice", 100),
            entity(2, "Bob", 200),
            entity(3, "Charlie", 300),
            entity(4, "Diana", 400)
        );
        TypedQuery<SimpleAssignedIdEntity> query = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.value > 100 AND e.value < 400",
            SimpleAssignedIdEntity.class
        );
        List<SimpleAssignedIdEntity> result = query.getResultList();
        assertThat(result).hasSize(2);
        assertThat(result).extracting(SimpleAssignedIdEntity::getName)
            .containsExactlyInAnyOrder("Bob", "Charlie");
    }
}
