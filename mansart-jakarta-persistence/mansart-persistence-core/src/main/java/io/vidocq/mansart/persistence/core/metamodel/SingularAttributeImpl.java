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

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;
import jakarta.persistence.metamodel.Type;
import jakarta.persistence.metamodel.Bindable;

/**
 * Implementation of {@link SingularAttribute} for the Mansart persistence provider.
 *
 * @param <X> the type containing the represented attribute
 * @param <T> the type of the represented attribute
 */
@SuppressWarnings("unchecked")
public final class SingularAttributeImpl<X, T> extends AttributeImpl<X, T>
        implements SingularAttribute<X, T>, Bindable<T> {

    private final boolean id;
    private final boolean version;
    private final Type<T> type;

    @SuppressWarnings("rawtypes")
    public SingularAttributeImpl(Class<?> javaType, String name,
                           ManagedType<?> declaringType,
                           PersistentAttributeType persistentAttributeType,
                           boolean optional, boolean id, boolean version,
                           Type<?> type) {
        super((Class<T>) javaType, name, (ManagedType<X>) declaringType,
              persistentAttributeType, optional);
        this.id = id;
        this.version = version;
        this.type = (Type<T>) type;
    }

    /**
     * Constructor without declaring type — for bootstrap-time metamodel building.
     */
    @SuppressWarnings("rawtypes")
    public SingularAttributeImpl(Class<?> javaType, String name,
                           PersistentAttributeType persistentAttributeType,
                           boolean optional, boolean id, boolean version,
                           Type<?> type) {
        super((Class<T>) javaType, name, persistentAttributeType, optional);
        this.id = id;
        this.version = version;
        this.type = (Type<T>) type;
    }

    @Override
    public boolean isId() {
        return id;
    }

    @Override
    public boolean isVersion() {
        return version;
    }

    @Override
    public boolean isOptional() {
        return optional;
    }

    @Override
    public Type<T> getType() {
        return type;
    }

    @Override
    public Bindable.BindableType getBindableType() {
        return Bindable.BindableType.SINGULAR_ATTRIBUTE;
    }

    @Override
    public Class<T> getBindableJavaType() {
        return getJavaType();
    }
}
