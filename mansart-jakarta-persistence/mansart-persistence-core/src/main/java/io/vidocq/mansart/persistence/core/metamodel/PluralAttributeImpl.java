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
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.Type;
import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;

/**
 * Implementation of {@link PluralAttribute} for the Mansart persistence provider.
 *
 * @param <X> the type the represented collection belongs to
 * @param <C> the type of the represented collection
 * @param <E> the element type of the represented collection
 */
@SuppressWarnings("unchecked")
public class PluralAttributeImpl<X, C, E> extends AttributeImpl<X, C>
        implements PluralAttribute<X, C, E> {

    private final PluralAttribute.CollectionType collectionType;
    private final Type<E> elementType;

    /**
     * Creates a plural attribute.
     *
     * @param javaType the collection Java type (e.g. {@code java.util.Collection})
     * @param name the attribute name
     * @param declaringType the declaring managed type
     * @param collectionType the collection type (COLLECTION, SET, LIST, MAP)
     * @param elementType the element type
     */
    public PluralAttributeImpl(Class<?> javaType, String name,
                               ManagedType<?> declaringType,
                               PluralAttribute.CollectionType collectionType,
                               Type<E> elementType) {
        super(javaType, name, declaringType,
              PersistentAttributeType.BASIC, true);
        this.collectionType = collectionType;
        this.elementType = elementType;
    }

    @Override
    public PluralAttribute.CollectionType getCollectionType() {
        return collectionType;
    }

    @Override
    public Type<E> getElementType() {
        return elementType;
    }

    @Override
    public boolean isCollection() {
        return true;
    }

    @Override
    public Bindable.BindableType getBindableType() {
        return Bindable.BindableType.PLURAL_ATTRIBUTE;
    }

    @Override
    public Class<E> getBindableJavaType() {
        return elementType.getJavaType();
    }
}
