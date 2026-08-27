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

import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Type;
import jakarta.persistence.metamodel.Attribute;

import java.util.List;

/**
 * Implementation of {@link EmbeddableType} for the Mansart persistence provider.
 *
 * @param <X> the represented embeddable type
 */
@SuppressWarnings("unchecked")
public final class EmbeddableTypeImpl<X> extends ManagedTypeImpl<X>
        implements EmbeddableType<X> {

    @SuppressWarnings("rawtypes")
    public EmbeddableTypeImpl(Class<?> javaType, List<Attribute<? super Object, ?>> declaredAttributes) {
        super(javaType, Type.PersistenceType.EMBEDDABLE, declaredAttributes);
    }
}
