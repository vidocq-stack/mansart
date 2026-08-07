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

import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for JPQL ALL/ANY/SOME predicates implementation.
 * M6 - P1: Validate ALL/ANY/SOME support for Jakarta Persistence 3.2
 */
class AllAnySomeTest {

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
    void testParseAllWithSubquery() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT e FROM TestEntity e WHERE e.id > ALL (SELECT t.id FROM TestEntity t WHERE t.name = 'test')");
        
        assertThat(stmt).isNotNull();
        assertThat(stmt).isInstanceOf(JpqlSelectStmt.class);
        
        JpqlSelectStmt selectStmt = (JpqlSelectStmt) stmt;
        assertThat(selectStmt.hasWhere()).isTrue();
        
        JpqlPredicate predicate = selectStmt.whereClause().get().predicate();
        assertThat(predicate).isInstanceOf(JpqlAllAnySomePredicate.class);
        
        JpqlAllAnySomePredicate allAnySome = (JpqlAllAnySomePredicate) predicate;
        assertThat(allAnySome.quantifier()).isEqualTo(JpqlAllAnySomePredicate.Quantifier.ALL);
        assertThat(allAnySome.operator()).isEqualTo(JpqlPredicate.ComparisonOperator.GREATER_THAN);
        assertThat(allAnySome.hasSubquery()).isTrue();
    }

    @Test
    void testParseAnyWithSubquery() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT e FROM TestEntity e WHERE e.id = ANY (SELECT t.id FROM TestEntity t)");
        
        assertThat(stmt).isInstanceOf(JpqlSelectStmt.class);
        JpqlSelectStmt selectStmt = (JpqlSelectStmt) stmt;
        
        JpqlPredicate predicate = selectStmt.whereClause().get().predicate();
        assertThat(predicate).isInstanceOf(JpqlAllAnySomePredicate.class);
        
        JpqlAllAnySomePredicate allAnySome = (JpqlAllAnySomePredicate) predicate;
        assertThat(allAnySome.quantifier()).isEqualTo(JpqlAllAnySomePredicate.Quantifier.ANY);
        assertThat(allAnySome.operator()).isEqualTo(JpqlPredicate.ComparisonOperator.EQUAL);
    }

    @Test
    void testParseSomeWithSubquery() {
        JpqlStmt stmt = JpqlParser.parse(
            "SELECT e FROM TestEntity e WHERE e.id < SOME (SELECT t.id FROM TestEntity t)");
        
        assertThat(stmt).isInstanceOf(JpqlSelectStmt.class);
        JpqlSelectStmt selectStmt = (JpqlSelectStmt) stmt;
        
        JpqlPredicate predicate = selectStmt.whereClause().get().predicate();
        assertThat(predicate).isInstanceOf(JpqlAllAnySomePredicate.class);
        
        JpqlAllAnySomePredicate allAnySome = (JpqlAllAnySomePredicate) predicate;
        assertThat(allAnySome.quantifier()).isEqualTo(JpqlAllAnySomePredicate.Quantifier.SOME);
        assertThat(allAnySome.operator()).isEqualTo(JpqlPredicate.ComparisonOperator.LESS_THAN);
    }

    @Test
    void testParseAllWithDifferentOperators() {
        // Test all comparison operators with ALL
        String[] queries = {
            "SELECT e FROM TestEntity e WHERE e.id = ALL (SELECT t.id FROM TestEntity t)",
            "SELECT e FROM TestEntity e WHERE e.id <> ALL (SELECT t.id FROM TestEntity t)",
            "SELECT e FROM TestEntity e WHERE e.id < ALL (SELECT t.id FROM TestEntity t)",
            "SELECT e FROM TestEntity e WHERE e.id <= ALL (SELECT t.id FROM TestEntity t)",
            "SELECT e FROM TestEntity e WHERE e.id > ALL (SELECT t.id FROM TestEntity t)",
            "SELECT e FROM TestEntity e WHERE e.id >= ALL (SELECT t.id FROM TestEntity t)"
        };
        
        JpqlPredicate.ComparisonOperator[] expectedOperators = {
            JpqlPredicate.ComparisonOperator.EQUAL,
            JpqlPredicate.ComparisonOperator.NOT_EQUAL,
            JpqlPredicate.ComparisonOperator.LESS_THAN,
            JpqlPredicate.ComparisonOperator.LESS_THAN_OR_EQUAL,
            JpqlPredicate.ComparisonOperator.GREATER_THAN,
            JpqlPredicate.ComparisonOperator.GREATER_THAN_OR_EQUAL
        };
        
        for (int i = 0; i < queries.length; i++) {
            JpqlStmt stmt = JpqlParser.parse(queries[i]);
            JpqlSelectStmt selectStmt = (JpqlSelectStmt) stmt;
            JpqlPredicate predicate = selectStmt.whereClause().get().predicate();
            
            assertThat(predicate).isInstanceOf(JpqlAllAnySomePredicate.class);
            JpqlAllAnySomePredicate allAnySome = (JpqlAllAnySomePredicate) predicate;
            assertThat(allAnySome.operator()).isEqualTo(expectedOperators[i]);
        }
    }

    // ==================== CONVERSION TESTS ====================

    @Test
    void testConvertAllPredicate() {
        Query query = entityManager.createQuery(
            "SELECT e FROM TestEntity e WHERE e.id > ALL (SELECT t.id FROM TestEntity t)");
        assertThat(query).isNotNull();
    }

    @Test
    void testConvertAnyPredicate() {
        Query query = entityManager.createQuery(
            "SELECT e FROM TestEntity e WHERE e.id = ANY (SELECT t.id FROM TestEntity t)");
        assertThat(query).isNotNull();
    }

    @Test
    void testConvertSomePredicate() {
        Query query = entityManager.createQuery(
            "SELECT e FROM TestEntity e WHERE e.id < SOME (SELECT t.id FROM TestEntity t)");
        assertThat(query).isNotNull();
    }

    // ==================== EXECUTION TESTS ====================

    @Test
    void testExecuteAllPredicate() {
        entityManager.getTransaction().begin();
        
        // Create test data
        TestEntity entity1 = new TestEntity("Name1", "Desc1");
        TestEntity entity2 = new TestEntity("Name2", "Desc2");
        TestEntity entity3 = new TestEntity("Name3", "Desc3");
        entityManager.persist(entity1);
        entityManager.persist(entity2);
        entityManager.persist(entity3);
        
        entityManager.getTransaction().commit();
        
        // Query: find entities where id > ALL (subquery returning no rows)
        // This is a basic test to verify ALL predicate doesn't throw exceptions
        // Use a subquery that returns no rows by filtering on an impossible condition
        // We use a parameter with value 0 which won't match any entity since ids start at 1
        Query query = entityManager.createQuery(
            "SELECT e FROM TestEntity e WHERE e.id > ALL (SELECT t.id FROM TestEntity t WHERE t.id = :impossibleId)");
        query.setParameter("impossibleId", 0L);
        List<?> results = query.getResultList();
        
        assertThat(results).isNotNull();
        // When subquery returns no rows, ALL predicate returns TRUE for all rows
        // So all entities should be returned
        assertThat(results.size()).isEqualTo(3);
    }

    @Test
    void testExecuteAnyPredicate() {
        entityManager.getTransaction().begin();
        
        TestEntity entity1 = new TestEntity("Name1", "Desc1");
        TestEntity entity2 = new TestEntity("Name2", "Desc2");
        entityManager.persist(entity1);
        entityManager.persist(entity2);
        
        entityManager.getTransaction().commit();
        
        // Query: find entities where id = ANY (subquery returning all ids)
        Query query = entityManager.createQuery(
            "SELECT e FROM TestEntity e WHERE e.id = ANY (SELECT t.id FROM TestEntity t)");
        List<?> results = query.getResultList();
        
        assertThat(results).isNotNull();
        // ANY with a subquery that returns all ids should return all entities
        assertThat(results.size()).isEqualTo(2);
    }

    @Test
    void testExecuteSomePredicate() {
        entityManager.getTransaction().begin();
        
        TestEntity entity1 = new TestEntity("Name1", "Desc1");
        TestEntity entity2 = new TestEntity("Name2", "Desc2");
        entityManager.persist(entity1);
        entityManager.persist(entity2);
        
        entityManager.getTransaction().commit();
        
        // Query: find entities where id < SOME (subquery returning all ids)
        Query query = entityManager.createQuery(
            "SELECT e FROM TestEntity e WHERE e.id < SOME (SELECT t.id FROM TestEntity t)");
        List<?> results = query.getResultList();
        
        assertThat(results).isNotNull();
        // SOME is equivalent to ANY
        // This should return entities where id < any id in the subquery
        assertThat(results.size()).isGreaterThanOrEqualTo(1);
    }
}
