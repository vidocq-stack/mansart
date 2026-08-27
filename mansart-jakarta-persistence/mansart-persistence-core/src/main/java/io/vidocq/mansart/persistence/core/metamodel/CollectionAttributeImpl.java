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

import jakarta.persistence.metamodel.CollectionAttribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Type;

/**
 * Implementation of {@link CollectionAttribute} for the Mansart persistence provider.
 *
 * <p>A collection attribute is a plural (collection) attribute with {@link
 * jakarta.persistence.metamodel.PluralAttribute.CollectionType#COLLECTION} semantics.
 * The interface itself is a marker — no methods beyond {@link
 * jakarta.persistence.metamodel.PluralAttribute} are added.</p>
 *
 * @param <X> the type the represented collection belongs to
 * @param <E> the element type of the represented collection
 */
public final class CollectionAttributeImpl<X, E> extends PluralAttributeImpl<X, java.util.Collection<E>, E>
        implements CollectionAttribute<X, E> {

    @SuppressWarnings("rawtypes")
    public CollectionAttributeImpl(Class<?> javaType, String name,
                             ManagedType<?> declaringType,
                             Type<E> elementType) {
        super(javaType, name, declaringType,
              jakarta.persistence.metamodel.PluralAttribute.CollectionType.COLLECTION, elementType);
    }

    /**
     * Constructor without declaring type — for bootstrap-time metamodel building.
     */
    @SuppressWarnings("rawtypes")
    public CollectionAttributeImpl(Class<?> javaType, String name,
                             Type<E> elementType) {
        super(javaType, name,
              jakarta.persistence.metamodel.PluralAttribute.CollectionType.COLLECTION, elementType);
    }
}
