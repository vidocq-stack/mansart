/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.testentities.listener;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity Listener class for testing.
 */
public class AuditListener {
    private static final List<String> listenerLog = new ArrayList<>();
    
    public static void clearListenerLog() {
        listenerLog.clear();
    }
    
    public static List<String> getListenerLog() {
        return new ArrayList<>(listenerLog);
    }
    
    @PrePersist
    public void onPrePersist(Object entity) {
        listenerLog.add("listener:prePersist");
    }
    
    @PostPersist
    public void onPostPersist(Object entity) {
        listenerLog.add("listener:postPersist");
    }
    
    @PreRemove
    public void onPreRemove(Object entity) {
        listenerLog.add("listener:preRemove");
    }
    
    @PostLoad
    public void onPostLoad(Object entity) {
        listenerLog.add("listener:postLoad");
    }
}
