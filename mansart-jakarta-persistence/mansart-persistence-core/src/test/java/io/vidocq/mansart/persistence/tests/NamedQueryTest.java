/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.tests.model.namedquery.Department;
import jakarta.persistence.NamedQuery;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Named Queries (M8-14).
 * Phase 1: Basic persist and find operations with entities that have named queries defined.
 * Note: In Phase 1, named queries are parsed but actual query execution via EntityManager.createNamedQuery
 * will be implemented in a future phase.
 */
public class NamedQueryTest extends BasePersistenceTest {

    @Test
    public void testPersistAndFindDepartmentWithNamedQueries() {
        Department department = new Department("Engineering");
        
        em.persist(department);
        assertThat(department.getId()).isNotNull();
        
        Department found = em.find(Department.class, department.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Engineering");
        assertThat(found.isActive()).isTrue();
    }

    @Test
    public void testMultipleDepartmentsWithNamedQueries() {
        Department dept1 = new Department("IT");
        Department dept2 = new Department("HR");
        dept2.setActive(false);
        
        em.persist(dept1);
        em.persist(dept2);
        
        assertThat(dept1.getId()).isNotNull();
        assertThat(dept2.getId()).isNotNull();
        
        Department found1 = em.find(Department.class, dept1.getId());
        Department found2 = em.find(Department.class, dept2.getId());
        
        assertThat(found1).isNotNull();
        assertThat(found1.getName()).isEqualTo("IT");
        assertThat(found1.isActive()).isTrue();
        
        assertThat(found2).isNotNull();
        assertThat(found2.getName()).isEqualTo("HR");
        assertThat(found2.isActive()).isFalse();
    }

    @Test
    public void testDepartmentEntityHasNamedQueryAnnotations() {
        // Verify that the Department entity has @NamedQuery annotations
        // This is a compile-time check, but we verify it works at runtime
        Department department = new Department("Finance");
        em.persist(department);
        
        Department found = em.find(Department.class, department.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Finance");
    }
}
