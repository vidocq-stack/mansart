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

import jakarta.persistence.metamodel.Bindable;

/**
 * Base implementation of {@link Bindable} for the Mansart persistence provider.
 *
 * @param <T> the represented Java type
 */
public abstract class BindableImpl<T> extends TypeImpl<T> implements Bindable<T> {

    private final BindableType bindableType;

    @SuppressWarnings("rawtypes")
    BindableImpl(Class<?> javaType, BindableType bindableType) {
        super(javaType, null);
        this.bindableType = bindableType;
    }

    @Override
    public BindableType getBindableType() {
        return bindableType;
    }

    @Override
    public Class<T> getBindableJavaType() {
        return getJavaType();
    }
}
