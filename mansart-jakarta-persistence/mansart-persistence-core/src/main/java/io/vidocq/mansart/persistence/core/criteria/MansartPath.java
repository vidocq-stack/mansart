/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.criteria;

import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Bindable;

/**
 * Mansart implementation of Path for JPA 3.2.
 */
public class MansartPath<X> implements Path<X>, From<X, X>, Selection<X> {

    private final MansartCriteriaBuilder builder;
    private final Path<?> parent;
    private final String attributeName;
    private final Class<X> type;

    protected MansartPath(MansartCriteriaBuilder builder, Path<?> parent, String attributeName, Class<X> type) {
        this.builder = builder;
        this.parent = parent;
        this.attributeName = attributeName;
        this.type = type;
    }

    @Override public <Y> Expression<Y> get(String attributeName) {
        return new MansartPath<>(builder, this, attributeName, Object.class);
    }

    @Override public <Y> Expression<Y> get(String attributeName, Class<Y> type) {
        return new MansartPath<>(builder, this, attributeName, type);
    }

    @Override public <K, V> MapExpression<X, K, V> getMap(String attributeName, Class<K> keyJavaType, Class<V> valueJavaType) {
        return new MansartPath<>(builder, this, attributeName, Object.class);
    }

    @Override public <C extends java.util.Collection<?>> CollectionExpression<X, ?> getCollection(String attributeName) {
        return new MansartPath<>(builder, this, attributeName, Object.class);
    }

    @Override public <E, C extends java.util.Collection<E>> CollectionExpression<X, C, E> getCollection(String attributeName, Class<C> collectionClass, Class<E> elementClass) {
        return new MansartPath<>(builder, this, attributeName, Object.class);
    }

    @Override public <E, C extends java.util.Set<E>> SetExpression<X, C, E> getSet(String attributeName, Class<C> setClass, Class<E> elementClass) {
        return new MansartPath<>(builder, this, attributeName, Object.class);
    }

    @Override public <E, C extends java.util.List<E>> ListExpression<X, C, E> getList(String attributeName, Class<C> listClass, Class<E> elementClass) {
        return new MansartPath<>(builder, this, attributeName, Object.class);
    }

    @Override public Expression<?> get(SingularAttribute<? super X, ?> attr) {
        return new MansartPath<>(builder, this, attr.getName(), Object.class);
    }

    @Override public <E> Expression<E> get(PluralAttribute<? super X, ?, E> attr) {
        return new MansartPath<>(builder, this, attr.getName(), Object.class);
    }

    @Override public String getAlias() {
        return attributeName;
    }

    @Override public void setAlias(String alias) {
        // no-op
    }

    @Override public Class<X> getJavaType() {
        return type;
    }

    @Override public <N extends Number> Expression<N> getNumber() {
        return new MansartPath<>(builder, this, attributeName, Number.class);
    }

    @Override public Expression<String> getString() {
        return new MansartPath<>(builder, this, attributeName, String.class);
    }

    @Override public <T> Expression<T> as(Class<T> type) {
        return new MansartPath<>(builder, this, attributeName, type);
    }

    @Override public boolean isCompoundSelection() {
        return false;
    }

    @Override public List<? extends TupleElement<?>> getCompoundSelectionItems() {
        return java.util.Collections.emptyList();
    }

    @Override public <Y> Expression<Y> get(String attributeName, String... pathNames) {
        Expression<?> current = this;
        for (String name : pathNames) {
            current = new MansartPath<>(builder, current, name, Object.class);
        }
        return (Expression<Y>) current;
    }

