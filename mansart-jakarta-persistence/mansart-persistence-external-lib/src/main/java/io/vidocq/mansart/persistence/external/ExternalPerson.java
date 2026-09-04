/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.external;

import jakarta.persistence.*;

/**
 * External entity for testing Tier 2 (Maven plugin) code generation.
 * This class is in a separate JAR that APT never sees during compilation,
 * so the Maven plugin must discover and enhance it.
 */
@Entity
@Table(name = "external_people")
public class ExternalPerson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = true, length = 255)
    private String description;

    @Version
    private int version;

    // Constructors
    public ExternalPerson() {
    }

    public ExternalPerson(String name, String description) {
        this.name = name;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "ExternalPerson{id=" + id + ", name='" + name + "', description='" + description + "', version=" + version + "}";
    }
}