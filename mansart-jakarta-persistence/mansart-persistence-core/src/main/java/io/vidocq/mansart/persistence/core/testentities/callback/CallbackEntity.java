/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.testentities.callback;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity with lifecycle callback annotations for testing.
 */
@Entity
public class CallbackEntity {
    @Id
    private Long id;
    
    private String name;
    
    private static final List<String> callbackLog = new ArrayList<>();
    
    public static void clearCallbackLog() {
        callbackLog.clear();
    }
    
    public static List<String> getCallbackLog() {
        return new ArrayList<>(callbackLog);
    }
    
    @PrePersist
    public void prePersist() {
        callbackLog.add("prePersist");
    }
    
    @PostPersist
    public void postPersist() {
        callbackLog.add("postPersist");
    }
    
    @PostLoad
    public void postLoad() {
        callbackLog.add("postLoad");
    }
    
    @PreRemove
    public void preRemove() {
        callbackLog.add("preRemove");
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
