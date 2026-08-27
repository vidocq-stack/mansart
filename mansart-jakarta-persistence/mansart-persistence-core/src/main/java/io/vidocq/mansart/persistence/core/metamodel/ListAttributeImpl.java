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

import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.Type;

/**
 * Implementation of {@link ListAttribute} for the Mansart persistence provider.
 *
 * <p>A list attribute is a plural (collection) attribute with {@link
 * PluralAttribute.CollectionType#LIST} ordering. The interface itself is a
 * marker — no methods beyond {@link PluralAttribute} are added.</p>
 *
 * @param <X> the type the represented list belongs to
 * @param <E> the element type of the represented list
 */
public final class ListAttributeImpl<X, E> extends PluralAttributeImpl<X, java.util.List<E>, E>
        implements ListAttribute<X, E> {

    @SuppressWarnings("rawtypes")
    public ListAttributeImpl(Class<?> javaType, String name,
                      ManagedType<?> declaringType,
                      Type<E> elementType) {
        super(javaType, name, declaringType,
              PluralAttribute.CollectionType.LIST, elementType);
    }
}
