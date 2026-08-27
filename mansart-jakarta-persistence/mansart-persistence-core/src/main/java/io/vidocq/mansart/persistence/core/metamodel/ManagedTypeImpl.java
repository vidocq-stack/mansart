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
        return (Set) Set.copyOf(declaredAttributes);
    }

    @Override
    public <Y> SingularAttribute<? super X, Y> getSingularAttribute(String name, Class<Y> type) {
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name) && attr instanceof SingularAttribute<?, ?> sa) {
                if (sa.getJavaType().equals(type)) {
                    return (SingularAttribute<? super X, Y>) sa;
                }
            }
        }
        throw new IllegalArgumentException("No singular attribute named: " + name + " with type: " + type);
    }

    @Override
    public <Y> SingularAttribute<X, Y> getDeclaredSingularAttribute(String name, Class<Y> type) {
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name) && attr instanceof SingularAttribute<?, ?> sa) {
                if (sa.getJavaType().equals(type)) {
                    return (SingularAttribute<X, Y>) attr;
                }
            }
        }
        throw new IllegalArgumentException("No declared singular attribute named: " + name + " with type: " + type);
    }

    @Override
    public Set<SingularAttribute<? super X, ?>> getSingularAttributes() {
        return declaredAttributes.stream()
                .filter(attr -> attr instanceof SingularAttribute<?, ?>)
                .map(attr -> (SingularAttribute<? super X, ?>) attr)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<SingularAttribute<X, ?>> getDeclaredSingularAttributes() {
        return (Set) getSingularAttributes();
    }

    @Override
    public <E> CollectionAttribute<? super X, E> getCollection(String name, Class<E> elementType) {
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name)) {
                if (attr instanceof CollectionAttribute<?, ?> ca) {
                    if (ca.getElementType().getJavaType().equals(elementType)) {
                        return (CollectionAttribute<? super X, E>) attr;
                    }
                } else if (attr instanceof SetAttribute<?, ?> sa) {
                    if (sa.getElementType().getJavaType().equals(elementType)) {
                        @SuppressWarnings("unchecked")
                        var casted = (CollectionAttribute<? super X, E>) (Object) attr;
                        return casted;
                    }
                } else if (attr instanceof ListAttribute<?, ?> la) {
                    if (la.getElementType().getJavaType().equals(elementType)) {
                        @SuppressWarnings("unchecked")
                        var casted = (CollectionAttribute<? super X, E>) (Object) attr;
                        return casted;
                    }
                }
            }
        }
        throw new IllegalArgumentException("No collection attribute named: " + name + " with element type: " + elementType);
    }

    @Override
    public <E> CollectionAttribute<X, E> getDeclaredCollection(String name, Class<E> elementType) {
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name)) {
                if (attr instanceof CollectionAttribute<?, ?> ca) {
                    if (ca.getElementType().getJavaType().equals(elementType)) {
                        return (CollectionAttribute<X, E>) attr;
                    }
                } else if (attr instanceof SetAttribute<?, ?> sa) {
                    if (sa.getElementType().getJavaType().equals(elementType)) {
                        return (CollectionAttribute<X, E>) attr;
                    }
                } else if (attr instanceof ListAttribute<?, ?> la) {
                    if (la.getElementType().getJavaType().equals(elementType)) {
                        return (CollectionAttribute<X, E>) attr;
                    }
                }
            }
        }
        throw new IllegalArgumentException("No declared collection attribute named: " + name + " with element type: " + elementType);
    }

    @Override
    public <E> SetAttribute<? super X, E> getSet(String name, Class<E> elementType) {
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name) && attr instanceof SetAttribute<?, ?> sa) {
                if (sa.getElementType().getJavaType().equals(elementType)) {
                    return (SetAttribute<? super X, E>) sa;
                }
            }
        }
        throw new IllegalArgumentException("No set attribute named: " + name + " with element type: " + elementType);
    }

    @Override
    public <E> SetAttribute<X, E> getDeclaredSet(String name, Class<E> elementType) {
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name) && attr instanceof SetAttribute<?, ?> sa) {
                if (sa.getElementType().getJavaType().equals(elementType)) {
                    return (SetAttribute<X, E>) attr;
                }
            }
        }
        throw new IllegalArgumentException("No declared set attribute named: " + name + " with element type: " + elementType);
    }

    @Override
    public <E> ListAttribute<? super X, E> getList(String name, Class<E> elementType) {
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name) && attr instanceof ListAttribute<?, ?> la) {
                if (la.getElementType().getJavaType().equals(elementType)) {
                    return (ListAttribute<? super X, E>) la;
                }
            }
        }
        throw new IllegalArgumentException("No list attribute named: " + name + " with element type: " + elementType);
    }

    @Override
    public <E> ListAttribute<X, E> getDeclaredList(String name, Class<E> elementType) {
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name) && attr instanceof ListAttribute<?, ?> la) {
                if (la.getElementType().getJavaType().equals(elementType)) {
                    return (ListAttribute<X, E>) attr;
                }
            }
        }
        throw new IllegalArgumentException("No declared list attribute named: " + name + " with element type: " + elementType);
    }

    @Override
    public <K, V> MapAttribute<? super X, K, V> getMap(String name, Class<K> keyType, Class<V> valueType) {
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name) && attr instanceof MapAttribute<?, ?, ?> ma) {
                if (ma.getKeyJavaType().equals(keyType) && ma.getElementType().getJavaType().equals(valueType)) {
                    return (MapAttribute<? super X, K, V>) ma;
                }
            }
        }
        throw new IllegalArgumentException("No map attribute named: " + name + " with key type: " + keyType + ", value type: " + valueType);
    }

    @Override
    public <K, V> MapAttribute<X, K, V> getDeclaredMap(String name, Class<K> keyType, Class<V> valueType) {
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name) && attr instanceof MapAttribute<?, ?, ?> ma) {
                if (ma.getKeyJavaType().equals(keyType) && ma.getElementType().getJavaType().equals(valueType)) {
                    return (MapAttribute<X, K, V>) attr;
                }
            }
        }
        throw new IllegalArgumentException("No declared map attribute named: " + name + " with key type: " + keyType + ", value type: " + valueType);
    }

    @Override
    public Set<PluralAttribute<? super X, ?, ?>> getPluralAttributes() {
        return declaredAttributes.stream()
                .filter(attr -> attr instanceof PluralAttribute<?, ?, ?>)
                .map(attr -> (PluralAttribute<? super X, ?, ?>) attr)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<PluralAttribute<X, ?, ?>> getDeclaredPluralAttributes() {
        return (Set) getPluralAttributes();
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
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name)) {
                if (attr instanceof CollectionAttribute<?, ?> ca) {
                    return (CollectionAttribute<? super X, ?>) ca;
                }
                if (attr instanceof SetAttribute<?, ?>) {
                    @SuppressWarnings("unchecked")
                    var casted = (CollectionAttribute<? super X, ?>) (Object) attr;
                    return casted;
                }
                if (attr instanceof ListAttribute<?, ?>) {
                    @SuppressWarnings("unchecked")
                    var casted = (CollectionAttribute<? super X, ?>) (Object) attr;
                    return casted;
                }
            }
        }
        throw new IllegalArgumentException("No collection attribute named: " + name);
    }

    @Override
    public CollectionAttribute<X, ?> getDeclaredCollection(String name) {
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name)) {
                if (attr instanceof CollectionAttribute<?, ?>) {
                    return (CollectionAttribute<X, ?>) attr;
                }
                if (attr instanceof SetAttribute<?, ?>) {
                    return (CollectionAttribute<X, ?>) attr;
                }
                if (attr instanceof ListAttribute<?, ?>) {
                    return (CollectionAttribute<X, ?>) attr;
                }
            }
        }
        throw new IllegalArgumentException("No declared collection attribute named: " + name);
    }

    @Override
    public SetAttribute<? super X, ?> getSet(String name) {
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name) && attr instanceof SetAttribute<?, ?>) {
                return (SetAttribute<? super X, ?>) attr;
            }
        }
        throw new IllegalArgumentException("No set attribute named: " + name);
    }

    @Override
    public SetAttribute<X, ?> getDeclaredSet(String name) {
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name) && attr instanceof SetAttribute<?, ?>) {
                return (SetAttribute<X, ?>) attr;
            }
        }
        throw new IllegalArgumentException("No declared set attribute named: " + name);
    }

    @Override
    public ListAttribute<? super X, ?> getList(String name) {
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name) && attr instanceof ListAttribute<?, ?>) {
                return (ListAttribute<? super X, ?>) attr;
            }
        }
        throw new IllegalArgumentException("No list attribute named: " + name);
    }

    @Override
    public ListAttribute<X, ?> getDeclaredList(String name) {
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name) && attr instanceof ListAttribute<?, ?>) {
                return (ListAttribute<X, ?>) attr;
            }
        }
        throw new IllegalArgumentException("No declared list attribute named: " + name);
    }

    @Override
    public MapAttribute<? super X, ?, ?> getMap(String name) {
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name) && attr instanceof MapAttribute<?, ?, ?>) {
                return (MapAttribute<? super X, ?, ?>) attr;
            }
        }
        throw new IllegalArgumentException("No map attribute named: " + name);
    }

    @Override
    public MapAttribute<X, ?, ?> getDeclaredMap(String name) {
        for (Attribute<? super X, ?> attr : declaredAttributes) {
            if (attr.getName().equals(name) && attr instanceof MapAttribute<?, ?, ?>) {
                return (MapAttribute<X, ?, ?>) attr;
            }
        }
        throw new IllegalArgumentException("No declared map attribute named: " + name);
    }
}
