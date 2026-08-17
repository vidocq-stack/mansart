/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import io.vidocq.mansart.persistence.core.testentities.model.inheritance.Car;
import io.vidocq.mansart.persistence.core.testentities.model.inheritance.Employee;
import io.vidocq.mansart.persistence.core.testentities.model.inheritance.Manager;
import io.vidocq.mansart.persistence.core.testentities.model.inheritance.Person;
import io.vidocq.mansart.persistence.core.testentities.model.inheritance.Student;
import io.vidocq.mansart.persistence.core.testentities.model.inheritance.Vehicle;
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

    // ========== JOINED Inheritance Tests (M8-8) ==========

    @Test
    public void testJoinedPersistAndFindPerson() {
        Person person = new Person("Bob Smith");
        
        em.persist(person);
        assertThat(person.getId()).isNotNull();
        
        // Find the persisted person
        Person found = em.find(Person.class, person.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Bob Smith");
    }

    @Test
    public void testJoinedPersistAndFindStudent() {
        Student student = new Student("Alice Brown", "Harvard", "Computer Science");
        
        em.persist(student);
        assertThat(student.getId()).isNotNull();
        
        // Find the persisted student
        Student found = em.find(Student.class, student.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Alice Brown");
        assertThat(found.getUniversity()).isEqualTo("Harvard");
        assertThat(found.getMajor()).isEqualTo("Computer Science");
    }

    @Test
    public void testJoinedPolymorphicPersist() {
        Person person = new Person("Charlie Wilson");
        Student student = new Student("Diana Prince", "MIT", "Physics");
        
        em.persist(person);
        em.persist(student);
        
        assertThat(person.getId()).isNotNull();
        assertThat(student.getId()).isNotNull();
        
        // Both should be findable
        Person foundPerson = em.find(Person.class, person.getId());
        Student foundStudent = em.find(Student.class, student.getId());
        
        assertThat(foundPerson).isNotNull();
        assertThat(foundStudent).isNotNull();
    }

    @Test
    public void testJoinedInheritanceHierarchy() {
        Student student = new Student("Eve Davis", "Stanford", "Mathematics");
        
        em.persist(student);
        
        Student found = em.find(Student.class, student.getId());
        assertThat(found).isNotNull();
        assertThat(found).isInstanceOf(Person.class);
        assertThat(found.getName()).isEqualTo("Eve Davis");
        assertThat(found.getUniversity()).isEqualTo("Stanford");
        assertThat(found.getMajor()).isEqualTo("Mathematics");
    }

    // ========== TABLE_PER_CLASS Inheritance Tests (M8-9) ==========

    @Test
    public void testTablePerClassPersistAndFindVehicle() {
        Vehicle vehicle = new Vehicle("Toyota");
        
        em.persist(vehicle);
        assertThat(vehicle.getId()).isNotNull();
        
        // Find the persisted vehicle
        Vehicle found = em.find(Vehicle.class, vehicle.getId());
        assertThat(found).isNotNull();
        assertThat(found.getManufacturer()).isEqualTo("Toyota");
    }

    @Test
    public void testTablePerClassPersistAndFindCar() {
        Car car = new Car("Honda", "Civic", 2023);
        
        em.persist(car);
        assertThat(car.getId()).isNotNull();
        
        // Find the persisted car
        Car found = em.find(Car.class, car.getId());
        assertThat(found).isNotNull();
        assertThat(found.getManufacturer()).isEqualTo("Honda");
        assertThat(found.getModel()).isEqualTo("Civic");
        assertThat(found.getYear()).isEqualTo(2023);
    }

    @Test
    public void testTablePerClassPolymorphicPersist() {
        Vehicle vehicle = new Vehicle("Ford");
        Car car = new Car("Tesla", "Model S", 2024);
        
        em.persist(vehicle);
        em.persist(car);
        
        assertThat(vehicle.getId()).isNotNull();
        assertThat(car.getId()).isNotNull();
        
        // Both should be findable
        Vehicle foundVehicle = em.find(Vehicle.class, vehicle.getId());
        Car foundCar = em.find(Car.class, car.getId());
        
        assertThat(foundVehicle).isNotNull();
        assertThat(foundCar).isNotNull();
    }

    @Test
    public void testTablePerClassInheritanceHierarchy() {
        Car car = new Car("BMW", "X5", 2023);
        
        em.persist(car);
        
        Car found = em.find(Car.class, car.getId());
        assertThat(found).isNotNull();
        assertThat(found).isInstanceOf(Vehicle.class);
        assertThat(found.getManufacturer()).isEqualTo("BMW");
        assertThat(found.getModel()).isEqualTo("X5");
        assertThat(found.getYear()).isEqualTo(2023);
    }
}
