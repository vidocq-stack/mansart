/*
 * Copyright (c) 2026 Yann Blazart, Antoine Sabot-Durand and the Vidocq contributors
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0 OR EUPL-1.2 OR GPL-2.0-or-later
 */
package io.vidocq.mansart.persistence.core.metamodel;

import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.CollectionAttribute;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Type;
import jakarta.persistence.metamodel.Type.PersistenceType;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * Base implementation of {@link ManagedType} for the Mansart persistence provider.
 *
 * @param <X> the represented type
 */
@SuppressWarnings("unchecked")
public abstract class ManagedTypeImpl<X> extends TypeImpl<X> implements ManagedType<X> {

    private final List<Attribute<? super X, ?>> declaredAttributes;

    @SuppressWarnings("rawtypes")
    ManagedTypeImpl(Class<?> javaType, PersistenceType persistenceType,
                    List<Attribute<? super Object, ?>> declaredAttributes) {
        super((Class<X>) javaType, persistenceType);
        this.declaredAttributes = declaredAttributes != null
                ? new ArrayList<>(declaredAttributes) : new ArrayList<>();
    }

    @Override
    public Set<Attribute<? super X, ?>> getAttributes() {
        return (Set) declaredAttributes;
    }

    @Override
    public Set<Attribute<X, ?>> getDeclaredAttributes() {
        return (Set) declaredAttributes;
    }

    @Override
    public <Y> SingularAttribute<? super X, Y> getSingularAttribute(String name, Class<Y> type) {
        throw new UnsupportedOperationException("not implemented: getSingularAttribute(String, Class)");
    }

    @Override
    public <Y> SingularAttribute<X, Y> getDeclaredSingularAttribute(String name, Class<Y> type) {
        throw new UnsupportedOperationException("not implemented: getDeclaredSingularAttribute(String, Class)");
    }

    @Override
    public Set<SingularAttribute<? super X, ?>> getSingularAttributes() {
        return (Set) declaredAttributes;
    }

    @Override
    public Set<SingularAttribute<X, ?>> getDeclaredSingularAttributes() {
        return (Set) declaredAttributes;
    }

    @Override
    public <E> CollectionAttribute<? super X, E> getCollection(String name, Class<E> elementType) {
        throw new UnsupportedOperationException("not implemented: getCollection(String, Class)");
    }

    @Override
    public <E> CollectionAttribute<X, E> getDeclaredCollection(String name, Class<E> elementType) {
        throw new UnsupportedOperationException("not implemented: getDeclaredCollection(String, Class)");
    }

    @Override
    public <E> SetAttribute<? super X, E> getSet(String name, Class<E> elementType) {
        throw new UnsupportedOperationException("not implemented: getSet(String, Class)");
    }

    @Override
    public <E> SetAttribute<X, E> getDeclaredSet(String name, Class<E> elementType) {
        throw new UnsupportedOperationException("not implemented: getDeclaredSet(String, Class)");
    }

    @Override
    public <E> ListAttribute<? super X, E> getList(String name, Class<E> elementType) {
        throw new UnsupportedOperationException("not implemented: getList(String, Class)");
    }

    @Override
    public <E> ListAttribute<X, E> getDeclaredList(String name, Class<E> elementType) {
        throw new UnsupportedOperationException("not implemented: getDeclaredList(String, Class)");
    }

    @Override
    public <K, V> MapAttribute<? super X, K, V> getMap(String name, Class<K> keyType, Class<V> valueType) {
        throw new UnsupportedOperationException("not implemented: getMap(String, Class, Class)");
    }

    @Override
    public <K, V> MapAttribute<X, K, V> getDeclaredMap(String name, Class<K> keyType, Class<V> valueType) {
        throw new UnsupportedOperationException("not implemented: getDeclaredMap(String, Class, Class)");
    }

    @Override
    public Set<PluralAttribute<? super X, ?, ?>> getPluralAttributes() {
        return Set.of();
    }

    @Override
    public Set<PluralAttribute<X, ?, ?>> getDeclaredPluralAttributes() {
        return Set.of();
    }

    @Override
    public Attribute<? super X, ?> getAttribute(String name) {
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name)) {
                return attr;
            }
        }
        throw new IllegalArgumentException("No attribute named: " + name);
    }

    @Override
    public Attribute<X, ?> getDeclaredAttribute(String name) {
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name)) {
                return (Attribute<X, ?>) attr;
            }
        }
        throw new IllegalArgumentException("No declared attribute named: " + name);
    }

    @Override
    public SingularAttribute<? super X, ?> getSingularAttribute(String name) {
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name)) {
                return (SingularAttribute<? super X, ?>) attr;
            }
        }
        throw new IllegalArgumentException("No singular attribute named: " + name);
    }

    @Override
    public SingularAttribute<X, ?> getDeclaredSingularAttribute(String name) {
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name)) {
                return (SingularAttribute<X, ?>) attr;
            }
        }
        throw new IllegalArgumentException("No declared singular attribute named: " + name);
    }

    @Override
    public CollectionAttribute<? super X, ?> getCollection(String name) {
        throw new UnsupportedOperationException("not implemented: getCollection(String)");
    }

    @Override
    public CollectionAttribute<X, ?> getDeclaredCollection(String name) {
        throw new UnsupportedOperationException("not implemented: getDeclaredCollection(String)");
    }

    @Override
    public SetAttribute<? super X, ?> getSet(String name) {
        throw new UnsupportedOperationException("not implemented: getSet(String)");
    }

    @Override
    public SetAttribute<X, ?> getDeclaredSet(String name) {
        throw new UnsupportedOperationException("not implemented: getDeclaredSet(String)");
    }

    @Override
    public ListAttribute<? super X, ?> getList(String name) {
        throw new UnsupportedOperationException("not implemented: getList(String)");
    }

    @Override
    public ListAttribute<X, ?> getDeclaredList(String name) {
        throw new UnsupportedOperationException("not implemented: getDeclaredList(String)");
    }

    @Override
    public MapAttribute<? super X, ?, ?> getMap(String name) {
        throw new UnsupportedOperationException("not implemented: getMap(String)");
    }

    @Override
    public MapAttribute<X, ?, ?> getDeclaredMap(String name) {
        throw new UnsupportedOperationException("not implemented: getDeclaredMap(String)");
    }
}
