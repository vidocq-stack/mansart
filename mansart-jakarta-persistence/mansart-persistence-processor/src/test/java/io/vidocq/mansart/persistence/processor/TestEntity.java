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
 * It is also made available under the European Union Public Licence v. 1.2,
 * which is available at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.processor;

import jakarta.persistence.*;
import java.util.List;

/**
 * Test entity for verifying static metamodel generation.
 */
@Entity
@Table(name = "TEST_ENTITIES")
public class TestEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Basic
    @Column(name = "NAME", length = 100)
    private String name;
    
    @Basic
    @Column(name = "AGE")
    private Integer age;
    
    @ManyToOne
    @JoinColumn(name = "PARENT_ID")
    private TestEntity parent;
    
    @OneToMany(mappedBy = "parent")
    private List<TestEntity> children;
    
    @Transient
    private String transientField;
    
    @Embedded
    private TestEmbeddable embeddedData;
    
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
    
    public Integer getAge() {
        return age;
    }
    
    public void setAge(Integer age) {
        this.age = age;
    }
    
    public TestEntity getParent() {
        return parent;
    }
    
    public void setParent(TestEntity parent) {
        this.parent = parent;
    }
    
    public List<TestEntity> getChildren() {
        return children;
    }
    
    public void setChildren(List<TestEntity> children) {
        this.children = children;
    }
    
    public TestEmbeddable getEmbeddedData() {
        return embeddedData;
    }
    
    public void setEmbeddedData(TestEmbeddable embeddedData) {
        this.embeddedData = embeddedData;
    }
    
    public String getTransientField() {
        return transientField;
    }
    
    public void setTransientField(String transientField) {
        this.transientField = transientField;
    }
}
