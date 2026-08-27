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

import jakarta.persistence.metamodel.BasicType;
import jakarta.persistence.metamodel.Type;

/**
 * Implementation of {@link BasicType} for the Mansart persistence provider.
 *
 * <p>A basic type represents a basic (possibly enumerated, LOB, or temporal)
 * attribute — as opposed to an entity, embeddable, or collection.</p>
 *
 * @param <X> the represented Java type
 */
final class BasicTypeImpl<X> extends TypeImpl<X> implements BasicType<X> {

    @SuppressWarnings("rawtypes")
    BasicTypeImpl(Class<?> javaType, Type.PersistenceType persistenceType) {
        super(javaType, persistenceType);
    }
}
