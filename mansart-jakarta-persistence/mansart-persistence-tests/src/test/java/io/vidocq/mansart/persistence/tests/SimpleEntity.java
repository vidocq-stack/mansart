/*
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Minimal test entity for persistence context tests.
 * Has public getters/setters so the runtime Class-File API (tier-3)
 * can build an EntityModel and EntityAccessor for it.
 */
@Entity
public class SimpleEntity {

    @Id
    private Long id;

    @Column
    private String name;

    public SimpleEntity() {
    }

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
}
