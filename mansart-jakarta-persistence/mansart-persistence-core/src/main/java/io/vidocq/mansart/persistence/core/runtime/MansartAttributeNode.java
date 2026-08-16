/*
 * Copyright (c) 2026 Vidocq contributors
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.runtime;

import jakarta.persistence.AttributeNode;
import jakarta.persistence.Subgraph;
import java.util.Collections;
import java.util.Map;

/**
 * Minimal stub implementation of AttributeNode for JPA 3.2.
 * Phase 1: Basic implementation for EntityGraph support.
 */
public class MansartAttributeNode<Y> implements AttributeNode<Y> {

    private final String attributeName;

    public MansartAttributeNode(String attributeName) {
        this.attributeName = attributeName;
    }

    @Override
    public String getAttributeName() {
        return attributeName;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Map<Class, Subgraph> getSubgraphs() {
        return Collections.emptyMap();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Map<Class, Subgraph> getKeySubgraphs() {
        return Collections.emptyMap();
    }

    @Override
    public String toString() {
        return "MansartAttributeNode{attributeName='" + attributeName + "'}";
    }
}
