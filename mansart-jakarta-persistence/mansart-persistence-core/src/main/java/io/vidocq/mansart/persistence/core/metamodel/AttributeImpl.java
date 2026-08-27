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

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Attribute.PersistentAttributeType;

/**
 * Base implementation of {@link Attribute} for the Mansart persistence provider.
 *
 * @param <X> the type containing the represented attribute
 * @param <Y> the type of the represented attribute
 */
public abstract class AttributeImpl<X, Y> extends TypeImpl<Y> implements Attribute<X, Y> {

    private final String name;
    private ManagedType<X> declaringType;
    private final PersistentAttributeType persistentAttributeType;
    protected final boolean optional;

    @SuppressWarnings({"rawtypes", "unchecked"})
    AttributeImpl(Class<?> javaType, String name,
                  ManagedType<?> declaringType,
                  PersistentAttributeType persistentAttributeType, boolean optional) {
        super(javaType, null);
        this.name = name;
        this.declaringType = (ManagedType<X>) declaringType;
        this.persistentAttributeType = persistentAttributeType;
        this.optional = optional;
    }

    /**
     * Constructor without declaring type — for bootstrap-time metamodel building
     * where the declaring type is not yet available. The declaring type is
     * set later via {@link #setDeclaringType}.
     */
    @SuppressWarnings("rawtypes")
    AttributeImpl(Class<?> javaType, String name,
                  PersistentAttributeType persistentAttributeType, boolean optional) {
        super(javaType, null);
        this.name = name;
        this.declaringType = null;
        this.persistentAttributeType = persistentAttributeType;
        this.optional = optional;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public PersistentAttributeType getPersistentAttributeType() {
        return persistentAttributeType;
    }

    @Override
    public ManagedType<X> getDeclaringType() {
        return declaringType;
    }

    /**
     * Package-private setter for wiring attributes to their declaring type
     * after the managed type is constructed.
     */
    @SuppressWarnings("unchecked")
    void setDeclaringType(ManagedType<?> declaringType) {
        this.declaringType = (ManagedType<X>) declaringType;
    }

    @Override
    public Class<Y> getJavaType() {
        return super.getJavaType();
    }

    @Override
    public java.lang.reflect.Member getJavaMember() {
        throw new UnsupportedOperationException(
            "not implemented: getJavaMember — use APT-generated accessors instead");
    }

    @Override
    public boolean isAssociation() {
        return false;
    }

    @Override
    public boolean isCollection() {
        return false;
    }
}
