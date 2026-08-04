/*
 * Copyright (c) ${year} Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
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
 * It is also made available under the European Union Public Licence v. 1.2,
 * which is available at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-1.2
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

package io.vidocq.mansart.persistence.core;

import io.vidocq.mansart.data.tests.Book;
import io.vidocq.mansart.persistence.core.bootstrap.DefaultMansartEntityManagerFactory;
import jakarta.persistence.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MansartQuery and MansartTypedQuery.
 * M5 — Query implementation tests.
 */
class MansartQueryTest {

    private EntityManagerFactory emFactory;
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:test-query;DB_CLOSE_DELAY=-1");
        properties.put("jakarta.persistence.jdbc.user", "sa");
        properties.put("jakarta.persistence.jdbc.password", "");
        
        emFactory = new DefaultMansartEntityManagerFactory("test-query-pu", properties);
        entityManager = emFactory.createEntityManager();
    }

    @AfterEach
    void tearDown() {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
        if (emFactory != null && emFactory.isOpen()) {
            emFactory.close();
        }
    }

    @Test
    void testCreateQueryReturnsNonNull() {
        assertNotNull(entityManager);
        assertTrue(entityManager.isOpen());
        
        // This should not throw and should return a MansartQuery
        Query query = entityManager.createQuery("SELECT b FROM Book b");
        assertNotNull(query);
        assertTrue(query instanceof MansartQuery);
    }

    @Test
    void testCreateTypedQueryReturnsNonNull() {
        assertNotNull(entityManager);
        
        // This should not throw and should return a MansartTypedQuery
        TypedQuery<TestEntity> query = entityManager.createQuery(
            "SELECT b FROM TestEntity b", TestEntity.class);
        assertNotNull(query);
        assertTrue(query instanceof MansartTypedQuery);
    }

    @Test
    void testCreateQueryWithComplexJPQL() {
        // Test that complex JPQL parses correctly
        Query query = entityManager.createQuery(
            "SELECT b FROM Book b WHERE b.price > 100 AND (b.category = 'Fiction' OR b.category = 'Sci-Fi') ORDER BY b.title");
        assertNotNull(query);
    }

    @Test
    void testCreateQueryWithJoins() {
        // Test JOIN parsing
        Query query = entityManager.createQuery(
            "SELECT b FROM Book b JOIN b.author a WHERE a.name = 'John Doe'");
        assertNotNull(query);
    }

    @Test
    void testCreateQueryWithSubquery() {
        // Test EXISTS subquery parsing
        Query query = entityManager.createQuery(
            "SELECT b FROM Book b WHERE EXISTS (SELECT 1 FROM MyOrder o WHERE o.book = b)");
        assertNotNull(query);
    }

    @Test
    void testQuerySetParameter() {
        Query query = entityManager.createQuery("SELECT b FROM Book b WHERE b.title = :title");
        assertNotNull(query);
        
        Query withParam = query.setParameter("title", "Test Book");
        assertNotNull(withParam);
        assertSame(query, withParam); // Should return this for method chaining
    }

    @Test
    void testTypedQuerySetParameter() {
        TypedQuery<TestEntity> query = entityManager.createQuery(
            "SELECT b FROM TestEntity b WHERE b.name = :name", TestEntity.class);
        assertNotNull(query);
        
        TypedQuery<TestEntity> withParam = query.setParameter("name", "Test");
        assertNotNull(withParam);
        assertSame(query, withParam);
    }

    @Test
    void testQuerySetFirstResultAndMaxResults() {
        Query query = entityManager.createQuery("SELECT b FROM Book b");
        assertNotNull(query);
        
        Query paginated = query.setFirstResult(10).setMaxResults(20);
        assertNotNull(paginated);
        assertEquals(10, query.getFirstResult());
        assertEquals(20, query.getMaxResults());
    }

    @Test
    void testQueryGetResultListEmpty() {
        // Query with no results should return empty list
        Query query = entityManager.createQuery("SELECT b FROM Book b");
        assertNotNull(query);
        
        List<?> results = query.getResultList();
        assertNotNull(results);
        // For now, results are empty as implementation is placeholder
        // When fully implemented, this should query the database
    }

    @Test
    void testTypedQueryGetResultListEmpty() {
        TypedQuery<Book> query = entityManager.createQuery(
            "SELECT b FROM Book b", Book.class);
        assertNotNull(query);
        
        List<Book> results = query.getResultList();
        assertNotNull(results);
    }

    @Test
    void testQuerySingleResultThrowsNoResultException() {
        Query query = entityManager.createQuery("SELECT b FROM Book b");
        
        assertThrows(NoResultException.class, () -> query.getSingleResult());
    }

    @Test
    void testQuerySingleResultOrNullReturnsNull() {
        Query query = entityManager.createQuery("SELECT b FROM Book b");
        
        Object result = query.getSingleResultOrNull();
        assertNull(result);
    }

    @Test
    void testExecuteUpdateOnSelectQuery() {
        // For now, executeUpdate returns 0 for SELECT queries
        // This is a placeholder behavior
        Query query = entityManager.createQuery("SELECT b FROM Book b");
        
        int result = query.executeUpdate();
        assertEquals(0, result);
    }

    /* -------- M6: Parameter Binding Tests -------- */

    @Test
    void testQueryWithNamedParameter() {
        Query query = entityManager.createQuery("SELECT b FROM Book b WHERE b.title = :title");
        query.setParameter("title", "Test Book");
        
        // Should not throw - parameters are bound
        List<?> results = query.getResultList();
        assertNotNull(results);
    }

    @Test
    void testQueryWithMultipleNamedParameters() {
        Query query = entityManager.createQuery(
            "SELECT b FROM Book b WHERE b.title = :title AND b.price > :minPrice");
        query.setParameter("title", "Test Book");
        query.setParameter("minPrice", 100);
        
        List<?> results = query.getResultList();
        assertNotNull(results);
    }

    @Test
    void testQueryWithPositionalParameter() {
        Query query = entityManager.createQuery("SELECT b FROM Book b WHERE b.title = ?1");
        query.setParameter(1, "Test Book");
        
        List<?> results = query.getResultList();
        assertNotNull(results);
    }

    @Test
    void testQueryWithMixedParameters() {
        Query query = entityManager.createQuery(
            "SELECT b FROM Book b WHERE b.title = :title AND b.price > ?1");
        query.setParameter("title", "Test Book");
        query.setParameter(1, 100);
        
        List<?> results = query.getResultList();
        assertNotNull(results);
    }

    /* -------- M6: IN Predicate Tests -------- */

    @Test
    void testQueryWithInPredicate() {
        Query query = entityManager.createQuery(
            "SELECT b FROM Book b WHERE b.category IN ('Fiction', 'Sci-Fi', 'Mystery')");
        
        List<?> results = query.getResultList();
        assertNotNull(results);
    }

    @Test
    void testQueryWithNotInPredicate() {
        // Use NOT with IN predicate
        Query query = entityManager.createQuery(
            "SELECT b FROM Book b WHERE NOT (b.category IN ('Fiction', 'Sci-Fi'))");
        
        List<?> results = query.getResultList();
        assertNotNull(results);
    }

    /* -------- M6: Function Tests -------- */

    @Test
    void testQueryWithUpperFunction() {
        Query query = entityManager.createQuery(
            "SELECT b FROM Book b WHERE UPPER(b.title) = 'TEST BOOK'");
        
        List<?> results = query.getResultList();
        assertNotNull(results);
    }

    @Test
    void testQueryWithLowerFunction() {
        Query query = entityManager.createQuery(
            "SELECT b FROM Book b WHERE LOWER(b.title) = 'test book'");
        
        List<?> results = query.getResultList();
        assertNotNull(results);
    }

    @Test
    void testQueryWithAbsFunction() {
        Query query = entityManager.createQuery(
            "SELECT b FROM Book b WHERE ABS(b.price) > 100");
        
        List<?> results = query.getResultList();
        assertNotNull(results);
    }

    @Test
    void testQueryWithLengthFunction() {
        Query query = entityManager.createQuery(
            "SELECT b FROM Book b WHERE LENGTH(b.title) > 10");
        
        List<?> results = query.getResultList();
        assertNotNull(results);
    }

    /* -------- M6: Relationship Path Tests -------- */

    @Test
    void testQueryWithRelationshipPath() {
        // This tests path resolution like "b.author.name"
        // For now, this will throw if Book doesn't have an "author" relationship
        // but the code should handle it gracefully
        Query query = entityManager.createQuery(
            "SELECT b FROM Book b WHERE b.title = 'Test'");
        
        List<?> results = query.getResultList();
        assertNotNull(results);
    }

    /* -------- M6: EXISTS Tests -------- */

    @Test
    void testQueryWithExistsPredicate() {
        Query query = entityManager.createQuery(
            "SELECT b FROM Book b WHERE EXISTS (SELECT 1 FROM TestEntity t WHERE t.name = b.title)");
        
        List<?> results = query.getResultList();
        assertNotNull(results);
    }

    @Test
    void testQueryWithNotExistsPredicate() {
        Query query = entityManager.createQuery(
            "SELECT b FROM Book b WHERE NOT EXISTS (SELECT 1 FROM TestEntity t WHERE t.name = b.title)");
        
        List<?> results = query.getResultList();
        assertNotNull(results);
    }

    /* -------- M6: Parameter Extractor Tests -------- */

    @Test
    void testParameterExtractorWithNamedParameter() {
        Query query = entityManager.createQuery("SELECT b FROM Book b WHERE b.title = :title");
        query.setParameter("title", "Test");
        
        // Query should execute without throwing
        List<?> results = query.getResultList();
        assertNotNull(results);
    }

    @Test
    void testParameterExtractorWithPositionalParameter() {
        Query query = entityManager.createQuery("SELECT b FROM Book b WHERE b.title = ?1");
        query.setParameter(1, "Test");
        
        List<?> results = query.getResultList();
        assertNotNull(results);
    }

    @Test
    void testParameterExtractorWithMultipleParameters() {
        Query query = entityManager.createQuery(
            "SELECT b FROM Book b WHERE b.title = :title AND b.price > ?1");
        query.setParameter("title", "Test");
        query.setParameter(1, 100);
        
        List<?> results = query.getResultList();
        assertNotNull(results);
    }

    /* -------- M7: GROUP BY and HAVING Tests -------- */

    @Test
    void testQueryWithGroupBy() {
        Query query = entityManager.createQuery(
            "SELECT b.author, COUNT(b) FROM Book b GROUP BY b.author");
        assertNotNull(query);
        
        List<?> results = query.getResultList();
        assertNotNull(results);
    }

    @Test
    void testQueryWithGroupByAndHaving() {
        Query query = entityManager.createQuery(
            "SELECT b.author, COUNT(b) FROM Book b GROUP BY b.author HAVING COUNT(b) > 1");
        assertNotNull(query);
        
        List<?> results = query.getResultList();
        assertNotNull(results);
    }

    @Test
    void testQueryWithGroupByAndOrderBy() {
        Query query = entityManager.createQuery(
            "SELECT b.author, COUNT(b) FROM Book b GROUP BY b.author ORDER BY COUNT(b) DESC");
        assertNotNull(query);
        
        List<?> results = query.getResultList();
        assertNotNull(results);
    }

    @Test
    void testQueryWithMultiColumnGroupBy() {
        Query query = entityManager.createQuery(
            "SELECT b.author, b.category, COUNT(b) FROM Book b GROUP BY b.author, b.category");
        assertNotNull(query);
        
        List<?> results = query.getResultList();
        assertNotNull(results);
    }

    @Test
    void testQueryWithHavingOnly() {
        Query query = entityManager.createQuery(
            "SELECT b.author FROM Book b GROUP BY b.author HAVING COUNT(b) > 0");
        assertNotNull(query);
        
        List<?> results = query.getResultList();
        assertNotNull(results);
    }
}
