/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * or any later version, which is available at
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.tests;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Entity used by {@code MansartPersistenceProcessorTest} to verify that the
 * APT processor generates plural attribute fields for {@code @OneToMany} and
 * {@code @ManyToMany} relationships.
 *
 * <p>Generates {@code TestRelationshipEntity_} with:</p>
 * <ul>
 *   <li>{@code SetAttribute<TestRelationshipEntity, TestOrderItem> items} for @OneToMany(Set)</li>
 *   <li>{@code ListAttribute<TestRelationshipEntity, TestOrderItem> orderedItems} for @OneToMany(List)</li>
 *   <li>{@code SetAttribute<TestRelationshipEntity, TestOrderItem> tags} for @ManyToMany(Set)</li>
 * </ul>
 */
@Entity(name = "RelationshipEntity")
public class TestRelationshipEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany
    private Set<TestOrderItem> items = new HashSet<>();

    @OneToMany
    private List<TestOrderItem> orderedItems = new ArrayList<>();

    @ManyToMany
    private Set<TestOrderItem> tags = new HashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<TestOrderItem> getItems() { return items; }
    public void setItems(Set<TestOrderItem> items) { this.items = items; }

    public List<TestOrderItem> getOrderedItems() { return orderedItems; }
    public void setOrderedItems(List<TestOrderItem> orderedItems) { this.orderedItems = orderedItems; }

    public Set<TestOrderItem> getTags() { return tags; }
    public void setTags(Set<TestOrderItem> tags) { this.tags = tags; }
}
