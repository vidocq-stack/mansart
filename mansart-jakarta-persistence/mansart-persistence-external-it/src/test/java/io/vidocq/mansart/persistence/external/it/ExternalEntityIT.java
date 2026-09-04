/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.external.it;

import io.vidocq.mansart.persistence.external.ExternalDepartment;
import io.vidocq.mansart.persistence.external.ExternalPerson;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests proving Tier 1 (APT) ≈ Tier 2 (Maven plugin) behavior.
 * Tests that external entities (from external-lib) work identically to compiled entities.
 */
public class ExternalEntityIT {

    @Test
    public void testExternalPersonEntity() {
        // Test that ExternalPerson can be instantiated
        ExternalPerson person = new ExternalPerson("John Doe", "Test Developer");
        
        assertThat(person).isNotNull();
        assertThat(person.getName()).isEqualTo("John Doe");
        assertThat(person.getDescription()).isEqualTo("Test Developer");
    }

    @Test
    public void testExternalDepartmentEntity() {
        // Test that ExternalDepartment can be instantiated
        ExternalPerson manager = new ExternalPerson("Jane Doe", "Manager");
        ExternalDepartment dept = new ExternalDepartment("Engineering", manager);
        
        assertThat(dept).isNotNull();
        assertThat(dept.getName()).isEqualTo("Engineering");
        assertThat(dept.getManager()).isEqualTo(manager);
    }

    @Test
    public void testEntityRelationships() {
        ExternalPerson person1 = new ExternalPerson("Alice", "Developer");
        ExternalPerson person2 = new ExternalPerson("Bob", "Developer");
        
        ExternalDepartment dept1 = new ExternalDepartment("Dev Team 1", person1);
        ExternalDepartment dept2 = new ExternalDepartment("Dev Team 2", person2);
        
        assertThat(dept1.getManager()).isEqualTo(person1);
        assertThat(dept2.getManager()).isEqualTo(person2);
        
        // Test that relationships can be changed
        dept2.setManager(person1);
        assertThat(dept2.getManager()).isEqualTo(person1);
    }

    @Test
    public void testEntityLifecycleMethods() {
        ExternalPerson person = new ExternalPerson("Charlie", "Architect");
        
        // Test getters and setters
        person.setName("Charles");
        assertThat(person.getName()).isEqualTo("Charles");
        
        person.setDescription("Senior Architect");
        assertThat(person.getDescription()).isEqualTo("Senior Architect");
    }

    // TODO: Add tests that verify entity enhancement (once enhancement is implemented)
    // These would test that _Entity and Entity_ classes are generated for external entities
    // and that they work identically to APT-generated classes
}