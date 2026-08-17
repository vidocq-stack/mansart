/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.testentities.listener;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity with EntityListeners annotation.
 */
@Entity
@EntityListeners(AuditListener.class)
public class AuditedEntity {
    @Id
    private Long id;
    
    private String name;
    
    private static final List<String> entityLog = new ArrayList<>();
    
    public static void clearEntityLog() {
        entityLog.clear();
    }
    
    public static List<String> getEntityLog() {
        return new ArrayList<>(entityLog);
    }
    
    @PrePersist
    public void onEntityPrePersist() {
        entityLog.add("entity:prePersist");
    }
    
    @PostPersist
    public void onEntityPostPersist() {
        entityLog.add("entity:postPersist");
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
}
