/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * or any later version, which is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * It is also made available under the European Union Public License v. 1.2,
 * which is available at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Comprehensive tests for native SQL queries (M8-16).
 * Tests SELECT, UPDATE, DELETE, INSERT statements with various parameter bindings.
 * Uses direct SQL DDL/DML instead of em.persist() to test against actual database.
 */
public class NativeQueryTest extends BasePersistenceTest {

    private Connection connection;

    // ========================================================================
    // Database setup
    // ========================================================================

    @BeforeEach
    void setUpDatabase() throws SQLException {
        super.setUp();
        String url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
        String user = "sa";
        String password = "";
        connection = DriverManager.getConnection(url, user, password);
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS SimpleTest");
            stmt.execute("CREATE TABLE SimpleTest (id BIGINT PRIMARY KEY, name VARCHAR(255), val VARCHAR(255))");
        }
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM SimpleTest");
        }
    }

    @AfterEach
    void tearDownDatabase() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS SimpleTest");
            } catch (SQLException e) {
                // Ignore drop errors during teardown
            }
            connection.close();
        }
        super.tearDown();
    }

    private long idCounter = 1;

    private void insertRow(Long id, String name, String val) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            long actualId = id != null ? id : idCounter++;
            stmt.execute("INSERT INTO SimpleTest (id, name, val) VALUES (" + actualId + ", '" + name + "', '" + val + "')");
        }
    }

    private int countRows() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM SimpleTest");
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    // ========================================================================
    // SELECT queries
    // ========================================================================

    @Test
    void nativeQuerySelectAllReturnsObjectArray() throws SQLException {
        insertRow(null, "name1", "value1");
        insertRow(null, "name2", "value2");

        Query query = em.createNativeQuery("SELECT id, name, val FROM SimpleTest ORDER BY id");
        List<?> results = query.getResultList();

        assertThat(results).hasSize(2);
        Object[] firstRow = (Object[]) results.get(0);
        assertThat(firstRow).hasSize(3);
        assertThat(firstRow[1]).isEqualTo("name1");
        assertThat(firstRow[2]).isEqualTo("value1");
        
        Object[] secondRow = (Object[]) results.get(1);
        assertThat(secondRow).hasSize(3);
        assertThat(secondRow[1]).isEqualTo("name2");
        assertThat(secondRow[2]).isEqualTo("value2");
    }

    @Test
    void nativeQuerySelectWithWhereClause() throws SQLException {
        insertRow(null, "specific", "data");
        insertRow(null, "other", "ignored");

        Query query = em.createNativeQuery("SELECT id, name, val FROM SimpleTest WHERE name = 'specific'");
        List<?> results = query.getResultList();

        assertThat(results).hasSize(1);
        Object[] row = (Object[]) results.get(0);
        assertThat(row[1]).isEqualTo("specific");
        assertThat(row[2]).isEqualTo("data");
    }

    @Test
    void nativeQuerySelectEmptyResult() {
        Query query = em.createNativeQuery("SELECT id, name, val FROM SimpleTest WHERE name = 'nonexistent'");
        List<?> results = query.getResultList();
        assertThat(results).isEmpty();
    }

    // ========================================================================
    // getSingleResult
    // ========================================================================

    @Test
    void nativeQueryGetSingleResultSuccess() throws SQLException {
        insertRow(null, "single", "result");

        Query query = em.createNativeQuery("SELECT id, name, val FROM SimpleTest WHERE name = 'single'");
        Object result = query.getSingleResult();

        assertThat(result).isNotNull();
        Object[] row = (Object[]) result;
        assertThat(row[1]).isEqualTo("single");
        assertThat(row[2]).isEqualTo("result");
    }

    @Test
    void nativeQueryGetSingleResultNoResultThrows() {
        Query query = em.createNativeQuery("SELECT id, name, val FROM SimpleTest WHERE name = 'nonexistent'");

        assertThatThrownBy(query::getSingleResult)
                .isInstanceOf(NoResultException.class)
                .hasMessageContaining("No result found");
    }

    @Test
    void nativeQueryGetSingleResultMultipleRowsThrows() throws SQLException {
        insertRow(null, "dup1", "val1");
        insertRow(null, "dup2", "val2");

        Query query = em.createNativeQuery("SELECT id, name, val FROM SimpleTest");

        assertThatThrownBy(query::getSingleResult)
                .isInstanceOf(NonUniqueResultException.class)
                .hasMessageContaining("Multiple results found");
    }

    // ========================================================================
    // getSingleResultOrNull
    // ========================================================================

    @Test
    void nativeQueryGetSingleResultOrNullWithResult() throws SQLException {
        insertRow(null, "nulltest", "data");

        Query query = em.createNativeQuery("SELECT id, name, val FROM SimpleTest WHERE name = 'nulltest'");
        Object result = query.getSingleResultOrNull();

        assertThat(result).isNotNull();
    }

    @Test
    void nativeQueryGetSingleResultOrNullWithNoResult() {
        Query query = em.createNativeQuery("SELECT id, name, val FROM SimpleTest WHERE name = 'nonexistent'");
        Object result = query.getSingleResultOrNull();
        assertThat(result).isNull();
    }

    // ========================================================================
    // UPDATE queries
    // ========================================================================

    @Test
    void nativeQueryUpdate() throws SQLException {
        insertRow(null, "updateTarget", "original");

        Query query = em.createNativeQuery("UPDATE SimpleTest SET val = 'updated' WHERE name = 'updateTarget'");
        int updateCount = query.executeUpdate();

        assertThat(updateCount).isEqualTo(1);

        try (var stmt = connection.createStatement()) {
            var rs = stmt.executeQuery("SELECT val FROM SimpleTest WHERE name = 'updateTarget'");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("val")).isEqualTo("updated");
        }
    }

    @Test
    void nativeQueryUpdateMultipleRows() throws SQLException {
        insertRow(null, "batch1", "old");
        insertRow(null, "batch2", "old");

        Query query = em.createNativeQuery("UPDATE SimpleTest SET val = 'new' WHERE val = 'old'");
        int updateCount = query.executeUpdate();

        assertThat(updateCount).isEqualTo(2);
    }

    @Test
    void nativeQueryUpdateNoMatch() {
        Query query = em.createNativeQuery("UPDATE SimpleTest SET val = 'new' WHERE name = 'nonexistent'");
        int updateCount = query.executeUpdate();
        assertThat(updateCount).isEqualTo(0);
    }

    // ========================================================================
    // DELETE queries
    // ========================================================================

    @Test
    void nativeQueryDelete() throws SQLException {
        insertRow(null, "deleteTarget", "value");

        Query query = em.createNativeQuery("DELETE FROM SimpleTest WHERE name = 'deleteTarget'");
        int deleteCount = query.executeUpdate();

        assertThat(deleteCount).isEqualTo(1);
        assertThat(countRows()).isEqualTo(0);
    }

    @Test
    void nativeQueryDeleteAll() throws SQLException {
        insertRow(null, "del1", "v1");
        insertRow(null, "del2", "v2");

        Query query = em.createNativeQuery("DELETE FROM SimpleTest");
        int deleteCount = query.executeUpdate();

        assertThat(deleteCount).isEqualTo(2);
    }

    // ========================================================================
    // INSERT queries
    // ========================================================================

    @Test
    void nativeQueryInsert() throws SQLException {
        assertThat(countRows()).isEqualTo(0);

        Query query = em.createNativeQuery("INSERT INTO SimpleTest (id, name, val) VALUES (999, 'inserted', 'viaNative')");
        int insertCount = query.executeUpdate();

        assertThat(insertCount).isEqualTo(1);

        try (var stmt = connection.createStatement()) {
            var rs = stmt.executeQuery("SELECT id, name, val FROM SimpleTest WHERE id = 999");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong("id")).isEqualTo(999);
            assertThat(rs.getString("name")).isEqualTo("inserted");
            assertThat(rs.getString("val")).isEqualTo("viaNative");
        }
    }

    // ========================================================================
    // Parameter binding - Positional
    // ========================================================================

    @Test
    void nativeQueryWithPositionalParameterSelect() throws SQLException {
        insertRow(null, "posParam", "value");

        Query query = em.createNativeQuery("SELECT id, name, val FROM SimpleTest WHERE name = ?");
        query.setParameter(1, "posParam");
        
        List<?> results = query.getResultList();
        assertThat(results).hasSize(1);
        Object[] row = (Object[]) results.get(0);
        assertThat(row[1]).isEqualTo("posParam");
    }

    @Test
    void nativeQueryWithPositionalParameterUpdate() throws SQLException {
        insertRow(null, "posUpdate", "original");

        Query query = em.createNativeQuery("UPDATE SimpleTest SET val = ? WHERE name = ?");
        query.setParameter(1, "modified");
        query.setParameter(2, "posUpdate");
        
        int updateCount = query.executeUpdate();
        assertThat(updateCount).isEqualTo(1);

        try (var stmt = connection.createStatement()) {
            var rs = stmt.executeQuery("SELECT val FROM SimpleTest WHERE name = 'posUpdate'");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("val")).isEqualTo("modified");
        }
    }

    // ========================================================================
    // Parameter binding - Multiple parameters
    // ========================================================================

    @Test
    void nativeQueryWithMultiplePositionalParameters() throws SQLException {
        insertRow(null, "multi1", "val1");
        insertRow(null, "multi2", "val2");

        Query query = em.createNativeQuery("SELECT id, name, val FROM SimpleTest WHERE name = ? AND val = ?");
        query.setParameter(1, "multi1");
        query.setParameter(2, "val1");
        
        List<?> results = query.getResultList();
        assertThat(results).hasSize(1);
    }

    // ========================================================================
    // Pagination
    // ========================================================================

    @Test
    void nativeQueryWithMaxResults() throws SQLException {
        for (int i = 0; i < 10; i++) {
            insertRow(null, "page" + i, "value" + i);
        }

        Query query = em.createNativeQuery("SELECT id, name, val FROM SimpleTest ORDER BY id");
        query.setMaxResults(5);
        
        List<?> results = query.getResultList();
        assertThat(results).hasSize(5);
    }

    @Test
    void nativeQueryWithFirstResult() throws SQLException {
        for (int i = 0; i < 10; i++) {
            insertRow(null, "offset" + i, "value" + i);
        }

        Query query = em.createNativeQuery("SELECT id, name, val FROM SimpleTest ORDER BY id");
        query.setFirstResult(0);
        query.setMaxResults(5);
        
        List<?> results = query.getResultList();
        assertThat(results).hasSize(5);
    }

    // ========================================================================
    // Null parameter handling
    // ========================================================================

    @Test
    void nativeQueryWithNullPositionalParameter() throws SQLException {
        insertRow(null, "nullSafe", "value");

        Query query = em.createNativeQuery("SELECT id, name, val FROM SimpleTest WHERE name = ? OR name IS NULL");
        query.setParameter(1, "nullSafe");
        
        List<?> results = query.getResultList();
        assertThat(results).isNotEmpty();
    }

    @Test
    void nativeQueryWithNullNamedParameter() throws SQLException {
        insertRow(null, "nullSafeNamed", "value");

        Query query = em.createNativeQuery("SELECT id, name, val FROM SimpleTest WHERE name = ? OR name IS NULL");
        query.setParameter(1, "nullSafeNamed");
        
        List<?> results = query.getResultList();
        assertThat(results).isNotEmpty();
    }

    // ========================================================================
    // Edge cases
    // ========================================================================

    @Test
    void nativeQuerySelectSpecificColumns() throws SQLException {
        insertRow(null, "test", "value");

        Query query = em.createNativeQuery("SELECT name, val FROM SimpleTest");
        List<?> results = query.getResultList();

        assertThat(results).hasSize(1);
        Object[] row = (Object[]) results.get(0);
        assertThat(row).hasSize(2);
        assertThat(row[0]).isEqualTo("test");
        assertThat(row[1]).isEqualTo("value");
    }

    @Test
    void nativeQueryWithOrderBy() throws SQLException {
        insertRow(null, "zebra", "val");
        insertRow(null, "alpha", "val");
        insertRow(null, "middle", "val");

        Query query = em.createNativeQuery("SELECT name FROM SimpleTest ORDER BY name ASC");
        List<?> results = query.getResultList();

        assertThat(results).hasSize(3);
        assertThat(((Object[]) results.get(0))[0]).isEqualTo("alpha");
        assertThat(((Object[]) results.get(1))[0]).isEqualTo("middle");
        assertThat(((Object[]) results.get(2))[0]).isEqualTo("zebra");
    }

    @Test
    void nativeQueryWithLikeClause() throws SQLException {
        insertRow(null, "test1", "val");
        insertRow(null, "test2", "val");
        insertRow(null, "other", "val");

        Query query = em.createNativeQuery("SELECT name FROM SimpleTest WHERE name LIKE ?");
        query.setParameter(1, "test%");
        
        List<?> results = query.getResultList();
        assertThat(results).hasSize(2);
    }

    @Test
    void nativeQueryCount() throws SQLException {
        insertRow(null, "test1", "val");
        insertRow(null, "test2", "val");

        Query query = em.createNativeQuery("SELECT COUNT(*) FROM SimpleTest");
        List<?> results = query.getResultList();

        assertThat(results).hasSize(1);
        Object[] row = (Object[]) results.get(0);
        assertThat(row[0]).isEqualTo(2L);
    }
}
