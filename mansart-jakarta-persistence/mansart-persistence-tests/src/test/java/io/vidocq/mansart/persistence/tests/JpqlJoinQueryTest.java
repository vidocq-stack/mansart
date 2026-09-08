/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
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

import io.vidocq.mansart.persistence.tests.model.Department;
import io.vidocq.mansart.persistence.tests.model.Employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for M6-JP-36: FROM clause joins (INNER JOIN and implicit
 * path navigation). LEFT/RIGHT/CROSS joins are out of scope and must throw.
 */
class JpqlJoinQueryTest {

    private static final String DB_URL = "jdbc:h2:mem:m6jp36;DB_CLOSE_DELAY=-1";
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
            stmt.execute("DROP TABLE IF EXISTS \"employees\"");
            stmt.execute("DROP TABLE IF EXISTS \"departments\"");
            stmt.execute("CREATE TABLE \"departments\" (\"id\" BIGINT PRIMARY KEY, \"name\" VARCHAR(255), \"active\" BOOLEAN)");
            stmt.execute("CREATE TABLE \"employees\" (\"id\" BIGINT PRIMARY KEY, \"name\" VARCHAR(255), \"salary\" DOUBLE, \"department_id\" BIGINT)");
        }
    }

    private void persist(Object... entities) {
        EntityTransaction tx = em.getTransaction();
        tx.begin();
        for (Object e : entities) {
            em.persist(e);
        }
        em.flush();
        tx.commit();
    }

    @Test
    void explicitInnerJoinReturnsJoinedEntities() {
        Department engineering = new Department();
        engineering.setId(1L);
        engineering.setName("Engineering");

        Department sales = new Department();
        sales.setId(2L);
        sales.setName("Sales");

        Employee emp1 = new Employee();
        emp1.setId(1L);
        emp1.setName("Alice");
        emp1.setDepartment(engineering);

        Employee emp2 = new Employee();
        emp2.setId(2L);
        emp2.setName("Bob");
        emp2.setDepartment(engineering);

        Employee emp3 = new Employee();
        emp3.setId(3L);
        emp3.setName("Charlie");
        emp3.setDepartment(sales);

        persist(engineering, sales, emp1, emp2, emp3);

        List<Employee> result = em.createQuery("SELECT e FROM Employee e INNER JOIN e.department d", Employee.class)
                .getResultList();
        assertThat(result).hasSize(3);
    }

    @Test
    void explicitJoinWithWhereOnJoinedEntityFiltersCorrectly() {
        Department engineering = new Department();
        engineering.setId(1L);
        engineering.setName("Engineering");

        Department sales = new Department();
        sales.setId(2L);
        sales.setName("Sales");

        Employee emp1 = new Employee();
        emp1.setId(1L);
        emp1.setName("Alice");
        emp1.setDepartment(engineering);

        Employee emp2 = new Employee();
        emp2.setId(2L);
        emp2.setName("Bob");
        emp2.setDepartment(engineering);

        Employee emp3 = new Employee();
        emp3.setId(3L);
        emp3.setName("Charlie");
        emp3.setDepartment(sales);

        persist(engineering, sales, emp1, emp2, emp3);

        List<Employee> result = em.createQuery(
                "SELECT e FROM Employee e JOIN e.department d WHERE d.name = 'Engineering'", Employee.class)
                .getResultList();
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Employee::getName).containsExactlyInAnyOrder("Alice", "Bob");
    }

    @Test
    void implicitJoinThroughPathNavigationInWhere() {
        Department engineering = new Department();
        engineering.setId(1L);
        engineering.setName("Engineering");

        Department sales = new Department();
        sales.setId(2L);
        sales.setName("Sales");

        Employee emp1 = new Employee();
        emp1.setId(1L);
        emp1.setName("Alice");
        emp1.setDepartment(engineering);

        Employee emp2 = new Employee();
        emp2.setId(2L);
        emp2.setName("Bob");
        emp2.setDepartment(engineering);

        Employee emp3 = new Employee();
        emp3.setId(3L);
        emp3.setName("Charlie");
        emp3.setDepartment(sales);

        persist(engineering, sales, emp1, emp2, emp3);

        List<Employee> result = em.createQuery(
                "SELECT e FROM Employee e WHERE e.department.name = 'Engineering'", Employee.class)
                .getResultList();
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Employee::getName).containsExactlyInAnyOrder("Alice", "Bob");
    }

    @Test
    void explicitJoinReturnsCorrectEmployeeNames() {
        Department engineering = new Department();
        engineering.setId(1L);
        engineering.setName("Engineering");

        Department sales = new Department();
        sales.setId(2L);
        sales.setName("Sales");

        Employee emp1 = new Employee();
        emp1.setId(1L);
        emp1.setName("Alice");
        emp1.setDepartment(engineering);

        Employee emp2 = new Employee();
        emp2.setId(2L);
        emp2.setName("Bob");
        emp2.setDepartment(engineering);

        Employee emp3 = new Employee();
        emp3.setId(3L);
        emp3.setName("Charlie");
        emp3.setDepartment(sales);

        persist(engineering, sales, emp1, emp2, emp3);

        List<Employee> result = em.createQuery(
                "SELECT e FROM Employee e JOIN e.department d WHERE d.name = 'Sales'", Employee.class)
                .getResultList();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Charlie");
    }

    @Test
    void leftJoinThrowsUnsupported() {
        Department engineering = new Department();
        engineering.setId(1L);
        engineering.setName("Engineering");

        Employee emp1 = new Employee();
        emp1.setId(1L);
        emp1.setName("Alice");
        emp1.setDepartment(engineering);

        persist(engineering, emp1);

        assertThatThrownBy(() -> em.createQuery("SELECT e FROM Employee e LEFT JOIN e.department d", Employee.class)
                .getResultList())
                .isInstanceOf(jakarta.persistence.PersistenceException.class);
    }
}
