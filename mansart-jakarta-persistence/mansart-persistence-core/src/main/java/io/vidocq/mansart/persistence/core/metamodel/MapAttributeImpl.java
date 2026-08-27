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
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.Type;

/**
 * Implementation of {@link MapAttribute} for the Mansart persistence provider.
 *
 * <p>A map attribute is a plural (collection) attribute with {@link
 * jakarta.persistence.metamodel.PluralAttribute.CollectionType#MAP} semantics,
 * plus key-type information via {@code getKeyType()} and {@code getKeyJavaType()}.</p>
 *
 * @param <X> the type the represented map belongs to
 * @param <K> the key type of the represented map
 * @param <V> the value type of the represented map
 */
final class MapAttributeImpl<X, K, V> extends PluralAttributeImpl<X, java.util.Map<K, V>, V>
        implements MapAttribute<X, K, V> {

    private final Type<K> keyType;
    private final Class<K> keyJavaType;

    @SuppressWarnings({"rawtypes", "unchecked"})
    MapAttributeImpl(Class<?> javaType, String name,
                     ManagedType<?> declaringType,
                     Type<K> keyType, Class<K> keyJavaType,
                     Type<V> elementType) {
        super(javaType, name, declaringType,
              jakarta.persistence.metamodel.PluralAttribute.CollectionType.MAP, elementType);
        this.keyType = keyType;
        this.keyJavaType = (Class<K>) keyJavaType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<K> getKeyJavaType() {
        return keyJavaType;
    }

    @Override
    public Type<K> getKeyType() {
        return keyType;
    }
}
