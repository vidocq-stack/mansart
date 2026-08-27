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
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.Type;

/**
 * Implementation of {@link SetAttribute} for the Mansart persistence provider.
 *
 * <p>A set attribute is a plural (collection) attribute with {@link
 * jakarta.persistence.metamodel.PluralAttribute.CollectionType#SET} semantics.
 * The interface itself is a marker — no methods beyond {@link
 * jakarta.persistence.metamodel.PluralAttribute} are added.</p>
 *
 * @param <X> the type the represented set belongs to
 * @param <E> the element type of the represented set
 */
public final class SetAttributeImpl<X, E> extends PluralAttributeImpl<X, java.util.Set<E>, E>
        implements SetAttribute<X, E> {

    @SuppressWarnings("rawtypes")
    public SetAttributeImpl(Class<?> javaType, String name,
                     ManagedType<?> declaringType,
                     Type<E> elementType) {
        super(javaType, name, declaringType,
              jakarta.persistence.metamodel.PluralAttribute.CollectionType.SET, elementType);
    }
}
