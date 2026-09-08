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

import io.vidocq.mansart.persistence.tests.model.Department;
import io.vidocq.mansart.persistence.tests.model.Employee;

/**
 * Tests for JPQL subquery support (EXISTS, NOT EXISTS, IN with subquery, ALL/ANY/SOME, scalar subqueries).
 * Follows the pattern from JpqlCaseExpressionQueryTest.
 */
class JpqlSubqueryQueryTest {

    private static final String PU_NAME = "test-pu";

    private EntityManagerFactory emf;
    private EntityManager em;
    private DataSource dataSource;

    @BeforeEach
    void beforeEach() throws SQLException {
        // Create in-memory H2 database
        org.h2.Driver.load();
        org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
        ds.setURL("jdbc:h2:mem:m6jp45;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        dataSource = ds;

        try (Connection conn = dataSource.getConnection()) {
            // Drop and create schema (quote identifiers)
            conn.createStatement().execute("DROP TABLE IF EXISTS \"employees\"");
            conn.createStatement().execute(
                "CREATE TABLE \"employees\" (\"id\" BIGINT PRIMARY KEY, \"name\" VARCHAR(255), \"salary\" DECIMAL(10,2), \"department_id\" BIGINT)"
            );
            conn.createStatement().execute("DROP TABLE IF EXISTS \"departments\"");
            conn.createStatement().execute(
                "CREATE TABLE \"departments\" (\"id\" BIGINT PRIMARY KEY, \"name\" VARCHAR(255), \"active\" BOOLEAN)"
            );
        }

        // Create EMF
        emf = Persistence.createEntityManagerFactory(PU_NAME, Map.of("jakarta.persistence.jtaDataSource", dataSource));
        em = emf.createEntityManager();
        registerEntityNames();
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
            conn.createStatement().execute("DROP TABLE IF EXISTS \"employees\"");
            conn.createStatement().execute("DROP TABLE IF EXISTS \"departments\"");
        } catch (Exception e) {
            // Ignore drop errors
        }
    }

    @Test
    void scalarSubqueryInComparison() {
        // Given: employees with different name lengths
        persistDepartment(1L, "Engineering", true);
        persistEmployee(1L, "Alice", 50000.0, 1L);
        persistEmployee(2L, "Bob", 50000.0, 1L);
        persistEmployee(3L, "Charlie", 50000.0, 1L);

        // When: scalar subquery in WHERE clause
        TypedQuery<Employee> query = em.createQuery(
            "SELECT e FROM Employee e WHERE LENGTH(e.name) > (SELECT AVG(LENGTH(e2.name)) FROM Employee e2)",
            Employee.class
        );
        List<Employee> results = query.getResultList();

        // Then: only employees with above-average name length
        assertThat(results).extracting(Employee::getName).containsExactlyInAnyOrder("Charlie");
    }

    @Test
    void existsSubquery() {
        // Given: employees and departments
        persistDepartment(1L, "Engineering", true);
        persistDepartment(2L, "Sales", false);
        persistEmployee(1L, "Alice", 90000.0, 1L);
        persistEmployee(2L, "Bob", 110000.0, 1L);
        persistEmployee(3L, "Charlie", 80000.0, 2L);

        // When: EXISTS subquery
        TypedQuery<Employee> query = em.createQuery(
            "SELECT e FROM Employee e WHERE EXISTS (SELECT 1 FROM Department d WHERE d.id = e.department.id)",
            Employee.class
        );
        List<Employee> results = query.getResultList();

        // Then: all employees should match
        assertThat(results).hasSize(3);
    }

    @Test
    void notExistsSubquery() {
        // Given: employees and departments
        persistDepartment(1L, "Engineering", true);
        persistEmployee(1L, "Alice", 90000.0, 1L);
        persistEmployee(2L, "Bob", 110000.0, 1L);

        // When: NOT EXISTS subquery (no departments with id=999)
        TypedQuery<Employee> query = em.createQuery(
            "SELECT e FROM Employee e WHERE NOT EXISTS (SELECT 1 FROM Department d WHERE d.id = 999)",
            Employee.class
        );
        List<Employee> results = query.getResultList();

        // Then: all employees should match (since subquery always returns false)
        assertThat(results).hasSize(2);
    }

    @Test
    void inSubquery() {
        // Given: employees and departments
        persistDepartment(1L, "Engineering", true);
        persistDepartment(2L, "Sales", true);
        persistEmployee(1L, "Alice", 90000.0, 1L);
        persistEmployee(2L, "Bob", 110000.0, 1L);
        persistEmployee(3L, "Charlie", 80000.0, 2L);

        // When: IN subquery
        TypedQuery<Employee> query = em.createQuery(
            "SELECT e FROM Employee e WHERE e.department.id IN (SELECT d.id FROM Department d WHERE d.active = TRUE)",
            Employee.class
        );
        List<Employee> results = query.getResultList();

        // Then: employees in active departments
        assertThat(results).extracting(Employee::getName).containsExactlyInAnyOrder("Alice", "Bob", "Charlie");
    }

