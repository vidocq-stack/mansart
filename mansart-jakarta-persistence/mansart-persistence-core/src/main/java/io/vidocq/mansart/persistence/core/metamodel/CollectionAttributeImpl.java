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
import jakarta.persistence.metamodel.CollectionAttribute;
import jakarta.persistence.metamodel.Type;
import jakarta.persistence.metamodel.PluralAttribute;

/**
 * Implementation of {@link CollectionAttribute} for the Mansart persistence provider.
 *
 * @param <X> the type the represented Collection belongs to
 * @param <E> the element type of the represented Collection
 */
@SuppressWarnings("unchecked")
public final class CollectionAttributeImpl<X, E> extends PluralAttributeImpl<X, java.util.Collection<E>, E>
        implements CollectionAttribute<X, E> {

    /**
     * Creates a collection attribute.
     *
     * @param name the attribute name
     * @param declaringType the declaring managed type
     * @param elementType the element type
     */
    public CollectionAttributeImpl(String name,
                                   ManagedType<?> declaringType,
                                   Type<E> elementType) {
        super(java.util.Collection.class, name, declaringType,
              PluralAttribute.CollectionType.COLLECTION, elementType);
    }
}
