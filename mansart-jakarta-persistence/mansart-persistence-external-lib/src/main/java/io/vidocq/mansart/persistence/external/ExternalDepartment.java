/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.external;

import jakarta.persistence.*;

/**
 * External entity with relationships for testing Tier 2 code generation.
 */
@Entity
@Table(name = "external_departments")
public class ExternalDepartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne
    @JoinColumn(name = "manager_id")
    private ExternalPerson manager;

    @Version
    private int version;

    // Constructors
    public ExternalDepartment() {
    }

    public ExternalDepartment(String name, ExternalPerson manager) {
        this.name = name;
        this.manager = manager;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ExternalPerson getManager() {
        return manager;
    }

    public void setManager(ExternalPerson manager) {
        this.manager = manager;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "ExternalDepartment{id=" + id + ", name='" + name + "', manager=" + manager + ", version=" + version + "}";
    }
}