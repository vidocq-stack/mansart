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

import jakarta.persistence.metamodel.Type;
import jakarta.persistence.metamodel.Type.PersistenceType;

/**
 * Base implementation of {@link Type} for the Mansart persistence provider.
 *
 * @param <X> the represented Java type
 */
public abstract class TypeImpl<X> implements Type<X> {

    private final Class<X> javaType;
    private final PersistenceType persistenceType;

    @SuppressWarnings("rawtypes")
    public TypeImpl(Class<?> javaType, PersistenceType persistenceType) {
        this.javaType = (Class<X>) javaType;
        this.persistenceType = persistenceType;
    }

    @Override
    public PersistenceType getPersistenceType() {
        return persistenceType;
    }

    @Override
    public Class<X> getJavaType() {
        return javaType;
    }
}
