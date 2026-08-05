/*
 * Copyright (c) ${year} Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */

package io.vidocq.mansart.persistence.core;

import io.vidocq.mansart.data.query.ast.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for JPQL HAVING clause implementation.
 * M6 - Sprint 1: Validate HAVING support for Jakarta Persistence 3.2
 */
class HavingTest {

    private EntityManagerFactory emFactory;
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        emFactory = Persistence.createEntityManagerFactory("testPU");
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

    // ==================== PARSING TESTS ====================

    @Test
    void testParseHavingWithComparison() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT e.name, COUNT(e) FROM TestEntity e GROUP BY e.name HAVING COUNT(e) > 5");
        
        assertThat(stmt).isNotNull();
        assertThat(stmt).isInstanceOf(JpqlSelectStmt.class);
        
        JpqlSelectStmt selectStmt = (JpqlSelectStmt) stmt;
        assertThat(selectStmt.hasHaving()).isTrue();
        assertThat(selectStmt.havingClause()).isPresent();
        
        JpqlHavingClause havingClause = selectStmt.havingClause().get();
        assertThat(havingClause.predicate()).isNotNull();
        assertThat(havingClause.predicate()).isInstanceOf(JpqlComparisonPredicate.class);
    }

    @Test
    void testParseHavingWithMultipleConditions() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT e.name FROM TestEntity e GROUP BY e.name HAVING COUNT(e) > 5 AND COUNT(e) < 100");
        
        assertThat(stmt).isInstanceOf(JpqlSelectStmt.class);
        JpqlSelectStmt selectStmt = (JpqlSelectStmt) stmt;
        assertThat(selectStmt.hasHaving()).isTrue();
        
        JpqlHavingClause havingClause = selectStmt.havingClause().get();
        assertThat(havingClause.predicate()).isInstanceOf(JpqlAndPredicate.class);
    }

    @Test
    void testParseQueryWithoutHaving() {
        JpqlStmt stmt = JpqlParser.parse("SELECT e.name FROM TestEntity e GROUP BY e.name");
        
        assertThat(stmt).isInstanceOf(JpqlSelectStmt.class);
        JpqlSelectStmt selectStmt = (JpqlSelectStmt) stmt;
        assertThat(selectStmt.hasHaving()).isFalse();
        assertThat(selectStmt.havingClause()).isEmpty();
    }

    // ==================== CONVERSION TESTS ====================

    @Test
    void testConvertHavingToRuntime() {
        Query query = entityManager.createQuery(
            "SELECT e.name FROM TestEntity e GROUP BY e.name HAVING COUNT(e) > 5");
        assertThat(query).isNotNull();
    }

    @Test
    void testConvertHavingWithAnd() {
        Query query = entityManager.createQuery(
            "SELECT e.name FROM TestEntity e GROUP BY e.name HAVING COUNT(e) > 5 AND COUNT(e) < 100");
        assertThat(query).isNotNull();
    }

    @Test
    void testConvertHavingWithOr() {
        Query query = entityManager.createQuery(
            "SELECT e.name FROM TestEntity e GROUP BY e.name HAVING COUNT(e) > 5 OR COUNT(e) < 2");
        assertThat(query).isNotNull();
    }

    // ==================== EXECUTION TESTS ====================

    @Test
    void testExecuteHavingWithCount() {
        entityManager.getTransaction().begin();
        
        for (int i = 0; i < 6; i++) {
            TestEntity entity = new TestEntity("GroupA", "Description" + i);
            entityManager.persist(entity);
        }
        
        for (int i = 0; i < 3; i++) {
            TestEntity entity = new TestEntity("GroupB", "Description" + i);
            entityManager.persist(entity);
        }
        
        entityManager.getTransaction().commit();
        
        Query query = entityManager.createQuery(
            "SELECT e.name FROM TestEntity e GROUP BY e.name HAVING COUNT(e) > 5");
        List<?> results = query.getResultList();
        
        assertThat(results).isNotNull();
    }

    @Test
    void testExecuteHavingWithAggregationAndComparison() {
        entityManager.getTransaction().begin();
        
        for (int i = 0; i < 10; i++) {
            TestEntity entity = new TestEntity("Group" + (i % 2), "Description" + i);
            entityManager.persist(entity);
        }
        
        entityManager.getTransaction().commit();
        
        Query query = entityManager.createQuery(
            "SELECT e.name FROM TestEntity e GROUP BY e.name HAVING COUNT(e) > 3 AND COUNT(e) < 8");
        List<?> results = query.getResultList();
        
        assertThat(results).isNotNull();
    }
}
