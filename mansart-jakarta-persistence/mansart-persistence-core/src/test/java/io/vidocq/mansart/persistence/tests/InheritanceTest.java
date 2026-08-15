/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.tests.model.inheritance.Employee;
import io.vidocq.mansart.persistence.tests.model.inheritance.Manager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SINGLE_TABLE inheritance (M8-7).
 * Phase 1: Basic persist and find operations with inheritance.
 */
public class InheritanceTest extends BasePersistenceTest {

    @Test
    public void testPersistAndFindEmployee() {
        Employee employee = new Employee("John Doe", "Engineering");
        
        em.persist(employee);
        assertThat(employee.getId()).isNotNull();
        
        // Find the persisted employee
        Employee found = em.find(Employee.class, employee.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("John Doe");
        assertThat(found.getDepartment()).isEqualTo("Engineering");
    }

    @Test
    public void testPersistAndFindManager() {
        Manager manager = new Manager("Jane Smith", "Management", "Dev Team", 5000);
        
        em.persist(manager);
        assertThat(manager.getId()).isNotNull();
        
        // Find the persisted manager
        Manager found = em.find(Manager.class, manager.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Jane Smith");
        assertThat(found.getDepartment()).isEqualTo("Management");
        assertThat(found.getTeam()).isEqualTo("Dev Team");
        assertThat(found.getBonus()).isEqualTo(5000);
    }

    @Test
    public void testPolymorphicPersist() {
        Employee employee = new Employee("John Doe", "Engineering");
        Manager manager = new Manager("Jane Smith", "Management", "Dev Team", 5000);
        
        em.persist(employee);
        em.persist(manager);
        
        assertThat(employee.getId()).isNotNull();
        assertThat(manager.getId()).isNotNull();
        assertThat(employee.getId()).isNotEqualTo(manager.getId());
        
        // Both should be findable
        Employee foundEmployee = em.find(Employee.class, employee.getId());
        Manager foundManager = em.find(Manager.class, manager.getId());
        
        assertThat(foundEmployee).isNotNull();
        assertThat(foundManager).isNotNull();
    }

    @Test
    public void testInheritanceHierarchy() {
        Manager manager = new Manager("Alice Johnson", "HR", "Recruitment", 3000);
        
        em.persist(manager);
        
        Manager found = em.find(Manager.class, manager.getId());
        assertThat(found).isNotNull();
        assertThat(found).isInstanceOf(Employee.class);
        assertThat(found.getName()).isEqualTo("Alice Johnson");
        assertThat(found.getDepartment()).isEqualTo("HR");
        assertThat(found.getTeam()).isEqualTo("Recruitment");
        assertThat(found.getBonus()).isEqualTo(3000);
    }
}