    // From methods
    @Override public JoinType getJoinType() { return JoinType.INNER; }
    @Override public boolean isAll() { return false; }
    @Override public void setAll(boolean all) { }
    @Override public boolean isDistinct() { return false; }
    @Override public void setDistinct(boolean distinct) { }
    @Override public <V> From<X, V> on(Expression<Boolean> restriction) { return this; }
    @Override public <Y> Join<X, Y> join(String attribute) { return new MansartJoin<>(this, attribute); }
    @Override public <Y> Join<X, Y> join(String attribute, JoinType joinType) { return new MansartJoin<>(this, attribute); }
    @Override public <Y> Join<X, Y> join(SingularAttribute<? super X, Y> attribute) { return new MansartJoin<>(this, attribute.getName()); }
    @Override public <Y> Join<X, Y> join(SingularAttribute<? super X, Y> attribute, JoinType joinType) { return new MansartJoin<>(this, attribute.getName()); }
    @Override public <E, C extends java.util.Collection<E>> Join<X, E> joinCollection(String attribute) { return new MansartJoin<>(this, attribute); }
    @Override public <E, C extends java.util.Collection<E>> Join<X, E> joinCollection(String attribute, JoinType joinType) { return new MansartJoin<>(this, attribute); }
    @Override public <E, C extends java.util.Collection<E>> Join<X, E> joinCollection(SingularAttribute<? super X, C> attribute) { return new MansartJoin<>(this, attribute.getName()); }
    @Override public <E, C extends java.util.Collection<E>> Join<X, E> joinCollection(SingularAttribute<? super X, C> attribute, JoinType joinType) { return new MansartJoin<>(this, attribute.getName()); }
    @Override public <E, C extends java.util.Set<E>> SetJoin<X, E> joinSet(String attribute) { return new MansartJoin<>(this, attribute); }
    @Override public <E, C extends java.util.Set<E>> SetJoin<X, E> joinSet(String attribute, JoinType joinType) { return new MansartJoin<>(this, attribute); }
    @Override public <E, C extends java.util.Set<E>> SetJoin<X, E> joinSet(SingularAttribute<? super X, C> attribute) { return new MansartJoin<>(this, attribute.getName()); }
    @Override public <E, C extends java.util.Set<E>> SetJoin<X, E> joinSet(SingularAttribute<? super X, C> attribute, JoinType joinType) { return new MansartJoin<>(this, attribute.getName()); }
    @Override public <E, C extends java.util.List<E>> ListJoin<X, E> joinList(String attribute) { return new MansartJoin<>(this, attribute); }
    @Override public <E, C extends java.util.List<E>> ListJoin<X, E> joinList(String attribute, JoinType joinType) { return new MansartJoin<>(this, attribute); }
    @Override public <E, C extends java.util.List<E>> ListJoin<X, E> joinList(SingularAttribute<? super X, C> attribute) { return new MansartJoin<>(this, attribute.getName()); }
    @Override public <E, C extends java.util.List<E>> ListJoin<X, E> joinList(SingularAttribute<? super X, C> attribute, JoinType joinType) { return new MansartJoin<>(this, attribute.getName()); }
    @Override public <K, V, M extends java.util.Map<K, V>> MapJoin<X, K, V> joinMap(String attribute) { return new MansartJoin<>(this, attribute); }
    @Override public <K, V, M extends java.util.Map<K, V>> MapJoin<X, K, V> joinMap(String attribute, JoinType joinType) { return new MansartJoin<>(this, attribute); }
    @Override public <K, V, M extends java.util.Map<K, V>> MapJoin<X, K, V> joinMap(SingularAttribute<? super X, M> attribute) { return new MansartJoin<>(this, attribute.getName()); }
    @Override public <K, V, M extends java.util.Map<K, V>> MapJoin<X, K, V> joinMap(SingularAttribute<? super X, M> attribute, JoinType joinType) { return new MansartJoin<>(this, attribute.getName()); }
    @Override public <Y> Fetch<X, Y> fetch(String attribute) { return new MansartJoin<>(this, attribute); }
    @Override public <Y> Fetch<X, Y> fetch(String attribute, JoinType joinType) { return new MansartJoin<>(this, attribute); }
    @Override public <Y> Fetch<X, Y> fetch(SingularAttribute<? super X, Y> attribute) { return new MansartJoin<>(this, attribute.getName()); }
    @Override public <Y> Fetch<X, Y> fetch(SingularAttribute<? super X, Y> attribute, JoinType joinType) { return new MansartJoin<>(this, attribute.getName()); }
    @Override public java.util.Set<Join<X, ?>> getJoins() { return java.util.Collections.emptySet(); }
    @Override public Path<?> getParent() { return parent; }
    @Override public BindableType getBindableType() { return Bindable.BindableType.ENTITY_TYPE; }
}
