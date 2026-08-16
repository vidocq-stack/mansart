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
import java.util.List;
import java.util.Objects;

/**
 * Mansart implementation of EntityGraph for JPA 3.2.
 * Phase 1: Minimal implementation to support named entity graphs.
 */
public final class MansartEntityGraph<T> implements EntityGraph<T> {

    private final String name;

    /**
     * Creates a named entity graph.
     *
     * @param name the name of the graph
     */
    public MansartEntityGraph(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<AttributeNode<?>> getAttributeNodes() {
        return List.of();
    }

    // Graph interface methods - Phase 1 stubs

    @Override
    public <Y> AttributeNode<Y> addAttributeNode(String attributeName) {
        return null;
    }

    @Override
    public <Y> AttributeNode<Y> addAttributeNode(Attribute<? super T, Y> attribute) {
        return null;
    }

    @Override
    public boolean hasAttributeNode(String attributeName) {
        return false;
    }

    @Override
    public boolean hasAttributeNode(Attribute<? super T, ?> attribute) {
        return false;
    }

    @Override
    public <Y> AttributeNode<Y> getAttributeNode(String attributeName) {
        return null;
    }

    @Override
    public <Y> AttributeNode<Y> getAttributeNode(Attribute<? super T, Y> attribute) {
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
        throw new UnsupportedOperationException("EntityGraph is immutable in Phase 1");
    }

    @Override
    public void addAttributeNodes(Attribute<? super T, ?>... attributes) {
        throw new UnsupportedOperationException("EntityGraph is immutable in Phase 1");
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
