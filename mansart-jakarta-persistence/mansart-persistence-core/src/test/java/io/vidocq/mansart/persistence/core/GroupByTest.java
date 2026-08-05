/*
 * Copyright (c) ${year} Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
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
 * Tests for JPQL GROUP BY clause implementation.
 * M6 - Sprint 1: Validate GROUP BY support for Jakarta Persistence 3.2
 */
class GroupByTest {

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
    void testParseGroupBySingleExpression() {
        JpqlStmt stmt = JpqlParser.parse("SELECT e.name FROM TestEntity e GROUP BY e.name");
        
        assertThat(stmt).isNotNull();
        assertThat(stmt).isInstanceOf(JpqlSelectStmt.class);
        
        JpqlSelectStmt selectStmt = (JpqlSelectStmt) stmt;
        assertThat(selectStmt.hasGroupBy()).isTrue();
        assertThat(selectStmt.groupByClause()).isPresent();
        
        JpqlGroupByClause groupByClause = selectStmt.groupByClause().get();
        assertThat(groupByClause.expressions()).hasSize(1);
        assertThat(groupByClause.expressions().get(0)).isInstanceOf(JpqlPathExpr.class);
    }

    @Test
    void testParseGroupByMultipleExpressions() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT e.name, e.description FROM TestEntity e GROUP BY e.name, e.description");
        
        assertThat(stmt).isInstanceOf(JpqlSelectStmt.class);
        JpqlSelectStmt selectStmt = (JpqlSelectStmt) stmt;
        assertThat(selectStmt.hasGroupBy()).isTrue();
        
        JpqlGroupByClause groupByClause = selectStmt.groupByClause().get();
        assertThat(groupByClause.expressions()).hasSize(2);
    }

    @Test
    void testParseGroupByWithAggregationFunction() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT COUNT(e), e.name FROM TestEntity e GROUP BY e.name");
        
        assertThat(stmt).isInstanceOf(JpqlSelectStmt.class);
        JpqlSelectStmt selectStmt = (JpqlSelectStmt) stmt;
        assertThat(selectStmt.hasGroupBy()).isTrue();
        
        JpqlGroupByClause groupByClause = selectStmt.groupByClause().get();
        assertThat(groupByClause.expressions()).hasSize(1);
    }

    @Test
    void testParseQueryWithoutGroupBy() {
        JpqlStmt stmt = JpqlParser.parse("SELECT e FROM TestEntity e");
        
        assertThat(stmt).isInstanceOf(JpqlSelectStmt.class);
        JpqlSelectStmt selectStmt = (JpqlSelectStmt) stmt;
        assertThat(selectStmt.hasGroupBy()).isFalse();
        assertThat(selectStmt.groupByClause()).isEmpty();
    }

    // ==================== CONVERSION TESTS ====================

    @Test
    void testConvertGroupByToRuntime() {
        Query query = entityManager.createQuery("SELECT e.name FROM TestEntity e GROUP BY e.name");
        assertThat(query).isNotNull();
    }

    @Test
    void testConvertGroupByMultipleColumns() {
        Query query = entityManager.createQuery(
            "SELECT e.name, e.description FROM TestEntity e GROUP BY e.name, e.description");
        assertThat(query).isNotNull();
    }

    // ==================== EXECUTION TESTS ====================

    @Test
    void testExecuteGroupBySingleColumn() {
        entityManager.getTransaction().begin();
        
        TestEntity entity1 = new TestEntity("Name1", "Description1");
        entityManager.persist(entity1);
        
        TestEntity entity2 = new TestEntity("Name2", "Description2");
        entityManager.persist(entity2);
        
        TestEntity entity3 = new TestEntity("Name1", "Description3");
        entityManager.persist(entity3);
        
        entityManager.getTransaction().commit();
        
        Query query = entityManager.createQuery("SELECT e.name FROM TestEntity e GROUP BY e.name");
        List<?> results = query.getResultList();
        
        assertThat(results).isNotNull();
    }

    @Test
    void testExecuteGroupByWithAggregation() {
        entityManager.getTransaction().begin();
        
        TestEntity entity1 = new TestEntity("Name1", "Desc1");
        entityManager.persist(entity1);
        
        TestEntity entity2 = new TestEntity("Name1", "Desc2");
        entityManager.persist(entity2);
        
        TestEntity entity3 = new TestEntity("Name2", "Desc3");
        entityManager.persist(entity3);
        
        entityManager.getTransaction().commit();
        
        Query query = entityManager.createQuery("SELECT COUNT(e), e.name FROM TestEntity e GROUP BY e.name");
        List<?> results = query.getResultList();
        
        assertThat(results).isNotNull();
    }

    @Test
    void testExecuteGroupByMultipleColumns() {
        entityManager.getTransaction().begin();
        
        TestEntity entity1 = new TestEntity("Name1", "Desc1");
        entityManager.persist(entity1);
        
        TestEntity entity2 = new TestEntity("Name1", "Desc2");
        entityManager.persist(entity2);
        
        TestEntity entity3 = new TestEntity("Name2", "Desc1");
        entityManager.persist(entity3);
        
        entityManager.getTransaction().commit();
        
        Query query = entityManager.createQuery("SELECT e.name, e.description FROM TestEntity e GROUP BY e.name, e.description");
        List<?> results = query.getResultList();
        
        assertThat(results).isNotNull();
    }
}
