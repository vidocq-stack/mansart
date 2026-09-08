package io.vidocq.mansart.persistence.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Parameter;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;
import jakarta.persistence.TemporalType;
import jakarta.persistence.TypedQuery;

import io.vidocq.mansart.persistence.tests.model.SimpleAssignedIdEntity;

/**
 * Integration tests for named and positional parameter support in JPQL queries.
 */
class JpqlParameterTest {

    private static final String PU_NAME = "test-pu";
    private static final String JDBC_URL = "jdbc:h2:mem:m6jp40;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private EntityManagerFactory emf;
    private EntityManager em;

    @BeforeEach
    void beforeEach() throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(JDBC_URL);
        dataSource.setUser(USER);
        dataSource.setPassword(PASSWORD);
        emf = Persistence.createEntityManagerFactory(PU_NAME, Map.of(
            "jakarta.persistence.jtaDataSource", dataSource
        ));
        em = emf.createEntityManager();
        createSchemaTables();
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
    }

    private void createSchemaTables() throws SQLException {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS \"simple_assigned_id_entities\"");
            stmt.execute("CREATE TABLE \"simple_assigned_id_entities\" (\"id\" BIGINT PRIMARY KEY, \"name\" VARCHAR(255), \"value\" INTEGER)");
        }
    }

    /**
     * Triggers entity name registration by persisting and flushing a seed
     * entity. The persistence provider registers entity names as a
     * side-effect of the first persist()+flush() cycle; without this, queries
     * that run before any persist() fail with "Unknown entity name".
     */
    private void registerEntityName() {
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        SimpleAssignedIdEntity seed = new SimpleAssignedIdEntity();
        seed.setId(0L);
        seed.setName("__seed__");
        seed.setValue(0);
        em.persist(seed);
        em.flush();
        em.remove(seed);
        em.flush();
        tx.commit();
        em.clear();
    }

    @Test
    void namedParameterSelect() {
        persist(
            entity(1L, "Alice", 1),
            entity(2L, "Bob", 2),
            entity(3L, "Alice", 3)
        );

        TypedQuery<SimpleAssignedIdEntity> q = em.createQuery("SELECT e FROM SimpleAssignedIdEntity e WHERE e.name = :name", SimpleAssignedIdEntity.class);
        q.setParameter("name", "Alice");
        List<SimpleAssignedIdEntity> result = q.getResultList();

        assertThat(result).extracting(SimpleAssignedIdEntity::getName).containsExactly("Alice", "Alice");
        assertThat(result).extracting(SimpleAssignedIdEntity::getValue).containsExactly(1, 3);
    }

    @Test
    void positionalParameterSelect() {
        persist(
            entity(1L, "Alice", 1),
            entity(2L, "Bob", 42),
            entity(3L, "Carol", 42)
        );

        TypedQuery<SimpleAssignedIdEntity> q = em.createQuery("SELECT e FROM SimpleAssignedIdEntity e WHERE e.value = ?1", SimpleAssignedIdEntity.class);
        q.setParameter(1, 42);
        List<SimpleAssignedIdEntity> result = q.getResultList();

        assertThat(result).extracting(SimpleAssignedIdEntity::getName).containsExactly("Bob", "Carol");
    }

    @Test
    void namedParameterUpdate() {
        persist(
            entity(1L, "Alice", 1),
            entity(2L, "Bob", 2)
        );

        Query q = em.createQuery("UPDATE SimpleAssignedIdEntity e SET e.value = :newValue WHERE e.name = :name");
        q.setParameter("newValue", 100);
        q.setParameter("name", "Alice");
        int updated = q.executeUpdate();

        assertThat(updated).isEqualTo(1);

        TypedQuery<SimpleAssignedIdEntity> q2 = em.createQuery("SELECT e FROM SimpleAssignedIdEntity e WHERE e.name = :name", SimpleAssignedIdEntity.class);
        q2.setParameter("name", "Alice");
        SimpleAssignedIdEntity alice = q2.getSingleResult();
        assertThat(alice.getValue()).isEqualTo(100);
    }

    @Test
    void positionalParameterDelete() {
        persist(
            entity(1L, "Alice", 1),
            entity(2L, "Bob", 42),
            entity(3L, "Carol", 42)
        );

        Query q = em.createQuery("DELETE FROM SimpleAssignedIdEntity e WHERE e.value = ?1");
        q.setParameter(1, 42);
        int deleted = q.executeUpdate();

        assertThat(deleted).isEqualTo(2);

        TypedQuery<SimpleAssignedIdEntity> q2 = em.createQuery("SELECT e FROM SimpleAssignedIdEntity e", SimpleAssignedIdEntity.class);
        List<SimpleAssignedIdEntity> remaining = q2.getResultList();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getName()).isEqualTo("Alice");
    }

    @Test
    void parameterMetadataNamed() {
        TypedQuery<SimpleAssignedIdEntity> q = em.createQuery("SELECT e FROM SimpleAssignedIdEntity e WHERE e.name = :name AND e.value = :val", SimpleAssignedIdEntity.class);

        var params = q.getParameters();
        assertThat(params).hasSize(2);

        Parameter<?> nameParam = q.getParameter("name");
        assertThat(nameParam).isNotNull();
        assertThat(nameParam.getName()).isEqualTo("name");
        assertThat(nameParam.getPosition()).isNull();

        Parameter<?> valParam = q.getParameter("val");
        assertThat(valParam).isNotNull();
        assertThat(valParam.getName()).isEqualTo("val");
        assertThat(valParam.getPosition()).isNull();

        assertThat(q.isBound(nameParam)).isFalse();
        assertThat(q.isBound(valParam)).isFalse();

        q.setParameter("name", "Alice");
        q.setParameter("val", 1);

        assertThat(q.isBound(nameParam)).isTrue();
        assertThat(q.isBound(valParam)).isTrue();

        assertThat(q.getParameterValue("name")).isEqualTo("Alice");
        assertThat(q.getParameterValue("val")).isEqualTo(1);

        Parameter<String> nameParamTyped = q.getParameter("name", String.class);
        assertThat(nameParamTyped.getParameterType()).isEqualTo(String.class);
        assertThat(q.getParameterValue(nameParamTyped)).isEqualTo("Alice");

        Parameter<Integer> valParamTyped = q.getParameter("val", Integer.class);
        assertThat(valParamTyped.getParameterType()).isEqualTo(Integer.class);
        assertThat(q.getParameterValue(valParamTyped)).isEqualTo(1);
    }

    @Test
    void parameterMetadataPositional() {
        Query q = em.createQuery("UPDATE SimpleAssignedIdEntity e SET e.value = ?2 WHERE e.name = ?1");

        var params = q.getParameters();
        assertThat(params).hasSize(2);

        Parameter<?> p1 = q.getParameter(1);
        assertThat(p1).isNotNull();
        assertThat(p1.getPosition()).isEqualTo(1);
        assertThat(p1.getName()).isNull();

        Parameter<?> p2 = q.getParameter(2);
        assertThat(p2).isNotNull();
        assertThat(p2.getPosition()).isEqualTo(2);
        assertThat(p2.getName()).isNull();

        assertThat(q.isBound(p1)).isFalse();
        assertThat(q.isBound(p2)).isFalse();

        q.setParameter(1, "Alice");
        q.setParameter(2, 100);

        assertThat(q.isBound(p1)).isTrue();
        assertThat(q.isBound(p2)).isTrue();

        assertThat(q.getParameterValue(1)).isEqualTo("Alice");
        assertThat(q.getParameterValue(2)).isEqualTo(100);

        Parameter<String> p1Typed = q.getParameter(1, String.class);
        assertThat(p1Typed.getParameterType()).isEqualTo(String.class);
        assertThat(q.getParameterValue(p1Typed)).isEqualTo("Alice");

        Parameter<Integer> p2Typed = q.getParameter(2, Integer.class);
        assertThat(p2Typed.getParameterType()).isEqualTo(Integer.class);
        assertThat(q.getParameterValue(p2Typed)).isEqualTo(100);
    }

    @Test
    void setParameterWithTemporalType() {
        TypedQuery<SimpleAssignedIdEntity> q = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.name = :name", SimpleAssignedIdEntity.class);

        java.util.Date now = new java.util.Date();
        q.setParameter("name", now, TemporalType.DATE);

        assertThat(q.isBound(q.getParameter("name"))).isTrue();
        assertThat(q.getParameterValue("name")).isInstanceOf(java.sql.Date.class);
    }

    @Test
    void setParameterWithTemporalTypePositional() {
        Query q = em.createQuery("UPDATE SimpleAssignedIdEntity e SET e.value = ?1");

        java.util.Calendar cal = java.util.Calendar.getInstance();
        q.setParameter(1, cal, TemporalType.TIMESTAMP);

        assertThat(q.isBound(q.getParameter(1))).isTrue();
        assertThat(q.getParameterValue(1)).isInstanceOf(java.sql.Timestamp.class);
    }

    @Test
    void getParameterNotDeclaredNamed() {
        TypedQuery<SimpleAssignedIdEntity> q = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.name = :name", SimpleAssignedIdEntity.class);
        assertThatThrownBy(() -> q.getParameter("unknown"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unknown");
    }

    @Test
    void getParameterNotDeclaredPositional() {
        Query q = em.createQuery("UPDATE SimpleAssignedIdEntity e SET e.value = ?1");
        assertThatThrownBy(() -> q.getParameter(2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("2");
    }

    @Test
    void getParameterValueNotBound() {
        TypedQuery<SimpleAssignedIdEntity> q = em.createQuery(
            "SELECT e FROM SimpleAssignedIdEntity e WHERE e.name = :name", SimpleAssignedIdEntity.class);
        Parameter<?> p = q.getParameter("name");
        assertThatThrownBy(() -> q.getParameterValue(p))
            .isInstanceOf(IllegalStateException.class);
    }

    private SimpleAssignedIdEntity entity(long id, String name, int value) {
        SimpleAssignedIdEntity e = new SimpleAssignedIdEntity();
        e.setId(id);
        e.setName(name);
        e.setValue(value);
        return e;
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
}
