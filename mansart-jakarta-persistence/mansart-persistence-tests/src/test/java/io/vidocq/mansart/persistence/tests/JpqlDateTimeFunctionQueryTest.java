package io.vidocq.mansart.persistence.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import javax.sql.DataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.TemporalType;
import jakarta.persistence.TypedQuery;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vidocq.mansart.persistence.tests.model.SimpleDateEntity;

/**
 * Tests for JPQL date/time functions (CURRENT_DATE, CURRENT_TIME, CURRENT_TIMESTAMP,
 * LOCAL_DATE, LOCAL_TIME, LOCAL_DATETIME, EXTRACT).
 */
class JpqlDateTimeFunctionQueryTest {

    private static final String PU_NAME = "test-pu";

    private EntityManagerFactory emf;
    private EntityManager em;
    private DataSource dataSource;

    @BeforeEach
    void beforeEach() throws Exception {
        // Create in-memory H2 database
        org.h2.Driver.load();
        org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
        ds.setURL("jdbc:h2:mem:m6jp43;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        dataSource = ds;

        try (Connection conn = dataSource.getConnection()) {
            // Drop and create schema (quote identifiers)
            conn.createStatement().execute("DROP TABLE IF EXISTS \"simple_date_entities\"");
            conn.createStatement().execute(
                "CREATE TABLE \"simple_date_entities\" (\"id\" BIGINT PRIMARY KEY, \"name\" VARCHAR(255), " +
                "\"local_date\" DATE, \"local_time\" TIME, \"local_date_time\" TIMESTAMP, \"legacy_date\" DATE)"
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
            conn.createStatement().execute("DROP TABLE IF EXISTS \"simple_date_entities\"");
        } catch (Exception e) {
            // Ignore drop errors
        }
    }

    @Test
    void currentDateReturnsSqlDate() {
        // When: CURRENT_DATE function
        TypedQuery<Date> query = em.createQuery("SELECT CURRENT_DATE FROM SimpleDateEntity e", Date.class);
        Date result = query.getSingleResult();

        // Then: result is a java.sql.Date
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(Date.class);
    }

    @Test
    void currentTimeReturnsSqlTime() {
        // When: CURRENT_TIME function
        TypedQuery<Time> query = em.createQuery("SELECT CURRENT_TIME FROM SimpleDateEntity e", Time.class);
        Time result = query.getSingleResult();

        // Then: result is a java.sql.Time
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(Time.class);
    }

    @Test
    void currentTimestampReturnsSqlTimestamp() {
        // When: CURRENT_TIMESTAMP function
        TypedQuery<Timestamp> query = em.createQuery("SELECT CURRENT_TIMESTAMP FROM SimpleDateEntity e", Timestamp.class);
        Timestamp result = query.getSingleResult();

        // Then: result is a java.sql.Timestamp
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(Timestamp.class);
    }

    @Test
    void localDateReturnsLocalDate() {
        // When: LOCAL_DATE function
        TypedQuery<LocalDate> query = em.createQuery("SELECT LOCAL_DATE FROM SimpleDateEntity e", LocalDate.class);
        LocalDate result = query.getSingleResult();

        // Then: result is a java.time.LocalDate
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(LocalDate.class);
    }

    @Test
    void localTimeReturnsLocalTime() {
        // When: LOCAL_TIME function
        TypedQuery<LocalTime> query = em.createQuery("SELECT LOCAL_TIME FROM SimpleDateEntity e", LocalTime.class);
        LocalTime result = query.getSingleResult();

        // Then: result is a java.time.LocalTime
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(LocalTime.class);
    }

    @Test
    void localDateTimeReturnsLocalDateTime() {
        // When: LOCAL_DATETIME function
        TypedQuery<LocalDateTime> query = em.createQuery("SELECT LOCAL_DATETIME FROM SimpleDateEntity e", LocalDateTime.class);
        LocalDateTime result = query.getSingleResult();

        // Then: result is a java.time.LocalDateTime
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(LocalDateTime.class);
    }

    @Test
    void extractYearFromLocalDate() {
        // Given: entity with a localDate
        persistEntity(1L, "test1", LocalDate.of(2023, 6, 15), null, null, null);

        // When: EXTRACT(YEAR FROM e.localDate)
        TypedQuery<Integer> query = em.createQuery("SELECT EXTRACT(YEAR FROM e.localDate) FROM SimpleDateEntity e WHERE e.id = 1", Integer.class);
        Integer result = query.getSingleResult();

        // Then
        assertThat(result).isEqualTo(2023);
    }

    @Test
    void extractMonthFromLocalDateTime() {
        // Given: entity with a localDateTime
        persistEntity(2L, "test2", null, null, LocalDateTime.of(2023, 6, 15, 14, 30, 0), null);

        // When: EXTRACT(MONTH FROM e.localDateTime)
        TypedQuery<Integer> query = em.createQuery("SELECT EXTRACT(MONTH FROM e.localDateTime) FROM SimpleDateEntity e WHERE e.id = 2", Integer.class);
        Integer result = query.getSingleResult();

        // Then
        assertThat(result).isEqualTo(6);
    }

    @Test
    void extractDayFromLocalDate() {
        // Given: entity with a localDate
        persistEntity(3L, "test3", LocalDate.of(2023, 6, 15), null, null, null);

        // When: EXTRACT(DAY FROM e.localDate)
        TypedQuery<Integer> query = em.createQuery("SELECT EXTRACT(DAY FROM e.localDate) FROM SimpleDateEntity e WHERE e.id = 3", Integer.class);
        Integer result = query.getSingleResult();

        // Then
        assertThat(result).isEqualTo(15);
    }

    @Test
    void extractHourFromLocalDateTime() {
        // Given: entity with a localDateTime
        persistEntity(4L, "test4", null, null, LocalDateTime.of(2023, 6, 15, 14, 30, 0), null);

        // When: EXTRACT(HOUR FROM e.localDateTime)
        TypedQuery<Integer> query = em.createQuery("SELECT EXTRACT(HOUR FROM e.localDateTime) FROM SimpleDateEntity e WHERE e.id = 4", Integer.class);
        Integer result = query.getSingleResult();

        // Then
        assertThat(result).isEqualTo(14);
    }

    @Test
    void extractMinuteFromLocalDateTime() {
        // Given: entity with a localDateTime
        persistEntity(5L, "test5", null, null, LocalDateTime.of(2023, 6, 15, 14, 30, 0), null);

        // When: EXTRACT(MINUTE FROM e.localDateTime)
        TypedQuery<Integer> query = em.createQuery("SELECT EXTRACT(MINUTE FROM e.localDateTime) FROM SimpleDateEntity e WHERE e.id = 5", Integer.class);
        Integer result = query.getSingleResult();

        // Then
        assertThat(result).isEqualTo(30);
    }

    @Test
    void extractSecondFromLocalDateTime() {
        // Given: entity with a localDateTime
        persistEntity(6L, "test6", null, null, LocalDateTime.of(2023, 6, 15, 14, 30, 45), null);

        // When: EXTRACT(SECOND FROM e.localDateTime)
        TypedQuery<Integer> query = em.createQuery("SELECT EXTRACT(SECOND FROM e.localDateTime) FROM SimpleDateEntity e WHERE e.id = 6", Integer.class);
        Integer result = query.getSingleResult();

        // Then
        assertThat(result).isEqualTo(45);
    }

    @Test
    void extractUnsupportedFieldThrows() {
        // Given: entity with a localDate
        persistEntity(7L, "test7", LocalDate.of(2023, 6, 15), null, null, null);

        // When: EXTRACT with unsupported field
        // Then: throws UnsupportedOperationException
        assertThatThrownBy(() ->
            em.createQuery("SELECT EXTRACT(WEEK FROM e.localDate) FROM SimpleDateEntity e WHERE e.id = 7", Integer.class)
              .getSingleResult()
        ).isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * Helper to persist a test entity
     */
    private void persistEntity(Long id, String name, LocalDate localDate, LocalTime localTime, LocalDateTime localDateTime, java.util.Date legacyDate) {
        em.getTransaction().begin();
        SimpleDateEntity entity = new SimpleDateEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setLocalDate(localDate);
        entity.setLocalTime(localTime);
        entity.setLocalDateTime(localDateTime);
        entity.setLegacyDate(legacyDate);
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
        SimpleDateEntity seed = new SimpleDateEntity();
        seed.setId(0L);
        seed.setName("__seed__");
        em.persist(seed);
        em.flush();
        tx.commit();
    }
}