    @Test
    void notInSubquery() {
        // Given: employees and departments
        persistDepartment(1L, "Engineering", true);
        persistDepartment(2L, "Sales", false);
        persistEmployee(1L, "Alice", 90000.0, 1L);
        persistEmployee(2L, "Bob", 110000.0, 1L);
        persistEmployee(3L, "Charlie", 80000.0, 2L);

        // When: NOT IN subquery
        TypedQuery<Employee> query = em.createQuery(
            "SELECT e FROM Employee e WHERE e.department.id NOT IN (SELECT d.id FROM Department d WHERE d.active = FALSE)",
            Employee.class
        );
        List<Employee> results = query.getResultList();

        // Then: employees not in inactive departments (Alice and Bob in Engineering)
        assertThat(results).extracting(Employee::getName).containsExactlyInAnyOrder("Alice", "Bob");
    }

    @Test
    void allQuantifiedComparison() {
        // Given: employees with different salaries in different departments
        persistDepartment(1L, "Engineering", true);
        persistDepartment(2L, "Sales", true);
        persistEmployee(1L, "Alice", 90000.0, 1L);
        persistEmployee(2L, "Bob", 110000.0, 1L);
        persistEmployee(3L, "Charlie", 80000.0, 2L);

        // When: > ALL subquery (salary greater than all salaries in department 2)
        TypedQuery<Employee> query = em.createQuery(
            "SELECT e FROM Employee e WHERE e.salary > ALL (SELECT e2.salary FROM Employee e2 WHERE e2.department.id = 2)",
            Employee.class
        );
        List<Employee> results = query.getResultList();

        // Then: only Alice and Bob (salaries > 80000)
        assertThat(results).extracting(Employee::getName).containsExactlyInAnyOrder("Alice", "Bob");
    }

    @Test
    void anyQuantifiedComparison() {
        // Given: employees with different salaries in different departments
        persistDepartment(1L, "Engineering", true);
        persistDepartment(2L, "Sales", true);
        persistEmployee(1L, "Alice", 90000.0, 1L);
        persistEmployee(2L, "Bob", 110000.0, 1L);
        persistEmployee(3L, "Charlie", 80000.0, 2L);

        // When: > ANY subquery (salary greater than any salary in department 2)
        TypedQuery<Employee> query = em.createQuery(
            "SELECT e FROM Employee e WHERE e.salary > ANY (SELECT e2.salary FROM Employee e2 WHERE e2.department.id = 2)",
            Employee.class
        );
        List<Employee> results = query.getResultList();

        // Then: all employees (all salaries > 80000 is false for Charlie, but > ANY means > at least one)
        // Charlie's salary 80000 is not > 80000, but Alice and Bob are > 80000
        assertThat(results).extracting(Employee::getName).containsExactlyInAnyOrder("Alice", "Bob");
    }

    @Test
    void someQuantifiedComparison() {
        // Given: employees with different salaries in different departments
        persistDepartment(1L, "Engineering", true);
        persistDepartment(2L, "Sales", true);
        persistEmployee(1L, "Alice", 90000.0, 1L);
        persistEmployee(2L, "Bob", 110000.0, 1L);
        persistEmployee(3L, "Charlie", 80000.0, 2L);

        // When: > SOME subquery (same as ANY)
        TypedQuery<Employee> query = em.createQuery(
            "SELECT e FROM Employee e WHERE e.salary > SOME (SELECT e2.salary FROM Employee e2 WHERE e2.department.id = 2)",
            Employee.class
        );
        List<Employee> results = query.getResultList();

        // Then: Alice and Bob (salaries > 80000)
        assertThat(results).extracting(Employee::getName).containsExactlyInAnyOrder("Alice", "Bob");
    }

    /**
     * Helper to persist a test department
     */
    private void persistDepartment(Long id, String name, boolean active) {
        em.getTransaction().begin();
        Department dept = new Department();
        dept.setId(id);
        dept.setName(name);
        dept.setActive(active);
        em.persist(dept);
        em.flush();
        em.getTransaction().commit();
    }

    /**
     * Helper to persist a test employee
     */
    private void persistEmployee(Long id, String name, Double salary, Long deptId) {
        em.getTransaction().begin();
        Employee emp = new Employee();
        emp.setId(id);
        emp.setName(name);
        emp.setSalary(salary);
        Department dept = em.find(Department.class, deptId);
        emp.setDepartment(dept);
        em.persist(emp);
        em.flush();
        em.getTransaction().commit();
    }

    /**
     * Triggers entity name registration by persisting and flushing seed entities.
     */
    private void registerEntityNames() {
        jakarta.persistence.EntityTransaction tx = em.getTransaction();
        tx.begin();
        Department seedDept = new Department();
        seedDept.setId(0L);
        seedDept.setName("seed");
        em.persist(seedDept);
        em.flush();
        em.remove(seedDept);
        em.flush();
        tx.commit();
    }
}
