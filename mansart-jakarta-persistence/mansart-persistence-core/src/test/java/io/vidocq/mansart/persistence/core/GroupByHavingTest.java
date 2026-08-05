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
 * Integration tests for combined JPQL GROUP BY and HAVING clauses.
 * M6 - Sprint 1: Validate GROUP BY + HAVING support for Jakarta Persistence 3.2
 */
class GroupByHavingTest {

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
    void testParseGroupByAndHaving() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT e.name, COUNT(e) FROM TestEntity e GROUP BY e.name HAVING COUNT(e) > 5");
        
        assertThat(stmt).isNotNull();
        assertThat(stmt).isInstanceOf(JpqlSelectStmt.class);
        
        JpqlSelectStmt selectStmt = (JpqlSelectStmt) stmt;
        assertThat(selectStmt.hasGroupBy()).isTrue();
        assertThat(selectStmt.hasHaving()).isTrue();
        assertThat(selectStmt.groupByClause()).isPresent();
        assertThat(selectStmt.havingClause()).isPresent();
    }

    @Test
    void testParseComplexGroupByHaving() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT e.name, e.description, COUNT(e) FROM TestEntity e " +
            "GROUP BY e.name, e.description HAVING COUNT(e) > 3 AND COUNT(e) < 10");
        
        assertThat(stmt).isInstanceOf(JpqlSelectStmt.class);
        JpqlSelectStmt selectStmt = (JpqlSelectStmt) stmt;
        assertThat(selectStmt.hasGroupBy()).isTrue();
        assertThat(selectStmt.hasHaving()).isTrue();
        
        JpqlGroupByClause groupByClause = selectStmt.groupByClause().get();
        assertThat(groupByClause.expressions()).hasSize(2);
        
        JpqlHavingClause havingClause = selectStmt.havingClause().get();
        assertThat(havingClause.predicate()).isInstanceOf(JpqlAndPredicate.class);
    }

    // ==================== CONVERSION TESTS ====================

    @Test
    void testConvertGroupByHavingToRuntime() {
        Query query = entityManager.createQuery(
            "SELECT e.name, COUNT(e) FROM TestEntity e GROUP BY e.name HAVING COUNT(e) > 5");
        assertThat(query).isNotNull();
    }

    @Test
    void testConvertGroupByMultipleHavingComplex() {
        Query query = entityManager.createQuery(
            "SELECT e.name, e.description, COUNT(e) FROM TestEntity e " +
            "GROUP BY e.name, e.description HAVING COUNT(e) > 3 AND COUNT(e) < 10");
        assertThat(query).isNotNull();
    }

    // ==================== EXECUTION TESTS ====================

    @Test
    void testExecuteGroupByHaving() {
        entityManager.getTransaction().begin();
        
        for (int i = 0; i < 6; i++) {
            TestEntity entity = new TestEntity("Popular", "Description" + i);
            entityManager.persist(entity);
        }
        
        for (int i = 0; i < 2; i++) {
            TestEntity entity = new TestEntity("Unpopular", "Description" + i);
            entityManager.persist(entity);
        }
        
        entityManager.getTransaction().commit();
        
        Query query = entityManager.createQuery(
            "SELECT e.name FROM TestEntity e GROUP BY e.name HAVING COUNT(e) > 5");
        List<?> results = query.getResultList();
        
        assertThat(results).isNotNull();
    }

    @Test
    void testExecuteGroupByMultipleColumnsHaving() {
        entityManager.getTransaction().begin();
        
        for (int i = 0; i < 5; i++) {
            TestEntity entity = new TestEntity("GroupA", "Desc" + (i % 2));
            entityManager.persist(entity);
        }
        
        entityManager.getTransaction().commit();
        
        Query query = entityManager.createQuery(
            "SELECT e.name, e.description FROM TestEntity e GROUP BY e.name, e.description HAVING COUNT(e) > 2");
        List<?> results = query.getResultList();
        
        assertThat(results).isNotNull();
    }

    @Test
    void testExecuteGroupByHavingWithAllAggregations() {
        entityManager.getTransaction().begin();
        
        for (int i = 0; i < 10; i++) {
            TestEntity entity = new TestEntity("Group" + (i % 3), "Description" + i);
            entityManager.persist(entity);
        }
        
        entityManager.getTransaction().commit();
        
        String[] queries = {
            "SELECT e.name FROM TestEntity e GROUP BY e.name HAVING COUNT(e) > 2",
            "SELECT e.name FROM TestEntity e GROUP BY e.name HAVING MIN(e.id) > 0",
            "SELECT e.name FROM TestEntity e GROUP BY e.name HAVING MAX(e.id) > 5"
        };
        
        for (String jpql : queries) {
            Query query = entityManager.createQuery(jpql);
            List<?> results = query.getResultList();
            assertThat(results).isNotNull();
        }
    }
}
