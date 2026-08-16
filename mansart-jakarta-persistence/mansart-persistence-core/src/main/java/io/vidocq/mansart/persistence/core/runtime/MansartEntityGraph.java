/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.runtime;

import jakarta.persistence.AttributeNode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.Subgraph;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import java.util.ArrayList;
import java.util.List;

/**
 * Mansart implementation of EntityGraph for JPA 3.2.
 * Phase 1: Minimal implementation to support named entity graphs.
 */
public final class MansartEntityGraph<T> implements EntityGraph<T> {

    private final String name;
    private final List<String> attributeNodeNames = new ArrayList<>();

    /**
     * Creates a named entity graph.
     *
     * @param name the name of the graph (can be null for unnamed graphs)
     */
    public MansartEntityGraph(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<AttributeNode<?>> getAttributeNodes() {
        // For Phase 1, return a list of AttributeNode wrappers for the attribute names
        List<AttributeNode<?>> result = new ArrayList<>();
        for (String attrName : attributeNodeNames) {
            result.add(new MansartAttributeNode<>(attrName));
        }
        return result;
    }

    // Graph interface methods - Phase 1 stubs

    @Override
    public <Y> AttributeNode<Y> addAttributeNode(String attributeName) {
        if (attributeName != null && !attributeNodeNames.contains(attributeName)) {
            attributeNodeNames.add(attributeName);
        }
        return new MansartAttributeNode<>(attributeName);
    }

    @Override
    public <Y> AttributeNode<Y> addAttributeNode(Attribute<? super T, Y> attribute) {
        // Phase 1: accept attribute but use its name
        if (attribute != null) {
            String name = attribute.getName();
            if (!attributeNodeNames.contains(name)) {
                attributeNodeNames.add(name);
            }
            return new MansartAttributeNode<>(name);
        }
        return null;
    }

    @Override
    public boolean hasAttributeNode(String attributeName) {
        return attributeName != null && attributeNodeNames.contains(attributeName);
    }

    @Override
    public boolean hasAttributeNode(Attribute<? super T, ?> attribute) {
        return attribute != null && hasAttributeNode(attribute.getName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Y> AttributeNode<Y> getAttributeNode(String attributeName) {
        if (attributeName != null && attributeNodeNames.contains(attributeName)) {
            return (AttributeNode<Y>) new MansartAttributeNode<>(attributeName);
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Y> AttributeNode<Y> getAttributeNode(Attribute<? super T, Y> attribute) {
        if (attribute != null) {
            return (AttributeNode<Y>) getAttributeNode(attribute.getName());
        }
        return null;
    }

    @Override
    public void removeAttributeNode(String attributeName) {
        throw new UnsupportedOperationException("EntityGraph is immutable in Phase 1");
    }

    @Override
    public void removeAttributeNode(Attribute<? super T, ?> attribute) {
        throw new UnsupportedOperationException("EntityGraph is immutable in Phase 1");
    }

    @Override
    public void removeAttributeNodes(Attribute.PersistentAttributeType attributeType) {
        throw new UnsupportedOperationException("EntityGraph is immutable in Phase 1");
    }

    @Override
    public void addAttributeNodes(String... attributeNames) {
        // JPA spec: if attributeNames is null or contains null, throw IAE
        if (attributeNames == null) {
            throw new IllegalArgumentException("attributeNames cannot be null");
        }
        for (String attrName : attributeNames) {
            if (attrName == null) {
                throw new IllegalArgumentException("attributeName cannot be null");
            }
            if (!attributeNodeNames.contains(attrName)) {
                attributeNodeNames.add(attrName);
            }
        }
    }

    @Override
    public void addAttributeNodes(Attribute<? super T, ?>... attributes) {
        if (attributes == null) {
            throw new IllegalArgumentException("attributes cannot be null");
        }
        for (Attribute<? super T, ?> attr : attributes) {
            if (attr == null) {
                throw new IllegalArgumentException("attribute cannot be null");
            }
            String name = attr.getName();
            if (!attributeNodeNames.contains(name)) {
                attributeNodeNames.add(name);
            }
        }
    }

    @Override
    public <X> Subgraph<X> addSubgraph(Attribute<? super T, X> attribute) {
        return null;
    }

    @Override
    public <Y> Subgraph<Y> addTreatedSubgraph(Attribute<? super T, ? super Y> attribute, Class<Y> type) {
        return null;
    }

    @Override
    public <X> Subgraph<X> addSubgraph(Attribute<? super T, X> attribute, Class<? extends X> type) {
        return null;
    }

    @Override
    public <X> Subgraph<X> addSubgraph(String attributeName) {
        return null;
    }

    @Override
    public <X> Subgraph<X> addSubgraph(String attributeName, Class<X> type) {
        return null;
    }

    @Override
    public <E> Subgraph<E> addElementSubgraph(PluralAttribute<? super T, ?, E> attribute) {
        return null;
    }

    @Override
    public <E> Subgraph<E> addTreatedElementSubgraph(PluralAttribute<? super T, ?, ? super E> attribute, Class<E> type) {
        return null;
    }

    @Override
    public <X> Subgraph<X> addElementSubgraph(String attributeName) {
        return null;
    }

    @Override
    public <X> Subgraph<X> addElementSubgraph(String attributeName, Class<X> type) {
        return null;
    }

    @Override
    public <K> Subgraph<K> addMapKeySubgraph(MapAttribute<? super T, K, ?> attribute) {
        return null;
    }

    @Override
    public <K> Subgraph<K> addTreatedMapKeySubgraph(MapAttribute<? super T, ? super K, ?> attribute, Class<K> type) {
        return null;
    }

    @Override
    public <X> Subgraph<X> addKeySubgraph(Attribute<? super T, X> attribute) {
        return null;
    }

    @Override
    public <X> Subgraph<? extends X> addKeySubgraph(Attribute<? super T, X> attribute, Class<? extends X> type) {
        return null;
    }

    @Override
    public <X> Subgraph<X> addKeySubgraph(String attributeName) {
        return null;
    }

    @Override
    public <X> Subgraph<X> addKeySubgraph(String attributeName, Class<X> type) {
        return null;
    }

    // EntityGraph-specific methods
    @Override
    public <S extends T> Subgraph<S> addTreatedSubgraph(Class<S> type) {
        return null;
    }

    @Override
    public <T> Subgraph<? extends T> addSubclassSubgraph(Class<? extends T> type) {
        return null;
    }

    @Override
    public String toString() {
        return "MansartEntityGraph{name='" + name + "'}";
    }
}
