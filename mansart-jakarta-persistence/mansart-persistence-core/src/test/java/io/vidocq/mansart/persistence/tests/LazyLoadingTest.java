/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.tests.model.lazy.Employee;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Lazy Loading (M8-11).
 * Phase 1: Basic persist and find operations with lazy-loaded attributes.
 * Note: In Phase 1, lazy loading is not yet implemented at runtime,
 * but the metadata is parsed and stored for future implementation.
 */
public class LazyLoadingTest extends BasePersistenceTest {

    @Test
    public void testPersistAndFindEmployeeWithLazyField() {
        Employee employee = new Employee("John Doe", "Software Engineer");
        
        em.persist(employee);
        assertThat(employee.getId()).isNotNull();
        
        Employee found = em.find(Employee.class, employee.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("John Doe");
        // In Phase 1, description is still loaded (lazy loading not yet implemented)
        assertThat(found.getDescription()).isEqualTo("Software Engineer");
    }

    @Test
    public void testMultipleEmployeesWithLazyFields() {
        Employee emp1 = new Employee("Alice", "Manager");
        Employee emp2 = new Employee("Bob", "Developer");
        
        em.persist(emp1);
        em.persist(emp2);
        
        assertThat(emp1.getId()).isNotNull();
        assertThat(emp2.getId()).isNotNull();
        
        Employee found1 = em.find(Employee.class, emp1.getId());
        Employee found2 = em.find(Employee.class, emp2.getId());
        
        assertThat(found1).isNotNull();
        assertThat(found1.getName()).isEqualTo("Alice");
        assertThat(found2).isNotNull();
        assertThat(found2.getName()).isEqualTo("Bob");
    }

    @Test
    public void testUpdateLazyField() {
        Employee employee = new Employee("Charlie", "Intern");
        
        em.persist(employee);
        
        Employee found = em.find(Employee.class, employee.getId());
        assertThat(found).isNotNull();
        
        // Update the lazy field
        found.setDescription("Senior Developer");
        
        // Verify the update
        assertThat(found.getDescription()).isEqualTo("Senior Developer");
    }
}
